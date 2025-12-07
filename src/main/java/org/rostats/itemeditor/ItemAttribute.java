package org.rostats.itemeditor;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;

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

    private boolean removeVanillaAttribute;

    public ItemAttribute() {
    }

    // --- 2) LOAD FROM YAML ---
    public static ItemAttribute fromConfig(ConfigurationSection section) {
        ItemAttribute attr = new ItemAttribute();
        if (section == null) return attr;

        attr.removeVanillaAttribute = section.getBoolean("remove-vanilla", false);
        ConfigurationSection att = section.getConfigurationSection("attributes");
        if (att == null) return attr;

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

        return attr;
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

    public boolean isRemoveVanillaAttribute() { return removeVanillaAttribute; }
    public void setRemoveVanillaAttribute(boolean removeVanillaAttribute) { this.removeVanillaAttribute = removeVanillaAttribute; }
}