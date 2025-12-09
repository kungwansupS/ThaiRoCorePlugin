package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.input.ChatInputHandler;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // 1. Main Category Menu
        if (title.equals(AttributeEditorGUI.TITLE_MAIN.replaceAll("§.", ""))) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.BARRIER) {
                player.closeInventory();
                return;
            }

            for (AttributeEditorGUI.EditorCategory cat : AttributeEditorGUI.EditorCategory.values()) {
                if (clicked.getType() == cat.icon) {
                    // Check logic can be improved by comparing DisplayName if icons are reused
                    // But for this setup, icons are unique enough per category set
                    plugin.getAttributeEditorGUI().openAttributeList(player, cat);
                    plugin.getItemEditorCommand().playClickSound(player);
                    return;
                }
            }
        }

        // 2. Attribute List Page
        else if (title.startsWith(AttributeEditorGUI.TITLE_PREFIX_CATEGORY.replaceAll("§.", ""))) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player player)) return;
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            if (clicked.getType() == Material.ARROW && event.getSlot() == 49) {
                plugin.getAttributeEditorGUI().openCategoryMenu(player);
                plugin.getItemEditorCommand().playClickSound(player);
                return;
            }

            if (clicked.getType() == Material.PAPER) {
                String displayName = PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName());
                // Strip colors and bold
                String rawName = displayName.replaceAll("§.", "").trim();

                ItemAttributeType type = ItemAttributeType.fromDisplayName(rawName);

                if (type == null) {
                    player.sendMessage("§cError: Could not identify attribute '" + rawName + "'");
                    return;
                }

                if (event.isRightClick()) {
                    ItemStack hand = player.getInventory().getItemInMainHand();
                    plugin.getItemAttributeManager().removeAttribute(hand, type);
                    player.sendMessage("§c[Editor] Removed " + type.getDisplayName());
                    player.closeInventory();
                } else {
                    player.closeInventory();
                    player.sendMessage("§e[Editor] Please type the value for §f" + type.getDisplayName() + " §ein chat.");
                    player.sendMessage("§7(Type 'cancel' to abort)");

                    new ChatInputHandler(plugin).awaitInput(player, (input) -> {
                        try {
                            double value = Double.parseDouble(input);
                            ItemStack hand = player.getInventory().getItemInMainHand();
                            plugin.getItemAttributeManager().setAttribute(hand, type, value);
                            player.sendMessage("§a[Editor] Set " + type.getDisplayName() + " to " + value);
                            plugin.getItemEditorCommand().playClickSound(player);
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid number format.");
                        }
                    });
                }
            }
        }
    }
}