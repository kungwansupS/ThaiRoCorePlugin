package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
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

    // แก้ไข: เปลี่ยน Type ใน Constructor เป็น ThaiRoCorePlugin
    public ItemAttributeManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        // Initialize NamespacedKeys once
        for (ItemAttribute attribute : ItemAttribute.values()) {
            attribute.initialize(plugin);
        }
    }

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

    // --- Lore Management (Main Stats + Custom Lore) ---

    public void updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        List<String> currentLore = meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : new ArrayList<>();

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
        meta.lore(newLore.stream().map(Component::text).toList());
        item.setItemMeta(meta);
    }
}