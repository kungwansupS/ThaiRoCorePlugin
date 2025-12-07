package org.rostats.engine.action;

public enum ActionType {
    DAMAGE,         // สร้างความเสียหาย
    HEAL,           // ฮีล
    APPLY_EFFECT,   // ยัด Effect (จาก Phase 1)
    SOUND,          // เล่นเสียง
    PARTICLE,       // เล่นเอฟเฟกต์
    PROJECTILE,     // ยิงลูกพลัง (ซับซ้อนสุด เดี๋ยวมาทำใน Phase 5)
    POTION,         // ยัด Vanilla Potion
    TELEPORT        // วาร์ป/พุ่ง
}