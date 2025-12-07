package org.rostats.itemeditor;

/**
 * A POJO to hold all custom attributes of a single item instance.
 * All fields are initialized to default values (0.0 or 0).
 */
public class ItemAttribute {

    // --- Core Stat Bonuses (int) ---
    private int strGear = 0;
    private int agiGear = 0;
    private int vitGear = 0;
    private int intGear = 0;
    private int dexGear = 0;
    private int lukGear = 0;

    // --- Flat ATK ---
    private double weaponPAtk = 0.0;
    private double weaponMAtk = 0.0;
    private double pAtkFlat = 0.0;
    private double mAtkFlat = 0.0;

    // --- Damage %/Flat ---
    private double pDmgPercent = 0.0;
    private double mDmgPercent = 0.0;
    private double pDmgFlat = 0.0;
    private double mDmgFlat = 0.0;

    // --- Critical ---
    private double critDmgPercent = 0.0;
    private double critDmgResPercent = 0.0;
    private double critRes = 0.0;

    // --- Penetration / Ignore Def ---
    private double pPenFlat = 0.0;
    private double mPenFlat = 0.0;
    private double pPenPercent = 0.0;
    private double mPenPercent = 0.0;

    // --- Final Damage ---
    private double finalDmgPercent = 0.0;
    private double finalDmgResPercent = 0.0;
    private double finalPDmgPercent = 0.0;
    private double finalMDmgPercent = 0.0;

    // --- PVE/PVP ---
    private double pveDmgPercent = 0.0;
    private double pvpDmgPercent = 0.0;
    private double pveDmgReductionPercent = 0.0;
    private double pvpDmgReductionPercent = 0.0;

    // --- Max HP/SP ---
    private double maxHPPercent = 0.0;
    private double maxSPPercent = 0.0;

    // --- Shield ---
    private double shieldValueFlat = 0.0;
    private double shieldRatePercent = 0.0;

    // --- Speed / Cast ---
    private double aSpdPercent = 0.0;
    private double mSpdPercent = 0.0;
    private double baseMSPD = 0.0; // Base Movement Speed Modifier
    private double varCTPercent = 0.0;
    private double varCTFlat = 0.0;
    private double fixedCTPercent = 0.0;
    private double fixedCTFlat = 0.0;

    // --- Healing / Lifesteal ---
    private double healingEffectPercent = 0.0;
    private double healingReceivedPercent = 0.0;
    private double lifestealPPercent = 0.0;
    private double lifestealMPercent = 0.0;

    // --- Hit/Flee ---
    private double hitFlat = 0.0;
    private double fleeFlat = 0.0;

    // --- Utility ---
    private boolean removeVanillaAttribute = false;


    public ItemAttribute() {
        // Default constructor initializes all fields above to 0.0/0/false
    }

    // --- Getters ---

