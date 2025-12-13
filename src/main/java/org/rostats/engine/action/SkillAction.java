package org.rostats.engine.action;

import org.bukkit.entity.LivingEntity;
import java.util.Map;

public interface SkillAction {

    ActionType getType();

    /**
     * @param context ตัวแปรที่ส่งมาจาก Loop หรือระบบ Placeholder (เช่น "i" -> 10.0)
     */
    void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context);

    Map<String, Object> serialize();
}