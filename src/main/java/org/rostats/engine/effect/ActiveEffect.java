package org.rostats.engine.effect;

import org.bukkit.entity.LivingEntity;
import java.util.UUID;

public class ActiveEffect {

    private final String id;           // ID อ้างอิงจาก Config (เช่น "burn_dot_lv1")
    private final EffectType type;     // ประเภทพฤติกรรม
    private final UUID source;         // ใครเป็นคนทำ (ผู้ร่าย)

    private int level;                 // เลเวลของ Effect (ใช้คำนวณความแรง)
    private double power;              // ค่าพลัง (เช่น Damage 50, Stat 10)

    private long durationTicks;        // เวลาที่เหลือ (หน่วย Tick)
    private final long maxDurationTicks; // เวลาตั้งต้น (สำหรับคำนวณหลอด % ถ้ามี)
    private long intervalTicks;        // สำหรับพวก Periodic: ทำงานทุกๆ กี่ Tick
    private long lastTickTime;         // เวลาล่าสุดที่ Effect นี้ทำงาน (สำหรับ Periodic)

    // ข้อมูลเสริมสำหรับ Stat Modifier (เช่น "STR", "P_ATK")
    private String statKey;

    public ActiveEffect(String id, EffectType type, int level, double power, long durationTicks, UUID source) {
        this.id = id;
        this.type = type;
        this.level = level;
        this.power = power;
        this.durationTicks = durationTicks;
        this.maxDurationTicks = durationTicks;
        this.source = source;
        this.lastTickTime = System.currentTimeMillis();
    }

    // Constructor เสริมสำหรับ Periodic Effect (ระบุ Interval)
    public ActiveEffect(String id, EffectType type, int level, double power, long durationTicks, long intervalTicks, UUID source) {
        this(id, type, level, power, durationTicks, source);
        this.intervalTicks = intervalTicks;
    }

    // ลดเวลาลง 1 Tick (เรียกโดย EffectManager)
    public void tick() {
        this.durationTicks--;
    }

    public boolean isExpired() {
        return durationTicks <= 0;
    }

    public boolean isReadyToTrigger(long currentTick) {
        // สำหรับพวก DoT/HoT เช็คว่าถึงรอบทำงานหรือยัง
        if (type == EffectType.PERIODIC_DAMAGE || type == EffectType.PERIODIC_HEAL) {
            return (maxDurationTicks - durationTicks) % intervalTicks == 0;
        }
        return false;
    }

    // --- Getters & Setters ---
    public String getId() { return id; }
    public EffectType getType() { return type; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public double getPower() { return power; }
    public long getDurationTicks() { return durationTicks; }
    public void setDurationTicks(long durationTicks) { this.durationTicks = durationTicks; } // สำหรับการ Refresh เวลา
    public UUID getSource() { return source; }
    public String getStatKey() { return statKey; }
    public void setStatKey(String statKey) { this.statKey = statKey; }
}