    public int getStrGear() { return strGear; }
    public int getAgiGear() { return agiGear; }
    public int getVitGear() { return vitGear; }
    public int getIntGear() { return intGear; }
    public int getDexGear() { return dexGear; }
    public int getLukGear() { return lukGear; }
    public double getWeaponPAtk() { return weaponPAtk; }
    public double getWeaponMAtk() { return weaponMAtk; }
    public double getPAtkFlat() { return pAtkFlat; }
    public double getMAtkFlat() { return mAtkFlat; }
    public double getPDmgPercent() { return pDmgPercent; }
    public double getMDmgPercent() { return mDmgPercent; }
    public double getPDmgFlat() { return pDmgFlat; }
    public double getMDmgFlat() { return mDmgFlat; }
    public double getCritDmgPercent() { return critDmgPercent; }
    public double getCritDmgResPercent() { return critDmgResPercent; }
    public double getCritRes() { return critRes; }
    public double getPPenFlat() { return pPenFlat; }
    public double getMPenFlat() { return mPenFlat; }
    public double getPPenPercent() { return pPenPercent; }
    public double getMPenPercent() { return mPenPercent; }
    public double getFinalDmgPercent() { return finalDmgPercent; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; }
    public double getPveDmgPercent() { return pveDmgPercent; }
    public double getPvpDmgPercent() { return pvpDmgPercent; }
    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; }
    public double getMaxHPPercent() { return maxHPPercent; }
    public double getMaxSPPercent() { return maxSPPercent; }
    public double getShieldValueFlat() { return shieldValueFlat; }
    public double getShieldRatePercent() { return shieldRatePercent; }
    public double getASpdPercent() { return aSpdPercent; }
    public double getMSpdPercent() { return mSpdPercent; }
    public double getBaseMSPD() { return baseMSPD; }
    public double getVarCTPercent() { return varCTPercent; }
    public double getVarCTFlat() { return varCTFlat; }
    public double getFixedCTPercent() { return fixedCTPercent; }
    public double getFixedCTFlat() { return fixedCTFlat; }
    public double getHealingEffectPercent() { return healingEffectPercent; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; }
    public double getLifestealPPercent() { return lifestealPPercent; }
    public double getLifestealMPercent() { return lifestealMPercent; }
    public double getHitFlat() { return hitFlat; }
    public double getFleeFlat() { return fleeFlat; }
    public boolean isRemoveVanillaAttribute() { return removeVanillaAttribute; }


    // --- Setters ---

    public void setStrGear(int strGear) { this.strGear = strGear; }
    public void setAgiGear(int agiGear) { this.agiGear = agiGear; }
    public void setVitGear(int vitGear) { this.vitGear = vitGear; }
    public void setIntGear(int intGear) { this.intGear = intGear; }
    public void setDexGear(int dexGear) { this.dexGear = dexGear; }
    public void setLukGear(int lukGear) { this.lukGear = lukGear; }
    public void setWeaponPAtk(double weaponPAtk) { this.weaponPAtk = weaponPAtk; }
    public void setWeaponMAtk(double weaponMAtk) { this.weaponMAtk = weaponMAtk; }
    public void setPAtkFlat(double pAtkFlat) { this.pAtkFlat = pAtkFlat; }
    public void setMAtkFlat(double mAtkFlat) { this.mAtkFlat = mAtkFlat; }
    public void setPDmgPercent(double pDmgPercent) { this.pDmgPercent = pDmgPercent; }
    public void setMDmgPercent(double mDmgPercent) { this.mDmgPercent = mDmgPercent; }
    public void setPDmgFlat(double pDmgFlat) { this.pDmgFlat = pDmgFlat; }
    public void setMDmgFlat(double mDmgFlat) { this.mDmgFlat = mDmgFlat; }
    public void setCritDmgPercent(double critDmgPercent) { this.critDmgPercent = critDmgPercent; }
    public void setCritDmgResPercent(double critDmgResPercent) { this.critDmgResPercent = critDmgResPercent; }
    public void setCritRes(double critRes) { this.critRes = critRes; }
    public void setPPenFlat(double pPenFlat) { this.pPenFlat = pPenFlat; }
    public void setMPenFlat(double mPenFlat) { this.mPenFlat = mPenFlat; }
    public void setPPenPercent(double pPenPercent) { this.pPenPercent = pPenPercent; }
    public void setMPenPercent(double mPenPercent) { this.mPenPercent = mPenPercent; }
    public void setFinalDmgPercent(double finalDmgPercent) { this.finalDmgPercent = finalDmgPercent; }
    public void setFinalDmgResPercent(double finalDmgResPercent) { this.finalDmgResPercent = finalDmgResPercent; }
    public void setFinalPDmgPercent(double finalPDmgPercent) { this.finalPDmgPercent = finalPDmgPercent; }
    public void setFinalMDmgPercent(double finalMDmgPercent) { this.finalMDmgPercent = finalMDmgPercent; }
    public void setPveDmgPercent(double pveDmgPercent) { this.pveDmgPercent = pveDmgPercent; }
    public void setPvpDmgPercent(double pvpDmgPercent) { this.pvpDmgPercent = pvpDmgPercent; }
    public void setPveDmgReductionPercent(double pveDmgReductionPercent) { this.pveDmgReductionPercent = pveDmgReductionPercent; }
    public void setPvpDmgReductionPercent(double pvpDmgReductionPercent) { this.pvpDmgReductionPercent = pvpDmgReductionPercent; }
    public void setMaxHPPercent(double maxHPPercent) { this.maxHPPercent = maxHPPercent; }
    public void setMaxSPPercent(double maxSPPercent) { this.maxSPPercent = maxSPPercent; }
    public void setShieldValueFlat(double shieldValueFlat) { this.shieldValueFlat = shieldValueFlat; }
    public void setShieldRatePercent(double shieldRatePercent) { this.shieldRatePercent = shieldRatePercent; }
    public void setASpdPercent(double aSpdPercent) { this.aSpdPercent = aSpdPercent; }
    public void setMSpdPercent(double mSpdPercent) { this.mSpdPercent = mSpdPercent; }
    public void setBaseMSPD(double baseMSPD) { this.baseMSPD = baseMSPD; }
    public void setVarCTPercent(double varCTPercent) { this.varCTPercent = varCTPercent; }
    public void setVarCTFlat(double varCTFlat) { this.varCTFlat = varCTFlat; }
    public void setFixedCTPercent(double fixedCTPercent) { this.fixedCTPercent = fixedCTPercent; }
    public void setFixedCTFlat(double fixedCTFlat) { this.fixedCTFlat = fixedCTFlat; }
    public void setHealingEffectPercent(double healingEffectPercent) { this.healingEffectPercent = healingEffectPercent; }
    public void setHealingReceivedPercent(double healingReceivedPercent) { this.healingReceivedPercent = healingReceivedPercent; }
    public void setLifestealPPercent(double lifestealPPercent) { this.lifestealPPercent = lifestealPPercent; }
    public void setLifestealMPercent(double lifestealMPercent) { this.lifestealMPercent = lifestealMPercent; }
    public void setHitFlat(double hitFlat) { this.hitFlat = hitFlat; }
    public void setFleeFlat(double fleeFlat) { this.fleeFlat = fleeFlat; }
    public void setRemoveVanillaAttribute(boolean removeVanillaAttribute) { this.removeVanillaAttribute = removeVanillaAttribute; }

    /**
     * Helper to set all double values from an ItemAttribute object.
     * Used by the manager to read from PDC.
     */
    public void setAttribute(String key, double value) {
        switch (key.toLowerCase()) {
            // Core Stat Bonuses (int)
            case "strgear" -> setStrGear((int) value);
            case "agigear" -> setAgiGear((int) value);
            case "vitgear" -> setVitGear((int) value);
            case "intgear" -> setIntGear((int) value);
            case "dexgear" -> setDexGear((int) value);
            case "lukgear" -> setLukGear((int) value);
            // Flat ATK
            case "weaponpatk" -> setWeaponPAtk(value);
            case "weaponmatk" -> setWeaponMAtk(value);
            case "patkflat" -> setPAtkFlat(value);
            case "matkflat" -> setMAtkFlat(value);
            // Damage %/Flat
            case "pdmgpercent" -> setPDmgPercent(value);
            case "mdmgpercent" -> setMDmgPercent(value);
            case "pdmgflat" -> setPDmgFlat(value);
            case "mdmgflat" -> setMDmgFlat(value);
            // Critical
            case "critdmgpercent" -> setCritDmgPercent(value);
            case "critdmgrespercent" -> setCritDmgResPercent(value);
            case "critres" -> setCritRes(value);
            // Penetration / Ignore Def
            case "ppenflat" -> setPPenFlat(value);
            case "mpenflat" -> setMPenFlat(value);
            case "ppenpercent" -> setPPenPercent(value);
            case "mpenpercent" -> setMPenPercent(value);
            // Final Damage
            case "finaldmgpercent" -> setFinalDmgPercent(value);
            case "finaldmgrespercent" -> setFinalDmgResPercent(value);
            case "finalpdmgpercent" -> setFinalPDmgPercent(value);
            case "finalmdmgpercent" -> setFinalMDmgPercent(value);
            // PVE/PVP
            case "pvedmgpercent" -> setPveDmgPercent(value);
            case "pvpdmgpercent" -> setPvpDmgPercent(value);
            case "pvedmgreductionpercent" -> setPveDmgReductionPercent(value);
            case "pvpdmgreductionpercent" -> setPvpDmgReductionPercent(value);
            // Max HP/SP
            case "maxhppercent" -> setMaxHPPercent(value);
            case "maxsppercent" -> setMaxSPPercent(value);
            // Shield
            case "shieldvalueflat" -> setShieldValueFlat(value);
            case "shieldratepercent" -> setShieldRatePercent(value);
            // Speed / Cast
            case "aspdpercent" -> setASpdPercent(value);
            case "mspdpercent" -> setMSpdPercent(value);
            case "basemspd" -> setBaseMSPD(value);
            case "varctpercent" -> setVarCTPercent(value);
            case "varctflat" -> setVarCTFlat(value);
            case "fixedctpercent" -> setFixedCTPercent(value);
            case "fixedctflat" -> setFixedCTFlat(value);
            // Healing / Lifesteal
            case "healingeffectpercent" -> setHealingEffectPercent(value);
            case "healingreceivedpercent" -> setHealingReceivedPercent(value);
            case "lifestealppercent" -> setLifestealPPercent(value);
            case "lifestealmercent" -> setLifestealMPercent(value);
            // Hit/Flee
            case "hitflat" -> setHitFlat(value);
            case "fleeflat" -> setFleeFlat(value);
            // Utility
            case "removevanillaattribute" -> setRemoveVanillaAttribute(value == 1.0);
            default -> {
                // Ignore unknown keys
            }
        }
    }
}