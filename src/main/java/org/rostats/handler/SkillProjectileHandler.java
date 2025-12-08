package org.rostats.handler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.SkillAction;

import java.util.List;
import java.util.UUID;

public class SkillProjectileHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    public SkillProjectileHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();

        if (!proj.hasMetadata("RO_ON_HIT")) return;

        List<SkillAction> actions = (List<SkillAction>) proj.getMetadata("RO_ON_HIT").get(0).value();
        int level = proj.getMetadata("RO_SKILL_LEVEL").get(0).asInt();
        String casterUUIDStr = proj.getMetadata("RO_CASTER").get(0).asString();

        Entity casterEntity = Bukkit.getEntity(UUID.fromString(casterUUIDStr));
        LivingEntity caster = (casterEntity instanceof LivingEntity) ? (LivingEntity) casterEntity : null;

        if (caster == null) return;

        LivingEntity target = null;
        if (event.getHitEntity() instanceof LivingEntity) {
            target = (LivingEntity) event.getHitEntity();
        }

        if (actions != null) {
            for (SkillAction action : actions) {
                try {
                    action.execute(caster, target, level);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}