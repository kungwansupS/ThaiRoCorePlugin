package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.skill.SkillData;

import java.util.Arrays;
import java.util.List;

public class SkillEditorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;
    private final SkillData skillData;
    private final int page;

    // Constants for Layout
    private static final int ACTION_START_SLOT = 18;
    private static final int ACTIONS_PER_PAGE = 27;

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this(plugin, skillId, 0);
    }

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId, int page) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.skillData = plugin.getSkillManager().getSkill(skillId);
        this.page = page;
    }

    public void open(Player player) {
        if (skillData == null) {
            player.sendMessage("§cError: Skill data not found!");
            player.closeInventory();
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillEditor: " + skillId + " #P" + page));

        // --- Row 1-2: Meta Data ---
        renderMetaData(inv);

        // --- Row 3-5: Action Timeline (Pagination) ---
        List<SkillAction> actions = skillData.getActions();
        int startIndex = page * ACTIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + ACTIONS_PER_PAGE, actions.size());

        int slot = ACTION_START_SLOT;
        for (int i = startIndex; i < endIndex; i++) {
            SkillAction action = actions.get(i);

            Material mat = getActionIcon(action);
            String typeDesc = action.getType().name();

            inv.setItem(slot, createGuiItem(mat, "§f#" + (i+1) + ": " + typeDesc,
                    "§7Index: " + i,
                    "§7Type: " + typeDesc,
                    "§8---------------",
                    "§eL-Click: §7Edit / แก้ไข",
                    "§eShift+L: §6Move Up / เลื่อนขึ้น",
                    "§eR-Click: §6Move Down / เลื่อนลง",
                    "§eShift+R: §cRemove / ลบ"
            ));
            slot++;
        }

        // --- Row 6: Controls ---

        // Page Controls
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§ePrevious Page / หน้าก่อนหน้า", "§7Page: " + page));
        }
        if (endIndex < actions.size()) {
            inv.setItem(53, createGuiItem(Material.ARROW, "§eNext Page / หน้าถัดไป", "§7Page: " + (page + 2)));
        }

        // Main Controls
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE SKILL / บันทึก", "§7Save all changes to file."));
        inv.setItem(50, createGuiItem(Material.LIME_DYE, "§a§l+ Add Action", "§7Add new action at the end."));
        inv.setItem(48, createGuiItem(Material.BOOK, "§bBack to Library", "§7Exit editor."));

        // Fill background
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private void renderMetaData(Inventory inv) {
        inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eDisplay Name", "§7Current: §f" + skillData.getDisplayName()));
        inv.setItem(1, createGuiItem(skillData.getIcon(), "§bIcon", "§7Current: §f" + skillData.getIcon().name()));
        inv.setItem(2, createGuiItem(Material.IRON_SWORD, "§6Skill Type", "§7Current: §e" + skillData.getSkillType()));
        inv.setItem(3, createGuiItem(Material.BOW, "§6Attack Type", "§7Current: §e" + skillData.getAttackType()));
        inv.setItem(4, createGuiItem(Material.COMPASS, "§bCast Range", "§7Range: §f" + skillData.getCastRange()));
        inv.setItem(5, createGuiItem(Material.LEVER, "§cTrigger", "§7Current: §e" + skillData.getTrigger().name()));
        inv.setItem(6, createGuiItem(Material.CLOCK, "§7Cooldown", "§7Base: §f" + skillData.getCooldownBase(), "§7Per Lvl: §f" + skillData.getCooldownPerLevel()));
        inv.setItem(7, createGuiItem(Material.EXPERIENCE_BOTTLE, "§aReq. Level", "§7Level: §e" + skillData.getRequiredLevel()));
        inv.setItem(8, createGuiItem(Material.BLUE_DYE, "§9SP Cost", "§7Base: §f" + skillData.getSpCostBase(), "§7Per Lvl: §f" + skillData.getSpCostPerLevel()));

        // Row 2
        inv.setItem(10, createGuiItem(Material.SUGAR, "§eVariable Cast", "§7Base: " + skillData.getVariableCastTime(), "§7Reduce%: " + skillData.getVariableCastTimeReduction()));
        inv.setItem(11, createGuiItem(Material.HONEY_BLOCK, "§6Fixed Cast", "§7Base: " + skillData.getFixedCastTime()));
        inv.setItem(12, createGuiItem(Material.FEATHER, "§fMotion", "§7Pre: " + skillData.getPreMotion(), "§7Post: " + skillData.getPostMotion()));
        inv.setItem(13, createGuiItem(Material.BARRIER, "§cACD", "§7Delay: " + skillData.getAfterCastDelayBase()));
    }

    private Material getActionIcon(SkillAction action) {
        switch(action.getType()) {
            case DAMAGE: return Material.IRON_SWORD;
            case HEAL: return Material.GOLDEN_APPLE;
            case APPLY_EFFECT: return Material.POTION;
            case SOUND: return Material.NOTE_BLOCK;
            case PARTICLE: return Material.BLAZE_POWDER;
            case PROJECTILE: return Material.ARROW;
            case TELEPORT: return Material.ENDER_PEARL;
            case LOOP: return Material.REPEATER;
            case CONDITION: return Material.COMPARATOR;
            case SET_VARIABLE: return Material.WRITABLE_BOOK;
            case SELECT_TARGET: return Material.SPYGLASS;
            default: return Material.PAPER;
        }
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