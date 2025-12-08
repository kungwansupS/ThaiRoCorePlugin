package org.rostats.engine.action.impl;

import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class SoundAction implements SkillAction {

    private final Sound sound;
    private final float volume;
    private final float pitch;

    public SoundAction(Sound sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public ActionType getType() {
        return ActionType.SOUND;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        LivingEntity locEntity = (target != null) ? target : caster;
        locEntity.getWorld().playSound(locEntity.getLocation(), sound, volume, pitch);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "SOUND");
        map.put("sound", sound.name());
        map.put("volume", volume);
        map.put("pitch", pitch);
        return map;
    }
}