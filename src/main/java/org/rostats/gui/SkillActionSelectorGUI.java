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

        inv.setItem(slot++, createGuiItem(Material.IRON_SWORD, "§cDAMAGE", "§7Deal damage.", "ActionType: DAMAGE"));
        inv.setItem(slot++, createGuiItem(Material.GOLDEN_APPLE, "§cHEAL", "§7Heal HP/SP.", "ActionType: HEAL"));
        inv.setItem(slot++, createGuiItem(Material.POTION, "§9APPLY_EFFECT", "§7Apply stats/dot.", "ActionType: APPLY_EFFECT"));

        inv.setItem(slot++, createGuiItem(Material.NOTE_BLOCK, "§eSOUND", "§7Play sound.", "ActionType: SOUND"));
        inv.setItem(slot++, createGuiItem(Material.BLAZE_POWDER, "§ePARTICLE", "§7Show particles.", "ActionType: PARTICLE"));
        inv.setItem(slot++, createGuiItem(Material.ARROW, "§bPROJECTILE", "§7Shoot projectile.", "ActionType: PROJECTILE"));

        inv.setItem(slot++, createGuiItem(Material.ENDER_PEARL, "§dTELEPORT", "§7Dash/Warp.", "ActionType: TELEPORT"));
        inv.setItem(slot++, createGuiItem(Material.GLASS_BOTTLE, "§fPOTION", "§7Vanilla Potion.", "ActionType: POTION"));

        inv.setItem(slot++, createGuiItem(Material.TNT, "§c§lAREA EFFECT",
                "§7Trigger skills on nearby targets.",
                "§7(AOE Damage, Buffs, etc.)",
                "ActionType: AREA_EFFECT"));

        // 10. RAYCAST (Simulates HITSCAN_ENTITIES / MOB_AROUND)
        inv.setItem(slot++, createGuiItem(Material.DIAMOND_SWORD, "§6RAYCAST",
                "§7Execute a skill on a line-of-sight target or AOE.",
                "§7(Simulates HITSCAN_ENTITIES / MOB_AROUND)",
                "ActionType: RAYCAST"));

        // 11. SPAWN_ENTITY (Simulates SPAWN_ENTITY)
        inv.setItem(slot++, createGuiItem(Material.EGG, "§6SPAWN_ENTITY",
                "§7Spawn a non-projectile entity (Lightning/Mob).",
                "§7(Simulates SPAWN_ENTITY)",
                "ActionType: SPAWN_ENTITY"));

        // 12. VELOCITY
        inv.setItem(slot++, createGuiItem(Material.FEATHER, "§dVELOCITY",
                "§7Apply force to move the target.",
                "ActionType: VELOCITY"));

        // 13. LOOP
        inv.setItem(slot++, createGuiItem(Material.REPEATER, "§bLOOP",
                "§7Execute sub-actions multiple times.",
                "ActionType: LOOP"));

        // 14. COMMAND
        inv.setItem(slot++, createGuiItem(Material.COMMAND_BLOCK, "§bCOMMAND",
                "§7Execute a console/player command.",
                "ActionType: COMMAND"));

        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack", "§7Cancel"));

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