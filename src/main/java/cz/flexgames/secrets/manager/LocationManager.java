package cz.flexgames.secrets.manager;

import cz.flexgames.secrets.FlexSecrets;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps block locations to head IDs in locations.yml.
 * Key format: "world|x|y|z"
 */
public class LocationManager {

    private final FlexSecrets plugin;
    private final File file;
    private FileConfiguration cfg;

    private final Map<String, String> locations = new HashMap<>();

    public LocationManager(FlexSecrets plugin) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), "locations.yml");
    }

    public void load() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception e) {
                plugin.getLogger().severe("Cannot create locations.yml!");
            }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        locations.clear();
        if (cfg.isConfigurationSection("locations")) {
            for (String k : cfg.getConfigurationSection("locations").getKeys(false)) {
                locations.put(k, cfg.getString("locations." + k));
            }
        }
        plugin.getLogger().info("Loaded " + locations.size() + " registered skull locations.");
    }

    private void save() {
        cfg.set("locations", null);
        for (Map.Entry<String, String> e : locations.entrySet()) {
            cfg.set("locations." + e.getKey(), e.getValue());
        }
        try { cfg.save(file); }
        catch (Exception e) { plugin.getLogger().severe("Cannot save locations.yml!"); }
    }

    // -----------------------------------------------------------------------

    public static String key(Location loc) {
        return loc.getWorld().getName() + "|" + loc.getBlockX()
                + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    public boolean isRegistered(Location loc)    { return locations.containsKey(key(loc)); }
    public String getHeadId(Location loc)         { return locations.get(key(loc)); }

    public void register(Location loc, String id) {
        locations.put(key(loc), id);
        save();
    }

    public boolean unregister(Location loc) {
        boolean removed = locations.remove(key(loc)) != null;
        if (removed) save();
        return removed;
    }
}