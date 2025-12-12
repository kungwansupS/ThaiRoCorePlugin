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

        // [FIX] อัปเดตการเรียกใช้ Constructor ให้ตรงกับ SkillLibraryGUI ล่าสุด
        // โดยส่ง Root Directory และ Page 0 ไปด้วย
        new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir(), 0).open(player);
        return true;
    }
}