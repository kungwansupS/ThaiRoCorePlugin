package org.rostats.engine.action.impl;

import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.handler.ProjectileHandler;

import java.util.HashMap;
import java.util.Map;

public class ProjectileAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String projectileType; // ARROW, SNOWBALL, FIREBALL, etc.
    private final double speed;
    private final String onHitSkillId;   // Skill ID to cast on hit

    public ProjectileAction(ThaiRoCorePlugin plugin, String projectileType, double speed, String onHitSkillId) {
        this.plugin = plugin;
        this.projectileType = projectileType;
        this.speed = speed;
        this.onHitSkillId = onHitSkillId;
    }

    @Override
    public ActionType getType() {
        return ActionType.PROJECTILE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        Class<? extends Projectile> projClass = getProjectileClass(projectileType);
        if (projClass == null) return;

        Location spawnLoc = caster.getEyeLocation().add(caster.getLocation().getDirection());
        Projectile proj = caster.launchProjectile(projClass);

        // Set Velocity
        Vector velocity = caster.getLocation().getDirection().multiply(speed);
        proj.setVelocity(velocity);

        // Inject Data for Handler
        ProjectileHandler handler = plugin.getProjectileHandler(); // Need to add getter in Plugin class
        if (handler != null) {
            proj.getPersistentDataContainer().set(handler.getSkillIdKey(), PersistentDataType.STRING, onHitSkillId);
            proj.getPersistentDataContainer().set(handler.getSkillLevelKey(), PersistentDataType.INTEGER, level);
        }
    }

    private Class<? extends Projectile> getProjectileClass(String name) {
        try {
            switch (name.toUpperCase()) {
                case "ARROW": return Arrow.class;
                case "SNOWBALL": return Snowball.class;
                case "EGG": return Egg.class;
                case "FIREBALL": return LargeFireball.class;
                case "SMALL_FIREBALL": return SmallFireball.class;
                case "WITHER_SKULL": return WitherSkull.class;
                case "DRAGON_FIREBALL": return DragonFireball.class;
                case "TRIDENT": return Trident.class;
                case "SHULKER_BULLET": return ShulkerBullet.class;
                case "LLAMA_SPIT": return LlamaSpit.class;
                case "SPECTRAL_ARROW": return SpectralArrow.class;
                default: return Arrow.class;
            }
        } catch (Exception e) {
            return Arrow.class;
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "PROJECTILE");
        map.put("projectile", projectileType);
        map.put("speed", speed);
        map.put("on-hit", onHitSkillId);
        return map;
    }
}