package me.annie312;


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


public class EventCanceller implements Listener {

    @EventHandler
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        event.setCancelled(true);
    }


    @EventHandler
    public void onLeavesDecay(@NotNull LeavesDecayEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(@NotNull BlockFadeEvent event) { event.setCancelled(true); }

    @EventHandler
    public void onItemSpawn(@NotNull ItemSpawnEvent event) { event.setCancelled(true); }

    @EventHandler
    public void onFoodLevelChange(@NotNull FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityChangeBlock(org.bukkit.event.entity.@NotNull EntityChangeBlockEvent event) {
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
        event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(org.bukkit.event.player.@NotNull PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

}
