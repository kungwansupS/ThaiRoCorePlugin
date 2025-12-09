package org.rostats.cmd;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;

public class CooldownCommand implements CommandExecutor {

    private final ThaiRoCorePlugin plugin;

    public CooldownCommand(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        
        // แสดง cooldown แบบละเอียดใน chat
        plugin.getCooldownDisplay().showDetailedCooldown(player);
        
        return true;
    }
}
