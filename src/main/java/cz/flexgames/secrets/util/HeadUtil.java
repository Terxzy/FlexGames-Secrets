package cz.flexgames.secrets.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import cz.flexgames.secrets.model.SecretHead;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility for building player-skull ItemStacks.
 * Supports Mojang username skins and Base64 texture values.
 */
public final class HeadUtil {

    // Invisible marker prefix embedded in the last lore line to identify our heads.
    // Format: \u00a70\u00a7r<headId>
    private static final String MARKER_PREFIX = "\u00a70\u00a7r";

    private HeadUtil() {}

    // ------------------------------------------------------------------
    // Build skull ItemStack
    // ------------------------------------------------------------------

    @SuppressWarnings("deprecation")
    public static ItemStack buildSkull(SecretHead head) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // Apply texture
        if (head.hasTexture()) {
            applyTexture(meta, head.getTexture());
        } else if (head.getSkin() != null && !head.getSkin().isEmpty()) {
            meta.setOwner(head.getSkin());
        }

        // Display name
        meta.setDisplayName(color(head.getDisplayName()));

        // Lore + hidden marker
        List<String> lore = new ArrayList<>();
        if (head.getLore() != null) {
            for (String line : head.getLore()) {
                lore.add(color(line));
            }
        }
        lore.add(MARKER_PREFIX + head.getId()); // hidden identifier (last line)
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    /** Builds skull with extra lore lines appended before the hidden marker. */
    @SuppressWarnings("deprecation")
    public static ItemStack buildSkullWithExtraLore(SecretHead head, List<String> extra) {
        ItemStack item = buildSkull(head);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        // Remove marker, insert extra, re-append marker
        String marker = lore.remove(lore.size() - 1);
        lore.addAll(extra);
        lore.add(marker);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // ------------------------------------------------------------------
    // Extract head ID from item
    // ------------------------------------------------------------------

    public static String extractId(ItemStack item) {
        if (item == null || item.getType() != Material.SKULL_ITEM) return null;
        if (!item.hasItemMeta()) return null;
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        List<String> lore = meta.getLore();
        if (lore == null || lore.isEmpty()) return null;
        String last = lore.get(lore.size() - 1);
        if (last.startsWith(MARKER_PREFIX)) {
            return last.substring(MARKER_PREFIX.length());
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Apply Base64 texture via GameProfile reflection
    // ------------------------------------------------------------------

    public static void applyTexture(SkullMeta meta, String base64) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "custom");
            profile.getProperties().put("textures", new Property("textures", base64));
            Field f = meta.getClass().getDeclaredField("profile");
            f.setAccessible(true);
            f.set(meta, profile);
        } catch (Exception e) {
            // Skull will appear as default Steve head
        }
    }

    // ------------------------------------------------------------------
    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}