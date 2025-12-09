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

    // กำหนดหมวดหมู่หน้าต่างใหม่ 8 หมวด + General
    public enum Page {
        GENERAL,
        BASE_STATS,     // Status & Derived (HP/SP/Hit/Flee/Speed)
        OFFENSE,        // P.ATK/M.ATK/Final Dmg/True Dmg
        DEFENSE,        // Reduce/Resist/Shield
        PEN_CRIT,       // Penetration/Ignore Def/Crit
        SPEED_CAST,     // ASPD/MSPD/Casting Time
        CONDITIONAL,    // Melee/Range/PvE/PvP
        RECOVERY        // Heal/Lifesteal
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
        // สร้าง Inventory ขนาด 54 ช่อง (6 แถว)
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + itemFile.getName() + " [" + page.name() + "]"));

        inv.setItem(4, currentItem); // แสดงไอเทมปัจจุบันตรงกลางบน
        setupNavigation(inv, page);  // สร้างปุ่มเปลี่ยนหน้าด้านล่าง

        // เลือก Render หน้าตาม Page ที่ส่งมา
        switch (page) {
            case GENERAL: renderGeneral(inv); break;
            case BASE_STATS: renderBaseStats(inv); break;
            case OFFENSE: renderOffense(inv); break;
            case DEFENSE: renderDefense(inv); break;
            case PEN_CRIT: renderPenCrit(inv); break;
            case SPEED_CAST: renderSpeedCast(inv); break;
            case CONDITIONAL: renderConditional(inv); break;
            case RECOVERY: renderRecovery(inv); break;
        }

        player.openInventory(inv);
    }

    private void renderGeneral(Inventory inv) {
        // ปุ่มพื้นฐาน (Rename, Lore, Type, Effect, Enchant, Skill, Save)
        inv.setItem(19, createIcon(Material.NAME_TAG, "§eRename Item / เปลี่ยนชื่อ",
                "§7Change the display name.", "§8---------------", "§7คลิกเพื่อเปลี่ยนชื่อ"));

        inv.setItem(20, createIcon(Material.WRITABLE_BOOK, "§eEdit Lore / แก้ไขคำอธิบาย",
                "§7Edit the item's lore.", "§8---------------", "§7คลิกเพื่อแก้ไข Lore"));

        inv.setItem(21, createIcon(Material.ANVIL, "§eChange Type / เปลี่ยนประเภท",
                "§7Change item material.", "§8---------------", "§7เปลี่ยนชนิดไอเทม (Material)"));

        inv.setItem(22, createIcon(Material.POTION, "§dEdit Effects / เอฟเฟกต์ยา",
                "§7Manage Potion Effects.", "§8---------------", "§7จัดการ Effect ติดตัว"));

        inv.setItem(23, createIcon(Material.ENCHANTED_BOOK, "§bEdit Enchantments / เอนชานต์",
                "§7Manage Enchantments.", "§8---------------", "§7จัดการ Enchantment"));

        inv.setItem(24, createIcon(Material.NETHER_STAR, "§6Edit Skills / สกิล",
                "§7Manage Skills/Triggers.", "§8---------------", "§7ตั้งค่าสกิลและเงื่อนไข"));

        boolean removeVanilla = currentAttr.isRemoveVanillaAttribute();
        inv.setItem(25, createIcon(removeVanilla ? Material.REDSTONE_BLOCK : Material.REDSTONE,
                "§cRemove Vanilla: " + removeVanilla,
                "§7Toggle hiding vanilla attributes.", "§8---------------", "§7ซ่อนสถานะเดิมของ Minecraft"));

        // ปุ่ม Save สีเขียวเด่นๆ
        inv.setItem(40, createIcon(Material.EMERALD_BLOCK, "§a§lSave to File / บันทึก",
                "§7Save changes to disk.", "§8---------------", "§7บันทึกข้อมูล"));
    }

    private void renderBaseStats(Inventory inv) {
        // STR-LUK
        int[] slots = {10, 11, 12, 19, 20, 21};
        ItemAttributeType[] base = {
                ItemAttributeType.STR_GEAR, ItemAttributeType.AGI_GEAR, ItemAttributeType.VIT_GEAR,
                ItemAttributeType.INT_GEAR, ItemAttributeType.DEX_GEAR, ItemAttributeType.LUK_GEAR
        };
        for(int i=0; i<base.length; i++) inv.setItem(slots[i], createStatIcon(base[i]));

        // HP/SP
        inv.setItem(14, createStatIcon(ItemAttributeType.MAXHP_PERCENT));
        inv.setItem(15, createStatIcon(ItemAttributeType.MAXSP_PERCENT));

        // Derived Combat
        inv.setItem(16, createStatIcon(ItemAttributeType.HIT_BONUS_FLAT));
        inv.setItem(25, createStatIcon(ItemAttributeType.FLEE_BONUS_FLAT));

        // Base Speed
        inv.setItem(23, createStatIcon(ItemAttributeType.BASE_MSPD));
    }

    private void renderOffense(Inventory inv) {
        ItemAttributeType[] types = {
                // Physical
                ItemAttributeType.WEAPON_PATK, ItemAttributeType.PATK_FLAT, ItemAttributeType.PDMG_PERCENT, ItemAttributeType.PDMG_FLAT, ItemAttributeType.FINAL_PDMG_PERCENT,
                // Magical
                ItemAttributeType.WEAPON_MATK, ItemAttributeType.MATK_FLAT, ItemAttributeType.MDMG_PERCENT, ItemAttributeType.MDMG_FLAT, ItemAttributeType.FINAL_MDMG_PERCENT,
                // Universal
                ItemAttributeType.FINAL_DMG_PERCENT, ItemAttributeType.TRUE_DMG
        };
        fillGrid(inv, types);
    }

    private void renderDefense(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.PDMG_REDUCTION_PERCENT, ItemAttributeType.MDMG_REDUCTION_PERCENT,
                ItemAttributeType.FINAL_DMG_RES_PERCENT,
                ItemAttributeType.SHIELD_VALUE_FLAT, ItemAttributeType.SHIELD_RATE_PERCENT
        };
        fillGrid(inv, types);
    }

    private void renderPenCrit(Inventory inv) {
        ItemAttributeType[] types = {
                // Physical Pen/Ignore
                ItemAttributeType.P_PEN_FLAT, ItemAttributeType.P_PEN_PERCENT,
                ItemAttributeType.IGNORE_PDEF_FLAT, ItemAttributeType.IGNORE_PDEF_PERCENT,
                // Magical Pen/Ignore
                ItemAttributeType.M_PEN_FLAT, ItemAttributeType.M_PEN_PERCENT,
                ItemAttributeType.IGNORE_MDEF_FLAT, ItemAttributeType.IGNORE_MDEF_PERCENT,
                // Crit
                ItemAttributeType.CRIT_RATE, ItemAttributeType.CRIT_DMG_PERCENT,
                ItemAttributeType.CRIT_RES, ItemAttributeType.CRIT_DMG_RES_PERCENT
        };
        // กรองค่าที่เป็น null ออก (เผื่อ ItemAttributeType บางตัวไม่ได้ถูกประกาศใน Phase 1)
        List<ItemAttributeType> validTypes = new ArrayList<>();
        for (ItemAttributeType t : types) {
            try { if(t != null) validTypes.add(t); } catch(Exception ignored){}
        }
        fillGrid(inv, validTypes.toArray(new ItemAttributeType[0]));
    }

    private void renderSpeedCast(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.ASPD_PERCENT, ItemAttributeType.MSPD_PERCENT,
                ItemAttributeType.VAR_CT_PERCENT, ItemAttributeType.VAR_CT_FLAT,
                ItemAttributeType.FIXED_CT_PERCENT, ItemAttributeType.FIXED_CT_FLAT
        };
        fillGrid(inv, types);
    }

    private void renderConditional(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.MELEE_PDMG_PERCENT, ItemAttributeType.MELEE_PDMG_REDUCTION_PERCENT,
                ItemAttributeType.RANGE_PDMG_PERCENT, ItemAttributeType.RANGE_PDMG_REDUCTION_PERCENT,
                ItemAttributeType.PVE_DMG_PERCENT, ItemAttributeType.PVE_DMG_REDUCTION_PERCENT,
                ItemAttributeType.PVP_DMG_PERCENT, ItemAttributeType.PVP_DMG_REDUCTION_PERCENT
        };
        fillGrid(inv, types);
    }

    private void renderRecovery(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.HEALING_EFFECT_PERCENT, ItemAttributeType.HEALING_RECEIVED_PERCENT,
                ItemAttributeType.LIFESTEAL_P_PERCENT, ItemAttributeType.LIFESTEAL_M_PERCENT
        };
        fillGrid(inv, types);
    }

    private void fillGrid(Inventory inv, ItemAttributeType[] types) {
        int slot = 9; // เริ่มต้นที่แถว 2
        for (ItemAttributeType type : types) {
            if (slot >= 45) break; // กันเกินพื้นที่
            inv.setItem(slot++, createStatIcon(type));
        }
    }

    private void setupNavigation(Inventory inv, Page page) {
        Page[] pages = Page.values();
        // ใช้ Slot 45-52 สำหรับ Tab (8 ช่องพอดี)
        for (int i = 0; i < pages.length; i++) {
            if (i >= 8) break;
            int slot = 45 + i;
            boolean active = (pages[i] == page);

            Material mat;
            // เลือกไอคอนให้สื่อความหมายแต่ละ Tab
            switch(pages[i]) {
                case GENERAL: mat = Material.BOOK; break;
                case BASE_STATS: mat = Material.PLAYER_HEAD; break;
                case OFFENSE: mat = Material.IRON_SWORD; break;
                case DEFENSE: mat = Material.IRON_CHESTPLATE; break;
                case PEN_CRIT: mat = Material.DIAMOND_AXE; break;
                case SPEED_CAST: mat = Material.CLOCK; break;
                case CONDITIONAL: mat = Material.TARGET; break;
                case RECOVERY: mat = Material.GOLDEN_APPLE; break;
                default: mat = Material.PAPER;
            }

            inv.setItem(slot, createIcon(
                    active ? Material.LIME_STAINED_GLASS_PANE : mat,
                    (active ? "§a" : "§7") + pages[i].name(),
                    "§7Click to open tab",
                    "§8---------------",
                    "§7คลิกเพื่อเปิดหน้าต่างนี้"
            ));
        }
        // ปุ่ม Back (Slot 53)
        inv.setItem(53, createIcon(Material.ARROW, "§eBack to Library / กลับ",
                "§7Return to folder view", "§8---------------", "§7กลับไปหน้าคลังไอเทม"));
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
        lore.add("§8---------------");
        lore.add("§eคลิกซ้าย: §7เพิ่ม " + type.getClickStep());
        lore.add("§eคลิกขวา: §7ลด " + type.getClickStep());

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