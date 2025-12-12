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
import java.util.ArrayList;
import java.util.List;

public class SkillBindingGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;

    public SkillBindingGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this.plugin = plugin;
        this.itemFile = itemFile;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + itemFile.getName() + " [SKILLS]"));

        ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
        List<ItemSkillBinding> bindings = attr.getSkillBindings();

        int slot = 0;
        for (ItemSkillBinding binding : bindings) {
            if (slot >= 45) break;
            List<String> lore = new ArrayList<>();
            lore.add("§7Skill ID: §f" + binding.getSkillId());
            lore.add("§7Trigger: §f" + binding.getTrigger());
            lore.add("§7Level: §f" + binding.getLevel());
            lore.add("§7Chance: §f" + (binding.getChance() * 100) + "%");
            lore.add("");
            lore.add("§cRight-Click to Remove");

            ItemStack item = createGuiItem(Material.ENCHANTED_BOOK, "§aBinding #" + (slot + 1), lore);
            inv.setItem(slot, item);
            slot++;
        }

        // Add Button
        inv.setItem(49, createGuiItem(Material.LIME_DYE, "§a§l+ ADD SKILL", "§7Click to add a skill binding."));
        inv.setItem(45, createGuiItem(Material.ARROW, "§cBack", "§7Return to editor."));

        // Fill BG
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        return createGuiItem(mat, name, Arrays.asList(lore));
    }

    private ItemStack createGuiItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}