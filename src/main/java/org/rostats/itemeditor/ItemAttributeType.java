package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.rostats.ThaiRoCorePlugin;

import java.util.Arrays;
import java.util.List;

public enum ItemAttributeType {
    // --- 1. Base Attributes ---
    STR_GEAR("STRBonusGear", "§cSTR", Material.IRON_BLOCK, "%.0f", 1.0, 10.0,
            "เพิ่มพลังโจมตีกายภาพ (P.ATK)", "และน้ำหนักที่ถือได้"),
    AGI_GEAR("AGIBonusGear", "§bAGI", Material.FEATHER, "%.0f", 1.0, 10.0,
            "เพิ่มความเร็วโจมตี (ASPD)", "และการหลบหลีก (FLEE)"),
    VIT_GEAR("VITBonusGear", "§aVIT", Material.IRON_CHESTPLATE, "%.0f", 1.0, 10.0,
            "เพิ่มเลือดสูงสุด (Max HP)", "พลังป้องกัน (DEF) และการฟื้นฟู"),
    INT_GEAR("INTBonusGear", "§dINT", Material.ENCHANTED_BOOK, "%.0f", 1.0, 10.0,
            "เพิ่มพลังโจมตีเวท (M.ATK)", "และมานาสูงสุด (Max SP)"),
    DEX_GEAR("DEXBonusGear", "§6DEX", Material.BOW, "%.0f", 1.0, 10.0,
            "เพิ่มความแม่นยำ (HIT)", "และลดเวลาร่ายเวท"),
    LUK_GEAR("LUKBonusGear", "§eLUK", Material.RABBIT_FOOT, "%.0f", 1.0, 10.0,
            "เพิ่มโอกาสคริติคอล (CRIT)", "และโชคดี"),

    MAXHP_PERCENT("MaxHPPercent", "§aMax HP %", Material.RED_WOOL, "%.1f%%", 1.0, 10.0,
            "เพิ่มเลือดสูงสุดเป็นเปอร์เซ็นต์"),
    MAXSP_PERCENT("MaxSPPercent", "§bMax SP %", Material.BLUE_WOOL, "%.1f%%", 1.0, 10.0,
            "เพิ่มมานาสูงสุดเป็นเปอร์เซ็นต์"),

    HIT_BONUS_FLAT("HitBonusFlat", "§6HIT", Material.TARGET, "%.0f", 1.0, 10.0,
            "เพิ่มค่าความแม่นยำ"),
    FLEE_BONUS_FLAT("FleeBonusFlat", "§bFLEE", Material.FEATHER, "%.0f", 1.0, 10.0,
            "เพิ่มค่าการหลบหลีก"),
    BASE_MSPD("BaseMSPD", "§aBase Move Speed", Material.SUGAR, "%.2f", 0.01, 0.1,
            "เพิ่มความเร็วการเดินพื้นฐาน", "(หน่วยทศนิยม เช่น 0.05)"),

    // --- 2. Core Combat Stats ---
    WEAPON_PATK("WeaponPAtk", "§cP.ATK (Equip)", Material.IRON_SWORD, "%.0f", 1.0, 10.0,
            "พลังโจมตีกายภาพจากอาวุธ"),
    WEAPON_MATK("WeaponMAtk", "§dM.ATK (Equip)", Material.ENCHANTED_BOOK, "%.0f", 1.0, 10.0,
            "พลังโจมตีเวทมนตร์จากอาวุธ"),
    PATK_FLAT("PAtkBonusFlat", "§cBonus P.ATK", Material.REDSTONE, "%.0f", 1.0, 10.0,
            "โบนัสพลังโจมตีกายภาพ (หน่วย)"),
    MATK_FLAT("MAtkBonusFlat", "§dBonus M.ATK", Material.LAPIS_LAZULI, "%.0f", 1.0, 10.0,
            "โบนัสพลังโจมตีเวทมนตร์ (หน่วย)"),

