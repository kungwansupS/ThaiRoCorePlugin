package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemFlag;
<<<<<<< HEAD
// แก้ไข: เปลี่ยนจาก ROStatsPlugin เป็น ThaiRoCorePlugin
import org.rostats.ThaiRoCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemAttributeManager {

    // แก้ไข: เปลี่ยน Type เป็น ThaiRoCorePlugin
    private final ThaiRoCorePlugin plugin;
    private static final String MAIN_STATS_HEADER = "§f§l--- Main Stats ---";
    private static final String CUSTOM_LORE_HEADER = "§f§l--- Custom Lore ---";

=======
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

public class ItemAttributeManager {

    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

    // แก้ไข: เปลี่ยน Type เป็น ThaiRoCorePlugin
    private final ThaiRoCorePlugin plugin;
    private static final String MAIN_STATS_HEADER = "§f§l--- Main Stats ---";
    private static final String CUSTOM_LORE_HEADER = "§f§l--- Custom Lore ---";

>>>>>>> parent of d30d525 (0)
    // แก้ไข: เปลี่ยน Type ใน Constructor เป็น ThaiRoCorePlugin
    public ItemAttributeManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        // Initialize NamespacedKeys once
        for (ItemAttribute attribute : ItemAttribute.values()) {
            attribute.initialize(plugin);
        }
    }

<<<<<<< HEAD
=======
    // --- Serialization Helper to resolve ambiguity (FIX) ---
    private String serializeComponentToString(Component component) {
        return PLAIN_TEXT_SERIALIZER.serialize(component);
    }

>>>>>>> parent of d30d525 (0)
    // --- Data Management ---

    public double getAttribute(ItemStack item, ItemAttribute attribute) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.getOrDefault(attribute.getNamespacedKey(), PersistentDataType.DOUBLE, 0.0);
    }

    public void setAttribute(ItemStack item, ItemAttribute attribute, double value) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (value == 0.0) {
            pdc.remove(attribute.getNamespacedKey());
        } else {
            pdc.set(attribute.getNamespacedKey(), PersistentDataType.DOUBLE, value);
        }
        item.setItemMeta(meta);
        updateLore(item); // Update lore automatically upon setting attribute
    }

<<<<<<< HEAD
    // --- NEW: Vanilla Attribute Management (Req 1 & 2) ---

    /**
     * Removes all vanilla Attribute Modifiers (e.g., from Diamond Sword, Armor)
     * โดยไม่กระทบต่อ Name/Lore/Custom Attributes
     */
    public void removeVanillaAttributes(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();

        // FIX: ใช้ setAttributeModifiers(null) เพื่อลบ Attribute Modifiers ทั้งหมด
        if (meta.hasAttributeModifiers()) {
            meta.setAttributeModifiers(null);
        }

        // NEW: เพิ่ม ItemFlag.HIDE_ATTRIBUTES เพื่อให้มั่นใจว่า Lore ของ Vanilla Attributes จะถูกซ่อน
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Apply meta changes
        item.setItemMeta(meta);

        // Refresh lore to ensure consistency (สำคัญ)
        updateLore(item);
    }

    // --- Lore Management (Main Stats + Custom Lore) ---

    public void updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
