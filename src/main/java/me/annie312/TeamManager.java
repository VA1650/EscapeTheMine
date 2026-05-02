package me.annie312;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TeamManager {
    private final EscapeTheMine plugin;

    @Getter private final Set<UUID> guards = new HashSet<>();
    @Getter private final Set<UUID> prisoners = new HashSet<>();
    @Getter private final Set<UUID> capturedPrisoners = new HashSet<>();
    @Getter private final Set<UUID> frozenPlayers = new HashSet<>();

    @Getter private final Map<UUID, UUID> dragging = new HashMap<>();
    @Getter private final List<Location> activeComputers = new ArrayList<>();

    @Getter private int repairedCount = 0;

    private Team guardTeam;
    private Team prisonerTeam;

    public TeamManager(EscapeTheMine plugin) {
        this.plugin = plugin;
        setupScoreboard();
    }

    private void setupScoreboard() {
        // Используем главный скорборд сервера
        // Скорборд для префиксов и цветов
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // Чистим старые команды, если они остались
        if (board.getTeam("ETM_Guards") != null) board.getTeam("ETM_Guards").unregister();
        if (board.getTeam("ETM_Prisoners") != null) board.getTeam("ETM_Prisoners").unregister();

        guardTeam = board.registerNewTeam("ETM_Guards");
        prisonerTeam = board.registerNewTeam("ETM_Prisoners");

        // Настраиваем визуал Охраны (Синие)
        guardTeam.setPrefix("§9[О] ");
        // В 1.12.2 setColor влияет на цвет ника в табе и над головой
        guardTeam.setColor(ChatColor.BLUE);

        // Настраиваем визуал Заключенных (Красные)
        prisonerTeam.setPrefix("§c[З] ");
        prisonerTeam.setColor(ChatColor.RED);

        prisonerTeam.setAllowFriendlyFire(true);
        guardTeam.setAllowFriendlyFire(true);
    }

    public void assignTeams(List<Player> players) {
        cleanupGame();
        if (players.isEmpty()) return;

        List<Player> pool = new ArrayList<>(players);
        Collections.shuffle(pool, new Random());
        Collections.shuffle(pool, new Random());

        int guardCount = Math.max(1, (int) Math.floor(pool.size() / 3.0));
        if (pool.size() == 2) guardCount = 1;

        for (int i = 0; i < pool.size(); i++) {
            Player p = pool.get(i);
            UUID id = p.getUniqueId();
            p.getInventory().clear();

            if (i < guardCount) {
                guards.add(id);
                guardTeam.addEntry(p.getName()); // Добавляем в команду скорборда

                p.setDisplayName("§9[О] " + p.getName() + "§f");
                p.setPlayerListName("§9[О] " + p.getName());

                p.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
                Location loc = plugin.getConfigManager().getLoc("spawn.guard");
                if (loc != null) p.teleport(loc);
                p.sendMessage("§9§l[ETM] §fТы — §9ОХРАННИК§f!");
            } else {
                prisoners.add(id);
                prisonerTeam.addEntry(p.getName()); // Добавляем в команду скорборда

                p.setDisplayName("§c[З] " + p.getName() + "§f");
                p.setPlayerListName("§c[З] " + p.getName());

                Location loc = plugin.getConfigManager().getLoc("spawn.prisoner");
                if (loc != null) p.teleport(loc);
                p.sendMessage("§c§l[ETM] §fТы — §cЗАКЛЮЧЕННЫЙ§f!");
            }
        }
    }

    public void incrementRepaired(Player p) {
        int current = repairedCount + 1;
        int total = activeComputers.size();
        repairedCount++;

        Bukkit.broadcastMessage("§6[!] " + p.getDisplayName() + " §fпочинил верстак! (§a" + current + "§f/" + total + "§f)");

        if (total > 0 && repairedCount >= total) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.getGameManager().stopGame("§a§lЗаключенные починили все верстаки и сбежали!"), 1L);
        }
    }

    public void startDragging(Player guard, Player victim) {
        dragging.put(guard.getUniqueId(), victim.getUniqueId());
        frozenPlayers.add(victim.getUniqueId());
        victim.sendMessage("§c§l[!] §fОхранник схватил тебя!");
        guard.sendMessage("§9§l[!] §fТы схватил " + victim.getDisplayName() + "§f.");
    }

    public void release(Player victim) {
        UUID id = victim.getUniqueId();
        dragging.values().remove(id);
        capturedPrisoners.remove(id);
        frozenPlayers.remove(id);

        Location spawn = plugin.getConfigManager().getLoc("spawn.prisoner");
        if (spawn != null) victim.teleport(spawn);
        victim.sendMessage("§a§l[!] §fТебя спасли!");
    }

    public void generateComputers(int count) {
        Location center = plugin.getConfigManager().getLoc("spawn.prisoner");
        if (center == null) return;
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            int x = center.getBlockX() + random.nextInt(41) - 20;
            int z = center.getBlockZ() + random.nextInt(41) - 20;
            int y = center.getWorld().getHighestBlockYAt(x, z);
            Location loc = new Location(center.getWorld(), x, y + 1, z);
            loc.getBlock().setType(Material.WORKBENCH);
            activeComputers.add(loc);

            for(Player online : Bukkit.getOnlinePlayers()) {
                online.sendBlockChange(loc, Material.WORKBENCH, (byte) 0);
            }
        }
    }

    public void handleQuit(Player p) {
        UUID id = p.getUniqueId();

        // Убираем из команд скорборда при выходе
        guardTeam.removeEntry(p.getName());
        prisonerTeam.removeEntry(p.getName());

        guards.remove(id);
        prisoners.remove(id);
        capturedPrisoners.remove(id);
        frozenPlayers.remove(id);
        dragging.remove(id);
        dragging.values().remove(id);

        checkWinConditions();
    }

    public void checkWinConditions() {
        if (plugin.getGameManager().getGameState() != GameState.INGAME) return;

        if (prisoners.isEmpty()) {
            plugin.getGameManager().stopGame("§9Охрана победила! Все заключенные покинули игру.");
        } else if (guards.isEmpty()) {
            plugin.getGameManager().stopGame("§cЗаключенные победили! Охрана дезертировала.");
        } else if (capturedPrisoners.size() >= prisoners.size()) {
            plugin.getGameManager().stopGame("§9Охрана победила! Все заключенные пойманы.");
        }
    }

    public void cleanupGame() {
        // Убираем всех из команд скорборда
        for (String entry : new HashSet<>(guardTeam.getEntries())) guardTeam.removeEntry(entry);
        for (String entry : new HashSet<>(prisonerTeam.getEntries())) prisonerTeam.removeEntry(entry);

        for (Location loc : activeComputers) loc.getBlock().setType(Material.AIR);
        activeComputers.clear();

        // Возвращаем дефолтные ники
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setDisplayName(p.getName());
            p.setPlayerListName(p.getName());
        }

        guards.clear();
        prisoners.clear();
        capturedPrisoners.clear();
        frozenPlayers.clear();
        dragging.clear();
        repairedCount = 0;
    }
}