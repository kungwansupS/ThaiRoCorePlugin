package org.rostats.engine.trigger;

public enum TriggerType {
    CAST,           // กดใช้ (Active Skill)
    ON_HIT,         // เมื่อโจมตีโดน (Physical)
    ON_DEFEND,      // เมื่อถูกโจมตี
    ON_KILL,        // เมื่อฆ่าศัตรูตาย
    PASSIVE_TICK,   // ทำงานตลอดเวลา (ทุกวินาที)
    PASSIVE_APPLY   // ทำงานทันทีที่ถือ/ใส่ (Stat Boost)
}