package org.rostats.engine.effect;

public enum EffectType {
    /**
     * เพิ่ม/ลด ค่าสถานะ (เช่น STR +10, P.ATK +5%)
     * ทำงาน: เมื่อได้รับ Effect จะไปบวกค่าใน StatManager, เมื่อหมดเวลาจะลบออก
     */
    STAT_MODIFIER,

    /**
     * ใช้ PotionEffect ของ Minecraft (เช่น SPEED, BLINDNESS)
     * ทำงาน: ยัด Potion ใส่ Entity จริงๆ
     */
    VANILLA_POTION,

    /**
     * ทำดาเมจต่อเนื่อง (Damage over Time) เช่น พิษ, ไฟไหม้, เลือดไหล
     * ทำงาน: ทุกๆ X วินาที จะลดเลือดเป้าหมาย
     */
    PERIODIC_DAMAGE,

    /**
     * ฟื้นฟูต่อเนื่อง (Heal over Time) เช่น Regen
     * ทำงาน: ทุกๆ X วินาที จะเพิ่มเลือด/SP
     */
    PERIODIC_HEAL,

    /**
     * สถานะผิดปกติ (Crowd Control) เช่น Stun, Root, Silence
     * ทำงาน: ขัดขวางการกระทำ (เดินไม่ได้, ใช้สกิลไม่ได้)
     */
    CROWD_CONTROL,

    /**
     * เอฟเฟกต์พิเศษอื่นๆ (Custom Logic)
     */
    CUSTOM
}