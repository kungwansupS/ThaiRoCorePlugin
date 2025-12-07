package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.itemeditor.AttributeEditorGUI.Page;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final ItemAttributeManager manager;

    public GUIListener(ThaiRoCorePlugin plugin, ItemAttributeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("Item Editor")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack editingItem = event.getInventory().getItem(0);
        if (editingItem == null) return;

        int slot = event.getSlot();
        if (slot == 0) return; // Cant click preview

        // Navigation
        if (slot >= 45) {
            handleNavigation(player, editingItem, slot, title);
            return;
        }

        // Logic based on page title
        if (title.contains("GENERAL")) {
            if (slot == 24) { // Remove Vanilla
                editingItem = manager.removeVanillaAttributes(editingItem);
                event.getInventory().setItem(0, editingItem);
                player.sendMessage("§aRemoved vanilla attributes.");
                plugin.getAttributeHandler().updatePlayerStats(player); // Update Stats
            }
        } else if (title.contains("SAVE_APPLY")) {
            if (slot == 20) { // Save & Get
                player.getInventory().addItem(editingItem.clone());
                player.sendMessage("§aItem given.");
                plugin.getAttributeHandler().updatePlayerStats(player); // Update Stats
            } else if (slot == 24) {
                player.closeInventory();
            }
        } else {
            // Stat Pages
            handleStatClick(event, editingItem, player);
        }
    }

    private void handleNavigation(Player player, ItemStack item, int slot, String title) {
        Page currentPage = getPageFromTitle(title);
        Page nextPage = currentPage;

        if (slot == 45) { // Prev
            int idx = Math.max(0, currentPage.ordinal() - 1);
            nextPage = Page.values()[idx];
        } else if (slot == 53) { // Next
            int idx = Math.min(Page.values().length - 1, currentPage.ordinal() + 1);
            nextPage = Page.values()[idx];
        } else if (slot >= 47 && slot < 47 + Page.values().length) {
            nextPage = Page.values()[slot - 47];
        }

        if (nextPage != currentPage) {
            new AttributeEditorGUI(plugin, manager).open(player, item, nextPage);
        }
    }

    private Page getPageFromTitle(String title) {
        for (Page p : Page.values()) {
            if (title.contains(p.name())) return p;
        }
        return Page.GENERAL;
    }

    private void handleStatClick(InventoryClickEvent event, ItemStack item, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        // Find attribute type by displayName matching
        String name = PlainTextComponentSerializer.plainText().serialize(clicked.displayName());
        for (ItemAttributeType type : ItemAttributeType.values()) {
            if (type.getDisplayName().equals(name)) {
                double current = manager.getAttributeValue(item, type);
                double change = 0;

                if (event.getClick() == ClickType.LEFT) change = type.getClickStep();
                if (event.getClick() == ClickType.RIGHT) change = -type.getClickStep();
                if (event.getClick() == ClickType.SHIFT_LEFT) change = type.getRightClickStep();
                if (event.getClick() == ClickType.SHIFT_RIGHT) change = -type.getRightClickStep();

                double newVal = current + change;
                manager.setAttribute(item, type, newVal);

                // Refresh Icon
                AttributeEditorGUI gui = new AttributeEditorGUI(plugin, manager); // Hacky way to access createStatIcon if private, assuming accessible or copied logic
                // Ideally refresh page. For now, reopen page:
                String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
                new AttributeEditorGUI(plugin, manager).open(player, item, getPageFromTitle(title));

                // IMMEDIATE UPDATE
                plugin.getAttributeHandler().updatePlayerStats(player);
                return;
            }
        }
    }
}