package me.annie312;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class ConfigManager {
    private final EscapeTheMine plugin;

    public ConfigManager(EscapeTheMine plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }

    public void saveLoc(String path, Location loc) {
        plugin.getConfig().set(path + ".world", loc.getWorld().getName());
        plugin.getConfig().set(path + ".x", loc.getX());
        plugin.getConfig().set(path + ".y", loc.getY());
        plugin.getConfig().set(path + ".z", loc.getZ());
        plugin.getConfig().set(path + ".yaw", loc.getYaw());
        plugin.getConfig().set(path + ".pitch", loc.getPitch());
        plugin.saveConfig();
    }

    public Location getLoc(String path) {
        if (!plugin.getConfig().contains(path)) return null;

        return new Location(
                Bukkit.getWorld(plugin.getConfig().getString(path + ".world")),
                plugin.getConfig().getDouble(path + ".x"),
                plugin.getConfig().getDouble(path + ".y"),
                plugin.getConfig().getDouble(path + ".z"),
                (float) plugin.getConfig().getDouble(path + ".yaw"),
                (float) plugin.getConfig().getDouble(path + ".pitch")
        );
    }

    /** Те же координаты, что в конфиге ETM, но в мире инстанса арены (world из шаблона в конфиге игнорируется). */
    public Location getLocInWorld(World arenaWorld, String path) {
        if (arenaWorld == null || !plugin.getConfig().contains(path + ".x")) return null;
        return new Location(
                arenaWorld,
                plugin.getConfig().getDouble(path + ".x"),
                plugin.getConfig().getDouble(path + ".y"),
                plugin.getConfig().getDouble(path + ".z"),
                (float) plugin.getConfig().getDouble(path + ".yaw"),
                (float) plugin.getConfig().getDouble(path + ".pitch")
        );
    }
}