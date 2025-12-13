package org.rostats.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.gui.SkillLibraryGUI;

public class SkillCommand implements CommandExecutor {

    private final ThaiRoCorePlugin plugin;

    public SkillCommand(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (!player.hasPermission("thairo.admin")) {
            player.sendMessage("§cYou don't have permission.");
            return true;
        }

        // เปิด GUI ที่หน้า Root (ใช้ Constructor แบบ 1 ค่า ที่เพิ่มไปใน SkillLibraryGUI)
        new SkillLibraryGUI(plugin).open(player);
        return true;
    }
}