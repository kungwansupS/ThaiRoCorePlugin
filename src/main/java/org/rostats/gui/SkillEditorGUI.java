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
    private final SkillData rootSkill;
    private final List<SkillAction> currentList;
    private final int page;
    private final String pathName; // Breadcrumb title

    private static final int ACTIONS_PER_PAGE = 27;

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this(plugin, skillId, plugin.getSkillManager().getSkill(skillId).getActions(), 0, "Main");
    }

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId, int page) {
        this(plugin, skillId, plugin.getSkillManager().getSkill(skillId).getActions(), page, "Main");
    }

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId, List<SkillAction> targetList, int page, String pathName) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.rootSkill = plugin.getSkillManager().getSkill(skillId);
        this.currentList = targetList;
        this.page = page;
        this.pathName = pathName;
    }

    public void open(Player player) {
        if (rootSkill == null) {
            player.sendMessage("§cError: Skill data not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillEditor: " + skillId + " #P" + page));

        // Row 1-2: Meta Data (Only show if at Main level, else show Breadcrumbs/Back)
        if (pathName.equals("Main")) {
            renderMetaData(inv);
        } else {
            inv.setItem(4, createGuiItem(Material.OAK_SIGN, "§eCurrent Path: §f" + pathName, "§7Editing nested actions."));
        }

        // Row 3-5: Action List
        int startIndex = page * ACTIONS_PER_PAGE;
        int endIndex = Math.min(startIndex + ACTIONS_PER_PAGE, currentList.size());

        int slot = 18;
        for (int i = startIndex; i < endIndex; i++) {
            SkillAction action = currentList.get(i);
            inv.setItem(slot, createGuiItem(getActionIcon(action),
                    "§f#" + (i+1) + ": " + action.getType().name(),
                    "§7Index: " + i,
                    "§7Type: " + action.getType().name(),
                    "§8---------------",
                    "§eL-Click: §7Edit Properties",
                    "§eShift+L: §6Move Up",
                    "§eR-Click: §6Move Down / Drill Down (Logic)",
                    "§eShift+R: §cRemove",
                    (action.getType().name().equals("CONDITION") ? "§b(R-Click: Success List | Shift+R: Fail List)" : "")
            ));
            slot++;
        }

        // Controls
        if (page > 0) inv.setItem(45, createGuiItem(Material.ARROW, "§ePrevious Page", "§7Page: " + page));
        if (endIndex < currentList.size()) inv.setItem(53, createGuiItem(Material.ARROW, "§eNext Page", "§7Page: " + (page + 2)));

        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE ROOT", "§7Save the entire skill structure."));
        inv.setItem(50, createGuiItem(Material.LIME_DYE, "§a§l+ Add Action", "§7Add to CURRENT list."));
        inv.setItem(48, createGuiItem(Material.BOOK, "§cBack / Up", "§7Go up one level or exit."));

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private void renderMetaData(Inventory inv) {
        inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eDisplay Name", "§7Current: §f" + rootSkill.getDisplayName()));
        inv.setItem(1, createGuiItem(rootSkill.getIcon(), "§bIcon", "§7Current: §f" + rootSkill.getIcon().name()));
        inv.setItem(2, createGuiItem(Material.IRON_SWORD, "§6Skill Type", "§7Current: §e" + rootSkill.getSkillType()));
        inv.setItem(3, createGuiItem(Material.BOW, "§6Attack Type", "§7Current: §e" + rootSkill.getAttackType()));
        inv.setItem(4, createGuiItem(Material.COMPASS, "§bCast Range", "§7Range: §f" + rootSkill.getCastRange()));
        inv.setItem(5, createGuiItem(Material.LEVER, "§cTrigger", "§7Current: §e" + rootSkill.getTrigger().name()));
        inv.setItem(6, createGuiItem(Material.CLOCK, "§7Cooldown", "§7Base: §f" + rootSkill.getCooldownBase(), "§7Per Lvl: §f" + rootSkill.getCooldownPerLevel()));
        inv.setItem(7, createGuiItem(Material.EXPERIENCE_BOTTLE, "§aReq. Level", "§7Level: §e" + rootSkill.getRequiredLevel()));
        inv.setItem(8, createGuiItem(Material.BLUE_DYE, "§9SP Cost", "§7Base: §f" + rootSkill.getSpCostBase(), "§7Per Lvl: §f" + rootSkill.getSpCostPerLevel()));

        inv.setItem(10, createGuiItem(Material.SUGAR, "§eVariable Cast", "§7Base: " + rootSkill.getVariableCastTime(), "§7Reduce%: " + rootSkill.getVariableCastTimeReduction()));
        inv.setItem(11, createGuiItem(Material.HONEY_BLOCK, "§6Fixed Cast", "§7Base: " + rootSkill.getFixedCastTime()));
        inv.setItem(12, createGuiItem(Material.FEATHER, "§fMotion", "§7Pre: " + rootSkill.getPreMotion(), "§7Post: " + rootSkill.getPostMotion()));
        inv.setItem(13, createGuiItem(Material.BARRIER, "§cACD", "§7Delay: " + rootSkill.getAfterCastDelayBase()));
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