package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.rostats.ThaiRoCorePlugin;

public enum ItemAttributeType {
    // --- 1. Base Attributes ---
    // Status
    STR_GEAR("STRBonusGear", "§cSTR", Material.IRON_SWORD, "%.0f", 1.0, 10.0),
    AGI_GEAR("AGIBonusGear", "§bAGI", Material.FEATHER, "%.0f", 1.0, 10.0),
    VIT_GEAR("VITBonusGear", "§aVIT", Material.IRON_CHESTPLATE, "%.0f", 1.0, 10.0),
    INT_GEAR("INTBonusGear", "§dINT", Material.ENCHANTED_BOOK, "%.0f", 1.0, 10.0),
    DEX_GEAR("DEXBonusGear", "§6DEX", Material.BOW, "%.0f", 1.0, 10.0),
    LUK_GEAR("LUKBonusGear", "§eLUK", Material.RABBIT_FOOT, "%.0f", 1.0, 10.0),
    // Derived Base Stats
    MAXHP_PERCENT("MaxHPPercent", "§aMax HP %", Material.RED_WOOL, "%.1f%%", 1.0, 5.0),
    MAXHP_FLAT("MaxHPFlat", "§aMax HP", Material.RED_DYE, "%.0f", 10.0, 100.0),
    MAXSP_PERCENT("MaxSPPercent", "§bMax SP %", Material.BLUE_WOOL, "%.1f%%", 1.0, 5.0),
    MAXSP_FLAT("MaxSPFlat", "§bMax SP", Material.BLUE_DYE, "%.0f", 5.0, 50.0),
    HP_RECOVERY("HPRecovery", "§aHP Recovery", Material.APPLE, "%.1f", 1.0, 5.0),
    SP_RECOVERY("SPRecovery", "§bSP Recovery", Material.GOLDEN_CARROT, "%.1f", 1.0, 5.0),
    HIT_BONUS_FLAT("HitBonusFlat", "§6HIT", Material.TARGET, "%.0f", 1.0, 10.0),
    FLEE_BONUS_FLAT("FleeBonusFlat", "§bFLEE", Material.LEATHER_BOOTS, "%.0f", 1.0, 10.0),

    // --- 2. Core Combat Stats ---
    // Attack
    WEAPON_PATK("WeaponPAtk", "§cP.ATK", Material.IRON_AXE, "%.0f", 1.0, 10.0),
    WEAPON_MATK("WeaponMAtk", "§dM.ATK", Material.BLAZE_ROD, "%.0f", 1.0, 10.0),
    REFINE_P_ATK("RefinePAtk", "§cRefine P.ATK", Material.FLINT, "%.0f", 1.0, 5.0),
    REFINE_M_ATK("RefineMAtk", "§dRefine M.ATK", Material.GLOWSTONE_DUST, "%.0f", 1.0, 5.0),
    // Defense
    P_DEF_BONUS("PDefBonus", "§cP.DEF", Material.IRON_CHESTPLATE, "%.0f", 1.0, 10.0),
    M_DEF_BONUS("MDefBonus", "§9M.DEF", Material.CHAINMAIL_CHESTPLATE, "%.0f", 1.0, 10.0),
    REFINE_P_DEF("RefinePDef", "§cRefine P.DEF", Material.IRON_NUGGET, "%.0f", 1.0, 5.0),
    REFINE_M_DEF("RefineMDef", "§9Refine M.DEF", Material.LAPIS_LAZULI, "%.0f", 1.0, 5.0),

    // --- 3. Penetration / Ignore Def ---
    // Physical
    P_PEN_FLAT("PPenFlat", "§6Phys Pen", Material.ARROW, "%.0f", 1.0, 5.0),
    P_PEN_PERCENT("PPenPercent", "§6Phys Pen %", Material.SPECTRAL_ARROW, "%.1f%%", 1.0, 5.0),
    IGNORE_PDEF_FLAT("IgnorePDefFlat", "§6Ignore P.DEF", Material.IRON_PICKAXE, "%.0f", 1.0, 10.0),
    IGNORE_PDEF_PERCENT("IgnorePDefPercent", "§6Ignore P.DEF %", Material.DIAMOND_PICKAXE, "%.1f%%", 1.0, 5.0),
    // Magical
    M_PEN_FLAT("MPenFlat", "§dMagic Pen", Material.ENDER_PEARL, "%.0f", 1.0, 5.0),
    M_PEN_PERCENT("MPenPercent", "§dMagic Pen %", Material.ENDER_EYE, "%.1f%%", 1.0, 5.0),
    IGNORE_MDEF_FLAT("IgnoreMDefFlat", "§dIgnore M.DEF", Material.GOLDEN_PICKAXE, "%.0f", 1.0, 10.0),
    IGNORE_MDEF_PERCENT("IgnoreMDefPercent", "§dIgnore M.DEF %", Material.NETHERITE_PICKAXE, "%.1f%%", 1.0, 5.0),

