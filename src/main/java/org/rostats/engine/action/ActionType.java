package org.rostats.engine.action;

public enum ActionType {
    DAMAGE,
    HEAL,
    APPLY_EFFECT,
    SOUND,
    PARTICLE,
    PROJECTILE,
    POTION,
    TELEPORT,
    AREA_EFFECT,
    DELAY,      // สำหรับหน่วงเวลา (จากรอบที่แล้ว)
    COMMAND     // [NEW] สำหรับรันคำสั่ง
}