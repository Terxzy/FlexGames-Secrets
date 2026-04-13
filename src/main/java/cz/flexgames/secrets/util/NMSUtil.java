package cz.flexgames.secrets.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Reflection-based NMS helper for Spigot 1.8.8.
 * Handles Titles, Action Bars, and Particles without direct NMS imports.
 */
public final class NMSUtil {

    private NMSUtil() {}

    private static String version;

    private static String ver() {
        if (version == null) {
            String pkg = Bukkit.getServer().getClass().getPackage().getName();
            version = pkg.substring(pkg.lastIndexOf('.') + 1);
        }
        return version;
    }

    private static Class<?> nms(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + ver() + "." + name);
    }

    // -----------------------------------------------------------------------
    // Internal: send NMS packet to player
    // -----------------------------------------------------------------------
    private static void sendPacket(Player player, Object packet) throws Exception {
        Object handle = player.getClass().getMethod("getHandle").invoke(player);
        Field connField = handle.getClass().getField("playerConnection");
        Object conn = connField.get(handle);
        Method send = conn.getClass().getMethod("sendPacket", nms("Packet"));
        send.invoke(conn, packet);
    }

    // -----------------------------------------------------------------------
    // Internal: wrap plain text in NMS IChatBaseComponent
    // -----------------------------------------------------------------------
    private static Object chatComponent(String text) throws Exception {
        String json = "{\"text\":\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        Class<?> serializer = nms("IChatBaseComponent$ChatSerializer");
        return serializer.getMethod("a", String.class).invoke(null, json);
    }

    // -----------------------------------------------------------------------
    // Title
    // -----------------------------------------------------------------------
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void sendTitle(Player player, String title, String subtitle,
                                  int fadeIn, int stay, int fadeOut) {
        try {
            Class<?> pktClass   = nms("PacketPlayOutTitle");
            Class<Enum> action  = (Class<Enum>) nms("PacketPlayOutTitle$EnumTitleAction");
            Class<?> chatComp   = nms("IChatBaseComponent");

            // TIMES
            Constructor<?> timesCtor = pktClass.getConstructor(action, chatComp, int.class, int.class, int.class);
            Object timesPacket = timesCtor.newInstance(
                    Enum.valueOf(action, "TIMES"), null, fadeIn, stay, fadeOut);
            sendPacket(player, timesPacket);

            // SUBTITLE
            if (subtitle != null && !subtitle.isEmpty()) {
                Constructor<?> ctor = pktClass.getConstructor(action, chatComp);
                Object pkt = ctor.newInstance(Enum.valueOf(action, "SUBTITLE"), chatComponent(subtitle));
                sendPacket(player, pkt);
            }

            // TITLE
            Constructor<?> ctor = pktClass.getConstructor(action, chatComp);
            Object pkt = ctor.newInstance(Enum.valueOf(action, "TITLE"), chatComponent(title));
            sendPacket(player, pkt);

        } catch (Exception e) {
            player.sendMessage(title);
        }
    }

    // -----------------------------------------------------------------------
    // Action Bar
    // -----------------------------------------------------------------------
    public static void sendActionBar(Player player, String message) {
        try {
            Class<?> pkt  = nms("PacketPlayOutChat");
            Class<?> comp = nms("IChatBaseComponent");
            Constructor<?> ctor = pkt.getConstructor(comp, byte.class);
            Object packet = ctor.newInstance(chatComponent(message), (byte) 2);
            sendPacket(player, packet);
        } catch (Exception e) {
            // silent fail
        }
    }

    // -----------------------------------------------------------------------
    // Particles
    // -----------------------------------------------------------------------
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void spawnParticles(Location loc, String particleName, int count, double visibleRadius) {
        try {
            Class<Enum> enumPart = (Class<Enum>) nms("EnumParticle");

            Enum<?> heart;
            try { heart = Enum.valueOf(enumPart, particleName); }
            catch (IllegalArgumentException ex) { heart = Enum.valueOf(enumPart, "HEART"); }

            Enum<?> spark;
            try { spark = Enum.valueOf(enumPart, "FIREWORKS_SPARK"); }
            catch (IllegalArgumentException ex) { spark = heart; }

            Class<?> pktClass = nms("PacketPlayOutWorldParticles");
            Constructor<?> ctor = pktClass.getConstructor(
                    enumPart, boolean.class,
                    float.class, float.class, float.class,
                    float.class, float.class, float.class,
                    float.class, int.class, int[].class);

            Object heartPkt = ctor.newInstance(heart, true,
                    (float)(loc.getX() + 0.5), (float)(loc.getY() + 1.1), (float)(loc.getZ() + 0.5),
                    0.3f, 0.3f, 0.3f, 0f, count, new int[0]);

            Object sparkPkt = ctor.newInstance(spark, true,
                    (float)(loc.getX() + 0.5), (float)(loc.getY() + 0.5), (float)(loc.getZ() + 0.5),
                    0.4f, 0.5f, 0.4f, 0.05f, count * 2, new int[0]);

            Class<?> pktInterface = nms("Packet");
            double radiusSq = visibleRadius * visibleRadius;

            for (Player p : loc.getWorld().getPlayers()) {
                if (p.getLocation().distanceSquared(loc) <= radiusSq) {
                    Object handle = p.getClass().getMethod("getHandle").invoke(p);
                    Object conn = handle.getClass().getField("playerConnection").get(handle);
                    Method send = conn.getClass().getMethod("sendPacket", pktInterface);
                    send.invoke(conn, heartPkt);
                    send.invoke(conn, sparkPkt);
                }
            }
        } catch (Exception e) {
            // Particles won't show if NMS reflection fails (version mismatch, etc.)
        }
    }

    // -----------------------------------------------------------------------
    // Sound (Bukkit API - safe across versions)
    // -----------------------------------------------------------------------
    public static void playSound(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            try { player.playSound(player.getLocation(), Sound.LEVEL_UP, volume, pitch); }
            catch (Exception ignored) {}
        }
    }
}