package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.configuration.ConfigurationSection;

public class ItemAttributeManager {

    private final ThaiRoCorePlugin plugin;
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();
    private final NamespacedKey REMOVE_VANILLA_KEY;
    private static final String ATTRIBUTE_PREFIX = "ro_attr_";

    public ItemAttributeManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.REMOVE_VANILLA_KEY = new NamespacedKey(plugin, ATTRIBUTE_PREFIX + "removevanilla");
    }

    // --- PDC Data Management ---

    /**
     * Retrieves all custom attributes from an ItemStack and returns them as a single POJO.
     * @param item The ItemStack to check.
     * @return ItemAttribute POJO with all values set.
     */
    public ItemAttribute getAttributesFromItem(ItemStack item) {
        ItemAttribute attr = new ItemAttribute();
        if (item == null || !item.hasItemMeta()) return attr;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Use reflection (or explicit list) to iterate through all ItemAttribute fields
        for (Field field : ItemAttribute.class.getDeclaredFields()) {
            if (field.getName().equals("removeVanillaAttribute")) continue; // Skip boolean for now

            if (field.getType() == double.class || field.getType() == int.class) {
                try {
                    field.setAccessible(true);
                    // Key format: ro_attr_<fieldname>
                    NamespacedKey key = new NamespacedKey(plugin, ATTRIBUTE_PREFIX + field.getName().toLowerCase());

                    // Get the value and set it on the POJO
                    double value = pdc.getOrDefault(key, PersistentDataType.DOUBLE, 0.0);
                    attr.setAttribute(field.getName(), value);
                } catch (SecurityException e) {
                    // Ignore
                }
            }
        }

        // Handle boolean field separately (using 1.0/0.0 double)
        double removeVanillaValue = pdc.getOrDefault(REMOVE_VANILLA_KEY, PersistentDataType.DOUBLE, 0.0);
        attr.setRemoveVanillaAttribute(removeVanillaValue == 1.0);

        return attr;
    }

    /**
     * Sets a specific attribute value on the ItemStack's Persistent Data Container.
     * @param item The ItemStack to modify.
     * @param attributeKey The key of the attribute (e.g., "pAtkFlat").
     * @param value The value to set.
     */
    public void setAttribute(ItemStack item, String attributeKey, double value) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Handle boolean separately
        if (attributeKey.equalsIgnoreCase("removeVanillaAttribute")) {
            if (value == 0.0) {
                pdc.remove(REMOVE_VANILLA_KEY);
            } else {
                pdc.set(REMOVE_VANILLA_KEY, PersistentDataType.DOUBLE, 1.0);
            }
            item.setItemMeta(meta);
            return;
        }

        // Handle normal attributes
        NamespacedKey key = new NamespacedKey(plugin, ATTRIBUTE_PREFIX + attributeKey.toLowerCase());
        if (value == 0.0) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.DOUBLE, value);
        }
        item.setItemMeta(meta);
    }

    // --- Utility Methods ---

    /**
     * Removes all vanilla Attribute Modifiers (e.g., from Diamond Sword, Armor)
     * as required by the prompt (Requirement 4).
     * @param item The item to modify.
     * @return The modified ItemStack.
     */
    public ItemStack removeVanillaAttributes(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return item;
        ItemMeta meta = item.getItemMeta();

        if (meta.hasAttributeModifiers()) {
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
            meta.removeAttributeModifier(Attribute.GENERIC_MAX_HEALTH);
            meta.removeAttributeModifier(Attribute.GENERIC_MOVEMENT_SPEED);
            meta.removeAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            meta.removeAttributeModifier(Attribute.GENERIC_LUCK);
        }

        // Add flag to hide default attributes in lore (important for the vanilla attributes remaining)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Saves the ItemStack and its custom attributes to a YAML file, creating a template.
     * (Retained from existing code but modified to use POJO and ItemAttributeLoader keys).
     */
    public boolean saveItemAsTemplate(ItemStack item, Player player) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        // 1. Setup file structure
        File templateFolder = new File(plugin.getDataFolder(), "item_templates");
        if (!templateFolder.exists()) {
            templateFolder.mkdirs();
        }

        ItemAttribute attr = getAttributesFromItem(item); // Get all current attributes from PDC

        String itemName = item.getItemMeta().hasDisplayName() ?
                PLAIN_TEXT_SERIALIZER.serialize(item.getItemMeta().displayName()).replaceAll("[^a-zA-Z0-9_]", "") :
                item.getType().name();

        String randomId = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));

        String fileName = player.getName() + "_" + itemName + "_" + randomId + ".yml";
        File templateFile = new File(templateFolder, fileName);

        YamlConfiguration config = new YamlConfiguration();
        ItemMeta meta = item.getItemMeta();

        // 2. Extract Core Item Data
        config.set("material", item.getType().name());
        if (meta.hasDisplayName()) {
            config.set("name", PLAIN_TEXT_SERIALIZER.serialize(meta.displayName()));
        } else {
            config.set("name", "<reset>" + item.getType().name());
        }
        if (meta.hasLore()) {
            List<String> stringLore = Objects.requireNonNull(meta.lore()).stream()
                    .map(PLAIN_TEXT_SERIALIZER::serialize)
                    .collect(Collectors.toList());
            config.set("lore", stringLore);
        } else {
            config.set("lore", new ArrayList<String>());
        }
        config.set("unbreakable", meta.isUnbreakable());
        config.set("remove-vanilla", attr.isRemoveVanillaAttribute()); // Save utility field

        // 3. Extract Custom Attributes (from POJO fields back to YAML keys)
        config.set("attributes", null); // Clear old map if exists

        // Use reflection to iterate through all POJO fields and save non-zero values
        for (Field field : ItemAttribute.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                String key = field.getName().toLowerCase().replaceAll("([a-z])([A-Z])", "$1-$2"); // Converts camelCase to kebab-case

                Object value = field.get(attr);
                if (value instanceof Double && (Double) value != 0.0) {
                    config.set("attributes." + key, value);
                } else if (value instanceof Integer && (Integer) value != 0) {
                    config.set("attributes." + key, value);
                }
            } catch (IllegalAccessException e) {
                // Ignore
            }
        }

        // 4. Save the file
        try {
            config.save(templateFile);
            plugin.getLogger().info("Successfully saved item template: " + fileName);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save item template " + fileName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // --- Helper for loading from YAML (Used by GUI/Admin) ---
    public ItemAttribute loadAttributesFromYaml(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection attrSection = config.getConfigurationSection("attributes");
        if (attrSection == null) attrSection = config.createSection("attributes");

        // Use the new Loader to parse the section.
        ItemAttribute attr = ItemAttributeLoader.load(attrSection);

        // Since loadAttributesFromYaml loads from a File, the boolean must be checked at the top-level.
        attr.setRemoveVanillaAttribute(config.getBoolean("remove-vanilla", false));

        return attr;
    }
}