package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.rostats.ThaiRoCorePlugin;

public enum ItemAttributeType {
    // --- 1. Base Attributes ---
    STR_GEAR("STRBonusGear", "§cSTR", Material.IRON_BLOCK, "%.0f", 1.0, 10.0),
    AGI_GEAR("AGIBonusGear", "§bAGI", Material.FEATHER, "%.0f", 1.0, 10.0),
    VIT_GEAR("VITBonusGear", "§aVIT", Material.IRON_CHESTPLATE, "%.0f", 1.0, 10.0),
    INT_GEAR("INTBonusGear", "§dINT", Material.ENCHANTED_BOOK, "%.0f", 1.0, 10.0),
    DEX_GEAR("DEXBonusGear", "§6DEX", Material.BOW, "%.0f", 1.0, 10.0),
    LUK_GEAR("LUKBonusGear", "§eLUK", Material.RABBIT_FOOT, "%.0f", 1.0, 10.0),

    MAXHP_PERCENT("MaxHPPercent", "§aMax HP %", Material.RED_WOOL, "%.1f%%", 1.0, 10.0),
    MAXSP_PERCENT("MaxSPPercent", "§bMax SP %", Material.BLUE_WOOL, "%.1f%%", 1.0, 10.0),

    HIT_BONUS_FLAT("HitBonusFlat", "§6HIT", Material.TARGET, "%.0f", 1.0, 10.0),
    FLEE_BONUS_FLAT("FleeBonusFlat", "§bFLEE", Material.FEATHER, "%.0f", 1.0, 10.0),
    BASE_MSPD("BaseMSPD", "§aBase Move Speed", Material.SUGAR, "%.2f", 0.01, 0.1),

    // --- 2. Core Combat Stats ---
    WEAPON_PATK("WeaponPAtk", "§cP.ATK (Equip)", Material.IRON_SWORD, "%.0f", 1.0, 10.0),
    WEAPON_MATK("WeaponMAtk", "§dM.ATK (Equip)", Material.ENCHANTED_BOOK, "%.0f", 1.0, 10.0),
    PATK_FLAT("PAtkBonusFlat", "§cBonus P.ATK", Material.REDSTONE, "%.0f", 1.0, 10.0),
    MATK_FLAT("MAtkBonusFlat", "§dBonus M.ATK", Material.LAPIS_LAZULI, "%.0f", 1.0, 10.0),

    // --- 3. Penetration / Ignore Def ---
    P_PEN_FLAT("PPenFlat", "§6P.Penetration", Material.IRON_NUGGET, "%.0f", 1.0, 10.0),
    P_PEN_PERCENT("PPenPercent", "§6P.Penetration %", Material.IRON_INGOT, "%.1f%%", 1.0, 10.0),
    IGNORE_PDEF_FLAT("IgnorePDefFlat", "§6Ignore P.DEF", Material.ANVIL, "%.0f", 1.0, 10.0),
    IGNORE_PDEF_PERCENT("IgnorePDefPercent", "§6Ignore P.DEF %", Material.IRON_BLOCK, "%.1f%%", 1.0, 10.0),

    M_PEN_FLAT("MPenFlat", "§6M.Penetration", Material.LAPIS_LAZULI, "%.0f", 1.0, 10.0),
    M_PEN_PERCENT("MPenPercent", "§6M.Penetration %", Material.DIAMOND, "%.1f%%", 1.0, 10.0),
    IGNORE_MDEF_FLAT("IgnoreMDefFlat", "§6Ignore M.DEF", Material.BREWING_STAND, "%.0f", 1.0, 10.0),
    IGNORE_MDEF_PERCENT("IgnoreMDefPercent", "§6Ignore M.DEF %", Material.LAPIS_BLOCK, "%.1f%%", 1.0, 10.0),

    // --- 4. Casting ---
    VAR_CT_PERCENT("VarCTPercent", "§dVariable Cast %", Material.CLOCK, "%.1f%%", 1.0, 10.0),
    VAR_CT_FLAT("VarCTFlat", "§dVariable Cast (s)", Material.CLOCK, "%.1f", 0.1, 10.0),
    FIXED_CT_PERCENT("FixedCTPercent", "§dFixed Cast %", Material.COMPASS, "%.1f%%", 1.0, 10.0),
    FIXED_CT_FLAT("FixedCTFlat", "§dFixed Cast (s)", Material.COMPASS, "%.1f", 0.1, 10.0),

    // --- 5. Speed & Mobility ---
    ASPD_PERCENT("ASpdPercent", "§aASPD %", Material.FEATHER, "%.1f%%", 1.0, 10.0),
    MSPD_PERCENT("MSpdPercent", "§aMove Speed %", Material.LEATHER_BOOTS, "%.1f%%", 1.0, 10.0),

    // --- 6. Critical System ---
    CRIT_RATE("CritBonusFlat", "§eCRIT", Material.GOLD_NUGGET, "%.0f", 1.0, 10.0), // Mapped to HitBonusFlat in logic? No, needs backend support, assuming existing keys
    // *NOTE*: Using existing keys for CRIT system from your provided file
    CRIT_DMG_PERCENT("CritDmgPercent", "§eCrit DMG %", Material.YELLOW_DYE, "%.1f%%", 5.0, 10.0),
    CRIT_RES("CritRes", "§eCrit Res", Material.SHIELD, "%.1f", 1.0, 10.0),
    CRIT_DMG_RES_PERCENT("CritDmgResPercent", "§eCrit DMG Res %", Material.SHULKER_SHELL, "%.1f%%", 1.0, 10.0),