    // --- 4. Casting ---
    VAR_CT_PERCENT("VarCTPercent", "§dVar Cast %", Material.CLOCK, "%.1f%%", 1.0, 5.0),
    VAR_CT_FLAT("VarCTFlat", "§dVar CT (Flat)", Material.COMPASS, "%.1f s", 0.1, 0.5),
    FIXED_CT_PERCENT("FixedCTPercent", "§eFixed Cast %", Material.WATCH, "%.1f%%", 1.0, 5.0), // Changed WATCH_DODGE to WATCH/CLOCK compatible
    FIXED_CT_FLAT("FixedCTFlat", "§eFixed CT (Flat)", Material.RECOVERY_COMPASS, "%.1f s", 0.1, 0.5),

    // --- 5. Cooldown / Delay / Motion ---
    // Cooldown
    SKILL_COOLDOWN_PERCENT("SkillCDPercent", "§bSkill CD %", Material.SNOWBALL, "%.1f%%", 1.0, 5.0),
    SKILL_COOLDOWN_FLAT("SkillCDFlat", "§bSkill CD (Flat)", Material.SNOW_BLOCK, "%.1f s", 0.1, 1.0),
    FINAL_COOLDOWN_PERCENT("FinalCDPercent", "§3Final CD %", Material.BLUE_ICE, "%.1f%%", 1.0, 5.0),
    // Global Delay
    GLOBAL_CD_PERCENT("GlobalCDPercent", "§7Global CD %", Material.GRAY_DYE, "%.1f%%", 1.0, 5.0),
    AFTER_CAST_DELAY_PERCENT("AfterCastDelayPercent", "§8After-Cast %", Material.GUNPOWDER, "%.1f%%", 1.0, 5.0),
    AFTER_CAST_DELAY_FLAT("AfterCastDelayFlat", "§8After-Cast (Flat)", Material.FLINT_AND_STEEL, "%.1f s", 0.1, 0.5),
    // Motion
    PRE_MOTION("PreMotion", "§6Pre-Motion", Material.STRING, "%.1f%%", 1.0, 5.0),
    POST_MOTION("PostMotion", "§6Post-Motion", Material.COBWEB, "%.1f%%", 1.0, 5.0),
    CANCEL_MOTION("CancelMotion", "§6Cancel Motion", Material.SHEARS, "%.1f%%", 1.0, 5.0),

    // --- 6. Speed & Mobility ---
    ASPD_PERCENT("ASpdPercent", "§eASPD %", Material.SUGAR, "%.1f%%", 1.0, 5.0),
    MSPD_PERCENT("MSpdPercent", "§fMSPD %", Material.LEATHER_BOOTS, "%.1f%%", 1.0, 5.0),
    BASE_MSPD("BaseMSPD", "§aBase MSPD", Material.SOUL_SAND, "%.2f", 0.01, 0.05),
    ATTACK_INTERVAL_PERCENT("AtkIntervalPercent", "§cAtk Interval %", Material.REPEATER, "%.1f%%", 1.0, 5.0),

    // --- 7. Critical System ---
    // Offensive
    CRIT("Crit", "§eCRIT", Material.GOLD_NUGGET, "%.0f", 1.0, 5.0),
    CRIT_DMG_PERCENT("CritDmgPercent", "§eCRIT DMG %", Material.GOLD_INGOT, "%.1f%%", 1.0, 10.0),
    FINAL_CRIT_DMG_PERCENT("FinalCritDmgPercent", "§6Final CRIT DMG %", Material.GOLD_BLOCK, "%.1f%%", 1.0, 10.0),
    PERFECT_HIT("PerfectHit", "§ePerfect Hit", Material.SPECTRAL_ARROW, "%.1f%%", 1.0, 5.0),
    // Defensive
    CRIT_RES("CritRes", "§7CRIT RES", Material.SHIELD, "%.0f", 1.0, 5.0),
    CRIT_DMG_RES_PERCENT("CritDmgResPercent", "§7CRIT DMG RES %", Material.IRON_BARS, "%.1f%%", 1.0, 5.0),
    PERFECT_DODGE("PerfectDodge", "§fPerfect Dodge", Material.GHAST_TEAR, "%.1f", 1.0, 5.0),

