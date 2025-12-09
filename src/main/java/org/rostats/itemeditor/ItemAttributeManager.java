package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemAttributeManager {

    private final ThaiRoCorePlugin plugin;

    public ItemAttributeManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // --- NEW: Method to apply Name and Lore from Config ---
    public void applyMetaFromConfig(ItemStack item, YamlConfiguration config) {
        if (item == null || config == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (config.contains("name")) {
            meta.setDisplayName(config.getString("name").replace("&", "§"));
        }
        if (config.contains("lore")) {
            List<String> lore = config.getStringList("lore");
            List<String> coloredLore = new ArrayList<>();
            for (String l : lore) {
                coloredLore.add(l.replace("&", "§"));
            }
            meta.setLore(coloredLore);
        }
        item.setItemMeta(meta);
    }

    // --- NEW: Method to apply Attributes (CMD, Flags) to Item ---
    public void applyAttributesToItem(ItemStack item, ItemAttribute attr) {
        if (item == null || attr == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Apply Custom Model Data
        if (attr.getCustomModelData() != null) {
            meta.setCustomModelData(attr.getCustomModelData());
        }

        // Apply Hide Attributes Flag
        if (attr.isRemoveVanillaAttribute()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        item.setItemMeta(meta);
    }

    public ItemAttribute readFromItem(ItemStack item) {
        // In a file-based system, reading strictly from the item usually implies looking up its file
        // or parsing NBT if available. For the editor's context, we usually load from file.
        return new ItemAttribute();
    }

    public double getAttributeValueFromAttrObject(ItemAttribute attr, ItemAttributeType type) {
        if (attr == null || type == null) return 0;

        return switch (type) {
            // Base
            case STR_GEAR -> attr.getStrGear();
            case AGI_GEAR -> attr.getAgiGear();
            case VIT_GEAR -> attr.getVitGear();
            case INT_GEAR -> attr.getIntGear();
            case DEX_GEAR -> attr.getDexGear();
            case LUK_GEAR -> attr.getLukGear();
            case MAXHP_PERCENT -> attr.getMaxHPPercent();
            case MAXSP_PERCENT -> attr.getMaxSPPercent();
            case HIT_BONUS_FLAT -> attr.getHitFlat();
            case FLEE_BONUS_FLAT -> attr.getFleeFlat();
            case BASE_MSPD -> attr.getBaseMSPD();

            // Combat
            case WEAPON_PATK -> attr.getWeaponPAtk();
            case WEAPON_MATK -> attr.getWeaponMAtk();
            case PATK_FLAT -> attr.getPAtkFlat();
            case MATK_FLAT -> attr.getMAtkFlat();

            // Pen
            case P_PEN_FLAT -> attr.getPPenFlat();
            case P_PEN_PERCENT -> attr.getPPenPercent();
            case IGNORE_PDEF_FLAT -> attr.getIgnorePDefFlat();
            case IGNORE_PDEF_PERCENT -> attr.getIgnorePDefPercent();
            case M_PEN_FLAT -> attr.getMPenFlat();
            case M_PEN_PERCENT -> attr.getMPenPercent();
            case IGNORE_MDEF_FLAT -> attr.getIgnoreMDefFlat();
            case IGNORE_MDEF_PERCENT -> attr.getIgnoreMDefPercent();

            // Cast
            case VAR_CT_PERCENT -> attr.getVarCTPercent();
            case VAR_CT_FLAT -> attr.getVarCTFlat();
            case FIXED_CT_PERCENT -> attr.getFixedCTPercent();
            case FIXED_CT_FLAT -> attr.getFixedCTFlat();

            // Speed
            case ASPD_PERCENT -> attr.getASpdPercent();
            case MSPD_PERCENT -> attr.getMSpdPercent();

            // Crit
            case CRIT_RATE -> attr.getCritRate();
            case CRIT_DMG_PERCENT -> attr.getCritDmgPercent();
            case CRIT_RES -> attr.getCritRes();
            case CRIT_DMG_RES_PERCENT -> attr.getCritDmgResPercent();

            // Dmg Universal
            case PDMG_PERCENT -> attr.getPDmgPercent();
            case PDMG_FLAT -> attr.getPDmgFlat();
            case PDMG_REDUCTION_PERCENT -> attr.getPDmgReductionPercent();
            case MDMG_PERCENT -> attr.getMDmgPercent();
            case MDMG_FLAT -> attr.getMDmgFlat();
            case MDMG_REDUCTION_PERCENT -> attr.getMDmgReductionPercent();
            case FINAL_DMG_PERCENT -> attr.getFinalDmgPercent();
            case FINAL_PDMG_PERCENT -> attr.getFinalPDmgPercent();
            case FINAL_MDMG_PERCENT -> attr.getFinalMDmgPercent();
            case FINAL_DMG_RES_PERCENT -> attr.getFinalDmgResPercent();
            case TRUE_DMG -> attr.getTrueDamageFlat();

            // Distance
            case MELEE_PDMG_PERCENT -> attr.getMeleePDmgPercent();
            case MELEE_PDMG_REDUCTION_PERCENT -> attr.getMeleePDReductionPercent();
            case RANGE_PDMG_PERCENT -> attr.getRangePDmgPercent();
            case RANGE_PDMG_REDUCTION_PERCENT -> attr.getRangePDReductionPercent();

            // Content
            case PVE_DMG_PERCENT -> attr.getPveDmgPercent();
            case PVE_DMG_REDUCTION_PERCENT -> attr.getPveDmgReductionPercent();
            case PVP_DMG_PERCENT -> attr.getPvpDmgPercent();
            case PVP_DMG_REDUCTION_PERCENT -> attr.getPvpDmgReductionPercent();

            // Heal & Shield
            case HEALING_EFFECT_PERCENT -> attr.getHealingEffectPercent();
            case HEALING_RECEIVED_PERCENT -> attr.getHealingReceivedPercent();
            case LIFESTEAL_P_PERCENT -> attr.getLifestealPPercent();
            case LIFESTEAL_M_PERCENT -> attr.getLifestealMPercent();
            case SHIELD_VALUE_FLAT -> attr.getShieldValueFlat();
            case SHIELD_RATE_PERCENT -> attr.getShieldRatePercent();

            default -> 0.0;
        };
    }

    public void setAttributeToObj(ItemAttribute attr, ItemAttributeType type, double value) {
        if (attr == null || type == null) return;

        switch (type) {
            case STR_GEAR -> attr.setStrGear((int) value);
            case AGI_GEAR -> attr.setAgiGear((int) value);
            case VIT_GEAR -> attr.setVitGear((int) value);
            case INT_GEAR -> attr.setIntGear((int) value);
            case DEX_GEAR -> attr.setDexGear((int) value);
            case LUK_GEAR -> attr.setLukGear((int) value);
            case MAXHP_PERCENT -> attr.setMaxHPPercent(value);
            case MAXSP_PERCENT -> attr.setMaxSPPercent(value);
            case HIT_BONUS_FLAT -> attr.setHitFlat(value);
            case FLEE_BONUS_FLAT -> attr.setFleeFlat(value);
            case BASE_MSPD -> attr.setBaseMSPD(value);

            case WEAPON_PATK -> attr.setWeaponPAtk(value);
            case WEAPON_MATK -> attr.setWeaponMAtk(value);
            case PATK_FLAT -> attr.setPAtkFlat(value);
            case MATK_FLAT -> attr.setMAtkFlat(value);

            case P_PEN_FLAT -> attr.setPPenFlat(value);
            case P_PEN_PERCENT -> attr.setPPenPercent(value);
            case IGNORE_PDEF_FLAT -> attr.setIgnorePDefFlat(value);
            case IGNORE_PDEF_PERCENT -> attr.setIgnorePDefPercent(value);
            case M_PEN_FLAT -> attr.setMPenFlat(value);
            case M_PEN_PERCENT -> attr.setMPenPercent(value);
            case IGNORE_MDEF_FLAT -> attr.setIgnoreMDefFlat(value);
            case IGNORE_MDEF_PERCENT -> attr.setIgnoreMDefPercent(value);

            case VAR_CT_PERCENT -> attr.setVarCTPercent(value);
            case VAR_CT_FLAT -> attr.setVarCTFlat(value);
            case FIXED_CT_PERCENT -> attr.setFixedCTPercent(value);
            case FIXED_CT_FLAT -> attr.setFixedCTFlat(value);

            case ASPD_PERCENT -> attr.setASpdPercent(value);
            case MSPD_PERCENT -> attr.setMSpdPercent(value);

            case CRIT_RATE -> attr.setCritRate(value);
            case CRIT_DMG_PERCENT -> attr.setCritDmgPercent(value);
            case CRIT_RES -> attr.setCritRes(value);
            case CRIT_DMG_RES_PERCENT -> attr.setCritDmgResPercent(value);

            case PDMG_PERCENT -> attr.setPDmgPercent(value);
            case PDMG_FLAT -> attr.setPDmgFlat(value);
            case PDMG_REDUCTION_PERCENT -> attr.setPDmgReductionPercent(value);
            case MDMG_PERCENT -> attr.setMDmgPercent(value);
            case MDMG_FLAT -> attr.setMDmgFlat(value);
            case MDMG_REDUCTION_PERCENT -> attr.setMDmgReductionPercent(value);
            case FINAL_DMG_PERCENT -> attr.setFinalDmgPercent(value);
            case FINAL_PDMG_PERCENT -> attr.setFinalPDmgPercent(value);
            case FINAL_MDMG_PERCENT -> attr.setFinalMDmgPercent(value);
            case FINAL_DMG_RES_PERCENT -> attr.setFinalDmgResPercent(value);
            case TRUE_DMG -> attr.setTrueDamageFlat(value);

            case MELEE_PDMG_PERCENT -> attr.setMeleePDmgPercent(value);
            case MELEE_PDMG_REDUCTION_PERCENT -> attr.setMeleePDReductionPercent(value);
            case RANGE_PDMG_PERCENT -> attr.setRangePDmgPercent(value);
            case RANGE_PDMG_REDUCTION_PERCENT -> attr.setRangePDReductionPercent(value);

            case PVE_DMG_PERCENT -> attr.setPveDmgPercent(value);
            case PVE_DMG_REDUCTION_PERCENT -> attr.setPveDmgReductionPercent(value);
            case PVP_DMG_PERCENT -> attr.setPvpDmgPercent(value);
            case PVP_DMG_REDUCTION_PERCENT -> attr.setPvpDmgReductionPercent(value);

            case HEALING_EFFECT_PERCENT -> attr.setHealingEffectPercent(value);
            case HEALING_RECEIVED_PERCENT -> attr.setHealingReceivedPercent(value);
            case LIFESTEAL_P_PERCENT -> attr.setLifestealPPercent(value);
            case LIFESTEAL_M_PERCENT -> attr.setLifestealMPercent(value);
            case SHIELD_VALUE_FLAT -> attr.setShieldValueFlat(value);
            case SHIELD_RATE_PERCENT -> attr.setShieldRatePercent(value);
        }
    }
}