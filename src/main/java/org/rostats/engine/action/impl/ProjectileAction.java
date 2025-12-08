package org.rostats.engine.action.impl;

import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectileAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final EntityType projectileType;
    private final double speed;
    private final List<SkillAction> onHitActions;

    public ProjectileAction(ThaiRoCorePlugin plugin, EntityType projectileType, double speed, List<SkillAction> onHitActions) {
        this.plugin = plugin;
        this.projectileType = projectileType;
        this.speed = speed;
        this.onHitActions = onHitActions;
    }

    @Override
    public ActionType getType() {
        return ActionType.PROJECTILE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        if (caster instanceof ProjectileSource) {
            ProjectileSource source = (ProjectileSource) caster;
            // Launch Projectile
            Class<? extends Entity> entityClass = projectileType.getEntityClass();
            if (entityClass != null && Projectile.class.isAssignableFrom(entityClass)) {
                Projectile proj = source.launchProjectile((Class<? extends Projectile>) entityClass);
                proj.setVelocity(caster.getEyeLocation().getDirection().multiply(speed));

                // Embed Metadata
                proj.setMetadata("RO_ON_HIT", new FixedMetadataValue(plugin, onHitActions));
                proj.setMetadata("RO_SKILL_LEVEL", new FixedMetadataValue(plugin, level));
                proj.setMetadata("RO_CASTER", new FixedMetadataValue(plugin, caster.getUniqueId().toString()));
            }
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "PROJECTILE");
        map.put("projectile-type", projectileType.name());
        map.put("speed", speed);

        List<Map<String, Object>> actionsList = new ArrayList<>();
        for (SkillAction action : onHitActions) {
            actionsList.add(action.serialize());
        }
        map.put("on-hit", actionsList);

        return map;
    }
}