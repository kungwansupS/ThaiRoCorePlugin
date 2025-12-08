package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.effect.ActiveEffect;
import org.rostats.engine.effect.EffectType;

import java.util.HashMap;
import java.util.Map;

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
        // FIX: Default to self if target is null (for Buffs/Self-Cast)
        if (target == null) target = caster;

        if (Math.random() > chance) return;

        ActiveEffect effect = new ActiveEffect(
                effectId, type, level, power, duration, 20L, caster.getUniqueId()
        );
        if (statKey != null) effect.setStatKey(statKey);

        plugin.getEffectManager().applyEffect(target, effect);

        if (type == EffectType.STAT_MODIFIER) {
            plugin.showCombatFloatingText(target.getLocation(), "§aBuff!");
        } else {
            plugin.showStatusDamageFCT(target.getLocation(), type.name(), 0);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "APPLY_EFFECT");
        map.put("effect-id", effectId);
        map.put("effect-type", type.name());
        map.put("level", level);
        map.put("power", power);
        map.put("duration", duration);
        map.put("chance", chance);
        if (statKey != null) map.put("stat-key", statKey);
        return map;
    }
}