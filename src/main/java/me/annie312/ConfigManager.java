package me.annie312;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final EscapeTheMine plugin;

    public ConfigManager(EscapeTheMine plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();
    }


    // Сохранение локации в config.yml
    public void saveLoc(String path, Location loc) {
        FileConfiguration config = plugin.getConfig();
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
        plugin.saveConfig();
    }

    // Получение локации из config.yml
    public Location getLoc(String path) {
        FileConfiguration config = plugin.getConfig();
        if (!config.contains(path)) return null;

        return new Location(
                Bukkit.getWorld(config.getString(path + ".world")),
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }
}