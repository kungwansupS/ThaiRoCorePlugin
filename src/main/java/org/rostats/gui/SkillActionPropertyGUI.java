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

        // --- Properties Display ---
        if (type == ActionType.DAMAGE) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "Formula / สูตร",
                    (String) data.getOrDefault("formula", "ATK"), "§eClick to edit formula"));

            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "element", "Element / ธาตุ",
                    (String) data.getOrDefault("element", "NEUTRAL"), "§eClick to edit element"));
        }
        else if (type == ActionType.HEAL) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "Amount Formula / สูตรการฮีล",
                    (String) data.getOrDefault("formula", "10"), "§eClick to edit"));

            boolean isMana = (boolean) data.getOrDefault("is-mana", false);
            inv.setItem(slot++, createPropItem(isMana ? Material.LAPIS_LAZULI : Material.REDSTONE, "is-mana", "Type / ประเภท",
                    isMana ? "MANA (SP)" : "HEALTH (HP)", "§eClick to toggle"));
        }
        else if (type == ActionType.APPLY_EFFECT) {
            inv.setItem(slot++, createPropItem(Material.NAME_TAG, "effect-id", "Effect ID",
                    (String) data.getOrDefault("effect-id", "unknown"), "§eClick to edit ID"));

            inv.setItem(slot++, createPropItem(Material.POTION, "effect-type", "Effect Type",
                    (String) data.getOrDefault("effect-type", "STAT_MODIFIER"), "§eClick to edit Type Enum"));

            inv.setItem(slot++, createPropItem(Material.EXPERIENCE_BOTTLE, "level", "Level",
                    data.getOrDefault("level", 1).toString(), "§eClick to edit Level"));

            inv.setItem(slot++, createPropItem(Material.IRON_SWORD, "power", "Power/Value",
                    data.getOrDefault("power", 0.0).toString(), "§eClick to edit Power"));

            inv.setItem(slot++, createPropItem(Material.CLOCK, "duration", "Duration (Ticks)",
                    data.getOrDefault("duration", 100).toString(), "§eClick to edit Duration (20 = 1s)"));

            inv.setItem(slot++, createPropItem(Material.FEATHER, "chance", "Chance (0.0-1.0)",
                    data.getOrDefault("chance", 1.0).toString(), "§eClick to edit Chance"));

            inv.setItem(slot++, createPropItem(Material.ANVIL, "stat-key", "Stat Key (Optional)",
                    (String) data.getOrDefault("stat-key", "None"), "§eClick to edit Stat Key"));
        }
        // --- PHASE 4 ADDITIONS ---
        else if (type == ActionType.SOUND) {
            inv.setItem(slot++, createPropItem(Material.NOTE_BLOCK, "sound", "Sound Name",
                    (String) data.getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP"), "§eClick to edit Sound Enum"));
            inv.setItem(slot++, createPropItem(Material.REPEATER, "volume", "Volume",
                    data.getOrDefault("volume", 1.0).toString(), "§eClick to edit Volume"));
            inv.setItem(slot++, createPropItem(Material.COMPARATOR, "pitch", "Pitch",
                    data.getOrDefault("pitch", 1.0).toString(), "§eClick to edit Pitch"));
        }
        else if (type == ActionType.PARTICLE) {
            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "particle", "Particle Name",
                    (String) data.getOrDefault("particle", "VILLAGER_HAPPY"), "§eClick to edit Particle Enum"));
            inv.setItem(slot++, createPropItem(Material.GLOWSTONE_DUST, "count", "Count",
                    data.getOrDefault("count", 5).toString(), "§eClick to edit Count"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "Speed",
                    data.getOrDefault("speed", 0.1).toString(), "§eClick to edit Speed"));
            inv.setItem(slot++, createPropItem(Material.COMPASS, "offset", "Offset/Spread",
                    data.getOrDefault("offset", 0.5).toString(), "§eClick to edit Offset"));
        }
        else if (type == ActionType.POTION) {
            inv.setItem(slot++, createPropItem(Material.POTION, "potion", "Potion Type",
                    (String) data.getOrDefault("potion", "SPEED"), "§eClick to edit Potion Type"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "duration", "Duration (Ticks)",
                    data.getOrDefault("duration", 60).toString(), "§eClick to edit Duration"));
            inv.setItem(slot++, createPropItem(Material.GLOWSTONE, "amplifier", "Amplifier (Lv-1)",
                    data.getOrDefault("amplifier", 0).toString(), "§eClick to edit Amplifier"));
        }
        else if (type == ActionType.TELEPORT) {
            inv.setItem(slot++, createPropItem(Material.ENDER_PEARL, "range", "Range (Blocks)",
                    data.getOrDefault("range", 5.0).toString(), "§eClick to edit Range"));
            boolean toTarget = (boolean) data.getOrDefault("to-target", false);
            inv.setItem(slot++, createPropItem(Material.ENDER_EYE, "to-target", "To Target?",
                    toTarget ? "YES" : "NO", "§eClick to toggle"));
        }

        // --- NEW: PROJECTILE PROPERTIES ---
        else if (type == ActionType.PROJECTILE) {
            inv.setItem(slot++, createPropItem(Material.ARROW, "projectile", "Projectile Type",
                    (String) data.getOrDefault("projectile", "ARROW"), "§eClick to edit (ARROW, SNOWBALL, etc.)"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "Speed",
                    data.getOrDefault("speed", 1.0).toString(), "§eClick to edit Speed"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "on-hit", "On-Hit Skill ID",
                    (String) data.getOrDefault("on-hit", "none"), "§eClick to edit Skill ID to cast on hit"));
        }
        // ---------------------------------

        // --- Controls ---
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE CHANGES",
                "§7Apply changes to skill.", "§8---------------", "§7บันทึกการแก้ไข"));

        inv.setItem(53, createGuiItem(Material.RED_CONCRETE, "§cCancel / ยกเลิก",
                "§7Discard changes."));

        // Fill
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createPropItem(Material mat, String key, String display, String value, String hint) {
        return createGuiItem(mat, "§e" + display,
                "§7Value: §f" + value,
                "§8---------------",
                hint,
                "§0Key:" + key);
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