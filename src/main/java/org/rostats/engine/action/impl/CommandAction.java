package org.rostats.engine.action.impl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class CommandAction implements SkillAction {

    private final String command;
    private final boolean asConsole;

    public CommandAction(String command, boolean asConsole) {
        this.command = command;
        this.asConsole = asConsole;
    }

    @Override
    public ActionType getType() {
        return ActionType.COMMAND;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // เตรียมคำสั่งและแทนที่ตัวแปร (Placeholders)
        String finalCmd = command
                .replace("%level%", String.valueOf(level))
                .replace("%player%", caster.getName())
                .replace("%player_uuid%", caster.getUniqueId().toString());

        // [NEW] แทนที่ตัวแปรจาก Loop/Context (เช่น %i%, %angle%)
        if (context != null) {
            for (Map.Entry<String, Double> entry : context.entrySet()) {
                finalCmd = finalCmd.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
            }
        }

        // ตัวแปรเกี่ยวกับตำแหน่งผู้ร่าย
        Location loc = caster.getLocation();
        finalCmd = finalCmd
                .replace("%x%", String.format("%.2f", loc.getX()))
                .replace("%y%", String.format("%.2f", loc.getY()))
                .replace("%z%", String.format("%.2f", loc.getZ()))
                .replace("%world%", loc.getWorld().getName());

        // ตัวแปรเกี่ยวกับเป้าหมาย (ถ้ามี)
        if (target != null) {
            finalCmd = finalCmd
                    .replace("%target%", target.getName())
                    .replace("%target_uuid%", target.getUniqueId().toString())
                    .replace("%target_x%", String.format("%.2f", target.getLocation().getX()))
                    .replace("%target_y%", String.format("%.2f", target.getLocation().getY()))
                    .replace("%target_z%", String.format("%.2f", target.getLocation().getZ()));
        } else {
            // ถ้าไม่มีเป้าหมาย ให้ใช้ค่าของผู้ร่ายแทน หรือเว้นว่างไว้
            finalCmd = finalCmd
                    .replace("%target%", caster.getName())
                    .replace("%target_uuid%", caster.getUniqueId().toString())
                    .replace("%target_x%", String.format("%.2f", loc.getX()))
                    .replace("%target_y%", String.format("%.2f", loc.getY()))
                    .replace("%target_z%", String.format("%.2f", loc.getZ()));
        }

        // รันคำสั่ง
        if (asConsole) {
            // รันในนาม Console (OP)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd);
        } else {
            // รันในนามผู้เล่น (ต้องเป็น Player เท่านั้น)
            if (caster instanceof Player player) {
                player.performCommand(finalCmd);
            }
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "COMMAND");
        map.put("command", command);
        map.put("as-console", asConsole);
        return map;
    }
}