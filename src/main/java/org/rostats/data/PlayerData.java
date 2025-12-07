package org.rostats.data;

import org.bukkit.Bukkit; // เพิ่ม import สำหรับ Bukkit
import org.bukkit.entity.Player; // เพิ่ม import สำหรับ Player
import org.rostats.ThaiRoCorePlugin; // แก้ไข: เปลี่ยนเป็น ThaiRoCorePlugin
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class PlayerData {
    private int baseLevel = 1;
    private long baseExp = 0;
    private int jobLevel = 1;
    private long jobExp = 0;
    private int statPoints = 0;
    private int skillPoints = 0;
    private double currentSP = 20;
    private int resetCount = 0;

    // Core Stats
    private final Map<String, Integer> stats = new HashMap<>();
    // Pending Stats
    private final Map<String, Integer> pendingStats = new HashMap<>();

    // Advanced/Bonus Attributes (All initialized to 0.0)
    // NOTE: These fields now represent the SUM of all Boni from all equipped items.

    // --- Flat ATK ---
    private double pAtkBonusFlat = 0.0; // PAtkBonusFlat
    private double mAtkBonusFlat = 0.0; // MAtkBonusFlat
    private double weaponPAtk = 0.0; // WeaponPAtk
    private double weaponMAtk = 0.0; // WeaponMAtk

    // --- Critical ---
    private double critRes = 0.0; // CritRes
    private double critDmgPercent = 50.0; // CritDmgPercent (Base is 50.0, added by gear)
    private double critDmgResPercent = 0.0; // CritDmgResPercent

    // --- Damage %/Flat ---
    private double pDmgBonusPercent = 0.0; // PDmgBonusPercent
    private double mDmgBonusPercent = 0.0; // MDmgBonusPercent
    private double pDmgBonusFlat = 0.0; // PDmgBonusFlat (same as PDMG_FLAT/MDMG_FLAT in editor)
    private double mDmgBonusFlat = 0.0; // MDmgBonusFlat
    private double pDmgReductionPercent = 0.0; // PDmgReductionPercent
    private double mDmgReductionPercent = 0.0; // MDmgReductionPercent

    // --- Melee/Range ---
    private double meleePDmgPercent = 0.0; // MeleePDmgPercent
    private double rangePDmgPercent = 0.0; // RangePDmgPercent
    private double meleePDReductionPercent = 0.0; // MeleePDReductionPercent
    private double rangePDReductionPercent = 0.0; // RangePDReductionPercent

    // --- Penetration / Ignore Def ---
    private double pPenFlat = 0.0; // PPenFlat
    private double mPenFlat = 0.0; // MPenFlat
    private double pPenPercent = 0.0; // PPenPercent
    private double mPenPercent = 0.0; // MPenPercent
    private double ignorePDefFlat = 0.0; // IgnorePDefFlat
    private double ignoreMDefFlat = 0.0; // IgnoreMDefFlat
    private double ignorePDefPercent = 0.0; // IgnorePDefPercent
    private double ignoreMDefPercent = 0.0; // IgnoreMDefPercent

    // --- Speed / Cast ---
    private double aSpdPercent = 0.0; // ASpdPercent
    private double mSpdPercent = 0.0; // MSpdPercent
    private double baseMSPD = 0.1; // BaseMSPD (Base is 0.1, added by gear)
    private double varCTPercent = 0.0; // VarCTPercent
    private double varCTFlat = 0.0; // VarCTFlat
    private double fixedCTPercent = 0.0; // FixedCTPercent
    private double fixedCTFlat = 0.0; // FixedCTFlat

    // --- Healing / Lifesteal ---
    private double healingEffectPercent = 0.0; // HealingEffectPercent
    private double healingReceivedPercent = 0.0; // HealingReceivedPercent
    private double lifestealPPercent = 0.0; // LifestealPPercent
    private double lifestealMPercent = 0.0; // LifestealMPercent
    private double trueDamageFlat = 0.0; // TrueDamageFlat

    // --- Final Damage ---
    private double finalDmgPercent = 0.0; // FinalDmgPercent
    private double finalDmgResPercent = 0.0; // FinalDmgResPercent
    private double finalPDmgPercent = 0.0; // FinalPDmgPercent
    private double finalMDmgPercent = 0.0; // FinalMDmgPercent

    // --- PVE/PVP ---
    private double pveDmgBonusPercent = 0.0; // PveDmgBonusPercent
    private double pvpDmgBonusPercent = 0.0; // PvpDmgBonusPercent
    private double pveDmgReductionPercent = 0.0; // PveDmgReductionPercent
    private double pvpDmgReductionPercent = 0.0; // PvpDmgReductionPercent

    // --- Max HP/SP % ---
    private double maxHPPercent = 0.0; // MaxHPPercent
    private double maxSPPercent = 0.0; // MaxSPPercent

    // --- Shield ---
    private double shieldValueFlat = 0.0; // ShieldValueFlat
    private double shieldRatePercent = 0.0; // ShieldRatePercent

    // --- Hit/Flee ---
    private double hitBonusFlat = 0.0; // HitBonusFlat
    private double fleeBonusFlat = 0.0; // FleeBonusFlat

    // NEW FIELDS for Core Stat Bonuses from Gear (Req 3)
    private int strBonusGear = 0;
    private int agiBonusGear = 0;
    private int vitBonusGear = 0;
    private int intBonusGear = 0;
    private int dexBonusGear = 0;
    private int lukBonusGear = 0;

    private final ThaiRoCorePlugin plugin; // แก้ไข: เปลี่ยน Type

    public PlayerData(ThaiRoCorePlugin plugin) { // แก้ไข: เปลี่ยน Type
        this.plugin = plugin;
        stats.put("STR", 1); stats.put("AGI", 1); stats.put("VIT", 1);
        stats.put("INT", 1); stats.put("DEX", 1); stats.put("LUK", 1);
        calculateMaxSP();
        pendingStats.put("STR", 0); pendingStats.put("AGI", 0); pendingStats.put("VIT", 0);
        pendingStats.put("INT", 0); pendingStats.put("DEX", 0); pendingStats.put("LUK", 0);
    }

    // --- General Getters/Setters ---

    public int getStat(String key) { return stats.getOrDefault(key.toUpperCase(), 1); }
    public void setStat(String key, int val) { stats.put(key.toUpperCase(), val); calculateMaxSP(); }
    public Set<String> getStatKeys() { return stats.keySet(); }
    public int getPendingStat(String key) { return pendingStats.getOrDefault(key.toUpperCase(), 0); }
    public void setPendingStat(String key, int count) { pendingStats.put(key.toUpperCase(), count); }
    public void clearAllPendingStats() { pendingStats.put("STR", 0); pendingStats.put("AGI", 0); pendingStats.put("VIT", 0);
        pendingStats.put("INT", 0); pendingStats.put("DEX", 0); pendingStats.put("LUK", 0); }
    public int getBaseLevel() { return baseLevel; }
    public void setBaseLevel(int level) { this.baseLevel = level; calculateMaxSP(); }
    public int getJobLevel() { return jobLevel; }
    public void setJobLevel(int jobLevel) { this.jobLevel = jobLevel; }
    public long getJobExp() { return jobExp; }
    public void setJobExp(long jobExp) { this.jobExp = jobExp; }
    public int getStatPoints() { return statPoints; }
    public void setStatPoints(int points) { this.statPoints = points; }
    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }
    public double getCurrentSP() { return currentSP; }
    public void setCurrentSP(double sp) { this.currentSP = sp; }
    public int getResetCount() { return resetCount; }
    public void setResetCount(int count) { this.resetCount = count; }
    public void incrementResetCount() { this.resetCount++; }

    // --- Gear/Item Attribute Getters/Setters (Required by Prompt) ---

    // Core Stat Bonuses (Gear)
    public int getSTRBonusGear() { return strBonusGear; }
    public void setSTRBonusGear(int val) { this.strBonusGear = val; }
    public int getAGIBonusGear() { return agiBonusGear; }
    public void setAGIBonusGear(int val) { this.agiBonusGear = val; }
    public int getVITBonusGear() { return vitBonusGear; }
    public void setVITBonusGear(int val) { this.vitBonusGear = val; }
    public int getINTBonusGear() { return intBonusGear; }
    public void setINTBonusGear(int val) { this.intBonusGear = val; }
    public int getDEXBonusGear() { return dexBonusGear; }
    public void setDEXBonusGear(int val) { this.dexBonusGear = val; }
    public int getLUKBonusGear() { return lukBonusGear; }
    public void setLUKBonusGear(int val) { this.lukBonusGear = val; }

    // Flat ATK
    public double getWeaponPAtk() { return weaponPAtk; }
    public void setWeaponPAtk(double weaponPAtk) { this.weaponPAtk = weaponPAtk; }
    public double getWeaponMAtk() { return weaponMAtk; }
    public void setWeaponMAtk(double weaponMAtk) { this.weaponMAtk = weaponMAtk; }
    public double getPAtkBonusFlat() { return pAtkBonusFlat; } // Used to be PAtkBonusFlat
    public void setPAtkBonusFlat(double pAtkBonusFlat) { this.pAtkBonusFlat = pAtkBonusFlat; }
    public double getMAtkBonusFlat() { return mAtkBonusFlat; } // Used to be MAtkBonusFlat
    public void setMAtkBonusFlat(double mAtkBonusFlat) { this.mAtkBonusFlat = mAtkBonusFlat; }

    // Damage %/Flat (P/M DMg)
    public double getPDmgBonusPercent() { return pDmgBonusPercent; }
    public void setPDmgBonusPercent(double pDmgBonusPercent) { this.pDmgBonusPercent = pDmgBonusPercent; }
    public double getMDmgBonusPercent() { return mDmgBonusPercent; }
    public void setMDmgBonusPercent(double mDmgBonusPercent) { this.mDmgBonusPercent = mDmgBonusPercent; }
    public double getPDmgBonusFlat() { return pDmgBonusFlat; }
    public void setPDmgBonusFlat(double pDmgBonusFlat) { this.pDmgBonusFlat = pDmgBonusFlat; }
    public double getMDmgBonusFlat() { return mDmgBonusFlat; }
    public void setMDmgBonusFlat(double mDmgBonusFlat) { this.mDmgBonusFlat = mDmgBonusFlat; }
    public double getPDmgReductionPercent() { return pDmgReductionPercent; }
    public void setPDmgReductionPercent(double pDmgReductionPercent) { this.pDmgReductionPercent = pDmgReductionPercent; }
    public double getMDmgReductionPercent() { return mDmgReductionPercent; }
    public void setMDmgReductionPercent(double mDmgReductionPercent) { this.mDmgReductionPercent = mDmgReductionPercent; }

    // Melee/Range
    public double getMeleePDmgPercent() { return meleePDmgPercent; }
    public void setMeleePDmgPercent(double meleePDmgPercent) { this.meleePDmgPercent = meleePDmgPercent; }
    public double getRangePDmgPercent() { return rangePDmgPercent; }
    public void setRangePDmgPercent(double rangePDmgPercent) { this.rangePDmgPercent = rangePDmgPercent; }
    public double getMeleePDReductionPercent() { return meleePDReductionPercent; }
    public void setMeleePDReductionPercent(double meleePDReductionPercent) { this.meleePDReductionPercent = meleePDReductionPercent; }
    public double getRangePDReductionPercent() { return rangePDReductionPercent; }
    public void setRangePDReductionPercent(double rangePDReductionPercent) { this.rangePDReductionPercent = rangePDReductionPercent; }

    // Critical
    public double getCritRes() { return critRes; }
    public void setCritRes(double critRes) { this.critRes = critRes; }
    public double getCritDmgPercent() { return critDmgPercent; }
    public void setCritDmgPercent(double critDmgPercent) { this.critDmgPercent = critDmgPercent; }
    public double getCritDmgResPercent() { return critDmgResPercent; }
    public void setCritDmgResPercent(double critDmgResPercent) { this.critDmgResPercent = critDmgResPercent; }

    // Penetration / Ignore Def
    public double getPPenFlat() { return pPenFlat; }
    public void setPPenFlat(double pPenFlat) { this.pPenFlat = pPenFlat; }
    public double getMPenFlat() { return mPenFlat; }
    public void setMPenFlat(double mPenFlat) { this.mPenFlat = mPenFlat; }
    public double getPPenPercent() { return pPenPercent; }
    public void setPPenPercent(double pPenPercent) { this.pPenPercent = pPenPercent; }
    public double getMPenPercent() { return mPenPercent; }
    public void setMPenPercent(double mPenPercent) { this.mPenPercent = mPenPercent; }
    public double getIgnorePDefFlat() { return ignorePDefFlat; }
    public void setIgnorePDefFlat(double ignorePDefFlat) { this.ignorePDefFlat = ignorePDefFlat; }
    public double getIgnoreMDefFlat() { return ignoreMDefFlat; }
    public void setIgnoreMDefFlat(double ignoreMDefFlat) { this.ignoreMDefFlat = ignoreMDefFlat; }
    public double getIgnorePDefPercent() { return ignorePDefPercent; }
    public void setIgnorePDefPercent(double ignorePDefPercent) { this.ignorePDefPercent = ignorePDefPercent; }
    public double getIgnoreMDefPercent() { return ignoreMDefPercent; }
    public void setIgnoreMDefPercent(double ignoreMDefPercent) { this.ignoreMDefPercent = ignoreMDefPercent; }

    // Speed / Cast
    public double getASpdPercent() { return aSpdPercent; }
    public void setASpdPercent(double aSpdPercent) { this.aSpdPercent = aSpdPercent; }
    public double getMSpdPercent() { return mSpdPercent; }
    public void setMSpdPercent(double mSpdPercent) { this.mSpdPercent = mSpdPercent; }
    public double getBaseMSPD() { return baseMSPD; }
    public void setBaseMSPD(double baseMSPD) { this.baseMSPD = baseMSPD; }
    public double getVarCTPercent() { return varCTPercent; }
    public void setVarCTPercent(double varCTPercent) { this.varCTPercent = varCTPercent; }
    public double getVarCTFlat() { return varCTFlat; }
    public void setVarCTFlat(double varCTFlat) { this.varCTFlat = varCTFlat; }
    public double getFixedCTPercent() { return fixedCTPercent; }
    public void setFixedCTPercent(double fixedCTPercent) { this.fixedCTPercent = fixedCTPercent; }
    public double getFixedCTFlat() { return fixedCTFlat; }
    public void setFixedCTFlat(double fixedCTFlat) { this.fixedCTFlat = fixedCTFlat; }

    // Healing / Lifesteal
    public double getHealingEffectPercent() { return healingEffectPercent; }
    public void setHealingEffectPercent(double healingEffectPercent) { this.healingEffectPercent = healingEffectPercent; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; }
    public void setHealingReceivedPercent(double healingReceivedPercent) { this.healingReceivedPercent = healingReceivedPercent; }
    public double getLifestealPPercent() { return lifestealPPercent; }
    public void setLifestealPPercent(double lifestealPPercent) { this.lifestealPPercent = lifestealPPercent; }
    public double getLifestealMPercent() { return lifestealMPercent; }
    public void setLifestealMPercent(double lifestealMPercent) { this.lifestealMPercent = lifestealMPercent; }
    public double getTrueDamageFlat() { return trueDamageFlat; }
    public void setTrueDamageFlat(double trueDamageFlat) { this.trueDamageFlat = trueDamageFlat; }

    // Final Damage
    public double getFinalDmgPercent() { return finalDmgPercent; }
    public void setFinalDmgPercent(double finalDmgPercent) { this.finalDmgPercent = finalDmgPercent; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; }
    public void setFinalDmgResPercent(double finalDmgResPercent) { this.finalDmgResPercent = finalDmgResPercent; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; }
    public void setFinalPDmgPercent(double finalPDmgPercent) { this.finalPDmgPercent = finalPDmgPercent; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; }
    public void setFinalMDmgPercent(double finalMDmgPercent) { this.finalMDmgPercent = finalMDmgPercent; }

    // PVE/PVP
    public double getPveDmgBonusPercent() { return pveDmgBonusPercent; }
    public void setPveDmgBonusPercent(double pveDmgBonusPercent) { this.pveDmgBonusPercent = pveDmgBonusPercent; }
    public double getPvpDmgBonusPercent() { return pvpDmgBonusPercent; }
    public void setPvpDmgBonusPercent(double pvpDmgBonusPercent) { this.pvpDmgBonusPercent = pvpDmgBonusPercent; }
    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; }
    public void setPveDmgReductionPercent(double pveDmgReductionPercent) { this.pveDmgReductionPercent = pveDmgReductionPercent; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; }
    public void setPvpDmgReductionPercent(double pvpDmgReductionPercent) { this.pvpDmgReductionPercent = pvpDmgReductionPercent; }

    // Max HP/SP %
    public double getMaxHPPercent() { return maxHPPercent; }
    public void setMaxHPPercent(double maxHPPercent) { this.maxHPPercent = maxHPPercent; }
    public double getMaxSPPercent() { return maxSPPercent; }
    public void setMaxSPPercent(double maxSPPercent) { this.maxSPPercent = maxSPPercent; }

    // Shield
    public double getShieldValueFlat() { return shieldValueFlat; }
    public void setShieldValueFlat(double shieldValueFlat) { this.shieldValueFlat = shieldValueFlat; }
    public double getShieldRatePercent() { return shieldRatePercent; }
    public void setShieldRatePercent(double shieldRatePercent) { this.shieldRatePercent = shieldRatePercent; }

    // Hit/Flee
    public double getHitBonusFlat() { return hitBonusFlat; }
    public void setHitBonusFlat(double hitBonusFlat) { this.hitBonusFlat = hitBonusFlat; }
    public double getFleeBonusFlat() { return fleeBonusFlat; }
    public void setFleeBonusFlat(double fleeBonusFlat) { this.fleeBonusFlat = fleeBonusFlat; }

    // --- Core Methods (Existing logic, shortened for space) ---

    // Formula A.1 (Max HP) - Corrected (Updated to include Gear Bonus)
    public double getMaxHP() {
        int vit = getStat("VIT") + getPendingStat("VIT") + getVITBonusGear(); // Base Stat + Pending Stat + Gear Bonus
        int baseLevel = getBaseLevel();

        // MaxHP = (BaseHP + BaseHP × VIT × 0.01) × (1 + MaxHP% / 100)
        // Assume BaseHP = 18 + baseLevel * 2.0
        double baseHealth = 18 + (baseLevel * 2.0);
        double vitMultiplier = 1.0 + (vit * 0.01);

        double finalMaxHealth = baseHealth * vitMultiplier;
        return Math.floor(finalMaxHealth * (1 + getMaxHPPercent() / 100.0));
    }

    // Formula A.2 (Max SP) - Corrected (Updated to include Gear Bonus)
    public double getMaxSP() {
        int intel = getStat("INT") + getPendingStat("INT") + getINTBonusGear();
        int baseLevel = getBaseLevel();

        // MaxSP = (BaseSP + BaseSP × INT × 0.01) × (1 + MaxSP% / 100)
        // Assume BaseSP = 20 + baseLevel * 3.0
        double baseSP = 20.0 + (baseLevel * 3.0);
        double intMultiplier = 1.0 + (intel * 0.01);

        double finalMaxSP = baseSP * intMultiplier;
        return Math.floor(finalMaxSP * (1 + getMaxSPPercent() / 100.0));
    }

    // NEW: HP Recovery Formula (Updated to include Gear Bonus)
    // HP Recovery = (BaseHPRecovery + VIT × 0.2) × (1 + HealingReceive% / 100)
    public double getHPRegen() {
        int vit = getStat("VIT") + getVITBonusGear(); // Base Stat + Gear Bonus
        double baseHPRecovery = 1.0; // Assume BaseHPRecovery = 1.0

        double baseRegen = baseHPRecovery + (vit * 0.2);
        return baseRegen * (1 + getHealingReceivedPercent() / 100.0);
    }

    // SP Regen - Corrected to include HealingReceive% and exclude maxSpBonus (Updated to include Gear Bonus)
    public void calculateMaxSP() { if (this.currentSP > getMaxSP()) this.currentSP = getMaxSP(); }
    public void regenSP() { double max = getMaxSP();
        if (this.currentSP < max) {
            int intel = getStat("INT") + getINTBonusGear(); // Base Stat + Gear Bonus

            // SP Recovery = (BaseSPRecovery + INT × small_bonus) * (1 + HealingReceive% / 100)
            double baseRegen = 1.0;
            double intelBonus = intel / plugin.getConfig().getDouble("sp-regen.regen-int-divisor", 6.0);

            double regen = baseRegen + intelBonus; // Removed maxSpBonus
            regen *= (1 + getHealingReceivedPercent() / 100.0); // Apply HealingReceive%

            this.currentSP += regen;
            if (this.currentSP > max) this.currentSP = max; }
    }
    public void resetStats() { stats.put("STR", 1); stats.put("AGI", 1); stats.put("VIT", 1);
        stats.put("INT", 1); stats.put("DEX", 1); stats.put("LUK", 1);
        int totalPoints = 0; for (int i = 1; i < baseLevel; i++) totalPoints += getStatPointsGain(i);
        this.statPoints = totalPoints; clearAllPendingStats(); calculateMaxSP(); this.currentSP = getMaxSP(); }

    public long getBaseExp() { return baseExp; }
    public void setBaseExp(long baseExp) { this.baseExp = baseExp; }
    public long getBaseExpReq() { return getBaseExpReq(baseLevel); }
    public long getJobExpReq() { return getJobExpReq(jobLevel); }
    private long customRoundExp(double rawExp) {
        if (rawExp < 1) return 1L; // Minimum EXP required is 1

        long roundedExp;
        // Calculate number of digits in the integer part (1-99, 100-99999, 100000+)
        int digits = (int) Math.log10(rawExp) + 1;

        if (digits <= 2) {
            // 1-2 digits (1-99): Round up the decimal point (ceiling)
            roundedExp = (long) Math.ceil(rawExp);
        } else if (digits <= 5) {
            // 3-5 digits (100-99,999): Round up to the next multiple of 10
            double divisionFactor = 10.0;
            roundedExp = (long) (Math.ceil(rawExp / divisionFactor) * divisionFactor);
        } else {
            // 6+ digits (100,000+): Round up to the next multiple of 100
            double divisionFactor = 100.0;
            roundedExp = (long) (Math.ceil(rawExp / divisionFactor) * divisionFactor);
        }

        // Ensure minimum of 1
        return Math.max(1L, roundedExp);
    }

    // MODIFIED: Base Exp Req (ใช้สูตร A * level^B * (1 + WLF) โดย WLF ถูกตั้งค่าไว้ที่ 0.0)
    private long getBaseExpReq(int level) {
        double A = plugin.getConfig().getDouble("exp-formula.base-exp-multiplier", 0.06663935073413368);
        double B = plugin.getConfig().getDouble("exp-formula.exp-exponent", 4.707041855905981);
        double WLF = plugin.getConfig().getDouble("exp-formula.world-level-factor", 0.0);

        // rawExp = A * level^B * (1 + WLF)
        double rawExp = A * Math.pow(level, B) * (1 + WLF);
        return customRoundExp(rawExp);
    }

    // MODIFIED: Job Exp Req (ใช้สูตร A * level^B * (1 + WLF) โดย WLF ถูกตั้งค่าไว้ที่ 0.0)
    private long getJobExpReq(int level) {
        double A = plugin.getConfig().getDouble("exp-formula.job-exp-multiplier", 0.06663935073413368); // Use same A for job
        double B = plugin.getConfig().getDouble("exp-formula.exp-exponent", 4.707041855905981);
        double WLF = plugin.getConfig().getDouble("exp-formula.world-level-factor", 0.0);

        // rawExp = A * level^B * (1 + WLF)
        double rawExp = A * Math.pow(level, B) * (1 + WLF);
        return customRoundExp(rawExp);
    }

    // NEW Helper: Get the actual Max Base Level
    public int getMaxBaseLevel() {
        int worldLevelBase = plugin.getConfig().getInt("exp-formula.max-level-world-base", 92);
        return worldLevelBase + 8;
    }

    // NEW Helper: Get the actual Max Job Level
    public int getMaxJobLevel() {
        return plugin.getConfig().getInt("exp-formula.max-job-level", 10);
    }


    // Renamed for clarity, kept functionality for compatibility
    private int getStatPointsGain(int level) {
        int threshold = plugin.getConfig().getInt("stat-points-gain.level-50-threshold", 50);
        int low = plugin.getConfig().getInt("stat-points-gain.points-low-level", 5);
        int high = plugin.getConfig().getInt("stat-points-gain.points-high-level", 8);
        return (level <= threshold) ? low : high;
    }
    private double getExpBonusMultiplier(int baseLevel) {
        int worldLevel = plugin.getConfig().getInt("exp-formula.max-level-world-base", 92);

        int levelDifference = worldLevel - baseLevel;

        if (levelDifference >= 30) {
            // base lv < 30 ของเวลโลก -> bonus+300% = multiplier 4.0
            return 4.0;
        } else if (levelDifference >= 20) {
            // base lv < 20 ของเวลโลก -> bonus+200% = multiplier 3.0
            return 3.0;
        } else if (levelDifference >= 10) {
            // base lv < 10 ของเวลโลก -> bonus+100% = multiplier 2.0
            return 2.0;
        } else if (levelDifference >= 1) {
            // base lv < 1 ของเวลโลก -> bonus+50% = multiplier 1.5
            return 1.5;
        } else {
            // base lv >= เวลโลก -> bonus + 0% = multiplier 1.0
            return 1.0;
        }
    }

    // MODIFIED: Base EXP gain logic (Uses higher offset 0.5 for stacking and applies new bonus)
    public void addBaseExp(long amount, UUID playerUUID) {
        // NEW: Max Base Level Cap check using dedicated config
        int maxBaseLevel = getMaxBaseLevel(); // ใช้ Helper method

        // Apply EXP Bonus Multiplier based on World Level difference
        double expMultiplier = getExpBonusMultiplier(this.baseLevel);
        amount = (long) Math.floor(amount * expMultiplier);

        long expGained = amount;
        plugin.showFloatingText(playerUUID, "§b+" + expGained + " Base EXP", 0.5); // Offset 0.5 (Higher)

        this.baseExp += amount;

        // Only level up if current level is less than the max level
        while (this.baseLevel < maxBaseLevel && this.baseExp >= getBaseExpReq(this.baseLevel)) { // ใช้ getBaseExpReq

            this.baseExp -= getBaseExpReq(this.baseLevel);
            this.baseLevel++;
            this.statPoints += getStatPointsGain(this.baseLevel);
            // Floating text for Level Up (Base above Job)
            plugin.showFloatingText(playerUUID, "§6LEVEL UP! §fLv " + this.baseLevel, 0.5); // Offset 0.5 (Higher)
        }

        // If we are at the max level after processing EXP, notify the player.
        if (this.baseLevel >= maxBaseLevel) {
            plugin.showFloatingText(playerUUID, "§cMAX BASE LEVEL REACHED!", 0.5);
        }

        // NEW: Update Base EXP Boss Bar
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            plugin.getManaManager().updateBaseExpBar(player);
        }

        calculateMaxSP();
    }

    // MODIFIED: Job EXP gain logic (Uses lower offset 0.0 for stacking and applies new bonus)
    public void addJobExp(long amount, UUID playerUUID) {
        // NEW: Max Job Level Cap check using dedicated config
        int maxJobLevel = getMaxJobLevel(); // ใช้ Helper method

        // Apply EXP Bonus Multiplier based on World Level difference
        double expMultiplier = getExpBonusMultiplier(this.baseLevel);
        amount = (long) Math.floor(amount * expMultiplier);

        long expGained = amount;
        plugin.showFloatingText(playerUUID, "§e+" + expGained + " Job EXP", 0.0); // Offset 0.0 (Lower)

        this.jobExp += amount;

        // Only level up if current level is less than the max job level
        while (this.jobLevel < maxJobLevel && this.jobExp >= getJobExpReq(this.jobLevel)) {

            this.jobExp -= getJobExpReq(this.jobLevel);
            this.jobLevel++;
            this.skillPoints += 1; // Assume 1 Skill Point per Job Level for now
            // Floating text for Job Level Up (Job below Base)
            plugin.showFloatingText(playerUUID, "§eJOB LEVEL UP! §fJob Lv " + this.jobLevel, 0.0); // Offset 0.0 (Lower)
        }

        // If we are at the max job level after processing EXP, notify the player.
        if (this.jobLevel >= maxJobLevel) {
            plugin.showFloatingText(playerUUID, "§cMAX JOB LEVEL REACHED!", 0.0);
        }

        // NEW: Update Job EXP Boss Bar
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            plugin.getManaManager().updateJobExpBar(player);
        }
    }
}