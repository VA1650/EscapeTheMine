package me.annie312;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {

    private final EscapeTheMine plugin;

    public CommandHandler(EscapeTheMine plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команды только для игроков!");
            return true;
        }
        Player p = (Player) sender;

        // Если ввели /lobby
        if (command.getName().equalsIgnoreCase("lobby")) {
            plugin.getLobbyManager().addPlayerToLobby(p);
            return true;
        }

        // Если ввели /etm
        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Используй: /etm [join|leave|setspawn]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "join":
                plugin.getLobbyManager().addPlayerToLobby(p);
                break;
            case "leave":
                plugin.getLobbyManager().removePlayerFromLobby(p);
                break;
            case "setspawn":
                if (!p.hasPermission("etm.admin")) return true;
                if (args.length < 2) {
                    p.sendMessage(ChatColor.RED + "Юзай: /etm setspawn [guard|prisoner|cell]");
                    return true;
                }
                String type = args[1].toLowerCase();
                plugin.getConfigManager().saveLoc(type.equals("cell") ? "cell" : "spawn." + type, p.getLocation());
                p.sendMessage(ChatColor.GREEN + "Точка " + type + " сохранена!");
                break;
            default:
                p.sendMessage(ChatColor.RED + "Неизвестная подкоманда.");
        }
        return true;
    }
}