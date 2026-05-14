package me.annie312;

import lombok.Getter;
import me.annie312.arena.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class EscapeTheMine extends JavaPlugin {

    private ConfigManager configManager;
    private TeamManager teamManager;
    private GameListener gameListener;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {

        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        this.teamManager = new TeamManager();
        this.gameListener = new GameListener(this);
        this.arenaManager = new ArenaManager(this);

        getServer().getPluginManager().registerEvents(gameListener, this);
        getServer().getPluginManager().registerEvents(new EventCanceller(), this);
        getServer().getPluginManager().registerEvents(new me.annie312.arena.ArenaEventListener(this, arenaManager), this);

        CommandHandler handler = new CommandHandler(this);
        getCommand("etm").setExecutor(handler);
        getCommand("lobby").setExecutor(handler);

        org.bukkit.World lobbyWorld = arenaManager.getLobbyWorld();
        if (lobbyWorld != null) {
            lobbyWorld.setTime(18000);
            lobbyWorld.setGameRuleValue("doDaylightCycle", "false");
            lobbyWorld.setGameRuleValue("doMobSpawning", "false");
            getLogger().info("Лобби-мир «" + lobbyWorld.getName() + "»: ночь, без спавна мобов.");
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            arenaManager.createArena("aqua");
            arenaManager.createArena("desert");
            arenaManager.createArena("space");
            getLogger().info("Стартовые арены созданы.");
        }, 20L);

        getLogger().info("EscapeTheMine включен!");
    }

    @Override
    public void onDisable() {
        if (arenaManager != null) {
            getLogger().info("Удаление временных арен...");
            java.util.List<String> ids = new java.util.ArrayList<>(arenaManager.getArenas().keySet());
            for (String id : ids) {
                arenaManager.removeArena(id);
            }
            
            getLogger().info("Принудительная очистка временных миров...");
            java.io.File worldContainer = Bukkit.getWorldContainer();
            java.io.File[] worldFolders = worldContainer.listFiles((dir, name) -> name.startsWith("run_"));
            
            if (worldFolders != null) {
                for (java.io.File worldFolder : worldFolders) {
                    try {
                        getLogger().info("Удаление оставшейся папки мира: " + worldFolder.getName());
                        deleteWorldFolderForce(worldFolder);
                    } catch (Exception e) {
                        getLogger().severe("Не удалось удалить папку мира " + worldFolder.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        getLogger().info("EscapeTheMine выключен.");
    }
    
    private void deleteWorldFolderForce(java.io.File folder) {
        if (folder.exists()) {
            try {
                java.io.File[] files = folder.listFiles();
                if (files != null) {
                    for (java.io.File file : files) {
                        if (file.isDirectory()) {
                            deleteWorldFolderForce(file);
                        } else {
                            file.setWritable(true);
                            file.delete();
                        }
                    }
                }
                folder.setWritable(true);
                folder.delete();
                getLogger().info("Папка мира успешно удалена: " + folder.getName());
            } catch (Exception e) {
                getLogger().severe("Ошибка при удалении папки мира " + folder.getName() + ": " + e.getMessage());
            }
        }
    }

}