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

    // --- Core Stats ---
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

    // --- 1. Base Attributes (Gear Bonuses) ---
    private int strBonusGear = 0;
    private int agiBonusGear = 0;
    private int vitBonusGear = 0;
    private int intBonusGear = 0;
    private int dexBonusGear = 0;
    private int lukBonusGear = 0;

    // Derived Base
    private double maxHPFlat = 0;
    private double maxHPPercent = 0;
    private double maxSPFlat = 0;
    private double maxSPPercent = 0;
    private double hpRecovery = 0;
    private double spRecovery = 0;
    private double hitBonusFlat = 0;
    private double fleeBonusFlat = 0;

    // --- 2. Core Combat Stats ---
    private double weaponPAtk = 0;
    private double weaponMAtk = 0;
    private double refinePAtk = 0;
    private double refineMAtk = 0;
    private double pAtkBonusFlat = 0; // General P.ATK Bonus
    private double mAtkBonusFlat = 0; // General M.ATK Bonus

    private double pDefBonus = 0; // Gear P.DEF
    private double mDefBonus = 0; // Gear M.DEF
    private double refinePDef = 0;
    private double refineMDef = 0;

    // --- 3. Penetration / Ignore ---
    private double pPenFlat = 0;
    private double pPenPercent = 0;
    private double ignorePDefFlat = 0;
    private double ignorePDefPercent = 0;

    private double mPenFlat = 0;
    private double mPenPercent = 0;
    private double ignoreMDefFlat = 0;
    private double ignoreMDefPercent = 0;

    // --- 4. Casting ---
    private double varCTPercent = 0;
    private double varCTFlat = 0;
    private double fixedCTPercent = 0;
    private double fixedCTFlat = 0;

    // --- 5. Cooldown / Delay / Motion ---
    private double skillCDPercent = 0;
    private double skillCDFlat = 0;
    private double finalCDPercent = 0;
    private double globalCDPercent = 0;
    private double afterCastDelayPercent = 0;
    private double afterCastDelayFlat = 0;
    private double preMotion = 0;
    private double postMotion = 0;
    private double cancelMotion = 0;

    // --- 6. Speed & Mobility ---
    private double aSpdPercent = 0;
    private double mSpdPercent = 0;
    private double baseMSPD = 0.1;
    private double atkIntervalPercent = 0;

    // --- 7. Critical ---
    private double crit = 0;
    private double critDmgPercent = 50.0; // Base 50%
    private double finalCritDmgPercent = 0;
    private double perfectHit = 0;

    private double critRes = 0;
    private double critDmgResPercent = 0;
    private double perfectDodge = 0;

    // --- 8. Universal Damage Modifiers ---
    private double pDmgPercent = 0;
    private double pDmgFlat = 0;
    private double pDmgReductionPercent = 0;

    private double mDmgPercent = 0;
    private double mDmgFlat = 0;
    private double mDmgReductionPercent = 0;

    private double trueDamageFlat = 0;
    private double finalDmgPercent = 0;
    private double finalDmgResPercent = 0;

    private double finalPDmgPercent = 0; // Legacy / Specific
    private double finalMDmgPercent = 0; // Legacy / Specific

    // --- 9. Distance ---
    private double meleePDmgPercent = 0;
    private double meleePDReductionPercent = 0;
    private double rangePDmgPercent = 0;
    private double rangePDReductionPercent = 0;

    // --- 10. Content ---
    private double pveDmgPercent = 0;
    private double pveDmgReductionPercent = 0;
    private double pvpDmgPercent = 0;
    private double pvpDmgReductionPercent = 0;

    // --- 11. Healing ---
    private double healingEffectPercent = 0;
    private double healingFlat = 0;
    private double healingReceivedPercent = 0;
    private double healingReceivedFlat = 0;
    private double lifestealPPercent = 0;
    private double lifestealMPercent = 0;

    // --- Misc ---
    private double shieldValueFlat = 0;
    private double shieldRatePercent = 0;


    public PlayerData(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        stats.put("STR", 1); stats.put("AGI", 1); stats.put("VIT", 1);
        stats.put("INT", 1); stats.put("DEX", 1); stats.put("LUK", 1);
        calculateMaxSP();
    }

    // --- Reset Logic ---
    public void resetGearBonuses() {
        this.strBonusGear = 0; this.agiBonusGear = 0; this.vitBonusGear = 0;
        this.intBonusGear = 0; this.dexBonusGear = 0; this.lukBonusGear = 0;

        this.maxHPFlat = 0; this.maxHPPercent = 0;
        this.maxSPFlat = 0; this.maxSPPercent = 0;
        this.hpRecovery = 0; this.spRecovery = 0;
        this.hitBonusFlat = 0; this.fleeBonusFlat = 0;

        this.weaponPAtk = 0; this.weaponMAtk = 0;
        this.refinePAtk = 0; this.refineMAtk = 0;
        this.pAtkBonusFlat = 0; this.mAtkBonusFlat = 0;
        this.pDefBonus = 0; this.mDefBonus = 0;
        this.refinePDef = 0; this.refineMDef = 0;

        this.pPenFlat = 0; this.pPenPercent = 0;
        this.ignorePDefFlat = 0; this.ignorePDefPercent = 0;
        this.mPenFlat = 0; this.mPenPercent = 0;
        this.ignoreMDefFlat = 0; this.ignoreMDefPercent = 0;

        this.varCTPercent = 0; this.varCTFlat = 0;
        this.fixedCTPercent = 0; this.fixedCTFlat = 0;

        this.skillCDPercent = 0; this.skillCDFlat = 0;
        this.finalCDPercent = 0; this.globalCDPercent = 0;
        this.afterCastDelayPercent = 0; this.afterCastDelayFlat = 0;
        this.preMotion = 0; this.postMotion = 0; this.cancelMotion = 0;

        this.aSpdPercent = 0; this.mSpdPercent = 0; this.baseMSPD = 0.1;
        this.atkIntervalPercent = 0;

        this.crit = 0; this.critDmgPercent = 50.0;
        this.finalCritDmgPercent = 0; this.perfectHit = 0;
        this.critRes = 0; this.critDmgResPercent = 0; this.perfectDodge = 0;

        this.pDmgPercent = 0; this.pDmgFlat = 0; this.pDmgReductionPercent = 0;
        this.mDmgPercent = 0; this.mDmgFlat = 0; this.mDmgReductionPercent = 0;
        this.trueDamageFlat = 0;
        this.finalDmgPercent = 0; this.finalDmgResPercent = 0;
        this.finalPDmgPercent = 0; this.finalMDmgPercent = 0;

        this.meleePDmgPercent = 0; this.meleePDReductionPercent = 0;
        this.rangePDmgPercent = 0; this.rangePDReductionPercent = 0;

        this.pveDmgPercent = 0; this.pveDmgReductionPercent = 0;
        this.pvpDmgPercent = 0; this.pvpDmgReductionPercent = 0;

        this.healingEffectPercent = 0; this.healingFlat = 0;
        this.healingReceivedPercent = 0; this.healingReceivedFlat = 0;
        this.lifestealPPercent = 0; this.lifestealMPercent = 0;

        this.shieldValueFlat = 0; this.shieldRatePercent = 0;
    }

    public void calculateFinalStats() {
        // Method placeholder for triggering any complex re-calculations.
        calculateMaxSP();
    }

    // --- Adder Methods (For AttributeHandler) ---
    public void addStr(int v) { this.strBonusGear += v; }
    public void addAgi(int v) { this.agiBonusGear += v; }
    public void addVit(int v) { this.vitBonusGear += v; }
    public void addInt(int v) { this.intBonusGear += v; }
    public void addDex(int v) { this.dexBonusGear += v; }
    public void addLuk(int v) { this.lukBonusGear += v; }

    public void addMaxHpFlat(double v) { this.maxHPFlat += v; }
    public void addMaxHpPercent(double v) { this.maxHPPercent += v; }
    public void addMaxSpFlat(double v) { this.maxSPFlat += v; }
    public void addMaxSpPercent(double v) { this.maxSPPercent += v; }
    public void addHpRecovery(double v) { this.hpRecovery += v; }
    public void addSpRecovery(double v) { this.spRecovery += v; }
    public void addHit(double v) { this.hitBonusFlat += v; }
    public void addFlee(double v) { this.fleeBonusFlat += v; }

    public void addWeaponPAtk(double v) { this.weaponPAtk += v; }
    public void addWeaponMAtk(double v) { this.weaponMAtk += v; }
    public void addRefinePAtk(double v) { this.refinePAtk += v; }
    public void addRefineMAtk(double v) { this.refineMAtk += v; }
    public void addPAtkBonusFlat(double v) { this.pAtkBonusFlat += v; }
    public void addMAtkBonusFlat(double v) { this.mAtkBonusFlat += v; }

    public void addPDef(double v) { this.pDefBonus += v; }
    public void addMDef(double v) { this.mDefBonus += v; }
    public void addRefinePDef(double v) { this.refinePDef += v; }
    public void addRefineMDef(double v) { this.refineMDef += v; }

    public void addPPenFlat(double v) { this.pPenFlat += v; }
    public void addPPenPercent(double v) { this.pPenPercent += v; }
    public void addIgnorePDefFlat(double v) { this.ignorePDefFlat += v; }
    public void addIgnorePDefPercent(double v) { this.ignorePDefPercent += v; }

    public void addMPenFlat(double v) { this.mPenFlat += v; }
    public void addMPenPercent(double v) { this.mPenPercent += v; }
    public void addIgnoreMDefFlat(double v) { this.ignoreMDefFlat += v; }
    public void addIgnoreMDefPercent(double v) { this.ignoreMDefPercent += v; }

    public void addVarCastPercent(double v) { this.varCTPercent += v; }
    public void addVarCastFlat(double v) { this.varCTFlat += v; }
    public void addFixedCastPercent(double v) { this.fixedCTPercent += v; }
    public void addFixedCastFlat(double v) { this.fixedCTFlat += v; }

    public void addSkillCDPercent(double v) { this.skillCDPercent += v; }
    public void addSkillCDFlat(double v) { this.skillCDFlat += v; }
    public void addFinalCDPercent(double v) { this.finalCDPercent += v; }
    public void addGlobalCDPercent(double v) { this.globalCDPercent += v; }
    public void addAfterCastDelayPercent(double v) { this.afterCastDelayPercent += v; }
    public void addAfterCastDelayFlat(double v) { this.afterCastDelayFlat += v; }
    public void addPreMotion(double v) { this.preMotion += v; }
    public void addPostMotion(double v) { this.postMotion += v; }
    public void addCancelMotion(double v) { this.cancelMotion += v; }

    public void addASpdPercent(double v) { this.aSpdPercent += v; }
    public void addMSpdPercent(double v) { this.mSpdPercent += v; }
    public void addBaseMSPD(double v) { this.baseMSPD += v; }
    public void addAtkIntervalPercent(double v) { this.atkIntervalPercent += v; }

    public void addCrit(double v) { this.crit += v; }
    public void addCritDmgPercent(double v) { this.critDmgPercent += v; }
    public void addFinalCritDmgPercent(double v) { this.finalCritDmgPercent += v; }
    public void addPerfectHit(double v) { this.perfectHit += v; }
    public void addCritRes(double v) { this.critRes += v; }
    public void addCritDmgResPercent(double v) { this.critDmgResPercent += v; }
    public void addPerfectDodge(double v) { this.perfectDodge += v; }

    public void addPDmgPercent(double v) { this.pDmgPercent += v; }
    public void addPDmgFlat(double v) { this.pDmgFlat += v; }
    public void addPDmgReductionPercent(double v) { this.pDmgReductionPercent += v; }
    public void addMDmgPercent(double v) { this.mDmgPercent += v; }
    public void addMDmgFlat(double v) { this.mDmgFlat += v; }
    public void addMDmgReductionPercent(double v) { this.mDmgReductionPercent += v; }
    public void addTrueDamageFlat(double v) { this.trueDamageFlat += v; }
    public void addFinalDmgPercent(double v) { this.finalDmgPercent += v; }
    public void addFinalDmgResPercent(double v) { this.finalDmgResPercent += v; }
    public void addFinalPDmgPercent(double v) { this.finalPDmgPercent += v; }
    public void addFinalMDmgPercent(double v) { this.finalMDmgPercent += v; }

    public void addMeleePDmgPercent(double v) { this.meleePDmgPercent += v; }
    public void addMeleePDReductionPercent(double v) { this.meleePDReductionPercent += v; }
    public void addRangePDmgPercent(double v) { this.rangePDmgPercent += v; }
    public void addRangePDReductionPercent(double v) { this.rangePDReductionPercent += v; }

    public void addPveDmgPercent(double v) { this.pveDmgPercent += v; }
    public void addPveDmgReductionPercent(double v) { this.pveDmgReductionPercent += v; }
    public void addPvpDmgPercent(double v) { this.pvpDmgPercent += v; }
    public void addPvpDmgReductionPercent(double v) { this.pvpDmgReductionPercent += v; }

    public void addHealingEffectPercent(double v) { this.healingEffectPercent += v; }
    public void addHealingFlat(double v) { this.healingFlat += v; }
    public void addHealingReceivedPercent(double v) { this.healingReceivedPercent += v; }
    public void addHealingReceivedFlat(double v) { this.healingReceivedFlat += v; }
    public void addLifestealPPercent(double v) { this.lifestealPPercent += v; }
    public void addLifestealMPercent(double v) { this.lifestealMPercent += v; }

    public void addShieldValueFlat(double v) { this.shieldValueFlat += v; }
    public void addShieldRatePercent(double v) { this.shieldRatePercent += v; }


    // --- Core Logic & Getters ---
    public int getStat(String key) { return stats.getOrDefault(key.toUpperCase(), 1); }
    public void setStat(String key, int val) { stats.put(key.toUpperCase(), val); calculateMaxSP(); }
    public Set<String> getStatKeys() { return stats.keySet(); }
    public int getPendingStat(String key) { return pendingStats.getOrDefault(key.toUpperCase(), 0); }
    public void setPendingStat(String key, int count) { pendingStats.put(key.toUpperCase(), count); }
    public void clearAllPendingStats() {
        pendingStats.put("STR", 0); pendingStats.put("AGI", 0); pendingStats.put("VIT", 0);
        pendingStats.put("INT", 0); pendingStats.put("DEX", 0); pendingStats.put("LUK", 0);
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

    public double getMaxHP() {
        int vit = getStat("VIT") + getPendingStat("VIT") + vitBonusGear + (int)getEffectBonus("VIT");
        double baseHealth = 18 + (baseLevel * 2.0) + maxHPFlat;
        double vitMultiplier = 1.0 + (vit * 0.01);
        double finalMaxHealth = baseHealth * vitMultiplier;
        double percentBonus = maxHPPercent + getEffectBonus("MAX_HP_PERCENT");
        return Math.floor(finalMaxHealth * (1 + percentBonus / 100.0));
    }

    public double getMaxSP() {
        int intel = getStat("INT") + getPendingStat("INT") + intBonusGear + (int)getEffectBonus("INT");
        double baseSP = 20.0 + (baseLevel * 3.0) + maxSPFlat;
        double intMultiplier = 1.0 + (intel * 0.01);
        double finalMaxSP = baseSP * intMultiplier;
        double percentBonus = maxSPPercent + getEffectBonus("MAX_SP_PERCENT");
        return Math.floor(finalMaxSP * (1 + percentBonus / 100.0));
    }

    public void calculateMaxSP() { if (this.currentSP > getMaxSP()) this.currentSP = getMaxSP(); }

    public double getHPRegen() {
        int vit = getStat("VIT") + vitBonusGear + (int)getEffectBonus("VIT");
        double baseRegen = 1.0 + (vit * 0.2) + hpRecovery;
        double healRecv = healingReceivedPercent + getEffectBonus("HEAL_RECEIVED");
        return baseRegen * (1 + healRecv / 100.0);
    }

    public void regenSP() {
        double max = getMaxSP();
        if (this.currentSP < max) {
            int intel = getStat("INT") + intBonusGear + (int)getEffectBonus("INT");
            double healRecv = healingReceivedPercent + getEffectBonus("HEAL_RECEIVED");
            double regen = (1.0 + (intel / plugin.getConfig().getDouble("sp-regen.regen-int-divisor", 6.0)) + spRecovery) * (1 + healRecv / 100.0);
            this.currentSP = Math.min(max, this.currentSP + regen);
        }
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

    // EXP Logic
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

    public long getJobExpReq(int level) {
        double A = plugin.getConfig().getDouble("exp-formula.job-exp-multiplier", 0.0666);
        double B = plugin.getConfig().getDouble("exp-formula.exp-exponent", 4.707);
        return (long) Math.max(1, Math.ceil(A * Math.pow(level, B)));
    }

    private int getStatPointsGain(int level) { return level <= 50 ? 5 : 8; }
    public int getMaxBaseLevel() { return plugin.getConfig().getInt("exp-formula.max-level-world-base", 92) + 8; }
    public int getMaxJobLevel() { return plugin.getConfig().getInt("exp-formula.max-job-level", 10); }

    // --- Getters for Handlers/PAPI ---
    public int getSTRBonusGear() { return strBonusGear; }
    public int getAGIBonusGear() { return agiBonusGear; }
    public int getVITBonusGear() { return vitBonusGear; }
    public int getINTBonusGear() { return intBonusGear; }
    public int getDEXBonusGear() { return dexBonusGear; }
    public int getLUKBonusGear() { return lukBonusGear; }

    public double getWeaponPAtk() { return weaponPAtk; }
    public double getWeaponMAtk() { return weaponMAtk; }
    public double getPAtkBonusFlat() { return pAtkBonusFlat; }
    public double getMAtkBonusFlat() { return mAtkBonusFlat; }
    public double getRefinePAtk() { return refinePAtk; }
    public double getRefineMAtk() { return refineMAtk; }

    public double getPDmgBonusPercent() { return pDmgPercent; }
    public double getMDmgBonusPercent() { return mDmgPercent; }
    public double getPDmgBonusFlat() { return pDmgFlat; }
    public double getMDmgBonusFlat() { return mDmgFlat; }

    public double getPDefBonus() { return pDefBonus; }
    public double getMDefBonus() { return mDefBonus; }
    public double getRefinePDef() { return refinePDef; }
    public double getRefineMDef() { return refineMDef; }

    public double getCritDmgPercent() { return critDmgPercent; }
    public double getCritDmgResPercent() { return critDmgResPercent; }
    public double getCritRes() { return critRes; }
    public double getCrit() { return crit; }

    public double getPPenFlat() { return pPenFlat; }
    public double getMPenFlat() { return mPenFlat; }
    public double getPPenPercent() { return pPenPercent; }
    public double getMPenPercent() { return mPenPercent; }
    public double getIgnorePDefPercent() { return ignorePDefPercent; }
    public double getIgnorePDefFlat() { return ignorePDefFlat; }
    public double getIgnoreMDefPercent() { return ignoreMDefPercent; }
    public double getIgnoreMDefFlat() { return ignoreMDefFlat; }

    public double getFinalDmgPercent() { return finalDmgPercent; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; }

    public double getPveDmgBonusPercent() { return pveDmgPercent; }
    public double getPvpDmgBonusPercent() { return pvpDmgPercent; }

    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; }

    public double getMaxHPPercent() { return maxHPPercent; }
    public double getMaxSPPercent() { return maxSPPercent; }

    public double getShieldValueFlat() { return shieldValueFlat; }
    public double getShieldRatePercent() { return shieldRatePercent; }

    public double getASpdPercent() { return aSpdPercent; }
    public double getMSpdPercent() { return mSpdPercent; }
    public double getBaseMSPD() { return baseMSPD; }
    public double getAtkIntervalPercent() { return atkIntervalPercent; }

    public double getVarCTPercent() { return varCTPercent; }
    public double getVarCTFlat() { return varCTFlat; }
    public double getFixedCTPercent() { return fixedCTPercent; }
    public double getFixedCTFlat() { return fixedCTFlat; }

    public double getSkillCDPercent() { return skillCDPercent; }
    public double getSkillCDFlat() { return skillCDFlat; }
    public double getFinalCDPercent() { return finalCDPercent; }
    public double getGlobalCDPercent() { return globalCDPercent; }
    public double getAfterCastDelayPercent() { return afterCastDelayPercent; }
    public double getAfterCastDelayFlat() { return afterCastDelayFlat; }
    public double getPreMotion() { return preMotion; }
    public double getPostMotion() { return postMotion; }
    public double getCancelMotion() { return cancelMotion; }

    public double getHealingEffectPercent() { return healingEffectPercent; }
    public double getHealingFlat() { return healingFlat; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; }
    public double getHealingReceivedFlat() { return healingReceivedFlat; }
    public double getLifestealPPercent() { return lifestealPPercent; }
    public double getLifestealMPercent() { return lifestealMPercent; }

    public double getHitBonusFlat() { return hitBonusFlat; }
    public double getFleeBonusFlat() { return fleeBonusFlat; }

    public double getPDmgReductionPercent() { return pDmgReductionPercent; }
    public double getMDmgReductionPercent() { return mDmgReductionPercent; }

    public double getMeleePDmgPercent() { return meleePDmgPercent; }
    public double getMeleePDReductionPercent() { return meleePDReductionPercent; }
    public double getRangePDmgPercent() { return rangePDmgPercent; }
    public double getRangePDReductionPercent() { return rangePDReductionPercent; }

    public double getTrueDamageFlat() { return trueDamageFlat; }
    public double getFinalCritDmgPercent() { return finalCritDmgPercent; }
    public double getPerfectHit() { return perfectHit; }
    public double getPerfectDodge() { return perfectDodge; }

    public long getSkillCooldown(String skillId) { return skillCooldowns.getOrDefault(skillId, 0L); }
    public void setSkillCooldown(String skillId, long timestamp) { skillCooldowns.put(skillId, timestamp); }

    // Standard Setters/Getters
    public int getBaseLevel() { return baseLevel; } public void setBaseLevel(int l) { this.baseLevel = l; calculateMaxSP(); }
    public long getBaseExp() { return baseExp; } public void setBaseExp(long e) { this.baseExp = e; }
    public int getJobLevel() { return jobLevel; } public void setJobLevel(int l) { this.jobLevel = l; }
    public long getJobExp() { return jobExp; } public void setJobExp(long e) { this.jobExp = e; }
    public int getStatPoints() { return statPoints; } public void setStatPoints(int p) { this.statPoints = p; }
    public int getSkillPoints() { return skillPoints; } public void setSkillPoints(int p) { this.skillPoints = p; }
    public int getResetCount() { return resetCount; } public void setResetCount(int c) { this.resetCount = c; }
    public void incrementResetCount() { this.resetCount++; }
    public double getCurrentSP() { return currentSP; } public void setCurrentSP(double s) { this.currentSP = s; }
}