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

import java.util.*;

public class AttributeEditorGUI {

    public static final String TITLE_MAIN = "§0§lItem Editor: Categories";
    public static final String TITLE_PREFIX_CATEGORY = "§0§lEdit: ";

    private final ThaiRoCorePlugin plugin;
    private static final Map<EditorCategory, List<ItemAttributeType>> CATEGORY_MAP = new LinkedHashMap<>();

    public enum EditorCategory {
        BASE("Base Attributes", Material.PLAYER_HEAD,
                "§7• Status", "§7• Derived Base Stats"),
        COMBAT("Core Combat Stats", Material.IRON_SWORD,
                "§7• Attack", "§7• Defense"),
        PENETRATION("Penetration / Ignore", Material.DIAMOND_PICKAXE,
                "§7• Physical Pen/Ignore", "§7• Magical Pen/Ignore"),
        CASTING("Casting System", Material.CLOCK,
                "§7• Variable Casting", "§7• Fixed Casting"),
        COOLDOWN("CD / Delay / Motion", Material.COMPASS,
                "§7• Cooldown", "§7• Global Delay", "§7• Motion"),
        SPEED("Speed & Mobility", Material.FEATHER,
                "§7• ASPD / MSPD", "§7• Attack Interval"),
        CRITICAL("Critical System", Material.GOLDEN_SWORD,
                "§7• Critical Offensive", "§7• Critical Defensive"),
        DAMAGE_MODS("Universal Dmg Mods", Material.BLAZE_POWDER,
                "§7• Physical Bonus/Reduct", "§7• Magical Bonus/Reduct"),
        DISTANCE("Distance-Type Dmg", Material.BOW,
                "§7• Melee Modifiers", "§7• Ranged Modifiers"),
        CONTENT("Content-Type Mods", Material.ZOMBIE_HEAD,
                "§7• PvE Modifiers", "§7• PvP Modifiers"),
        HEALING("Healing System", Material.GLISTERING_MELON_SLICE,
                "§7• Heal Output", "§7• Heal Taken"),
        SPECIAL("Special / Misc", Material.NETHER_STAR,
                "§7• HP/SP %", "§7• Shield");

        final String title;
        final Material icon;
        final String[] desc;

        EditorCategory(String title, Material icon, String... desc) {
            this.title = title;
            this.icon = icon;
            this.desc = desc;
        }
    }

