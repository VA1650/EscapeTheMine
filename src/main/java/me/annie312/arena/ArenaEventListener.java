package me.annie312.arena;

import me.annie312.EscapeTheMine;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;

/**
 * Правила в мирах инстансов {@code run_*}: без урона в матче, без грифа (кроме etm.admin).
 */
public class ArenaEventListener implements Listener {

    private final EscapeTheMine plugin;
    private final ArenaManager arenaManager;

    public ArenaEventListener(EscapeTheMine plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    private static boolean isRunWorld(Player p) {
        return p.getWorld().getName().startsWith("run_");
    }

    /** На аренах полёт не нужен (кроме режима наблюдателя). */
    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player p = event.getPlayer();
        if (!isRunWorld(p)) return;
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        event.setCancelled(true);
        p.setFlying(false);
        p.setAllowFlight(false);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();
        if (!isRunWorld(p)) return;
        Arena arena = arenaManager.getArenaByPlayer(p);
        if (arena == null) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            Location safe = plugin.getConfigManager().getLocInWorld(arena.getWorld(), "spawn.prisoner");
            if (safe != null) {
                p.teleport(safe.clone().add(0, 4, 0));
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!isRunWorld(event.getPlayer())) return;
        if (!event.getPlayer().hasPermission("etm.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (!isRunWorld(event.getPlayer())) return;
        if (!event.getPlayer().hasPermission("etm.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (!isRunWorld(event.getPlayer())) return;
        if (!event.getPlayer().hasPermission("etm.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player p = (Player) event.getWhoClicked();
        if (!isRunWorld(p)) return;
        if (!p.hasPermission("etm.admin")) {
            if (event.getClick().toString().contains("DROP")
                    || event.getAction() == InventoryAction.DROP_ALL_SLOT
                    || event.getAction() == InventoryAction.DROP_ONE_SLOT
                    || event.getAction() == InventoryAction.DROP_ONE_CURSOR) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!isRunWorld(event.getPlayer())) return;
        if (!event.getPlayer().hasPermission("etm.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (event.getBlock().getWorld().getName().startsWith("run_")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().getWorld().getName().startsWith("run_")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        if (event.getEntity().getWorld().getName().startsWith("run_")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player p = (Player) event.getEntity();
            if (isRunWorld(p) && arenaManager.getArenaByPlayer(p) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getBlock().getWorld().getName().startsWith("run_")
                && event.getEntityType() == org.bukkit.entity.EntityType.FALLING_BLOCK) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        if (!isRunWorld(event.getPlayer())) return;
        if (!event.getPlayer().hasPermission("etm.admin")) {
            event.setCancelled(true);
        }
    }
}
