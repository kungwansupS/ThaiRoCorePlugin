package org.rostats.itemeditor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class ItemAttribute {

    // Gear Stats
    private int strGear;
    private int agiGear;
    private int vitGear;
    private int intGear;
    private int dexGear;
    private int lukGear;

    // Offensive
    private double weaponPAtk;
    private double weaponMAtk;
    private double pAtkFlat;
    private double mAtkFlat;
    private double pDmgPercent;
    private double mDmgPercent;
    private double pDmgFlat;
    private double mDmgFlat;
    private double critDmgPercent;
    private double critDmgResPercent;
    private double critRes;
    private double pPenFlat;
    private double mPenFlat;
    private double pPenPercent;
    private double mPenPercent;
    private double finalDmgPercent;
    private double finalDmgResPercent;
    private double finalPDmgPercent;
    private double finalMDmgPercent;

    // PvP / PvE Bonus & Reduction
    private double pveDmgPercent;
    private double pvpDmgPercent;
    private double pveDmgReductionPercent;
    private double pvpDmgReductionPercent;

    // Defensive & Misc
    private double maxHPPercent;
    private double maxSPPercent;
    private double shieldValueFlat;
    private double shieldRatePercent;
    private double aSpdPercent;
    private double mSpdPercent;
    private double baseMSPD;
    private double varCTPercent;
    private double varCTFlat;
    private double fixedCTPercent;
    private double fixedCTFlat;
    private double healingEffectPercent;
    private double healingReceivedPercent;
    private double lifestealPPercent;
    private double lifestealMPercent;
    private double hitFlat;
    private double fleeFlat;
    private double pDmgReductionPercent;
    private double mDmgReductionPercent;

    // Ignore Def
    private double ignorePDefPercent;
    private double ignoreMDefPercent;
    private double ignorePDefFlat;
    private double ignoreMDefFlat;

    // Melee / Range
    private double meleePDmgPercent;
    private double rangePDmgPercent;
    private double meleePDReductionPercent;
    private double rangePDReductionPercent;

    // True Damage
    private double trueDamageFlat;

    // Cosmetic / System
    private boolean removeVanillaAttribute;
    private Integer customModelData;

    // NEW: Potion Effects (Type -> Amplifier/Level)
    private Map<PotionEffectType, Integer> potionEffects = new HashMap<>();

    public ItemAttribute() {}

    public static ItemAttribute fromConfig(ConfigurationSection root) {
        ItemAttribute attr = new ItemAttribute();
        if (root == null) return attr;

        attr.removeVanillaAttribute = root.getBoolean("remove-vanilla", false);
        if (root.contains("custom-model-data")) {
            attr.customModelData = root.getInt("custom-model-data");
        } else if (root.contains("CustomModelData")) {
            attr.customModelData = root.getInt("CustomModelData");
        }

        ConfigurationSection att = root.getConfigurationSection("attributes");
        if (att == null) return attr;

        // Load Attributes
        attr.strGear = att.getInt("str", 0);
        attr.agiGear = att.getInt("agi", 0);
        attr.vitGear = att.getInt("vit", 0);
        attr.intGear = att.getInt("int", 0);
        attr.dexGear = att.getInt("dex", 0);
        attr.lukGear = att.getInt("luk", 0);

        attr.weaponPAtk = att.getDouble("weapon-p-atk", 0);
        attr.weaponMAtk = att.getDouble("weapon-m-atk", 0);
        attr.pAtkFlat = att.getDouble("p-atk-flat", 0);
        attr.mAtkFlat = att.getDouble("m-atk-flat", 0);

        attr.pDmgPercent = att.getDouble("p-dmg-%", 0);
        attr.mDmgPercent = att.getDouble("m-dmg-%", 0);
        attr.pDmgFlat = att.getDouble("p-dmg-flat", 0);
        attr.mDmgFlat = att.getDouble("m-dmg-flat", 0);

        attr.critDmgPercent = att.getDouble("crit-dmg-%", 0);
        attr.critDmgResPercent = att.getDouble("crit-dmg-res-%", 0);
        attr.critRes = att.getDouble("crit-res", 0);

        attr.pPenFlat = att.getDouble("p-pen-flat", 0);
        attr.mPenFlat = att.getDouble("m-pen-flat", 0);
        attr.pPenPercent = att.getDouble("p-pen-%", 0);
        attr.mPenPercent = att.getDouble("m-pen-%", 0);

        attr.finalDmgPercent = att.getDouble("final-dmg-%", 0);
        attr.finalDmgResPercent = att.getDouble("final-dmg-res-%", 0);
        attr.finalPDmgPercent = att.getDouble("final-p-dmg-%", 0);
        attr.finalMDmgPercent = att.getDouble("final-m-dmg-%", 0);

        attr.pveDmgPercent = att.getDouble("pve-dmg-%", 0);
        attr.pvpDmgPercent = att.getDouble("pvp-dmg-%", 0);
        attr.pveDmgReductionPercent = att.getDouble("pve-reduce-%", 0);
        attr.pvpDmgReductionPercent = att.getDouble("pvp-reduce-%", 0);

        attr.maxHPPercent = att.getDouble("max-hp-%", 0);
        attr.maxSPPercent = att.getDouble("max-sp-%", 0);
        attr.shieldValueFlat = att.getDouble("shield-flat", 0);
        attr.shieldRatePercent = att.getDouble("shield-rate-%", 0);

        attr.aSpdPercent = att.getDouble("aspd-%", 0);
        attr.mSpdPercent = att.getDouble("mspd-%", 0);
        attr.baseMSPD = att.getDouble("base-mspd", 0);

        attr.varCTPercent = att.getDouble("var-ct-%", 0);
        attr.varCTFlat = att.getDouble("var-ct-flat", 0);
        attr.fixedCTPercent = att.getDouble("fixed-ct-%", 0);
        attr.fixedCTFlat = att.getDouble("fixed-ct-flat", 0);

        attr.healingEffectPercent = att.getDouble("heal-effect-%", 0);
        attr.healingReceivedPercent = att.getDouble("heal-received-%", 0);
        attr.lifestealPPercent = att.getDouble("lifesteal-p-%", 0);
        attr.lifestealMPercent = att.getDouble("lifesteal-m-%", 0);

        attr.hitFlat = att.getDouble("hit-flat", 0);
        attr.fleeFlat = att.getDouble("flee-flat", 0);

        attr.pDmgReductionPercent = att.getDouble("p-dmg-reduce-%", 0);
        attr.mDmgReductionPercent = att.getDouble("m-dmg-reduce-%", 0);

        attr.ignorePDefFlat = att.getDouble("ignore-p-def-flat", 0);
        attr.ignoreMDefFlat = att.getDouble("ignore-m-def-flat", 0);
        attr.ignorePDefPercent = att.getDouble("ignore-p-def-%", 0);
        attr.ignoreMDefPercent = att.getDouble("ignore-m-def-%", 0);

        attr.meleePDmgPercent = att.getDouble("melee-p-dmg-%", 0);
        attr.rangePDmgPercent = att.getDouble("range-p-dmg-%", 0);
        attr.meleePDReductionPercent = att.getDouble("melee-p-reduce-%", 0);
        attr.rangePDReductionPercent = att.getDouble("range-p-reduce-%", 0);

        attr.trueDamageFlat = att.getDouble("true-damage", 0);

        // Load Potion Effects
        if (att.contains("effects")) {
            ConfigurationSection effectsSec = att.getConfigurationSection("effects");
            for (String key : effectsSec.getKeys(false)) {
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type != null) {
                    attr.potionEffects.put(type, effectsSec.getInt(key));
                }
            }
        }

        return attr;
    }

    public void saveToConfig(ConfigurationSection section) {
        if (strGear != 0) section.set("str", strGear);
        if (agiGear != 0) section.set("agi", agiGear);
        if (vitGear != 0) section.set("vit", vitGear);
        if (intGear != 0) section.set("int", intGear);
        if (dexGear != 0) section.set("dex", dexGear);
        if (lukGear != 0) section.set("luk", lukGear);

        if (weaponPAtk != 0) section.set("weapon-p-atk", weaponPAtk);
        if (weaponMAtk != 0) section.set("weapon-m-atk", weaponMAtk);
        if (pAtkFlat != 0) section.set("p-atk-flat", pAtkFlat);
        if (mAtkFlat != 0) section.set("m-atk-flat", mAtkFlat);

        if (pDmgPercent != 0) section.set("p-dmg-%", pDmgPercent);
        if (mDmgPercent != 0) section.set("m-dmg-%", mDmgPercent);
        if (pDmgFlat != 0) section.set("p-dmg-flat", pDmgFlat);
        if (mDmgFlat != 0) section.set("m-dmg-flat", mDmgFlat);

        if (critDmgPercent != 0) section.set("crit-dmg-%", critDmgPercent);
        if (critDmgResPercent != 0) section.set("crit-dmg-res-%", critDmgResPercent);
        if (critRes != 0) section.set("crit-res", critRes);

        if (pPenFlat != 0) section.set("p-pen-flat", pPenFlat);
        if (mPenFlat != 0) section.set("m-pen-flat", mPenFlat);
        if (pPenPercent != 0) section.set("p-pen-%", pPenPercent);
        if (mPenPercent != 0) section.set("m-pen-%", mPenPercent);

        if (finalDmgPercent != 0) section.set("final-dmg-%", finalDmgPercent);
        if (finalDmgResPercent != 0) section.set("final-dmg-res-%", finalDmgResPercent);
        if (finalPDmgPercent != 0) section.set("final-p-dmg-%", finalPDmgPercent);
        if (finalMDmgPercent != 0) section.set("final-m-dmg-%", finalMDmgPercent);

        if (pveDmgPercent != 0) section.set("pve-dmg-%", pveDmgPercent);
        if (pvpDmgPercent != 0) section.set("pvp-dmg-%", pvpDmgPercent);
        if (pveDmgReductionPercent != 0) section.set("pve-reduce-%", pveDmgReductionPercent);
        if (pvpDmgReductionPercent != 0) section.set("pvp-reduce-%", pvpDmgReductionPercent);

        if (maxHPPercent != 0) section.set("max-hp-%", maxHPPercent);
        if (maxSPPercent != 0) section.set("max-sp-%", maxSPPercent);
        if (shieldValueFlat != 0) section.set("shield-flat", shieldValueFlat);
        if (shieldRatePercent != 0) section.set("shield-rate-%", shieldRatePercent);

        if (aSpdPercent != 0) section.set("aspd-%", aSpdPercent);
        if (mSpdPercent != 0) section.set("mspd-%", mSpdPercent);
        if (baseMSPD != 0) section.set("base-mspd", baseMSPD);

        if (varCTPercent != 0) section.set("var-ct-%", varCTPercent);
        if (varCTFlat != 0) section.set("var-ct-flat", varCTFlat);
        if (fixedCTPercent != 0) section.set("fixed-ct-%", fixedCTPercent);
        if (fixedCTFlat != 0) section.set("fixed-ct-flat", fixedCTFlat);

        if (healingEffectPercent != 0) section.set("heal-effect-%", healingEffectPercent);
        if (healingReceivedPercent != 0) section.set("heal-received-%", healingReceivedPercent);
        if (lifestealPPercent != 0) section.set("lifesteal-p-%", lifestealPPercent);
        if (lifestealMPercent != 0) section.set("lifesteal-m-%", lifestealMPercent);

        if (hitFlat != 0) section.set("hit-flat", hitFlat);
        if (fleeFlat != 0) section.set("flee-flat", fleeFlat);

        if (pDmgReductionPercent != 0) section.set("p-dmg-reduce-%", pDmgReductionPercent);
        if (mDmgReductionPercent != 0) section.set("m-dmg-reduce-%", mDmgReductionPercent);

        if (ignorePDefFlat != 0) section.set("ignore-p-def-flat", ignorePDefFlat);
        if (ignoreMDefFlat != 0) section.set("ignore-m-def-flat", ignoreMDefFlat);
        if (ignorePDefPercent != 0) section.set("ignore-p-def-%", ignorePDefPercent);
        if (ignoreMDefPercent != 0) section.set("ignore-m-def-%", ignoreMDefPercent);

        if (meleePDmgPercent != 0) section.set("melee-p-dmg-%", meleePDmgPercent);
        if (rangePDmgPercent != 0) section.set("range-p-dmg-%", rangePDmgPercent);
        if (meleePDReductionPercent != 0) section.set("melee-p-reduce-%", meleePDReductionPercent);
        if (rangePDReductionPercent != 0) section.set("range-p-reduce-%", rangePDReductionPercent);

        if (trueDamageFlat != 0) section.set("true-damage", trueDamageFlat);

        // Save Potion Effects
        if (!potionEffects.isEmpty()) {
            ConfigurationSection effectsSec = section.createSection("effects");
            for (Map.Entry<PotionEffectType, Integer> entry : potionEffects.entrySet()) {
                effectsSec.set(entry.getKey().getName(), entry.getValue());
            }
        }
    }

    // Getters and Setters
    public int getStrGear() { return strGear; }
    public void setStrGear(int strGear) { this.strGear = strGear; }
    public int getAgiGear() { return agiGear; }
    public void setAgiGear(int agiGear) { this.agiGear = agiGear; }
    public int getVitGear() { return vitGear; }
    public void setVitGear(int vitGear) { this.vitGear = vitGear; }
    public int getIntGear() { return intGear; }
    public void setIntGear(int intGear) { this.intGear = intGear; }
    public int getDexGear() { return dexGear; }
    public void setDexGear(int dexGear) { this.dexGear = dexGear; }
    public int getLukGear() { return lukGear; }
    public void setLukGear(int lukGear) { this.lukGear = lukGear; }

    public double getWeaponPAtk() { return weaponPAtk; }
    public void setWeaponPAtk(double weaponPAtk) { this.weaponPAtk = weaponPAtk; }
    public double getWeaponMAtk() { return weaponMAtk; }
    public void setWeaponMAtk(double weaponMAtk) { this.weaponMAtk = weaponMAtk; }
    public double getPAtkFlat() { return pAtkFlat; }
    public void setPAtkFlat(double pAtkFlat) { this.pAtkFlat = pAtkFlat; }
    public double getMAtkFlat() { return mAtkFlat; }
    public void setMAtkFlat(double mAtkFlat) { this.mAtkFlat = mAtkFlat; }

    public double getPDmgPercent() { return pDmgPercent; }
    public void setPDmgPercent(double pDmgPercent) { this.pDmgPercent = pDmgPercent; }
    public double getMDmgPercent() { return mDmgPercent; }
    public void setMDmgPercent(double mDmgPercent) { this.mDmgPercent = mDmgPercent; }
    public double getPDmgFlat() { return pDmgFlat; }
    public void setPDmgFlat(double pDmgFlat) { this.pDmgFlat = pDmgFlat; }
    public double getMDmgFlat() { return mDmgFlat; }
    public void setMDmgFlat(double mDmgFlat) { this.mDmgFlat = mDmgFlat; }

    public double getCritDmgPercent() { return critDmgPercent; }
    public void setCritDmgPercent(double critDmgPercent) { this.critDmgPercent = critDmgPercent; }
    public double getCritDmgResPercent() { return critDmgResPercent; }
    public void setCritDmgResPercent(double critDmgResPercent) { this.critDmgResPercent = critDmgResPercent; }
    public double getCritRes() { return critRes; }
    public void setCritRes(double critRes) { this.critRes = critRes; }

    public double getPPenFlat() { return pPenFlat; }
    public void setPPenFlat(double pPenFlat) { this.pPenFlat = pPenFlat; }
    public double getMPenFlat() { return mPenFlat; }
    public void setMPenFlat(double mPenFlat) { this.mPenFlat = mPenFlat; }
    public double getPPenPercent() { return pPenPercent; }
    public void setPPenPercent(double pPenPercent) { this.pPenPercent = pPenPercent; }
    public double getMPenPercent() { return mPenPercent; }
    public void setMPenPercent(double mPenPercent) { this.mPenPercent = mPenPercent; }

    public double getFinalDmgPercent() { return finalDmgPercent; }
    public void setFinalDmgPercent(double finalDmgPercent) { this.finalDmgPercent = finalDmgPercent; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; }
    public void setFinalDmgResPercent(double finalDmgResPercent) { this.finalDmgResPercent = finalDmgResPercent; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; }
    public void setFinalPDmgPercent(double finalPDmgPercent) { this.finalPDmgPercent = finalPDmgPercent; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; }
    public void setFinalMDmgPercent(double finalMDmgPercent) { this.finalMDmgPercent = finalMDmgPercent; }

    public double getPveDmgPercent() { return pveDmgPercent; }
    public void setPveDmgPercent(double pveDmgPercent) { this.pveDmgPercent = pveDmgPercent; }
    public double getPvpDmgPercent() { return pvpDmgPercent; }
    public void setPvpDmgPercent(double pvpDmgPercent) { this.pvpDmgPercent = pvpDmgPercent; }
    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; }
    public void setPveDmgReductionPercent(double pveDmgReductionPercent) { this.pveDmgReductionPercent = pveDmgReductionPercent; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; }
    public void setPvpDmgReductionPercent(double pvpDmgReductionPercent) { this.pvpDmgReductionPercent = pvpDmgReductionPercent; }

    public double getMaxHPPercent() { return maxHPPercent; }
    public void setMaxHPPercent(double maxHPPercent) { this.maxHPPercent = maxHPPercent; }
    public double getMaxSPPercent() { return maxSPPercent; }
    public void setMaxSPPercent(double maxSPPercent) { this.maxSPPercent = maxSPPercent; }

    public double getShieldValueFlat() { return shieldValueFlat; }
    public void setShieldValueFlat(double shieldValueFlat) { this.shieldValueFlat = shieldValueFlat; }
    public double getShieldRatePercent() { return shieldRatePercent; }
    public void setShieldRatePercent(double shieldRatePercent) { this.shieldRatePercent = shieldRatePercent; }

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

    public double getHealingEffectPercent() { return healingEffectPercent; }
    public void setHealingEffectPercent(double healingEffectPercent) { this.healingEffectPercent = healingEffectPercent; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; }
    public void setHealingReceivedPercent(double healingReceivedPercent) { this.healingReceivedPercent = healingReceivedPercent; }

    public double getLifestealPPercent() { return lifestealPPercent; }
    public void setLifestealPPercent(double lifestealPPercent) { this.lifestealPPercent = lifestealPPercent; }
    public double getLifestealMPercent() { return lifestealMPercent; }
    public void setLifestealMPercent(double lifestealMPercent) { this.lifestealMPercent = lifestealMPercent; }

    public double getHitFlat() { return hitFlat; }
    public void setHitFlat(double hitFlat) { this.hitFlat = hitFlat; }
    public double getFleeFlat() { return fleeFlat; }
    public void setFleeFlat(double fleeFlat) { this.fleeFlat = fleeFlat; }

    public double getPDmgReductionPercent() { return pDmgReductionPercent; }
    public void setPDmgReductionPercent(double pDmgReductionPercent) { this.pDmgReductionPercent = pDmgReductionPercent; }
    public double getMDmgReductionPercent() { return mDmgReductionPercent; }
    public void setMDmgReductionPercent(double mDmgReductionPercent) { this.mDmgReductionPercent = mDmgReductionPercent; }

    public double getIgnorePDefPercent() { return ignorePDefPercent; }
    public void setIgnorePDefPercent(double ignorePDefPercent) { this.ignorePDefPercent = ignorePDefPercent; }
    public double getIgnoreMDefPercent() { return ignoreMDefPercent; }
    public void setIgnoreMDefPercent(double ignoreMDefPercent) { this.ignoreMDefPercent = ignoreMDefPercent; }
    public double getIgnorePDefFlat() { return ignorePDefFlat; }
    public void setIgnorePDefFlat(double ignorePDefFlat) { this.ignorePDefFlat = ignorePDefFlat; }
    public double getIgnoreMDefFlat() { return ignoreMDefFlat; }
    public void setIgnoreMDefFlat(double ignoreMDefFlat) { this.ignoreMDefFlat = ignoreMDefFlat; }

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

    public boolean isRemoveVanillaAttribute() { return removeVanillaAttribute; }
    public void setRemoveVanillaAttribute(boolean removeVanillaAttribute) { this.removeVanillaAttribute = removeVanillaAttribute; }

    public Integer getCustomModelData() { return customModelData; }
    public void setCustomModelData(Integer customModelData) { this.customModelData = customModelData; }

    public Map<PotionEffectType, Integer> getPotionEffects() { return potionEffects; }
    public void setPotionEffects(Map<PotionEffectType, Integer> potionEffects) { this.potionEffects = potionEffects; }
}