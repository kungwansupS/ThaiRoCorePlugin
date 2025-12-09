package org.rostats.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.effect.ActiveEffect;
import org.rostats.engine.effect.EffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PlayerData {
    private int baseLevel = 1;
    private long baseExp = 0;
    private int jobLevel = 1;
    private long jobExp = 0;
    private int statPoints = 0;
    private int skillPoints = 0;
    private double currentSP = 20;
    private int resetCount = 0;

    private final Map<String, Integer> stats = new HashMap<>();
    private final Map<String, Integer> pendingStats = new HashMap<>();

    private final Map<String, Long> skillCooldowns = new ConcurrentHashMap<>();

    private final List<ActiveEffect> activeEffects = new CopyOnWriteArrayList<>();

    // Gear Bonuses & Advanced Stats
    private int strBonusGear = 0;
    private int agiBonusGear = 0;
    private int vitBonusGear = 0;
    private int intBonusGear = 0;
    private int dexBonusGear = 0;
    private int lukBonusGear = 0;

    private double weaponPAtk = 0.0;
    private double weaponMAtk = 0.0;
    private double pAtkBonusFlat = 0.0;
    private double mAtkBonusFlat = 0.0;

    private double pDmgBonusPercent = 0.0;
    private double mDmgBonusPercent = 0.0;
    private double pDmgBonusFlat = 0.0;
    private double mDmgBonusFlat = 0.0;

    private double critDmgPercent = 50.0;
    private double critDmgResPercent = 0.0;
    private double critRes = 0.0;

    private double pPenFlat = 0.0;
    private double mPenFlat = 0.0;
    private double pPenPercent = 0.0;
    private double mPenPercent = 0.0;

    private double finalDmgPercent = 0.0;
    private double finalDmgResPercent = 0.0;
    private double finalPDmgPercent = 0.0;
    private double finalMDmgPercent = 0.0;

    private double pveDmgBonusPercent = 0.0;
    private double pvpDmgBonusPercent = 0.0;
    private double pveDmgReductionPercent = 0.0;
    private double pvpDmgReductionPercent = 0.0;

    private double maxHPPercent = 0.0;
    private double maxSPPercent = 0.0;

    private double shieldValueFlat = 0.0;
    private double shieldRatePercent = 0.0;

    private double aSpdPercent = 0.0;
    private double mSpdPercent = 0.0;
    private double baseMSPD = 0.1;

    // Casting Stats
    private double varCTPercent = 0.0;
    private double varCTFlat = 0.0;
    private double fixedCTPercent = 0.0;
    private double fixedCTFlat = 0.0;

    // Cooldown & Delay System
    private double skillCooldownReductionPercent = 0.0;
    private double skillCooldownReductionFlat = 0.0;

    // [NEW] After-Cast Delay (ACD) Reduction
    private double acdReductionPercent = 0.0;
    private double acdReductionFlat = 0.0;

    // [NEW] Global Cooldown (GCD) Reduction
    private double gcdReductionPercent = 0.0;
    private double gcdReductionFlat = 0.0;

    // "Global Delay" - Blocks ALL skills (Priority Lock Result)
    private long globalDelayEndTime = 0L;

    private double healingEffectPercent = 0.0;
    private double healingReceivedPercent = 0.0;

    private double lifestealPPercent = 0.0;
    private double lifestealMPercent = 0.0;

    private double hitBonusFlat = 0.0;
    private double fleeBonusFlat = 0.0;

    private double pDmgReductionPercent = 0.0;
    private double mDmgReductionPercent = 0.0;

    private double ignorePDefFlat = 0.0;
    private double ignoreMDefFlat = 0.0;
    private double ignorePDefPercent = 0.0;
    private double ignoreMDefPercent = 0.0;
    private double meleePDmgPercent = 0.0;
    private double rangePDmgPercent = 0.0;
    private double meleePDReductionPercent = 0.0;
    private double rangePDReductionPercent = 0.0;
    private double trueDamageFlat = 0.0;

    private final ThaiRoCorePlugin plugin;

    public PlayerData(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        stats.put("STR", 1); stats.put("AGI", 1); stats.put("VIT", 1);
        stats.put("INT", 1); stats.put("DEX", 1); stats.put("LUK", 1);
        calculateMaxSP();
    }

    public List<ActiveEffect> getActiveEffects() { return activeEffects; }
    public void addActiveEffect(ActiveEffect effect) { this.activeEffects.add(effect); }
    public void removeActiveEffect(ActiveEffect effect) { this.activeEffects.remove(effect); }

    public double getEffectBonus(String key) {
        double bonus = 0.0;
        for (ActiveEffect effect : activeEffects) {
            if (effect.getType() == EffectType.STAT_MODIFIER &&
                    effect.getStatKey() != null &&
                    effect.getStatKey().equalsIgnoreCase(key)) {
                bonus += effect.getPower();
            }
        }
        return bonus;
    }

    public long getSkillCooldown(String skillId) {
        return skillCooldowns.getOrDefault(skillId, 0L);
    }

    public void setSkillCooldown(String skillId, long timestamp) {
        skillCooldowns.put(skillId, timestamp);
    }

    public void resetGearBonuses() {
        this.strBonusGear = 0; this.agiBonusGear = 0; this.vitBonusGear = 0;
        this.intBonusGear = 0; this.dexBonusGear = 0; this.lukBonusGear = 0;
        this.weaponPAtk = 0; this.weaponMAtk = 0;
        this.pAtkBonusFlat = 0; this.mAtkBonusFlat = 0;
        this.pDmgBonusPercent = 0; this.mDmgBonusPercent = 0;
        this.pDmgBonusFlat = 0; this.mDmgBonusFlat = 0;
        this.critDmgPercent = 50.0;
        this.critDmgResPercent = 0; this.critRes = 0;
        this.pPenFlat = 0; this.mPenFlat = 0;
        this.pPenPercent = 0; this.mPenPercent = 0;
        this.finalDmgPercent = 0; this.finalDmgResPercent = 0;
        this.finalPDmgPercent = 0; this.finalMDmgPercent = 0;
        this.pveDmgBonusPercent = 0; this.pvpDmgBonusPercent = 0;
        this.pveDmgReductionPercent = 0; this.pvpDmgReductionPercent = 0;
        this.maxHPPercent = 0; this.maxSPPercent = 0;
        this.shieldValueFlat = 0; this.shieldRatePercent = 0;
        this.aSpdPercent = 0; this.mSpdPercent = 0; this.baseMSPD = 0.1;

        // Casting
        this.varCTPercent = 0; this.varCTFlat = 0;
        this.fixedCTPercent = 0; this.fixedCTFlat = 0;

        // Cooldowns
        this.skillCooldownReductionPercent = 0;
        this.skillCooldownReductionFlat = 0;
        this.acdReductionPercent = 0;
        this.acdReductionFlat = 0;
        this.gcdReductionPercent = 0;
        this.gcdReductionFlat = 0;

        this.healingEffectPercent = 0; this.healingReceivedPercent = 0;
        this.lifestealPPercent = 0; this.lifestealMPercent = 0;
        this.hitBonusFlat = 0; this.fleeBonusFlat = 0;
        this.pDmgReductionPercent = 0; this.mDmgReductionPercent = 0;

        this.ignorePDefFlat = 0; this.ignoreMDefFlat = 0;
        this.ignorePDefPercent = 0; this.ignoreMDefPercent = 0;
        this.meleePDmgPercent = 0; this.rangePDmgPercent = 0;
        this.meleePDReductionPercent = 0; this.rangePDReductionPercent = 0;
        this.trueDamageFlat = 0;
    }

    public void resetStats() {
        stats.put("STR", 1); stats.put("AGI", 1); stats.put("VIT", 1);
        stats.put("INT", 1); stats.put("DEX", 1); stats.put("LUK", 1);
        int totalPoints = 0;
        for (int i = 1; i < baseLevel; i++) totalPoints += getStatPointsGain(i);
        this.statPoints = totalPoints;
        clearAllPendingStats();
        calculateMaxSP();
        this.currentSP = getMaxSP();
    }

    // --- EXP Logic ---

    public void addBaseExp(long amount, UUID uuid) {
        int maxBaseLevel = getMaxBaseLevel();
        double multiplier = getExpBonusMultiplier(this.baseLevel);
        amount = (long) (amount * multiplier);
        plugin.showFloatingText(uuid, "§b+" + amount + " Base EXP", 0.5);
        this.baseExp += amount;
        while (this.baseLevel < maxBaseLevel && this.baseExp >= getBaseExpReq(this.baseLevel)) {
            this.baseExp -= getBaseExpReq(this.baseLevel);
            this.baseLevel++;
            this.statPoints += getStatPointsGain(this.baseLevel);
            plugin.showFloatingText(uuid, "§6LEVEL UP! §fLv " + this.baseLevel, 0.5);
        }
        calculateMaxSP();
        if (Bukkit.getPlayer(uuid) != null) plugin.getManaManager().updateBaseExpBar(Bukkit.getPlayer(uuid));
    }

    public void addJobExp(long amount, UUID uuid) {
        int maxJobLevel = getMaxJobLevel();
        double multiplier = getExpBonusMultiplier(this.baseLevel);
        amount = (long) (amount * multiplier);
        plugin.showFloatingText(uuid, "§e+" + amount + " Job EXP", 0.0);
        this.jobExp += amount;
        while (this.jobLevel < maxJobLevel && this.jobExp >= getJobExpReq(this.jobLevel)) {
            this.jobExp -= getJobExpReq(this.jobLevel);
            this.jobLevel++;
            this.skillPoints += 1;
            plugin.showFloatingText(uuid, "§eJOB LEVEL UP! §fJob Lv " + this.jobLevel, 0.0);
        }
        if (Bukkit.getPlayer(uuid) != null) plugin.getManaManager().updateJobExpBar(Bukkit.getPlayer(uuid));
    }

    private double getExpBonusMultiplier(int baseLevel) {
        int worldLevel = plugin.getConfig().getInt("exp-formula.max-level-world-base", 92);
        int diff = worldLevel - baseLevel;
        if (diff >= 30) return 4.0;
        if (diff >= 20) return 3.0;
        if (diff >= 10) return 2.0;
        if (diff >= 1) return 1.5;
        return 1.0;
    }

    public long getBaseExpReq(int level) {
        double A = plugin.getConfig().getDouble("exp-formula.base-exp-multiplier", 0.0666);
        double B = plugin.getConfig().getDouble("exp-formula.exp-exponent", 4.707);
        return (long) Math.max(1, Math.ceil(A * Math.pow(level, B)));
    }

    public long getBaseExpReq() { return getBaseExpReq(this.baseLevel); }

    public long getJobExpReq(int level) {
        double A = plugin.getConfig().getDouble("exp-formula.job-exp-multiplier", 0.0666);
        double B = plugin.getConfig().getDouble("exp-formula.exp-exponent", 4.707);
        return (long) Math.max(1, Math.ceil(A * Math.pow(level, B)));
    }

    public long getJobExpReq() { return getJobExpReq(this.jobLevel); }

    private int getStatPointsGain(int level) { return level <= 50 ? 5 : 8; }
    public int getMaxBaseLevel() { return plugin.getConfig().getInt("exp-formula.max-level-world-base", 92) + 8; }
    public int getMaxJobLevel() { return plugin.getConfig().getInt("exp-formula.max-job-level", 10); }

    private int getTotalEffectiveStat(String key) {
        return getStat(key) + getPendingStat(key) + switch(key) {
            case "DEX" -> getDEXBonusGear();
            case "INT" -> getINTBonusGear();
            case "VIT" -> getVITBonusGear();
            case "STR" -> getSTRBonusGear();
            case "AGI" -> getAGIBonusGear();
            case "LUK" -> getLUKBonusGear();
            default -> 0;
        } + (int)getEffectBonus(key);
    }

    public double calculateTotalCastTime(double baseVar, double skillVarRedPercent, double baseFix, double skillFixRedPercent) {
        double playerVarRedPercent = getVariableCastTimeReductionPercent();
        double totalVarRedPercent = playerVarRedPercent + skillVarRedPercent;
        double finalVar = baseVar * Math.max(0.0, 1.0 - (totalVarRedPercent / 100.0));
        double varFlatRed = getVarCTFlat() + getEffectBonus("VAR_CT_FLAT");
        finalVar = Math.max(0.0, finalVar - varFlatRed);

        double playerFixRedPercent = getFixedCTPercent() + getEffectBonus("FIXED_CT_PERCENT");
        double totalFixRedPercent = playerFixRedPercent + skillFixRedPercent;
        double finalFix = baseFix * Math.max(0.0, 1.0 - (totalFixRedPercent / 100.0));
        double fixFlatRed = getFixedCTFlat() + getEffectBonus("FIXED_CT_FLAT");
        finalFix = Math.max(0.0, finalFix - fixFlatRed);

        return finalVar + finalFix;
    }

    private double getVariableCastTimeReductionPercent() {
        int totalDex = getTotalEffectiveStat("DEX");
        int totalInt = getTotalEffectiveStat("INT");
        double statReduction = ((totalDex * 2.0) + totalInt) / 530.0 * 100.0;
        double gearReduction = getVarCTPercent() + getEffectBonus("VAR_CT_PERCENT");
        return statReduction + gearReduction;
    }

    public double getSkillCooldownReductionPercent() { return skillCooldownReductionPercent; }
    public void setSkillCooldownReductionPercent(double v) { this.skillCooldownReductionPercent = v; }

    public double getSkillCooldownReductionFlat() { return skillCooldownReductionFlat; }
    public void setSkillCooldownReductionFlat(double v) { this.skillCooldownReductionFlat = v; }

    public long getGlobalDelayEndTime() { return globalDelayEndTime; }
    public void setGlobalDelayEndTime(long timestamp) { this.globalDelayEndTime = timestamp; }

    // ACD Getters/Setters
    public double getAcdReductionPercent() { return acdReductionPercent; }
    public void setAcdReductionPercent(double v) { this.acdReductionPercent = v; }
    public double getAcdReductionFlat() { return acdReductionFlat; }
    public void setAcdReductionFlat(double v) { this.acdReductionFlat = v; }

    // [NEW] GCD Getters/Setters
    public double getGcdReductionPercent() { return gcdReductionPercent; }
    public void setGcdReductionPercent(double v) { this.gcdReductionPercent = v; }
    public double getGcdReductionFlat() { return gcdReductionFlat; }
    public void setGcdReductionFlat(double v) { this.gcdReductionFlat = v; }

    public int getSTRBonusGear() { return strBonusGear; }
    public void setSTRBonusGear(int v) { this.strBonusGear = v; }
    public int getAGIBonusGear() { return agiBonusGear; }
    public void setAGIBonusGear(int v) { this.agiBonusGear = v; }
    public int getVITBonusGear() { return vitBonusGear; }
    public void setVITBonusGear(int v) { this.vitBonusGear = v; }
    public int getINTBonusGear() { return intBonusGear; }
    public void setINTBonusGear(int v) { this.intBonusGear = v; }
    public int getDEXBonusGear() { return dexBonusGear; }
    public void setDEXBonusGear(int v) { this.dexBonusGear = v; }
    public int getLUKBonusGear() { return lukBonusGear; }
    public void setLUKBonusGear(int v) { this.lukBonusGear = v; }

    public double getWeaponPAtk() { return weaponPAtk; }
    public void setWeaponPAtk(double v) { this.weaponPAtk = v; }
    public double getWeaponMAtk() { return weaponMAtk; }
    public void setWeaponMAtk(double v) { this.weaponMAtk = v; }
    public double getPAtkBonusFlat() { return pAtkBonusFlat; }
    public void setPAtkBonusFlat(double v) { this.pAtkBonusFlat = v; }
    public double getMAtkBonusFlat() { return mAtkBonusFlat; }
    public void setMAtkBonusFlat(double v) { this.mAtkBonusFlat = v; }

    public double getPDmgBonusPercent() { return pDmgBonusPercent; }
    public void setPDmgBonusPercent(double v) { this.pDmgBonusPercent = v; }
    public double getMDmgBonusPercent() { return mDmgBonusPercent; }
    public void setMDmgBonusPercent(double v) { this.mDmgBonusPercent = v; }
    public double getPDmgBonusFlat() { return pDmgBonusFlat; }
    public void setPDmgBonusFlat(double v) { this.pDmgBonusFlat = v; }
    public double getMDmgBonusFlat() { return mDmgBonusFlat; }
    public void setMDmgBonusFlat(double v) { this.mDmgBonusFlat = v; }

    public double getCritDmgPercent() { return critDmgPercent; }
    public void setCritDmgPercent(double v) { this.critDmgPercent = v; }
    public double getCritDmgResPercent() { return critDmgResPercent; }
    public void setCritDmgResPercent(double v) { this.critDmgResPercent = v; }
    public double getCritRes() { return critRes; }
    public void setCritRes(double v) { this.critRes = v; }

    public double getPPenFlat() { return pPenFlat; }
    public void setPPenFlat(double v) { this.pPenFlat = v; }
    public double getMPenFlat() { return mPenFlat; }
    public void setMPenFlat(double v) { this.mPenFlat = v; }
    public double getPPenPercent() { return pPenPercent; }
    public void setPPenPercent(double v) { this.pPenPercent = v; }
    public double getMPenPercent() { return mPenPercent; }
    public void setMPenPercent(double v) { this.mPenPercent = v; }

    public double getFinalDmgPercent() { return finalDmgPercent; }
    public void setFinalDmgPercent(double v) { this.finalDmgPercent = v; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; }
    public void setFinalDmgResPercent(double v) { this.finalDmgResPercent = v; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; }
    public void setFinalPDmgPercent(double v) { this.finalPDmgPercent = v; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; }
    public void setFinalMDmgPercent(double v) { this.finalMDmgPercent = v; }

    public double getPveDmgBonusPercent() { return pveDmgBonusPercent; }
    public void setPveDmgBonusPercent(double v) { this.pveDmgBonusPercent = v; }
    public double getPvpDmgBonusPercent() { return pvpDmgBonusPercent; }
    public void setPvpDmgBonusPercent(double v) { this.pvpDmgBonusPercent = v; }

    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; }
    public void setPveDmgReductionPercent(double v) { this.pveDmgReductionPercent = v; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; }
    public void setPvpDmgReductionPercent(double v) { this.pvpDmgReductionPercent = v; }

    public double getMaxHPPercent() { return maxHPPercent; }
    public void setMaxHPPercent(double v) { this.maxHPPercent = v; }
    public double getMaxSPPercent() { return maxSPPercent; }
    public void setMaxSPPercent(double v) { this.maxSPPercent = v; }

    public double getShieldValueFlat() { return shieldValueFlat; }
    public void setShieldValueFlat(double v) { this.shieldValueFlat = v; }
    public double getShieldRatePercent() { return shieldRatePercent; }
    public void setShieldRatePercent(double v) { this.shieldRatePercent = v; }

    public double getASpdPercent() { return aSpdPercent; }
    public void setASpdPercent(double v) { this.aSpdPercent = v; }
    public double getMSpdPercent() { return mSpdPercent; }
    public void setMSpdPercent(double v) { this.mSpdPercent = v; }
    public double getBaseMSPD() { return baseMSPD; }
    public void setBaseMSPD(double v) { this.baseMSPD = v; }

    public double getVarCTPercent() { return varCTPercent; }
    public void setVarCTPercent(double v) { this.varCTPercent = v; }
    public double getVarCTFlat() { return varCTFlat; }
    public void setVarCTFlat(double v) { this.varCTFlat = v; }
    public double getFixedCTPercent() { return fixedCTPercent; }
    public void setFixedCTPercent(double v) { this.fixedCTPercent = v; }
    public double getFixedCTFlat() { return fixedCTFlat; }
    public void setFixedCTFlat(double v) { this.fixedCTFlat = v; }

    public double getHealingEffectPercent() { return healingEffectPercent; }
    public void setHealingEffectPercent(double v) { this.healingEffectPercent = v; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; }
    public void setHealingReceivedPercent(double v) { this.healingReceivedPercent = v; }

    public double getLifestealPPercent() { return lifestealPPercent; }
    public void setLifestealPPercent(double v) { this.lifestealPPercent = v; }
    public double getLifestealMPercent() { return lifestealMPercent; }
    public void setLifestealMPercent(double v) { this.lifestealMPercent = v; }

    public double getHitBonusFlat() { return hitBonusFlat; }
    public void setHitBonusFlat(double v) { this.hitBonusFlat = v; }
    public double getFleeBonusFlat() { return fleeBonusFlat; }
    public void setFleeBonusFlat(double v) { this.fleeBonusFlat = v; }

    public double getPDmgReductionPercent() { return pDmgReductionPercent; }
    public void setPDmgReductionPercent(double v) { this.pDmgReductionPercent = v; }
    public double getMDmgReductionPercent() { return mDmgReductionPercent; }
    public void setMDmgReductionPercent(double v) { this.mDmgReductionPercent = v; }

    public double getIgnorePDefFlat() { return ignorePDefFlat; }
    public void setIgnorePDefFlat(double v) { this.ignorePDefFlat = v; }
    public double getIgnoreMDefFlat() { return ignoreMDefFlat; }
    public void setIgnoreMDefFlat(double v) { this.ignoreMDefFlat = v; }
    public double getIgnorePDefPercent() { return ignorePDefPercent; }
    public void setIgnorePDefPercent(double v) { this.ignorePDefPercent = v; }
    public double getIgnoreMDefPercent() { return ignoreMDefPercent; }
    public void setIgnoreMDefPercent(double v) { this.ignoreMDefPercent = v; }

    public double getMeleePDmgPercent() { return meleePDmgPercent; }
    public void setMeleePDmgPercent(double v) { this.meleePDmgPercent = v; }
    public double getRangePDmgPercent() { return rangePDmgPercent; }
    public void setRangePDmgPercent(double v) { this.rangePDmgPercent = v; }
    public double getMeleePDReductionPercent() { return meleePDReductionPercent; }
    public void setMeleePDReductionPercent(double v) { this.meleePDReductionPercent = v; }
    public double getRangePDReductionPercent() { return rangePDReductionPercent; }
    public void setRangePDReductionPercent(double v) { this.rangePDReductionPercent = v; }

    public double getTrueDamageFlat() { return trueDamageFlat; }
    public void setTrueDamageFlat(double v) { this.trueDamageFlat = v; }

    public int getStat(String key) { return stats.getOrDefault(key.toUpperCase(), 1); }
    public void setStat(String key, int val) { stats.put(key.toUpperCase(), val); calculateMaxSP(); }
    public Set<String> getStatKeys() { return stats.keySet(); }
    public int getPendingStat(String key) { return pendingStats.getOrDefault(key.toUpperCase(), 0); }
    public void setPendingStat(String key, int count) { pendingStats.put(key.toUpperCase(), count); }
    public void clearAllPendingStats() {
        pendingStats.put("STR", 0); pendingStats.put("AGI", 0); pendingStats.put("VIT", 0);
        pendingStats.put("INT", 0); pendingStats.put("DEX", 0); pendingStats.put("LUK", 0);
    }

    public double getMaxHP() {
        int vit = getTotalEffectiveStat("VIT");
        double baseHealth = 18 + (baseLevel * 2.0);
        double vitMultiplier = 1.0 + (vit * 0.01);
        double finalMaxHealth = baseHealth * vitMultiplier;
        double percentBonus = getMaxHPPercent() + getEffectBonus("MAX_HP_PERCENT");
        return Math.floor(finalMaxHealth * (1 + percentBonus / 100.0));
    }

    public double getMaxSP() {
        int intel = getTotalEffectiveStat("INT");
        double baseSP = 20.0 + (baseLevel * 3.0);
        double intMultiplier = 1.0 + (intel * 0.01);
        double finalMaxSP = baseSP * intMultiplier;
        double percentBonus = getMaxSPPercent() + getEffectBonus("MAX_SP_PERCENT");
        return Math.floor(finalMaxSP * (1 + percentBonus / 100.0));
    }

    public double getHPRegen() {
        int vit = getTotalEffectiveStat("VIT");
        double baseRegen = 1.0 + (vit * 0.2);
        double healRecv = getHealingReceivedPercent() + getEffectBonus("HEAL_RECEIVED");
        return baseRegen * (1 + healRecv / 100.0);
    }

    public void calculateMaxSP() { if (this.currentSP > getMaxSP()) this.currentSP = getMaxSP(); }

    public void regenSP() {
        double max = getMaxSP();
        if (this.currentSP < max) {
            int intel = getTotalEffectiveStat("INT");
            double healRecv = getHealingReceivedPercent() + getEffectBonus("HEAL_RECEIVED");
            double regen = (1.0 + (intel / plugin.getConfig().getDouble("sp-regen.regen-int-divisor", 6.0))) * (1 + healRecv / 100.0);
            this.currentSP = Math.min(max, this.currentSP + regen);
        }
    }

    public int getBaseLevel() { return baseLevel; }
    public void setBaseLevel(int l) { this.baseLevel = l; calculateMaxSP(); }
    public long getBaseExp() { return baseExp; }
    public void setBaseExp(long e) { this.baseExp = e; }
    public int getJobLevel() { return jobLevel; }
    public void setJobLevel(int l) { this.jobLevel = l; }
    public long getJobExp() { return jobExp; }
    public void setJobExp(long e) { this.jobExp = e; }
    public int getStatPoints() { return statPoints; }
    public void setStatPoints(int p) { this.statPoints = p; }
    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int p) { this.skillPoints = p; }
    public int getResetCount() { return resetCount; }
    public void setResetCount(int c) { this.resetCount = c; }
    public void incrementResetCount() { this.resetCount++; }
    public double getCurrentSP() { return currentSP; }
    public void setCurrentSP(double s) { this.currentSP = s; }
}