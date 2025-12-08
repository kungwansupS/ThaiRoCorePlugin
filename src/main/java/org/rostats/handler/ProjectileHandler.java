package org.rostats.handler;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.rostats.ThaiRoCorePlugin;

public class ProjectileHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    // Keys for storing data in projectile
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

        // 1. Check if it's our custom projectile
        if (!projectile.getPersistentDataContainer().has(skillIdKey, PersistentDataType.STRING)) {
            return;
        }

        // 2. Retrieve Data
        String onHitSkillId = projectile.getPersistentDataContainer().get(skillIdKey, PersistentDataType.STRING);
        Integer level = projectile.getPersistentDataContainer().get(skillLevelKey, PersistentDataType.INTEGER);

        if (onHitSkillId == null || level == null) return;

        // 3. Get Caster
        if (!(projectile.getShooter() instanceof LivingEntity caster)) return;

        // 4. Get Target (Entity Hit)
        Entity hitEntity = event.getHitEntity();
        LivingEntity target = null;

        if (hitEntity instanceof LivingEntity) {
            target = (LivingEntity) hitEntity;
        } else {
            // Hit block? Optional: handling block hit effects could go here
            // For now, we only cast if we hit an entity, or we could cast self/location based skills
            // Let's assume we want to cast the skill at the location if no entity hit
            // BUT castSkill requires a LivingEntity target currently.
            // So we only proceed if we hit a LivingEntity.
        }

        if (target != null && target != caster) {
            // Cast the linked skill on the target
            plugin.getSkillManager().castSkill(caster, onHitSkillId, level, target);
        }

        // Remove projectile to prevent vanilla damage/knockback loop if desired,
        // OR let it happen. Usually custom skills handle damage via the triggered skill.
        // We remove it to be safe and purely logic-based.
        projectile.remove();
    }

    public NamespacedKey getSkillIdKey() { return skillIdKey; }
    public NamespacedKey getSkillLevelKey() { return skillLevelKey; }
}