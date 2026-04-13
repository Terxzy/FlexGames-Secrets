package cz.flexgames.secrets.gui;

import cz.flexgames.secrets.FlexSecrets;
import cz.flexgames.secrets.model.SecretHead;
import cz.flexgames.secrets.util.HeadUtil;
import cz.flexgames.secrets.util.NMSUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin GUI – prohlizec vsech basehead z configu.
 * Kliknuti na hlavu = dostanes item do inventare k umisteni.
 */
public class SecretsGUI implements Listener {

    private final FlexSecrets plugin;

    public SecretsGUI(FlexSecrets plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        List<SecretHead> heads = plugin.getCfgManager().getAllHeads();
        int size = Math.min(6, (int) Math.ceil((heads.size() + 9) / 9.0)) * 9;
        if (size < 9) size = 9;

        String title = ChatColor.translateAlternateColorCodes('&',
                plugin.getMsgManager().getRaw("gui.title"));
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Hlavy
        for (int i = 0; i < heads.size() && i < size - 9; i++) {
            inv.setItem(i, buildHeadIcon(heads.get(i)));
        }

        // Spodni lista – filler
        ItemStack filler = glassPane((short) 7, " ");
        for (int i = size - 9; i < size; i++) inv.setItem(i, filler);

        // Zavreni
        String closeName = ChatColor.translateAlternateColorCodes('&',
                plugin.getMsgManager().getRaw("gui.close-name"));
        inv.setItem(size - 5, buildItem(Material.BARRIER, (short) 0, closeName, null));

        player.openInventory(inv);
        if (plugin.getCfgManager().isSoundEnabled())
            NMSUtil.playSound(player, plugin.getCfgManager().getSoundGuiOpen(), 0.8f, 1f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (event.getInventory() == null) return;

        String expected = ChatColor.translateAlternateColorCodes('&',
                plugin.getMsgManager().getRaw("gui.title"));
        if (!expected.equals(event.getInventory().getTitle())) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (plugin.getCfgManager().isSoundEnabled())
            NMSUtil.playSound(player, plugin.getCfgManager().getSoundGuiClick(), 0.5f, 1.2f);

        // Zavreni
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Kliknuti na skull icon – dej hracovi item
        if (clicked.getType() == Material.SKULL_ITEM) {
            String id = HeadUtil.extractId(clicked);
            if (id != null) {
                SecretHead head = plugin.getCfgManager().getHead(id);
                if (head != null) {
                    player.closeInventory();
                    player.getInventory().addItem(HeadUtil.buildSkull(head));

                    String msg = plugin.getMsgManager().get("cmd.give-success", null);
                    msg = msg.replace("{head_name}",
                            ChatColor.translateAlternateColorCodes('&', head.getDisplayName()));
                    player.sendMessage(msg);
                }
            }
        }
    }

    // ------------------------------------------------------------------

    private ItemStack buildHeadIcon(SecretHead head) {
        List<String> extra = new ArrayList<>();
        extra.add("");
        extra.add(ChatColor.translateAlternateColorCodes('&', "&7ID: &e" + head.getId()));
        extra.add(ChatColor.translateAlternateColorCodes('&', "&aKlikni pro ziskani itemu!"));
        return HeadUtil.buildSkullWithExtraLore(head, extra);
    }

    @SuppressWarnings("deprecation")
    private ItemStack glassPane(short data, String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildItem(Material mat, short data, String name, List<String> lore) {
        @SuppressWarnings("deprecation")
        ItemStack item = new ItemStack(mat, 1, data);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}