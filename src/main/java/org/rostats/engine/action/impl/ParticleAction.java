package org.rostats.engine.action.impl;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class ParticleAction implements SkillAction {

    private final String particleName;
    private final int count;
    private final double speed;
    private final double offset;

    public ParticleAction(String particleName, int count, double speed, double offset) {
        this.particleName = particleName;
        this.count = count;
        this.speed = speed;
        this.offset = offset;
    }

    @Override
    public ActionType getType() {
        return ActionType.PARTICLE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            LivingEntity locEntity = (target != null) ? target : caster;

            locEntity.getWorld().spawnParticle(
                    particle,
                    locEntity.getLocation().add(0, 1, 0),
                    count,
                    offset, offset, offset,
                    speed
            );
        } catch (IllegalArgumentException e) {
            // Ignore invalid particle
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "PARTICLE");
        map.put("particle", particleName);
        map.put("count", count);
        map.put("speed", speed);
        map.put("offset", offset);
        return map;
    }
}