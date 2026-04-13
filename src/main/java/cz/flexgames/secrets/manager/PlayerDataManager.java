package cz.flexgames.secrets.manager;

import cz.flexgames.secrets.FlexSecrets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/** Persists per-player found-head data in playerdata.yml. */
public class PlayerDataManager {

    private final FlexSecrets plugin;
    private final File file;
    private FileConfiguration cfg;

    private final Map<UUID, Set<String>> data = new HashMap<>();

    public PlayerDataManager(FlexSecrets plugin) {
        this.plugin = plugin;
        this.file   = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    public void load() {
        if (!file.exists()) {
            try { file.createNewFile(); } catch (Exception e) {
                plugin.getLogger().severe("Cannot create playerdata.yml: " + e.getMessage());
            }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        data.clear();
        if (cfg.isConfigurationSection("players")) {
            for (String key : cfg.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    List<String> found = cfg.getStringList("players." + key + ".found");
                    data.put(uuid, new LinkedHashSet<>(found));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        for (Map.Entry<UUID, Set<String>> e : data.entrySet()) {
            cfg.set("players." + e.getKey() + ".found", new ArrayList<>(e.getValue()));
        }
        try { cfg.save(file); }
        catch (Exception e) { plugin.getLogger().severe("Cannot save playerdata.yml!"); }
    }

    // -----------------------------------------------------------------------

    public Set<String> getFound(UUID uuid) {
        return Collections.unmodifiableSet(data.getOrDefault(uuid, Collections.emptySet()));
    }

    public int getFoundCount(UUID uuid) {
        return data.getOrDefault(uuid, Collections.emptySet()).size();
    }

    public boolean hasFound(UUID uuid, String headId) {
        return data.getOrDefault(uuid, Collections.emptySet()).contains(headId);
    }

    /** @return true if this is a new find, false if already found. */
    public boolean markFound(UUID uuid, String headId) {
        Set<String> set = data.computeIfAbsent(uuid, k -> new LinkedHashSet<>());
        if (!set.add(headId)) return false;
        save();
        return true;
    }

    public void reset(UUID uuid) {
        data.remove(uuid);
        cfg.set("players." + uuid, null);
        save();
    }

    public int getPercent(UUID uuid, int total) {
        if (total <= 0) return 0;
        return (int) Math.min(100, Math.round(getFoundCount(uuid) * 100.0 / total));
    }
}