package org.rostats.engine.action.impl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AreaAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final double radius;
    private final String targetType; // "ENEMY", "ALLY", "ALL"
    private final String subSkillId;
    private final int maxTargets;

    public AreaAction(ThaiRoCorePlugin plugin, double radius, String targetType, String subSkillId, int maxTargets) {
        this.plugin = plugin;
        this.radius = radius;
        this.targetType = targetType.toUpperCase();
        this.subSkillId = subSkillId;
        this.maxTargets = maxTargets;
    }

    @Override
    public ActionType getType() {
        return ActionType.AREA_EFFECT;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        // ถ้ามี target (เช่นจาก Projectile ชน) ให้ใช้ location ของ target เป็นจุดศูนย์กลาง
        // ถ้าไม่มี (เช่นกดใช้เอง) ให้ใช้ location ของ caster
        LivingEntity centerEntity = (target != null) ? target : caster;

        List<Entity> nearby = centerEntity.getNearbyEntities(radius, radius, radius);
        int count = 0;

        for (Entity e : nearby) {
            if (count >= maxTargets) break;
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity victim = (LivingEntity) e;

            if (isValidTarget(caster, victim)) {
                // สั่งร่ายสกิลย่อยใส่เป้าหมายที่เจอ
                // isPassive = true เพื่อไม่ให้เช็ค CD/SP ซ้ำซ้อน (เพราะจ่ายไปแล้วตอนกดสกิลหลัก)
                plugin.getSkillManager().castSkill(caster, subSkillId, level, victim, true);
                count++;
            }
        }
    }

    private boolean isValidTarget(LivingEntity caster, LivingEntity victim) {
        if (victim.equals(caster)) return false; // ไม่โดนตัวเอง (ถ้าอยากโดนต้องสร้างสกิลแยก)

        switch (targetType) {
            case "ENEMY":
                // Logic แยกมิตร/ศัตรูแบบง่าย
                if (caster instanceof Player && victim instanceof Player) return true; // PVP (เปิดตลอดในตัวอย่างนี้)
                if (caster instanceof Player && !(victim instanceof Player)) return true; // Player ตี Mob
                return false;
            case "ALLY":
                if (caster instanceof Player && victim instanceof Player) return true; // Player ช่วย Player
                return false;
            case "ALL":
                return true;
            default:
                return true;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "AREA_EFFECT");
        map.put("radius", radius);
        map.put("target-type", targetType);
        map.put("sub-skill", subSkillId);
        map.put("max-targets", maxTargets);
        return map;
    }
}