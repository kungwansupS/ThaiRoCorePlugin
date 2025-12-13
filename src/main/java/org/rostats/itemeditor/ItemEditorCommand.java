package org.rostats.itemeditor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.rostats.ThaiRoCorePlugin;

public class ItemEditorCommand implements CommandExecutor {

    private final ThaiRoCorePlugin plugin;

    public ItemEditorCommand(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!player.hasPermission("rostats.itemeditor.admin")) {
            player.sendMessage("§cNo Permission.");
            return true;
        }

        // Open Root Library GUI
        new ItemLibraryGUI(plugin, plugin.getItemManager().getRootDir()).open(player);
        return true;
    }
}