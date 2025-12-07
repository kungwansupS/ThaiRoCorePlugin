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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AttributeEditorGUI {

    public enum Page {
        GENERAL, CORE_STATS, OFFENSIVE, DEFENSIVE, UTILITY, SAVE_APPLY
    }

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final ItemStack currentItem; // The visual representation being edited
    private final ItemAttribute currentAttr;

    public AttributeEditorGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        // Load data fresh from file
        this.currentItem = plugin.getItemManager().loadItemStack(itemFile);
        this.currentAttr = plugin.getItemManager().loadAttribute(itemFile);
    }

    public void open(Player player, Page page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + itemFile.getName() + " [" + page.name() + "]"));

        // Slot 4: Preview Item (Visual only)
        inv.setItem(4, currentItem);

        // Navigation (Row 6)
        setupNavigation(inv, page);

        // Page Content
        switch (page) {
            case GENERAL: renderGeneral(inv); break;
            case CORE_STATS: renderCoreStats(inv); break;
            case OFFENSIVE: renderOffensive(inv); break;
            case DEFENSIVE: renderDefensive(inv); break;
            case UTILITY: renderUtility(inv); break;
            case SAVE_APPLY: renderSaveApply(inv); break;
        }

        player.openInventory(inv);
    }

    private void renderGeneral(Inventory inv) {
        inv.setItem(20, createIcon(Material.NAME_TAG, "§eRename Item", "§7Click to rename via Chat"));
        inv.setItem(22, createIcon(Material.WRITABLE_BOOK, "§eEdit Lore", "§7Click to edit lore via Chat"));

        boolean removeVanilla = currentAttr.isRemoveVanillaAttribute();
        inv.setItem(24, createIcon(removeVanilla ? Material.REDSTONE_BLOCK : Material.REDSTONE,
                "§cRemove Vanilla: " + removeVanilla, "§7Click to toggle"));
    }

    private void renderCoreStats(Inventory inv) {
        int[] slots = {19, 20, 21, 22, 23, 24};
        ItemAttributeType[] types = {
                ItemAttributeType.STR_GEAR, ItemAttributeType.AGI_GEAR, ItemAttributeType.VIT_GEAR,
                ItemAttributeType.INT_GEAR, ItemAttributeType.DEX_GEAR, ItemAttributeType.LUK_GEAR
        };
        for (int i = 0; i < types.length; i++) {
            inv.setItem(slots[i], createStatIcon(types[i]));
        }
    }

    private void renderOffensive(Inventory inv) {
        // Layout for offensive stats
        ItemAttributeType[] types = {
                ItemAttributeType.WEAPON_PATK, ItemAttributeType.WEAPON_MATK,
                ItemAttributeType.PDMG_PERCENT, ItemAttributeType.MDMG_PERCENT,
                ItemAttributeType.CRIT_DMG_PERCENT, ItemAttributeType.CRIT_RES,
                ItemAttributeType.PVE_DMG_PERCENT, ItemAttributeType.PVP_DMG_PERCENT
        };
        int slot = 18;
        for (ItemAttributeType type : types) {
            inv.setItem(slot++, createStatIcon(type));
        }
    }

    private void renderDefensive(Inventory inv) {
        // Layout including PVP/PVE Reduction
        ItemAttributeType[] types = {
                ItemAttributeType.MAXHP_PERCENT, ItemAttributeType.MAXSP_PERCENT,
                ItemAttributeType.PDMG_REDUCTION_PERCENT, ItemAttributeType.MDMG_REDUCTION_PERCENT,
                ItemAttributeType.PVE_DMG_REDUCTION_PERCENT, ItemAttributeType.PVP_DMG_REDUCTION_PERCENT,
                ItemAttributeType.SHIELD_VALUE_FLAT
        };
        int slot = 18;
        for (ItemAttributeType type : types) {
            inv.setItem(slot++, createStatIcon(type));
        }
    }

    private void renderUtility(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.ASPD_PERCENT, ItemAttributeType.MSPD_PERCENT,
                ItemAttributeType.VAR_CT_PERCENT, ItemAttributeType.FIXED_CT_PERCENT,
                ItemAttributeType.HEALING_EFFECT_PERCENT, ItemAttributeType.LIFESTEAL_P_PERCENT
        };
        int slot = 18;
        for (ItemAttributeType type : types) {
            inv.setItem(slot++, createStatIcon(type));
        }
    }

    private void renderSaveApply(Inventory inv) {
        inv.setItem(22, createIcon(Material.EMERALD_BLOCK, "§a§lSave to File", "§7Click to save changes permanently"));
    }

    private void setupNavigation(Inventory inv, Page page) {
        int startSlot = 45;
        Page[] pages = Page.values();
        for (int i = 0; i < pages.length; i++) {
            boolean active = (pages[i] == page);
            inv.setItem(startSlot + i, createIcon(
                    active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                    (active ? "§a" : "§7") + pages[i].name(),
                    "§7Click to go to page"
            ));
        }
        inv.setItem(53, createIcon(Material.ARROW, "§eBack to Library", "§7Return to folder view"));
    }

    private ItemStack createStatIcon(ItemAttributeType type) {
        // Get value using the specific getter logic or reflection (simplified here using manager helper)
        double val = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(currentAttr, type);

        ItemStack icon = new ItemStack(type.getMaterial());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(type.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + String.format(type.getFormat(), val));
        lore.add(" ");
        lore.add("§eLeft: §7+" + type.getClickStep());
        lore.add("§eRight: §7-" + type.getClickStep());
        lore.add("§eShift+Left: §7+" + type.getRightClickStep());
        lore.add("§eShift+Right: §7-" + type.getRightClickStep());
        lore.add("§dMiddle Click: §7Type Value");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createIcon(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}