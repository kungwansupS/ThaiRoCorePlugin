package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ItemLibraryGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentDir;

    public ItemLibraryGUI(ThaiRoCorePlugin plugin, File currentDir) {
        this.plugin = plugin;
        this.currentDir = currentDir;
    }

    public void open(Player player) {
        // ใช้ Relative Path เพื่อให้ Title ไม่ยาวเกินไปและ GUIListener อ่านค่ากลับได้ถูกต้อง
        String pathDisplay = plugin.getItemManager().getRelativePath(currentDir);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Library: " + pathDisplay));

        // 1. Navigation Row (Top)
        if (!currentDir.equals(plugin.getItemManager().getRootDir())) {
            inv.setItem(0, createGuiItem(Material.ARROW, "§eBack", "§7Click to go up"));
        } else {
            inv.setItem(0, createGuiItem(Material.BOOKSHELF, "§eRoot", "§7You are at root"));
        }

        inv.setItem(4, createGuiItem(Material.CHEST, "§aNew Folder", "§7Click to create new folder"));
        inv.setItem(8, createGuiItem(Material.EMERALD, "§aNew Item", "§7Click to create new item"));

        // 2. Content Grid (Row 1-5)
        List<File> files = plugin.getItemManager().listContents(currentDir);
        int slot = 9;

        for (File file : files) {
            if (slot >= 54) break;

            if (file.isDirectory()) {
                inv.setItem(slot, createGuiItem(Material.CHEST, "§6§l" + file.getName(),
                        "§eLEFT CLICK §7to Open",
                        "§cSHIFT+LEFT §7to Delete",
                        "§bSHIFT+RIGHT §7to Rename"));
            } else {
                // พยายามอ่าน Material จากไฟล์ ถ้าอ่านไม่ได้ให้ใช้ PAPER
                Material mat = Material.PAPER;
                try {
                    ItemStack temp = plugin.getItemManager().loadItemStack(file);
                    if (temp != null) mat = temp.getType();
                } catch (Exception e) {
                    // Ignore load error for icon
                }

                inv.setItem(slot, createGuiItem(mat, "§f" + file.getName().replace(".yml", ""),
                        "§eLEFT CLICK §7to Edit",
                        "§aSHIFT+RIGHT §7to Give",
                        "§cSHIFT+LEFT §7to Delete",
                        "§dMIDDLE CLICK §7to Duplicate"));
            }
            slot++;
        }

        // Fill empty slots
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}