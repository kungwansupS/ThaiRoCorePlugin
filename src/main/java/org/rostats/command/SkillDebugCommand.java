package org.rostats.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.impl.DamageAction;

import java.util.ArrayList;
import java.util.List;

public class SkillDebugCommand implements CommandExecutor, TabCompleter {

    private final ThaiRoCorePlugin plugin;

    public SkillDebugCommand(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rostats.admin")) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§6§lSkill Debug System");
            sender.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§7Usage:");
            sender.sendMessage("§e/skilldebug §fon  §7- Enable debug mode");
            sender.sendMessage("§e/skilldebug §foff §7- Disable debug mode");
            sender.sendMessage("§e/skilldebug §fstatus §7- Check current status");
            sender.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return true;
        }

        String subCmd = args[0].toLowerCase();

        switch (subCmd) {
            case "on":
            case "enable":
                DamageAction.setDebugMode(true);
                sender.sendMessage("§a✓ Skill debug mode §lENABLED");
                sender.sendMessage("§7All damage calculations will show debug info.");
                break;

            case "off":
            case "disable":
                DamageAction.setDebugMode(false);
                sender.sendMessage("§c✗ Skill debug mode §lDISABLED");
                sender.sendMessage("§7Debug messages are now hidden.");
                break;

            case "status":
            case "check":
                boolean isEnabled = DamageAction.isDebugMode();
                sender.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§6§lSkill Debug Status");
                sender.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§7Debug Mode: " + (isEnabled ? "§a§lENABLED" : "§c§lDISABLED"));
                sender.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                break;

            default:
                sender.sendMessage("§cInvalid subcommand. Use: §e/skilldebug §7for help.");
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("on");
            completions.add("off");
            completions.add("status");
            
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        }

        return completions;
    }
}
