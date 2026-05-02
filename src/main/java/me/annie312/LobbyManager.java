package me.annie312;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class LobbyManager {
    private final EscapeTheMine plugin;
    @Getter private final List<Player> lobbyPlayers = new ArrayList<>();
    private boolean isTimerRunning = false;

    public LobbyManager(EscapeTheMine plugin) {
        this.plugin = plugin;
    }

    public void addPlayerToLobby(Player p) {
        if (plugin.getGameManager().getGameState() != GameState.WAITING &&
                plugin.getGameManager().getGameState() != GameState.STARTING) {
            p.sendMessage("§cИгра уже идет!");
            return;
        }

        if (!lobbyPlayers.contains(p)) {
            lobbyPlayers.add(p);
            broadcast("§e" + p.getName() + " §fприсоединился к игре! (§a" + lobbyPlayers.size() + "§f/10)");

            // Если набралось 2 игрока (минимум), запускаем отсчет
            if (lobbyPlayers.size() >= 2 && !isTimerRunning) {
                startLobbyTimer();
            }
        }
    }

    public void removePlayerFromLobby(Player p) {
        if (lobbyPlayers.remove(p)) {
            broadcast("§e" + p.getName() + " §fпокинул лобби.");
        }
    }

    private void startLobbyTimer() {
        isTimerRunning = true;
        plugin.getGameManager().setGameState(GameState.STARTING);

        new BukkitRunnable() {
            int seconds = 20; // Время ожидания в лобби

            @Override
            public void run() {
                if (lobbyPlayers.size() < 2) {
                    broadcast("§cНедостаточно игроков для старта. Отмена.");
                    plugin.getGameManager().setGameState(GameState.WAITING);
                    isTimerRunning = false;
                    this.cancel();
                    return;
                }

                if (seconds <= 0) {
                    broadcast("§a§lИгра начинается!");
                    plugin.getGameManager().startGame(); // ПУСК
                    isTimerRunning = false;
                    this.cancel();
                    return;
                }

                if (seconds == 20 || seconds == 10 || seconds <= 5) {
                    broadcast("§fСтарт через §e" + seconds + " §fсек.");
                }

                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void clearLobby() {
        lobbyPlayers.clear();
    }

    private void broadcast(String message) {
        for (Player p : lobbyPlayers) {
            p.sendMessage("§7[Лобби] " + message);
        }
    }
}