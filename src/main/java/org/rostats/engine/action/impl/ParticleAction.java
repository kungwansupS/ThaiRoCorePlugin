package org.rostats.engine.action.impl;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class ParticleAction implements SkillAction {

    private final Particle particle;
    private final int count;
    private final double speed;
    private final double yOffset;

    public ParticleAction(Particle particle, int count, double speed, double yOffset) {
        this.particle = particle;
        this.count = count;
        this.speed = speed;
        this.yOffset = yOffset;
    }

    @Override
    public ActionType getType() {
        return ActionType.PARTICLE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        LivingEntity locEntity = (target != null) ? target : caster;
        locEntity.getWorld().spawnParticle(particle,
                locEntity.getLocation().add(0, yOffset, 0),
                count, 0.5, 0.5, 0.5, speed);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "PARTICLE");
        map.put("particle", particle.name());
        map.put("count", count);
        map.put("speed", speed);
        map.put("y-offset", yOffset);
        return map;
    }
}