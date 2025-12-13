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

    // [Phase 2] Logic
    CONDITION,
    SET_VARIABLE,

    // [Phase 3] Targeting
    SELECT_TARGET   // เปลี่ยนเป้าหมายปัจจุบัน (Current Target)
}