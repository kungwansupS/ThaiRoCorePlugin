package org.rostats.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.skill.SkillData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillEditorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;

    public SkillEditorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this.plugin = plugin;
        this.skillId = skillId;
    }

    public void open(Player player) {
        SkillData skillData = plugin.getSkillManager().getSkill(skillId);
        if (skillData == null) {
            player.sendMessage("§cSkill not found: " + skillId);
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, "§8§l[§6Skill Editor§8] §f" + skillData.getDisplayName());

        // --- Row 1: Metadata & Settings ---

        // 0: Display Name
        inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eDisplay Name / ชื่อสกิล",
                "§7Current: §f" + skillData.getDisplayName(),
                "§8---------------",
                "§7คลิกเพื่อเปลี่ยนชื่อที่แสดง"
        ));

        // 1: Max Level
        inv.setItem(1, createGuiItem(Material.EXPERIENCE_BOTTLE, "§aMax Level / เลเวลสูงสุด",
                "§7Current: §f" + skillData.getMaxLevel(),
                "§8---------------",
                "§7คลิกเพื่อเปลี่ยนเลเวลสูงสุดของสกิล"
        ));

        // 2: Trigger
        inv.setItem(2, createGuiItem(Material.COMPARATOR, "§bTrigger Type / เงื่อนไขการทำงาน",
                "§7Current: §f" + skillData.getTrigger().name(),
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

        // 6: Cooldown & Cast & GCD (UPDATED)
        inv.setItem(6, createGuiItem(Material.CLOCK, "§aTiming / เวลา",
                "§7Cooldown: §f" + skillData.getCooldownBase() + "s",
                "§7Global CD: §e" + skillData.getGlobalCooldownBase() + "s",
                "§7Cast Time: §f" + skillData.getCastTime() + "s",
                "§8---------------",
                "§eLeft Click: §7Edit Cooldown",
                "§eRight Click: §7Edit Cast Time",
                "§eShift + Left: §7Edit Global CD",
                "§8---------------",
                "§eคลิกซ้าย: §7แก้คูลดาวน์",
                "§eคลิกขวา: §7แก้เวลาร่าย",
                "§eShift + คลิกซ้าย: §7แก้ Global CD"
        ));

        // 7: Required Level
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
        int slot = 18;
        int index = 0;
        for (SkillAction action : skillData.getActions()) {
            if (slot > 44) break;

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

        // 45: Add Action
        inv.setItem(45, createGuiItem(Material.LIME_DYE, "§a§l+ ADD ACTION",
                "§7เพิ่ม Action ใหม่เข้าไปใน Skill",
                "§8---------------",
                "§7คลิกเพื่อเลือกประเภท Action"
        ));

        // 49: Save
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE SKILL / บันทึก",
                "§7Save all changes to file.",
                "§8---------------",
                "§7คลิกเพื่อบันทึกสกิลนี้"
        ));

        // 53: Back
        inv.setItem(53, createGuiItem(Material.ARROW, "§c§lBACK / กลับ",
                "§7กลับไปหน้าจัดการสกิล"
        ));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}