package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class PotionAction implements SkillAction {

    private final String potionType;
    private final int durationTicks;
    private final int amplifier;
    private final boolean isSelfOnly; // [FIX] New Field

    public PotionAction(String potionType, int durationTicks, int amplifier, boolean isSelfOnly) {
        this.potionType = potionType;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
        this.isSelfOnly = isSelfOnly;
    }

    @Override
    public ActionType getType() {
        return ActionType.POTION;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        // [FIX] Logic เลือกเป้าหมาย
        if (isSelfOnly || target == null) {
            target = caster;
        }

        PotionEffectType type = PotionEffectType.getByName(potionType);
        if (type != null) {
            target.addPotionEffect(new PotionEffect(type, durationTicks, amplifier));
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "POTION");
        map.put("potion", potionType);
        map.put("duration", durationTicks);
        map.put("amplifier", amplifier);
        map.put("self-only", isSelfOnly);
        return map;
    }
}