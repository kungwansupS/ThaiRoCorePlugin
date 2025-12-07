package org.rostats.itemeditor;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

/**
 * Utility class to load ItemAttribute POJO from a YAML ConfigurationSection.
 */
public class ItemAttributeLoader {

    /**
     * Loads a single ItemAttribute object from a ConfigurationSection.
     * @param attributesSection The YAML section containing attribute keys and values.
     * @return A fully populated ItemAttribute object.
     */
    public static ItemAttribute load(ConfigurationSection attributesSection) {
        ItemAttribute attr = new ItemAttribute();

        if (attributesSection == null) {
            return attr;
        }

        // --- Helper to safely load double values ---
        attr.setStrGear(attributesSection.getInt("str", 0));
        attr.setAgiGear(attributesSection.getInt("agi", 0));
        attr.setVitGear(attributesSection.getInt("vit", 0));
        attr.setIntGear(attributesSection.getInt("int", 0));
        attr.setDexGear(attributesSection.getInt("dex", 0));
        attr.setLukGear(attributesSection.getInt("luk", 0));

        attr.setWeaponPAtk(attributesSection.getDouble("weapon-p-atk", 0.0));
        attr.setWeaponMAtk(attributesSection.getDouble("weapon-m-atk", 0.0));
        attr.setPAtkFlat(attributesSection.getDouble("p-atk-flat", 0.0));
        attr.setMAtkFlat(attributesSection.getDouble("m-atk-flat", 0.0));

        attr.setPDmgPercent(attributesSection.getDouble("p-dmg-%", 0.0));
        attr.setMDmgPercent(attributesSection.getDouble("m-dmg-%", 0.0));
        attr.setPDmgFlat(attributesSection.getDouble("p-dmg-flat", 0.0));
        attr.setMDmgFlat(attributesSection.getDouble("m-dmg-flat", 0.0));

        attr.setCritDmgPercent(attributesSection.getDouble("crit-dmg-%", 0.0));
        attr.setCritDmgResPercent(attributesSection.getDouble("crit-dmg-res-%", 0.0));
        attr.setCritRes(attributesSection.getDouble("crit-res", 0.0));

        attr.setPPenFlat(attributesSection.getDouble("p-pen-flat", 0.0));
        attr.setMPenFlat(attributesSection.getDouble("m-pen-flat", 0.0));
        attr.setPPenPercent(attributesSection.getDouble("p-pen-%", 0.0));
        attr.setMPenPercent(attributesSection.getDouble("m-pen-%", 0.0));

        attr.setFinalDmgPercent(attributesSection.getDouble("final-dmg-%", 0.0));
        attr.setFinalDmgResPercent(attributesSection.getDouble("final-dmg-res-%", 0.0));
        attr.setFinalPDmgPercent(attributesSection.getDouble("final-p-dmg-%", 0.0));
        attr.setFinalMDmgPercent(attributesSection.getDouble("final-m-dmg-%", 0.0));

        attr.setPveDmgPercent(attributesSection.getDouble("pve-dmg-%", 0.0));
        attr.setPvpDmgPercent(attributesSection.getDouble("pvp-dmg-%", 0.0));
        attr.setPveDmgReductionPercent(attributesSection.getDouble("pve-dmg-reduce-%", 0.0));
        attr.setPvpDmgReductionPercent(attributesSection.getDouble("pvp-dmg-reduce-%", 0.0));

        attr.setMaxHPPercent(attributesSection.getDouble("max-hp-%", 0.0));
        attr.setMaxSPPercent(attributesSection.getDouble("max-sp-%", 0.0));

        attr.setShieldValueFlat(attributesSection.getDouble("shield-value-flat", 0.0));
        attr.setShieldRatePercent(attributesSection.getDouble("shield-rate-%", 0.0));

        attr.setASpdPercent(attributesSection.getDouble("aspd-%", 0.0));
        attr.setMSpdPercent(attributesSection.getDouble("mspd-%", 0.0));
        attr.setBaseMSPD(attributesSection.getDouble("base-mspd", 0.0));

        attr.setVarCTPercent(attributesSection.getDouble("var-ct-%", 0.0));
        attr.setVarCTFlat(attributesSection.getDouble("var-ct-flat", 0.0));
        attr.setFixedCTPercent(attributesSection.getDouble("fixed-ct-%", 0.0));
        attr.setFixedCTFlat(attributesSection.getDouble("fixed-ct-flat", 0.0));

        attr.setHealingEffectPercent(attributesSection.getDouble("healing-effect-%", 0.0));
        attr.setHealingReceivedPercent(attributesSection.getDouble("healing-receive-%", 0.0));

        attr.setLifestealPPercent(attributesSection.getDouble("lifesteal-p-%", 0.0));
        attr.setLifestealMPercent(attributesSection.getDouble("lifesteal-m-%", 0.0));

        attr.setHitFlat(attributesSection.getDouble("hit-flat", 0.0));
        attr.setFleeFlat(attributesSection.getDouble("flee-flat", 0.0));

        // Utility: Must check the top level key for this
        ConfigurationSection itemSection = attributesSection.getParent();
        attr.setRemoveVanillaAttribute(itemSection.getBoolean("remove-vanilla", false));

        return attr;
    }
}