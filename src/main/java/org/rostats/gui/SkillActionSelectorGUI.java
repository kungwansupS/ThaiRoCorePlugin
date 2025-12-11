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

import java.util.Arrays;

public class SkillActionSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;

    public SkillActionSelectorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this.plugin = plugin;
        this.skillId = skillId;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ActionSelector: " + skillId));
        int slot = 0;

        // 1. DAMAGE
        inv.setItem(slot++, createGuiItem(Material.IRON_SWORD, "§cDAMAGE",
                "§7Deal damage based on formula.",
                "§7สร้างความเสียหายตามสูตรคำนวณ",
                "ActionType: DAMAGE"));

        // 2. HEAL
        inv.setItem(slot++, createGuiItem(Material.GOLDEN_APPLE, "§aHEAL",
                "§7Heal HP or Recover SP.",
                "§7ฟื้นฟูเลือด (HP) หรือมานา (SP)",
                "ActionType: HEAL"));

        // 3. APPLY_EFFECT
        inv.setItem(slot++, createGuiItem(Material.POTION, "§9APPLY_EFFECT",
                "§7Apply Buffs/Debuffs/Stats.",
                "§7เพิ่มค่าสถานะ, บัฟ หรือดีบัฟให้เป้าหมาย",
                "ActionType: APPLY_EFFECT"));

        // 4. SOUND
        inv.setItem(slot++, createGuiItem(Material.NOTE_BLOCK, "§eSOUND",
                "§7Play a sound effect.",
                "§7เล่นเสียงเอฟเฟกต์ที่กำหนด",
                "ActionType: SOUND"));

        // 5. PARTICLE
        inv.setItem(slot++, createGuiItem(Material.BLAZE_POWDER, "§6PARTICLE",
                "§7Show particle effects.",
                "§7แสดงเอฟเฟกต์อนุภาค (Particle)",
                "ActionType: PARTICLE"));

        // 6. PROJECTILE
        inv.setItem(slot++, createGuiItem(Material.ARROW, "§bPROJECTILE",
                "§7Shoot a projectile (Arrow, Fireball).",
                "§7ยิงวัตถุออกไป (เช่น ธนู, ลูกไฟ)",
                "ActionType: PROJECTILE"));

        // 7. TELEPORT
        inv.setItem(slot++, createGuiItem(Material.ENDER_PEARL, "§dTELEPORT",
                "§7Teleport caster or target.",
                "§7วาร์ป, พุ่งตัว (Dash) หรือสลับตำแหน่ง",
                "ActionType: TELEPORT"));

        // 8. POTION
        inv.setItem(slot++, createGuiItem(Material.GLASS_BOTTLE, "§fPOTION",
                "§7Apply Vanilla Potion Effect.",
                "§7ให้ผลของน้ำยา Minecraft ปกติ (เช่น Speed)",
                "ActionType: POTION"));

        // 9. AREA_EFFECT
        inv.setItem(slot++, createGuiItem(Material.TNT, "§c§lAREA EFFECT",
                "§7Trigger skills on nearby entities.",
                "§7(AOE Damage, Buffs, etc.)",
                "§7ใช้งานสกิลย่อยกับเป้าหมายรอบตัว (AOE)",
                "ActionType: AREA_EFFECT"));

        // 10. RAYCAST
        inv.setItem(slot++, createGuiItem(Material.DIAMOND_SWORD, "§6RAYCAST",
                "§7Line-of-sight / Hitscan target.",
                "§7ยิงสกิลแบบเล็งเป้า (Hitscan) หรือ AOE ระยะไกล",
                "ActionType: RAYCAST"));

        // 11. SPAWN_ENTITY
        inv.setItem(slot++, createGuiItem(Material.EGG, "§6SPAWN_ENTITY",
                "§7Spawn Mob or Lightning Bolt.",
                "§7เสกมอนสเตอร์, สัตว์ หรือฟ้าผ่า",
                "ActionType: SPAWN_ENTITY"));

        // 12. VELOCITY
        inv.setItem(slot++, createGuiItem(Material.FEATHER, "§fVELOCITY",
                "§7Apply force to push/pull.",
                "§7กระแทก, ผลัก หรือดึงเป้าหมาย",
                "ActionType: VELOCITY"));

        // 13. LOOP
        inv.setItem(slot++, createGuiItem(Material.REPEATER, "§aLOOP",
                "§7Repeat sub-actions multiple times.",
                "§7วนทำซ้ำคำสั่งย่อย (Loop)",
                "ActionType: LOOP"));

        // 14. COMMAND
        inv.setItem(slot++, createGuiItem(Material.COMMAND_BLOCK, "§7COMMAND",
                "§7Run Console/Player command.",
                "§7รันคำสั่ง Console หรือคำสั่งผู้เล่น",
                "ActionType: COMMAND"));

        // Cancel Button
        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack / ยกเลิก", "§7Cancel adding action."));

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) { if (inv.getItem(i) == null) inv.setItem(i, bg); }
        player.openInventory(inv);
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