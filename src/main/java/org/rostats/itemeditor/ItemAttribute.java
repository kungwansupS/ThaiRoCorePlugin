package org.rostats.itemeditor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
import org.rostats.engine.trigger.TriggerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemAttribute {

    // --- Base Attributes ---
    private int strGear, agiGear, vitGear, intGear, dexGear, lukGear;
    private double maxHPPercent, maxHPFlat, maxSPPercent, maxSPFlat;
    private double hpRecovery, spRecovery;
    private double hitFlat, fleeFlat;

    // --- Core Combat ---
    private double weaponPAtk, weaponMAtk;
    private double refinePAtk, refineMAtk;
    private double pDefBonus, mDefBonus;
    private double refinePDef, refineMDef;

    // --- Legacy / Compatibility Fields (Required for Handler) ---
    private double pAtkFlat, mAtkFlat;

    // --- Penetration / Ignore ---
    private double pPenFlat, pPenPercent, ignorePDefFlat, ignorePDefPercent;
    private double mPenFlat, mPenPercent, ignoreMDefFlat, ignoreMDefPercent;

    // --- Casting ---
    private double varCTPercent, varCTFlat;
    private double fixedCTPercent, fixedCTFlat;

    // --- Cooldown / Delay / Motion ---
    private double skillCDPercent, skillCDFlat, finalCDPercent;
    private double globalCDPercent, afterCastDelayPercent, afterCastDelayFlat;
    private double preMotion, postMotion, cancelMotion;

    // --- Speed & Mobility ---
    private double aSpdPercent, mSpdPercent, baseMSPD;
    private double atkIntervalPercent;

    // --- Critical ---
    private double crit, critDmgPercent, finalCritDmgPercent;
    private double perfectHit;
    private double critRes, critDmgResPercent, perfectDodge;

    // --- Universal Damage ---
    private double pDmgPercent, pDmgFlat, pDmgReductionPercent;
    private double mDmgPercent, mDmgFlat, mDmgReductionPercent;
    private double trueDamageFlat;
    private double finalDmgPercent, finalDmgResPercent;

    // [FIX] Added missing Final Specific Fields
    private double finalPDmgPercent, finalMDmgPercent;

    // --- Distance Type ---
    private double meleePDmgPercent, meleePDReductionPercent;
    private double rangePDmgPercent, rangePDReductionPercent;

    // --- Content Type ---
    private double pveDmgPercent, pveDmgReductionPercent;
    private double pvpDmgPercent, pvpDmgReductionPercent;

    // --- Healing ---
    private double healingEffectPercent, healingFlat;
    private double healingReceivedPercent, healingReceivedFlat;
    private double lifestealPPercent, lifestealMPercent;

    // --- Misc ---
    private double shieldValueFlat, shieldRatePercent;

    private boolean removeVanillaAttribute;
    private Integer customModelData;
    private Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
    private List<ItemSkillBinding> skillBindings = new ArrayList<>();

    public ItemAttribute() {}

    public static ItemAttribute fromConfig(ConfigurationSection root) {
        ItemAttribute attr = new ItemAttribute();
        if (root == null) return attr;

        attr.removeVanillaAttribute = root.getBoolean("remove-vanilla", false);
        if (root.contains("custom-model-data")) attr.customModelData = root.getInt("custom-model-data");

        ConfigurationSection att = root.getConfigurationSection("attributes");
        if (att == null) return attr;

        // Base
        attr.strGear = att.getInt("str", 0);
        attr.agiGear = att.getInt("agi", 0);
        attr.vitGear = att.getInt("vit", 0);
        attr.intGear = att.getInt("int", 0);
        attr.dexGear = att.getInt("dex", 0);
        attr.lukGear = att.getInt("luk", 0);
        attr.maxHPPercent = att.getDouble("max-hp-%", 0);
        attr.maxHPFlat = att.getDouble("max-hp-flat", 0);
        attr.maxSPPercent = att.getDouble("max-sp-%", 0);
        attr.maxSPFlat = att.getDouble("max-sp-flat", 0);
        attr.hpRecovery = att.getDouble("hp-recovery", 0);
        attr.spRecovery = att.getDouble("sp-recovery", 0);
        attr.hitFlat = att.getDouble("hit-flat", 0);
        attr.fleeFlat = att.getDouble("flee-flat", 0);

        // Combat
        attr.weaponPAtk = att.getDouble("weapon-p-atk", 0);
        attr.weaponMAtk = att.getDouble("weapon-m-atk", 0);
        attr.refinePAtk = att.getDouble("refine-p-atk", 0);
        attr.refineMAtk = att.getDouble("refine-m-atk", 0);
        attr.pDefBonus = att.getDouble("p-def", 0);
        attr.mDefBonus = att.getDouble("m-def", 0);
        attr.refinePDef = att.getDouble("refine-p-def", 0);
        attr.refineMDef = att.getDouble("refine-m-def", 0);

        attr.pAtkFlat = att.getDouble("p-atk-flat", 0);
        attr.mAtkFlat = att.getDouble("m-atk-flat", 0);

        // Pen/Ignore
        attr.pPenFlat = att.getDouble("p-pen-flat", 0);
        attr.pPenPercent = att.getDouble("p-pen-%", 0);
        attr.ignorePDefFlat = att.getDouble("ignore-p-def-flat", 0);
        attr.ignorePDefPercent = att.getDouble("ignore-p-def-%", 0);
        attr.mPenFlat = att.getDouble("m-pen-flat", 0);
        attr.mPenPercent = att.getDouble("m-pen-%", 0);
        attr.ignoreMDefFlat = att.getDouble("ignore-m-def-flat", 0);
        attr.ignoreMDefPercent = att.getDouble("ignore-m-def-%", 0);

        // Casting
        attr.varCTPercent = att.getDouble("var-ct-%", 0);
        attr.varCTFlat = att.getDouble("var-ct-flat", 0);
        attr.fixedCTPercent = att.getDouble("fixed-ct-%", 0);
        attr.fixedCTFlat = att.getDouble("fixed-ct-flat", 0);

        // Cooldown/Delay/Motion
        attr.skillCDPercent = att.getDouble("skill-cd-%", 0);
        attr.skillCDFlat = att.getDouble("skill-cd-flat", 0);
        attr.finalCDPercent = att.getDouble("final-cd-%", 0);
        attr.globalCDPercent = att.getDouble("global-cd-%", 0);
        attr.afterCastDelayPercent = att.getDouble("after-cast-delay-%", 0);
        attr.afterCastDelayFlat = att.getDouble("after-cast-delay-flat", 0);
        attr.preMotion = att.getDouble("pre-motion", 0);
        attr.postMotion = att.getDouble("post-motion", 0);
        attr.cancelMotion = att.getDouble("cancel-motion", 0);

        // Speed
        attr.aSpdPercent = att.getDouble("aspd-%", 0);
        attr.mSpdPercent = att.getDouble("mspd-%", 0);
        attr.baseMSPD = att.getDouble("base-mspd", 0);
        attr.atkIntervalPercent = att.getDouble("atk-interval-%", 0);

        // Critical
        attr.crit = att.getDouble("crit", 0);
        attr.critDmgPercent = att.getDouble("crit-dmg-%", 0);
        attr.finalCritDmgPercent = att.getDouble("final-crit-dmg-%", 0);
        attr.perfectHit = att.getDouble("perfect-hit", 0);
        attr.critRes = att.getDouble("crit-res", 0);
        attr.critDmgResPercent = att.getDouble("crit-dmg-res-%", 0);
        attr.perfectDodge = att.getDouble("perfect-dodge", 0);

        // Universal Damage
        attr.pDmgPercent = att.getDouble("p-dmg-%", 0);
        attr.pDmgFlat = att.getDouble("p-dmg-flat", 0);
        attr.pDmgReductionPercent = att.getDouble("p-dmg-reduce-%", 0);
        attr.mDmgPercent = att.getDouble("m-dmg-%", 0);
        attr.mDmgFlat = att.getDouble("m-dmg-flat", 0);
        attr.mDmgReductionPercent = att.getDouble("m-dmg-reduce-%", 0);
        attr.trueDamageFlat = att.getDouble("true-damage", 0);
        attr.finalDmgPercent = att.getDouble("final-dmg-%", 0);
        attr.finalDmgResPercent = att.getDouble("final-dmg-res-%", 0);

        attr.finalPDmgPercent = att.getDouble("final-p-dmg-%", 0);
        attr.finalMDmgPercent = att.getDouble("final-m-dmg-%", 0);

        // Distance
        attr.meleePDmgPercent = att.getDouble("melee-p-dmg-%", 0);
        attr.meleePDReductionPercent = att.getDouble("melee-p-reduce-%", 0);
        attr.rangePDmgPercent = att.getDouble("range-p-dmg-%", 0);
        attr.rangePDReductionPercent = att.getDouble("range-p-reduce-%", 0);

        // Content
        attr.pveDmgPercent = att.getDouble("pve-dmg-%", 0);
        attr.pveDmgReductionPercent = att.getDouble("pve-reduce-%", 0);
        attr.pvpDmgPercent = att.getDouble("pvp-dmg-%", 0);
        attr.pvpDmgReductionPercent = att.getDouble("pvp-reduce-%", 0);

        // Healing
        attr.healingEffectPercent = att.getDouble("heal-effect-%", 0);
        attr.healingFlat = att.getDouble("heal-flat", 0);
        attr.healingReceivedPercent = att.getDouble("heal-received-%", 0);
        attr.healingReceivedFlat = att.getDouble("heal-received-flat", 0);
        attr.lifestealPPercent = att.getDouble("lifesteal-p-%", 0);
        attr.lifestealMPercent = att.getDouble("lifesteal-m-%", 0);

        // Shield
        attr.shieldValueFlat = att.getDouble("shield-flat", 0);
        attr.shieldRatePercent = att.getDouble("shield-rate-%", 0);

        if (!potionEffects.isEmpty()) {
            ConfigurationSection effectsSec = section.createSection("effects");
            for (Map.Entry<PotionEffectType, Integer> entry : potionEffects.entrySet()) {
                effectsSec.set(entry.getKey().getName(), entry.getValue());
            }
        }

        if (!skillBindings.isEmpty()) {
            List<Map<String, Object>> skillList = new ArrayList<>();
            for (ItemSkillBinding binding : skillBindings) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", binding.getSkillId());
                map.put("trigger", binding.getTrigger().name());
                map.put("level", binding.getLevel());
                map.put("chance", binding.getChance());
                skillList.add(map);
            }
            section.set("skills", skillList);
        }
    }

    // Getters & Setters
    public int getStrGear() { return strGear; } public void setStrGear(int v) { this.strGear = v; }
    public int getAgiGear() { return agiGear; } public void setAgiGear(int v) { this.agiGear = v; }
    public int getVitGear() { return vitGear; } public void setVitGear(int v) { this.vitGear = v; }
    public int getIntGear() { return intGear; } public void setIntGear(int v) { this.intGear = v; }
    public int getDexGear() { return dexGear; } public void setDexGear(int v) { this.dexGear = v; }
    public int getLukGear() { return lukGear; } public void setLukGear(int v) { this.lukGear = v; }
    public double getMaxHPPercent() { return maxHPPercent; } public void setMaxHPPercent(double v) { this.maxHPPercent = v; }
    public double getMaxHPFlat() { return maxHPFlat; } public void setMaxHPFlat(double v) { this.maxHPFlat = v; }
    public double getMaxSPPercent() { return maxSPPercent; } public void setMaxSPPercent(double v) { this.maxSPPercent = v; }
    public double getMaxSPFlat() { return maxSPFlat; } public void setMaxSPFlat(double v) { this.maxSPFlat = v; }
    public double getHpRecovery() { return hpRecovery; } public void setHpRecovery(double v) { this.hpRecovery = v; }
    public double getSpRecovery() { return spRecovery; } public void setSpRecovery(double v) { this.spRecovery = v; }
    public double getHitFlat() { return hitFlat; } public void setHitFlat(double v) { this.hitFlat = v; }
    public double getFleeFlat() { return fleeFlat; } public void setFleeFlat(double v) { this.fleeFlat = v; }

    public double getWeaponPAtk() { return weaponPAtk; } public void setWeaponPAtk(double v) { this.weaponPAtk = v; }
    public double getWeaponMAtk() { return weaponMAtk; } public void setWeaponMAtk(double v) { this.weaponMAtk = v; }
    public double getRefinePAtk() { return refinePAtk; } public void setRefinePAtk(double v) { this.refinePAtk = v; }
    public double getRefineMAtk() { return refineMAtk; } public void setRefineMAtk(double v) { this.refineMAtk = v; }
    public double getPDefBonus() { return pDefBonus; } public void setPDefBonus(double v) { this.pDefBonus = v; }
    public double getMDefBonus() { return mDefBonus; } public void setMDefBonus(double v) { this.mDefBonus = v; }
    public double getRefinePDef() { return refinePDef; } public void setRefinePDef(double v) { this.refinePDef = v; }
    public double getRefineMDef() { return refineMDef; } public void setRefineMDef(double v) { this.refineMDef = v; }

    public double getPAtkFlat() { return pAtkFlat; } public void setPAtkFlat(double v) { this.pAtkFlat = v; }
    public double getMAtkFlat() { return mAtkFlat; } public void setMAtkFlat(double v) { this.mAtkFlat = v; }

    public double getPPenFlat() { return pPenFlat; } public void setPPenFlat(double v) { this.pPenFlat = v; }
    public double getPPenPercent() { return pPenPercent; } public void setPPenPercent(double v) { this.pPenPercent = v; }
    public double getIgnorePDefFlat() { return ignorePDefFlat; } public void setIgnorePDefFlat(double v) { this.ignorePDefFlat = v; }
    public double getIgnorePDefPercent() { return ignorePDefPercent; } public void setIgnorePDefPercent(double v) { this.ignorePDefPercent = v; }
    public double getMPenFlat() { return mPenFlat; } public void setMPenFlat(double v) { this.mPenFlat = v; }
    public double getMPenPercent() { return mPenPercent; } public void setMPenPercent(double v) { this.mPenPercent = v; }
    public double getIgnoreMDefFlat() { return ignoreMDefFlat; } public void setIgnoreMDefFlat(double v) { this.ignoreMDefFlat = v; }
    public double getIgnoreMDefPercent() { return ignoreMDefPercent; } public void setIgnoreMDefPercent(double v) { this.ignoreMDefPercent = v; }

    public double getVarCTPercent() { return varCTPercent; } public void setVarCTPercent(double v) { this.varCTPercent = v; }
    public double getVarCTFlat() { return varCTFlat; } public void setVarCTFlat(double v) { this.varCTFlat = v; }
    public double getFixedCTPercent() { return fixedCTPercent; } public void setFixedCTPercent(double v) { this.fixedCTPercent = v; }
    public double getFixedCTFlat() { return fixedCTFlat; } public void setFixedCTFlat(double v) { this.fixedCTFlat = v; }

    public double getSkillCDPercent() { return skillCDPercent; } public void setSkillCDPercent(double v) { this.skillCDPercent = v; }
    public double getSkillCDFlat() { return skillCDFlat; } public void setSkillCDFlat(double v) { this.skillCDFlat = v; }
    public double getFinalCDPercent() { return finalCDPercent; } public void setFinalCDPercent(double v) { this.finalCDPercent = v; }
    public double getGlobalCDPercent() { return globalCDPercent; } public void setGlobalCDPercent(double v) { this.globalCDPercent = v; }
    public double getAfterCastDelayPercent() { return afterCastDelayPercent; } public void setAfterCastDelayPercent(double v) { this.afterCastDelayPercent = v; }
    public double getAfterCastDelayFlat() { return afterCastDelayFlat; } public void setAfterCastDelayFlat(double v) { this.afterCastDelayFlat = v; }
    public double getPreMotion() { return preMotion; } public void setPreMotion(double v) { this.preMotion = v; }
    public double getPostMotion() { return postMotion; } public void setPostMotion(double v) { this.postMotion = v; }
    public double getCancelMotion() { return cancelMotion; } public void setCancelMotion(double v) { this.cancelMotion = v; }

    public double getASpdPercent() { return aSpdPercent; } public void setASpdPercent(double v) { this.aSpdPercent = v; }
    public double getMSpdPercent() { return mSpdPercent; } public void setMSpdPercent(double v) { this.mSpdPercent = v; }
    public double getBaseMSPD() { return baseMSPD; } public void setBaseMSPD(double v) { this.baseMSPD = v; }
    public double getAtkIntervalPercent() { return atkIntervalPercent; } public void setAtkIntervalPercent(double v) { this.atkIntervalPercent = v; }

    public double getCrit() { return crit; } public void setCrit(double v) { this.crit = v; }
    public double getCritDmgPercent() { return critDmgPercent; } public void setCritDmgPercent(double v) { this.critDmgPercent = v; }
    public double getFinalCritDmgPercent() { return finalCritDmgPercent; } public void setFinalCritDmgPercent(double v) { this.finalCritDmgPercent = v; }
    public double getPerfectHit() { return perfectHit; } public void setPerfectHit(double v) { this.perfectHit = v; }
    public double getCritRes() { return critRes; } public void setCritRes(double v) { this.critRes = v; }
    public double getCritDmgResPercent() { return critDmgResPercent; } public void setCritDmgResPercent(double v) { this.critDmgResPercent = v; }
    public double getPerfectDodge() { return perfectDodge; } public void setPerfectDodge(double v) { this.perfectDodge = v; }

    public double getPDmgPercent() { return pDmgPercent; } public void setPDmgPercent(double v) { this.pDmgPercent = v; }
    public double getPDmgFlat() { return pDmgFlat; } public void setPDmgFlat(double v) { this.pDmgFlat = v; }
    public double getPDmgReductionPercent() { return pDmgReductionPercent; } public void setPDmgReductionPercent(double v) { this.pDmgReductionPercent = v; }
    public double getMDmgPercent() { return mDmgPercent; } public void setMDmgPercent(double v) { this.mDmgPercent = v; }
    public double getMDmgFlat() { return mDmgFlat; } public void setMDmgFlat(double v) { this.mDmgFlat = v; }
    public double getMDmgReductionPercent() { return mDmgReductionPercent; } public void setMDmgReductionPercent(double v) { this.mDmgReductionPercent = v; }
    public double getTrueDamageFlat() { return trueDamageFlat; } public void setTrueDamageFlat(double v) { this.trueDamageFlat = v; }
    public double getFinalDmgPercent() { return finalDmgPercent; } public void setFinalDmgPercent(double v) { this.finalDmgPercent = v; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; } public void setFinalDmgResPercent(double v) { this.finalDmgResPercent = v; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; } public void setFinalPDmgPercent(double v) { this.finalPDmgPercent = v; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; } public void setFinalMDmgPercent(double v) { this.finalMDmgPercent = v; }

    public double getMeleePDmgPercent() { return meleePDmgPercent; } public void setMeleePDmgPercent(double v) { this.meleePDmgPercent = v; }
    public double getMeleePDReductionPercent() { return meleePDReductionPercent; } public void setMeleePDReductionPercent(double v) { this.meleePDReductionPercent = v; }
    public double getRangePDmgPercent() { return rangePDmgPercent; } public void setRangePDmgPercent(double v) { this.rangePDmgPercent = v; }
    public double getRangePDReductionPercent() { return rangePDReductionPercent; } public void setRangePDReductionPercent(double v) { this.rangePDReductionPercent = v; }

    public double getPveDmgPercent() { return pveDmgPercent; } public void setPveDmgPercent(double v) { this.pveDmgPercent = v; }
    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; } public void setPveDmgReductionPercent(double v) { this.pveDmgReductionPercent = v; }
    public double getPvpDmgPercent() { return pvpDmgPercent; } public void setPvpDmgPercent(double v) { this.pvpDmgPercent = v; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; } public void setPvpDmgReductionPercent(double v) { this.pvpDmgReductionPercent = v; }

    public double getHealingEffectPercent() { return healingEffectPercent; } public void setHealingEffectPercent(double v) { this.healingEffectPercent = v; }
    public double getHealingFlat() { return healingFlat; } public void setHealingFlat(double v) { this.healingFlat = v; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; } public void setHealingReceivedPercent(double v) { this.healingReceivedPercent = v; }
    public double getHealingReceivedFlat() { return healingReceivedFlat; } public void setHealingReceivedFlat(double v) { this.healingReceivedFlat = v; }
    public double getLifestealPPercent() { return lifestealPPercent; } public void setLifestealPPercent(double v) { this.lifestealPPercent = v; }
    public double getLifestealMPercent() { return lifestealMPercent; } public void setLifestealMPercent(double v) { this.lifestealMPercent = v; }

    public double getShieldValueFlat() { return shieldValueFlat; } public void setShieldValueFlat(double v) { this.shieldValueFlat = v; }
    public double getShieldRatePercent() { return shieldRatePercent; } public void setShieldRatePercent(double v) { this.shieldRatePercent = v; }

    public boolean isRemoveVanillaAttribute() { return removeVanillaAttribute; } public void setRemoveVanillaAttribute(boolean v) { this.removeVanillaAttribute = v; }
    public Integer getCustomModelData() { return customModelData; } public void setCustomModelData(Integer v) { this.customModelData = v; }
    public Map<PotionEffectType, Integer> getPotionEffects() { return potionEffects; } public void setPotionEffects(Map<PotionEffectType, Integer> v) { this.potionEffects = v; }
    public List<ItemSkillBinding> getSkillBindings() { return skillBindings; } public void setSkillBindings(List<ItemSkillBinding> v) { this.skillBindings = v; }
}