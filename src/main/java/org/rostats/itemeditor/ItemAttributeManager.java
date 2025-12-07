package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.attribute.Attribute;
import org.rostats.ThaiRoCorePlugin;

import java.util.ArrayList;
import java.util.List;

public class ItemAttributeManager {

    private final ThaiRoCorePlugin plugin;

    public ItemAttributeManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        for (ItemAttributeType type : ItemAttributeType.values()) {
            type.initialize(plugin);
        }
    }

    public ItemStack removeVanillaAttributes(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
        meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    // Read full object from item
    public ItemAttribute readFromItem(ItemStack item) {
        ItemAttribute attr = new ItemAttribute();
        if (item == null || !item.hasItemMeta()) return attr;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        // Core
        attr.setStrGear(getInt(pdc, ItemAttributeType.STR_GEAR));
        attr.setAgiGear(getInt(pdc, ItemAttributeType.AGI_GEAR));
        attr.setVitGear(getInt(pdc, ItemAttributeType.VIT_GEAR));
        attr.setIntGear(getInt(pdc, ItemAttributeType.INT_GEAR));
        attr.setDexGear(getInt(pdc, ItemAttributeType.DEX_GEAR));
        attr.setLukGear(getInt(pdc, ItemAttributeType.LUK_GEAR));

        // Offense
        attr.setWeaponPAtk(getDouble(pdc, ItemAttributeType.WEAPON_PATK));
        attr.setWeaponMAtk(getDouble(pdc, ItemAttributeType.WEAPON_MATK));
        attr.setPAtkFlat(getDouble(pdc, ItemAttributeType.PATK_FLAT));
        attr.setMAtkFlat(getDouble(pdc, ItemAttributeType.MATK_FLAT));
        attr.setPDmgPercent(getDouble(pdc, ItemAttributeType.PDMG_PERCENT));
        attr.setMDmgPercent(getDouble(pdc, ItemAttributeType.MDMG_PERCENT));
        attr.setPDmgFlat(getDouble(pdc, ItemAttributeType.PDMG_FLAT));
        attr.setMDmgFlat(getDouble(pdc, ItemAttributeType.MDMG_FLAT));
        attr.setCritDmgPercent(getDouble(pdc, ItemAttributeType.CRIT_DMG_PERCENT));
        attr.setCritDmgResPercent(getDouble(pdc, ItemAttributeType.CRIT_DMG_RES_PERCENT));
        attr.setCritRes(getDouble(pdc, ItemAttributeType.CRIT_RES));
        attr.setPPenFlat(getDouble(pdc, ItemAttributeType.P_PEN_FLAT));
        attr.setMPenFlat(getDouble(pdc, ItemAttributeType.M_PEN_FLAT));
        attr.setPPenPercent(getDouble(pdc, ItemAttributeType.P_PEN_PERCENT));
        attr.setMPenPercent(getDouble(pdc, ItemAttributeType.M_PEN_PERCENT));
        attr.setFinalDmgPercent(getDouble(pdc, ItemAttributeType.FINAL_DMG_PERCENT));
        attr.setFinalDmgResPercent(getDouble(pdc, ItemAttributeType.FINAL_DMG_RES_PERCENT));
        attr.setFinalPDmgPercent(getDouble(pdc, ItemAttributeType.FINAL_PDMG_PERCENT));
        attr.setFinalMDmgPercent(getDouble(pdc, ItemAttributeType.FINAL_MDMG_PERCENT));
        attr.setPveDmgPercent(getDouble(pdc, ItemAttributeType.PVE_DMG_PERCENT));
        attr.setPvpDmgPercent(getDouble(pdc, ItemAttributeType.PVP_DMG_PERCENT));
        attr.setPveDmgReductionPercent(getDouble(pdc, ItemAttributeType.PVE_DMG_REDUCTION_PERCENT));
        attr.setPvpDmgReductionPercent(getDouble(pdc, ItemAttributeType.PVP_DMG_REDUCTION_PERCENT));

        // Defense / Misc
        attr.setMaxHPPercent(getDouble(pdc, ItemAttributeType.MAXHP_PERCENT));
        attr.setMaxSPPercent(getDouble(pdc, ItemAttributeType.MAXSP_PERCENT));
        attr.setShieldValueFlat(getDouble(pdc, ItemAttributeType.SHIELD_VALUE_FLAT));
        attr.setShieldRatePercent(getDouble(pdc, ItemAttributeType.SHIELD_RATE_PERCENT));
        attr.setASpdPercent(getDouble(pdc, ItemAttributeType.ASPD_PERCENT));
        attr.setMSpdPercent(getDouble(pdc, ItemAttributeType.MSPD_PERCENT));
        attr.setBaseMSPD(getDouble(pdc, ItemAttributeType.BASE_MSPD));
        attr.setVarCTPercent(getDouble(pdc, ItemAttributeType.VAR_CT_PERCENT));
        attr.setVarCTFlat(getDouble(pdc, ItemAttributeType.VAR_CT_FLAT));
        attr.setFixedCTPercent(getDouble(pdc, ItemAttributeType.FIXED_CT_PERCENT));
        attr.setFixedCTFlat(getDouble(pdc, ItemAttributeType.FIXED_CT_FLAT));
        attr.setHealingEffectPercent(getDouble(pdc, ItemAttributeType.HEALING_EFFECT_PERCENT));
        attr.setHealingReceivedPercent(getDouble(pdc, ItemAttributeType.HEALING_RECEIVED_PERCENT));
        attr.setLifestealPPercent(getDouble(pdc, ItemAttributeType.LIFESTEAL_P_PERCENT));
        attr.setLifestealMPercent(getDouble(pdc, ItemAttributeType.LIFESTEAL_M_PERCENT));
        attr.setHitFlat(getDouble(pdc, ItemAttributeType.HIT_BONUS_FLAT));
        attr.setFleeFlat(getDouble(pdc, ItemAttributeType.FLEE_BONUS_FLAT));
        attr.setPDmgReductionPercent(getDouble(pdc, ItemAttributeType.PDMG_REDUCTION_PERCENT));
        attr.setMDmgReductionPercent(getDouble(pdc, ItemAttributeType.MDMG_REDUCTION_PERCENT));

        return attr;
    }

    // Apply single change to item (Used by GUI)
    public void setAttribute(ItemStack item, ItemAttributeType type, double value) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (value == 0.0) {
            pdc.remove(type.getNamespacedKey());
        } else {
            pdc.set(type.getNamespacedKey(), PersistentDataType.DOUBLE, value);
        }
        item.setItemMeta(meta);
        updateLore(item);
    }

    public double getAttributeValue(ItemStack item, ItemAttributeType type) {
        if (item == null || !item.hasItemMeta()) return 0.0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(type.getNamespacedKey(), PersistentDataType.DOUBLE, 0.0);
    }

    private double getDouble(PersistentDataContainer pdc, ItemAttributeType type) {
        return pdc.getOrDefault(type.getNamespacedKey(), PersistentDataType.DOUBLE, 0.0);
    }

    private int getInt(PersistentDataContainer pdc, ItemAttributeType type) {
        return pdc.getOrDefault(type.getNamespacedKey(), PersistentDataType.DOUBLE, 0.0).intValue();
    }

    public void updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();

        lore.add("§f§l--- Item Stats ---");
        for (ItemAttributeType type : ItemAttributeType.values()) {
            double val = getAttributeValue(item, type);
            if (val != 0) {
                lore.add(type.getDisplayName() + ": §f" + String.format(type.getFormat(), val));
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}