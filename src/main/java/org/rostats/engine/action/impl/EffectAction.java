package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.effect.ActiveEffect;
import org.rostats.engine.effect.EffectType;

public class EffectAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String effectId;
    private final EffectType type;
    private final int level;
    private final double power;
    private final long duration;
    private final double chance;
    private final String statKey;

    public EffectAction(ThaiRoCorePlugin plugin, String effectId, EffectType type, int level, double power, long duration, double chance, String statKey) {
        this.plugin = plugin;
        this.effectId = effectId;
        this.type = type;
        this.level = level;
        this.power = power;
        this.duration = duration;
        this.chance = chance;
        this.statKey = statKey;
    }

    @Override
    public ActionType getType() {
        return ActionType.APPLY_EFFECT;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int skillLevel) {
        if (target == null) return;

        // Check Chance
        if (Math.random() > chance) return;

        // Create Effect Instance
        // Note: duration is in ticks (from config)
        ActiveEffect effect = new ActiveEffect(
                effectId,
                type,
                level, // Can use skillLevel logic later if needed
                power,
                duration,
                20L, // Default interval 1s for DoT
                caster.getUniqueId()
        );

        if (statKey != null) {
            effect.setStatKey(statKey);
        }

        plugin.getEffectManager().applyEffect(target, effect);

        // Visual Feedback
        if (type == EffectType.STAT_MODIFIER) {
            plugin.showCombatFloatingText(target.getLocation(), "§aBuff Applied!");
        } else {
            plugin.showStatusDamageFCT(target.getLocation(), type.name(), 0);
        }
    }
}