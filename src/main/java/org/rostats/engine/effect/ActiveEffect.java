package org.rostats.engine.effect;

import java.util.UUID;

public class ActiveEffect {

    private final String id;           // ID อ้างอิงจาก Config
    private final EffectType type;     // ประเภทพฤติกรรม
    private final UUID source;         // ใครเป็นคนทำ (ผู้ร่าย)

    private int level;                 // เลเวลของ Effect
    private double power;              // ค่าพลัง
    private long durationTicks;        // เวลาที่เหลือ (หน่วย Tick)
    private final long maxDurationTicks;
    private long intervalTicks;        // สำหรับพวก Periodic
    private String statKey;

    public ActiveEffect(String id, EffectType type, int level, double power, long durationTicks, UUID source) {
        this.id = id;
        this.type = type;
        this.level = level;
        this.power = power;
        this.durationTicks = durationTicks;
        this.maxDurationTicks = durationTicks;
        this.source = source;
    }

    public ActiveEffect(String id, EffectType type, int level, double power, long durationTicks, long intervalTicks, UUID source) {
        this(id, type, level, power, durationTicks, source);
        this.intervalTicks = intervalTicks;
    }

    // ลดเวลาลง 1 Tick (Default)
    public void tick() {
        this.durationTicks--;
    }

    // [FIX] เพิ่มเมธอดลดเวลาตามจำนวนที่ระบุ (เพื่อรองรับการทำงานแบบ 5 Ticks/Run)
    public void tick(long amount) {
        this.durationTicks -= amount;
    }

    public boolean isExpired() {
        return durationTicks <= 0;
    }

    public boolean isReadyToTrigger(long currentTick) {
        if (type == EffectType.PERIODIC_DAMAGE || type == EffectType.PERIODIC_HEAL) {
            // เช็คว่าถึงรอบทำงานหรือยัง โดยดูจากเวลาที่ผ่านไป
            long timePassed = maxDurationTicks - durationTicks;
            return timePassed > 0 && timePassed % intervalTicks == 0;
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
    public void setDurationTicks(long durationTicks) { this.durationTicks = durationTicks; }
    public UUID getSource() { return source; }
    public String getStatKey() { return statKey; }
    public void setStatKey(String statKey) { this.statKey = statKey; }
}