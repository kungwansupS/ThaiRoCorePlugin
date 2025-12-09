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

        // ขยายขนาด GUI เป็น 54 ช่องเพื่อรองรับค่า Status ที่มากขึ้น
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillEditor: " + skillId));

        // --- Row 1: Basic Info & Meta Data ---

        // 0: Display Name
        inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eDisplay Name / ชื่อแสดงผล",
                "§7Current: §f" + skillData.getDisplayName(),
                "§8---------------",
                "§7Click to rename via Chat.",
                "§7คลิกเพื่อเปลี่ยนชื่อ"
        ));

        // 1: Icon
        inv.setItem(1, createGuiItem(skillData.getIcon(), "§bSkill Icon / ไอคอน",
                "§7Current: §f" + skillData.getIcon().name(),
                "§8---------------",
                "§7Drag & Drop item to change.",
                "§7ลากไอเทมมาวางเพื่อเปลี่ยนรูป"
        ));

        // 2: Skill Type (Physical/Magic)
        inv.setItem(2, createGuiItem(Material.IRON_SWORD, "§6Skill Type / ประเภทสกิล",
                "§7Current: §e" + skillData.getSkillType(),
                "§8---------------",
                "§eClick: §7Cycle (Physical/Magic/Mixed/Heal)",
                "§eคลิก: §7เปลี่ยนประเภทสกิล"
        ));

        // 3: Attack Type (Melee/Ranged)
        inv.setItem(3, createGuiItem(Material.BOW, "§6Attack Type / ระยะโจมตี",
                "§7Current: §e" + skillData.getAttackType(),
                "§8---------------",
                "§eClick: §7Cycle (Melee/Ranged)",
                "§eคลิก: §7เปลี่ยนระยะ (ใกล้/ไกล)"
        ));

        // 4: Cast Range
        inv.setItem(4, createGuiItem(Material.COMPASS, "§bCast Range / ระยะร่าย",
                "§7Range: §f" + skillData.getCastRange() + " blocks",
                "§8---------------",
                "§eClick: §7Edit Range",
                "§eคลิก: §7แก้ไขระยะสกิล"
        ));

        // 5: Trigger
        inv.setItem(5, createGuiItem(Material.LEVER, "§cTrigger / เงื่อนไขใช้",
                "§7Current: §e" + skillData.getTrigger().name(),
                "§8---------------",
                "§eClick: §7Change Trigger",
                "§eคลิก: §7เปลี่ยนเงื่อนไข (กดใช้/ตีโดน/ฯลฯ)"
        ));

        // 7: Required Level
        inv.setItem(7, createGuiItem(Material.EXPERIENCE_BOTTLE, "§aReq. Level / เลเวลที่ต้องการ",
                "§7Level: §e" + skillData.getRequiredLevel(),
                "§8---------------",
                "§eClick: §7Edit Level",
                "§eคลิก: §7แก้เลเวล"
        ));

        // 8: SP Cost
        inv.setItem(8, createGuiItem(Material.BLUE_DYE, "§9SP Cost / ค่ามานา",
                "§7Base: §f" + skillData.getSpCostBase(),
                "§7Per Lvl: §f" + skillData.getSpCostPerLevel(),
                "§8---------------",
                "§eL-Click: §7Edit Base",
                "§eR-Click: §7Edit Per Level"
        ));

        // --- Row 2: Advance Mechanics (Cast Time & Delay) ---

        // 9: Cooldown
        inv.setItem(9, createGuiItem(Material.CLOCK, "§7Cooldown / คูลดาวน์",
                "§7Base: §f" + skillData.getCooldownBase() + "s",
                "§7Per Lvl: §f" + skillData.getCooldownPerLevel() + "s",
                "§8---------------",
                "§eL-Click: §7Edit Base",
                "§eR-Click: §7Edit Per Level"
        ));

        // 10: Variable Cast Time
        inv.setItem(10, createGuiItem(Material.SUGAR, "§eVariable Cast / ร่ายแปรผัน",
                "§7(Reduced by DEX/INT)",
                "§7Base Time: §f" + skillData.getVariableCastTime() + "s",
                "§7Reduction: §f" + skillData.getVariableCastTimeReduction() + "%",
                "§8---------------",
                "§7เวลาร่ายที่ลดได้ตามค่าสเตตัส",
                "§eL-Click: §7Edit Time",
                "§eR-Click: §7Edit Reduction %"
        ));

        // 11: Fixed Cast Time
        inv.setItem(11, createGuiItem(Material.HONEY_BLOCK, "§6Fixed Cast / ร่ายคงที่",
                "§7(Cannot be reduced normally)",
                "§7Time: §f" + skillData.getFixedCastTime() + "s",
                "§8---------------",
                "§7เวลาร่ายที่ลดไม่ได้",
                "§eClick: §7Edit Time"
        ));

        // 12: Motions (Pre/Post)
        inv.setItem(12, createGuiItem(Material.FEATHER, "§fMotion Delay / ดีเลย์ท่าทาง",
                "§7Pre-Motion: §f" + skillData.getPreMotion() + "s",
                "§7Post-Motion: §f" + skillData.getPostMotion() + "s",
                "§8---------------",
                "§eL-Click: §7Edit Pre-Motion (ก่อนออก)",
                "§eR-Click: §7Edit Post-Motion (หลังออก)"
        ));

        // 13: After Cast Delay (ACD)
        inv.setItem(13, createGuiItem(Material.BARRIER, "§cAfter Cast Delay (ACD)",
                "§7Global Skill Delay: §f" + skillData.getAfterCastDelayBase() + "s",
                "§8---------------",
                "§7ดีเลย์รวมทุกสกิลหลังใช้ (รัวสกิลไม่ได้)",
                "§eClick: §7Edit ACD"
        ));

        // --- Row 3-5: Action Timeline ---
        // Start showing actions from slot 18
        int slot = 18;
        int index = 0;
        for (SkillAction action : skillData.getActions()) {
            if (slot > 44) break; // Limit display area

            Material mat = Material.PAPER;
            String typeDesc = "Action";
            switch(action.getType()) {
                case DAMAGE: mat = Material.IRON_SWORD; typeDesc = "Damage"; break;
                case HEAL: mat = Material.GOLDEN_APPLE; typeDesc = "Heal"; break;
                case APPLY_EFFECT: mat = Material.POTION; typeDesc = "Effect"; break;
                case SOUND: mat = Material.NOTE_BLOCK; typeDesc = "Sound"; break;
                case PARTICLE: mat = Material.BLAZE_POWDER; typeDesc = "Particle"; break;
                case PROJECTILE: mat = Material.ARROW; typeDesc = "Projectile"; break;
                case TELEPORT: mat = Material.ENDER_PEARL; typeDesc = "Teleport"; break;
                case LOOP: mat = Material.REPEATER; typeDesc = "Loop"; break;
                default: mat = Material.PAPER; break;
            }

            inv.setItem(slot, createGuiItem(mat, "§f#" + (index+1) + ": " + action.getType().name(),
                    "§7Index: " + index,
                    "§7Type: " + typeDesc,
                    "§8---------------",
                    "§eL-Click: §7Edit / แก้ไข",
                    "§eShift+L: §6Move Up / เลื่อนขึ้น",
                    "§eR-Click: §6Move Down / เลื่อนลง",
                    "§eShift+R: §cRemove / ลบ"
            ));

            slot++;
            index++;
        }

        // --- Row 6: Controls ---

        // 49: Save
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE SKILL / บันทึก",
                "§7Save all changes to file.",
                "§7บันทึกการเปลี่ยนแปลง"
        ));

        // 50: Add Action
        inv.setItem(50, createGuiItem(Material.LIME_DYE, "§a§l+ Add Action / เพิ่มคำสั่ง",
                "§7Add logic block.",
                "§7เพิ่ม Action ใหม่ในไทม์ไลน์"
        ));

        // 53: Back
        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack / กลับ",
                "§7Return to menu.",
                "§7ย้อนกลับ"
        ));

        // Fill background
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
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}