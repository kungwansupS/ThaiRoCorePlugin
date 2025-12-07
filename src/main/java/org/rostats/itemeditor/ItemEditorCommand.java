package org.rostats.itemeditor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
// Import the new main plugin class
import org.rostats.ThaiRoCorePlugin;

public class ItemEditorCommand implements CommandExecutor {

    // Change type from ItemEditorPlugin to ThaiRoCorePlugin
    private final ThaiRoCorePlugin plugin;
    private final ItemAttributeManager attributeManager;

    public ItemEditorCommand(ThaiRoCorePlugin plugin, ItemAttributeManager attributeManager) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("rostats.itemeditor.admin")) {
            player.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.sendMessage("§cYou must be holding an item to edit its attributes.");
            return true;
        }

        // Update lore structure before opening (ensures the item can be edited safely)
        attributeManager.updateLore(item);

        new AttributeEditorGUI(plugin, attributeManager).open(player, item);

        return true;
    }
}