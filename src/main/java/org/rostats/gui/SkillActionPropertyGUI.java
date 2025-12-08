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
import org.rostats.engine.action.SkillAction;

import java.util.Arrays;
import java.util.Map;

public class SkillActionPropertyGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;
    private final int actionIndex;
    private final ActionType type;
    private final Map<String, Object> data;

    public SkillActionPropertyGUI(ThaiRoCorePlugin plugin, String skillId, int actionIndex, SkillAction action) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.actionIndex = actionIndex;
        this.type = action.getType();
        this.data = action.serialize();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ActionEdit: " + skillId + " #" + actionIndex));
        int slot = 0;

        if (type == ActionType.DAMAGE) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "Formula", (String) data.getOrDefault("formula", "ATK"), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "element", "Element", (String) data.getOrDefault("element", "NEUTRAL"), "§eClick to edit"));
        }
        else if (type == ActionType.HEAL) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "Amount", (String) data.getOrDefault("formula", "10"), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.REDSTONE, "is-mana", "Is Mana?", data.getOrDefault("is-mana", false).toString(), "§eClick to toggle"));
        }
        else if (type == ActionType.APPLY_EFFECT) {
            inv.setItem(slot++, createPropItem(Material.NAME_TAG, "effect-id", "Effect ID", (String) data.getOrDefault("effect-id", "unknown"), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.POTION, "effect-type", "Type", (String) data.getOrDefault("effect-type", "STAT_MODIFIER"), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.EXPERIENCE_BOTTLE, "level", "Level", data.getOrDefault("level", 1).toString(), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "duration", "Duration", data.getOrDefault("duration", 100).toString(), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "chance", "Chance", data.getOrDefault("chance", 1.0).toString(), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.IRON_SWORD, "power", "Power", data.getOrDefault("power", 0.0).toString(), "§eClick to edit"));
        }
        // NEW PROPERTIES
        else if (type == ActionType.SOUND) {
            inv.setItem(slot++, createPropItem(Material.NOTE_BLOCK, "sound", "Sound Name",
                    (String) data.getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), "§eClick to edit Enum"));
            inv.setItem(slot++, createPropItem(Material.REPEATER, "volume", "Volume",
                    data.getOrDefault("volume", 1.0f).toString(), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.COMPARATOR, "pitch", "Pitch",
                    data.getOrDefault("pitch", 1.0f).toString(), "§eClick to edit"));
        }
        else if (type == ActionType.PARTICLE) {
            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "particle", "Particle Type",
                    (String) data.getOrDefault("particle", "VILLAGER_HAPPY"), "§eClick to edit Enum"));
            inv.setItem(slot++, createPropItem(Material.SLIME_BALL, "count", "Count",
                    data.getOrDefault("count", 10).toString(), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "Speed",
                    data.getOrDefault("speed", 0.1).toString(), "§eClick to edit"));
            inv.setItem(slot++, createPropItem(Material.PISTON, "y-offset", "Y Offset",
                    data.getOrDefault("y-offset", 1.0).toString(), "§eClick to edit"));
        }
        else if (type == ActionType.PROJECTILE) {
            inv.setItem(slot++, createPropItem(Material.ARROW, "projectile-type", "Projectile Type",
                    (String) data.getOrDefault("projectile-type", "ARROW"), "§eClick to edit Enum"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "Speed",
                    data.getOrDefault("speed", 1.0).toString(), "§eClick to edit"));
            inv.setItem(slot++, createGuiItem(Material.CHEST, "§6§lOn-Hit Actions", "§eEdit in YAML file."));
        }

        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE CHANGES", "§7Apply changes.", "§8---------------", "§7บันทึก"));
        inv.setItem(53, createGuiItem(Material.RED_CONCRETE, "§cCancel", "§7Discard."));

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createPropItem(Material mat, String key, String display, String value, String hint) {
        return createGuiItem(mat, "§e" + display, "§7Value: §f" + value, "§8---------------", hint, "§0Key:" + key);
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