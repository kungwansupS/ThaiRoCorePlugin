package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.gui.CharacterGUI; // For refreshing the Stat Panel (Req 7)

import java.io.File;
import java.util.Objects;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final ItemAttributeManager attributeManager;
    private final CharacterGUI statPanel; // For refreshing the Stat Panel (Req 7)

    public GUIListener(ThaiRoCorePlugin plugin, ItemAttributeManager attributeManager, CharacterGUI statPanel) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
        this.statPanel = statPanel;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!title.contains(AttributeEditorGUI.GUI_TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clickedItem = event.getCurrentItem();
        ItemStack editingItem = event.getInventory().getItem(0);

        if (clickedItem == null || clickedItem.getType().isAir()) return;
        if (editingItem == null || editingItem.getType().isAir()) return;

        int slot = event.getSlot();
        int page;
        try {
            // Extract page number from title "§0§lItem Attribute Editor (Page X)"
            String pageString = title.substring(title.lastIndexOf("Page ") + 5, title.lastIndexOf(")"));
            page = Integer.parseInt(pageString);
        } catch (Exception e) {
            return; // Invalid page title
        }

        // 1. Prevent clicking on the item being edited (Slot 0)
        if (slot == 0) return;

        // --- Handle Header / Navigation Buttons (Slots 2-8) ---
        if (slot == 8 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        } else if (slot == 2 && clickedItem.getType() == Material.ARROW) {
            new AttributeEditorGUI(plugin, attributeManager).open(player, editingItem, page - 1);
            return;
        } else if (slot == 4 && clickedItem.getType() == Material.ARROW) {
            new AttributeEditorGUI(plugin, attributeManager).open(player, editingItem, page + 1);
            return;
        } else if (slot == 6 && clickedItem.getType() == Material.REDSTONE_BLOCK) {
            // Toggle Remove Vanilla
            ItemAttribute currentAttr = attributeManager.getAttributesFromItem(editingItem);
            boolean isEnabled = currentAttr.isRemoveVanillaAttribute();
            attributeManager.setAttribute(editingItem, "removeVanillaAttribute", isEnabled ? 0.0 : 1.0);

            // Re-open/refresh the GUI (สำคัญเพื่อให้เห็นการเปลี่ยนแปลงของ Status)
            new AttributeEditorGUI(plugin, attributeManager).open(player, editingItem, page);
            plugin.getAttributeHandler().updatePlayerStats(player); // Recalculate stats immediately (Req 7)
            return;
        } else if (slot == 7 && clickedItem.getType() == Material.ENDER_CHEST) {
            // Save & Copy
            attributeManager.saveItemAsTemplate(editingItem, player);
            ItemStack finalCopy = editingItem.clone();
            player.getInventory().addItem(finalCopy);
            player.sendMessage("§d[Editor] Item saved and a copy has been placed in your inventory.");
            return;
        } else if (slot == 53 && page == 6 && clickedItem.getType() == Material.WRITABLE_BOOK) {
            // Export to YAML (Page 6 Utility)
            if (attributeManager.saveItemAsTemplate(editingItem, player)) {
                player.sendMessage("§a[Editor] Item exported to YAML template file!");
            } else {
                player.sendMessage("§c[Editor] Could not export item to YAML.");
            }
            return;
        }

        // 2. Handle Attribute Modification Clicks (Slots 9+)
        AttributeEditorGUI gui = new AttributeEditorGUI(plugin, attributeManager);
        String attributeKey = gui.getAttributeKeyBySlot(slot, page);

        if (attributeKey != null) {
            handleAttributeModification(player, editingItem, attributeKey, event.getClick(), page, gui);
        }
    }

    private void handleAttributeModification(Player player, ItemStack editingItem, String attributeKey, ClickType click, int page, AttributeEditorGUI gui) {
        AttributeEditorGUI.AttributeMeta meta = AttributeEditorGUI.ATTRIBUTE_METADATA.get(attributeKey);

        // Handle boolean fields (Toggle)
        if (meta.dataType.equals("boolean")) {
            ItemAttribute currentAttr = attributeManager.getAttributesFromItem(editingItem);
            boolean currentValue = currentAttr.isRemoveVanillaAttribute();
            attributeManager.setAttribute(editingItem, attributeKey, currentValue ? 0.0 : 1.0);

            new AttributeEditorGUI(plugin, attributeManager).open(player, editingItem, page); // Refresh
            plugin.getAttributeHandler().updatePlayerStats(player); // Recalculate stats immediately (Req 7)
            return;
        }

        // Handle numerical fields (Double/Int)

        // Use reflection to get the current value from the ItemAttribute POJO
        ItemAttribute currentAttr = attributeManager.getAttributesFromItem(editingItem);
        double currentValue;
        try {
            Field field = ItemAttribute.class.getDeclaredField(attributeKey);
            field.setAccessible(true);
            Object value = field.get(currentAttr);
            currentValue = (value instanceof Integer) ? (Integer) value : (Double) value;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            player.sendMessage("§cError: Could not read attribute value.");
            return;
        }

        double newValue = currentValue;
        double step = meta.clickStep;
        double largeStep = meta.rightClickStep;

        if (click == ClickType.LEFT) {
            newValue += event.isShiftClick() ? largeStep : step;
        } else if (click == ClickType.RIGHT) {
            newValue -= event.isShiftClick() ? largeStep : step;
        } else if (click == ClickType.MIDDLE) {
            newValue = 0.0;
        }

        // Clamp to a sensible range
        newValue = Math.max(-1000000.0, Math.min(1000000.0, newValue));

        // Round to maintain precision to 3 decimal places
        newValue = Math.round(newValue * 1000.0) / 1000.0;

        // Ensure integer fields remain integers
        if (meta.dataType.equals("int")) {
            newValue = (double) Math.round(newValue);
        }

        // 4. Apply changes and refresh
        if (newValue != currentValue) {
            attributeManager.setAttribute(editingItem, attributeKey, newValue);

            // Update the item in the GUI slot 0
            event.getInventory().setItem(0, editingItem.clone());

            // Update the clicked attribute icon
            event.getInventory().setItem(slot, gui.createAttributeIcon(editingItem, attributeKey));

            // Trigger recalculation of player stats if it affects gear
            plugin.getAttributeHandler().updatePlayerStats(player); // Recalculate stats immediately (Req 7)
        }
    }
}