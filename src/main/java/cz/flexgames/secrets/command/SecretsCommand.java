package cz.flexgames.secrets.command;

import cz.flexgames.secrets.FlexSecrets;
import cz.flexgames.secrets.model.SecretHead;
import cz.flexgames.secrets.util.HeadUtil;
import cz.flexgames.secrets.util.NMSUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class SecretsCommand implements CommandExecutor {

    private final FlexSecrets plugin;

    public SecretsCommand(FlexSecrets plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // /secret  -> admin GUI
        if (args.length == 0) {
            if (!requirePlayer(sender)) return true;
            Player p = (Player) sender;
            if (!p.hasPermission("flexsecrets.admin")) {
                plugin.getMsgManager().send(p, "cmd.no-permission", null);
                return true;
            }
            plugin.getSecretsGUI().open(p);
            return true;
        }

        switch (args[0].toLowerCase()) {

            /* ----------------------------------------------------------------
             * /secret give <jmenoHrace|headId>
             *   - pokud headId existuje v configu -> dat basehead item
             *   - jinak -> dat player skull se skinem daneho hrace (Mojang)
             * ---------------------------------------------------------------- */
            case "give": {
                if (!requireAdmin(sender)) return true;
                if (args.length < 2) {
                    sender.sendMessage(col("&cPouziti: /secret give <jmenoHrace|headId>"));
                    return true;
                }
                String target = args[1];

                // Zjisti ciloveho hrace (kdo dostane item) - nepovinny 3. argument
                Player receiver = args.length >= 3
                        ? Bukkit.getPlayer(args[2])
                        : (sender instanceof Player ? (Player) sender : null);

                if (receiver == null) {
                    sendK(sender, "cmd.player-not-found", "player", args.length >= 3 ? args[2] : "?");
                    return true;
                }

                // Zkus najit basehead v configu
                SecretHead head = plugin.getCfgManager().getHead(target);

                if (head != null) {
                    // -- Basehead z configu --
                    receiver.getInventory().addItem(HeadUtil.buildSkull(head));
                    Map<String, String> ph = twoK("player", receiver.getName(),
                            "head_name", col(head.getDisplayName()));
                    if (!receiver.equals(sender) && sender instanceof Player)
                        plugin.getMsgManager().send((Player) sender, "cmd.give-admin", ph);
                    plugin.getMsgManager().send(receiver, "cmd.give-success", ph);
                } else {
                    // -- Player skull podle Mojang usernamu --
                    @SuppressWarnings("deprecation")
                    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    meta.setOwner(target);
                    meta.setDisplayName(ChatColor.YELLOW + target + "'s Head");
                    skull.setItemMeta(meta);
                    receiver.getInventory().addItem(skull);

                    Map<String, String> ph = twoK("player", receiver.getName(),
                            "head_name", target + "'s Head");
                    if (!receiver.equals(sender) && sender instanceof Player)
                        plugin.getMsgManager().send((Player) sender, "cmd.give-admin", ph);
                    plugin.getMsgManager().send(receiver, "cmd.give-success", ph);
                }
                if (plugin.getCfgManager().isSoundEnabled())
                    NMSUtil.playSound(receiver, plugin.getCfgManager().getSoundGuiClick(), 1f, 1f);
                return true;
            }

            /* ----------------------------------------------------------------
             * /secret place <id>  - zaregistruje lebku, na kterou se divas
             * ---------------------------------------------------------------- */
            case "place": {
                if (!requireAdmin(sender)) return true;
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { sender.sendMessage(col("&cPouziti: /secret place <id>")); return true; }
                Player p = (Player) sender;
                if (!plugin.getCfgManager().hasHead(args[1])) {
                    sendK(p, "cmd.head-not-found", "id", args[1]); return true;
                }
                @SuppressWarnings("deprecation")
                Block target = p.getTargetBlock((HashSet<Byte>) null, 5);
                if (target == null || target.getType() != Material.SKULL) {
                    plugin.getMsgManager().send(p, "cmd.no-skull-nearby", null); return true;
                }
                plugin.getLocManager().register(target.getLocation(), args[1]);
                sendK(p, "cmd.place-success", "id", args[1]);
                return true;
            }

            /* ----------------------------------------------------------------
             * /secret remove  - odregistruje lebku, na kterou se divas
             * ---------------------------------------------------------------- */
            case "remove": {
                if (!requireAdmin(sender)) return true;
                if (!requirePlayer(sender)) return true;
                Player p = (Player) sender;
                @SuppressWarnings("deprecation")
                Block target = p.getTargetBlock((HashSet<Byte>) null, 5);
                if (target == null || target.getType() != Material.SKULL) {
                    plugin.getMsgManager().send(p, "cmd.no-skull-nearby", null); return true;
                }
                boolean removed = plugin.getLocManager().unregister(target.getLocation());
                if (removed) plugin.getMsgManager().send(p, "cmd.remove-success", null);
                else plugin.getMsgManager().send(p, "cmd.remove-not-found", null);
                return true;
            }

            /* ----------------------------------------------------------------
             * /secret reset <hrac>
             * ---------------------------------------------------------------- */
            case "reset": {
                if (!requireAdmin(sender)) return true;
                if (args.length < 2) { sender.sendMessage(col("&cPouziti: /secret reset <hrac>")); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sendK(sender, "cmd.player-not-found", "player", args[1]); return true; }
                plugin.getPdManager().reset(target.getUniqueId());
                sendK(sender, "cmd.reset-success", "player", target.getName());
                return true;
            }

            /* ----------------------------------------------------------------
             * /secret reload
             * ---------------------------------------------------------------- */
            case "reload": {
                if (!requireAdmin(sender)) return true;
                plugin.getCfgManager().load();
                plugin.getMsgManager().load();
                plugin.getLocManager().load();
                plugin.getPdManager().load();
                if (sender instanceof Player)
                    plugin.getMsgManager().send((Player) sender, "cmd.reload-success", null);
                else sender.sendMessage(col("&aReloaded!"));
                return true;
            }

            /* ----------------------------------------------------------------
             * /secret help
             * ---------------------------------------------------------------- */
            default: {
                if (sender instanceof Player)
                    plugin.getMsgManager().sendList((Player) sender, "cmd.help", null);
                else sender.sendMessage("FlexGames-Secrets | /secret [give|place|remove|reset|reload]");
                return true;
            }
        }
    }

    // ------------------------------------------------------------------

    private boolean requirePlayer(CommandSender s) {
        if (s instanceof Player) return true;
        s.sendMessage(col(plugin.getMsgManager().get("cmd.player-only")));
        return false;
    }

    private boolean requireAdmin(CommandSender s) {
        if (s.hasPermission("flexsecrets.admin")) return true;
        if (s instanceof Player) plugin.getMsgManager().send((Player) s, "cmd.no-permission", null);
        else s.sendMessage(col("&cNo permission."));
        return false;
    }

    private void sendK(CommandSender s, String path, String k, String v) {
        Map<String, String> ph = new HashMap<>();
        ph.put(k, v);
        if (s instanceof Player) plugin.getMsgManager().send((Player) s, path, ph);
        else s.sendMessage(plugin.getMsgManager().get(path, ph));
    }

    private Map<String, String> twoK(String k1, String v1, String k2, String v2) {
        Map<String, String> m = new HashMap<>();
        m.put(k1, v1); m.put(k2, v2);
        return m;
    }

    private String col(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}