package me.annie312.arena;

import lombok.Getter;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {

    private final me.annie312.EscapeTheMine plugin;
    @Getter
    private final Map<String, Arena> arenas = new HashMap<>();

    public ArenaManager(me.annie312.EscapeTheMine plugin) {
        this.plugin = plugin;
    }

    /**
     * Создает новую арену, копируя мир из папки-шаблона.
     * @param mapType тип карты (aqua, desert, space)
     * @return ID созданной арены или null в случае ошибки
     */
    public String createArena(String mapType) {
        String templateName = "template_" + mapType;
        
        // Находим следующий доступный номер для этого типа карты
        String arenaId = getString(mapType);
        String worldName = "run_" + arenaId;

        File templateDir = new File(Bukkit.getWorldContainer(), templateName);
        File targetDir = new File(Bukkit.getWorldContainer(), worldName);

        // Проверка существования шаблона
        if (!templateDir.exists()) {
            Bukkit.getLogger().severe("[ETM] Шаблон " + templateName + " не найден в корне сервера!");
            return null;
        }

        try {
            // 1. Копируем файлы мира
            FileUtil.copy(templateDir, targetDir);

            // 2. Удаляем сессионный замок и UID, чтобы Bukkit создал новые
            new File(targetDir, "uid.dat").delete();
            new File(targetDir, "session.lock").delete();

            // 3. Загружаем мир в систему
            WorldCreator creator = new WorldCreator(worldName);
            World world = Bukkit.createWorld(creator);

            if (world == null) return null;

            // Настройки оптимизации: не сохраняем изменения блоков на диск
            world.setAutoSave(false);
            world.setKeepSpawnInMemory(false);

            // 4. Определяем время суток для атмосферы
            long time = 6000; // День по умолчанию
            if (mapType.equalsIgnoreCase("desert")) time = 12000; // Закат
            if (mapType.equalsIgnoreCase("space")) time = 18000;  // Ночь

            // 5. Создаем объект арены
            Arena arena = new Arena(plugin, arenaId, world, time);

            arenas.put(arenaId, arena);
            Bukkit.getLogger().info("[ETM] Создана арена: " + arenaId);

            return arenaId;

        } catch (Exception e) {
            Bukkit.getLogger().severe("[ETM] Ошибка при копировании мира: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private @NotNull String getString(String mapType) {
        int nextNumber = 1;
        for (Arena existing : arenas.values()) {
            if (existing.getId().startsWith(mapType + "-")) {
                String[] parts = existing.getId().split("-");
                try {
                    int existingNumber = Integer.parseInt(parts[1]);
                    if (existingNumber >= nextNumber) {
                        nextNumber = existingNumber + 1;
                    }
                } catch (NumberFormatException e) {
                    // Игнорируем некорректные ID
                }
            }
        }

        return mapType + "-" + nextNumber;
    }

    /**
     * Полностью удаляет арену: кикает игроков, выгружает мир и чистит папку.
     */
    public void removeArena(String id) {
        Arena arena = arenas.get(id);
        if (arena == null) return;

        arena.stopGame("§cАрена была закрыта.");

        World world = arena.getWorld();
        Location lobbySpawn = resolveLobbySpawn();

        // Телепортируем оставшихся в мире (например наблюдателей вне списка арены)
        for (Player p : world.getPlayers()) {
            p.getInventory().clear();
            p.setGameMode(org.bukkit.GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(false);
            for (org.bukkit.potion.PotionEffect ef : p.getActivePotionEffects()) {
                p.removePotionEffect(ef.getType());
            }
            if (lobbySpawn != null) {
                p.teleport(lobbySpawn);
            }
        }

        // Выгружаем мир без сохранения
        Bukkit.unloadWorld(world, false);

        // Сначала синхронно пытаемся удалить мир
        File worldFolder = new File(Bukkit.getWorldContainer(), world.getName());
        if (worldFolder.exists()) {
            try {
                deleteWorldFolder(worldFolder);
                Bukkit.getLogger().info("[ETM] Папка мира удалена синхронно: " + world.getName());
            } catch (Exception e) {
                Bukkit.getLogger().warning("[ETM] Синхронное удаление не удалось: " + e.getMessage() + ". Пробуем асинхронно...");
                
                // Если синхронное удаление не удалось, пробуем асинхронно с задержкой
                new BukkitRunnable() {
                    private int attempts = 0;
                    @Override
                    public void run() {
                        File worldFolder = new File(Bukkit.getWorldContainer(), world.getName());
                        if (worldFolder.exists()) {
                            FileUtil.delete(worldFolder);
                            attempts++;
                            
                            if (worldFolder.exists()) {
                                if (attempts < 5) {
                                    Bukkit.getLogger().warning("[ETM] Попытка " + attempts + " удаления папки мира не удалась: " + world.getName() + ". Повторная попытка через 3 секунды...");
                                    // Повторная попытка через 60 тиков (3 секунды)
                                    this.runTaskLater(plugin, 60L);
                                } else {
                                    Bukkit.getLogger().severe("[ETM] Не удалось удалить папку мира после 5 попыток: " + world.getName());
                                    // Последняя попытка - принудительное удаление
                                    try {
                                        deleteWorldFolderForce(worldFolder);
                                    } catch (Exception ex) {
                                        Bukkit.getLogger().severe("[ETM] Критическая ошибка при удалении мира: " + ex.getMessage());
                                    }
                                }
                            } else {
                                Bukkit.getLogger().info("[ETM] Папка мира успешно удалена: " + world.getName() + " (попытка " + attempts + ")");
                            }
                        }
                    }
                }.runTaskLater(plugin, 60L); // Задержка 3 секунды для выгрузки
            }
        }

        arenas.remove(id);
        Bukkit.getLogger().info("[ETM] Арена " + id + " удалена.");
    }

    public Arena getArenaByPlayer(Player p) {
        for (Arena a : arenas.values()) {
            if (a.getPlayers().contains(p)) return a;
        }
        return null;
    }

    public Arena getArenaById(String id) {
        return arenas.get(id);
    }

    public void handlePlayerQuit(Player p) {
        Arena a = getArenaByPlayer(p);
        if (a != null) {
            a.handlePlayerQuit(p);
        }
    }

    public Arena getQuickJoinArena() {
        for (Arena a : arenas.values()) {
            // Ищем арену, которая не запущена и где есть место (допустим, 12 человек)
            if (!a.isRunning() && a.getPlayers().size() < 12) {
                return a;
            }
        }
        return null;
    }

    /** Точка лобби из config.yml (ключ lobby) или спавн мира world. */
    public Location resolveLobbySpawn() {
        Location cfg = plugin.getConfigManager().getLoc("lobby");
        if (cfg != null) return cfg;
        World w = Bukkit.getWorld("world");
        if (w != null) return w.getSpawnLocation();
        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    /** Мир лобби (по точке lobby в конфиге или мир world). */
    public World getLobbyWorld() {
        Location l = resolveLobbySpawn();
        if (l != null && l.getWorld() != null) {
            return l.getWorld();
        }
        return Bukkit.getWorld("world");
    }

    public boolean isLobbyWorld(World world) {
        if (world == null) return false;
        World lw = getLobbyWorld();
        return world.equals(lw);
    }
    
    /**
     * Принудительное удаление папки мира с обработкой ошибок
     */
    private void deleteWorldFolder(File folder) {
        if (folder.exists()) {
            // Удаляем все файлы и подпапки
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteWorldFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
            // Удаляем саму папку
            folder.delete();
        }
    }
    
    /**
     * Максимально принудительное удаление папки мира
     */
    private void deleteWorldFolderForce(File folder) {
        if (folder.exists()) {
            // Устанавливаем права на запись для папки
            folder.setWritable(true);
            
            // Удаляем все файлы и подпапки с принудительными правами
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.setWritable(true);
                    if (file.isDirectory()) {
                        deleteWorldFolderForce(file);
                    } else {
                        file.delete();
                    }
                }
            }
            // Удаляем саму папку
            folder.delete();
        }
    }
}