=======
    // --- NEW: Template Saving Logic ---

    /**
     * Saves the ItemStack and its custom attributes to a YAML file, creating a template.
     * @param item The item to save.
     * @param player The player performing the save (for logging/feedback).
     * @return true if save was successful.
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

        // Generate a unique file name (e.g., PLAYERNAME_ITEMTYPE_RANDOMID.yml)
        String itemName = item.getItemMeta().hasDisplayName() ?
                serializeComponentToString(item.getItemMeta().displayName()).replaceAll("[^a-zA-Z0-9_]", "") :
                item.getType().name();

        // Use a short random integer instead of UUID for simplicity
        String randomId = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));

        String fileName = player.getName() + "_" + itemName + "_" + randomId + ".yml";
        File templateFile = new File(templateFolder, fileName);

        YamlConfiguration config = new YamlConfiguration();
>>>>>>> parent of d30d525 (0)
        ItemMeta meta = item.getItemMeta();
        List<String> currentLore = meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : new ArrayList<>();

<<<<<<< HEAD
        List<String> newLore = new ArrayList<>();

        // 1. Separate existing lore to preserve CUSTOM LORE
        List<String> customLore = new ArrayList<>();
        boolean inCustomLore = false;

        for (String line : currentLore) {
            String stripped = line.replaceAll("§[0-9a-fk-or]", "");
            if (stripped.equals("--- Custom Lore ---")) {
                inCustomLore = true;
                continue;
            }
            if (stripped.equals("--- Main Stats ---")) { // Reset flag if we hit the start of the Main Stats (shouldn't happen if they are at the top)
                inCustomLore = false;
                continue;
            }

            if (inCustomLore) {
                customLore.add(line);
=======
        // 2. Extract Core Item Data
        config.set("material", item.getType().name());

        // Set Name (Converted to plain text/string for YAML)
        if (meta.hasDisplayName()) {
            config.set("name", serializeComponentToString(meta.displayName())); // Use helper function
        } else {
            config.set("name", "<reset>" + item.getType().name());
        }

        // Set Lore (Converted to string list)
        if (meta.hasLore()) {
            // Convert Component list to String list for YAML
            List<String> stringLore = Objects.requireNonNull(meta.lore()).stream()
                    .map(this::serializeComponentToString) // Use helper function to resolve ambiguity
                    .collect(Collectors.toList());
            config.set("lore", stringLore);
        } else {
            config.set("lore", new ArrayList<String>());
        }

        // Set basic Minecraft attributes (unbreakable, etc.)
        config.set("unbreakable", meta.isUnbreakable());

        // 3. Extract Custom Attributes (from PDC)
        config.set("custom-attributes", null); // Clear old map if exists

        for (ItemAttribute attribute : ItemAttribute.values()) {
            double value = getAttribute(item, attribute);
            if (value != 0.0) {
                // Key: ATTRIBUTE_KEY.toLowerCase()
                // Value: double
                config.set("custom-attributes." + attribute.getKey(), value);
>>>>>>> parent of d30d525 (0)
            }
        }

        // 2. Generate new Main Stats Lore
        List<String> mainStats = new ArrayList<>();
        for (ItemAttribute attribute : ItemAttribute.values()) {
            double value = getAttribute(item, attribute);
            if (value != 0.0) {
                String formattedValue = String.format(attribute.getFormat(), value);
                String line = attribute.getDisplayName() + ": §f" + formattedValue;
                mainStats.add(line);
            }
        }

<<<<<<< HEAD
        // 3. Combine Lore: Main Stats (Auto-generated) + Custom Lore (Preserved)
        if (!mainStats.isEmpty()) {
            newLore.add(MAIN_STATS_HEADER);
            newLore.addAll(mainStats);
        }

        if (!customLore.isEmpty()) {
            if (!newLore.isEmpty()) newLore.add(" "); // Add separator if Main Stats exist
            newLore.add(CUSTOM_LORE_HEADER);
            newLore.addAll(customLore);
        }

        // Apply new lore
=======
    // --- NEW: Vanilla Attribute Management (Req 1 & 2) ---

    /**
     * Removes all vanilla Attribute Modifiers (e.g., from Diamond Sword, Armor)
     * โดยไม่กระทบต่อ Name/Lore/Custom Attributes
     */
    public void removeVanillaAttributes(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();

        // FIX: ใช้ setAttributeModifiers(null) เพื่อลบ Attribute Modifiers ทั้งหมด
        if (meta.hasAttributeModifiers()) {
            meta.setAttributeModifiers(null);
        }

        // NEW: เพิ่ม ItemFlag.HIDE_ATTRIBUTES เพื่อให้มั่นใจว่า Lore ของ Vanilla Attributes จะถูกซ่อน
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Apply meta changes
        item.setItemMeta(meta);

        // Refresh lore to ensure consistency (สำคัญ)
        updateLore(item);
    }

    // --- Lore Management (Main Stats + Custom Lore) ---

    public void updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();

        // Use meta.getLore() (List<String>) to read the existing lore lines to preserve custom formatting/color codes.
        List<String> currentLore = meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : new ArrayList<>();

        List<String> newLore = new ArrayList<>();

        // 1. Separate existing lore to preserve CUSTOM LORE
        List<String> customLore = new ArrayList<>();
        boolean inCustomLore = false;

        for (String line : currentLore) {
            // Assuming currentLore contains Strings with Bukkit color codes ('§').
            String stripped = line.replaceAll("§[0-9a-fk-or]", "");
            if (stripped.equals("--- Custom Lore ---")) {
                inCustomLore = true;
                continue;
            }
            if (stripped.equals("--- Main Stats ---")) { // Reset flag if we hit the start of the Main Stats (shouldn't happen if they are at the top)
                inCustomLore = false;
                continue;
            }

            if (inCustomLore) {
                customLore.add(line);
            }
        }

        // 2. Generate new Main Stats Lore
        List<String> mainStats = new ArrayList<>();
        for (ItemAttribute attribute : ItemAttribute.values()) {
            double value = getAttribute(item, attribute);
            if (value != 0.0) {
                String formattedValue = String.format(attribute.getFormat(), value);
                String line = attribute.getDisplayName() + ": §f" + formattedValue;
                mainStats.add(line);
            }
        }

        // 3. Combine Lore: Main Stats (Auto-generated) + Custom Lore (Preserved)
        if (!mainStats.isEmpty()) {
            newLore.add(MAIN_STATS_HEADER);
            newLore.addAll(mainStats);
        }

        if (!customLore.isEmpty()) {
            if (!newLore.isEmpty()) newLore.add(" "); // Add separator if Main Stats exist
            newLore.add(CUSTOM_LORE_HEADER);
            newLore.addAll(customLore);
        }

        // Apply new lore
        // Convert String list back to Component list
>>>>>>> parent of d30d525 (0)
        meta.lore(newLore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
    }
}