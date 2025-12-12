package org.rostats.engine.action.impl;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
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
    private final String targetType; // SINGLE, AOE, LOCATION

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
            // Fallback
        }
        if (range <= 0.0) return;

        // Predicate: ไม่รวม caster และ entities ที่ไม่ใช่ LivingEntity
        Predicate<Entity> hitFilter = e -> e instanceof LivingEntity && !e.equals(caster);

        if (targetType.equals("LOCATION")) {
            // Raycast หา Block/Entity ที่ชน
            RayTraceResult result = caster.getWorld().rayTrace(
                    caster.getEyeLocation(),
                    caster.getEyeLocation().getDirection(),
                    range,
                    FluidCollisionMode.NEVER,
                    true,
                    0.5,
                    hitFilter
            );

            Location hitLoc = null;
            if (result != null) {
                if (result.getHitEntity() != null) {
                    hitLoc = result.getHitEntity().getLocation();
                } else if (result.getHitBlock() != null) {
                    hitLoc = result.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
                }
            }

            if (hitLoc != null) {
                // สร้าง ArmorStand ชั่วคราวเพื่อเป็นเป้าหมายสกิลที่พิกัดที่ชน
                final ArmorStand marker = (ArmorStand) hitLoc.getWorld().spawnEntity(hitLoc, EntityType.ARMOR_STAND);
                marker.setVisible(false);
                marker.setGravity(false);
                marker.setMarker(true);
                marker.setSmall(true);

                // ร่ายสกิลใส่ Marker
                // [FIXED]: ใช้ marker เป็นเป้าหมาย
                plugin.getSkillManager().castSkill(caster, subSkillId, level, marker, true);

                // ลบ Marker ทิ้ง
                plugin.getServer().getScheduler().runTaskLater(plugin, marker::remove, 20L);
            }

        } else if (targetType.equals("SINGLE")) {
            // Raycast หา LivingEntity ตัวแรกที่ชน
            RayTraceResult result = caster.getWorld().rayTraceEntities(
                    caster.getEyeLocation(),
                    caster.getEyeLocation().getDirection(),
                    range,
                    0.5,
                    hitFilter
            );

            if (result != null && result.getHitEntity() instanceof LivingEntity victim) {
                // [FIXED]: victim เป็นเป้าหมาย
                plugin.getSkillManager().castSkill(caster, subSkillId, level, victim, true);
            }
        } else if (targetType.equals("AOE")) {
            // AOE จากตำแหน่งเป้าหมายเดิม (ถ้ามี) หรือ Caster
            Location center = (target != null) ? target.getLocation() : caster.getLocation();
            Collection<Entity> nearby = center.getNearbyEntities(range, range, range);
            for (Entity e : nearby) {
                if (e instanceof LivingEntity victim && !e.equals(caster)) {
                    // [FIXED]: victim เป็นเป้าหมาย
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