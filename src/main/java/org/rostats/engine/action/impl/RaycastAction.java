package org.rostats.engine.action.impl;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class RaycastAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String rangeExpr;
    private final String subSkillId;
    private final String targetType; // SINGLE, AOE

    public RaycastAction(ThaiRoCorePlugin plugin, String range, String subSkillId, String targetType) {
        this.plugin = plugin;
        this.rangeExpr = range;
        this.subSkillId = subSkillId;
        this.targetType = targetType.toUpperCase();
    }

    @Override
    public ActionType getType() {
        return ActionType.RAYCAST;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        if (subSkillId.equalsIgnoreCase("none")) return;

        double range = 10.0;
        try {
            range = FormulaParser.eval(rangeExpr, caster, target, level, context, plugin);
        } catch (Exception e) {
            // Fallback to default
        }
        if (range <= 0.0) return;

        if (targetType.equals("SINGLE")) {
            Predicate<Entity> filter = e -> e instanceof LivingEntity && !e.equals(caster);

            RayTraceResult result = caster.getWorld().rayTraceEntities(
                    caster.getEyeLocation(),
                    caster.getEyeLocation().getDirection(),
                    range,
                    0.5,
                    filter
            );

            if (result != null && result.getHitEntity() instanceof LivingEntity victim) {
                plugin.getSkillManager().castSkill(caster, subSkillId, level, victim, true);
            }
        } else if (targetType.equals("AOE")) {
            // Simple AOE logic (simulates MOB_AROUND)
            Location center = (target != null) ? target.getLocation() : caster.getLocation();
            Collection<Entity> nearby = center.getNearbyEntities(range, range, range);
            for (Entity e : nearby) {
                if (e instanceof LivingEntity victim && !e.equals(caster)) {
                    plugin.getSkillManager().castSkill(caster, subSkillId, level, victim, true);
                }
            }
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "RAYCAST");
        map.put("range", rangeExpr);
        map.put("sub-skill", subSkillId);
        map.put("target-type", targetType);
        return map;
    }
}