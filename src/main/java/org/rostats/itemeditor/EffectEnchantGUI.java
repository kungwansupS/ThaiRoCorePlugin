package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class EffectEnchantGUI {

    public enum Mode {
        EFFECT, ENCHANT
    }

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final Mode mode;

    public EffectEnchantGUI(ThaiRoCorePlugin plugin, File itemFile, Mode mode) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        this.mode = mode;
    }

    public void open(Player player) {
        // FIX: ใส่ชื่อไฟล์ลงไปใน Title ให้ตรง Pattern ที่ GUIListener รอรับ
        // Format: "Editor: <FileName> [<Mode> Select]"
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + itemFile.getName() + " [" + mode.name() + " Select]"));

        // Load current data
        ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
        ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);

        // ดึงค่า Selection ที่เลือกค้างไว้จาก Metadata
        String metaKey = "RO_EDITOR_SEL_" + mode.name();
        String selectedKey = null;
        if (player.hasMetadata(metaKey)) {
            selectedKey = player.getMetadata(metaKey).get(0).asString();
        }

        // Populate List (Slots 0-44)
        int slot = 0;
        if (mode == Mode.EFFECT) {
            for (PotionEffectType type : PotionEffectType.values()) {
                if (type == null) continue;
                if (slot >= 45) break;

                boolean has = attr.getPotionEffects().containsKey(type);
                int lvl = has ? attr.getPotionEffects().get(type) : 0;
                boolean isSelected = type.getName().equals(selectedKey);

                inv.setItem(slot++, createOptionItem(
                        has ? Material.LIME_STAINED_GLASS_PANE : (isSelected ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE),
                        type.getName(),
                        has,
                        lvl,
                        isSelected
                ));
            }
        } else {
            for (Enchantment ench : Enchantment.values()) {
                if (ench == null) continue;
                if (slot >= 45) break;

                boolean has = stack.containsEnchantment(ench);
                int lvl = has ? stack.getEnchantmentLevel(ench) : 0;
                // Enchantment key is usually namespaced, get key name
                String name = ench.getKey().getKey().toUpperCase();
                boolean isSelected = name.equals(selectedKey);

                inv.setItem(slot++, createOptionItem(
                        has ? Material.ENCHANTED_BOOK : (isSelected ? Material.BOOK : Material.BOOK),
                        name,
                        has,
                        lvl,
                        isSelected
                ));
            }
        }

        // Control Panel (Bottom Row)
        updateControlPanel(inv, selectedKey);

        player.openInventory(inv);
    }

    private void updateControlPanel(Inventory inv, String selectedKey) {
        // Slot 49: Selected Info
        if (selectedKey != null) {
            inv.setItem(49, createGuiItem(Material.PAPER, "§eSelected: §f" + selectedKey, "§7Click options above to change"));
            // Slot 50: Anvil (Input Level)
            inv.setItem(50, createGuiItem(Material.ANVIL, "§eSet Level", "§7Click to input level via Chat"));
            // Slot 51: Confirm Add/Update
            inv.setItem(51, createGuiItem(Material.LIME_CONCRETE, "§a§lADD / UPDATE", "§7Apply level to item"));
            // Slot 52: Remove
            inv.setItem(52, createGuiItem(Material.RED_CONCRETE, "§c§lREMOVE", "§7Remove from item"));
        } else {
            inv.setItem(49, createGuiItem(Material.BARRIER, "§cNo Selection", "§7Click an option above"));
            inv.setItem(50, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            inv.setItem(51, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            inv.setItem(52, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Back Button
        inv.setItem(53, createGuiItem(Material.ARROW, "§eBack", "§7Return to Editor"));
    }

    private ItemStack createOptionItem(Material mat, String name, boolean active, int level, boolean selected) {
        String displayName = (active ? "§a" : "§7") + name;
        String status = active ? "§a[ADDED] Lv." + level : "§7[NOT ADDED]";
        String selectStatus = selected ? "§e▶ SELECTED ◀" : "§eClick to Select";

        // ถ้าเป็น Enchantment แล้วยังไม่ Active ให้ใช้ Book ปกติ แต่ถ้า Selected ให้ Enchanted Book เรืองแสงหลอกๆ (ในที่นี้ใช้ Material แยกแทนง่ายกว่า)
        if (selected && mat == Material.BOOK) mat = Material.WRITABLE_BOOK;

        return createGuiItem(mat, displayName, status, selectStatus);
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