    // --- 3. Penetration / Ignore Def ---
    P_PEN_FLAT("PPenFlat", "§6P.Penetration", Material.IRON_NUGGET, "%.0f", 1.0, 10.0,
            "ค่าเจาะเกราะกายภาพ (หน่วย)"),
    P_PEN_PERCENT("PPenPercent", "§6P.Penetration %", Material.IRON_INGOT, "%.1f%%", 1.0, 10.0,
            "ค่าเจาะเกราะกายภาพ (%)"),
    IGNORE_PDEF_FLAT("IgnorePDefFlat", "§6Ignore P.DEF", Material.ANVIL, "%.0f", 1.0, 10.0,
            "เมินพลังป้องกันกายภาพ (หน่วย)"),
    IGNORE_PDEF_PERCENT("IgnorePDefPercent", "§6Ignore P.DEF %", Material.IRON_BLOCK, "%.1f%%", 1.0, 10.0,
            "เมินพลังป้องกันกายภาพ (%)"),

    M_PEN_FLAT("MPenFlat", "§6M.Penetration", Material.LAPIS_LAZULI, "%.0f", 1.0, 10.0,
            "ค่าเจาะเกราะเวทมนตร์ (หน่วย)"),
    M_PEN_PERCENT("MPenPercent", "§6M.Penetration %", Material.DIAMOND, "%.1f%%", 1.0, 10.0,
            "ค่าเจาะเกราะเวทมนตร์ (%)"),
    IGNORE_MDEF_FLAT("IgnoreMDefFlat", "§6Ignore M.DEF", Material.BREWING_STAND, "%.0f", 1.0, 10.0,
            "เมินพลังป้องกันเวทมนตร์ (หน่วย)"),
    IGNORE_MDEF_PERCENT("IgnoreMDefPercent", "§6Ignore M.DEF %", Material.LAPIS_BLOCK, "%.1f%%", 1.0, 10.0,
            "เมินพลังป้องกันเวทมนตร์ (%)"),

    // --- 4. Casting & Cooldown ---
    VAR_CT_PERCENT("VarCTPercent", "§dVariable Cast %", Material.CLOCK, "%.1f%%", 1.0, 10.0,
            "ลดระยะเวลาร่ายเวท (แปรผัน) %"),
    VAR_CT_FLAT("VarCTFlat", "§dVariable Cast (s)", Material.CLOCK, "%.1f", 0.1, 10.0,
            "ลดระยะเวลาร่ายเวท (แปรผัน) วินาที"),
    FIXED_CT_PERCENT("FixedCTPercent", "§dFixed Cast %", Material.COMPASS, "%.1f%%", 1.0, 10.0,
            "ลดระยะเวลาร่ายเวท (คงที่) %"),
    FIXED_CT_FLAT("FixedCTFlat", "§dFixed Cast (s)", Material.COMPASS, "%.1f", 0.1, 10.0,
            "ลดระยะเวลาร่ายเวท (คงที่) วินาที"),

    SKILL_CD_PERCENT("SkillCDPercent", "§bSkill CD %", Material.MUSIC_DISC_CAT, "%.1f%%", 1.0, 10.0,
            "ลดคูลดาวน์สกิล %"),
    SKILL_CD_FLAT("SkillCDFlat", "§bSkill CD (s)", Material.MUSIC_DISC_13, "%.1f", 0.1, 1.0,
            "ลดคูลดาวน์สกิล (วินาที)"),

    // [FIXED] Added missing Global Cooldown Enums
    GLOBAL_CD_PERCENT("GlobalCDPercent", "§eGlobal CD %", Material.RECOVERY_COMPASS, "%.1f%%", 1.0, 10.0,
            "ลด Global Cooldown (GCD) %"),
    GLOBAL_CD_FLAT("GlobalCDFlat", "§eGlobal CD (s)", Material.GOAT_HORN, "%.1f", 0.1, 1.0,
            "ลด Global Cooldown (GCD) วินาที"),

    // [NEW] After-Cast Delay (ACD)
    ACD_PERCENT("AfterCastDelayPercent", "§eACD (Delay) %", Material.RECOVERY_COMPASS, "%.1f%%", 1.0, 10.0,
            "ลดดีเลย์หลังร่าย (ACD) %", "ทำให้กดสกิลอื่นต่อได้ไวขึ้น"),
    ACD_FLAT("AfterCastDelayFlat", "§eACD (Delay) (s)", Material.GOAT_HORN, "%.1f", 0.1, 1.0,
            "ลดดีเลย์หลังร่าย (ACD) วินาที"),

