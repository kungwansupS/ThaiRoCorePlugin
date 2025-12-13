package org.rostats.itemeditor;

import org.rostats.engine.trigger.TriggerType;

public class ItemSkillBinding {

    private final String skillId;
    private final TriggerType trigger;
    private final int level;
    private final double chance; // 0.0 - 1.0 (e.g. 0.1 = 10%)

    public ItemSkillBinding(String skillId, TriggerType trigger, int level, double chance) {
        this.skillId = skillId;
        this.trigger = trigger;
        this.level = level;
        this.chance = chance;
    }

    public String getSkillId() { return skillId; }
    public TriggerType getTrigger() { return trigger; }
    public int getLevel() { return level; }
    public double getChance() { return chance; }
}