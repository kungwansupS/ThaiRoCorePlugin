package org.rostats.handler;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.rostats.ThaiRoCorePlugin;

public class ProjectileHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final NamespacedKey skillIdKey;
    private final NamespacedKey skillLevelKey;

    public ProjectileHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.skillIdKey = new NamespacedKey(plugin, "RO_SKILL_ID");
        this.skillLevelKey = new NamespacedKey(plugin, "RO_SKILL_LEVEL");
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        if (!projectile.getPersistentDataContainer().has(skillIdKey, PersistentDataType.STRING)) {
            return;
        }

        String onHitSkillId = projectile.getPersistentDataContainer().get(skillIdKey, PersistentDataType.STRING);
        Integer level = projectile.getPersistentDataContainer().get(skillLevelKey, PersistentDataType.INTEGER);

        if (onHitSkillId == null || level == null) return;

        if (!(projectile.getShooter() instanceof LivingEntity caster)) return;

        Entity hitEntity = event.getHitEntity();
        LivingEntity target = null;

        if (hitEntity instanceof LivingEntity) {
            target = (LivingEntity) hitEntity;
        }

        if (target != null && target != caster) {
            // [MODIFIED] Use master overload with isPassive=true and null context.
            plugin.getSkillManager().castSkill(caster, onHitSkillId, level, target, true, null);
        }
        // No action if hit a block, relying on the skill logic to handle self-destruction.

        // [FIXED] Removed projectile.remove() to prevent instant despawn logic risk
        // projectile.remove();
    }

    public NamespacedKey getSkillIdKey() { return skillIdKey; }
    public NamespacedKey getSkillLevelKey() { return skillLevelKey; }
}