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
        // Serialize current state to Map for editing
        this.data = action.serialize();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ActionEdit: " + skillId + " #" + actionIndex));

        // Display Properties based on Type
        int slot = 0;

        // --- Common Properties ---

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
        // Store the key in Lore (hidden) or rely on slot index logic in listener?
        // Let's put key in lore for safety.
        return createGuiItem(mat, "§e" + display,
                "§7Value: §f" + value,
                "§8---------------",
                hint,
                "§0Key:" + key); // Hidden key
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS);
        item.setItemMeta(meta);
        return item;
    }

    // Helpers for Listener to retrieve context
    public Map<String, Object> getData() { return data; }
}