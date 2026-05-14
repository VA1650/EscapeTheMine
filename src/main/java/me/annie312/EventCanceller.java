package me.annie312;


import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.jetbrains.annotations.NotNull;


/**
 * Защита лобби и основного мира. Миры инстансов арен ({@code run_*}) обрабатывает {@link me.annie312.arena.ArenaEventListener}.
 */
public class EventCanceller implements Listener {

    private static boolean isArenaRuntimeWorld(World world) {
        return world != null && world.getName().startsWith("run_");
    }

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        if (isArenaRuntimeWorld(event.getPlayer().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        if (isArenaRuntimeWorld(event.getPlayer().getWorld())) return;
        event.setCancelled(true);
    }


    @EventHandler
    public void onLeavesDecay(@NotNull LeavesDecayEvent event) {
        if (isArenaRuntimeWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(@NotNull BlockFadeEvent event) {
        if (isArenaRuntimeWorld(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(@NotNull ItemSpawnEvent event) {
        if (isArenaRuntimeWorld(event.getEntity().getWorld())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(@NotNull FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player && isArenaRuntimeWorld(((Player) event.getEntity()).getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityChangeBlock(org.bukkit.event.entity.@NotNull EntityChangeBlockEvent event) {
        if (isArenaRuntimeWorld(event.getBlock().getWorld())) return;
        if (event.getEntityType() == org.bukkit.entity.EntityType.FALLING_BLOCK) {
            event.setCancelled(true);
            event.getBlock().getState().update(true, false);
        }
    }

    @EventHandler
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) {
            return;
        }
        if (event.getEntity() instanceof Player && isArenaRuntimeWorld(((Player) event.getEntity()).getWorld())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(org.bukkit.event.player.@NotNull PlayerPickupItemEvent event) {
        if (isArenaRuntimeWorld(event.getPlayer().getWorld())) return;
        event.setCancelled(true);
    }

}
