package org.rostats.engine.action;

import org.bukkit.entity.LivingEntity;
import java.util.Map;

public interface SkillAction {

    ActionType getType();

    void execute(LivingEntity caster, LivingEntity target, int level);

    // NEW: แปลงข้อมูลกลับเป็น Map เพื่อบันทึกลง YAML
    Map<String, Object> serialize();
}