    static {
        // 1. Base Attributes
        CATEGORY_MAP.put(EditorCategory.BASE, Arrays.asList(
                ItemAttributeType.STR, ItemAttributeType.AGI, ItemAttributeType.VIT,
                ItemAttributeType.INT, ItemAttributeType.DEX, ItemAttributeType.LUK,
                ItemAttributeType.MAX_HP, ItemAttributeType.MAX_SP,
                ItemAttributeType.HP_RECOVERY, ItemAttributeType.SP_RECOVERY,
                ItemAttributeType.HIT, ItemAttributeType.FLEE
        ));

        // 2. Core Combat Stats
        CATEGORY_MAP.put(EditorCategory.COMBAT, Arrays.asList(
                ItemAttributeType.P_ATK, ItemAttributeType.M_ATK,
                ItemAttributeType.REFINE_P_ATK, ItemAttributeType.REFINE_M_ATK,
                ItemAttributeType.P_DEF, ItemAttributeType.M_DEF,
                ItemAttributeType.REFINE_P_DEF, ItemAttributeType.REFINE_M_DEF
        ));

        // 3. Penetration / Ignore Def
        CATEGORY_MAP.put(EditorCategory.PENETRATION, Arrays.asList(
                ItemAttributeType.PEN_P_FLAT, ItemAttributeType.PEN_P_PERCENT,
                ItemAttributeType.IGNORE_P_DEF_FLAT, ItemAttributeType.IGNORE_P_DEF_PERCENT,
                ItemAttributeType.PEN_M_FLAT, ItemAttributeType.PEN_M_PERCENT,
                ItemAttributeType.IGNORE_M_DEF_FLAT, ItemAttributeType.IGNORE_M_DEF_PERCENT
        ));

        // 4. Casting
        CATEGORY_MAP.put(EditorCategory.CASTING, Arrays.asList(
                ItemAttributeType.VAR_CASTING_PERCENT, ItemAttributeType.VAR_CT_FLAT,
                ItemAttributeType.FIXED_CASTING_PERCENT, ItemAttributeType.FIXED_CT_FLAT
        ));

        // 5. Cooldown / Delay / Motion
        CATEGORY_MAP.put(EditorCategory.COOLDOWN, Arrays.asList(
                ItemAttributeType.SKILL_COOLDOWN_PERCENT, ItemAttributeType.SKILL_COOLDOWN_FLAT,
                ItemAttributeType.FINAL_COOLDOWN_PERCENT,
                ItemAttributeType.GLOBAL_CD_PERCENT,
                ItemAttributeType.AFTER_CAST_DELAY_PERCENT, ItemAttributeType.AFTER_CAST_DELAY_FLAT,
                ItemAttributeType.PRE_MOTION, ItemAttributeType.POST_MOTION, ItemAttributeType.CANCEL_MOTION
        ));

        // 6. Speed & Mobility
        CATEGORY_MAP.put(EditorCategory.SPEED, Arrays.asList(
                ItemAttributeType.ASPD_PERCENT, ItemAttributeType.MSPD_PERCENT,
                ItemAttributeType.ATK_INTERVAL_REDUCTION
        ));

        // 7. Critical System
        CATEGORY_MAP.put(EditorCategory.CRITICAL, Arrays.asList(
                ItemAttributeType.CRIT, ItemAttributeType.CRIT_DMG_PERCENT,
                ItemAttributeType.FINAL_CRIT_DMG_PERCENT,
                ItemAttributeType.CRIT_RES, ItemAttributeType.CRIT_DMG_RES_PERCENT,
                ItemAttributeType.PERFECT_DODGE, ItemAttributeType.PERFECT_HIT
        ));

        // 8. Universal Damage Modifiers
        CATEGORY_MAP.put(EditorCategory.DAMAGE_MODS, Arrays.asList(
                ItemAttributeType.P_DMG_BONUS_PERCENT, ItemAttributeType.P_DMG_BONUS_FLAT,
                ItemAttributeType.P_DMG_REDUCTION_PERCENT,
                ItemAttributeType.M_DMG_BONUS_PERCENT, ItemAttributeType.M_DMG_BONUS_FLAT,
                ItemAttributeType.M_DMG_REDUCTION_PERCENT,
                ItemAttributeType.TRUE_DAMAGE
        ));

        // 9. Distance-Type Damage
        CATEGORY_MAP.put(EditorCategory.DISTANCE, Arrays.asList(
                ItemAttributeType.MELEE_P_DMG_PERCENT, ItemAttributeType.MELEE_P_DMG_REDUCTION_PERCENT,
                ItemAttributeType.RANGED_P_DMG_PERCENT, ItemAttributeType.RANGED_P_DMG_REDUCTION_PERCENT
        ));

        // 10. Content-Type Modifiers
        CATEGORY_MAP.put(EditorCategory.CONTENT, Arrays.asList(
                ItemAttributeType.PVE_DMG_BONUS, ItemAttributeType.PVE_DMG_REDUCTION,
                ItemAttributeType.PVP_DMG_BONUS, ItemAttributeType.PVP_DMG_REDUCTION
        ));

        // 11. Healing System
        CATEGORY_MAP.put(EditorCategory.HEALING, Arrays.asList(
                ItemAttributeType.HEALING_EFFECT_PERCENT, ItemAttributeType.HEALING_FLAT,
                ItemAttributeType.HEALING_RECEIVED_PERCENT, ItemAttributeType.RECEIVED_HEAL_FLAT
        ));

        // 12. Special
        CATEGORY_MAP.put(EditorCategory.SPECIAL, Arrays.asList(
                ItemAttributeType.MAX_HP_PERCENT, ItemAttributeType.MAX_SP_PERCENT,
                ItemAttributeType.SHIELD_VALUE
        ));
    }

    public AttributeEditorGUI(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void openCategoryMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE_MAIN));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};
        int index = 0;

        for (EditorCategory cat : EditorCategory.values()) {
            if (index >= slots.length) break;
            inv.setItem(slots[index], createCategoryItem(cat));
            index++;
        }

        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        inv.setItem(49, createItem(Material.BARRIER, "§c§lClose Editor"));

        player.openInventory(inv);
    }

    public void openAttributeList(Player player, EditorCategory category) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE_PREFIX_CATEGORY + category.title));
        List<ItemAttributeType> attributes = CATEGORY_MAP.getOrDefault(category, Collections.emptyList());

        for (int i = 0; i < attributes.size() && i < 45; i++) {
            inv.setItem(i, createAttributeItem(attributes.get(i)));
        }

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, bg);
        }

        inv.setItem(49, createItem(Material.ARROW, "§e§l< Back to Categories"));

        player.openInventory(inv);
    }

    private ItemStack createCategoryItem(EditorCategory cat) {
        ItemStack item = new ItemStack(cat.icon);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§6§l" + cat.title));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7คลิกเพื่อแก้ไขค่าในหมวดหมู่นี้:"));
        for (String line : cat.desc) {
            lore.add(Component.text(line));
        }
        lore.add(Component.text(" "));
        lore.add(Component.text("§e► คลิกเพื่อเปิด"));
        meta.lore(loreList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createAttributeItem(ItemAttributeType type) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("§a§l" + type.getDisplayName()));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7ID: " + type.name()));
        lore.add(Component.text(" "));
        lore.add(Component.text("§eคลิกซ้าย: §7แก้ไขค่า (พิมพ์ในแชท)"));
        lore.add(Component.text("§eคลิกขวา: §7ลบค่านี้ออกจากไอเทม"));
        meta.lore(loreList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private List<Component> loreList(List<Component> list) {
        return list;
    }
}