package cz.flexgames.secrets.model;

import java.util.List;

/** Represents a single configurable secret head. */
public class SecretHead {

    private final String id;
    private final String displayName;   // raw & color-coded
    private final String skin;          // Mojang username (nullable)
    private final String texture;       // Base64 value (nullable, takes priority over skin)
    private final List<String> lore;
    private final List<String> commands;

    public SecretHead(String id, String displayName, String skin, String texture,
                      List<String> lore, List<String> commands) {
        this.id = id;
        this.displayName = displayName;
        this.skin = skin;
        this.texture = texture;
        this.lore = lore;
        this.commands = commands;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getSkin()        { return skin; }
    public String getTexture()     { return texture; }
    public List<String> getLore()  { return lore; }
    public List<String> getCommands() { return commands; }

    /** True when a base64 texture is configured (preferred over skin). */
    public boolean hasTexture() {
        return texture != null && !texture.isEmpty();
    }
}