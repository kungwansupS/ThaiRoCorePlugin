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
    private final List<SkillAction> actions;
    private final int page;
    private final String titleSuffix;

    // Constructor หลัก
    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this(plugin, skillId, 0);
    }

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId, int page) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.page = page;
        SkillData data = plugin.getSkillManager().getSkill(skillId);
        this.actions = (data != null) ? data.getActions() : null;
        this.titleSuffix = "Main";
    }

    // Constructor สำหรับ Nested List (Loop, Condition)
    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId, List<SkillAction> actions, int page, String suffix) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.actions = actions;
        this.page = page;
        this.titleSuffix = suffix;
    }

    public void open(Player player) {
        if (actions == null) {
            player.sendMessage("§cSkill data not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillEditor: " + skillId + " #P" + page + " (" + titleSuffix + ")"));

        // --- Meta Data Row (Top) ---
        SkillData root = plugin.getSkillManager().getSkill(skillId);
        if (root != null) {
            inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eName: §f" + root.getDisplayName(), "§7Click to rename"));
            inv.setItem(1, createGuiItem(root.getIcon(), "§eIcon: §f" + root.getIcon(), "§7Drag item here to change"));
            inv.setItem(2, createGuiItem(Material.DIAMOND_SWORD, "§eType: §f" + root.getSkillType(), "§7Click to toggle"));
            inv.setItem(3, createGuiItem(Material.BOW, "§eAttack: §f" + root.getAttackType(), "§7Click to toggle"));
            inv.setItem(4, createGuiItem(Material.COMPASS, "§eRange: §f" + root.getCastRange(), "§7Click to edit"));
            inv.setItem(5, createGuiItem(Material.LEVER, "§eTrigger: §f" + root.getTrigger(), "§7Click to toggle"));

            // Cooldown & Cost
            inv.setItem(6, createGuiItem(Material.CLOCK, "§eCooldown",
                    "§7Base: §f" + root.getCooldownBase(),
                    "§7Per Lvl: §f" + root.getCooldownPerLevel(),
                    "§eL-Click: Base | R-Click: Lvl"));

            inv.setItem(7, createGuiItem(Material.EXPERIENCE_BOTTLE, "§eReq Level: §f" + root.getRequiredLevel(), "§7Click to edit"));

            inv.setItem(8, createGuiItem(Material.LAPIS_LAZULI, "§eSP Cost",
                    "§7Base: §f" + root.getSpCostBase(),
                    "§7Per Lvl: §f" + root.getSpCostPerLevel(),
                    "§eL-Click: Base | R-Click: Lvl"));

            // Row 2: Casting & Delay
            inv.setItem(10, createGuiItem(Material.CLOCK, "§eVar Cast", "§7Time: " + root.getVariableCastTime(), "§7Reduct: " + root.getVariableCastTimeReduction() + "%", "§eL: Time | R: Reduct"));
            inv.setItem(11, createGuiItem(Material.OBSERVER, "§eFixed Cast", "§7Time: " + root.getFixedCastTime()));
            inv.setItem(12, createGuiItem(Material.FEATHER, "§eMotion", "§7Pre: " + root.getPreMotion(), "§7Post: " + root.getPostMotion(), "§eL: Pre | R: Post"));
            inv.setItem(13, createGuiItem(Material.REPEATER, "§eACD (Delay)", "§7Time: " + root.getAfterCastDelayBase()));
        }

        // --- Action List (Center) ---
        int start = page * 27;
        for (int i = 0; i < 27; i++) {
            int index = start + i;
            if (index >= actions.size()) break;

            SkillAction action = actions.get(index);
            String typeName = action.getType().name();
            Material icon = getActionIcon(action);

            inv.setItem(18 + i, createGuiItem(icon, "§6[" + index + "] " + typeName,
                    "§7" + action.serialize().toString(),
                    "§8------------------",
                    "§eL-Click: Edit Properties",
                    "§eR-Click: Move Down",
                    "§eShift+L: Move Up",
                    "§cShift+R: Delete",
                    (action.getType().name().equals("CONDITION") || action.getType().name().equals("LOOP")) ? "§bRight-Click: Edit Nested Actions" : ""
            ));
        }

        // --- Controls (Bottom) ---
        inv.setItem(45, createGuiItem(Material.ARROW, "§cPrevious Page"));

        // [FIXED] Back logic handled in GUIListener
        inv.setItem(48, createGuiItem(Material.OAK_DOOR, "§eBack / Up", "§7Go to parent list or library"));

        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE SKILL", "§7Save to file"));
        inv.setItem(50, createGuiItem(Material.NETHER_STAR, "§b§lADD ACTION", "§7Add new action to end"));

        inv.setItem(53, createGuiItem(Material.ARROW, "§aNext Page"));

        // Fill background
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private Material getActionIcon(SkillAction action) {
        return switch (action.getType()) {
            case DAMAGE -> Material.IRON_SWORD;
            case HEAL -> Material.GOLDEN_APPLE;
            case CONDITION -> Material.COMPARATOR;
            case LOOP -> Material.REPEATER;
            case SOUND -> Material.NOTE_BLOCK;
            case PARTICLE -> Material.BLAZE_POWDER;
            case APPLY_EFFECT -> Material.POTION;
            default -> Material.PAPER;
        };
    }

    // [FIX] Helper Method ที่ถูกต้องสำหรับ List<String>
    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore)); // แปลง array เป็น list
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}