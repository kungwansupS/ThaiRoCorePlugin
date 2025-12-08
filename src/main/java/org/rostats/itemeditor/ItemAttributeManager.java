package org.rostats.itemeditor;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.trigger.TriggerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemAttributeManager {

    private final ThaiRoCorePlugin plugin;

    public ItemAttributeManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        for (ItemAttributeType type : ItemAttributeType.values()) {
            type.initialize(plugin);
        }
    }

    public ItemAttribute readFromItem(ItemStack item) {
        ItemAttribute attr = new ItemAttribute();
        if (item == null || !item.hasItemMeta()) return attr;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (ItemAttributeType type : ItemAttributeType.values()) {
            double value = getDoubleFromPDC(pdc, type);
            if (value != 0) {
                setAttributeToObj(attr, type, value);
            }
        }

        if (meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) {
            attr.setRemoveVanillaAttribute(true);
        }

        if (meta.hasCustomModelData()) {
            attr.setCustomModelData(meta.getCustomModelData());
        }

        // Read Potion Effects
        NamespacedKey effectKey = new NamespacedKey(plugin, "RO_EFFECTS");
        if (pdc.has(effectKey, PersistentDataType.STRING)) {
            String encoded = pdc.get(effectKey, PersistentDataType.STRING);
            if (encoded != null && !encoded.isEmpty()) {
                String[] parts = encoded.split(",");
                for (String part : parts) {
                    String[] pair = part.split(":");
                    if (pair.length == 2) {
                        PotionEffectType type = PotionEffectType.getByName(pair[0]);
                        try {
                            int lvl = Integer.parseInt(pair[1]);
                            if (type != null) attr.getPotionEffects().put(type, lvl);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        // NEW: Read Skill Bindings from PDC
        NamespacedKey skillKey = new NamespacedKey(plugin, "RO_SKILLS");
        if (pdc.has(skillKey, PersistentDataType.STRING)) {
            String encoded = pdc.get(skillKey, PersistentDataType.STRING);
            if (encoded != null && !encoded.isEmpty()) {
                String[] parts = encoded.split(",");
                for (String part : parts) {
                    // Format: skillId:TRIGGER:level:chance
                    String[] d = part.split(":");
                    if (d.length == 4) {
                        try {
                            String skillId = d[0];
                            TriggerType trigger = TriggerType.valueOf(d[1]);
                            int lvl = Integer.parseInt(d[2]);
                            double chance = Double.parseDouble(d[3]);
                            attr.getSkillBindings().add(new ItemSkillBinding(skillId, trigger, lvl, chance));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        return attr;
    }

    private double getDoubleFromPDC(PersistentDataContainer pdc, ItemAttributeType type) {
        if (pdc.has(type.getNamespacedKey(), PersistentDataType.DOUBLE)) {
            return pdc.get(type.getNamespacedKey(), PersistentDataType.DOUBLE);
        }
        if (pdc.has(type.getNamespacedKey(), PersistentDataType.INTEGER)) {
            return pdc.get(type.getNamespacedKey(), PersistentDataType.INTEGER).doubleValue();
        }
        return 0.0;
    }

    public void applyAttributesToItem(ItemStack item, ItemAttribute attr) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (ItemAttributeType type : ItemAttributeType.values()) {
            double value = getAttributeValueFromAttrObject(attr, type);
            if (value != 0) {
                pdc.set(type.getNamespacedKey(), PersistentDataType.DOUBLE, value);
            } else {
                pdc.remove(type.getNamespacedKey());
            }
        }

        // Apply Potion Effects
        NamespacedKey effectKey = new NamespacedKey(plugin, "RO_EFFECTS");
        if (!attr.getPotionEffects().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<PotionEffectType, Integer> entry : attr.getPotionEffects().entrySet()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(entry.getKey().getName()).append(":").append(entry.getValue());
            }
            pdc.set(effectKey, PersistentDataType.STRING, sb.toString());
        } else {
            pdc.remove(effectKey);
        }

        // NEW: Apply Skill Bindings to PDC
        NamespacedKey skillKey = new NamespacedKey(plugin, "RO_SKILLS");
        if (!attr.getSkillBindings().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ItemSkillBinding binding : attr.getSkillBindings()) {
                if (sb.length() > 0) sb.append(",");
                // Format: skillId:TRIGGER:level:chance
                sb.append(binding.getSkillId()).append(":")
                        .append(binding.getTrigger().name()).append(":")
                        .append(binding.getLevel()).append(":")
                        .append(binding.getChance());
            }
            pdc.set(skillKey, PersistentDataType.STRING, sb.toString());
        } else {
            pdc.remove(skillKey);
        }

        if (attr.isRemoveVanillaAttribute()) {
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_SPEED);
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        if (attr.getCustomModelData() != null && attr.getCustomModelData() != 0) {
            meta.setCustomModelData(attr.getCustomModelData());
        }

        item.setItemMeta(meta);
        updateLore(item);
    }

    public void applyMetaFromConfig(ItemStack item, YamlConfiguration config) {
        ItemMeta meta = item.getItemMeta();
        if (config.contains("name")) meta.setDisplayName(config.getString("name").replace("&", "§"));
        if (config.contains("lore")) {
            List<String> lore = config.getStringList("lore");
            lore.replaceAll(s -> s.replace("&", "§"));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
    }

    public void setAttributeToObj(ItemAttribute attr, ItemAttributeType type, double val) {
        // ... (คงเดิม) ...
        switch (type) {
            case STR_GEAR: attr.setStrGear((int)val); break;
            case AGI_GEAR: attr.setAgiGear((int)val); break;
            case VIT_GEAR: attr.setVitGear((int)val); break;
            case INT_GEAR: attr.setIntGear((int)val); break;
            case DEX_GEAR: attr.setDexGear((int)val); break;
            case LUK_GEAR: attr.setLukGear((int)val); break;
            case WEAPON_PATK: attr.setWeaponPAtk(val); break;
            case WEAPON_MATK: attr.setWeaponMAtk(val); break;
            case PATK_FLAT: attr.setPAtkFlat(val); break;
            case MATK_FLAT: attr.setMAtkFlat(val); break;
            case PDMG_PERCENT: attr.setPDmgPercent(val); break;
            case MDMG_PERCENT: attr.setMDmgPercent(val); break;
            case PDMG_FLAT: attr.setPDmgFlat(val); break;
            case MDMG_FLAT: attr.setMDmgFlat(val); break;
            case CRIT_DMG_PERCENT: attr.setCritDmgPercent(val); break;
            case CRIT_DMG_RES_PERCENT: attr.setCritDmgResPercent(val); break;
            case CRIT_RES: attr.setCritRes(val); break;
            case P_PEN_FLAT: attr.setPPenFlat(val); break;
            case M_PEN_FLAT: attr.setMPenFlat(val); break;
            case P_PEN_PERCENT: attr.setPPenPercent(val); break;
            case M_PEN_PERCENT: attr.setMPenPercent(val); break;
            case FINAL_DMG_PERCENT: attr.setFinalDmgPercent(val); break;
            case FINAL_DMG_RES_PERCENT: attr.setFinalDmgResPercent(val); break;
            case FINAL_PDMG_PERCENT: attr.setFinalPDmgPercent(val); break;
            case FINAL_MDMG_PERCENT: attr.setFinalMDmgPercent(val); break;
            case PVE_DMG_PERCENT: attr.setPveDmgPercent(val); break;
            case PVP_DMG_PERCENT: attr.setPvpDmgPercent(val); break;
            case PVE_DMG_REDUCTION_PERCENT: attr.setPveDmgReductionPercent(val); break;
            case PVP_DMG_REDUCTION_PERCENT: attr.setPvpDmgReductionPercent(val); break;
            case MAXHP_PERCENT: attr.setMaxHPPercent(val); break;
            case MAXSP_PERCENT: attr.setMaxSPPercent(val); break;
            case SHIELD_VALUE_FLAT: attr.setShieldValueFlat(val); break;
            case SHIELD_RATE_PERCENT: attr.setShieldRatePercent(val); break;
            case ASPD_PERCENT: attr.setASpdPercent(val); break;
            case MSPD_PERCENT: attr.setMSpdPercent(val); break;
            case BASE_MSPD: attr.setBaseMSPD(val); break;
            case VAR_CT_PERCENT: attr.setVarCTPercent(val); break;
            case VAR_CT_FLAT: attr.setVarCTFlat(val); break;
            case FIXED_CT_PERCENT: attr.setFixedCTPercent(val); break;
            case FIXED_CT_FLAT: attr.setFixedCTFlat(val); break;
            case HEALING_EFFECT_PERCENT: attr.setHealingEffectPercent(val); break;
            case HEALING_RECEIVED_PERCENT: attr.setHealingReceivedPercent(val); break;
            case LIFESTEAL_P_PERCENT: attr.setLifestealPPercent(val); break;
            case LIFESTEAL_M_PERCENT: attr.setLifestealMPercent(val); break;
            case HIT_BONUS_FLAT: attr.setHitFlat(val); break;
            case FLEE_BONUS_FLAT: attr.setFleeFlat(val); break;
            case PDMG_REDUCTION_PERCENT: attr.setPDmgReductionPercent(val); break;
            case MDMG_REDUCTION_PERCENT: attr.setMDmgReductionPercent(val); break;
            case IGNORE_PDEF_FLAT: attr.setIgnorePDefFlat(val); break;
            case IGNORE_MDEF_FLAT: attr.setIgnoreMDefFlat(val); break;
            case IGNORE_PDEF_PERCENT: attr.setIgnorePDefPercent(val); break;
            case IGNORE_MDEF_PERCENT: attr.setIgnoreMDefPercent(val); break;
            case MELEE_PDMG_PERCENT: attr.setMeleePDmgPercent(val); break;
            case RANGE_PDMG_PERCENT: attr.setRangePDmgPercent(val); break;
            case MELEE_PDMG_REDUCTION_PERCENT: attr.setMeleePDReductionPercent(val); break;
            case RANGE_PDMG_REDUCTION_PERCENT: attr.setRangePDReductionPercent(val); break;
            case TRUE_DMG: attr.setTrueDamageFlat(val); break;
        }
    }

    public double getAttributeValueFromAttrObject(ItemAttribute attr, ItemAttributeType type) {
        // ... (คงเดิม) ...
        switch (type) {
            case STR_GEAR: return attr.getStrGear();
            case AGI_GEAR: return attr.getAgiGear();
            case VIT_GEAR: return attr.getVitGear();
            case INT_GEAR: return attr.getIntGear();
            case DEX_GEAR: return attr.getDexGear();
            case LUK_GEAR: return attr.getLukGear();
            case WEAPON_PATK: return attr.getWeaponPAtk();
            case WEAPON_MATK: return attr.getWeaponMAtk();
            case PATK_FLAT: return attr.getPAtkFlat();
            case MATK_FLAT: return attr.getMAtkFlat();
            case PDMG_PERCENT: return attr.getPDmgPercent();
            case MDMG_PERCENT: return attr.getMDmgPercent();
            case PDMG_FLAT: return attr.getPDmgFlat();
            case MDMG_FLAT: return attr.getMDmgFlat();
            case CRIT_DMG_PERCENT: return attr.getCritDmgPercent();
            case CRIT_DMG_RES_PERCENT: return attr.getCritDmgResPercent();
            case CRIT_RES: return attr.getCritRes();
            case P_PEN_FLAT: return attr.getPPenFlat();
            case M_PEN_FLAT: return attr.getMPenFlat();
            case P_PEN_PERCENT: return attr.getPPenPercent();
            case M_PEN_PERCENT: return attr.getMPenPercent();
            case FINAL_DMG_PERCENT: return attr.getFinalDmgPercent();
            case FINAL_DMG_RES_PERCENT: return attr.getFinalDmgResPercent();
            case FINAL_PDMG_PERCENT: return attr.getFinalPDmgPercent();
            case FINAL_MDMG_PERCENT: return attr.getFinalMDmgPercent();
            case PVE_DMG_PERCENT: return attr.getPveDmgPercent();
            case PVP_DMG_PERCENT: return attr.getPvpDmgPercent();
            case PVE_DMG_REDUCTION_PERCENT: return attr.getPveDmgReductionPercent();
            case PVP_DMG_REDUCTION_PERCENT: return attr.getPvpDmgReductionPercent();
            case MAXHP_PERCENT: return attr.getMaxHPPercent();
            case MAXSP_PERCENT: return attr.getMaxSPPercent();
            case SHIELD_VALUE_FLAT: return attr.getShieldValueFlat();
            case SHIELD_RATE_PERCENT: return attr.getShieldRatePercent();
            case ASPD_PERCENT: return attr.getASpdPercent();
            case MSPD_PERCENT: return attr.getMSpdPercent();
            case BASE_MSPD: return attr.getBaseMSPD();
            case VAR_CT_PERCENT: return attr.getVarCTPercent();
            case VAR_CT_FLAT: return attr.getVarCTFlat();
            case FIXED_CT_PERCENT: return attr.getFixedCTPercent();
            case FIXED_CT_FLAT: return attr.getFixedCTFlat();
            case HEALING_EFFECT_PERCENT: return attr.getHealingEffectPercent();
            case HEALING_RECEIVED_PERCENT: return attr.getHealingReceivedPercent();
            case LIFESTEAL_P_PERCENT: return attr.getLifestealPPercent();
            case LIFESTEAL_M_PERCENT: return attr.getLifestealMPercent();
            case HIT_BONUS_FLAT: return attr.getHitFlat();
            case FLEE_BONUS_FLAT: return attr.getFleeFlat();
            case PDMG_REDUCTION_PERCENT: return attr.getPDmgReductionPercent();
            case MDMG_REDUCTION_PERCENT: return attr.getMDmgReductionPercent();
            case IGNORE_PDEF_FLAT: return attr.getIgnorePDefFlat();
            case IGNORE_MDEF_FLAT: return attr.getIgnoreMDefFlat();
            case IGNORE_PDEF_PERCENT: return attr.getIgnorePDefPercent();
            case IGNORE_MDEF_PERCENT: return attr.getIgnoreMDefPercent();
            case MELEE_PDMG_PERCENT: return attr.getMeleePDmgPercent();
            case RANGE_PDMG_PERCENT: return attr.getRangePDmgPercent();
            case MELEE_PDMG_REDUCTION_PERCENT: return attr.getMeleePDReductionPercent();
            case RANGE_PDMG_REDUCTION_PERCENT: return attr.getRangePDReductionPercent();
            case TRUE_DMG: return attr.getTrueDamageFlat();
            default: return 0.0;
        }
    }

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
        return getDoubleFromPDC(item.getItemMeta().getPersistentDataContainer(), type);
    }

    public void updateLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        List<String> newLore = new ArrayList<>();

        String header = "§f§l--- Item Stats ---";

        for (String line : lore) {
            if (line.equals(header)) break;
            newLore.add(line);
        }

        boolean hasStats = false;
        for (ItemAttributeType type : ItemAttributeType.values()) {
            if (getAttributeValue(item, type) != 0) {
                hasStats = true;
                break;
            }
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        NamespacedKey effectKey = new NamespacedKey(plugin, "RO_EFFECTS");
        if (pdc.has(effectKey, PersistentDataType.STRING)) hasStats = true;

        NamespacedKey skillKey = new NamespacedKey(plugin, "RO_SKILLS");
        if (pdc.has(skillKey, PersistentDataType.STRING)) hasStats = true;

        if (hasStats) {
            if (!newLore.isEmpty() && !newLore.get(newLore.size() - 1).trim().isEmpty()) {
                newLore.add(" ");
            }

            newLore.add(header);
            for (ItemAttributeType type : ItemAttributeType.values()) {
                double val = getAttributeValue(item, type);
                if (val != 0) {
                    newLore.add(type.getDisplayName() + ": §f" + String.format(type.getFormat(), val));
                }
            }

            if (pdc.has(effectKey, PersistentDataType.STRING)) {
                String encoded = pdc.get(effectKey, PersistentDataType.STRING);
                if (encoded != null && !encoded.isEmpty()) {
                    String[] parts = encoded.split(",");
                    for (String part : parts) {
                        String[] pair = part.split(":");
                        if (pair.length == 2) {
                            newLore.add("§aEffect: " + pair[0] + " Lv." + pair[1]);
                        }
                    }
                }
            }

            // Show Skills in Lore
            if (pdc.has(skillKey, PersistentDataType.STRING)) {
                String encoded = pdc.get(skillKey, PersistentDataType.STRING);
                if (encoded != null && !encoded.isEmpty()) {
                    String[] parts = encoded.split(",");
                    for (String part : parts) {
                        String[] d = part.split(":");
                        if (d.length == 4) {
                            newLore.add("§6Skill: §e" + d[0] + " §7(Lv." + d[2] + ") [" + d[1] + "]");
                        }
                    }
                }
            }
        }

        meta.setLore(newLore);
        item.setItemMeta(meta);
    }
}