package cz.flexgames.secrets.listener;

import cz.flexgames.secrets.FlexSecrets;
import cz.flexgames.secrets.model.SecretHead;
import cz.flexgames.secrets.util.HeadUtil;
import cz.flexgames.secrets.util.NMSUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SecretsListener implements Listener {

    private final FlexSecrets plugin;

    public SecretsListener(FlexSecrets plugin) { this.plugin = plugin; }

    // ------------------------------------------------------------------
    // Auto-register when admin places a tagged secret skull
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("flexsecrets.admin")) return;

        ItemStack hand = event.getItemInHand();
        String id = HeadUtil.extractId(hand);
        if (id == null) return;

        plugin.getLocManager().register(event.getBlockPlaced().getLocation(), id);
        SecretHead head = plugin.getCfgManager().getHead(id);
        String name = head != null ? ChatColor.translateAlternateColorCodes('&', head.getDisplayName()) : id;
        player.sendMessage(plugin.getMsgManager().getPrefix()
                + ChatColor.GREEN + " Secret " + ChatColor.YELLOW + name
                + ChatColor.GREEN + " auto-registered at this location.");
    }

    // ------------------------------------------------------------------
    // Unregister when a registered skull is broken
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block b = event.getBlock();
        if (b.getType() != Material.SKULL) return;
        plugin.getLocManager().unregister(b.getLocation());
    }

    // ------------------------------------------------------------------
    // Player right-clicks a registered skull
    // ------------------------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.SKULL) return;
        if (!plugin.getLocManager().isRegistered(block.getLocation())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        if (!player.hasPermission("flexsecrets.use")) {
            plugin.getMsgManager().send(player, "cmd.no-permission", null);
            return;
        }

        String headId = plugin.getLocManager().getHeadId(block.getLocation());
        SecretHead head = plugin.getCfgManager().getHead(headId);
        if (head == null) return; // stale/removed from config

        UUID uuid = player.getUniqueId();
        int total = plugin.getCfgManager().getTotalSecrets();
        boolean alreadyFound = plugin.getPdManager().hasFound(uuid, headId);

        if (alreadyFound) {
            int fc = plugin.getPdManager().getFoundCount(uuid);
            Map<String, String> ph = ph(player, fc, total, Math.max(0, total - fc), head);
            plugin.getMsgManager().send(player, "found.already-found", ph);
            return;
        }

        // Mark as found
        plugin.getPdManager().markFound(uuid, headId);
        int foundNow  = plugin.getPdManager().getFoundCount(uuid);
        int remaining = Math.max(0, total - foundNow);
        int percent   = plugin.getPdManager().getPercent(uuid, total);
        boolean complete = foundNow >= total;

        Map<String, String> ph = ph(player, foundNow, total, remaining, head);
        ph.put("percent", String.valueOf(percent));

        // -- Chat --
        if (foundNow == 1) {
            plugin.getMsgManager().sendList(player, "found.first", ph);
        } else {
            plugin.getMsgManager().sendList(player, "found.normal", ph);
        }
        if (complete) {
            plugin.getMsgManager().sendList(player, "found.complete", ph);
            String broadcast = plugin.getMsgManager().get("found.complete-broadcast", ph);
            if (!broadcast.isEmpty()) {
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    online.sendMessage(broadcast);
                }
            }
        }

        // -- Title --
        String titleText    = complete ? plugin.getMsgManager().getTitleCompleteTitle(ph)  : plugin.getMsgManager().getTitleFindTitle(ph);
        String subtitleText = complete ? plugin.getMsgManager().getTitleCompleteSub(ph)    : plugin.getMsgManager().getTitleFindSubtitle(ph);
        NMSUtil.sendTitle(player, titleText, subtitleText,
                plugin.getMsgManager().getTitleFadeIn(),
                plugin.getMsgManager().getTitleStay(),
                plugin.getMsgManager().getTitleFadeOut());

        // -- Action Bar --
        String bar = complete
                ? plugin.getMsgManager().get("actionbar.complete", ph)
                : plugin.getMsgManager().get("actionbar.find", ph);
        if (!bar.isEmpty()) NMSUtil.sendActionBar(player, bar);

        // -- Sound --
        if (plugin.getCfgManager().isSoundEnabled()) {
            NMSUtil.playSound(player,
                    plugin.getCfgManager().getSoundFind(),
                    plugin.getCfgManager().getSoundFindVolume(),
                    plugin.getCfgManager().getSoundFindPitch());
        }

        // -- Particles --
        if (plugin.getCfgManager().isParticlesEnabled()) {
            NMSUtil.spawnParticles(
                    block.getLocation(),
                    plugin.getCfgManager().getParticleType(),
                    plugin.getCfgManager().getParticleCount(),
                    plugin.getCfgManager().getParticleRadius());
        }

        // -- Head-specific commands --
        dispatchCommands(head.getCommands(), player);
        dispatchCommands(plugin.getCfgManager().getGlobalFindCommands(), player);
        if (complete) dispatchCommands(plugin.getCfgManager().getGlobalCompleteCommands(), player);
    }

    // ------------------------------------------------------------------

    private void dispatchCommands(List<String> cmds, Player player) {
        if (cmds == null) return;
        for (String cmd : cmds) {
            String resolved = cmd
                    .replace("%player%", player.getName())
                    .replace("{player}", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved);
        }
    }

    private Map<String, String> ph(Player player, int found, int total, int remaining, SecretHead head) {
        Map<String, String> m = new HashMap<>();
        m.put("player", player.getName());
        m.put("found", String.valueOf(found));
        m.put("total", String.valueOf(total));
        m.put("remaining", String.valueOf(remaining));
        m.put("percent", String.valueOf(plugin.getPdManager().getPercent(player.getUniqueId(), total)));
        m.put("head_name", head != null
                ? ChatColor.translateAlternateColorCodes('&', head.getDisplayName()) : "???");
        return m;
    }
}