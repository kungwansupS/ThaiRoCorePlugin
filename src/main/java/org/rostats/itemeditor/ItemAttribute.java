package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
// แก้ไข: เปลี่ยนจาก ROStatsPlugin เป็น ThaiRoCorePlugin
import org.rostats.ThaiRoCorePlugin;

public enum ItemAttribute {
    // Core Flat Bonuses
    WEAPON_PATK("WeaponPAtk", "§cWeapon P.ATK", Material.IRON_SWORD, "%.0f", 1.0, 10.0),
    WEAPON_MATK("WeaponMAtk", "§dWeapon M.ATK", Material.ENCHANTED_BOOK, "%.0f", 1.0, 10.0),
    HIT_BONUS_FLAT("HitBonusFlat", "§6HIT Flat", Material.TARGET, "%.0f", 1.0, 10.0),
    FLEE_BONUS_FLAT("FleeBonusFlat", "§bFLEE Flat", Material.FEATHER, "%.0f", 1.0, 10.0),
    BASE_MSPD("BaseMSPD", "§aBase Move Speed Flat", Material.SUGAR, "%.2f", 0.01, 0.1),
    PATK_FLAT("PAtkBonusFlat", "§cBonus P.ATK Flat", Material.REDSTONE, "%.0f", 1.0, 10.0),
    MATK_FLAT("MAtkBonusFlat", "§dBonus M.ATK Flat", Material.LAPIS_LAZULI, "%.0f", 1.0, 10.0),
    PDMG_FLAT("PDmgBonusFlat", "§cP.DMG Flat", Material.REDSTONE_BLOCK, "%.0f", 1.0, 10.0),
    MDMG_FLAT("MDmgBonusFlat", "§dM.DMG Flat", Material.LAPIS_BLOCK, "%.0f", 1.0, 10.0),
    TRUE_DMG("TrueDamageFlat", "§6True Damage Flat", Material.NETHER_STAR, "%.0f", 1.0, 10.0),

    // Percent Bonuses
    MAXHP_PERCENT("MaxHPPercent", "§aMax HP %", Material.RED_WOOL, "%.1f%%", 1.0, 5.0),
    MAXSP_PERCENT("MaxSPPercent", "§bMax SP %", Material.BLUE_WOOL, "%.1f%%", 1.0, 5.0),
    PDMG_PERCENT("PDmgBonusPercent", "§aP.DMG Bonus %", Material.DIAMOND_SWORD, "%.1f%%", 1.0, 5.0),
    MDMG_PERCENT("MDmgBonusPercent", "§bM.DMG Bonus %", Material.DIAMOND_HOE, "%.1f%%", 1.0, 5.0),
    FINAL_DMG_PERCENT("FinalDmgPercent", "§6Final DMG %", Material.GOLD_INGOT, "%.1f%%", 1.0, 5.0),
    FINAL_DMG_RES_PERCENT("FinalDmgResPercent", "§6Final DMG RES %", Material.GOLD_BLOCK, "%.1f%%", 1.0, 5.0),
    CRIT_DMG_PERCENT("CritDmgPercent", "§eCrit DMG %", Material.YELLOW_DYE, "%.1f%%", 5.0, 10.0),
    FINAL_PDMG_PERCENT("FinalPDmgPercent", "§6Final P.DMG %", Material.DIAMOND_CHESTPLATE, "%.1f%%", 1.0, 5.0),
    FINAL_MDMG_PERCENT("FinalMDmgPercent", "§6Final M.DMG %", Material.GOLDEN_CHESTPLATE, "%.1f%%", 1.0, 5.0),
    MELEE_PDMG_PERCENT("MeleePDmgPercent", "§aMelee P.DMG %", Material.BLAZE_ROD, "%.1f%%", 1.0, 5.0),
    RANGE_PDMG_PERCENT("RangePDmgPercent", "§aRange P.DMG %", Material.SPECTRAL_ARROW, "%.1f%%", 1.0, 5.0),
    PVE_DMG_BONUS_PERCENT("PveDmgBonusPercent", "§aPVE DMG Bonus %", Material.OAK_SAPLING, "%.0f", 1.0, 10.0),
    PVP_DMG_BONUS_PERCENT("PvpDmgBonusPercent", "§cPVP DMG Bonus %", Material.IRON_SWORD, "%.0f", 1.0, 10.0),

