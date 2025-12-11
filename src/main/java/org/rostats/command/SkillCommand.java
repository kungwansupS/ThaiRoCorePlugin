package org.rostats.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.gui.SkillLibraryGUI;

public class SkillCommand implements CommandExecutor {

    private final ThaiRoCorePlugin plugin;

    public SkillCommand(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cPlayers only.");
            return true;
        }

        if (!player.hasPermission("rostats.admin")) {
            sender.sendMessage("§cNo Permission.");
            return true;
        }

        // [FIXED] Allow empty args OR 'editor' arg to open GUI
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("editor"))) {
            // Open Skill Library GUI at root
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
            return true;
        }

        sender.sendMessage("§6Usage: /roskill editor");
        return true;
    }
}