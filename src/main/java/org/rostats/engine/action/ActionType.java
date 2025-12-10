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
    DELAY,
    COMMAND,
    VELOCITY,
    LOOP,
    RAYCAST,
    SPAWN_ENTITY,

    // [NEW] Logic & Targeting (Phase 2-3 Prep)
    CONDITION,      // เงื่อนไข If-Else
    SET_VARIABLE,   // ตั้งค่าตัวแปร
    SELECT_TARGET   // เปลี่ยนเป้าหมาย
}