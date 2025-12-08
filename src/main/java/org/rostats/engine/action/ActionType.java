package org.rostats.engine.action;

public enum ActionType {
    DAMAGE,         // สร้างความเสียหาย
    HEAL,           // ฮีล
    APPLY_EFFECT,   // ยัด Effect
    SOUND,          // เล่นเสียง
    PARTICLE,       // เล่นเอฟเฟกต์
    PROJECTILE,     // ยิงลูกพลัง
    POTION,         // ยัด Vanilla Potion
    TELEPORT,       // วาร์ป/พุ่ง
    AREA_EFFECT     // NEW: สกิลหมู่ (AOE)
}