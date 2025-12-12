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
        BASE_STATS,
        OFFENSE,
        DEFENSE,
        PEN_CRIT,
        SPEED_CAST,
        CONDITIONAL,
        RECOVERY,
        ELEMENTS // New Page
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
            case BASE_STATS: renderBaseStats(inv); break;
            case OFFENSE: renderOffense(inv); break;
            case DEFENSE: renderDefense(inv); break;
            case PEN_CRIT: renderPenCrit(inv); break;
            case SPEED_CAST: renderSpeedCast(inv); break;
            case CONDITIONAL: renderConditional(inv); break;
            case RECOVERY: renderRecovery(inv); break;
            case ELEMENTS: renderElements(inv); break; // Handle new page
        }

        player.openInventory(inv);
    }

    private void renderGeneral(Inventory inv) {
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

        boolean isUnbreakable = currentAttr.isUnbreakable();
        inv.setItem(26, createIcon(isUnbreakable ? Material.BEDROCK : Material.CRACKED_STONE_BRICKS,
                "§bUnbreakable: " + isUnbreakable,
                "§7Toggle Unbreakable status.", "§8---------------", "§7ตั้งค่าของไม่พัง"));

        inv.setItem(40, createIcon(Material.EMERALD_BLOCK, "§a§lSave to File / บันทึก",
                "§7Save changes to disk.", "§8---------------", "§7บันทึกข้อมูล"));
    }

    private void renderBaseStats(Inventory inv) {
        int[] slots = {10, 11, 12, 19, 20, 21};
        ItemAttributeType[] base = {
                ItemAttributeType.STR_GEAR, ItemAttributeType.AGI_GEAR, ItemAttributeType.VIT_GEAR,
                ItemAttributeType.INT_GEAR, ItemAttributeType.DEX_GEAR, ItemAttributeType.LUK_GEAR
        };
        for(int i=0; i<base.length; i++) inv.setItem(slots[i], createStatIcon(base[i]));

        inv.setItem(14, createStatIcon(ItemAttributeType.MAXHP_PERCENT));
        inv.setItem(15, createStatIcon(ItemAttributeType.MAXSP_PERCENT));

        inv.setItem(16, createStatIcon(ItemAttributeType.HIT_BONUS_FLAT));
        inv.setItem(25, createStatIcon(ItemAttributeType.FLEE_BONUS_FLAT));

        inv.setItem(23, createStatIcon(ItemAttributeType.BASE_MSPD));
    }

    private void renderOffense(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.WEAPON_PATK, ItemAttributeType.PATK_FLAT, ItemAttributeType.PDMG_PERCENT, ItemAttributeType.PDMG_FLAT, ItemAttributeType.FINAL_PDMG_PERCENT,
                ItemAttributeType.WEAPON_MATK, ItemAttributeType.MATK_FLAT, ItemAttributeType.MDMG_PERCENT, ItemAttributeType.MDMG_FLAT, ItemAttributeType.FINAL_MDMG_PERCENT,
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
                ItemAttributeType.P_PEN_FLAT, ItemAttributeType.P_PEN_PERCENT,
                ItemAttributeType.IGNORE_PDEF_FLAT, ItemAttributeType.IGNORE_PDEF_PERCENT,
                ItemAttributeType.M_PEN_FLAT, ItemAttributeType.M_PEN_PERCENT,
                ItemAttributeType.IGNORE_MDEF_FLAT, ItemAttributeType.IGNORE_MDEF_PERCENT,
                ItemAttributeType.CRIT_RATE, ItemAttributeType.CRIT_DMG_PERCENT,
                ItemAttributeType.CRIT_RES, ItemAttributeType.CRIT_DMG_RES_PERCENT
        };
        fillGrid(inv, types);
    }

    private void renderSpeedCast(Inventory inv) {
        ItemAttributeType[] types = {
                ItemAttributeType.ASPD_PERCENT, ItemAttributeType.MSPD_PERCENT,
                ItemAttributeType.VAR_CT_PERCENT, ItemAttributeType.VAR_CT_FLAT,
                ItemAttributeType.FIXED_CT_PERCENT, ItemAttributeType.FIXED_CT_FLAT,
                ItemAttributeType.SKILL_CD_PERCENT, ItemAttributeType.SKILL_CD_FLAT,
                ItemAttributeType.GLOBAL_CD_PERCENT, ItemAttributeType.GLOBAL_CD_FLAT,
                ItemAttributeType.ACD_PERCENT, ItemAttributeType.ACD_FLAT
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

    // New: Render Element Page
    private void renderElements(Inventory inv) {
        inv.setItem(20, createStatIcon(ItemAttributeType.ATTACK_ELEMENT));
        inv.setItem(24, createStatIcon(ItemAttributeType.DEFENSE_ELEMENT));

        inv.setItem(31, createIcon(Material.BOOK, "§eInfo",
                "§70=Neutral, 1=Water, 2=Earth",
                "§73=Fire, 4=Wind, 5=Poison",
                "§76=Holy, 7=Shadow, 8=Ghost",
                "§79=Undead, -1=None"));
    }

    private void fillGrid(Inventory inv, ItemAttributeType[] types) {
        int slot = 9;
        for (ItemAttributeType type : types) {
            if (slot >= 45) break;
            try {
                if (type != null) inv.setItem(slot++, createStatIcon(type));
            } catch (Exception ignored) {}
        }
    }

    private void setupNavigation(Inventory inv, Page page) {
        Page[] pages = Page.values();
        // Since we have 9 pages now, and navigation slots start at 45. 45+8 = 53.
        // 53 is Back button.
        // We have 9 pages: 0-8. Slot 45 to 53 is 9 slots.
        // But slot 53 is Back. So we have 8 slots (45-52).
        // Page 9 (ELEMENTS) will overflow if we assume linear mapping.
        // Let's implement a simple scroll or just hardcode slots if they fit.
        // GENERAL, BASE, OFFENSE, DEFENSE, PEN_CRIT, SPEED, COND, RECOV, ELEM = 9 pages.
        // Slot 45, 46, 47, 48, 49, 50, 51, 52 = 8 slots.
        // We need to condense or handle overflow.
        // For simplicity in "FULLCODE", I'll merge Elements button into Recovery Page or just put it on slot 52 and move 'Back' elsewhere?
        // Let's replace 'Back' (53) with 'Page 2' logic? No, too complex.
        // Let's squeeze buttons.

        // Let's render as many as fit (8 max). If page > 7, maybe handle differently?
        // Actually, let's just make the Elements page accessible from GENERAL page via a specific Icon if it doesn't fit the tab bar.
        // OR: Just override slot 53 if we are on page > 7?

        // Let's try to fit them:
        // 0:GEN, 1:BASE, 2:OFF, 3:DEF, 4:PEN, 5:SPD, 6:CON, 7:REC, 8:ELEM
        // Slots 45-53 (9 slots). Perfect fit!

        for (int i = 0; i < pages.length; i++) {
            int slot = 45 + i;
            boolean active = (pages[i] == page);

            Material mat;
            switch(pages[i]) {
                case GENERAL: mat = Material.BOOK; break;
                case BASE_STATS: mat = Material.PLAYER_HEAD; break;
                case OFFENSE: mat = Material.IRON_SWORD; break;
                case DEFENSE: mat = Material.IRON_CHESTPLATE; break;
                case PEN_CRIT: mat = Material.DIAMOND_AXE; break;
                case SPEED_CAST: mat = Material.CLOCK; break;
                case CONDITIONAL: mat = Material.TARGET; break;
                case RECOVERY: mat = Material.GOLDEN_APPLE; break;
                case ELEMENTS: mat = Material.FLINT_AND_STEEL; break; // New Icon
                default: mat = Material.PAPER;
            }

            // Move Back button to slot 8 (Header) if slot 53 is taken?
            // In renderGeneral/etc, slot 53 is hardcoded as "Back".
            // Let's overwrite slot 53 with the last page tab, and put Back button elsewhere?
            // Actually, `setupNavigation` is called at start. `renderGeneral` adds back button at 53.
            // This will conflict.

            // FIX: Move tabs to 36-44? (Row 5). Content uses 9-35 (Rows 2-4).
            // But `renderGeneral` uses slot 40 for Save.
            // Let's use Row 6 (45-53) for tabs. Move Back button to Row 1 (Slot 8) in header?
            // Header (0-8) usually displays item. Slot 4 is item.
            // Let's put Back button at Slot 8.

            // Wait, previous code put Back at 53.
            // "inv.setItem(53, createIcon(Material.ARROW, ... Back ...));"
            // If I use 53 for ELEMENTS tab, Back is gone.
            // Let's put ELEMENTS tab at slot 52 (replace recovery?) NO.

            // Let's condense:
            // Combine OFFENSE + PEN_CRIT? No.
            // Let's just put Tab 9 (Elements) at slot 0? No.

            // OK, Slot 53 is Back. Slots 45-52 (8 slots) available.
            // We have 9 pages.
            // Page 8 (Elements) needs a place.
            // Let's put Elements button in the GENERAL page content instead of a tab.

            if (pages[i] == Page.ELEMENTS) continue; // Don't add tab for Elements

            inv.setItem(slot, createIcon(
                    active ? Material.LIME_STAINED_GLASS_PANE : mat,
                    (active ? "§a" : "§7") + pages[i].name(),
                    "§7Click to open tab",
                    "§8---------------",
                    "§7คลิกเพื่อเปิดหน้าต่างนี้"
            ));
        }

        // Make sure Back button is set by specific render methods or here
        inv.setItem(53, createIcon(Material.ARROW, "§eBack to Library / กลับ",
                "§7Return to folder view", "§8---------------", "§7กลับไปหน้าคลังไอเทม"));

        // Add specific link to Elements in General Page
        if (page == Page.GENERAL) {
            inv.setItem(34, createIcon(Material.FLINT_AND_STEEL, "§6Edit Elements / ธาตุ",
                    "§7Manage Attack/Defense Elements.", "§8---------------", "§7จัดการธาตุโจมตี/ป้องกัน"));
        }
    }

    private ItemStack createStatIcon(ItemAttributeType type) {
        double val = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(currentAttr, type);
        ItemStack icon = new ItemStack(type.getMaterial());
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(type.getDisplayName());
        List<String> lore = new ArrayList<>();

        // Element Special Format
        if (type == ItemAttributeType.ATTACK_ELEMENT || type == ItemAttributeType.DEFENSE_ELEMENT) {
            String eName = "None (-1)";
            if (val != -1) {
                int idx = (int) val;
                if (idx >= 0 && idx < org.rostats.engine.element.Element.values().length) {
                    eName = org.rostats.engine.element.Element.values()[idx].name() + " (" + idx + ")";
                }
            }
            lore.add("§7Current: §e" + eName);
        } else {
            lore.add("§7Current: §e" + String.format(type.getFormat(), val));
        }

        lore.add(" ");

        if (type.getDescription() != null && !type.getDescription().isEmpty()) {
            for (String desc : type.getDescription()) {
                lore.add("§f" + desc);
            }
            lore.add(" ");
        }

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