    // --- 7. Universal Damage Modifiers ---
    PDMG_PERCENT("PDmgBonusPercent", "§cP.DMG Bonus %", Material.DIAMOND_SWORD, "%.1f%%", 1.0, 10.0),
    PDMG_FLAT("PDmgBonusFlat", "§cP.DMG Bonus (Flat)", Material.REDSTONE_BLOCK, "%.0f", 1.0, 10.0),
    PDMG_REDUCTION_PERCENT("PDmgReductionPercent", "§cP.DMG Reduction %", Material.IRON_CHESTPLATE, "%.1f%%", 1.0, 10.0),

    MDMG_PERCENT("MdmgBonusPercent", "§dM.DMG Bonus %", Material.DIAMOND_HOE, "%.1f%%", 1.0, 10.0),
    MDMG_FLAT("MdmgBonusFlat", "§dM.DMG Bonus (Flat)", Material.LAPIS_BLOCK, "%.0f", 1.0, 10.0),
    MDMG_REDUCTION_PERCENT("MdmgReductionPercent", "§dM.DMG Reduction %", Material.LEATHER_CHESTPLATE, "%.1f%%", 1.0, 10.0),

    FINAL_DMG_PERCENT("FinalDmgPercent", "§6Final DMG %", Material.GOLD_INGOT, "%.1f%%", 1.0, 10.0),
    FINAL_PDMG_PERCENT("FinalPDmgPercent", "§6Final P.DMG %", Material.DIAMOND_CHESTPLATE, "%.1f%%", 1.0, 10.0),
    FINAL_MDMG_PERCENT("FinalMDmgPercent", "§6Final M.DMG %", Material.GOLDEN_CHESTPLATE, "%.1f%%", 1.0, 10.0),

    FINAL_DMG_RES_PERCENT("FinalDmgResPercent", "§6Final Res %", Material.GOLD_BLOCK, "%.1f%%", 1.0, 10.0),
    TRUE_DMG("TrueDamageFlat", "§6True Damage", Material.NETHER_STAR, "%.0f", 1.0, 10.0),

    // --- 8. Distance-Type ---
    MELEE_PDMG_PERCENT("MeleePDmgPercent", "§aMelee P.DMG %", Material.BLAZE_ROD, "%.1f%%", 1.0, 10.0),
    MELEE_PDMG_REDUCTION_PERCENT("MeleePDReductionPercent", "§aMelee P.Res %", Material.CHAINMAIL_CHESTPLATE, "%.1f%%", 1.0, 10.0),

    RANGE_PDMG_PERCENT("RangePDmgPercent", "§aRange P.DMG %", Material.SPECTRAL_ARROW, "%.1f%%", 1.0, 10.0),
    RANGE_PDMG_REDUCTION_PERCENT("RangePDReductionPercent", "§aRange P.Res %", Material.CHAINMAIL_HELMET, "%.1f%%", 1.0, 10.0),

    // --- 9. Content-Type ---
    PVE_DMG_PERCENT("PveDmgBonusPercent", "§aPvE DMG Bonus", Material.OAK_SAPLING, "%.0f", 1.0, 10.0),
    PVE_DMG_REDUCTION_PERCENT("PveDmgReductionPercent", "§aPvE DMG Res", Material.SPRUCE_SAPLING, "%.0f", 1.0, 10.0),

    PVP_DMG_PERCENT("PvpDmgBonusPercent", "§cPvP DMG Bonus", Material.IRON_SWORD, "%.0f", 1.0, 10.0),
    PVP_DMG_REDUCTION_PERCENT("PvpDmgReductionPercent", "§cPvP DMG Res", Material.IRON_AXE, "%.0f", 1.0, 10.0),

    // --- 10. Healing & Defense ---
    HEALING_EFFECT_PERCENT("HealingEffectPercent", "§aHeal Effect %", Material.GLOW_BERRIES, "%.1f%%", 1.0, 10.0),
    HEALING_RECEIVED_PERCENT("HealingReceivedPercent", "§aHeal Received %", Material.GLOWSTONE_DUST, "%.1f%%", 1.0, 10.0),

    LIFESTEAL_P_PERCENT("LifestealPPercent", "§cLifesteal %", Material.POISONOUS_POTATO, "%.1f%%", 1.0, 10.0),
    LIFESTEAL_M_PERCENT("LifestealMPercent", "§dM. Lifesteal %", Material.ROTTEN_FLESH, "%.1f%%", 1.0, 10.0),

    SHIELD_VALUE_FLAT("ShieldValueFlat", "§bShield", Material.PRISMARINE_SHARD, "%.0f", 1.0, 10.0),
    SHIELD_RATE_PERCENT("ShieldRatePercent", "§bShield Rate %", Material.PRISMARINE_CRYSTALS, "%.1f%%", 1.0, 10.0);

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