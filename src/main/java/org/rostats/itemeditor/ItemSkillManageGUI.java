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

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ItemSkillManageGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;

    public ItemSkillManageGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this.plugin = plugin;
        this.itemFile = itemFile;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ItemSkills: " + itemFile.getName()));
        ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);

        // List existing skills
        int slot = 0;
        int index = 0;
        for (ItemSkillBinding binding : attr.getSkillBindings()) {
            if (slot >= 45) break;

            inv.setItem(slot++, createGuiItem(Material.ENCHANTED_BOOK,
                    "§eSkill #" + (index + 1),
                    "§7ID: §f" + binding.getSkillId(),
                    "§7Trigger: §a" + binding.getTrigger().name(),
                    "§7Level: §a" + binding.getLevel(),
                    "§7Chance: §a" + (binding.getChance() * 100) + "%",
                    "§8---------------",
                    "§cShift+Right Click to Remove",
                    "§8index:" + index
            ));
            index++;
        }

        // Controls
        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§l+ ADD NEW SKILL", "§7Click to select a skill to add."));
        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack", "§7Return to Item Editor"));

        // Fill
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