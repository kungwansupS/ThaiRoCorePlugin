package org.rostats.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.rostats.ThaiRoCorePlugin;
// NEW: Import SkillManager for context
import org.rostats.engine.skill.SkillManager;

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

        if (args.length == 1 && args[0].equalsIgnoreCase("editor")) {
            // TODO: Implement SkillEditorGUI.open(player);

            player.sendMessage("§a[Skill Editor] Accessing Skill Manager (" + plugin.getSkillManager().getSkills().size() + " skills loaded).");
            player.sendMessage("§eระบบ GUI สำหรับการสร้างสกิลจะถูกเปิดในอนาคต! ตอนนี้ใช้ไฟล์ YAML ในโฟลเดอร์ skills/ ก่อน");

            // Example: Open a simple GUI (Placeholder for now)
            // new SkillEditorGUI(plugin).open(player);
            return true;
        }

        sender.sendMessage("§6Usage: /roskill editor");
        return true;
    }
}