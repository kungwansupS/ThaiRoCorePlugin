package org.rostats.engine.action;

import org.bukkit.entity.LivingEntity;

public interface SkillAction {

    // คืนค่าประเภทของ Action
    ActionType getType();

    // สั่งให้ทำงาน
    // caster: คนร่าย
    // target: เป้าหมาย (ถ้ามี)
    // level: เลเวลสกิล (เพื่อคำนวณความแรง)
    void execute(LivingEntity caster, LivingEntity target, int level);
}