    // --- 5. Speed & Mobility ---
    ASPD_PERCENT("ASpdPercent", "§aASPD %", Material.FEATHER, "%.1f%%", 1.0, 10.0,
            "เพิ่มความเร็วในการโจมตี %"),
    MSPD_PERCENT("MSpdPercent", "§aMove Speed %", Material.LEATHER_BOOTS, "%.1f%%", 1.0, 10.0,
            "เพิ่มความเร็วในการเคลื่อนที่ %"),

    // --- 6. Critical System ---
    CRIT_RATE("CritBonusFlat", "§eCRIT Rate", Material.GOLD_NUGGET, "%.0f", 1.0, 10.0,
            "เพิ่มโอกาสติดคริติคอล"),
    CRIT_DMG_PERCENT("CritDmgPercent", "§eCrit DMG %", Material.YELLOW_DYE, "%.1f%%", 5.0, 10.0,
            "เพิ่มความแรงคริติคอล %"),
    CRIT_RES("CritRes", "§eCrit Res", Material.SHIELD, "%.1f", 1.0, 10.0,
            "ค่าป้องกันการถูกคริติคอล"),
    CRIT_DMG_RES_PERCENT("CritDmgResPercent", "§eCrit DMG Res %", Material.SHULKER_SHELL, "%.1f%%", 1.0, 10.0,
            "ลดความแรงคริติคอลที่ได้รับ %"),

    // --- 7. Universal Damage Modifiers ---
    PDMG_PERCENT("PDmgBonusPercent", "§cP.DMG Bonus %", Material.DIAMOND_SWORD, "%.1f%%", 1.0, 10.0,
            "เพิ่มความเสียหายกายภาพ %"),
    PDMG_FLAT("PDmgBonusFlat", "§cP.DMG Bonus (Flat)", Material.REDSTONE_BLOCK, "%.0f", 1.0, 10.0,
            "เพิ่มความเสียหายกายภาพ (หน่วย)"),
    PDMG_REDUCTION_PERCENT("PDmgReductionPercent", "§cP.DMG Reduction %", Material.IRON_CHESTPLATE, "%.1f%%", 1.0, 10.0,
            "ลดความเสียหายกายภาพที่ได้รับ %"),

    MDMG_PERCENT("MdmgBonusPercent", "§dM.DMG Bonus %", Material.DIAMOND_HOE, "%.1f%%", 1.0, 10.0,
            "เพิ่มความเสียหายเวทมนตร์ %"),
    MDMG_FLAT("MdmgBonusFlat", "§dM.DMG Bonus (Flat)", Material.LAPIS_BLOCK, "%.0f", 1.0, 10.0,
            "เพิ่มความเสียหายเวทมนตร์ (หน่วย)"),
    MDMG_REDUCTION_PERCENT("MdmgReductionPercent", "§dM.DMG Reduction %", Material.LEATHER_CHESTPLATE, "%.1f%%", 1.0, 10.0,
            "ลดความเสียหายเวทมนตร์ที่ได้รับ %"),

    FINAL_DMG_PERCENT("FinalDmgPercent", "§6Final DMG %", Material.GOLD_INGOT, "%.1f%%", 1.0, 10.0,
            "คูณความเสียหายสุดท้ายทั้งหมด %"),
    FINAL_PDMG_PERCENT("FinalPDmgPercent", "§6Final P.DMG %", Material.DIAMOND_CHESTPLATE, "%.1f%%", 1.0, 10.0,
            "คูณความเสียหายกายภาพสุดท้าย %"),
    FINAL_MDMG_PERCENT("FinalMDmgPercent", "§6Final M.DMG %", Material.GOLDEN_CHESTPLATE, "%.1f%%", 1.0, 10.0,
            "คูณความเสียหายเวทมนตร์สุดท้าย %"),

    FINAL_DMG_RES_PERCENT("FinalDmgResPercent", "§6Final Res %", Material.GOLD_BLOCK, "%.1f%%", 1.0, 10.0,
            "ลดความเสียหายสุดท้ายทั้งหมด %"),
    TRUE_DMG("TrueDamageFlat", "§6True Damage", Material.NETHER_STAR, "%.0f", 1.0, 10.0,
            "ความเสียหายจริง (ไม่คิดเกราะ)"),

    // --- 8. Distance-Type ---
    MELEE_PDMG_PERCENT("MeleePDmgPercent", "§aMelee P.DMG %", Material.BLAZE_ROD, "%.1f%%", 1.0, 10.0,
            "เพิ่มความเสียหายกายภาพระยะใกล้ %"),
    MELEE_PDMG_REDUCTION_PERCENT("MeleePDReductionPercent", "§aMelee P.Res %", Material.CHAINMAIL_CHESTPLATE, "%.1f%%", 1.0, 10.0,
            "ลดความเสียหายกายภาพระยะใกล้ %"),

