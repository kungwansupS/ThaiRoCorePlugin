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
        GENERAL,
        BASE_ATTR,
        CORE_COMBAT,
        PENETRATION,
        CASTING,
        CD_DELAY,
        SPEED_MOBILITY,
        CRITICAL,
        UNIVERSAL_DMG,
        DISTANCE_CONTENT,
        HEALING,
        SAVE_APPLY
    }

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final ItemStack currentItem;
    private final ItemAttribute currentAttr;

    public AttributeEditorGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        this.currentItem = plugin.getItemManager().loadItemStack(itemFile);
        this.currentAttr = plugin.getItemManager().loadAttribute(itemFile);
    }

    public void open(Player player, Page page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + itemFile.getName() + " [" + page.name() + "]"));

        inv.setItem(4, currentItem);
        setupNavigation(inv, page);

        switch (page) {
            case GENERAL: renderGeneral(inv); break;
            case BASE_ATTR: renderBaseAttr(inv); break;
            case CORE_COMBAT: renderCoreCombat(inv); break;
            case PENETRATION: renderPenetration(inv); break;
            case CASTING: renderCasting(inv); break;
            case CD_DELAY: renderCDDelay(inv); break;
            case SPEED_MOBILITY: renderSpeed(inv); break;
            case CRITICAL: renderCritical(inv); break;
            case UNIVERSAL_DMG: renderUniversalDmg(inv); break;
            case DISTANCE_CONTENT: renderDistContent(inv); break;
            case HEALING: renderHealing(inv); break;
            case SAVE_APPLY: renderSaveApply(inv); break;
        }

        player.openInventory(inv);
    }

    private void renderGeneral(Inventory inv) {
        inv.setItem(19, createIcon(Material.NAME_TAG, "§eRename Item / เปลี่ยนชื่อ", "§7Change display name.", "§7Supports &aColors."));
        inv.setItem(20, createIcon(Material.WRITABLE_BOOK, "§eEdit Lore / แก้ไข Lore", "§7Multi-line editor."));
        inv.setItem(21, createIcon(Material.ANVIL, "§eChange Type / เปลี่ยนชนิด", "§7Change material (Sword/Armor/etc)."));
        inv.setItem(22, createIcon(Material.POTION, "§dEdit Effects / Potion Effect", "§7Passive effects when worn."));
        inv.setItem(23, createIcon(Material.ENCHANTED_BOOK, "§bEdit Enchantments", "§7Vanilla Enchantments."));
        inv.setItem(24, createIcon(Material.NETHER_STAR, "§6Edit Skills / สกิล", "§7Bind active/passive skills."));
        boolean removeVanilla = currentAttr.isRemoveVanillaAttribute();
        inv.setItem(25, createIcon(removeVanilla ? Material.REDSTONE_BLOCK : Material.REDSTONE,
                "§cRemove Vanilla: " + removeVanilla, "§7Hide vanilla attributes."));
    }

    private void renderBaseAttr(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.STR_GEAR, ItemAttributeType.AGI_GEAR, ItemAttributeType.VIT_GEAR,
                ItemAttributeType.INT_GEAR, ItemAttributeType.DEX_GEAR, ItemAttributeType.LUK_GEAR,
                ItemAttributeType.MAXHP_PERCENT, ItemAttributeType.MAXHP_FLAT,
                ItemAttributeType.MAXSP_PERCENT, ItemAttributeType.MAXSP_FLAT,
                ItemAttributeType.HP_RECOVERY, ItemAttributeType.SP_RECOVERY,
                ItemAttributeType.HIT_BONUS_FLAT, ItemAttributeType.FLEE_BONUS_FLAT
        };
        fillSlots(inv, types, 9);
    }

    private void renderCoreCombat(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.WEAPON_PATK, ItemAttributeType.WEAPON_MATK,
                ItemAttributeType.REFINE_P_ATK, ItemAttributeType.REFINE_M_ATK,
                ItemAttributeType.P_DEF_BONUS, ItemAttributeType.M_DEF_BONUS,
                ItemAttributeType.REFINE_P_DEF, ItemAttributeType.REFINE_M_DEF
        };
        fillSlots(inv, types, 18);
    }

    private void renderPenetration(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.P_PEN_FLAT, ItemAttributeType.P_PEN_PERCENT,
                ItemAttributeType.IGNORE_PDEF_FLAT, ItemAttributeType.IGNORE_PDEF_PERCENT,
                ItemAttributeType.M_PEN_FLAT, ItemAttributeType.M_PEN_PERCENT,
                ItemAttributeType.IGNORE_MDEF_FLAT, ItemAttributeType.IGNORE_MDEF_PERCENT
        };
        fillSlots(inv, types, 18);
    }

    private void renderCasting(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.VAR_CT_PERCENT, ItemAttributeType.VAR_CT_FLAT,
                ItemAttributeType.FIXED_CT_PERCENT, ItemAttributeType.FIXED_CT_FLAT
        };
        fillSlots(inv, types, 20);
    }

    private void renderCDDelay(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.SKILL_COOLDOWN_PERCENT, ItemAttributeType.SKILL_COOLDOWN_FLAT,
                ItemAttributeType.FINAL_COOLDOWN_PERCENT,
                ItemAttributeType.GLOBAL_CD_PERCENT,
                ItemAttributeType.AFTER_CAST_DELAY_PERCENT, ItemAttributeType.AFTER_CAST_DELAY_FLAT,
                ItemAttributeType.PRE_MOTION, ItemAttributeType.POST_MOTION, ItemAttributeType.CANCEL_MOTION
        };
        fillSlots(inv, types, 18);
    }

    private void renderSpeed(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.ASPD_PERCENT, ItemAttributeType.MSPD_PERCENT,
                ItemAttributeType.BASE_MSPD, ItemAttributeType.ATTACK_INTERVAL_PERCENT
        };
        fillSlots(inv, types, 20);
    }

    private void renderCritical(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.CRIT, ItemAttributeType.CRIT_DMG_PERCENT, ItemAttributeType.FINAL_CRIT_DMG_PERCENT,
                ItemAttributeType.PERFECT_HIT,
                ItemAttributeType.CRIT_RES, ItemAttributeType.CRIT_DMG_RES_PERCENT, ItemAttributeType.PERFECT_DODGE
        };
        fillSlots(inv, types, 18);
    }

    private void renderUniversalDmg(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.PDMG_PERCENT, ItemAttributeType.PDMG_FLAT, ItemAttributeType.PDMG_REDUCTION_PERCENT,
                ItemAttributeType.MDMG_PERCENT, ItemAttributeType.MDMG_FLAT, ItemAttributeType.MDMG_REDUCTION_PERCENT,
                ItemAttributeType.TRUE_DMG,
                ItemAttributeType.FINAL_DMG_PERCENT, ItemAttributeType.FINAL_DMG_RES_PERCENT
        };
        fillSlots(inv, types, 18);
    }

    private void renderDistContent(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.MELEE_PDMG_PERCENT, ItemAttributeType.MELEE_PDMG_REDUCTION_PERCENT,
                ItemAttributeType.RANGE_PDMG_PERCENT, ItemAttributeType.RANGE_PDMG_REDUCTION_PERCENT,
                ItemAttributeType.PVE_DMG_PERCENT, ItemAttributeType.PVE_DMG_REDUCTION_PERCENT,
                ItemAttributeType.PVP_DMG_PERCENT, ItemAttributeType.PVP_DMG_REDUCTION_PERCENT
        };
        fillSlots(inv, types, 18);
    }

    private void renderHealing(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.HEALING_EFFECT_PERCENT, ItemAttributeType.HEALING_FLAT,
                ItemAttributeType.HEALING_RECEIVED_PERCENT, ItemAttributeType.HEALING_RECEIVED_FLAT,
                ItemAttributeType.LIFESTEAL_P_PERCENT, ItemAttributeType.LIFESTEAL_M_PERCENT
        };
        fillSlots(inv, types, 18);
    }

    private void renderSaveApply(Inventory inv) {
        inv.setItem(22, createIcon(Material.EMERALD_BLOCK, "§a§lSave to File / บันทึก", "§7Save all changes to file."));
    }

    private void fillSlots(Inventory inv, ItemAttributeType[] types, int startSlot) {
        int slot = startSlot;
        for (ItemAttributeType type : types) {
            if (slot >= 45) break;
            inv.setItem(slot++, createStatIcon(type));
        }
    }

    // --- Navigation Logic ---
    private void setupNavigation(Inventory inv, Page page) {
        int ord = page.ordinal();
        Page prev = Page.values()[(ord - 1 + Page.values().length) % Page.values().length];
        Page next = Page.values()[(ord + 1) % Page.values().length];

        inv.setItem(45, createIcon(Material.ARROW, "§ePrevious: " + prev.name(), "§7Click to go back"));
        inv.setItem(53, createIcon(Material.ARROW, "§eNext: " + next.name(), "§7Click to go next"));

        inv.setItem(48, createIcon(Material.BOOK, "§bBack to Library", "§7Exit editor"));
        inv.setItem(49, createIcon(Material.EMERALD_BLOCK, "§a§lSave", "§7Quick Save"));

        inv.setItem(8, createIcon(Material.PAPER, "§ePage: " + page.name(), "§7" + (ord+1) + "/" + Page.values().length));
    }

    private ItemStack createStatIcon(ItemAttributeType type) {
        double val = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(currentAttr, type);
        ItemStack icon = new ItemStack(type.getMaterial());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(type.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7Current: §e" + String.format(type.getFormat(), val));
        lore.add(" ");
        lore.add("§eLeft Click: §7+" + type.getClickStep());
        lore.add("§eRight Click: §7-" + type.getClickStep());
        lore.add("§eShift+Left: §7+" + type.getRightClickStep());
        lore.add("§eShift+Right: §7-" + type.getRightClickStep());
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createIcon(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}