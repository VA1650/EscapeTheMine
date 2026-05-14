package me.annie312;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Общий скорборд префиксов О/З. Состояние матча хранится в {@link me.annie312.arena.Arena}.
 */
public class TeamManager {
    private Team guardTeam;
    private Team prisonerTeam;

    public TeamManager() {
        setupScoreboard();
    }

    private void setupScoreboard() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        if (board.getTeam("ETM_Guards") != null) board.getTeam("ETM_Guards").unregister();
        if (board.getTeam("ETM_Prisoners") != null) board.getTeam("ETM_Prisoners").unregister();

        guardTeam = board.registerNewTeam("ETM_Guards");
        prisonerTeam = board.registerNewTeam("ETM_Prisoners");

        guardTeam.setPrefix("§9[О] ");
        guardTeam.setColor(ChatColor.BLUE);

        prisonerTeam.setPrefix("§c[З] ");
        prisonerTeam.setColor(ChatColor.RED);

        prisonerTeam.setAllowFriendlyFire(true);
        guardTeam.setAllowFriendlyFire(true);
    }

    public void attachGuard(Player p) {
        guardTeam.addEntry(p.getName());
        p.setDisplayName("§9[О] " + p.getName() + "§f");
        p.setPlayerListName("§9[О] " + p.getName());
    }

    public void attachPrisoner(Player p) {
        prisonerTeam.addEntry(p.getName());
        p.setDisplayName("§c[З] " + p.getName() + "§f");
        p.setPlayerListName("§c[З] " + p.getName());
    }

    public void detachPlayer(Player p) {
        guardTeam.removeEntry(p.getName());
        prisonerTeam.removeEntry(p.getName());
        p.setDisplayName(p.getName());
        p.setPlayerListName(p.getName());
    }
}
