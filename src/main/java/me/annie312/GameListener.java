package me.annie312;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameListener implements Listener {
    private final EscapeTheMine plugin;
    @Getter private final Map<UUID, Integer> repairProgress = new HashMap<>();
    private final Map<UUID, Location> currentRepairingBlock = new HashMap<>();

    public GameListener(EscapeTheMine plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        TeamManager tm = plugin.getTeamManager();

        // Сброс починки при ходьбе
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            if (repairProgress.containsKey(uuid)) {
                repairProgress.remove(uuid);
                currentRepairingBlock.remove(uuid);
                p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent("§cПочинка прервана!"));
            }
        }

        // Конвоирование (UUID-based)
        if (tm.getDragging().containsKey(uuid)) {
            Player victim = Bukkit.getPlayer(tm.getDragging().get(uuid));
            if (victim != null) {
                victim.teleport(p.getLocation());
                Location cell = plugin.getConfigManager().getLoc("cell");
                if (cell != null && p.getLocation().distance(cell) < 2.5) {
                    UUID vId = victim.getUniqueId();
                    tm.getDragging().remove(uuid);
                    tm.getCapturedPrisoners().add(vId);
                    victim.teleport(cell);
                    p.sendMessage("§a§l[!] §fПосажен!");
                    tm.checkWinConditions();
                }
            }
        }

        // Заморозка
        if (tm.getFrozenPlayers().contains(uuid) && !tm.getDragging().containsValue(uuid)) {
            if (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onRepair(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock().getType() != Material.WORKBENCH) return;

        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        TeamManager tm = plugin.getTeamManager();

        // ПРОВЕРКА 1: Ты зэк?
        if (!tm.getPrisoners().contains(uuid)) return;

        // ПРОВЕРКА 2: Ты в тюрьме? (Запрещаем чинить)
        if (tm.getCapturedPrisoners().contains(uuid) || tm.getDragging().containsValue(uuid)) {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§cВы не можете чинить, пока пойманы!"));
            return;
        }

        event.setCancelled(true);
        Location loc = event.getClickedBlock().getLocation();
        String timeStr = plugin.getGameManager().getTimeString(plugin.getGameManager().getCurrentTimeLeft());

        // ПРОВЕРКА 3: Дистанция + Таймер (чтобы не дергалось)
        if (p.getLocation().distance(loc) > 3.5) {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§7[" + timeStr + "] §c§l! СЛИШКОМ ДАЛЕКО !"));
            return;
        }

        if (currentRepairingBlock.containsKey(uuid) && currentRepairingBlock.get(uuid).distance(loc) > 0.5) {
            repairProgress.put(uuid, 0);
        }
        currentRepairingBlock.put(uuid, loc);

        int max = 50;
        int progress = repairProgress.getOrDefault(uuid, 0) + 1;

        // ОБНОВЛЕННАЯ МАТЕМАТИКА ПОЛОСКИ
        // Теперь она доползает до конца ровно на 50-м клике
        if (progress >= max) {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§7[" + timeStr + "] " + getProgressBar(max, max)));

            repairProgress.remove(uuid);
            currentRepairingBlock.remove(uuid);
            event.getClickedBlock().setType(Material.AIR);

            tm.incrementRepaired(p);
        } else {
            repairProgress.put(uuid, progress);
            String bar = getProgressBar(progress, max);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§7[" + timeStr + "] " + bar));
        }
    }

    private String getProgressBar(int current, int max) {
        int totalBars = 20;
        // Используем Math.ceil, чтобы даже минимальный прогресс давал 1 деление,
        // а финальный клик закрашивал всю полоску.
        int completed = (int) Math.ceil((double) current / max * totalBars);

        StringBuilder sb = new StringBuilder("§eЧиним: §a");
        for (int i = 0; i < totalBars; i++) {
            if (i == completed) sb.append("§7");
            sb.append("■");
        }
        return sb.toString();
    }

    @EventHandler
    public void onChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        // Если игра идет — используем дисплейнейм с префиксом
        if (plugin.getGameManager().getGameState() == GameState.INGAME) {
            event.setFormat(p.getDisplayName() + "§7: §f" + event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) return;

        Player d = (Player) event.getDamager();
        Player v = (Player) event.getEntity();
        TeamManager tm = plugin.getTeamManager();

        UUID damagerID = d.getUniqueId();
        UUID victimID = v.getUniqueId();

        // Отключаем обычный урон, чтобы никто никого не убил
        event.setCancelled(true);

        // СИТУАЦИЯ А: Охранник бьет Заключенного (Ловит)
        if (tm.getGuards().contains(damagerID) && tm.getPrisoners().contains(victimID)) {
            // Проверяем, что в руке именно железный меч
            if (d.getInventory().getItemInMainHand().getType() == Material.IRON_SWORD) {
                // Ловим только если он еще не пойман и его не тащат
                if (!tm.getCapturedPrisoners().contains(victimID) && !tm.getDragging().containsValue(victimID)) {
                    tm.startDragging(d, v);
                }
            }
            return; // Выходим из метода
        }

        // СИТУАЦИЯ Б: Заключенный бьет Заключенного (Спасает)
        if (tm.getPrisoners().contains(damagerID) && tm.getPrisoners().contains(victimID)) {

            // 1. САМОПРОВЕРКА: Если атакующий зэк сам в камере или его тащат — он не может спасать
            if (tm.getCapturedPrisoners().contains(damagerID) || tm.getDragging().containsValue(damagerID)) {
                return;
            }

            // 2. ПРОВЕРКА ЦЕЛИ: Если цель в камере ИЛИ цель тащат охранником — освобождаем
            if (tm.getCapturedPrisoners().contains(victimID) || tm.getDragging().containsValue(victimID)) {
                tm.release(v);

                d.sendMessage("§a§l[!] §fТы освободил §e" + v.getName() + "§f!");
                v.sendMessage("§a§l[!] §e" + d.getName() + " §fспас тебя!");
            }
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { plugin.getTeamManager().handleQuit(e.getPlayer()); }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getGameManager().getGameState() == GameState.INGAME) {
            Location lobby = plugin.getConfigManager().getLoc("lobby");
            if (lobby != null) event.getPlayer().teleport(lobby);
        }
    }

    public void clearProgress() { repairProgress.clear(); currentRepairingBlock.clear(); }
}