package org.rostats.data;

import org.bukkit.Bukkit; // เพิ่ม import สำหรับ Bukkit
import org.bukkit.entity.Player; // เพิ่ม import สำหรับ Player
import org.rostats.ROStatsPlugin; // Import ถูกต้อง
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
    private double pAtkBonusFlat = 0.0;
    private double mAtkBonusFlat = 0.0;
    private double critRes = 0.0;
    private double critDmgPercent = 50.0;
    private double critDmgResPercent = 0.0;
    private double pDmgBonusPercent = 0.0;
    private double mDmgBonusPercent = 0.0;
    private double pDmgBonusFlat = 0.0;
    private double mDmgBonusFlat = 0.0;
    private double pDmgReductionPercent = 0.0;
    private double mDmgReductionPercent = 0.0;
    private double meleePDmgPercent = 0.0;
    private double rangePDmgPercent = 0.0;
    private double meleePDReductionPercent = 0.0;
    private double rangePDReductionPercent = 0.0;
    private double pPenFlat = 0.0;
    private double mPenFlat = 0.0;
    private double pPenPercent = 0.0;
    private double mPenPercent = 0.0;
    private double ignorePDefFlat = 0.0;
    private double ignoreMDefFlat = 0.0;
    private double ignorePDefPercent = 0.0;
    private double ignoreMDefPercent = 0.0;
    private double aSpdPercent = 0.0;
    private double mSpdPercent = 0.0;
    private double varCTPercent = 0.0;
    private double varCTFlat = 0.0;
    private double fixedCTPercent = 0.0;
    private double fixedCTFlat = 0.0;
    private double healingEffectPercent = 0.0;
    private double healingReceivedPercent = 0.0;
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
    private double lifestealPPercent = 0.0;
    private double lifestealMPercent = 0.0;
    private double trueDamageFlat = 0.0;
    private double shieldValueFlat = 0.0;
    private double shieldRatePercent = 0.0;
    private double weaponPAtk = 0.0;
    private double weaponMAtk = 0.0;
    private double hitBonusFlat = 0.0;
    private double fleeBonusFlat = 0.0;
    private double baseMSPD = 0.1;

    private final ROStatsPlugin plugin;

    public PlayerData(ROStatsPlugin plugin) {
        this.plugin = plugin;
        stats.put("STR", 1); stats.put("AGI", 1); stats.put("VIT", 1);
        stats.put("INT", 1); stats.put("DEX", 1); stats.put("LUK", 1);
        calculateMaxSP();
        pendingStats.put("STR", 0); pendingStats.put("AGI", 0); pendingStats.put("VIT", 0);
        pendingStats.put("INT", 0); pendingStats.put("DEX", 0); pendingStats.put("LUK", 0);
    }

    // --- Getters for New Fields ---
    public double getPAtkBonusFlat() { return pAtkBonusFlat; }
    public double getMAtkBonusFlat() { return mAtkBonusFlat; }
    public double getCritRes() { return critRes; }
    public double getCritDmgPercent() { return critDmgPercent; }
    public double getPDmgBonusPercent() { return pDmgBonusPercent; }
    public double getMDmgBonusPercent() { return mDmgBonusPercent; }
    public double getPPenPercent() { return pPenPercent; }
    public double getMPenPercent() { return mPenPercent; }
    public double getIgnorePDefPercent() { return ignorePDefPercent; }
    public double getIgnoreMDefPercent() { return ignoreMDefPercent; }
    public double getASpdPercent() { return aSpdPercent; }
    public double getMSpdPercent() { return mSpdPercent; }
    public double getFinalDmgPercent() { return finalDmgPercent; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; }
    public double getPveDmgBonusPercent() { return pveDmgBonusPercent; }
    public double getPvpDmgBonusPercent() { return pvpDmgBonusPercent; }
    public double getPDmgBonusFlat() { return pDmgBonusFlat; }
    public double getMDmgBonusFlat() { return mDmgBonusFlat; }
    public double getMaxHPPercent() { return maxHPPercent; }
    public double getMaxSPPercent() { return maxSPPercent; }
    public double getLifestealPPercent() { return lifestealPPercent; }
    public double getLifestealMPercent() { return lifestealMPercent; }
    public double getTrueDamageFlat() { return trueDamageFlat; }
    public double getShieldValueFlat() { return shieldValueFlat; }
    public double getShieldRatePercent() { return shieldRatePercent; }
    public double getFixedCTFlat() { return fixedCTFlat; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; }
    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; }
    public double getPDmgReductionPercent() { return pDmgReductionPercent; }
    public double getMDmgReductionPercent() { return mDmgReductionPercent; }
    public double getFixedCTPercent() { return fixedCTPercent; }
    public double getVarCTPercent() { return varCTPercent; }
    public double getVarCTFlat() { return varCTFlat; }
    public double getHealingEffectPercent() { return healingEffectPercent; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; }
    public double getMeleePDmgPercent() { return meleePDmgPercent; }
    public double getRangePDmgPercent() { return rangePDmgPercent; }
    public double getMeleePDReductionPercent() { return meleePDReductionPercent; }
    public double getRangePDReductionPercent() { return rangePDReductionPercent; }
    public double getPPenFlat() { return pPenFlat; }
    public double getMPenFlat() { return mPenFlat; }
    public double getCritDmgResPercent() { return critDmgResPercent; }
    public double getIgnorePDefFlat() { return ignorePDefFlat; }
    public double getIgnoreMDefFlat() { return ignoreMDefFlat; }
    public double getWeaponPAtk() { return weaponPAtk; }
    public double getWeaponMAtk() { return weaponMAtk; }
    public double getHitBonusFlat() { return hitBonusFlat; }
    public double getFleeBonusFlat() { return fleeBonusFlat; }
    public double getBaseMSPD() { return baseMSPD; }

    // NEW Getters/Setters for Job Level/Exp/Points
    public int getJobLevel() { return jobLevel; }
    public void setJobLevel(int jobLevel) { this.jobLevel = jobLevel; }
    public long getJobExp() { return jobExp; }
    public void setJobExp(long jobExp) { this.jobExp = jobExp; }
    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }


    // --- Core Methods (Modified) ---
    public int getStat(String key) { return stats.getOrDefault(key.toUpperCase(), 1); }
    public void setStat(String key, int val) { stats.put(key.toUpperCase(), val); calculateMaxSP(); }
    public Set<String> getStatKeys() { return stats.keySet(); }
    public int getPendingStat(String key) { return pendingStats.getOrDefault(key.toUpperCase(), 0); }
    public void setPendingStat(String key, int count) { pendingStats.put(key.toUpperCase(), count); }
    public void clearAllPendingStats() { pendingStats.put("STR", 0); pendingStats.put("AGI", 0); pendingStats.put("VIT", 0);
        pendingStats.put("INT", 0); pendingStats.put("DEX", 0); pendingStats.put("LUK", 0); }

    // Formula A.1 (Max HP) - Corrected
    public double getMaxHP() {
        int vit = getStat("VIT") + getPendingStat("VIT"); // Base Stat + Pending Stat
        int baseLevel = getBaseLevel();

        // MaxHP = (BaseHP + BaseHP × VIT × 0.01) × (1 + MaxHP% / 100)
        // Assume BaseHP = 18 + baseLevel * 2.0
        double baseHealth = 18 + (baseLevel * 2.0);
        double vitMultiplier = 1.0 + (vit * 0.01);

        double finalMaxHealth = baseHealth * vitMultiplier;
        return Math.floor(finalMaxHealth * (1 + getMaxHPPercent() / 100.0));
    }

    // Formula A.2 (Max SP) - Corrected
    public double getMaxSP() {
        int intel = getStat("INT") + getPendingStat("INT");
        int baseLevel = getBaseLevel();

        // MaxSP = (BaseSP + BaseSP × INT × 0.01) × (1 + MaxSP% / 100)
        // Assume BaseSP = 20 + baseLevel * 3.0
        double baseSP = 20.0 + (baseLevel * 3.0);
        double intMultiplier = 1.0 + (intel * 0.01);

        double finalMaxSP = baseSP * intMultiplier;
        return Math.floor(finalMaxSP * (1 + getMaxSPPercent() / 100.0));
    }

    // NEW: HP Recovery Formula
    // HP Recovery = (BaseHPRecovery + VIT × 0.2) × (1 + HealingReceive% / 100)
    public double getHPRegen() {
        int vit = getStat("VIT");
        double baseHPRecovery = 1.0; // Assume BaseHPRecovery = 1.0

        double baseRegen = baseHPRecovery + (vit * 0.2);
        return baseRegen * (1 + getHealingReceivedPercent() / 100.0);
    }

    // SP Regen - Corrected to include HealingReceive% and exclude maxSpBonus
    public void calculateMaxSP() { if (this.currentSP > getMaxSP()) this.currentSP = getMaxSP(); }
    public void regenSP() { double max = getMaxSP();
        if (this.currentSP < max) {
            int intel = getStat("INT");

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
    public int getBaseLevel() { return baseLevel; }
    public void setBaseLevel(int level) { this.baseLevel = level; calculateMaxSP(); }
    public int getStatPoints() { return statPoints; }
    public void setStatPoints(int points) { this.statPoints = points; }
    public double getCurrentSP() { return currentSP; }
    public void setCurrentSP(double sp) { this.currentSP = sp; }
    public int getResetCount() { return resetCount; }
    public void setResetCount(int count) { this.resetCount = count; }
    public void incrementResetCount() { this.resetCount++; }

    // NEW Helper: Calculate EXP Bonus Multiplier based on Player LV vs World LV
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

    public long getBaseExp() { return baseExp; }
    public void setBaseExp(long baseExp) { this.baseExp = baseExp; }
    public long getBaseExpReq() { return getBaseExpReq(baseLevel); }
    public long getJobExpReq() { return getJobExpReq(jobLevel); }

    // NEW Helper: Custom Rounding Logic for EXP
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
}