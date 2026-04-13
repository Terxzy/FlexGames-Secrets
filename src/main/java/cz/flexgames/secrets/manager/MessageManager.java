package cz.flexgames.secrets.manager;

import cz.flexgames.secrets.FlexSecrets;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loads messages.yml and provides formatted/placeholder-replaced access. */
public class MessageManager {

    private final FlexSecrets plugin;
    private FileConfiguration cfg;
    private String prefix;

    public MessageManager(FlexSecrets plugin) { this.plugin = plugin; }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        cfg = YamlConfiguration.loadConfiguration(file);
        try (InputStreamReader r = new InputStreamReader(
                plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(r));
        } catch (Exception ignored) {}
        prefix = col(cfg.getString("prefix", "&8[&6FlexSecrets&8]"));
    }

    // -----------------------------------------------------------------------
    // Raw + formatted
    // -----------------------------------------------------------------------

    public String getRaw(String path) {
        return cfg.getString(path, "&cMissing: " + path);
    }

    /** Returns a single-line message with placeholders replaced. */
    public String get(String path, Map<String, String> ph) {
        String s = col(getRaw(path)).replace("{prefix}", prefix);
        return applyPh(s, ph);
    }

    public String get(String path) { return get(path, null); }

    /** Returns a multi-line list message. */
    public List<String> getList(String path, Map<String, String> ph) {
        List<String> raw = cfg.getStringList(path);
        List<String> out = new ArrayList<>();
        for (String line : raw) {
            String s = col(line).replace("{prefix}", prefix);
            out.add(applyPh(s, ph));
        }
        return out;
    }

    // -----------------------------------------------------------------------
    // Send helpers
    // -----------------------------------------------------------------------

    public void send(Player p, String path, Map<String, String> ph) {
        String msg = get(path, ph);
        if (!msg.isEmpty()) p.sendMessage(msg);
    }

    public void sendList(Player p, String path, Map<String, String> ph) {
        for (String line : getList(path, ph)) p.sendMessage(line);
    }

    // -----------------------------------------------------------------------
    // Title / actionbar convenience (still formatted)
    // -----------------------------------------------------------------------

    public String getTitleFindTitle(Map<String, String> ph)      { return get("title.find-title", ph); }
    public String getTitleFindSubtitle(Map<String, String> ph)   { return get("title.find-subtitle", ph); }
    public String getTitleCompleteTitle(Map<String, String> ph)  { return get("title.complete-title", ph); }
    public String getTitleCompleteSub(Map<String, String> ph)    { return get("title.complete-subtitle", ph); }

    public int getTitleFadeIn()  { return cfg.getInt("title.fade-in", 10); }
    public int getTitleStay()    { return cfg.getInt("title.stay", 70); }
    public int getTitleFadeOut() { return cfg.getInt("title.fade-out", 20); }

    // -----------------------------------------------------------------------
    public String getPrefix() { return prefix; }
    public FileConfiguration getCfg() { return cfg; }

    // -----------------------------------------------------------------------
    private static String col(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String applyPh(String s, Map<String, String> ph) {
        if (ph == null || s == null) return s == null ? "" : s;
        for (Map.Entry<String, String> e : ph.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
            s = s.replace("%" + e.getKey() + "%", e.getValue());
        }
        return s;
    }
}