package me.annie312;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class EscapeTheMine extends JavaPlugin {

    private ConfigManager configManager;
    private TeamManager teamManager;
    private LobbyManager lobbyManager;
    private GameManager gameManager;
    private GameListener gameListener;

    @Override
    public void onEnable() {
        // 1. Сначала конфиг
        this.configManager = new ConfigManager(this);
        configManager.loadConfig();

        // 2. Инициализируем менеджеры
        this.teamManager = new TeamManager(this);
        this.gameManager = new GameManager(this);
        this.lobbyManager = new LobbyManager(this);
        this.gameListener = new GameListener(this);

        // 3. Регистрируем события
        getServer().getPluginManager().registerEvents(gameListener, this);
        getServer().getPluginManager().registerEvents(new EventCanceller(), this);

        // 4. Регистрируем команды
        CommandHandler handler = new CommandHandler(this);
        getCommand("etm").setExecutor(handler);
        getCommand("lobby").setExecutor(handler);

        getLogger().info("EscapeTheMine включен!");
    }

}