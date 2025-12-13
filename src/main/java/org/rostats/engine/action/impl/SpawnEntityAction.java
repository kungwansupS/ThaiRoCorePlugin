package org.rostats.engine.action.impl;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class SpawnEntityAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String entityType;
    private final String skillId; // On-spawn skill (Optional)

    public SpawnEntityAction(ThaiRoCorePlugin plugin, String entityType, String skillId) {
        this.plugin = plugin;
        this.entityType = entityType;
        this.skillId = skillId;
    }

    @Override
    public ActionType getType() {
        return ActionType.SPAWN_ENTITY;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        LivingEntity origin = (target != null) ? target : caster;
        Location loc = origin.getLocation();
        World world = loc.getWorld();

        try {
            // ใช้ Context สำหรับการชดเชยตำแหน่ง (Offset)
            double xOffset = context.getOrDefault("x_offset", 0.0);
            double yOffset = context.getOrDefault("y_offset", 0.0);
            double zOffset = context.getOrDefault("z_offset", 0.0);

            Location spawnLoc = loc.clone().add(xOffset, yOffset, zOffset);

            EntityType type = EntityType.valueOf(entityType.toUpperCase());

            if (type == EntityType.LIGHTNING_BOLT) {
                world.strikeLightningEffect(spawnLoc);
            } else if (type.isAlive()) {
                Entity entity = world.spawnEntity(spawnLoc, type);
                if (entity instanceof LivingEntity && !skillId.equalsIgnoreCase("none")) {
                    // ร่ายสกิลย่อยเมื่อสร้าง Mob ขึ้นมา (เช่น บัฟ Mob ทันที)
                    // [MODIFIED] ส่ง context เข้าไปด้วย
                    plugin.getSkillManager().castSkill(caster, skillId, level, (LivingEntity) entity, true, context);
                }
            }
        } catch (Exception e) {
            // Ignore invalid entity type
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "SPAWN_ENTITY");
        map.put("entity-type", entityType);
        map.put("skill-id", skillId);
        return map;
    }
}