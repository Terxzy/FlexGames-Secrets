package cz.flexgames.secrets.manager;

import cz.flexgames.secrets.FlexSecrets;
import cz.flexgames.secrets.model.SecretHead;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/** Loads and exposes all configuration values from config.yml. */
public class ConfigManager {

    private final FlexSecrets plugin;

    private int totalSecrets;
    private boolean particlesEnabled;
    private String particleType;
    private int particleCount;
    private double particleRadius;
    private boolean soundEnabled;
    private String soundFind;
    private float soundFindVolume, soundFindPitch;
    private String soundGuiOpen, soundGuiClick;
    private List<String> globalFindCommands;
    private List<String> globalCompleteCommands;

    private final Map<String, SecretHead> heads = new LinkedHashMap<>();

    public ConfigManager(FlexSecrets plugin) { this.plugin = plugin; }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        totalSecrets     = c.getInt("total-secrets", 25);
        particlesEnabled = c.getBoolean("particles.enabled", true);
        particleType     = c.getString("particles.type", "HEART").toUpperCase();
        particleCount    = c.getInt("particles.count", 25);
        particleRadius   = c.getDouble("particles.visible-radius", 20);
        soundEnabled     = c.getBoolean("sound.enabled", true);
        soundFind        = c.getString("sound.on-find", "LEVEL_UP");
        soundFindVolume  = (float) c.getDouble("sound.on-find-volume", 1.0);
        soundFindPitch   = (float) c.getDouble("sound.on-find-pitch", 1.0);
        soundGuiOpen     = c.getString("sound.on-gui-open", "ORB_PICKUP");
        soundGuiClick    = c.getString("sound.on-gui-click", "CLICK");
        globalFindCommands     = c.getStringList("global-rewards.on-find");
        globalCompleteCommands = c.getStringList("global-rewards.on-complete");

        heads.clear();
        ConfigurationSection sec = c.getConfigurationSection("heads");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection h = sec.getConfigurationSection(id);
                if (h == null) continue;
                String name    = h.getString("name", "&e" + id);
                String skin    = h.getString("skin", null);
                String texture = h.getString("texture", null);
                List<String> lore = h.getStringList("lore");
                List<String> cmds = h.getStringList("commands");
                heads.put(id, new SecretHead(id, name, skin, texture, lore, cmds));
            }
        }
        plugin.getLogger().info("Loaded " + heads.size() + " heads (total declared: " + totalSecrets + ")");
    }

    // Getters
    public int getTotalSecrets()           { return totalSecrets; }
    public boolean isParticlesEnabled()    { return particlesEnabled; }
    public String getParticleType()        { return particleType; }
    public int getParticleCount()          { return particleCount; }
    public double getParticleRadius()      { return particleRadius; }
    public boolean isSoundEnabled()        { return soundEnabled; }
    public String getSoundFind()           { return soundFind; }
    public float getSoundFindVolume()      { return soundFindVolume; }
    public float getSoundFindPitch()       { return soundFindPitch; }
    public String getSoundGuiOpen()        { return soundGuiOpen; }
    public String getSoundGuiClick()       { return soundGuiClick; }
    public List<String> getGlobalFindCommands()     { return globalFindCommands; }
    public List<String> getGlobalCompleteCommands() { return globalCompleteCommands; }

    public SecretHead getHead(String id)             { return heads.get(id); }
    public boolean hasHead(String id)                { return heads.containsKey(id); }
    public List<SecretHead> getAllHeads()             { return new ArrayList<>(heads.values()); }
    public Map<String, SecretHead> getHeadsMap()     { return Collections.unmodifiableMap(heads); }
}