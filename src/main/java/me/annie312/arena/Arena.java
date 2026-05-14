package me.annie312.arena;

import lombok.Getter;
import me.annie312.EscapeTheMine;
import me.annie312.GameState;
import me.annie312.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Одна инстанс-арена: очередь в лобби арены и матч «Побег» в своём мире.
 * Координаты охраны, заключённого, клетки и радиус верстаков — из config.yml (как в ETM), мир подставляется из этой арены.
 */
public class Arena {

    private final EscapeTheMine plugin;
    @Getter
    private final String id;
    @Getter
    private final World world;
    private final long arenaTime;

    @Getter
    private final List<Player> players = new ArrayList<>();

    @Getter
    private GameState gameState = GameState.WAITING;
    private boolean lobbyTimerRunning;
    private BukkitTask lobbyTask;
    private BukkitTask gameTimerTask;

    @Getter
    private int timeLeftSeconds;
    private int repairedCount;

    @Getter
    private final Set<UUID> guards = new HashSet<>();
    @Getter
    private final Set<UUID> prisoners = new HashSet<>();
    @Getter
    private final Set<UUID> capturedPrisoners = new HashSet<>();
    @Getter
    private final Set<UUID> frozenPlayers = new HashSet<>();
    @Getter
    private final Map<UUID, UUID> dragging = new HashMap<>();
    private final List<Location> activeComputers = new ArrayList<>();

    public Arena(EscapeTheMine plugin, String id, World world, long arenaTime) {
        this.plugin = plugin;
        this.id = id;
        this.world = world;
        this.arenaTime = arenaTime;
    }

    public boolean isRunning() { return gameState == GameState.INGAME; }

