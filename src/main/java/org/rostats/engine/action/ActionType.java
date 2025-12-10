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

    // [Phase 2] Logic Actions
    CONDITION,      // ตรวจสอบเงื่อนไข If-Else
    SET_VARIABLE    // กำหนดค่าตัวแปร
}