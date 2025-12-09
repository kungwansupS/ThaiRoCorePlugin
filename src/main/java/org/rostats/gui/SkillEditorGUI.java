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
import org.rostats.engine.trigger.TriggerType;

import java.util.Arrays;
import java.util.List;

public class SkillEditorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;
    private final SkillData skillData;

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.skillData = plugin.getSkillManager().getSkill(skillId);
    }

    public void open(Player player) {
        if (skillData == null) {
            player.sendMessage("§cError: Skill data not found!");
            player.closeInventory();
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillEditor: " + skillId));

        // --- Row 1: Basic Info ---

        // 0: Rename
        inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eDisplay Name / ชื่อแสดงผล",
                "§7Current: §f" + skillData.getDisplayName(),
                "§8---------------",
                "§7Click to rename via Chat.",
                "§7คลิกเพื่อเปลี่ยนชื่อผ่านช่องแชท"
        ));

        // 2: Trigger
        inv.setItem(2, createGuiItem(Material.LEVER, "§6Trigger / เงื่อนไขการทำงาน",
                "§7Current: §e" + skillData.getTrigger().name(),
                "§8---------------",
                "§7Click to cycle trigger types.",
                "§7(CAST, ON_HIT, ON_DEFEND, etc.)",
                "§8---------------",
                "§7คลิกเพื่อเปลี่ยนเงื่อนไขการทำงาน",
                "§7(เช่น กดใช้, ตีโดน, ถูกตี)"
        ));

        // 4: Icon (Center)
        inv.setItem(4, createGuiItem(skillData.getIcon(), "§bSkill Icon / ไอคอน",
                "§7Current: §f" + skillData.getIcon().name(),
                "§8---------------",
                "§7Drag & Drop an item here to change icon.",
                "§8---------------",
                "§7ลากไอเทมจากตัวคุณมาวางทับ",
                "§7เพื่อเปลี่ยนรูปไอคอนของสกิล"
        ));

        // 6: Cooldown & Cast
        inv.setItem(6, createGuiItem(Material.CLOCK, "§aTiming / เวลา",
                "§7Cooldown: §f" + skillData.getCooldownBase() + "s",
                "§7Cast Time: §f" + skillData.getCastTime() + "s",
                "§8---------------",
                "§eLeft Click: §7Edit Cooldown",
                "§eRight Click: §7Edit Cast Time",
                "§8---------------",
                "§eคลิกซ้าย: §7แก้คูลดาวน์",
                "§eคลิกขวา: §7แก้เวลาร่าย"
        ));

        // 7: [NEW] Required Level
        inv.setItem(7, createGuiItem(Material.EXPERIENCE_BOTTLE, "§aRequirements / เงื่อนไข",
                "§7Required Level: §e" + skillData.getRequiredLevel(),
                "§8---------------",
                "§eClick: §7Edit Required Level",
                "§8---------------",
                "§eคลิก: §7แก้เลเวลที่ต้องการ"
        ));

        // 8: Cost
        inv.setItem(8, createGuiItem(Material.POTION, "§dCost / ค่าใช้จ่าย",
                "§7SP Cost: §f" + skillData.getSpCostBase(),
                "§8---------------",
                "§eLeft Click: §7Edit SP Cost",
                "§8---------------",
                "§eคลิกซ้าย: §7แก้ค่า SP ที่ใช้"
        ));

        // --- Row 2-5: Action Timeline ---
        // Render added actions
        int slot = 18;
        int index = 0;
        for (SkillAction action : skillData.getActions()) {
            if (slot > 44) break; // Limit display

            Material mat = Material.PAPER;
            switch(action.getType()) {
                case DAMAGE: mat = Material.IRON_SWORD; break;
                case HEAL: mat = Material.GOLDEN_APPLE; break;
                case APPLY_EFFECT: mat = Material.POTION; break;
                case SOUND: mat = Material.NOTE_BLOCK; break;
                case PARTICLE: mat = Material.BLAZE_POWDER; break;
                case PROJECTILE: mat = Material.ARROW; break;
                case AREA_EFFECT: mat = Material.TNT; break;
                default: mat = Material.PAPER; break;
            }

            inv.setItem(slot, createGuiItem(mat, "§fAction #" + (index+1) + ": " + action.getType().name(),
                    "§7Index: " + index,
                    "§8---------------",
                    "§eL-Click: §7Edit Properties / แก้ไข",
                    "§eShift+L: §6Move Left/Up / เลื่อนขึ้น",
                    "§eR-Click: §6Move Right/Down / เลื่อนลง",
                    "§eShift+R: §cRemove / ลบ",
                    "§8---------------"
            ));

            slot++;
            index++;
        }

        // --- Row 6: Controls ---

        // 49: Save
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE SKILL / บันทึก",
                "§7Save all changes to file.",
                "§8---------------",
                "§7บันทึกข้อมูลลงไฟล์"
        ));

        // 50: Add Action
        inv.setItem(50, createGuiItem(Material.LIME_DYE, "§a§l+ Add Action / เพิ่มการกระทำ",
                "§7Add a new logic block (Damage, Effect, Sound).",
                "§8---------------",
                "§7เพิ่มคำสั่งใหม่ (เช่น ดาเมจ, เอฟเฟกต์, เสียง)"
        ));

        // 53: Back
        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack / กลับ",
                "§7Return to Skill Library.",
                "§8---------------",
                "§7กลับไปหน้าคลังสกิล"
        ));

        // Fill bg
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        // Removed HIDE_POTION_EFFECTS to fix compatibility
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}