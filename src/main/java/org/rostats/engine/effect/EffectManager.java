package org.rostats.engine.effect;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EffectManager {

    private final ThaiRoCorePlugin plugin;
    // Map เก็บ Effect ของ Mob (Non-Player) เท่านั้น
    private final Map<UUID, List<ActiveEffect>> mobEffectsMap = new ConcurrentHashMap<>();

    public EffectManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    private void startTickTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    private void tickAll() {
        // 1. Tick Players (ดึงจาก PlayerData)
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            processEffects(player, data.getActiveEffects(), true);
        }

        // 2. Tick Mobs (ดึงจาก Local Map)
        Iterator<Map.Entry<UUID, List<ActiveEffect>>> it = mobEffectsMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<ActiveEffect>> entry = it.next();
            UUID uuid = entry.getKey();
            List<ActiveEffect> effects = entry.getValue();

            if (effects.isEmpty()) {
                it.remove();
                continue;
            }

            LivingEntity entity = (LivingEntity) Bukkit.getEntity(uuid);
            if (entity == null || !entity.isValid()) {
                it.remove();
                continue;
            }

            processEffects(entity, effects, false);
        }
    }

    // แก้ไข: ใช้ removeIf แทน Iterator เพื่อรองรับ CopyOnWriteArrayList
    private void processEffects(LivingEntity entity, List<ActiveEffect> effects, boolean isPlayer) {
        long currentTick = entity.getServer().getCurrentTick();

        // Tick & Trigger
        for (ActiveEffect effect : effects) {
            effect.tick();
            if (effect.isReadyToTrigger(currentTick)) {
                triggerEffect(entity, effect);
            }
        }

        // Remove Expired
        boolean removed = effects.removeIf(effect -> {
            if (effect.isExpired()) {
                removeEffectLogic(entity, effect);
                return true;
            }
            return false;
        });

        if (removed && isPlayer && entity instanceof Player) {
            plugin.getAttributeHandler().updatePlayerStats((Player) entity);
        }
    }

    public void applyEffect(LivingEntity target, ActiveEffect newEffect) {
        List<ActiveEffect> effects;
        if (target instanceof Player player) {
            effects = plugin.getStatManager().getData(player.getUniqueId()).getActiveEffects();
        } else {
            effects = mobEffectsMap.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());
        }

        boolean found = false;
        for (ActiveEffect existing : effects) {
            if (existing.getId().equals(newEffect.getId())) {
                existing.setDurationTicks(newEffect.getDurationTicks());
                if (newEffect.getLevel() > existing.getLevel()) {
                    existing.setLevel(newEffect.getLevel());
                }
                found = true;
                break;
            }
        }

        if (!found) {
            effects.add(newEffect);
            if (newEffect.getType() == EffectType.STAT_MODIFIER && target instanceof Player player) {
                plugin.getAttributeHandler().updatePlayerStats(player);
            }
        }
    }

    public void removeEffect(LivingEntity target, String effectId) {
        List<ActiveEffect> effects;
        if (target instanceof Player player) {
            effects = plugin.getStatManager().getData(player.getUniqueId()).getActiveEffects();
        } else {
            effects = mobEffectsMap.get(target.getUniqueId());
        }

        if (effects != null) {
            boolean removed = effects.removeIf(e -> {
                if (e.getId().equals(effectId)) {
                    removeEffectLogic(target, e);
                    return true;
                }
                return false;
            });

            if (removed && target instanceof Player player) {
                plugin.getAttributeHandler().updatePlayerStats(player);
            }
        }
    }

    private void triggerEffect(LivingEntity target, ActiveEffect effect) {
        switch (effect.getType()) {
            case PERIODIC_DAMAGE:
                target.damage(effect.getPower());
                target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 3);
                break;
            case PERIODIC_HEAL:
                double newHealth = Math.min(target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), target.getHealth() + effect.getPower());
                target.setHealth(newHealth);
                plugin.showHealHPFCT(target.getLocation(), effect.getPower());
                break;
        }
    }

    private void removeEffectLogic(LivingEntity target, ActiveEffect effect) {
        // Handle cleanup if needed
    }
}