    public static String formatTime(int sec) {
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    /** Ожидание: точка из spawn.prisoner в мире арены (как в ETM). */
    public Location getWaitingLocation() {
        return plugin.getConfigManager().getLocInWorld(world, "spawn.prisoner");
    }

    /** Наблюдатель: над точкой заключённого. */
    public Location getSpectateLocation() {
        Location base = getWaitingLocation();
        if (base == null) return world.getSpawnLocation();
        return base.clone().add(0, 8, 0);
    }

    public void addPlayer(Player p) {
        if (players.contains(p)) return;
        ArenaManager mgr = plugin.getArenaManager();
        for (Arena other : mgr.getArenas().values()) {
            if (other != this && other.players.contains(p)) {
                other.removePlayer(p);
            }
        }
        players.add(p);
        if (gameState == GameState.WAITING && !lobbyTimerRunning) {
            int need = plugin.getConfig().getInt("min-number-of-players", 2);
            if (players.size() >= need) {
                startLobbyCountdown();
            }
        }
    }

    public void removePlayer(Player p) {
        players.remove(p);
        if (gameState == GameState.INGAME) {
            handlePlayerLeftMatch(p);
        }
        if (gameState == GameState.STARTING && players.size() < plugin.getConfig().getInt("min-number-of-players", 2)) {
            cancelLobbyCountdownWithMessage();
        }
        checkWinConditions();
    }

    public void handlePlayerQuit(Player p) {
        removePlayer(p);
    }

    private void handlePlayerLeftMatch(Player p) {
        UUID id = p.getUniqueId();
        plugin.getTeamManager().detachPlayer(p);
        guards.remove(id);
        prisoners.remove(id);
        capturedPrisoners.remove(id);
        frozenPlayers.remove(id);
        dragging.remove(id);
        dragging.values().remove(id);
    }

    private void abortLobbyTaskOnly() {
        if (lobbyTask != null) {
            lobbyTask.cancel();
            lobbyTask = null;
        }
        lobbyTimerRunning = false;
    }

    private void cancelLobbyCountdownWithMessage() {
        abortLobbyTaskOnly();
        gameState = GameState.WAITING;
        broadcast("§c[Арена " + id + "] Недостаточно игроков, отсчёт отменён.");
    }

    private void startLobbyCountdown() {
        lobbyTimerRunning = true;
        gameState = GameState.STARTING;
        broadcast("§7[Арена " + id + "] Старт через §e20 §7сек. (игроков: §a" + players.size() + "§7)");

        lobbyTask = new BukkitRunnable() {
            int seconds = 20;

            @Override
            public void run() {
                if (gameState != GameState.STARTING) {
                    cancel();
                    return;
                }
                int need = plugin.getConfig().getInt("min-number-of-players", 2);
                if (players.size() < need) {
                    cancelLobbyCountdownWithMessage();
                    cancel();
                    return;
                }
                if (seconds <= 0) {
                    lobbyTimerRunning = false;
                    lobbyTask = null;
                    cancel();
                    startEscapeGame();
                    return;
                }
                if (seconds == 20 || seconds == 10 || seconds <= 5) {
                    broadcast("§7[Арена " + id + "] До старта: §e" + seconds + " §7сек.");
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /** Ручной старт (например /etm start): только из очереди, без повторного таймера. */
    public void startGameManual() {
        if (gameState == GameState.INGAME) return;
        if (players.size() < plugin.getConfig().getInt("min-number-of-players", 2)) {
            return;
        }
        abortLobbyTaskOnly();
        startEscapeGame();
    }

    private void startEscapeGame() {
        if (gameState == GameState.INGAME) return;
        abortLobbyTaskOnly();
        gameState = GameState.INGAME;

        world.setTime(arenaTime);
        world.setGameRuleValue("doDaylightCycle", "false");
        world.setStorm(false);

        assignTeams();
        generateComputers(5);

        for (UUID gid : guards) {
            frozenPlayers.add(gid);
            Player g = Bukkit.getPlayer(gid);
            if (g != null) g.sendMessage("§6§l[!] §fЖди 10 сек!");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.INGAME) return;
                for (UUID gid : guards) {
                    frozenPlayers.remove(gid);
                    Player g = Bukkit.getPlayer(gid);
                    if (g != null) {
                        g.sendMessage("§a§l[!] §fВПЕРЁД!");
                        g.playSound(g.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                    }
                }
            }
        }.runTaskLater(plugin, 200L);

        timeLeftSeconds = plugin.getConfig().getInt("game-duration", 300);
        if (gameTimerTask != null) gameTimerTask.cancel();
        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.INGAME) {
                    cancel();
                    return;
                }
                if (timeLeftSeconds <= 0) {
                    stopGame("§cВремя вышло! Победа охранников");
                    cancel();
                    return;
                }
                String msg = "§fДо конца: §e" + formatTime(timeLeftSeconds);
                for (Player p : players) {
                    if (p == null || !p.isOnline()) continue;
                    if (!plugin.getGameListener().getRepairProgress().containsKey(p.getUniqueId())) {
                        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(msg));
                    }
                }
                timeLeftSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        broadcast("§a§l[Арена " + id + "] Игра началась!");
    }

    private void assignTeams() {
        TeamManager tm = plugin.getTeamManager();
        for (Player p : new ArrayList<>(players)) {
            tm.detachPlayer(p);
            p.getInventory().clear();
        }
        guards.clear();
        prisoners.clear();
        capturedPrisoners.clear();
        frozenPlayers.clear();
        dragging.clear();
        repairedCount = 0;

        List<Player> pool = new ArrayList<>(players);
        Collections.shuffle(pool);
        int guardCount = Math.max(1, pool.size() / 3);
        if (pool.size() == 2) guardCount = 1;

        for (int i = 0; i < pool.size(); i++) {
            Player p = pool.get(i);
            UUID uid = p.getUniqueId();
            if (i < guardCount) {
                assignGuard(p, uid, tm);
            } else {
                assignPrisoner(p, uid, tm);
            }
        }
    }

    private void assignGuard(Player p, UUID id, TeamManager tm) {
        guards.add(id);
        tm.attachGuard(p);
        p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
        Location loc = plugin.getConfigManager().getLocInWorld(world, "spawn.guard");
        if (loc != null) p.teleport(loc);
        p.sendMessage("§9§l[ETM] §fТы — §9ОХРАННИК§f! (арена §e" + id + "§f)");
    }

    private void assignPrisoner(Player p, UUID id, TeamManager tm) {
        prisoners.add(id);
        tm.attachPrisoner(p);
        Location loc = plugin.getConfigManager().getLocInWorld(world, "spawn.prisoner");
        if (loc != null) p.teleport(loc);
        p.sendMessage("§c§l[ETM] §fТы — §cЗАКЛЮЧЕННЫЙ§f! (арена §e" + id + "§f)");
    }

    @SuppressWarnings("deprecation")
    public void generateComputers(int count) {
        Location center = plugin.getConfigManager().getLocInWorld(world, "spawn.prisoner");
        if (center == null) return;
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            int x = center.getBlockX() + random.nextInt(41) - 20;
            int z = center.getBlockZ() + random.nextInt(41) - 20;
            int y = world.getHighestBlockYAt(x, z);
            Location loc = new Location(world, x, y + 1, z);
            loc.getBlock().setType(Material.WORKBENCH);
            activeComputers.add(loc);
            for (Player pl : world.getPlayers()) {
                pl.sendBlockChange(loc, Material.WORKBENCH, (byte) 0);
            }
        }
    }

    public void incrementRepaired(Player p) {
        repairedCount++;
        int total = activeComputers.size();
        broadcast("§6[!] " + p.getDisplayName() + " §fпочинил верстак! (§a" + repairedCount + "§f/" + total + "§f)");
        if (total > 0 && repairedCount >= total) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> stopGame("§a§lЗаключенные починили все верстаки и сбежали!"), 1L);
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
        Location spawn = plugin.getConfigManager().getLocInWorld(world, "spawn.prisoner");
        if (spawn != null) victim.teleport(spawn);
        victim.sendMessage("§a§l[!] §fТебя спасли!");
    }

