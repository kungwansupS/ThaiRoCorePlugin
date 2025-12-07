package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
// Import the new main plugin class
import org.rostats.ThaiRoCorePlugin;

public class GUIListener implements Listener {

    // Change type from ItemEditorPlugin to ThaiRoCorePlugin
    private final ThaiRoCorePlugin plugin;
    private final ItemAttributeManager attributeManager;

    public GUIListener(ThaiRoCorePlugin plugin, ItemAttributeManager attributeManager) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
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

        // --- NEW: Handle Utility Buttons (R0, C5-C7) ---

        // Slot 8: Close Button
        if (slot == 8 && clickedItem.getType() == Material.BARRIER) {
            player.closeInventory();
            player.sendMessage("§aItem editing closed. Changes applied automatically.");
            return;
        }

        // Slot 6: Remove Vanilla Attributes (Req 1 & 2)
        if (slot == 6 && clickedItem.getType() == Material.REDSTONE_BLOCK) {
            attributeManager.removeVanillaAttributes(editingItem);
            // Re-open/refresh the GUI (สำคัญเพื่อให้เห็นการเปลี่ยนแปลงของ Item Icon)
            new AttributeEditorGUI(plugin, attributeManager).open(player, editingItem);
            player.sendMessage("§aVanilla Minecraft attributes removed (Attack Damage, Armor, etc.).");
            return;
        }

        // Slot 7: Save & Copy (Req 4 - Placeholder)
        if (slot == 7 && clickedItem.getType() == Material.ENDER_CHEST) {
            // In a real implementation: Save item PDC to database/file, then copy and give the item.
            ItemStack finalCopy = editingItem.clone();
            player.getInventory().addItem(finalCopy);
            player.sendMessage("§d[Editor] Item attributes saved (Placeholder) and a copy has been placed in your inventory.");
            return;
        }

        // Slot 5: Load Template (Req 4 - Placeholder)
        if (slot == 5 && clickedItem.getType() == Material.BOOK) {
            // In a real implementation: Open a secondary menu to select a template.
            player.sendMessage("§b[Editor] Loading templates is not yet implemented. Requires Template Manager system.");
            return;
        }

        // ---------------------------------------------------

        // 2. Prevent clicking on the item being edited (Slot 0)
        if (slot == 0) return;

        // 3. Handle Attribute Modification Clicks (Slots 9+)
        AttributeEditorGUI gui = new AttributeEditorGUI(plugin, attributeManager);
        ItemAttribute attribute = gui.getAttributeBySlot(slot);
        if (attribute == null) return; // Not an attribute slot

        ClickType click = event.getClick();
        double currentValue = attributeManager.getAttribute(editingItem, attribute);
        double newValue = currentValue;
        double step = attribute.getClickStep();
        double largeStep = attribute.getRightClickStep();

        if (click == ClickType.LEFT) {
            newValue += event.isShiftClick() ? largeStep : step;
        } else if (click == ClickType.RIGHT) {
            newValue -= event.isShiftClick() ? largeStep : step;
        } else if (click == ClickType.MIDDLE) {
            newValue = 0.0;
        }

        // Clamp to a sensible range
        newValue = Math.max(-1000.0, Math.min(1000.0, newValue));

        // Round to maintain precision to 3 decimal places (based on the steps defined in enum)
        newValue = Math.round(newValue * 1000.0) / 1000.0;

        // 4. Apply changes and refresh
        if (newValue != currentValue) {
            attributeManager.setAttribute(editingItem, attribute, newValue);

            // Update the item in the GUI slot 0
            event.getInventory().setItem(0, editingItem.clone());

            // Update the clicked attribute icon
            event.getInventory().setItem(slot, gui.createAttributeIcon(editingItem, attribute));

            // Trigger recalculation of player stats if a gear bonus was changed
            if (attribute.getKey().contains("BonusGear")) {
                plugin.getAttributeHandler().updatePlayerStats(player);
            }
        }
    }
}