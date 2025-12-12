package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.element.Element;

import java.util.Arrays;

public class SkillElementSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;
    private final int actionIndex;

    public SkillElementSelectorGUI(ThaiRoCorePlugin plugin, String skillId, int actionIndex) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.actionIndex = actionIndex;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Skill Element: " + skillId + " #" + actionIndex));

        int[] slots = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23};
        Element[] elements = Element.values();

        for (int i = 0; i < elements.length && i < slots.length; i++) {
            Element e = elements[i];
            Material icon = getElementIcon(e);
            inv.setItem(slots[i], createElementItem(e, icon));
        }

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName("§cBack");
        back.setItemMeta(meta);
        inv.setItem(18, back);

        // Fill BG
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for(int i=0; i<27; i++) {
            if(inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private Material getElementIcon(Element e) {
        switch (e) {
            case NEUTRAL: return Material.WHITE_WOOL;
            case WATER: return Material.WATER_BUCKET;
            case EARTH: return Material.GRASS_BLOCK;
            case FIRE: return Material.FLINT_AND_STEEL;
            case WIND: return Material.FEATHER;
            case POISON: return Material.SPIDER_EYE;
            case HOLY: return Material.GOLDEN_APPLE;
            case SHADOW: return Material.OBSIDIAN;
            case GHOST: return Material.SOUL_SAND;
            case UNDEAD: return Material.ROTTEN_FLESH;
            default: return Material.PAPER;
        }
    }

    private ItemStack createElementItem(Element element, Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e" + element.name());
        meta.setLore(Arrays.asList("§7Click to select", "§8ID: " + element.getIndex()));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        NamespacedKey key = new NamespacedKey(plugin, "skill_elem_val");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, element.name());

        item.setItemMeta(meta);
        return item;
    }
}