package cz.flexgames.secrets;

import cz.flexgames.secrets.command.SecretsCommand;
import cz.flexgames.secrets.gui.SecretsGUI;
import cz.flexgames.secrets.listener.SecretsListener;
import cz.flexgames.secrets.manager.ConfigManager;
import cz.flexgames.secrets.manager.LocationManager;
import cz.flexgames.secrets.manager.MessageManager;
import cz.flexgames.secrets.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class FlexSecrets extends JavaPlugin {

    private ConfigManager    cfgManager;
    private MessageManager   msgManager;
    private PlayerDataManager pdManager;
    private LocationManager  locManager;
    private SecretsGUI       secretsGUI;

    @Override
    public void onEnable() {
        cfgManager = new ConfigManager(this);
        msgManager = new MessageManager(this);
        pdManager  = new PlayerDataManager(this);
        locManager = new LocationManager(this);

        cfgManager.load();
        msgManager.load();
        pdManager.load();
        locManager.load();

        secretsGUI = new SecretsGUI(this);

        Bukkit.getPluginManager().registerEvents(new SecretsListener(this), this);
        getCommand("secret").setExecutor(new SecretsCommand(this));

        getLogger().info("");
        getLogger().info("  \u00a76\u00a7l\u2726 FlexGames-Secrets v" + getDescription().getVersion() + " enabled!");
        getLogger().info("  Heads loaded : " + cfgManager.getAllHeads().size());
        getLogger().info("  Total secrets: " + cfgManager.getTotalSecrets());
        getLogger().info("");
    }

    @Override
    public void onDisable() {
        if (pdManager != null) pdManager.save();
        getLogger().info("\u00a76\u00a7l\u2726 FlexGames-Secrets disabled.");
    }

    public ConfigManager     getCfgManager()  { return cfgManager; }
    public MessageManager    getMsgManager()  { return msgManager; }
    public PlayerDataManager getPdManager()   { return pdManager; }
    public LocationManager   getLocManager()  { return locManager; }
    public SecretsGUI        getSecretsGUI()  { return secretsGUI; }
}