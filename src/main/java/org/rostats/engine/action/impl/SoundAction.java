package org.rostats.engine.action.impl;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class SoundAction implements SkillAction {

    private final String soundName;
    private final float volume;
    private final float pitch;

    public SoundAction(String soundName, float volume, float pitch) {
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public ActionType getType() {
        return ActionType.SOUND;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            LivingEntity locEntity = (target != null) ? target : caster;
            locEntity.getWorld().playSound(locEntity.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Ignore invalid sound
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "SOUND");
        map.put("sound", soundName);
        map.put("volume", (double) volume);
        map.put("pitch", (double) pitch);
        return map;
    }
}