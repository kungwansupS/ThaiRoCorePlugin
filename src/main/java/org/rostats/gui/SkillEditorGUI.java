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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map; // [FIX] เพิ่มบรรทัดนี้

public class SkillEditorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;
    private final List<SkillAction> actionList;
    private final int page;
    private final String listName;

    // Constructor สำหรับ GUIListener เรียกใช้ (Default Page 0)
    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this(plugin, skillId, 0);
    }

    // Constructor ระบุหน้า
    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId, int page) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.page = page;
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        this.actionList = (skill != null) ? skill.getActions() : new ArrayList<>();
        this.listName = "Main";
    }

    // Full Constructor (สำหรับ Nested List เช่น Loop/Condition)
    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId, List<SkillAction> list, int page, String listName) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.actionList = list;
        this.page = page;
        this.listName = listName;
    }

    public void open(Player player) {
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) {
            player.sendMessage("§cSkill not found!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillEditor: " + skillId + " #P" + page));

        // --- Meta Data Row (Top) ---
        if (listName.equals("Main")) {
            inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eDisplay Name", "§7" + skill.getDisplayName()));
            inv.setItem(1, createGuiItem(skill.getIcon(), "§eIcon", "§7Click item in inventory to set"));
            inv.setItem(2, createGuiItem(Material.DIAMOND_SWORD, "§eType", "§7" + skill.getSkillType()));
            inv.setItem(3, createGuiItem(Material.BOW, "§eAttack Type", "§7" + skill.getAttackType()));
            inv.setItem(4, createGuiItem(Material.ENDER_PEARL, "§eRange", "§7" + skill.getCastRange() + "m"));
            inv.setItem(5, createGuiItem(Material.LEVER, "§eTrigger", "§7" + skill.getTrigger()));

            inv.setItem(6, createGuiItem(Material.CLOCK, "§eCooldown",
                    "§7Base: " + skill.getCooldownBase(), "§7Per Lvl: " + skill.getCooldownPerLevel(), "§eL-Click: Base, R-Click: Lvl"));
            inv.setItem(7, createGuiItem(Material.EXPERIENCE_BOTTLE, "§eReq Level", "§7" + skill.getRequiredLevel()));
            inv.setItem(8, createGuiItem(Material.BLUE_DYE, "§eMana Cost",
                    "§7Base: " + skill.getSpCostBase(), "§7Per Lvl: " + skill.getSpCostPerLevel(), "§eL-Click: Base, R-Click: Lvl"));

            // Row 2: Advanced Cast Time
            inv.setItem(10, createGuiItem(Material.CLOCK, "§eVariable Cast", "§7Time: " + skill.getVariableCastTime(), "§7Reduct%: " + skill.getVariableCastTimeReduction()));
            inv.setItem(11, createGuiItem(Material.CLOCK, "§eFixed Cast", "§7Time: " + skill.getFixedCastTime(), "§7Reduct%: " + skill.getFixedCastTimeReduction()));
            inv.setItem(12, createGuiItem(Material.FEATHER, "§eMotion", "§7Pre: " + skill.getPreMotion(), "§7Post: " + skill.getPostMotion()));
            inv.setItem(13, createGuiItem(Material.BARRIER, "§eAfter Cast Delay", "§7Duration: " + skill.getAfterCastDelayBase()));
        } else {
            // Nested List Header
            inv.setItem(4, createGuiItem(Material.PAPER, "§eEditing: " + listName, "§7Inside nested action list"));
        }

        // --- Action List (Middle) ---
        int start = page * 27;
        for (int i = 0; i < 27; i++) {
            int index = start + i;
            if (index < actionList.size()) {
                SkillAction action = actionList.get(index);
                inv.setItem(18 + i, createActionItem(action, index));
            }
        }

        // --- Controls (Bottom) ---
        if (page > 0) inv.setItem(45, createGuiItem(Material.ARROW, "§ePrevious Page"));
        if ((page + 1) * 27 < actionList.size()) inv.setItem(53, createGuiItem(Material.ARROW, "§eNext Page"));

        inv.setItem(48, createGuiItem(Material.ARROW, "§cBack / Up", "§7Go back to Library/Root"));
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE SKILL", "§7Save to file"));
        inv.setItem(50, createGuiItem(Material.CHEST, "§eAdd Action", "§7Click to add new action"));

        // Fill BG
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) { if (inv.getItem(i) == null) inv.setItem(i, bg); }

        player.openInventory(inv);
    }

    private ItemStack createActionItem(SkillAction action, int index) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bAction #" + (index + 1));
        List<String> lore = new ArrayList<>();
        lore.add("§7Type: " + action.getType());

        // Show brief details
        Map<String, Object> data = action.serialize();
        // [FIXED] ตอนนี้รู้จัก Map และ forEach แล้ว
        data.forEach((k, v) -> {
            if (!k.equals("type") && !k.equals("actions") && !k.equals("success") && !k.equals("fail")) {
                lore.add("§7" + k + ": §f" + v);
            }
        });

        lore.add("");
        lore.add("§eLeft-Click to Edit");
        lore.add("§eRight-Click to Move Down");
        lore.add("§eShift+Left to Move Up");
        lore.add("§cShift+Right to Delete");

        // Special Hints for Nested Actions
        if (action.getType().name().equals("CONDITION") || action.getType().name().equals("LOOP")) {
            lore.add("§6Right-Click to Edit Inner Actions");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}