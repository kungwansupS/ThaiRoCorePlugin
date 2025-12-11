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
    private final String shape; // POINT, CIRCLE, SPHERE, CYLINDER, SQUARE
    private final String radiusExpr; // Radius or Height/Length
    private final String pointsExpr; // for shapes (density/slices/points)

    // [NEW] Advanced FX Fields
    private final String colorExpr; // Format: "R,G,B" e.g., "255,0,0"
    private final String rotationExpr; // Format: "X,Y,Z" (Degrees)
    private final String offsetExpr; // Format: "X,Y,Z" (Displacement)

    // Full Constructor for Advanced FX
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

    // Legacy Constructor (Backward Compatibility)
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
            // [FIX] 1.21 Compatibility: Map old names to new Enums safely
            Particle particle = getSafeParticle(particleName);

            LivingEntity origin = (target != null) ? target : caster;

            // Base Values
            int count = (int) FormulaParser.eval(countExpr, caster, target, level, context, plugin);
            double speed = FormulaParser.eval(speedExpr, caster, target, level, context, plugin);
            double radius = FormulaParser.eval(radiusExpr, caster, target, level, context, plugin);

            // [NEW] Parse Advanced FX Vectors
            Vector rot = parseVector(rotationExpr, caster, target, level, context); // Rotation (Degrees)
            Vector off = parseVector(offsetExpr, caster, target, level, context);   // Offset (Blocks)

            // Calculate Center Point (Origin + Offset)
            // Default origin is body center (add 1.0 y)
            Location center = origin.getLocation().clone().add(0, 1.0, 0).add(off);

            if (shape.equalsIgnoreCase("CIRCLE")) {
                int points = (int) FormulaParser.eval(pointsExpr, caster, target, level, context, plugin);
                if (points <= 0) points = 20;
                double increment = (2 * Math.PI) / points;

                for (int i = 0; i < points; i++) {
                    double angle = i * increment;
                    // Create vector relative to center
                    Vector v = new Vector(radius * Math.cos(angle), 0, radius * Math.sin(angle));
                    // Spawn with rotation & color logic
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

    /**
     * Handles 1.21 Name Mapping (REDSTONE -> DUST, SPELL_MOB -> ENTITY_EFFECT)
     */
    private Particle getSafeParticle(String name) {
        if (name == null) return Particle.FLAME;
        String upper = name.toUpperCase();
        // 1.21 Mapping
        if (upper.equals("REDSTONE")) return Particle.DUST;
        if (upper.equals("SPELL_MOB") || upper.equals("SPELL_MOB_AMBIENT")) return Particle.ENTITY_EFFECT;

        try {
            return Particle.valueOf(upper);
        } catch (IllegalArgumentException e) {
            return Particle.FLAME; // Fallback
        }
    }

    /**
     * Handles 3D rotation, color application, and final spawning.
     */
    private void spawnParticle(Location center, Vector v, Vector rot, Particle particle, int count, double speed, LivingEntity caster, LivingEntity target, int level, Map<String, Double> ctx) {
        // 1. Rotate the vector
        rotateVector(v, rot.getX(), rot.getY(), rot.getZ());

        // 2. Determine final location
        Location loc = center.clone().add(v);

        // 3. Handle Color for 1.21 (DUST & ENTITY_EFFECT)
        if (particle == Particle.DUST || particle == Particle.ENTITY_EFFECT) {
            try {
                // Parse Color
                String[] rgb = colorExpr.split(",");
                int r = 255, g = 0, b = 0;

                if (rgb.length >= 3) {
                    r = (int) FormulaParser.eval(rgb[0], caster, target, level, ctx, plugin);
                    g = (int) FormulaParser.eval(rgb[1], caster, target, level, ctx, plugin);
                    b = (int) FormulaParser.eval(rgb[2], caster, target, level, ctx, plugin);
                }
                // Clamp 0-255
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));
                Color color = Color.fromRGB(r, g, b);

                if (particle == Particle.DUST) {
                    // DUST requires DustOptions in 1.21
                    Particle.DustOptions dust = new Particle.DustOptions(color, 1.0f);
                    loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0, dust);
                }
                else {
                    // ENTITY_EFFECT requires Color in 1.21 (Paper)
                    loc.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, 0, color);
                }
            } catch (Exception e) {
                // Fallback if color/data fails
                loc.getWorld().spawnParticle(particle, loc, count, 0, 0, 0, speed);
            }
        }
        else {
            // Standard spawn
            loc.getWorld().spawnParticle(particle, loc, count, 0, 0, 0, speed);
        }
    }

    /**
     * Rotates a vector around X, Y, Z axes (Euler angles in degrees).
     */
    private void rotateVector(Vector v, double xDeg, double yDeg, double zDeg) {
        // Rotate X (Pitch)
        if (xDeg != 0) {
            double rad = Math.toRadians(xDeg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double y = v.getY() * cos - v.getZ() * sin;
            double z = v.getY() * sin + v.getZ() * cos;
            v.setY(y).setZ(z);
        }
        // Rotate Y (Yaw)
        if (yDeg != 0) {
            double rad = Math.toRadians(yDeg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double x = v.getX() * cos + v.getZ() * sin;
            double z = -v.getX() * sin + v.getZ() * cos;
            v.setX(x).setZ(z);
        }
        // Rotate Z (Roll)
        if (zDeg != 0) {
            double rad = Math.toRadians(zDeg);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double x = v.getX() * cos - v.getY() * sin;
            double y = v.getX() * sin + v.getY() * cos;
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
        // Serialize new fields
        map.put("color", colorExpr);
        map.put("rotation", rotationExpr);
        map.put("offset", offsetExpr);
        return map;
    }
}