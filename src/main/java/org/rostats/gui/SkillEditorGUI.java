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

        // --- Row 1: Basic Info (ข้อมูลพื้นฐาน) ---

        // 0: Rename
        inv.setItem(0, createGuiItem(Material.NAME_TAG, "§eชื่อสกิล (Display Name)",
                "§7ปัจจุบัน: §f" + skillData.getDisplayName(),
                "§8---------------",
                "§eคลิกซ้าย: §7เปลี่ยนชื่อสกิล (พิมพ์ในแชท)",
                "§7(ใช้สัญลักษณ์ & เพื่อใส่สีได้)"
        ));

        // 1: Max Level
        inv.setItem(1, createGuiItem(Material.LADDER, "§bเลเวลสูงสุด (Max Level)",
                "§7ปัจจุบัน: §f" + skillData.getMaxLevel(),
                "§8---------------",
                "§eคลิกซ้าย: §7แก้ไขเลเวลสูงสุด",
                "§7(กำหนดเพดานเลเวลของสกิล)"
        ));

        // 2: Trigger
        inv.setItem(2, createGuiItem(Material.LEVER, "§6เงื่อนไขการทำงาน (Trigger)",
                "§7ปัจจุบัน: §e" + skillData.getTrigger().name(),
                "§8---------------",
                "§eคลิกซ้าย: §7เปลี่ยนเงื่อนไข",
                "§f- CAST: §7กดใช้ปกติ",
                "§f- ON_HIT: §7เมื่อโจมตีโดน",
                "§f- ON_DEFEND: §7เมื่อถูกโจมตี",
                "§f- PASSIVE...: §7สกิลติดตัว"
        ));

        // 3: Meta (Type & Range)
        inv.setItem(3, createGuiItem(Material.OAK_SIGN, "§fประเภทและระยะ (Type & Range)",
                "§7ประเภทความเสียหาย: §b" + skillData.getSkillType(),
                "§7รูปแบบการโจมตี: §e" + skillData.getAttackType(),
                "§7ระยะร่าย (Range): §a" + skillData.getCastRange() + " เมตร",
                "§8---------------",
                "§eคลิกซ้าย: §7แก้ไขระยะร่าย (Range)",
                "§eคลิกขวา: §7เปลี่ยนรูปแบบ (ใกล้/ไกล)",
                "§eShift+ขวา: §7เปลี่ยนประเภท (กายภาพ/เวท)"
        ));

        // 4: Icon (Center)
        inv.setItem(4, createGuiItem(skillData.getIcon(), "§bไอคอนสกิล (Icon)",
                "§7ปัจจุบัน: §f" + skillData.getIcon().name(),
                "§8---------------",
                "§eวิธีเปลี่ยน:",
                "§7ลากไอเทมจากกระเป๋าคุณ",
                "§7มาวางทับที่ช่องนี้เพื่อเปลี่ยนรูป"
        ));

        // 5: Animation & Delay
        inv.setItem(5, createGuiItem(Material.COMPASS, "§6อนิเมชันและดีเลย์ (Motion & Delay)",
                "§7ดีเลย์หลังร่าย (ACD): §f" + skillData.getAfterCastDelayBase() + "วิ",
                "§7ท่าทางหลังร่าย (Post-Motion): §f" + skillData.getPostMotion() + "วิ",
                "§7ท่าทางก่อนร่าย (Pre-Motion): §f" + skillData.getPreMotion() + "วิ",
                "§8---------------",
                "§eคลิกซ้าย: §7แก้ไข ACD (ดีเลย์สกิลต่อเนื่อง)",
                "§eคลิกขวา: §7แก้ไข Post-Motion (ขยับไม่ได้หลังร่าย)",
                "§eShift+ขวา: §7แก้ไข Pre-Motion (ท่าง้าง)"
        ));

        // 6: Cooldown & Cast
        inv.setItem(6, createGuiItem(Material.CLOCK, "§aเวลาและการร่าย (Timing)",
                "§7คูลดาวน์: §f" + skillData.getCooldownBase() + "s §7(ต่อเลเวล: " + skillData.getCooldownPerLevel() + "s)",
                "§7ร่ายแปรผัน (ลดได้): §f" + skillData.getVariableCastTime() + "s",
                "§7ร่ายคงที่ (ลดไม่ได้): §f" + skillData.getFixedCastTime() + "s",
                "§8---------------",
                "§eคลิกซ้าย: §7แก้คูลดาวน์พื้นฐาน",
                "§6Shift+ซ้าย: §7แก้คูลดาวน์ต่อเลเวล",
                "§eคลิกขวา: §7แก้เวลาร่ายแปรผัน (Variable)",
                "§eShift+ขวา: §7แก้เวลาร่ายคงที่ (Fixed)"
        ));

        // 7: Required Level
        inv.setItem(7, createGuiItem(Material.EXPERIENCE_BOTTLE, "§aเงื่อนไขเลเวล (Req. Level)",
                "§7เลเวลตัวละครขั้นต่ำ: §e" + skillData.getRequiredLevel(),
                "§8---------------",
                "§eคลิกซ้าย: §7แก้ไขเลเวลที่ต้องการ"
        ));

        // 8: Cost
        inv.setItem(8, createGuiItem(Material.POTION, "§dค่าใช้จ่าย (Cost)",
                "§7SP ที่ใช้: §f" + skillData.getSpCostBase() + " §7(ต่อเลเวล: " + skillData.getSpCostPerLevel() + ")",
                "§8---------------",
                "§eคลิกซ้าย: §7แก้ SP พื้นฐาน",
                "§6Shift+ซ้าย: §7แก้ SP ต่อเลเวล"
        ));

        // --- Row 2-5: Action Timeline ---
        int slot = 18;
        int index = 0;
        for (SkillAction action : skillData.getActions()) {
            if (slot > 44) break;

            Material mat = Material.PAPER;
            String nameTH = "";
            switch(action.getType()) {
                case DAMAGE: mat = Material.IRON_SWORD; nameTH = "สร้างความเสียหาย"; break;
                case HEAL: mat = Material.GOLDEN_APPLE; nameTH = "ฟื้นฟู (Heal)"; break;
                case APPLY_EFFECT: mat = Material.POTION; nameTH = "ให้สถานะ (Buff/Debuff)"; break;
                case SOUND: mat = Material.NOTE_BLOCK; nameTH = "เล่นเสียง"; break;
                case PARTICLE: mat = Material.BLAZE_POWDER; nameTH = "เอฟเฟกต์ (Particle)"; break;
                case PROJECTILE: mat = Material.ARROW; nameTH = "ยิงกระสุน"; break;
                case AREA_EFFECT: mat = Material.TNT; nameTH = "พื้นที่วงกว้าง (AOE)"; break;
                case TELEPORT: mat = Material.ENDER_PEARL; nameTH = "เทเลพอร์ต"; break;
                case POTION: mat = Material.GLASS_BOTTLE; nameTH = "ยา (Potion)"; break;
                case COMMAND: mat = Material.COMMAND_BLOCK; nameTH = "คำสั่งเซิฟเวอร์"; break;
                default: mat = Material.PAPER; nameTH = action.getType().name(); break;
            }

            inv.setItem(slot, createGuiItem(mat, "§f#" + (index+1) + " " + nameTH,
                    "§7Type: " + action.getType().name(),
                    "§8---------------",
                    "§eคลิกซ้าย: §7แก้ไขค่าต่างๆ",
                    "§6Shift+ซ้าย: §6เลื่อนขึ้น / ไปซ้าย",
                    "§eคลิกขวา: §6เลื่อนลง / ไปขวา",
                    "§cShift+ขวา: §cลบรายการนี้",
                    "§8---------------"
            ));

            slot++;
            index++;
        }

        // --- Row 6: Controls ---

        // 49: Save
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lบันทึกข้อมูล (SAVE)",
                "§7บันทึกการเปลี่ยนแปลงทั้งหมดลงไฟล์",
                "§8---------------",
                "§7(อย่าลืมกดบันทึกทุกครั้ง)"
        ));

        // 50: Add Action
        inv.setItem(50, createGuiItem(Material.LIME_DYE, "§a§l+ เพิ่มคำสั่ง (Add Action)",
                "§7เพิ่มการกระทำใหม่ให้สกิลนี้",
                "§7เช่น ดาเมจ, เสียง, หรือเอฟเฟกต์",
                "§8---------------"
        ));

        // 53: Back
        inv.setItem(53, createGuiItem(Material.ARROW, "§cกลับ (Back)",
                "§7กลับไปหน้าคลังสกิล",
                "§8---------------"
        ));

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