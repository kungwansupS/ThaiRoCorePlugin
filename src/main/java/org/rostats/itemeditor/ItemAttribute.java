package org.rostats.itemeditor;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionEffectType;
import org.rostats.engine.trigger.TriggerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemAttribute {

    // --- 1. Base Attributes ---
    private int strGear;
    private int agiGear;
    private int vitGear;
    private int intGear;
    private int dexGear;
    private int lukGear;

    private double maxHPPercent;
    private double maxSPPercent;

    private double hitFlat;
    private double fleeFlat;
    private double baseMSPD;

    // --- 2. Core Combat Stats ---
    private double weaponPAtk;
    private double weaponMAtk;
    private double pAtkFlat;
    private double mAtkFlat;

    // --- 3. Penetration / Ignore Def ---
    private double pPenFlat;
    private double pPenPercent;
    private double ignorePDefFlat;
    private double ignorePDefPercent;

    private double mPenFlat;
    private double mPenPercent;
    private double ignoreMDefFlat;
    private double ignoreMDefPercent;

    // --- 4. Casting & Cooldown ---
    private double varCTPercent;
    private double varCTFlat;
    private double fixedCTPercent;
    private double fixedCTFlat;

    private double skillCooldownPercent;
    private double skillCooldownFlat;

    private double globalCooldownPercent;
    private double globalCooldownFlat;

    private double acdPercent;
    private double acdFlat;

    // --- 5. Speed & Mobility ---
    private double aSpdPercent;
    private double mSpdPercent;

    // --- 6. Critical System ---
    private double critRate;
    private double critDmgPercent;
    private double critRes;
    private double critDmgResPercent;

    // --- 7. Universal Damage Modifiers ---
    private double pDmgPercent;
    private double pDmgFlat;
    private double pDmgReductionPercent;

    private double mDmgPercent;
    private double mDmgFlat;
    private double mDmgReductionPercent;

    private double finalDmgPercent;
    private double finalPDmgPercent;
    private double finalMDmgPercent;

    private double finalDmgResPercent;
    private double trueDamageFlat;

    // --- 8. Distance-Type ---
    private double meleePDmgPercent;
    private double meleePDReductionPercent;
    private double rangePDmgPercent;
    private double rangePDReductionPercent;

    // --- 9. Content-Type ---
    private double pveDmgPercent;
    private double pveDmgReductionPercent;
    private double pvpDmgPercent;
    private double pvpDmgReductionPercent;

    // --- 10. Healing & Defense ---
    private double healingEffectPercent;
    private double healingReceivedPercent;
    private double lifestealPPercent;
    private double lifestealMPercent;

    private double shieldValueFlat;
    private double shieldRatePercent;

    // --- 11. Elements (Stored as Int index 0-9) ---
    private int attackElement = -1; // -1 means no change
    private int defenseElement = -1; // -1 means no change

    // Misc
    private boolean removeVanillaAttribute;
    private Integer customModelData;
    private boolean unbreakable;

    private Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
    private List<ItemSkillBinding> skillBindings = new ArrayList<>();

    public ItemAttribute() {}

    public static ItemAttribute fromConfig(ConfigurationSection root) {
        ItemAttribute attr = new ItemAttribute();
        if (root == null) return attr;

        attr.removeVanillaAttribute = root.getBoolean("remove-vanilla", false);
        attr.unbreakable = root.getBoolean("unbreakable", false);

        if (root.contains("custom-model-data")) attr.customModelData = root.getInt("custom-model-data");
        else if (root.contains("CustomModelData")) attr.customModelData = root.getInt("CustomModelData");

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
        attr.maxSPPercent = att.getDouble("max-sp-%", 0);
        attr.hitFlat = att.getDouble("hit-flat", 0);
        attr.fleeFlat = att.getDouble("flee-flat", 0);
        attr.baseMSPD = att.getDouble("base-mspd", 0);

        attr.weaponPAtk = att.getDouble("weapon-p-atk", 0);
        attr.weaponMAtk = att.getDouble("weapon-m-atk", 0);
        attr.pAtkFlat = att.getDouble("p-atk-flat", 0);
        attr.mAtkFlat = att.getDouble("m-atk-flat", 0);

        attr.pPenFlat = att.getDouble("p-pen-flat", 0);
        attr.pPenPercent = att.getDouble("p-pen-%", 0);
        attr.ignorePDefFlat = att.getDouble("ignore-p-def-flat", 0);
        attr.ignorePDefPercent = att.getDouble("ignore-p-def-%", 0);
        attr.mPenFlat = att.getDouble("m-pen-flat", 0);
        attr.mPenPercent = att.getDouble("m-pen-%", 0);
        attr.ignoreMDefFlat = att.getDouble("ignore-m-def-flat", 0);
        attr.ignoreMDefPercent = att.getDouble("ignore-m-def-%", 0);

        attr.varCTPercent = att.getDouble("var-ct-%", 0);
        attr.varCTFlat = att.getDouble("var-ct-flat", 0);
        attr.fixedCTPercent = att.getDouble("fixed-ct-%", 0);
        attr.fixedCTFlat = att.getDouble("fixed-ct-flat", 0);
        attr.skillCooldownPercent = att.getDouble("skill-cd-%", 0);
        attr.skillCooldownFlat = att.getDouble("skill-cd-flat", 0);

        attr.globalCooldownPercent = att.getDouble("global-cd-%", 0);
        attr.globalCooldownFlat = att.getDouble("global-cd-flat", 0);
        attr.acdPercent = att.getDouble("acd-%", 0);
        attr.acdFlat = att.getDouble("acd-flat", 0);

        attr.aSpdPercent = att.getDouble("aspd-%", 0);
        attr.mSpdPercent = att.getDouble("mspd-%", 0);

        attr.critRate = att.getDouble("crit-rate", 0);
        attr.critDmgPercent = att.getDouble("crit-dmg-%", 0);
        attr.critRes = att.getDouble("crit-res", 0);
        attr.critDmgResPercent = att.getDouble("crit-dmg-res-%", 0);

        attr.pDmgPercent = att.getDouble("p-dmg-%", 0);
        attr.pDmgFlat = att.getDouble("p-dmg-flat", 0);
        attr.pDmgReductionPercent = att.getDouble("p-dmg-reduce-%", 0);
        attr.mDmgPercent = att.getDouble("m-dmg-%", 0);
        attr.mDmgFlat = att.getDouble("m-dmg-flat", 0);
        attr.mDmgReductionPercent = att.getDouble("m-dmg-reduce-%", 0);
        attr.finalDmgPercent = att.getDouble("final-dmg-%", 0);
        attr.finalPDmgPercent = att.getDouble("final-p-dmg-%", 0);
        attr.finalMDmgPercent = att.getDouble("final-m-dmg-%", 0);
        attr.finalDmgResPercent = att.getDouble("final-dmg-res-%", 0);
        attr.trueDamageFlat = att.getDouble("true-damage", 0);

        attr.meleePDmgPercent = att.getDouble("melee-p-dmg-%", 0);
        attr.meleePDReductionPercent = att.getDouble("melee-p-reduce-%", 0);
        attr.rangePDmgPercent = att.getDouble("range-p-dmg-%", 0);
        attr.rangePDReductionPercent = att.getDouble("range-p-reduce-%", 0);
        attr.pveDmgPercent = att.getDouble("pve-dmg-%", 0);
        attr.pveDmgReductionPercent = att.getDouble("pve-reduce-%", 0);
        attr.pvpDmgPercent = att.getDouble("pvp-dmg-%", 0);
        attr.pvpDmgReductionPercent = att.getDouble("pvp-reduce-%", 0);

        attr.healingEffectPercent = att.getDouble("heal-effect-%", 0);
        attr.healingReceivedPercent = att.getDouble("heal-received-%", 0);
        attr.lifestealPPercent = att.getDouble("lifesteal-p-%", 0);
        attr.lifestealMPercent = att.getDouble("lifesteal-m-%", 0);
        attr.shieldValueFlat = att.getDouble("shield-flat", 0);
        attr.shieldRatePercent = att.getDouble("shield-rate-%", 0);

        // Elements
        attr.attackElement = att.getInt("atk-element", -1);
        attr.defenseElement = att.getInt("def-element", -1);

        if (att.contains("effects")) {
            ConfigurationSection effectsSec = att.getConfigurationSection("effects");
            for (String key : effectsSec.getKeys(false)) {
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type != null) attr.potionEffects.put(type, effectsSec.getInt(key));
            }
        }
        if (att.contains("skills")) {
            List<Map<?, ?>> skillList = att.getMapList("skills");
            for (Map<?, ?> map : skillList) {
                try {
                    String skillId = (String) map.get("id");
                    TriggerType trigger = TriggerType.valueOf((String) map.get("trigger"));
                    int level = (int) map.get("level");
                    double chance = ((Number) map.get("chance")).doubleValue();
                    attr.skillBindings.add(new ItemSkillBinding(skillId, trigger, level, chance));
                } catch (Exception ignored) {}
            }
        }

        return attr;
    }

    public void saveToConfig(ConfigurationSection section) {
        section.set("remove-vanilla", removeVanillaAttribute);
        section.set("unbreakable", unbreakable);

        if (customModelData != null) section.set("custom-model-data", customModelData);

        setIfNonZero(section, "str", strGear);
        setIfNonZero(section, "agi", agiGear);
        setIfNonZero(section, "vit", vitGear);
        setIfNonZero(section, "int", intGear);
        setIfNonZero(section, "dex", dexGear);
        setIfNonZero(section, "luk", lukGear);

        setIfNonZero(section, "max-hp-%", maxHPPercent);
        setIfNonZero(section, "max-sp-%", maxSPPercent);
        setIfNonZero(section, "hit-flat", hitFlat);
        setIfNonZero(section, "flee-flat", fleeFlat);
        setIfNonZero(section, "base-mspd", baseMSPD);

        setIfNonZero(section, "weapon-p-atk", weaponPAtk);
        setIfNonZero(section, "weapon-m-atk", weaponMAtk);
        setIfNonZero(section, "p-atk-flat", pAtkFlat);
        setIfNonZero(section, "m-atk-flat", mAtkFlat);

        setIfNonZero(section, "p-pen-flat", pPenFlat);
        setIfNonZero(section, "p-pen-%", pPenPercent);
        setIfNonZero(section, "ignore-p-def-flat", ignorePDefFlat);
        setIfNonZero(section, "ignore-p-def-%", ignorePDefPercent);
        setIfNonZero(section, "m-pen-flat", mPenFlat);
        setIfNonZero(section, "m-pen-%", mPenPercent);
        setIfNonZero(section, "ignore-m-def-flat", ignoreMDefFlat);
        setIfNonZero(section, "ignore-m-def-%", ignoreMDefPercent);

        setIfNonZero(section, "var-ct-%", varCTPercent);
        setIfNonZero(section, "var-ct-flat", varCTFlat);
        setIfNonZero(section, "fixed-ct-%", fixedCTPercent);
        setIfNonZero(section, "fixed-ct-flat", fixedCTFlat);
        setIfNonZero(section, "skill-cd-%", skillCooldownPercent);
        setIfNonZero(section, "skill-cd-flat", skillCooldownFlat);

        setIfNonZero(section, "global-cd-%", globalCooldownPercent);
        setIfNonZero(section, "global-cd-flat", globalCooldownFlat);
        setIfNonZero(section, "acd-%", acdPercent);
        setIfNonZero(section, "acd-flat", acdFlat);

        setIfNonZero(section, "aspd-%", aSpdPercent);
        setIfNonZero(section, "mspd-%", mSpdPercent);

        setIfNonZero(section, "crit-rate", critRate);
        setIfNonZero(section, "crit-dmg-%", critDmgPercent);
        setIfNonZero(section, "crit-res", critRes);
        setIfNonZero(section, "crit-dmg-res-%", critDmgResPercent);

        setIfNonZero(section, "p-dmg-%", pDmgPercent);
        setIfNonZero(section, "p-dmg-flat", pDmgFlat);
        setIfNonZero(section, "p-dmg-reduce-%", pDmgReductionPercent);
        setIfNonZero(section, "m-dmg-%", mDmgPercent);
        setIfNonZero(section, "m-dmg-flat", mDmgFlat);
        setIfNonZero(section, "m-dmg-reduce-%", mDmgReductionPercent);
        setIfNonZero(section, "final-dmg-%", finalDmgPercent);
        setIfNonZero(section, "final-p-dmg-%", finalPDmgPercent);
        setIfNonZero(section, "final-m-dmg-%", finalMDmgPercent);
        setIfNonZero(section, "final-dmg-res-%", finalDmgResPercent);
        setIfNonZero(section, "true-damage", trueDamageFlat);

        setIfNonZero(section, "melee-p-dmg-%", meleePDmgPercent);
        setIfNonZero(section, "melee-p-reduce-%", meleePDReductionPercent);
        setIfNonZero(section, "range-p-dmg-%", rangePDmgPercent);
        setIfNonZero(section, "range-p-reduce-%", rangePDReductionPercent);
        setIfNonZero(section, "pve-dmg-%", pveDmgPercent);
        setIfNonZero(section, "pve-reduce-%", pveDmgReductionPercent);
        setIfNonZero(section, "pvp-dmg-%", pvpDmgPercent);
        setIfNonZero(section, "pvp-reduce-%", pvpDmgReductionPercent);

        setIfNonZero(section, "heal-effect-%", healingEffectPercent);
        setIfNonZero(section, "heal-received-%", healingReceivedPercent);
        setIfNonZero(section, "lifesteal-p-%", lifestealPPercent);
        setIfNonZero(section, "lifesteal-m-%", lifestealMPercent);
        setIfNonZero(section, "shield-flat", shieldValueFlat);
        setIfNonZero(section, "shield-rate-%", shieldRatePercent);

        // Elements
        if (attackElement != -1) section.set("atk-element", attackElement);
        if (defenseElement != -1) section.set("def-element", defenseElement);

        if (!potionEffects.isEmpty()) {
            ConfigurationSection effectsSec = section.createSection("effects");
            potionEffects.forEach((k, v) -> effectsSec.set(k.getName(), v));
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

    private static void setIfNonZero(ConfigurationSection s, String k, double v) {
        if (v != 0) s.set(k, v);
    }

    // Getters and Setters
    public int getStrGear() { return strGear; }
    public void setStrGear(int v) { this.strGear = v; }
    public int getAgiGear() { return agiGear; }
    public void setAgiGear(int v) { this.agiGear = v; }
    public int getVitGear() { return vitGear; }
    public void setVitGear(int v) { this.vitGear = v; }
    public int getIntGear() { return intGear; }
    public void setIntGear(int v) { this.intGear = v; }
    public int getDexGear() { return dexGear; }
    public void setDexGear(int v) { this.dexGear = v; }
    public int getLukGear() { return lukGear; }
    public void setLukGear(int v) { this.lukGear = v; }
    public double getMaxHPPercent() { return maxHPPercent; }
    public void setMaxHPPercent(double v) { this.maxHPPercent = v; }
    public double getMaxSPPercent() { return maxSPPercent; }
    public void setMaxSPPercent(double v) { this.maxSPPercent = v; }
    public double getHitFlat() { return hitFlat; }
    public void setHitFlat(double v) { this.hitFlat = v; }
    public double getFleeFlat() { return fleeFlat; }
    public void setFleeFlat(double v) { this.fleeFlat = v; }
    public double getBaseMSPD() { return baseMSPD; }
    public void setBaseMSPD(double v) { this.baseMSPD = v; }
    public double getWeaponPAtk() { return weaponPAtk; }
    public void setWeaponPAtk(double v) { this.weaponPAtk = v; }
    public double getWeaponMAtk() { return weaponMAtk; }
    public void setWeaponMAtk(double v) { this.weaponMAtk = v; }
    public double getPAtkFlat() { return pAtkFlat; }
    public void setPAtkFlat(double v) { this.pAtkFlat = v; }
    public double getMAtkFlat() { return mAtkFlat; }
    public void setMAtkFlat(double v) { this.mAtkFlat = v; }
    public double getPPenFlat() { return pPenFlat; }
    public void setPPenFlat(double v) { this.pPenFlat = v; }
    public double getPPenPercent() { return pPenPercent; }
    public void setPPenPercent(double v) { this.pPenPercent = v; }
    public double getIgnorePDefFlat() { return ignorePDefFlat; }
    public void setIgnorePDefFlat(double v) { this.ignorePDefFlat = v; }
    public double getIgnorePDefPercent() { return ignorePDefPercent; }
    public void setIgnorePDefPercent(double v) { this.ignorePDefPercent = v; }
    public double getMPenFlat() { return mPenFlat; }
    public void setMPenFlat(double v) { this.mPenFlat = v; }
    public double getMPenPercent() { return mPenPercent; }
    public void setMPenPercent(double v) { this.mPenPercent = v; }
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
    public double getSkillCooldownPercent() { return skillCooldownPercent; }
    public void setSkillCooldownPercent(double v) { this.skillCooldownPercent = v; }
    public double getSkillCooldownFlat() { return skillCooldownFlat; }
    public void setSkillCooldownFlat(double v) { this.skillCooldownFlat = v; }
    public double getGlobalCooldownPercent() { return globalCooldownPercent; }
    public void setGlobalCooldownPercent(double v) { this.globalCooldownPercent = v; }
    public double getGlobalCooldownFlat() { return globalCooldownFlat; }
    public void setGlobalCooldownFlat(double v) { this.globalCooldownFlat = v; }
    public double getAcdPercent() { return acdPercent; }
    public void setAcdPercent(double v) { this.acdPercent = v; }
    public double getAcdFlat() { return acdFlat; }
    public void setAcdFlat(double v) { this.acdFlat = v; }
    public double getASpdPercent() { return aSpdPercent; }
    public void setASpdPercent(double v) { this.aSpdPercent = v; }
    public double getMSpdPercent() { return mSpdPercent; }
    public void setMSpdPercent(double v) { this.mSpdPercent = v; }
    public double getCritRate() { return critRate; }
    public void setCritRate(double v) { this.critRate = v; }
    public double getCritDmgPercent() { return critDmgPercent; }
    public void setCritDmgPercent(double v) { this.critDmgPercent = v; }
    public double getCritRes() { return critRes; }
    public void setCritRes(double v) { this.critRes = v; }
    public double getCritDmgResPercent() { return critDmgResPercent; }
    public void setCritDmgResPercent(double v) { this.critDmgResPercent = v; }
    public double getPDmgPercent() { return pDmgPercent; }
    public void setPDmgPercent(double v) { this.pDmgPercent = v; }
    public double getPDmgFlat() { return pDmgFlat; }
    public void setPDmgFlat(double v) { this.pDmgFlat = v; }
    public double getPDmgReductionPercent() { return pDmgReductionPercent; }
    public void setPDmgReductionPercent(double v) { this.pDmgReductionPercent = v; }
    public double getMDmgPercent() { return mDmgPercent; }
    public void setMDmgPercent(double v) { this.mDmgPercent = v; }
    public double getMDmgFlat() { return mDmgFlat; }
    public void setMDmgFlat(double v) { this.mDmgFlat = v; }
    public double getMDmgReductionPercent() { return mDmgReductionPercent; }
    public void setMDmgReductionPercent(double v) { this.mDmgReductionPercent = v; }
    public double getFinalDmgPercent() { return finalDmgPercent; }
    public void setFinalDmgPercent(double v) { this.finalDmgPercent = v; }
    public double getFinalPDmgPercent() { return finalPDmgPercent; }
    public void setFinalPDmgPercent(double v) { this.finalPDmgPercent = v; }
    public double getFinalMDmgPercent() { return finalMDmgPercent; }
    public void setFinalMDmgPercent(double v) { this.finalMDmgPercent = v; }
    public double getFinalDmgResPercent() { return finalDmgResPercent; }
    public void setFinalDmgResPercent(double v) { this.finalDmgResPercent = v; }
    public double getTrueDamageFlat() { return trueDamageFlat; }
    public void setTrueDamageFlat(double v) { this.trueDamageFlat = v; }
    public double getMeleePDmgPercent() { return meleePDmgPercent; }
    public void setMeleePDmgPercent(double v) { this.meleePDmgPercent = v; }
    public double getMeleePDReductionPercent() { return meleePDReductionPercent; }
    public void setMeleePDReductionPercent(double v) { this.meleePDReductionPercent = v; }
    public double getRangePDmgPercent() { return rangePDmgPercent; }
    public void setRangePDmgPercent(double v) { this.rangePDmgPercent = v; }
    public double getRangePDReductionPercent() { return rangePDReductionPercent; }
    public void setRangePDReductionPercent(double v) { this.rangePDReductionPercent = v; }
    public double getPveDmgPercent() { return pveDmgPercent; }
    public void setPveDmgPercent(double v) { this.pveDmgPercent = v; }
    public double getPveDmgReductionPercent() { return pveDmgReductionPercent; }
    public void setPveDmgReductionPercent(double v) { this.pveDmgReductionPercent = v; }
    public double getPvpDmgPercent() { return pvpDmgPercent; }
    public void setPvpDmgPercent(double v) { this.pvpDmgPercent = v; }
    public double getPvpDmgReductionPercent() { return pvpDmgReductionPercent; }
    public void setPvpDmgReductionPercent(double v) { this.pvpDmgReductionPercent = v; }
    public double getHealingEffectPercent() { return healingEffectPercent; }
    public void setHealingEffectPercent(double v) { this.healingEffectPercent = v; }
    public double getHealingReceivedPercent() { return healingReceivedPercent; }
    public void setHealingReceivedPercent(double v) { this.healingReceivedPercent = v; }
    public double getLifestealPPercent() { return lifestealPPercent; }
    public void setLifestealPPercent(double v) { this.lifestealPPercent = v; }
    public double getLifestealMPercent() { return lifestealMPercent; }
    public void setLifestealMPercent(double v) { this.lifestealMPercent = v; }
    public double getShieldValueFlat() { return shieldValueFlat; }
    public void setShieldValueFlat(double v) { this.shieldValueFlat = v; }
    public double getShieldRatePercent() { return shieldRatePercent; }
    public void setShieldRatePercent(double v) { this.shieldRatePercent = v; }
    public boolean isRemoveVanillaAttribute() { return removeVanillaAttribute; }
    public void setRemoveVanillaAttribute(boolean v) { this.removeVanillaAttribute = v; }
    public Integer getCustomModelData() { return customModelData; }
    public void setCustomModelData(Integer v) { this.customModelData = v; }
    public Map<PotionEffectType, Integer> getPotionEffects() { return potionEffects; }
    public void setPotionEffects(Map<PotionEffectType, Integer> v) { this.potionEffects = v; }
    public List<ItemSkillBinding> getSkillBindings() { return skillBindings; }
    public void setSkillBindings(List<ItemSkillBinding> v) { this.skillBindings = v; }
    public boolean isUnbreakable() { return unbreakable; }
    public void setUnbreakable(boolean v) { this.unbreakable = v; }

    public int getAttackElement() { return attackElement; }
    public void setAttackElement(int v) { this.attackElement = v; }
    public int getDefenseElement() { return defenseElement; }
    public void setDefenseElement(int v) { this.defenseElement = v; }
}