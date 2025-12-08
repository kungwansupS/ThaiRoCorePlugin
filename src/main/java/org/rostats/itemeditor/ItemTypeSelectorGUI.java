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

public class ItemTypeSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;

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
    );

    public ItemTypeSelectorGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this.plugin = plugin;
        this.itemFile = itemFile;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Material Select: " + itemFile.getName()));

        int slot = 0;
        for (Material mat : COMMON_MATERIALS) {
            if (slot >= 53) break;
            inv.setItem(slot++, createGuiItem(mat));
        }

        // Back Button at 53
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName("§cCancel / ยกเลิก");
        back.setItemMeta(meta);
        inv.setItem(53, back);

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.setLore(Arrays.asList("§eClick to select this type.", "§7คลิกเพื่อเลือกประเภทนี้"));
        item.setItemMeta(meta);
        return item;
    }
}