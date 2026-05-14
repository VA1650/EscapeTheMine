package me.annie312;

import me.annie312.arena.Arena;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

        if (command.getName().equalsIgnoreCase("lobby")) {
            handleWorldLobby(p);
            return true;
        }

        if (!command.getName().equalsIgnoreCase("etm")) {
            return true;
        }

        if (args.length == 0) {
            sendEtmHelp(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendEtmHelp(p);
                break;

            case "create":
                if (!p.hasPermission("etm.admin")) {
                    p.sendMessage("§cНет прав.");
                    return true;
                }
                if (args.length < 2) {
                    p.sendMessage("§cИспользование: /etm create <aqua|desert|space>");
                    return true;
                }
                String mapType = args[1].toLowerCase();
                p.sendMessage("§7Создание мира §e" + mapType + "§7...");
                String newId = plugin.getArenaManager().createArena(mapType);
                if (newId != null) {
                    p.sendMessage("§a§lГотово! §fАрена: §e" + newId);
                } else {
                    p.sendMessage("§cОшибка. Нужна папка template_" + mapType + " в корне сервера.");
                }
                break;

            case "join":
                if (args.length >= 2) {
                    Arena target = plugin.getArenaManager().getArenaById(args[1]);
                    if (target == null) {
                        p.sendMessage("§cАрена не найдена.");
                    } else if (target.isRunning()) {
                        p.sendMessage("§cИгра уже идёт. §7/etm spectate " + args[1]);
                    } else {
                        joinArenaWaiting(p, target);
                    }
                } else {
                    Arena quick = plugin.getArenaManager().getQuickJoinArena();
                    if (quick != null) {
                        joinArenaWaiting(p, quick);
                    } else {
                        p.sendMessage("§cНет свободных арен.");
                    }
                }
                break;

            case "quickjoin":
                Arena q = plugin.getArenaManager().getQuickJoinArena();
                if (q != null) {
                    joinArenaWaiting(p, q);
                } else {
                    p.sendMessage("§cНет свободных арен.");
                }
                break;

            case "spectate":
                if (args.length < 2) {
                    p.sendMessage("§cИспользование: /etm spectate <id>");
                    return true;
                }
                Arena spec = plugin.getArenaManager().getArenaById(args[1]);
                if (spec == null) {
                    p.sendMessage("§cАрена не найдена.");
                } else {
                    p.setGameMode(GameMode.SPECTATOR);
                    clearSpectatorEffects(p);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100000, 0, false, false));
                    p.teleport(spec.getSpectateLocation());
                    p.sendMessage("§7Наблюдение: §e" + spec.getId());
                }
                break;

            case "leave":
                leaveArenaToSpawn(p);
                break;

            case "lobby":
                handleWorldLobby(p);
                break;

            case "start":
                Arena toStart = plugin.getArenaManager().getArenaByPlayer(p);
                if (toStart == null) {
                    p.sendMessage("§cСначала зайди на арену (/etm join).");
                } else if (toStart.isRunning()) {
                    p.sendMessage("§cИгра уже идёт.");
                } else if (toStart.getPlayers().size() < plugin.getConfig().getInt("min-number-of-players", 2)) {
                    p.sendMessage("§cМало игроков в очереди этой арены.");
                } else {
                    toStart.startGameManual();
                    p.sendMessage("§aСтарт матча на арене §e" + toStart.getId());
                }
                break;

            case "list":
                p.sendMessage("§b§lАрены:");
                if (plugin.getArenaManager().getArenas().isEmpty()) {
                    p.sendMessage("§7Нет ни одной.");
                }
                for (Arena a : plugin.getArenaManager().getArenas().values()) {
                    String status = a.isRunning() ? "§cИГРА" : "§aОЖИДАНИЕ";
                    p.sendMessage("§8- §e" + a.getId() + " §7(" + a.getPlayers().size() + " игр.) " + status);
                }
                break;

            case "setspawn":
                handleSetSpawn(p, args);
                break;

            default:
                p.sendMessage(ChatColor.RED + "Неизвестная подкоманда. /etm help");
        }
        return true;
    }

    private void joinArenaWaiting(Player p, Arena arena) {
        arena.addPlayer(p);
        Location wait = arena.getWaitingLocation();
        if (wait != null) {
            p.teleport(wait);
        } else {
            p.teleport(arena.getWorld().getSpawnLocation());
        }
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 100000, 0, false, false));
        p.sendMessage("§aАрена §e" + arena.getId() + "§a. Ожидание игроков…");
    }

    private void leaveArenaToSpawn(Player p) {
        Arena current = plugin.getArenaManager().getArenaByPlayer(p);
        if (current != null) {
            current.removePlayer(p);
        }
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(true);
        p.setFlying(false);
        Location lobby = plugin.getArenaManager().resolveLobbySpawn();
        if (lobby != null) p.teleport(lobby);
        p.sendMessage("§6Ты покинул арену.");
    }

    private void handleWorldLobby(Player p) {
        Arena inArena = plugin.getArenaManager().getArenaByPlayer(p);
        if (inArena != null) {
            inArena.removePlayer(p);
        }
        p.getInventory().clear();
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(true);
        p.setFlying(false);
        Location lobby = plugin.getArenaManager().resolveLobbySpawn();
        if (lobby != null) {
            p.teleport(lobby);
        } else if (p.getServer().getWorld("world") != null) {
            p.teleport(p.getServer().getWorld("world").getSpawnLocation());
        }
        p.sendMessage("§aЛобби (мир world).");
    }

    private static void clearSpectatorEffects(Player p) {
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
        p.setWalkSpeed(0.2f);
        p.setFlySpeed(0.1f);
    }

    private void sendEtmHelp(Player p) {
        p.sendMessage("§8§m----------------------------------");
        p.sendMessage("§6§lEscapeTheMine §7— мультиарены");
        p.sendMessage("§e/etm join §7или §e/etm join <id> §7— очередь на арену");
        p.sendMessage("§e/etm quickjoin §7— случайная свободная арена");
        p.sendMessage("§e/etm spectate <id> §7— наблюдатель");
        p.sendMessage("§e/etm leave §7— выйти в спавн лобби");
        p.sendMessage("§e/etm lobby §7или §e/lobby §7— мир world (лобби)");
        p.sendMessage("§e/etm start §7— начать матч на своей арене");
        p.sendMessage("§e/etm list §7— список арен");
        p.sendMessage("§e/etm setspawn <guard|prisoner|cell|lobby> §7— точки из шаблона");
        if (p.hasPermission("etm.admin")) {
            p.sendMessage("§c/etm create <aqua|desert|space> §7— новый инстанс");
        }
        p.sendMessage("§8§m----------------------------------");
    }

    private void handleSetSpawn(Player p, String[] args) {
        if (!p.hasPermission("etm.admin")) {
            p.sendMessage(ChatColor.RED + "Нет прав.");
            return;
        }
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Используй: /etm setspawn <guard|prisoner|cell|lobby>");
            return;
        }
        String type = args[1].toLowerCase();
        String path;
        switch (type) {
            case "cell":
                path = "cell";
                break;
            case "lobby":
                path = "lobby";
                break;
            case "guard":
            case "prisoner":
                path = "spawn." + type;
                break;
            default:
                p.sendMessage(ChatColor.RED + "Только: guard, prisoner, cell, lobby");
                return;
        }
        plugin.getConfigManager().saveLoc(path, p.getLocation());
        p.sendMessage(ChatColor.GREEN + "Сохранено: " + path);
    }
}
