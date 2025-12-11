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
        for (ActionType type : ActionType.values()) {
            if (slot >= 54) break;

            String desc = switch(type) {
                case DAMAGE -> "Deal damage to target";
                case HEAL -> "Heal target (HP/SP)";
                case CONDITION -> "Check conditions (HP, Mana, etc)";
                case SET_VARIABLE -> "Set a temporary variable";
                case LOOP -> "Repeat actions multiple times";
                case SELECT_TARGET -> "Change current target";
                case SOUND -> "Play a sound";
                case APPLY_EFFECT -> "Apply Buff/Debuff";
                case PARTICLE -> "Spawn particles";
                case POTION -> "Apply Vanilla Potion";
                case TELEPORT -> "Teleport caster/target";
                case PROJECTILE -> "Launch a projectile";
                case AREA_EFFECT -> "Damage/Effect in area";
                case VELOCITY -> "Push/Pull entity";
                case COMMAND -> "Run console/player command";
                case RAYCAST -> "Fire a raycast (Hitscan)";
                case SPAWN_ENTITY -> "Summon an entity";
                default -> "Unknown action";
            };

            // สร้างไอเทมพร้อม Lore (Helper Method จะจัดการเรื่อง List<String> ให้)
            inv.setItem(slot++, createGuiItem(getIcon(type), "§e" + type.name(),
                    "§7" + desc,
                    "§8---------------",
                    "§bClick to Add",
                    "§0ActionType: " + type.name()));
        }

        // Back Button
        inv.setItem(45, createGuiItem(Material.ARROW, "§cBack", "§7Return to Editor"));

        player.openInventory(inv);
    }

    private Material getIcon(ActionType type) {
        return switch(type) {
            case DAMAGE -> Material.IRON_SWORD;
            case HEAL -> Material.GOLDEN_APPLE;
            case CONDITION -> Material.COMPARATOR;
            case SET_VARIABLE -> Material.NAME_TAG;
            case LOOP -> Material.REPEATER;
            case SELECT_TARGET -> Material.TARGET;
            case SOUND -> Material.NOTE_BLOCK;
            case APPLY_EFFECT -> Material.POTION;
            case PARTICLE -> Material.BLAZE_POWDER;
            case POTION -> Material.GLASS_BOTTLE;
            case TELEPORT -> Material.ENDER_PEARL;
            case PROJECTILE -> Material.ARROW;
            case AREA_EFFECT -> Material.LINGERING_POTION;
            case VELOCITY -> Material.FEATHER;
            case COMMAND -> Material.COMMAND_BLOCK;
            case RAYCAST -> Material.SPECTRAL_ARROW;
            case SPAWN_ENTITY -> Material.ZOMBIE_SPAWN_EGG;
            default -> Material.PAPER;
        };
    }

    // [FIX] เมธอดนี้สำคัญ: แปลง String... เป็น List<String> ก่อนส่งให้ setLore
    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore)); // แก้ไขตรงนี้: ห้ามส่ง String เดี่ยวๆ
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}