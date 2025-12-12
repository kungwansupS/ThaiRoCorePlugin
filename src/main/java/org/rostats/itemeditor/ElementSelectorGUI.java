package org.rostats.itemeditor;

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

import java.io.File;
import java.util.Arrays;

public class ElementSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final ItemAttributeType targetAttribute; // ATTACK_ELEMENT or DEFENSE_ELEMENT

    public ElementSelectorGUI(ThaiRoCorePlugin plugin, File itemFile, ItemAttributeType targetAttribute) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        this.targetAttribute = targetAttribute;
    }

    public void open(Player player) {
        String typeName = targetAttribute == ItemAttributeType.ATTACK_ELEMENT ? "Attack" : "Defense";
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Element Select: " + typeName));

        // 0-9 Elements + None
        int[] slots = {10, 11, 12, 13, 14, 19, 20, 21, 22, 23};

        // Element Icons
        inv.setItem(slots[0], createElementItem(Element.NEUTRAL, Material.WHITE_WOOL, "§fNeutral (0)"));
        inv.setItem(slots[1], createElementItem(Element.WATER, Material.WATER_BUCKET, "§9Water (1)"));
        inv.setItem(slots[2], createElementItem(Element.EARTH, Material.GRASS_BLOCK, "§2Earth (2)"));
        inv.setItem(slots[3], createElementItem(Element.FIRE, Material.FLINT_AND_STEEL, "§cFire (3)"));
        inv.setItem(slots[4], createElementItem(Element.WIND, Material.FEATHER, "§aWind (4)"));
        inv.setItem(slots[5], createElementItem(Element.POISON, Material.SPIDER_EYE, "§5Poison (5)"));
        inv.setItem(slots[6], createElementItem(Element.HOLY, Material.GOLDEN_APPLE, "§eHoly (6)"));
        inv.setItem(slots[7], createElementItem(Element.SHADOW, Material.OBSIDIAN, "§8Shadow (7)"));
        inv.setItem(slots[8], createElementItem(Element.GHOST, Material.SOUL_SAND, "§7Ghost (8)"));
        inv.setItem(slots[9], createElementItem(Element.UNDEAD, Material.ROTTEN_FLESH, "§4Undead (9)"));

        // None Option
        inv.setItem(26, createGuiItem(Material.BARRIER, "§cNone (-1)", "select_element", "-1", "§7Reset to None"));

        // Back Button
        inv.setItem(18, createGuiItem(Material.ARROW, "§cBack", "back", "", "§7Return to Editor"));

        // Fill BG
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "bg", "");
        for (int i = 0; i < 27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createElementItem(Element element, Material mat, String name) {
        return createGuiItem(mat, name, "select_element", String.valueOf(element.getIndex()), "§eClick to select " + element.name());
    }

    private ItemStack createGuiItem(Material mat, String name, String type, String value, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            NamespacedKey keyType = new NamespacedKey(plugin, "icon_type");
            NamespacedKey keyValue = new NamespacedKey(plugin, "icon_value");
            // Store target attribute key as well so we know which one to update
            NamespacedKey keyAttr = new NamespacedKey(plugin, "target_attr");

            PersistentDataType<String, String> strType = PersistentDataType.STRING;
            meta.getPersistentDataContainer().set(keyType, strType, type);
            meta.getPersistentDataContainer().set(keyValue, strType, value);
            meta.getPersistentDataContainer().set(keyAttr, strType, targetAttribute.name());

            item.setItemMeta(meta);
        }
        return item;
    }
}