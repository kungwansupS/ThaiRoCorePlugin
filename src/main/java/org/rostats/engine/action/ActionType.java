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
    VELOCITY, // สำหรับการพุ่ง/กระแทก
    LOOP,      // สำหรับการวนลูป
    RAYCAST,    // [NEW] สำหรับการโจมตีแบบ Hitscan / Line-of-sight
    SPAWN_ENTITY // [NEW] สำหรับการสร้าง Entity (เช่น Lightning Bolt, Mob)
}