package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
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
        String pathDisplay = plugin.getItemManager().getRelativePath(currentDir);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Library: " + pathDisplay));

        // 1. Navigation Row
        if (!currentDir.equals(plugin.getItemManager().getRootDir())) {
            inv.setItem(0, createGuiItem(Material.ARROW, "§eBack / ย้อนกลับ", "back",
                    "§7Go back to the previous folder."));
        } else {
            inv.setItem(0, createGuiItem(Material.BOOKSHELF, "§eRoot / หน้าแรก", "root",
                    "§7You are at the root directory."));
        }

        inv.setItem(4, createGuiItem(Material.CHEST, "§aNew Folder / สร้างโฟลเดอร์", "new_folder",
                "§7Create a new folder."));

        inv.setItem(8, createGuiItem(Material.EMERALD, "§aNew Item / สร้างไอเทม", "new_item",
                "§7Create a new item file."));

        // 2. Content Grid
        List<File> files = plugin.getItemManager().listContents(currentDir);
        int slot = 9;

        for (File file : files) {
            if (slot >= 54) break;

            if (file.isDirectory()) {
                // Folder Item
                ItemStack folderItem = createGuiItem(Material.CHEST, "§6§l" + file.getName(), file.getName(),
                        "§eLEFT CLICK §7to Open",
                        "§bSHIFT+RIGHT §7to Rename",
                        "§cSHIFT+LEFT §7to Delete"
                );
                inv.setItem(slot, folderItem);
            } else {
                // File Item (Load actual item stack for display)
                ItemStack displayItem;
                try {
                    ItemStack loaded = plugin.getItemManager().loadItemStack(file);
                    if (loaded != null) {
                        displayItem = loaded.clone();
                    } else {
                        displayItem = new ItemStack(Material.PAPER);
                    }
                } catch (Exception e) {
                    displayItem = new ItemStack(Material.PAPER);
                }

                ItemMeta meta = displayItem.getItemMeta();
                if (meta != null) {
                    String fileName = file.getName();

                    // Use existing DisplayName (supports Hex) or fallback to filename
                    String currentName = meta.hasDisplayName() ? meta.getDisplayName() : "§f" + fileName.replace(".yml", "");

                    // Format: "RealName §8(filename.yml)"
                    meta.setDisplayName(currentName + " §8(" + fileName + ")");

                    // Clean up tooltip for Editor
                    meta.setLore(Arrays.asList(
                            "§eLEFT CLICK §7to Edit",
                            "§aSHIFT+RIGHT §7to Give",
                            "§cSHIFT+LEFT §7to Delete"
                    ));

                    // Hide attributes to keep GUI clean (Removed HIDE_POTION_EFFECTS for 1.8 compatibility)
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);

                    // Store real filename in PDC for safe retrieval
                    NamespacedKey key = new NamespacedKey(plugin, "filename");
                    meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, fileName);

                    displayItem.setItemMeta(meta);
                }
                inv.setItem(slot, displayItem);
            }
            slot++;
        }

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    public void openConfirmDelete(Player player, File fileToDelete) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Confirm Delete: " + fileToDelete.getName()));

        inv.setItem(11, createGuiItem(Material.LIME_CONCRETE, "§a§lCONFIRM DELETE", "confirm", "§7File: " + fileToDelete.getName()));
        inv.setItem(15, createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL", "cancel", "§7Return to library"));

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }
        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat, String name, String pdcValue, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        if (pdcValue != null) {
            NamespacedKey key = new NamespacedKey(plugin, "filename");
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, pdcValue);
        }

        item.setItemMeta(meta);
        return item;
    }
}