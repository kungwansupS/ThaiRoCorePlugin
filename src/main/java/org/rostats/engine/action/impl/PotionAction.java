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

    public PotionAction(String potionType, int durationTicks, int amplifier) {
        this.potionType = potionType;
        this.durationTicks = durationTicks;
        this.amplifier = amplifier;
    }

    @Override
    public ActionType getType() {
        return ActionType.POTION;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        if (target == null) target = caster; // ถ้าไม่มีเป้าหมาย ให้ใส่ตัวเอง

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
        return map;
    }
}