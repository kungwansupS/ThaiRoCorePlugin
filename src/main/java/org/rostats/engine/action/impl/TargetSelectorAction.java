package org.rostats.engine.action.impl;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TargetSelectorAction implements SkillAction {

    public enum SelectorMode {
        SELF,           // ตัวผู้ใช้
        ORIGINAL,       // เป้าหมายแรกสุดที่กดสกิลใส่
        NEAREST_ENEMY,  // ศัตรูที่ใกล้ที่สุด
        NEAREST_ALLY,   // เพื่อนที่ใกล้ที่สุด
        CURSOR,         // เป้าหมายที่เล็งอยู่ (Raycast)
        RANDOM_NEARBY   // สุ่มคนรอบตัว
    }

    private final SelectorMode mode;
    private final double radius;

    public TargetSelectorAction(SelectorMode mode, double radius) {
        this.mode = mode;
        this.radius = radius;
    }

    @Override
    public ActionType getType() {
        return ActionType.SELECT_TARGET;
    }

    /**
     * คำนวณหาเป้าหมายใหม่
     * @param caster ผู้ร่าย
     * @param originalTarget เป้าหมายดั้งเดิม (ตอนเริ่มสกิล)
     * @return LivingEntity เป้าหมายใหม่ (อาจเป็น null ถ้าหาไม่เจอ)
     */
    public LivingEntity resolveTarget(LivingEntity caster, LivingEntity originalTarget) {
        switch (mode) {
            case SELF:
                return caster;
            case ORIGINAL:
                return originalTarget;
            case CURSOR:
                return getCursorTarget(caster, radius);
            case NEAREST_ENEMY:
                return getNearestEntity(caster, radius, true);
            case NEAREST_ALLY:
                return getNearestEntity(caster, radius, false);
            case RANDOM_NEARBY:
                List<LivingEntity> nearby = getNearbyEntities(caster, radius);
                if (nearby.isEmpty()) return null;
                return nearby.get((int) (Math.random() * nearby.size()));
            default:
                return caster;
        }
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // ทำงานใน SkillRunner โดยตรง
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getType().name());
        map.put("mode", mode.name());
        map.put("radius", radius);
        return map;
    }

    // --- Helper Methods ---

    private LivingEntity getCursorTarget(LivingEntity caster, double range) {
        Location start = caster.getEyeLocation();
        RayTraceResult result = caster.getWorld().rayTraceEntities(start, start.getDirection(), range,
                e -> e instanceof LivingEntity && !e.getUniqueId().equals(caster.getUniqueId()));
        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }
        return null; // ไม่เจอเป้า
    }

    private LivingEntity getNearestEntity(LivingEntity center, double range, boolean enemy) {
        List<LivingEntity> nearby = getNearbyEntities(center, range);

        // Simple Enemy Logic: ถ้าเป็น Player เหมือนกันถือเป็น Ally, ถ้าเป็น Mob ถือเป็น Enemy
        // (ในเซิร์ฟจริงควรเช็ค Party/Clan/PVP Flag)
        return nearby.stream()
                .filter(e -> {
                    boolean isPlayer = e instanceof Player;
                    boolean casterIsPlayer = center instanceof Player;
                    boolean sameType = (isPlayer == casterIsPlayer);
                    return enemy ? !sameType : sameType;
                })
                .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(center.getLocation())))
                .orElse(null);
    }

    private List<LivingEntity> getNearbyEntities(LivingEntity center, double range) {
        return center.getNearbyEntities(range, range, range).stream()
                .filter(e -> e instanceof LivingEntity && !e.getUniqueId().equals(center.getUniqueId()))
                .map(e -> (LivingEntity) e)
                .collect(Collectors.toList());
    }
}