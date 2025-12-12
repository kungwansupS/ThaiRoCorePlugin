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

public class ItemTypeSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final int page;

    private static final List<Material> COMMON_MATERIALS = Arrays.asList(
            // Weapons
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT,
            // Armor (Helmets)
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
            // Armor (Chestplates)
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE,
            // Armor (Leggings)
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS,
            // Armor (Boots)
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS,
            // Misc / Offhand
            Material.SHIELD, Material.TOTEM_OF_UNDYING, Material.ELYTRA,
            // Resources / Misc Items
            Material.STICK, Material.PAPER, Material.BOOK, Material.WRITABLE_BOOK, Material.ENCHANTED_BOOK,
            Material.ARROW, Material.SPECTRAL_ARROW, Material.FEATHER, Material.FLINT,
            Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND, Material.EMERALD, Material.NETHER_STAR,
            Material.BLAZE_ROD, Material.BONE, Material.STRING, Material.LEATHER
            // Note: If you add more items here, pagination will handle it automatically.
    );

    public ItemTypeSelectorGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this(plugin, itemFile, 0);
    }

    public ItemTypeSelectorGUI(ThaiRoCorePlugin plugin, File itemFile, int page) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        this.page = page;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Material Select: " + itemFile.getName()));

        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, COMMON_MATERIALS.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(slot++, createGuiItem(COMMON_MATERIALS.get(i)));
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, createNavButton(Material.ARROW, "§ePrevious Page", "prev_page", String.valueOf(page - 1)));
        } else {
            inv.setItem(45, createNavButton(Material.RED_CONCRETE, "§cCancel / ยกเลิก", "cancel", ""));
        }

        if (COMMON_MATERIALS.size() > (page + 1) * itemsPerPage) {
            inv.setItem(53, createNavButton(Material.ARROW, "§eNext Page", "next_page", String.valueOf(page + 1)));
        }

        // Fill BG
        ItemStack bg = createNavButton(Material.GRAY_STAINED_GLASS_PANE, " ", "bg", "");
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.setLore(Arrays.asList("§eClick to select this type.", "§7คลิกเพื่อเลือกประเภทนี้"));

            NamespacedKey keyType = new NamespacedKey(plugin, "icon_type");
            NamespacedKey keyValue = new NamespacedKey(plugin, "icon_value");
            meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, "select_mat");
            meta.getPersistentDataContainer().set(keyValue, PersistentDataType.STRING, mat.name());

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavButton(Material mat, String name, String type, String value) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            NamespacedKey keyType = new NamespacedKey(plugin, "icon_type");
            NamespacedKey keyValue = new NamespacedKey(plugin, "icon_value");
            meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type);
            meta.getPersistentDataContainer().set(keyValue, PersistentDataType.STRING, value);

            item.setItemMeta(meta);
        }
        return item;
    }
}