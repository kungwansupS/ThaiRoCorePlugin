package org.rostats.engine.trigger;

public enum TriggerType {
    // Active Triggers (กดใช้)
    RIGHT_CLICK,        // คลิกขวาปกติ
    LEFT_CLICK,         // คลิกซ้ายปกติ
    SHIFT_RIGHT_CLICK,  // กด Shift + คลิกขวา
    SHIFT_LEFT_CLICK,   // กด Shift + คลิกซ้าย

    // Legacy mapping (เผื่อของเก่า)
    CAST,               // เทียบเท่า RIGHT_CLICK

    // Reactive Triggers (ทำงานเมื่อเกิดเหตุการณ์)
    ON_HIT,             // เมื่อโจมตีโดน (Physical)
    ON_MAGIC_HIT,       // เมื่อโจมตีโดน (Magic) - เผื่ออนาคต
    ON_DEFEND,          // เมื่อถูกโจมตี
    ON_KILL,            // เมื่อฆ่าศัตรูตาย

    // Passive Triggers (ทำงานตลอด)
    PASSIVE_TICK,       // ทำงานตลอดเวลา (ทุกวินาที)
    PASSIVE_APPLY       // ทำงานทันทีที่ถือ/ใส่ (Stat Boost)
}