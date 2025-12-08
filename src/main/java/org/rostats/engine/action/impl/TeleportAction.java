package org.rostats.engine.action.impl;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class TeleportAction implements SkillAction {

    private final double range;
    private final boolean toTarget; // true = วาร์ปไปหาเป้า, false = พุ่งไปข้างหน้า

    public TeleportAction(double range, boolean toTarget) {
        this.range = range;
        this.toTarget = toTarget;
    }

    @Override
    public ActionType getType() {
        return ActionType.TELEPORT;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        Location start = caster.getLocation();

        if (toTarget && target != null) {
            // วาร์ปไปหาเป้าหมาย (ด้านหลัง)
            Location dest = target.getLocation().add(target.getLocation().getDirection().multiply(-1.0));
            dest.setDirection(target.getLocation().getDirection());
            caster.teleport(dest);
        } else {
            // พุ่งไปข้างหน้า (Dash)
            Vector dir = start.getDirection().multiply(range);
            Location dest = start.add(dir);
            // เช็คว่าติดบล็อกไหม (แบบง่าย)
            if (!dest.getBlock().getType().isSolid()) {
                caster.teleport(dest);
            }
        }

        // แก้ไขตรงนี้: เรียกผ่าน World แทน
        caster.getWorld().playSound(caster.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "TELEPORT");
        map.put("range", range);
        map.put("to-target", toTarget);
        return map;
    }
}