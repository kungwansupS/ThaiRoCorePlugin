package org.rostats.itemeditor;

public enum ItemAttributeType {

    // --- 1. Base Attributes ---
    // Status
    STR("STR"),
    AGI("AGI"),
    VIT("VIT"),
    INT("INT"),
    DEX("DEX"),
    LUK("LUK"),
    // Derived Base Stats
    MAX_HP("Max HP"),
    MAX_SP("Max SP"),
    HP_RECOVERY("HP Recovery"),
    SP_RECOVERY("SP Recovery"),
    HIT("HIT"),
    FLEE("FLEE"),

    // --- 2. Core Combat Stats ---
    // Attack
    P_ATK("P.ATK"),
    M_ATK("M.ATK"),
    REFINE_P_ATK("Refine P.ATK"),
    REFINE_M_ATK("Refine M.ATK"),
    // Defense
    P_DEF("P.DEF"),
    M_DEF("M.DEF"),
    REFINE_P_DEF("Refine P.DEF"),
    REFINE_M_DEF("Refine M.DEF"),

    // --- 3. Penetration / Ignore Def ---
    // Physical
    PEN_P_FLAT("Physical Penetration (flat)"),
    PEN_P_PERCENT("Physical Penetration%"),
    IGNORE_P_DEF_FLAT("Ignore P.DEF (flat)"),
    IGNORE_P_DEF_PERCENT("Ignore P.DEF%"),
    // Magical
    PEN_M_FLAT("Magic Penetration (flat)"),
    PEN_M_PERCENT("Magic Penetration%"),
    IGNORE_M_DEF_FLAT("Ignore M.DEF (flat)"),
    IGNORE_M_DEF_PERCENT("Ignore M.DEF%"),

    // --- 4. Casting ---
    // Variable Casting
    VAR_CASTING_PERCENT("Variable Casting%"),
    VAR_CT_FLAT("Variable CT (Flat)"),
    // Fixed Casting
    FIXED_CASTING_PERCENT("Fixed Casting%"),
    FIXED_CT_FLAT("Fixed CT (Flat)"),

    // --- 5. Cooldown / Delay / Motion System ---
    // Cooldown
    SKILL_COOLDOWN_PERCENT("Skill Cooldown%"),
    SKILL_COOLDOWN_FLAT("Skill Cooldown Flat"),
    FINAL_COOLDOWN_PERCENT("Final Cooldown%"),
    // Global Delay
    GLOBAL_CD_PERCENT("Global CD%"),
    AFTER_CAST_DELAY_PERCENT("After-Cast Delay%"),
    AFTER_CAST_DELAY_FLAT("After-Cast Delay Flat"),
    // Motion
    PRE_MOTION("Pre-motion modifier"),
    POST_MOTION("Post-motion modifier"),
    CANCEL_MOTION("Cancel motion factor"),

    // --- 6. Speed & Mobility ---
    ASPD_PERCENT("ASPD%"),
    MSPD_PERCENT("MSPD%"),
    ATK_INTERVAL_REDUCTION("Attack Interval Reduction%"),

    // --- 7. Critical System ---
    // Critical Offensive
    CRIT("CRIT"),
    CRIT_DMG_PERCENT("CRIT DMG%"),
    FINAL_CRIT_DMG_PERCENT("Final Crit DMG%"),
    // Critical Defensive
    CRIT_RES("CRIT RES"),
    CRIT_DMG_RES_PERCENT("CRIT DMG RES%"),
    PERFECT_DODGE("Perfect Dodge"),
    PERFECT_HIT("Perfect Hit"),

    // --- 8. Universal Damage Modifiers ---
    // Physical
    P_DMG_BONUS_PERCENT("P.DMG Bonus%"),
    P_DMG_BONUS_FLAT("P.DMG Bonus (flat)"),
    P_DMG_REDUCTION_PERCENT("P.DMG Reduction%"),
    // Magical
    M_DMG_BONUS_PERCENT("M.DMG Bonus%"),
    M_DMG_BONUS_FLAT("M.DMG Bonus (flat)"),
    M_DMG_REDUCTION_PERCENT("M.DMG Reduction%"),
    // Extra
    TRUE_DAMAGE("True Damage"),

    // --- 9. Distance-Type Damage ---
    // Melee
    MELEE_P_DMG_PERCENT("Melee P.DMG%"),
    MELEE_P_DMG_REDUCTION_PERCENT("Melee P.DMG Reduction%"),
    // Ranged
    RANGED_P_DMG_PERCENT("Ranged P.DMG%"),
    RANGED_P_DMG_REDUCTION_PERCENT("Ranged P.DMG Reduction%"),

    // --- 10. Content-Type Modifiers ---
    // PvE
    PVE_DMG_BONUS("PvE DMG Bonus"),
    PVE_DMG_REDUCTION("PvE DMG Reduction"),
    // PvP
    PVP_DMG_BONUS("PvP DMG Bonus"),
    PVP_DMG_REDUCTION("PvP DMG Reduction"),

    // --- 11. Healing System ---
    // Heal Output
    HEALING_EFFECT_PERCENT("Healing Effect%"),
    HEALING_FLAT("Healing Flat"),
    // Heal Taken
    HEALING_RECEIVED_PERCENT("Healing Received%"),
    RECEIVED_HEAL_FLAT("Received Heal Flat"),

    // --- Special / Misc ---
    MAX_HP_PERCENT("Max HP%"),
    MAX_SP_PERCENT("Max SP%"),
    SHIELD_VALUE("Shield Value");

    private final String displayName;

    ItemAttributeType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ItemAttributeType fromDisplayName(String name) {
        for (ItemAttributeType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(name.replace("§a§l", "").trim())) {
                return type;
            }
            // Fallback for loose matching
            if (type.getDisplayName().replace(" (flat)", "").equalsIgnoreCase(name)) {
                return type;
            }
        }
        // Try matching enum name directly as backup
        try {
            return ItemAttributeType.valueOf(name.replace(" ", "_").toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}