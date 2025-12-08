package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.HashMap;
import java.util.Map;

public class DelayAction implements SkillAction {

    private final long delay;

    public DelayAction(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }

    @Override
    public ActionType getType() {
        return ActionType.DELAY;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        // ไม่ทำอะไรในนี้ เพราะ Logic การรอจะอยู่ที่ SkillRunner
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "DELAY");
        map.put("ticks", delay);
        return map;
    }
}