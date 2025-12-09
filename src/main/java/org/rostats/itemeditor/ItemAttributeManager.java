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

        // Potion Effects
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

        // Skill Bindings
        NamespacedKey skillKey = new NamespacedKey(plugin, "RO_SKILLS");
        if (pdc.has(skillKey, PersistentDataType.STRING)) {
            String encoded = pdc.get(skillKey, PersistentDataType.STRING);
            if (encoded != null && !encoded.isEmpty()) {
                String[] parts = encoded.split(",");
                for (String part : parts) {
                    String[] d = part.split(";");
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

        NamespacedKey skillKey = new NamespacedKey(plugin, "RO_SKILLS");
        if (!attr.getSkillBindings().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ItemSkillBinding binding : attr.getSkillBindings()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(binding.getSkillId()).append(";")
                        .append(binding.getTrigger().name()).append(";")
                        .append(binding.getLevel()).append(";")
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
        switch (type) {
            // Base
            case STR_GEAR: attr.setStrGear((int)val); break;
            case AGI_GEAR: attr.setAgiGear((int)val); break;
            case VIT_GEAR: attr.setVitGear((int)val); break;
            case INT_GEAR: attr.setIntGear((int)val); break;
            case DEX_GEAR: attr.setDexGear((int)val); break;
            case LUK_GEAR: attr.setLukGear((int)val); break;
            case MAXHP_PERCENT: attr.setMaxHPPercent(val); break;
            case MAXHP_FLAT: attr.setMaxHPFlat(val); break;
            case MAXSP_PERCENT: attr.setMaxSPPercent(val); break;
            case MAXSP_FLAT: attr.setMaxSPFlat(val); break;
            case HP_RECOVERY: attr.setHpRecovery(val); break;
            case SP_RECOVERY: attr.setSpRecovery(val); break;
            case HIT_BONUS_FLAT: attr.setHitFlat(val); break;
            case FLEE_BONUS_FLAT: attr.setFleeFlat(val); break;

            // Combat
            case WEAPON_PATK: attr.setWeaponPAtk(val); break;
            case WEAPON_MATK: attr.setWeaponMAtk(val); break;
            case REFINE_P_ATK: attr.setRefinePAtk(val); break;
            case REFINE_M_ATK: attr.setRefineMAtk(val); break;
            case P_DEF_BONUS: attr.setPDefBonus(val); break;
            case M_DEF_BONUS: attr.setMDefBonus(val); break;
            case REFINE_P_DEF: attr.setRefinePDef(val); break;
            case REFINE_M_DEF: attr.setRefineMDef(val); break;

            // Pen/Ignore
            case P_PEN_FLAT: attr.setPPenFlat(val); break;
            case P_PEN_PERCENT: attr.setPPenPercent(val); break;
            case IGNORE_PDEF_FLAT: attr.setIgnorePDefFlat(val); break;
            case IGNORE_PDEF_PERCENT: attr.setIgnorePDefPercent(val); break;
            case M_PEN_FLAT: attr.setMPenFlat(val); break;
            case M_PEN_PERCENT: attr.setMPenPercent(val); break;
            case IGNORE_MDEF_FLAT: attr.setIgnoreMDefFlat(val); break;
            case IGNORE_MDEF_PERCENT: attr.setIgnoreMDefPercent(val); break;

            // Casting
            case VAR_CT_PERCENT: attr.setVarCTPercent(val); break;
            case VAR_CT_FLAT: attr.setVarCTFlat(val); break;
            case FIXED_CT_PERCENT: attr.setFixedCTPercent(val); break;
            case FIXED_CT_FLAT: attr.setFixedCTFlat(val); break;

            // Cooldown
            case SKILL_COOLDOWN_PERCENT: attr.setSkillCDPercent(val); break;
            case SKILL_COOLDOWN_FLAT: attr.setSkillCDFlat(val); break;
            case FINAL_COOLDOWN_PERCENT: attr.setFinalCDPercent(val); break;
            case GLOBAL_CD_PERCENT: attr.setGlobalCDPercent(val); break;
            case AFTER_CAST_DELAY_PERCENT: attr.setAfterCastDelayPercent(val); break;
            case AFTER_CAST_DELAY_FLAT: attr.setAfterCastDelayFlat(val); break;
            case PRE_MOTION: attr.setPreMotion(val); break;
            case POST_MOTION: attr.setPostMotion(val); break;
            case CANCEL_MOTION: attr.setCancelMotion(val); break;

            // Speed
            case ASPD_PERCENT: attr.setASpdPercent(val); break;
            case MSPD_PERCENT: attr.setMSpdPercent(val); break;
            case BASE_MSPD: attr.setBaseMSPD(val); break;
            case ATTACK_INTERVAL_PERCENT: attr.setAtkIntervalPercent(val); break;

            // Crit
            case CRIT: attr.setCrit(val); break;
            case CRIT_DMG_PERCENT: attr.setCritDmgPercent(val); break;
            case FINAL_CRIT_DMG_PERCENT: attr.setFinalCritDmgPercent(val); break;
            case PERFECT_HIT: attr.setPerfectHit(val); break;
            case CRIT_RES: attr.setCritRes(val); break;
            case CRIT_DMG_RES_PERCENT: attr.setCritDmgResPercent(val); break;
            case PERFECT_DODGE: attr.setPerfectDodge(val); break;

            // Universal
            case PDMG_PERCENT: attr.setPDmgPercent(val); break;
            case PDMG_FLAT: attr.setPDmgFlat(val); break;
            case PDMG_REDUCTION_PERCENT: attr.setPDmgReductionPercent(val); break;
            case MDMG_PERCENT: attr.setMDmgPercent(val); break;
            case MDMG_FLAT: attr.setMDmgFlat(val); break;
            case MDMG_REDUCTION_PERCENT: attr.setMDmgReductionPercent(val); break;
            case TRUE_DMG: attr.setTrueDamageFlat(val); break;
            case FINAL_DMG_PERCENT: attr.setFinalDmgPercent(val); break;
            case FINAL_DMG_RES_PERCENT: attr.setFinalDmgResPercent(val); break;

            // Distance
            case MELEE_PDMG_PERCENT: attr.setMeleePDmgPercent(val); break;
            case MELEE_PDMG_REDUCTION_PERCENT: attr.setMeleePDReductionPercent(val); break;
            case RANGE_PDMG_PERCENT: attr.setRangePDmgPercent(val); break;
            case RANGE_PDMG_REDUCTION_PERCENT: attr.setRangePDReductionPercent(val); break;

            // Content
            case PVE_DMG_PERCENT: attr.setPveDmgPercent(val); break;
            case PVE_DMG_REDUCTION_PERCENT: attr.setPveDmgReductionPercent(val); break;
            case PVP_DMG_PERCENT: attr.setPvpDmgPercent(val); break;
            case PVP_DMG_REDUCTION_PERCENT: attr.setPvpDmgReductionPercent(val); break;

            // Healing
            case HEALING_EFFECT_PERCENT: attr.setHealingEffectPercent(val); break;
            case HEALING_FLAT: attr.setHealingFlat(val); break;
            case HEALING_RECEIVED_PERCENT: attr.setHealingReceivedPercent(val); break;
            case HEALING_RECEIVED_FLAT: attr.setHealingReceivedFlat(val); break;
            case LIFESTEAL_P_PERCENT: attr.setLifestealPPercent(val); break;
            case LIFESTEAL_M_PERCENT: attr.setLifestealMPercent(val); break;

            // Misc
            case SHIELD_VALUE_FLAT: attr.setShieldValueFlat(val); break;
            case SHIELD_RATE_PERCENT: attr.setShieldRatePercent(val); break;
        }
    }

    public double getAttributeValueFromAttrObject(ItemAttribute attr, ItemAttributeType type) {
        switch (type) {
            case STR_GEAR: return attr.getStrGear();
            case AGI_GEAR: return attr.getAgiGear();
            case VIT_GEAR: return attr.getVitGear();
            case INT_GEAR: return attr.getIntGear();
            case DEX_GEAR: return attr.getDexGear();
            case LUK_GEAR: return attr.getLukGear();
            case MAXHP_PERCENT: return attr.getMaxHPPercent();
            case MAXHP_FLAT: return attr.getMaxHPFlat();
            case MAXSP_PERCENT: return attr.getMaxSPPercent();
            case MAXSP_FLAT: return attr.getMaxSPFlat();
            case HP_RECOVERY: return attr.getHpRecovery();
            case SP_RECOVERY: return attr.getSpRecovery();
            case HIT_BONUS_FLAT: return attr.getHitFlat();
            case FLEE_BONUS_FLAT: return attr.getFleeFlat();

            case WEAPON_PATK: return attr.getWeaponPAtk();
            case WEAPON_MATK: return attr.getWeaponMAtk();
            case REFINE_P_ATK: return attr.getRefinePAtk();
            case REFINE_M_ATK: return attr.getRefineMAtk();
            case P_DEF_BONUS: return attr.getPDefBonus();
            case M_DEF_BONUS: return attr.getMDefBonus();
            case REFINE_P_DEF: return attr.getRefinePDef();
            case REFINE_M_DEF: return attr.getRefineMDef();

            case P_PEN_FLAT: return attr.getPPenFlat();
            case P_PEN_PERCENT: return attr.getPPenPercent();
            case IGNORE_PDEF_FLAT: return attr.getIgnorePDefFlat();
            case IGNORE_PDEF_PERCENT: return attr.getIgnorePDefPercent();
            case M_PEN_FLAT: return attr.getMPenFlat();
            case M_PEN_PERCENT: return attr.getMPenPercent();
            case IGNORE_MDEF_FLAT: return attr.getIgnoreMDefFlat();
            case IGNORE_MDEF_PERCENT: return attr.getIgnoreMDefPercent();

            case VAR_CT_PERCENT: return attr.getVarCTPercent();
            case VAR_CT_FLAT: return attr.getVarCTFlat();
            case FIXED_CT_PERCENT: return attr.getFixedCTPercent();
            case FIXED_CT_FLAT: return attr.getFixedCTFlat();

            case SKILL_COOLDOWN_PERCENT: return attr.getSkillCDPercent();
            case SKILL_COOLDOWN_FLAT: return attr.getSkillCDFlat();
            case FINAL_COOLDOWN_PERCENT: return attr.getFinalCDPercent();
            case GLOBAL_CD_PERCENT: return attr.getGlobalCDPercent();
            case AFTER_CAST_DELAY_PERCENT: return attr.getAfterCastDelayPercent();
            case AFTER_CAST_DELAY_FLAT: return attr.getAfterCastDelayFlat();
            case PRE_MOTION: return attr.getPreMotion();
            case POST_MOTION: return attr.getPostMotion();
            case CANCEL_MOTION: return attr.getCancelMotion();

            case ASPD_PERCENT: return attr.getASpdPercent();
            case MSPD_PERCENT: return attr.getMSpdPercent();
            case BASE_MSPD: return attr.getBaseMSPD();
            case ATTACK_INTERVAL_PERCENT: return attr.getAtkIntervalPercent();

            case CRIT: return attr.getCrit();
            case CRIT_DMG_PERCENT: return attr.getCritDmgPercent();
            case FINAL_CRIT_DMG_PERCENT: return attr.getFinalCritDmgPercent();
            case PERFECT_HIT: return attr.getPerfectHit();
            case CRIT_RES: return attr.getCritRes();
            case CRIT_DMG_RES_PERCENT: return attr.getCritDmgResPercent();
            case PERFECT_DODGE: return attr.getPerfectDodge();

            case PDMG_PERCENT: return attr.getPDmgPercent();
            case PDMG_FLAT: return attr.getPDmgFlat();
            case PDMG_REDUCTION_PERCENT: return attr.getPDmgReductionPercent();
            case MDMG_PERCENT: return attr.getMDmgPercent();
            case MDMG_FLAT: return attr.getMDmgFlat();
            case MDMG_REDUCTION_PERCENT: return attr.getMDmgReductionPercent();
            case TRUE_DMG: return attr.getTrueDamageFlat();
            case FINAL_DMG_PERCENT: return attr.getFinalDmgPercent();
            case FINAL_DMG_RES_PERCENT: return attr.getFinalDmgResPercent();

            case MELEE_PDMG_PERCENT: return attr.getMeleePDmgPercent();
            case MELEE_PDMG_REDUCTION_PERCENT: return attr.getMeleePDReductionPercent();
            case RANGE_PDMG_PERCENT: return attr.getRangePDmgPercent();
            case RANGE_PDMG_REDUCTION_PERCENT: return attr.getRangePDReductionPercent();

            case PVE_DMG_PERCENT: return attr.getPveDmgPercent();
            case PVE_DMG_REDUCTION_PERCENT: return attr.getPveDmgReductionPercent();
            case PVP_DMG_PERCENT: return attr.getPvpDmgPercent();
            case PVP_DMG_REDUCTION_PERCENT: return attr.getPvpDmgReductionPercent();

            case HEALING_EFFECT_PERCENT: return attr.getHealingEffectPercent();
            case HEALING_FLAT: return attr.getHealingFlat();
            case HEALING_RECEIVED_PERCENT: return attr.getHealingReceivedPercent();
            case HEALING_RECEIVED_FLAT: return attr.getHealingReceivedFlat();
            case LIFESTEAL_P_PERCENT: return attr.getLifestealPPercent();
            case LIFESTEAL_M_PERCENT: return attr.getLifestealMPercent();

            case SHIELD_VALUE_FLAT: return attr.getShieldValueFlat();
            case SHIELD_RATE_PERCENT: return attr.getShieldRatePercent();

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

            if (pdc.has(skillKey, PersistentDataType.STRING)) {
                String encoded = pdc.get(skillKey, PersistentDataType.STRING);
                if (encoded != null && !encoded.isEmpty()) {
                    String[] parts = encoded.split(",");
                    for (String part : parts) {
                        String[] d = part.split(";");
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