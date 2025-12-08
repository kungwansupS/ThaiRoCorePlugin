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
import org.rostats.engine.action.ActionType;

import java.util.Arrays;
import java.util.List;

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

        inv.setItem(slot++, createGuiItem(Material.IRON_SWORD, "§cDAMAGE / สร้างความเสียหาย",
                "§7Deal damage to target.", "§8---------------", "§7ทำดาเมจใส่เป้าหมาย", "ActionType: DAMAGE"));

        inv.setItem(slot++, createGuiItem(Material.GOLDEN_APPLE, "§cHEAL / ฟื้นฟู",
                "§7Heal HP or SP.", "§8---------------", "§7ฟื้นฟูเลือดหรือมานา", "ActionType: HEAL"));

        inv.setItem(slot++, createGuiItem(Material.POTION, "§9APPLY_EFFECT / สถานะ",
                "§7Apply buff/debuff/dot.", "§8---------------", "§7ยัดสถานะใส่เป้าหมาย", "ActionType: APPLY_EFFECT"));

        // --- Visual Actions (ACTIVE) ---
        inv.setItem(slot++, createGuiItem(Material.NOTE_BLOCK, "§eSOUND / เสียง",
                "§7Play sound effect.", "§8---------------", "§7เล่นเสียงเอฟเฟกต์", "ActionType: SOUND"));

        inv.setItem(slot++, createGuiItem(Material.BLAZE_POWDER, "§ePARTICLE / ละออง",
                "§7Play particle effect.", "§8---------------", "§7แสดงเอฟเฟกต์ละออง", "ActionType: PARTICLE"));

        inv.setItem(slot++, createGuiItem(Material.ARROW, "§bPROJECTILE / ลูกพลัง",
                "§7Launch projectile.", "§8---------------", "§7ยิงลูกพลังออกไป", "ActionType: PROJECTILE"));

        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack / กลับ", "§7Cancel selection"));

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
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}