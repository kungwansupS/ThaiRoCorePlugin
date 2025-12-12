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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillBindingGUI {

    private final ThaiRoCorePlugin plugin;
    private final String itemId;

    public SkillBindingGUI(ThaiRoCorePlugin plugin, String itemId) {
        this.plugin = plugin;
        this.itemId = itemId;
    }

    // Constructor accepting File for convenience
    public SkillBindingGUI(ThaiRoCorePlugin plugin, java.io.File itemFile) {
        this.plugin = plugin;
        this.itemId = itemFile.getName().replace(".yml", "");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ItemEditor: Skill Binding: " + itemId));

        ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(player.getInventory().getItemInMainHand());
        List<ItemSkillBinding> bindings = attr.getSkillBindings();

        // Bindings List
        for (int i = 0; i < 45; i++) {
            if (i < bindings.size()) {
                inv.setItem(i, createBindingItem(bindings.get(i), i));
            } else {
                inv.setItem(i, createEmptySlot());
            }
        }

        // --- Controls ---
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, bg);
        }

        inv.setItem(45, createGuiItem(Material.RED_BED, "§cBack to Item Editor"));
        inv.setItem(51, createGuiItem(Material.EMERALD, "§aAdd New Skill Binding"));
        inv.setItem(53, createGuiItem(Material.CHEST, "§aSave Item"));

        player.openInventory(inv);
    }

    private ItemStack createBindingItem(ItemSkillBinding binding, int index) {
        SkillData skill = plugin.getSkillManager().getSkill(binding.getSkillId());
        Material mat = (skill != null && skill.getIcon() != null) ? skill.getIcon() : Material.BOOK;
        String name = (skill != null && skill.getDisplayName() != null) ? skill.getDisplayName().replace("&", "§") : binding.getSkillId();

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6#" + (index + 1) + " Skill: §e" + name);

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: " + binding.getSkillId());
        lore.add("§7Level: " + binding.getLevel());
        lore.add("§7Chance: " + String.format("%.0f%%", binding.getChance() * 100));
        lore.add("§7Trigger: " + binding.getTrigger().name());
        lore.add("");
        lore.add("§eLeft-Click to Edit");
        lore.add("§cRight-Click to Remove");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptySlot() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§7Empty Slot");
        item.setItemMeta(meta);
        return item;
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