    // Defense & Resistances
    PDR_PERCENT("PDmgReductionPercent", "§cP.DMG Reduce %", Material.IRON_CHESTPLATE, "%.1f%%", 1.0, 5.0),
    MDR_PERCENT("MDmgReductionPercent", "§dM.DMG Reduce %", Material.LEATHER_CHESTPLATE, "%.1f%%", 1.0, 5.0),
    CRIT_RES("CritRes", "§eCrit Resistance", Material.SHIELD, "%.1f", 1.0, 10.0),
    CRIT_DMG_RES_PERCENT("CritDmgResPercent", "§eCrit DMG RES %", Material.SHULKER_SHELL, "%.1f%%", 1.0, 5.0),
    PVE_DMG_REDUCTION_PERCENT("PveDmgReductionPercent", "§aPVE DMG Reduce %", Material.SPRUCE_SAPLING, "%.0f", 1.0, 10.0),
    PVP_DMG_REDUCTION_PERCENT("PvpDmgReductionPercent", "§cPVP DMG Reduce %", Material.IRON_AXE, "%.0f", 1.0, 10.0),

    // Penetration & Ignore Def
    P_PEN_FLAT("PPenFlat", "§6P.Pen Flat", Material.IRON_NUGGET, "%.0f", 1.0, 10.0),
    M_PEN_FLAT("MPenFlat", "§6M.Pen Flat", Material.LAPIS_LAZULI, "%.0f", 1.0, 10.0), // <-- CORRECTED: Used LAPIS_LAZULI
    IGNORE_PDEF_PERCENT("IgnorePDefPercent", "§6Ignore P.DEF %", Material.IRON_BLOCK, "%.1f%%", 1.0, 5.0),
    IGNORE_MDEF_PERCENT("IgnoreMDefPercent", "§6Ignore M.DEF %", Material.LAPIS_BLOCK, "%.1f%%", 1.0, 5.0),
    P_PEN_PERCENT("PPenPercent", "§6P.Pen %", Material.IRON_INGOT, "%.1f%%", 1.0, 5.0),
    M_PEN_PERCENT("MPenPercent", "§6M.Pen %", Material.DIAMOND, "%.1f%%", 1.0, 5.0), // <-- CORRECTED: Used DIAMOND
    IGNORE_PDEF_FLAT("IgnorePDefFlat", "§6Ignore P.DEF Flat", Material.ANVIL, "%.0f", 1.0, 10.0),
    IGNORE_MDEF_FLAT("IgnoreMDefFlat", "§6Ignore M.DEF Flat", Material.BREWING_STAND, "%.0f", 1.0, 10.0),

    // Speed & Casting
    ASPD_PERCENT("ASpdPercent", "§aASPD %", Material.FEATHER, "%.1f%%", 1.0, 5.0),
    MSPD_PERCENT("MSpdPercent", "§aMovement SPD %", Material.LEATHER_BOOTS, "%.1f%%", 1.0, 5.0),
    VAR_CT_PERCENT("VarCTPercent", "§dVariable CT %", Material.CLOCK, "%.1f%%", 1.0, 5.0),
    FIXED_CT_PERCENT("FixedCTPercent", "§dFixed CT %", Material.COMPASS, "%.1f%%", 1.0, 5.0),
    VAR_CT_FLAT("VarCTFlat", "§dVariable CT Flat (s)", Material.CLOCK, "%.1f", 0.1, 1.0),
    FIXED_CT_FLAT("FixedCTFlat", "§dFixed CT Flat (s)", Material.COMPASS, "%.1f", 0.1, 1.0),

    // Recovery & Lifesteal
    HEALING_EFFECT_PERCENT("HealingEffectPercent", "§aHealing Effect %", Material.GLOW_BERRIES, "%.1f%%", 1.0, 5.0),
    HEALING_RECEIVED_PERCENT("HealingReceivedPercent", "§aHealing Receive %", Material.GLOWSTONE_DUST, "%.1f%%", 1.0, 5.0),
    LIFESTEAL_P_PERCENT("LifestealPPercent", "§cP. Lifesteal %", Material.POISONOUS_POTATO, "%.1f%%", 1.0, 5.0),
    LIFESTEAL_M_PERCENT("LifestealMPercent", "§dM. Lifesteal %", Material.ROTTEN_FLESH, "%.1f%%", 1.0, 5.0),

    // Shield
    SHIELD_VALUE_FLAT("ShieldValueFlat", "§bShield Value Flat", Material.PRISMARINE_SHARD, "%.0f", 1.0, 10.0),
    SHIELD_RATE_PERCENT("ShieldRatePercent", "§bShield Rate %", Material.PRISMARINE_CRYSTALS, "%.1f%%", 1.0, 5.0);


    private final String key;
    private final String displayName;
    private final Material material;
    private final String format;
    private final double clickStep;
    private final double rightClickStep;
    private NamespacedKey namespacedKey;

    ItemAttribute(String key, String displayName, Material material, String format, double clickStep, double rightClickStep) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.format = format;
        this.clickStep = clickStep;
        this.rightClickStep = rightClickStep;
    }

    // แก้ไข: เปลี่ยน Type ของ plugin เป็น ThaiRoCorePlugin
    public void initialize(ThaiRoCorePlugin plugin) {
        // ใช้ชื่อปลั๊กอินหลักสำหรับ NamespacedKey
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