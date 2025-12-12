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
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // ใช้ตำแหน่งของ target เป็นจุดศูนย์กลางถ้ามี, ถ้าไม่มีใช้ของ caster
        LivingEntity centerEntity = (target != null) ? target : caster;

        List<Entity> nearby = centerEntity.getNearbyEntities(radius, radius, radius);
        int count = 0;

        for (Entity e : nearby) {
            if (count >= maxTargets) break;
            if (!(e instanceof LivingEntity)) continue;
            LivingEntity victim = (LivingEntity) e;

            if (isValidTarget(caster, victim)) {
                // ร่ายสกิลย่อยใส่เป้าหมาย
                // [MODIFIED] ส่ง context เข้าไปด้วย
                plugin.getSkillManager().castSkill(caster, subSkillId, level, victim, true, context);
                count++;
            }
        }
    }

    private boolean isValidTarget(LivingEntity caster, LivingEntity victim) {
        if (victim.equals(caster)) return false;

        switch (targetType) {
            case "ENEMY":
                if (caster instanceof Player && victim instanceof Player) return true; // PVP
                if (caster instanceof Player && !(victim instanceof Player)) return true; // PVE
                return false;
            case "ALLY":
                if (caster instanceof Player && victim instanceof Player) return true;
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