package me.annie312;

import lombok.Getter;
import me.annie312.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameListener implements Listener {
    private final EscapeTheMine plugin;
    @Getter
    private final Map<UUID, Integer> repairProgress = new HashMap<>();
    private final Map<UUID, Location> currentRepairingBlock = new HashMap<>();

    public GameListener(EscapeTheMine plugin) {
        this.plugin = plugin;
    }

    private Arena getActiveMatch(Player p) {
        Arena a = plugin.getArenaManager().getArenaByPlayer(p);
        if (a == null || a.getGameState() != GameState.INGAME) return null;
        return a;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        UUID uuid = p.getUniqueId();
        Arena arena = getActiveMatch(p);
        if (arena == null) return;

        if (hasPlayerMoved(event)) {
            handleRepairInterruption(p, uuid);
            handleFrozenMovement(event, arena, uuid);
        }
        handleDragging(arena, uuid, p);
    }

    private boolean hasPlayerMoved(PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
    }

    private void handleRepairInterruption(Player p, UUID uuid) {
        if (repairProgress.containsKey(uuid)) {
            repairProgress.remove(uuid);
            currentRepairingBlock.remove(uuid);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§cПочинка прервана!"));
        }
    }

    private void handleFrozenMovement(PlayerMoveEvent event, Arena arena, UUID uuid) {
        if (arena.getFrozenPlayers().contains(uuid) && !arena.getDragging().containsValue(uuid)) {
            event.setTo(event.getFrom());
        }
    }

    private void handleDragging(Arena arena, UUID uuid, Player p) {
        if (!arena.getDragging().containsKey(uuid)) return;
        Player victim = Bukkit.getPlayer(arena.getDragging().get(uuid));
        if (victim == null) return;
        victim.teleport(p.getLocation());
        Location cell = plugin.getConfigManager().getLocInWorld(arena.getWorld(), "cell");
        if (cell != null && p.getLocation().distance(cell) < 2.5) {
            UUID vId = victim.getUniqueId();
            arena.getDragging().remove(uuid);
            arena.getCapturedPrisoners().add(vId);
            victim.teleport(cell);
            p.sendMessage("§a§l[!] §fПосажен!");
            arena.checkWinConditions();
        }
    }

    @EventHandler
    public void onRepair(PlayerInteractEvent event) {
        if (!isValidRepairAction(event)) return;
        Player p = event.getPlayer();
        Arena arena = getActiveMatch(p);
        if (arena == null) return;
        UUID uuid = p.getUniqueId();

        if (!canPlayerRepair(p, uuid, arena)) return;

        event.setCancelled(true);
        Location loc = event.getClickedBlock().getLocation();
        String timeStr = Arena.formatTime(arena.getTimeLeftSeconds());

        if (!isInRepairRange(p, loc, timeStr)) return;

        handleRepairProgress(p, uuid, loc, timeStr, arena);
    }

    private boolean isValidRepairAction(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.getClickedBlock().getType() == Material.WORKBENCH;
    }

    private boolean canPlayerRepair(Player p, UUID uuid, Arena arena) {
        if (!arena.getPrisoners().contains(uuid)) return false;
        if (arena.getCapturedPrisoners().contains(uuid) || arena.getDragging().containsValue(uuid)) {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§cНельзя чинить, пока пойман!"));
            return false;
        }
        return true;
    }

    private boolean isInRepairRange(Player p, Location loc, String timeStr) {
        if (p.getLocation().distance(loc) > 3.5) {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§7[" + timeStr + "] §c§l! СЛИШКОМ ДАЛЕКО !"));
            return false;
        }
        return true;
    }

    private void handleRepairProgress(Player p, UUID uuid, Location loc, String timeStr, Arena arena) {
        if (currentRepairingBlock.containsKey(uuid) && currentRepairingBlock.get(uuid).distance(loc) > 0.5) {
            repairProgress.put(uuid, 0);
        }
        currentRepairingBlock.put(uuid, loc);

        int max = 50;
        int progress = repairProgress.getOrDefault(uuid, 0) + 1;

        if (progress >= max) {
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§7[" + timeStr + "] " + getProgressBar(50, 50)));
            repairProgress.remove(uuid);
            currentRepairingBlock.remove(uuid);
            loc.getBlock().setType(Material.AIR);
            arena.incrementRepaired(p);
        } else {
            repairProgress.put(uuid, progress);
            String bar = getProgressBar(progress, max);
            p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent("§7[" + timeStr + "] " + bar));
        }
    }

    private String getProgressBar(int current, int max) {
        int totalBars = 20;
        int completed = (int) Math.ceil((double) current / max * totalBars);
        StringBuilder sb = new StringBuilder("§eЧиним: §a");
        for (int i = 0; i < totalBars; i++) {
            if (i == completed) sb.append("§7");
            sb.append("■");
        }
        return sb.toString();
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        if (getActiveMatch(p) != null) {
            event.setFormat(p.getDisplayName() + "§7: §f" + event.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player)) return;
        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();

        if (plugin.getArenaManager().isLobbyWorld(damager.getWorld())
                && plugin.getArenaManager().isLobbyWorld(victim.getWorld())) {
            event.setCancelled(true);
            return;
        }

        Arena da = getActiveMatch(damager);
        Arena va = getActiveMatch(victim);

        if (va != null && da == va) {
            event.setCancelled(true);
            if (isGuardCatchingPrisoner(da, damager, victim)) {
                handleGuardCatch(da, damager, victim);
            } else if (isPrisonerRescuing(da, damager, victim)) {
                handlePrisonerRescue(da, damager, victim);
            }
            return;
        }

        if (damager.getWorld().getName().startsWith("run_") || victim.getWorld().getName().startsWith("run_")) {
            event.setCancelled(true);
        }
    }

    private boolean isGuardCatchingPrisoner(Arena arena, Player damager, Player victim) {
        return arena.getGuards().contains(damager.getUniqueId())
                && arena.getPrisoners().contains(victim.getUniqueId());
    }

    private boolean isPrisonerRescuing(Arena arena, Player damager, Player victim) {
        return arena.getPrisoners().contains(damager.getUniqueId())
                && arena.getPrisoners().contains(victim.getUniqueId());
    }

    private void handleGuardCatch(Arena arena, Player guard, Player prisoner) {
        if (guard.getInventory().getItemInMainHand().getType() != Material.IRON_SWORD) return;
        UUID prisonerId = prisoner.getUniqueId();
        if (!arena.getCapturedPrisoners().contains(prisonerId) && !arena.getDragging().containsValue(prisonerId)) {
            arena.startDragging(guard, prisoner);
        }
    }

    private void handlePrisonerRescue(Arena arena, Player rescuer, Player victim) {
        UUID rescuerId = rescuer.getUniqueId();
        UUID victimId = victim.getUniqueId();
        if (arena.getCapturedPrisoners().contains(rescuerId) || arena.getDragging().containsValue(rescuerId)) {
            return;
        }
        if (arena.getCapturedPrisoners().contains(victimId) || arena.getDragging().containsValue(victimId)) {
            arena.release(victim);
            rescuer.sendMessage("§a§l[!] §fТы освободил §e" + victim.getName() + "§f!");
            victim.sendMessage("§a§l[!] §e" + rescuer.getName() + " §fспас тебя!");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getArenaManager().handlePlayerQuit(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> applyLobbyOnJoin(p), 1L);
    }

    private void applyLobbyOnJoin(Player p) {
        if (!p.isOnline()) return;
        plugin.getArenaManager().handlePlayerQuit(p);
        p.getInventory().clear();
        for (org.bukkit.potion.PotionEffect ef : p.getActivePotionEffects()) {
            p.removePotionEffect(ef.getType());
        }
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(true);
        p.setFlying(false);
        Location lobby = plugin.getArenaManager().resolveLobbySpawn();
        if (lobby != null) {
            p.teleport(lobby);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        Arena a = plugin.getArenaManager().getArenaByPlayer(p);
        if (a != null) {
            a.removePlayer(p);
            a.checkWinConditions();
        }
        event.getDrops().clear();
        event.setKeepInventory(false);
        event.setKeepLevel(false);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Location lobby = plugin.getArenaManager().resolveLobbySpawn();
        if (lobby != null) {
            event.setRespawnLocation(lobby);
        }
        Player p = event.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setAllowFlight(true);
            p.setFlying(false);
            for (org.bukkit.potion.PotionEffect ef : p.getActivePotionEffects()) {
                p.removePotionEffect(ef.getType());
            }
        });
    }

    @EventHandler
    public void onCreatureSpawnInLobby(CreatureSpawnEvent event) {
        if (plugin.getArenaManager().isLobbyWorld(event.getLocation().getWorld())) {
            event.setCancelled(true);
        }
    }

    public void clearProgress() {
        repairProgress.clear();
        currentRepairingBlock.clear();
    }
}
