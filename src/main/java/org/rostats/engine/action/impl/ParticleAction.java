package org.rostats.engine.action.impl;

import org.bukkit.Color;
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
    private final String shape; // POINT, CIRCLE, SPHERE, CYLINDER, SQUARE, STAR, SPIRAL
    private final String radiusExpr; // Radius or Height/Length
    private final String pointsExpr; // for shapes (density/slices/points)

    // Advanced FX Fields
    private final String colorExpr; // Format: "R,G,B" e.g., "255,0,0"
    private final String rotationExpr; // Format: "X,Y,Z" (Degrees)
    private final String offsetExpr; // Format: "X,Y,Z" (Displacement)

    // Full Constructor
    public ParticleAction(ThaiRoCorePlugin plugin, String particleName, String count, String speed, String shape, String radius, String points, String color, String rotation, String offset) {
        this.plugin = plugin;
        this.particleName = particleName;
        this.countExpr = count;
        this.speedExpr = speed;
        this.shape = shape;
        this.radiusExpr = radius;
        this.pointsExpr = points;
        this.colorExpr = color != null ? color : "0,0,0";
        this.rotationExpr = rotation != null ? rotation : "0,0,0";
        this.offsetExpr = offset != null ? offset : "0,0,0";
    }

    // Legacy Constructor
    public ParticleAction(ThaiRoCorePlugin plugin, String particleName, String count, String speed, String shape, String radius, String points) {
        this(plugin, particleName, count, speed, shape, radius, points, "255,255,255", "0,0,0", "0,0,0");
    }

    @Override
    public ActionType getType() {
        return ActionType.PARTICLE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        try {
            Particle particle = getSafeParticle(particleName);
            LivingEntity origin = (target != null) ? target : caster;

            // Base Values
            int count = (int) FormulaParser.eval(countExpr, caster, target, level, context, plugin);
            double speed = FormulaParser.eval(speedExpr, caster, target, level, context, plugin);
            double radius = FormulaParser.eval(radiusExpr, caster, target, level, context, plugin);

            // Advanced FX Vectors
            Vector rot = parseVector(rotationExpr, caster, target, level, context); // Rotation (Degrees)
            Vector off = parseVector(offsetExpr, caster, target, level, context);   // Offset (Blocks)

            // Center Point
            Location center = origin.getLocation().clone().add(0, 1.0, 0).add(off);

            if (shape.equalsIgnoreCase("CIRCLE")) {
                int points = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (points <= 0) points = 20;
                double increment = (2 * Math.PI) / points;

                for (int i = 0; i < points; i++) {
                    double angle = i * increment;
                    Vector v = new Vector(radius * Math.cos(angle), 0, radius * Math.sin(angle));
                    spawnParticle(center, v, rot, particle, count, speed, caster, target, level, context);
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

                    Vector v = new Vector(Math.cos(theta) * r * radius, y * radius, Math.sin(theta) * r * radius);
                    spawnParticle(center, v, rot, particle, count, speed, caster, target, level, context);
                }
            }
            else if (shape.equalsIgnoreCase("CYLINDER")) {
                int slices = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (slices <= 0) slices = 5;
                double height = radius * 2;
                double yStep = height / (slices > 1 ? slices - 1 : 1);
                int particlesPerSlice = 10;
                double angleStep = (2 * Math.PI) / particlesPerSlice;

                for (int i = 0; i < slices; i++) {
                    double y = -height / 2 + (i * yStep);
                    for (int j = 0; j < particlesPerSlice; j++) {
                        double angle = j * angleStep;
                        Vector v = new Vector(radius * Math.cos(angle), y, radius * Math.sin(angle));
                        spawnParticle(center, v, rot, particle, count, speed, caster, target, level, context);
                    }
                }
            }
            // [NEW] STAR SHAPE
            else if (shape.equalsIgnoreCase("STAR")) {
                int spikes = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (spikes < 3) spikes = 5; // Default 5-pointed star

                double innerRadius = radius * 0.4; // Inner vertex distance
                double step = Math.PI / spikes; // Half-step angle
                int particlesPerLine = 4; // Density of lines

                for (int i = 0; i < 2 * spikes; i++) {
                    // Current vertex
                    double r1 = (i % 2 == 0) ? radius : innerRadius;
                    double a1 = i * step;
                    Vector p1 = new Vector(r1 * Math.sin(a1), 0, r1 * Math.cos(a1));

                    // Next vertex
                    double r2 = ((i + 1) % 2 == 0) ? radius : innerRadius;
                    double a2 = (i + 1) * step;
                    Vector p2 = new Vector(r2 * Math.sin(a2), 0, r2 * Math.cos(a2));

                    // Interpolate Line between p1 and p2
                    Vector direction = p2.clone().subtract(p1);
                    double length = direction.length();
                    direction.normalize();
                    double distStep = length / particlesPerLine;

                    for (int j = 0; j < particlesPerLine; j++) {
                        Vector v = p1.clone().add(direction.clone().multiply(j * distStep));
                        spawnParticle(center, v, rot, particle, count, speed, caster, target, level, context);
                    }
                }
            }
            // [NEW] SPIRAL SHAPE
            else if (shape.equalsIgnoreCase("SPIRAL")) {
                int points = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (points <= 0) points = 30;
                double height = radius * 2;
                double coils = 3.0; // 3 Full rotations
                double yStep = height / points;
                double angleStep = (2 * Math.PI * coils) / points;

                for (int i = 0; i < points; i++) {
                    double y = -height / 2 + (i * yStep);
                    double angle = i * angleStep;

                    Vector v = new Vector(radius * Math.cos(angle), y, radius * Math.sin(angle));
                    spawnParticle(center, v, rot, particle, count, speed, caster, target, level, context);
                }
            }
            else if (shape.equalsIgnoreCase("SQUARE")) { // Line/Beam
                double length = radius * 2;
                int points = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (points <= 0) points = 20;
                Vector direction = caster.getEyeLocation().getDirection().normalize();
                for (int i = 0; i < points; i++) {
                    double distance = (double) i / points * length;
                    Vector v = direction.clone().multiply(distance);
                    spawnParticle(center, v, rot, particle, count, speed, caster, target, level, context);
                }
            }
            else {
                // Default POINT
                spawnParticle(center, new Vector(0,0,0), rot, particle, count, speed, caster, target, level, context);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Particle getSafeParticle(String name) {
        if (name == null) return Particle.FLAME;
        String upper = name.toUpperCase();
        if (upper.equals("REDSTONE")) return Particle.DUST;
        if (upper.equals("SPELL_MOB") || upper.equals("SPELL_MOB_AMBIENT")) return Particle.ENTITY_EFFECT;
        try { return Particle.valueOf(upper); } catch (IllegalArgumentException e) { return Particle.FLAME; }
    }

    private void spawnParticle(Location center, Vector v, Vector rot, Particle particle, int count, double speed, LivingEntity caster, LivingEntity target, int level, Map<String, Double> ctx) {
        rotateVector(v, rot.getX(), rot.getY(), rot.getZ());
        Location loc = center.clone().add(v);

        if (particle == Particle.DUST || particle == Particle.ENTITY_EFFECT) {
            try {
                String[] rgb = colorExpr.split(",");
                int r = 255, g = 0, b = 0;
                if (rgb.length >= 3) {
                    r = (int) FormulaParser.eval(rgb[0], caster, target, level, ctx, plugin);
                    g = (int) FormulaParser.eval(rgb[1], caster, target, level, ctx, plugin);
                    b = (int) FormulaParser.eval(rgb[2], caster, target, level, ctx, plugin);
                }
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                Color color = Color.fromRGB(r, g, b);

                if (particle == Particle.DUST) {
                    Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
                    loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0, dust);
                } else {
                    loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0, color);
                }
            } catch (Exception e) {
                loc.getWorld().spawnParticle(particle, loc, count, 0, 0, 0, speed);
            }
        } else {
            loc.getWorld().spawnParticle(particle, loc, count, 0, 0, 0, speed);
        }
    }

    private void rotateVector(Vector v, double xDeg, double yDeg, double zDeg) {
        if (xDeg != 0) {
            double rad = Math.toRadians(xDeg);
            double y = v.getY() * Math.cos(rad) - v.getZ() * Math.sin(rad);
            double z = v.getY() * Math.sin(rad) + v.getZ() * Math.cos(rad);
            v.setY(y).setZ(z);
        }
        if (yDeg != 0) {
            double rad = Math.toRadians(yDeg);
            double x = v.getX() * Math.cos(rad) + v.getZ() * Math.sin(rad);
            double z = -v.getX() * Math.sin(rad) + v.getZ() * Math.cos(rad);
            v.setX(x).setZ(z);
        }
        if (zDeg != 0) {
            double rad = Math.toRadians(zDeg);
            double x = v.getX() * Math.cos(rad) - v.getY() * Math.sin(rad);
            double y = v.getX() * Math.sin(rad) + v.getY() * Math.cos(rad);
            v.setX(x).setY(y);
        }
    }

    private Vector parseVector(String expr, LivingEntity caster, LivingEntity target, int level, Map<String, Double> ctx) {
        try {
            if (expr == null || expr.isEmpty()) return new Vector(0,0,0);
            String[] parts = expr.split(",");
            double x = parts.length > 0 ? FormulaParser.eval(parts[0], caster, target, level, ctx, plugin) : 0;
            double y = parts.length > 1 ? FormulaParser.eval(parts[1], caster, target, level, ctx, plugin) : 0;
            double z = parts.length > 2 ? FormulaParser.eval(parts[2], caster, target, level, ctx, plugin) : 0;
            return new Vector(x, y, z);
        } catch (Exception e) {
            return new Vector(0,0,0);
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
        map.put("color", colorExpr);
        map.put("rotation", rotationExpr);
        map.put("offset", offsetExpr);
        return map;
    }
}