    public void checkWinConditions() {
        if (gameState != GameState.INGAME) return;
        if (prisoners.isEmpty()) {
            stopGame("§9Охрана победила! Все заключенные покинули игру.");
        } else if (guards.isEmpty()) {
            stopGame("§cЗаключенные победили! Охрана дезертировала.");
        } else if (capturedPrisoners.size() >= prisoners.size()) {
            stopGame("§9Охрана победила! Все заключенные пойманы.");
        }
    }

    public void stopGame(String message) {
        abortLobbyTaskOnly();
        gameState = GameState.WAITING;

        if (gameTimerTask != null) {
            gameTimerTask.cancel();
            gameTimerTask = null;
        }

        broadcast("§6§l[!] " + message);

        plugin.getGameListener().clearProgress();

        TeamManager tm = plugin.getTeamManager();
        Location lobby = plugin.getArenaManager().resolveLobbySpawn();

        for (Player p : new ArrayList<>(players)) {
            if (p != null && p.isOnline()) {
                p.getInventory().clear();
                for (org.bukkit.potion.PotionEffect ef : p.getActivePotionEffects()) {
                    p.removePotionEffect(ef.getType());
                }
                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(""));
                tm.detachPlayer(p);
                p.setGameMode(org.bukkit.GameMode.ADVENTURE);
                p.setAllowFlight(true);
                p.setFlying(false);
                if (lobby != null) p.teleport(lobby);
            }
        }

        // Teleport spectators (players in arena world but not in players list)
        for (Player p : world.getPlayers()) {
            if (p != null && p.isOnline() && !players.contains(p)) {
                p.setGameMode(org.bukkit.GameMode.ADVENTURE);
                p.setAllowFlight(true);
                p.setFlying(false);
                if (lobby != null) p.teleport(lobby);
            }
        }

        for (Location loc : activeComputers) {
            if (loc.getWorld() != null) loc.getBlock().setType(Material.AIR);
        }
        activeComputers.clear();

        guards.clear();
        prisoners.clear();
        capturedPrisoners.clear();
        frozenPlayers.clear();
        dragging.clear();
        repairedCount = 0;
        players.clear();
    }

    private void broadcast(String msg) {
        for (Player p : players) {
            if (p != null && p.isOnline()) p.sendMessage(msg);
        }
    }
}
