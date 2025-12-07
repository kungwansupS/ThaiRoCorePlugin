package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;

import java.util.ArrayList;
import java.util.List;

public class AttributeEditorGUI {

    public enum Page {
        GENERAL, CORE_STATS, OFFENSIVE, DEFENSIVE, UTILITY, SAVE_APPLY
    }

    private final ThaiRoCorePlugin plugin;
    private final ItemAttributeManager manager;

    public AttributeEditorGUI(ThaiRoCorePlugin plugin, ItemAttributeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player, ItemStack item, Page page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Item Editor - " + page.name()));

        // Slot 0: Preview Item
        inv.setItem(0, item);

        // Navigation (Bottom Row)
        setupNavigation(inv, page);

        // Page Content
        switch (page) {
            case GENERAL: renderGeneral(inv, item); break;
            case CORE_STATS: renderCoreStats(inv, item); break;
            case OFFENSIVE: renderOffensive(inv, item); break;
            case DEFENSIVE: renderDefensive(inv, item); break;
            case UTILITY: renderUtility(inv, item); break;
            case SAVE_APPLY: renderSaveApply(inv, item); break;
        }

        player.openInventory(inv);
    }

    private void renderGeneral(Inventory inv, ItemStack item) {
        inv.setItem(20, createIcon(Material.PAPER, "§eRename Item", "§7Click to rename (Chat interaction)"));
        inv.setItem(22, createIcon(Material.BOOK, "§eEdit Lore", "§7Click to edit lore"));
        inv.setItem(24, createIcon(Material.REDSTONE, "§cRemove Vanilla", "§7Click to remove vanilla attributes"));
    }

    private void renderCoreStats(Inventory inv, ItemStack item) {
        int[] slots = {19, 20, 21, 22, 23, 24};
        ItemAttributeType[] types = {
                ItemAttributeType.STR_GEAR, ItemAttributeType.AGI_GEAR, ItemAttributeType.VIT_GEAR,
                ItemAttributeType.INT_GEAR, ItemAttributeType.DEX_GEAR, ItemAttributeType.LUK_GEAR
        };
        for (int i = 0; i < types.length; i++) {
            inv.setItem(slots[i], createStatIcon(item, types[i]));
        }
    }

    private void renderOffensive(Inventory inv, ItemStack item) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        ItemAttributeType[] types = {
                ItemAttributeType.WEAPON_PATK, ItemAttributeType.WEAPON_MATK,
                ItemAttributeType.PDMG_PERCENT, ItemAttributeType.MDMG_PERCENT,
                ItemAttributeType.PDMG_FLAT, ItemAttributeType.MDMG_FLAT,
                ItemAttributeType.CRIT_DMG_PERCENT, ItemAttributeType.CRIT_RES,
                ItemAttributeType.P_PEN_PERCENT, ItemAttributeType.M_PEN_PERCENT,
                ItemAttributeType.FINAL_DMG_PERCENT, ItemAttributeType.PVE_DMG_PERCENT,
                ItemAttributeType.PVP_DMG_PERCENT, ItemAttributeType.TRUE_DMG
        };

        for (int i = 0; i < types.length && i < slots.length; i++) {
            inv.setItem(slots[i], createStatIcon(item, types[i]));
        }
    }

    private void renderDefensive(Inventory inv, ItemStack item) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        ItemAttributeType[] types = {
                ItemAttributeType.MAXHP_PERCENT, ItemAttributeType.MAXSP_PERCENT,
                ItemAttributeType.SHIELD_VALUE_FLAT, ItemAttributeType.SHIELD_RATE_PERCENT,
                ItemAttributeType.IGNORE_PDEF_PERCENT, ItemAttributeType.IGNORE_MDEF_PERCENT,
                ItemAttributeType.PDMG_REDUCTION_PERCENT
        };
        for (int i = 0; i < types.length && i < slots.length; i++) {
            inv.setItem(slots[i], createStatIcon(item, types[i]));
        }
    }

    private void renderUtility(Inventory inv, ItemStack item) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20};
        ItemAttributeType[] types = {
                ItemAttributeType.ASPD_PERCENT, ItemAttributeType.MSPD_PERCENT,
                ItemAttributeType.VAR_CT_PERCENT, ItemAttributeType.FIXED_CT_PERCENT,
                ItemAttributeType.HEALING_EFFECT_PERCENT, ItemAttributeType.HEALING_RECEIVED_PERCENT,
                ItemAttributeType.LIFESTEAL_P_PERCENT, ItemAttributeType.HIT_BONUS_FLAT, ItemAttributeType.FLEE_BONUS_FLAT
        };
        for (int i = 0; i < types.length && i < slots.length; i++) {
            inv.setItem(slots[i], createStatIcon(item, types[i]));
        }
    }

    private void renderSaveApply(Inventory inv, ItemStack item) {
        inv.setItem(20, createIcon(Material.ENDER_CHEST, "§aSave & Get", "§7Save item attributes and give to inventory"));
        inv.setItem(24, createIcon(Material.BARRIER, "§cClose", "§7Exit editor"));
    }

    private void setupNavigation(Inventory inv, Page page) {
        inv.setItem(45, createIcon(Material.ARROW, "§ePrevious Page", ""));
        inv.setItem(53, createIcon(Material.ARROW, "§eNext Page", ""));

        // Page Indicators
        int startSlot = 47;
        Page[] pages = Page.values();
        for (int i = 0; i < pages.length; i++) {
            boolean active = (pages[i] == page);
            inv.setItem(startSlot + i, createIcon(
                    active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                    (active ? "§a" : "§7") + pages[i].name(),
                    "§7Click to jump"
            ));
        }
    }

    private ItemStack createStatIcon(ItemStack item, ItemAttributeType type) {
        double val = manager.getAttributeValue(item, type);
        ItemStack icon = new ItemStack(type.getMaterial());
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text(type.getDisplayName()));
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + String.format(type.getFormat(), val));
        lore.add("§7Key: " + type.getKey());
        lore.add(" ");
        lore.add("§eLeft: §7+" + type.getClickStep());
        lore.add("§eRight: §7-" + type.getClickStep());
        lore.add("§eShift+Left: §7+" + type.getRightClickStep());
        lore.add("§eShift+Right: §7-" + type.getRightClickStep());
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createIcon(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}