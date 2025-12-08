package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.trigger.TriggerType;

import java.io.File;
import java.util.Arrays;

public class ItemSkillSettingsGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final ItemSkillBinding binding; // Current settings state

    public ItemSkillSettingsGUI(ThaiRoCorePlugin plugin, File itemFile, ItemSkillBinding binding) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        this.binding = binding;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("SkillSettings: " + itemFile.getName()));

        // Info
        inv.setItem(4, createGuiItem(Material.BOOK, "§eSelected Skill", "§7ID: §f" + binding.getSkillId()));

        // Trigger
        inv.setItem(11, createGuiItem(Material.LEVER, "§6Trigger",
                "§7Current: §e" + binding.getTrigger().name(),
                "§8---------------",
                "§eClick to toggle"));

        // Level
        inv.setItem(13, createGuiItem(Material.EXPERIENCE_BOTTLE, "§aLevel",
                "§7Current: §e" + binding.getLevel(),
                "§8---------------",
                "§eClick to edit"));

        // Chance
        inv.setItem(15, createGuiItem(Material.FEATHER, "§bChance",
                "§7Current: §e" + (binding.getChance() * 100) + "%",
                "§8---------------",
                "§eClick to edit"));

        // Save
        inv.setItem(22, createGuiItem(Material.EMERALD_BLOCK, "§a§lADD TO ITEM", "§7Confirm and Save"));

        // Cancel
        inv.setItem(26, createGuiItem(Material.RED_CONCRETE, "§cCancel"));

        // Fill
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
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