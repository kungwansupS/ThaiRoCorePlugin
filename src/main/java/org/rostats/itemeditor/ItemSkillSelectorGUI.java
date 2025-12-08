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
import org.rostats.engine.skill.SkillData;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class ItemSkillSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;

    public ItemSkillSelectorGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this.plugin = plugin;
        this.itemFile = itemFile;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillSelect: " + itemFile.getName()));

        Map<String, SkillData> skills = plugin.getSkillManager().getSkills();
        int slot = 0;

        for (SkillData skill : skills.values()) {
            if (slot >= 53) break;

            inv.setItem(slot++, createGuiItem(skill.getIcon(),
                    "§a" + skill.getDisplayName(),
                    "§7ID: §f" + skill.getId(),
                    "§8---------------",
                    "§eClick to Select"
            ));
        }

        inv.setItem(53, createGuiItem(Material.ARROW, "§cBack", "§7Cancel"));

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