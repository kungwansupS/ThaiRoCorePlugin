package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class VelocityAction implements SkillAction {

    private final double x;
    private final double y;
    private final double z;
    private final boolean add;

    public VelocityAction(double x, double y, double z, boolean add) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.add = add;
    }

    @Override
    public ActionType getType() {
        return ActionType.VELOCITY;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        LivingEntity entity = (target != null) ? target : caster;
        Vector v = new Vector(x, y, z);

        if (add) {
            entity.setVelocity(entity.getVelocity().add(v));
        } else {
            entity.setVelocity(v);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "VELOCITY");
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("add", add);
        return map;
    }
}