    // --- 8. Universal Damage Modifiers ---
    // Physical
    PDMG_PERCENT("PDmgBonusPercent", "§cP.DMG Bonus %", Material.DIAMOND_SWORD, "%.1f%%", 1.0, 5.0),
    PDMG_FLAT("PDmgBonusFlat", "§cP.DMG Bonus (Flat)", Material.REDSTONE, "%.0f", 1.0, 10.0),
    PDMG_REDUCTION_PERCENT("PDmgReductionPercent", "§cP.DMG Reduction %", Material.DIAMOND_CHESTPLATE, "%.1f%%", 1.0, 5.0),
    // Magical
    MDMG_PERCENT("MDmgBonusPercent", "§dM.DMG Bonus %", Material.AMETHYST_SHARD, "%.1f%%", 1.0, 5.0),
    MDMG_FLAT("MDmgBonusFlat", "§dM.DMG Bonus (Flat)", Material.LAPIS_LAZULI, "%.0f", 1.0, 10.0),
    MDMG_REDUCTION_PERCENT("MDmgReductionPercent", "§dM.DMG Reduction %", Material.GOLDEN_CHESTPLATE, "%.1f%%", 1.0, 5.0),
    // True
    TRUE_DMG("TrueDamageFlat", "§6True Damage", Material.NETHER_STAR, "%.0f", 1.0, 10.0),
    // Final
    FINAL_DMG_PERCENT("FinalDmgPercent", "§4Final DMG %", Material.TNT, "%.1f%%", 1.0, 5.0),
    FINAL_DMG_RES_PERCENT("FinalDmgResPercent", "§4Final RES %", Material.BEDROCK, "%.1f%%", 1.0, 5.0),

    // --- 9. Distance-Type Damage ---
    MELEE_PDMG_PERCENT("MeleePDmgPercent", "§cMelee P.DMG %", Material.WOODEN_SWORD, "%.1f%%", 1.0, 5.0),
    MELEE_PDMG_REDUCTION_PERCENT("MeleePDReductionPercent", "§cMelee Reduc %", Material.LEATHER_HELMET, "%.1f%%", 1.0, 5.0),
    RANGE_PDMG_PERCENT("RangePDmgPercent", "§aRanged P.DMG %", Material.BOW, "%.1f%%", 1.0, 5.0),
    RANGE_PDMG_REDUCTION_PERCENT("RangePDReductionPercent", "§aRanged Reduc %", Material.LEATHER_CHESTPLATE, "%.1f%%", 1.0, 5.0),

    // --- 10. Content-Type Modifiers ---
    PVE_DMG_PERCENT("PveDmgBonusPercent", "§2PvE DMG Bonus", Material.ZOMBIE_HEAD, "%.0f", 1.0, 10.0),
    PVE_DMG_REDUCTION_PERCENT("PveDmgReductionPercent", "§2PvE DMG Reduc", Material.CREEPER_HEAD, "%.0f", 1.0, 10.0),
    PVP_DMG_PERCENT("PvpDmgBonusPercent", "§cPvP DMG Bonus", Material.PLAYER_HEAD, "%.0f", 1.0, 10.0),
    PVP_DMG_REDUCTION_PERCENT("PvpDmgReductionPercent", "§cPvP DMG Reduc", Material.SKELETON_SKULL, "%.0f", 1.0, 10.0),

    // --- 11. Healing System ---
    HEALING_EFFECT_PERCENT("HealingEffectPercent", "§aHeal Effect %", Material.GLOW_BERRIES, "%.1f%%", 1.0, 5.0),
    HEALING_FLAT("HealingFlat", "§aHeal (Flat)", Material.MELON_SLICE, "%.0f", 1.0, 10.0),
    HEALING_RECEIVED_PERCENT("HealingReceivedPercent", "§aReceived Heal %", Material.GLOWSTONE_DUST, "%.1f%%", 1.0, 5.0),
    HEALING_RECEIVED_FLAT("HealingReceivedFlat", "§aReceived Heal (Flat)", Material.GLISTERING_MELON_SLICE, "%.0f", 1.0, 10.0),
    LIFESTEAL_P_PERCENT("LifestealPPercent", "§cLifesteal %", Material.POISONOUS_POTATO, "%.1f%%", 1.0, 5.0),
    LIFESTEAL_M_PERCENT("LifestealMPercent", "§dLifesteal M %", Material.ROTTEN_FLESH, "%.1f%%", 1.0, 5.0),

    // Misc
    SHIELD_VALUE_FLAT("ShieldValueFlat", "§bShield", Material.PRISMARINE_SHARD, "%.0f", 10.0, 100.0),
    SHIELD_RATE_PERCENT("ShieldRatePercent", "§bShield Rate %", Material.PRISMARINE_CRYSTALS, "%.1f%%", 1.0, 5.0);

    private final String key;
    private final String displayName;
    private final Material material;
    private final String format;
    private final double clickStep;
    private final double rightClickStep;
    private NamespacedKey namespacedKey;

    ItemAttributeType(String key, String displayName, Material material, String format, double clickStep, double rightClickStep) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.format = format;
        this.clickStep = clickStep;
        this.rightClickStep = rightClickStep;
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
    public NamespacedKey getNamespacedKey() { return namespacedKey; }
}