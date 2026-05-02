package me.annie312;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GameManager {
    private final EscapeTheMine plugin;
    @Getter @Setter private GameState gameState = GameState.WAITING;
    @Getter private int currentTimeLeft = 0; // Для Action Bar

    public GameManager(EscapeTheMine plugin) { this.plugin = plugin; }

    public void startGame() {
        if (gameState == GameState.INGAME) return;
        setGameState(GameState.INGAME);

        TeamManager tm = plugin.getTeamManager();
        tm.assignTeams(plugin.getLobbyManager().getLobbyPlayers());
        plugin.getLobbyManager().clearLobby();
        tm.generateComputers(5);

        for (UUID id : tm.getGuards()) {
            tm.getFrozenPlayers().add(id);
            Player g = Bukkit.getPlayer(id);
            if (g != null) g.sendMessage("§6§l[!] §fЖди 10 сек!");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.INGAME) return;
                for (UUID id : tm.getGuards()) {
                    tm.getFrozenPlayers().remove(id);
                    Player g = Bukkit.getPlayer(id);
                    if (g != null) {
                        g.sendMessage("§a§l[!] §fВПЕРЁД!");
                        g.playSound(g.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1f);
                    }
                }
            }
        }.runTaskLater(plugin, 200L);

        startGameTimer();
    }

    private void startGameTimer() {
        currentTimeLeft = 300;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.INGAME) { this.cancel(); return; }
                if (currentTimeLeft <= 0) { stopGame("§cВремя вышло! Победа охранников"); this.cancel(); return; }

                // Находим цикл в методе startGameTimer() и заменяем на этот:
                String msg = "§fДо конца: §e" + getTimeString(currentTimeLeft);

                for (Player p : Bukkit.getOnlinePlayers()) {
                    // Если игрок НЕ чинит (его нет в списке активных ремонтников), шлем обычный таймер
                    if (!plugin.getGameListener().getRepairProgress().containsKey(p.getUniqueId())) {
                        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(msg));
                    }
                }

                currentTimeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void stopGame(String message) {
        if (gameState == GameState.WAITING) return;
        setGameState(GameState.WAITING);
        Bukkit.broadcastMessage("§6§l[!] " + message);

        if (plugin.getGameListener() != null) plugin.getGameListener().clearProgress();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.getInventory().clear();
            for (org.bukkit.potion.PotionEffect ef : p.getActivePotionEffects()) p.removePotionEffect(ef.getType());
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(""));
            Location lb = plugin.getConfigManager().getLoc("lobby");
            if (lb != null) p.teleport(lb);
        }
        plugin.getTeamManager().cleanupGame();
    }

    public String getTimeString(int sec) { return String.format("%02d:%02d", sec / 60, sec % 60); }
}