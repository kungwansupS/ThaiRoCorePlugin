package org.rostats.engine.action.impl;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

import java.util.HashMap;
import java.util.Map;

public class ParticleAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String particleName;
    private final String countExpr;
    private final String speedExpr;
    private final String shape; // POINT, CIRCLE, SPHERE, CYLINDER, SQUARE
    private final String radiusExpr; // Radius or Height/Length
    private final String pointsExpr; // for shapes (density/slices/points)

    public ParticleAction(ThaiRoCorePlugin plugin, String particleName, String count, String speed, String shape, String radius, String points) {
        this.plugin = plugin;
        this.particleName = particleName;
        this.countExpr = count;
        this.speedExpr = speed;
        this.shape = shape;
        this.radiusExpr = radius;
        this.pointsExpr = points;
    }

    @Override
    public ActionType getType() {
        return ActionType.PARTICLE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            LivingEntity origin = (target != null) ? target : caster;
            Location loc = origin.getLocation().clone().add(0, 1.0, 0); // Center of the body

            int count = (int) FormulaParser.eval(countExpr, caster, target, level, context, plugin);
            double speed = FormulaParser.eval(speedExpr, caster, target, level, context, plugin);
            double radius = FormulaParser.eval(radiusExpr, caster, target, level, context, plugin);

            if (shape.equalsIgnoreCase("CIRCLE")) {
                int points = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (points <= 0) points = 20;
                double increment = (2 * Math.PI) / points;

                for (int i = 0; i < points; i++) {
                    double angle = i * increment;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);

                    Location spawnLoc = loc.clone().add(x, 0, z);
                    loc.getWorld().spawnParticle(particle, spawnLoc, 1, 0, 0, 0, speed);
                }
            }
            else if (shape.equalsIgnoreCase("SPHERE")) {
                int points = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (points <= 0) points = 50;
                double phi = Math.PI * (3.0 - Math.sqrt(5.0));

                for (int i = 0; i < points; i++) {
                    double y = 1 - (i / (float) (points - 1)) * 2;
                    double r = Math.sqrt(1 - y * y);
                    double theta = phi * i;
                    double x = Math.cos(theta) * r;
                    double z = Math.sin(theta) * r;

                    Location spawnLoc = loc.clone().add(x * radius, y * radius, z * radius);
                    loc.getWorld().spawnParticle(particle, spawnLoc, 1, 0, 0, 0, speed);
                }
            }
            else if (shape.equalsIgnoreCase("CYLINDER")) { // Vertical stack of circles (simulated)
                int slices = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (slices <= 0) slices = 5;
                double height = radius * 2; // Assuming radius is used as max height
                double yStep = height / (slices > 1 ? slices - 1 : 1);

                int particlesPerSlice = 10; // Fixed density per slice
                double angleStep = (2 * Math.PI) / particlesPerSlice;

                for (int i = 0; i < slices; i++) {
                    double y = -height / 2 + (i * yStep);
                    for (int j = 0; j < particlesPerSlice; j++) {
                        double angle = j * angleStep;
                        double x = radius * Math.cos(angle);
                        double z = radius * Math.sin(angle);

                        Location spawnLoc = loc.clone().add(x, y, z);
                        loc.getWorld().spawnParticle(particle, spawnLoc, 1, 0, 0, 0, speed);
                    }
                }
            }
            else if (shape.equalsIgnoreCase("SQUARE")) { // Simulated line/wall
                double length = radius * 2; // Using radius as max length
                int points = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (points <= 0) points = 20;

                // Get the direction vector from the caster's eye location
                Vector direction = caster.getEyeLocation().getDirection().normalize();

                // Simplified: Spread particles in a line along the direction
                for (int i = 0; i < points; i++) {
                    double distance = (double) i / points * length;
                    Location spawnLoc = loc.clone().add(direction.clone().multiply(distance));
                    loc.getWorld().spawnParticle(particle, spawnLoc, 1, 0, 0, 0, speed);
                }
            }
            else {
                // Default POINT
                loc.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, speed);
            }

        } catch (Exception e) {
            // Ignore invalid particle
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "PARTICLE");
        map.put("particle", particleName);
        map.put("count", countExpr);
        map.put("speed", speedExpr);
        map.put("shape", shape);
        map.put("radius", radiusExpr);
        map.put("points", pointsExpr);
        return map;
    }
}