    RANGE_PDMG_PERCENT("RangePDmgPercent", "§aRange P.DMG %", Material.SPECTRAL_ARROW, "%.1f%%", 1.0, 10.0,
            "เพิ่มความเสียหายกายภาพระยะไกล %"),
    RANGE_PDMG_REDUCTION_PERCENT("RangePDReductionPercent", "§aRange P.Res %", Material.CHAINMAIL_HELMET, "%.1f%%", 1.0, 10.0,
            "ลดความเสียหายกายภาพระยะไกล %"),

    // --- 9. Content-Type ---
    PVE_DMG_PERCENT("PveDmgBonusPercent", "§aPvE DMG Bonus", Material.OAK_SAPLING, "%.0f", 1.0, 10.0,
            "เพิ่มความเสียหายต่อมอนสเตอร์ (PvE)"),
    PVE_DMG_REDUCTION_PERCENT("PveDmgReductionPercent", "§aPvE DMG Res", Material.SPRUCE_SAPLING, "%.0f", 1.0, 10.0,
            "ลดความเสียหายจากมอนสเตอร์ (PvE)"),

    PVP_DMG_PERCENT("PvpDmgBonusPercent", "§cPvP DMG Bonus", Material.IRON_SWORD, "%.0f", 1.0, 10.0,
            "เพิ่มความเสียหายต่อผู้เล่น (PvP)"),
    PVP_DMG_REDUCTION_PERCENT("PvpDmgReductionPercent", "§cPvP DMG Res", Material.IRON_AXE, "%.0f", 1.0, 10.0,
            "ลดความเสียหายจากผู้เล่น (PvP)"),

    // --- 10. Healing & Defense ---
    HEALING_EFFECT_PERCENT("HealingEffectPercent", "§aHeal Effect %", Material.GLOW_BERRIES, "%.1f%%", 1.0, 10.0,
            "เพิ่มผลการรักษาที่ทำได้ %"),
    HEALING_RECEIVED_PERCENT("HealingReceivedPercent", "§aHeal Received %", Material.GLOWSTONE_DUST, "%.1f%%", 1.0, 10.0,
            "เพิ่มผลการรักษาที่ได้รับ %"),

    LIFESTEAL_P_PERCENT("LifestealPPercent", "§cLifesteal %", Material.POISONOUS_POTATO, "%.1f%%", 1.0, 10.0,
            "ดูดเลือดจากการโจมตีกายภาพ %"),
    LIFESTEAL_M_PERCENT("LifestealMPercent", "§dM. Lifesteal %", Material.ROTTEN_FLESH, "%.1f%%", 1.0, 10.0,
            "ดูดเลือดจากการโจมตีเวทมนตร์ %"),

    SHIELD_VALUE_FLAT("ShieldValueFlat", "§bShield", Material.PRISMARINE_SHARD, "%.0f", 1.0, 10.0,
            "สร้างเกราะป้องกันความเสียหาย (หน่วย)"),
    SHIELD_RATE_PERCENT("ShieldRatePercent", "§bShield Rate %", Material.PRISMARINE_CRYSTALS, "%.1f%%", 1.0, 10.0,
            "อัตราการสร้างเกราะ %");

    private final String key;
    private final String displayName;
    private final Material material;
    private final String format;
    private final double clickStep;
    private final double rightClickStep;
    private final String[] description;
    private NamespacedKey namespacedKey;

    ItemAttributeType(String key, String displayName, Material material, String format, double clickStep, double rightClickStep, String... description) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.format = format;
        this.clickStep = clickStep;
        this.rightClickStep = rightClickStep;
        this.description = description;
    }

    public void initialize(ThaiRoCorePlugin plugin) {
        this.namespacedKey = new NamespacedKey(plugin, "RO_BONUS_" + this.key.toUpperCase());
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public String getFormat() { return format; }
    public double getClickStep() { return clickStep; }
    public double getRightClickStep() { return rightClickStep; }
    public List<String> getDescription() { return Arrays.asList(description); }
    public NamespacedKey getNamespacedKey() { return namespacedKey; }
}