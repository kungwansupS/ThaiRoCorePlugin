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
    private final ThaiRoCorePlugin plugin;
    private final UUID uuid;

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

    // Cooldown tracking - Thread-safe
    private final Map<String, Long> skillCooldowns = new ConcurrentHashMap<>();

    // [NEW] Global Cooldown tracking
    private long globalCooldownEnd = 0L;

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

    private double ignorePDefFlat = 0.0;
    private double ignorePDefPercent = 0.0;
    private double ignoreMDefFlat = 0.0;
    private double ignoreMDefPercent = 0.0;

    private double varCTPercent = 0.0;
    private double varCTFlat = 0.0;
    private double fixedCTPercent = 0.0;
    private double fixedCTFlat = 0.0;

    // [NEW] Cooldown Reduction Stats
    private double skillCooldownReductionPercent = 0.0;
    private double skillCooldownReductionFlat = 0.0;
    private double globalCooldownReductionPercent = 0.0;
    private double globalCooldownReductionFlat = 0.0;

    private double aSpdPercent = 0.0;
    private double mSpdPercent = 0.0;

    private double hitFlat = 0.0;
    private double fleeFlat = 0.0;
    private double baseMSPD = 1.0;

    private double maxSP = 50.0;

    public PlayerData(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.uuid = null;
        initStats();
        calculateMaxSP();
        this.currentSP = this.maxSP;
    }

    public PlayerData(ThaiRoCorePlugin plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        initStats();
        calculateMaxSP();
        this.currentSP = this.maxSP;
    }

    private void initStats() {
        stats.put("STR", 1);
        stats.put("AGI", 1);
        stats.put("VIT", 1);
        stats.put("INT", 1);
        stats.put("DEX", 1);
        stats.put("LUK", 1);

        pendingStats.put("STR", 0);
        pendingStats.put("AGI", 0);
        pendingStats.put("VIT", 0);
        pendingStats.put("INT", 0);
        pendingStats.put("DEX", 0);
        pendingStats.put("LUK", 0);
    }

    // --- Cooldown Management ---

    public long getSkillCooldown(String skillId) {
        return skillCooldowns.getOrDefault(skillId, 0L);
    }

    public void setSkillCooldown(String skillId, long time) {
        skillCooldowns.put(skillId, time);
    }

    public boolean isSkillOnCooldown(String skillId, double cooldownSeconds) {
        long now = System.currentTimeMillis();
        long lastUse = getSkillCooldown(skillId);
        long cooldownMillis = (long) (cooldownSeconds * 1000);
        return (now - lastUse) < cooldownMillis;
    }

    public double getRemainingSkillCooldown(String skillId, double cooldownSeconds) {
        long now = System.currentTimeMillis();
        long lastUse = getSkillCooldown(skillId);
        long cooldownMillis = (long) (cooldownSeconds * 1000);
        long remaining = cooldownMillis - (now - lastUse);
        return remaining > 0 ? remaining / 1000.0 : 0.0;
    }

    // [NEW] Global Cooldown Methods

    public long getGlobalCooldownEnd() {
        return globalCooldownEnd;
    }

    public void setGlobalCooldownEnd(long time) {
        this.globalCooldownEnd = time;
    }

    public boolean isOnGlobalCooldown() {
        return System.currentTimeMillis() < globalCooldownEnd;
    }

    public double getRemainingGlobalCooldown() {
        long remaining = globalCooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000.0 : 0.0;
    }

    public void applyGlobalCooldown(double baseGcdSeconds) {
        // Apply GCD reduction from gear/buffs
        double finalGcd = baseGcdSeconds;

        // 1. Apply % reduction
        finalGcd = finalGcd * (1.0 - (skillCooldownReductionPercent + getEffectBonus("GLOBAL_CD_PERCENT")) / 100.0);

        // 2. Apply flat reduction
        finalGcd = finalGcd - (globalCooldownReductionFlat + getEffectBonus("GLOBAL_CD_FLAT"));

        // Minimum 0.1 seconds GCD
        finalGcd = Math.max(0.1, finalGcd);

        this.globalCooldownEnd = System.currentTimeMillis() + (long)(finalGcd * 1000);
    }

    // Apply cooldown reduction to skill cooldown
    public double getFinalSkillCooldown(double baseCooldown) {
        double finalCd = baseCooldown;

        // 1. Apply % reduction
        finalCd = finalCd * (1.0 - (skillCooldownReductionPercent + getEffectBonus("SKILL_CD_PERCENT")) / 100.0);

        // 2. Apply flat reduction
        finalCd = finalCd - (skillCooldownReductionFlat + getEffectBonus("SKILL_CD_FLAT"));

        return Math.max(0, finalCd);
    }

    // --- Active Effects ---

    public void addActiveEffect(ActiveEffect effect) {
        activeEffects.add(effect);
    }

    public void removeActiveEffect(ActiveEffect effect) {
        activeEffects.remove(effect);
    }

    public List<ActiveEffect> getActiveEffects() {
        return new ArrayList<>(activeEffects);
    }

    public double getEffectBonus(String statKey) {
        double total = 0.0;
        for (ActiveEffect effect : activeEffects) {
            if (effect.getType() == EffectType.STAT_MODIFIER && statKey.equals(effect.getStatKey())) {
                total += effect.getPower();
            }
        }
        return total;
    }

    // --- Stats Management ---

    public int getStat(String stat) {
        return stats.getOrDefault(stat, 1);
    }

    public void setStat(String stat, int value) {
        stats.put(stat, value);
    }

    public int getPendingStat(String stat) {
        return pendingStats.getOrDefault(stat, 0);
    }

    public void setPendingStat(String stat, int value) {
        pendingStats.put(stat, value);
    }

    public void clearAllPendingStats() {
        for (String key : pendingStats.keySet()) {
            pendingStats.put(key, 0);
        }
    }

    public Set<String> getStatKeys() {
        return stats.keySet();
    }

    // --- Gear Bonuses ---

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

    public double getIgnorePDefFlat() { return ignorePDefFlat; }
    public void setIgnorePDefFlat(double v) { this.ignorePDefFlat = v; }

    public double getIgnorePDefPercent() { return ignorePDefPercent; }
    public void setIgnorePDefPercent(double v) { this.ignorePDefPercent = v; }

    public double getIgnoreMDefFlat() { return ignoreMDefFlat; }
    public void setIgnoreMDefFlat(double v) { this.ignoreMDefFlat = v; }

    public double getIgnoreMDefPercent() { return ignoreMDefPercent; }
    public void setIgnoreMDefPercent(double v) { this.ignoreMDefPercent = v; }

    public double getVarCTPercent() { return varCTPercent; }
    public void setVarCTPercent(double v) { this.varCTPercent = v; }

    public double getVarCTFlat() { return varCTFlat; }
    public void setVarCTFlat(double v) { this.varCTFlat = v; }

    public double getFixedCTPercent() { return fixedCTPercent; }
    public void setFixedCTPercent(double v) { this.fixedCTPercent = v; }

    public double getFixedCTFlat() { return fixedCTFlat; }
    public void setFixedCTFlat(double v) { this.fixedCTFlat = v; }

    // [NEW] Cooldown Reduction Getters/Setters
    public double getSkillCooldownReductionPercent() { return skillCooldownReductionPercent; }
    public void setSkillCooldownReductionPercent(double v) { this.skillCooldownReductionPercent = v; }

    public double getSkillCooldownReductionFlat() { return skillCooldownReductionFlat; }
    public void setSkillCooldownReductionFlat(double v) { this.skillCooldownReductionFlat = v; }

    public double getGlobalCooldownReductionPercent() { return globalCooldownReductionPercent; }
    public void setGlobalCooldownReductionPercent(double v) { this.globalCooldownReductionPercent = v; }

    public double getGlobalCooldownReductionFlat() { return globalCooldownReductionFlat; }
    public void setGlobalCooldownReductionFlat(double v) { this.globalCooldownReductionFlat = v; }

    public double getASpdPercent() { return aSpdPercent; }
    public void setASpdPercent(double v) { this.aSpdPercent = v; }

    public double getMSpdPercent() { return mSpdPercent; }
    public void setMSpdPercent(double v) { this.mSpdPercent = v; }

    public double getHitFlat() { return hitFlat; }
    public void setHitFlat(double v) { this.hitFlat = v; }

    public double getFleeFlat() { return fleeFlat; }
    public void setFleeFlat(double v) { this.fleeFlat = v; }

    public double getBaseMSPD() { return baseMSPD; }
    public void setBaseMSPD(double v) { this.baseMSPD = v; }

    // --- SP / MaxSP ---

    public double getMaxSP() {
        return maxSP;
    }

    public void calculateMaxSP() {
        int totalINT = getStat("INT") + getPendingStat("INT") + intBonusGear + (int) getEffectBonus("INT");
        int baseLevel = getBaseLevel();
        this.maxSP = 50.0 + (totalINT * 1.5) + (baseLevel * 2.0);
    }

    // --- Experience ---

    public void addBaseExp(long amount) {
        this.baseExp += amount;
        long required = getBaseExpReq(baseLevel);

        while (baseExp >= required && baseLevel < getMaxBaseLevel()) {
            baseExp -= required;
            baseLevel++;
            statPoints += getStatPointsGain(baseLevel);
            calculateMaxSP();
            this.currentSP = this.maxSP;
            required = getBaseExpReq(baseLevel);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendTitle("§6§lLEVEL UP!", "§fBase Lv " + this.baseLevel, 10, 40, 10);
            }
        }

        if (baseExp < 0) baseExp = 0;
        if (baseLevel >= getMaxBaseLevel()) baseExp = 0;

        if (Bukkit.getPlayer(uuid) != null) plugin.getManaManager().updateBaseExpBar(Bukkit.getPlayer(uuid));
    }

    public void addJobExp(long amount) {
        this.jobExp += amount;
        long required = getJobExpReq(jobLevel);

        while (jobExp >= required && jobLevel < getMaxJobLevel()) {
            jobExp -= required;
            jobLevel++;
            skillPoints++;
            required = getJobExpReq(jobLevel);

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendTitle("§d§lJOB LEVEL UP!", "§fJob Lv " + this.jobLevel, 10, 40, 10);
            }
        }

        if (jobExp < 0) jobExp = 0;
        if (jobLevel >= getMaxJobLevel()) jobExp = 0;

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

    public long getJobExpReq(int level) {
        double A = plugin.getConfig().getDouble("exp-formula.job-exp-multiplier", 0.0666);
        double B = plugin.getConfig().getDouble("exp-formula.exp-exponent", 4.707);
        return (long) Math.max(1, Math.ceil(A * Math.pow(level, B)));
    }

    private int getStatPointsGain(int level) { return level <= 50 ? 5 : 8; }
    public int getMaxBaseLevel() { return plugin.getConfig().getInt("exp-formula.max-level-world-base", 92) + 8; }
    public int getMaxJobLevel() { return plugin.getConfig().getInt("exp-formula.max-job-level", 10); }

    // --- Cast Time Calculation ---

    private double getVariableCastTimeReduction() {
        int totalDex = getStat("DEX") + getPendingStat("DEX") + dexBonusGear + (int)getEffectBonus("DEX");
        int totalInt = getStat("INT") + getPendingStat("INT") + intBonusGear + (int)getEffectBonus("INT");

        double statReduction = (totalDex / 2.0) + (totalInt / 4.0);
        double gearReduction = varCTPercent + getEffectBonus("VAR_CT_PERCENT");

        return statReduction + gearReduction;
    }

    public double getFinalCastTime(double baseCastTime) {
        if (baseCastTime <= 0) return 0;

        double reduction = getVariableCastTimeReduction();
        double multiplier = Math.max(0.0, 1.0 - (reduction / 100.0));

        double castTimeAfterVarReduction = baseCastTime * multiplier;
        double fixedCTFlatReduction = fixedCTFlat + getEffectBonus("FIXED_CT_FLAT");

        double finalCastTime = castTimeAfterVarReduction - fixedCTFlatReduction;

        return Math.max(0.0, finalCastTime);
    }

    // --- Basic Getters/Setters ---

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

    // --- Hit & Flee Getters ---
    public double getHitBonusFlat() { return hitFlat; }
    public void setHitBonusFlat(double v) { this.hitFlat = v; }

    public double getFleeBonusFlat() { return fleeFlat; }
    public void setFleeBonusFlat(double v) { this.fleeFlat = v; }
}