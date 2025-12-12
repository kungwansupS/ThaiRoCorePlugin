package org.rostats.itemeditor;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.skill.SkillData;
import org.rostats.engine.trigger.TriggerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemAttributeManager {

    private final ThaiRoCorePlugin plugin;
    private final NamespacedKey SKILLS_KEY;
    private final NamespacedKey POTIONS_KEY;

    public ItemAttributeManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.SKILLS_KEY = new NamespacedKey(plugin, "RO_SKILLS_DATA");
        this.POTIONS_KEY = new NamespacedKey(plugin, "RO_POTIONS_DATA");
    }

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

    public void applyAttributesToItem(ItemStack item, ItemAttribute attr) {
        if (item == null || attr == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 1. Apply Visuals
        if (attr.getCustomModelData() != null) {
            meta.setCustomModelData(attr.getCustomModelData());
        }
        if (attr.isRemoveVanillaAttribute()) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        }

        meta.setUnbreakable(attr.isUnbreakable());

        // 2. Store Stats into PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (ItemAttributeType type : ItemAttributeType.values()) {
            double value = getAttributeValueFromAttrObject(attr, type);
            NamespacedKey key = new NamespacedKey(plugin, "RO_STAT_" + type.getKey().toUpperCase());

            if (value != 0 || (type == ItemAttributeType.ATTACK_ELEMENT && value != -1) || (type == ItemAttributeType.DEFENSE_ELEMENT && value != -1)) {
                // Special case for Elements: stored as Double but logic handles -1
                // Actually if it's not -1 (default neutral/none), we store it.
                if (type == ItemAttributeType.ATTACK_ELEMENT || type == ItemAttributeType.DEFENSE_ELEMENT) {
                    if (value != -1) pdc.set(key, PersistentDataType.DOUBLE, value);
                    else pdc.remove(key);
                } else {
                    pdc.set(key, PersistentDataType.DOUBLE, value);
                }
            } else {
                pdc.remove(key);
            }
        }

        // Store Skills
        if (!attr.getSkillBindings().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ItemSkillBinding binding : attr.getSkillBindings()) {
                sb.append(binding.getSkillId()).append(":")
                        .append(binding.getTrigger().name()).append(":")
                        .append(binding.getLevel()).append(":")
                        .append(binding.getChance()).append("|");
            }
            pdc.set(SKILLS_KEY, PersistentDataType.STRING, sb.toString());
        } else {
            pdc.remove(SKILLS_KEY);
        }

        // Store Potions
        if (!attr.getPotionEffects().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            attr.getPotionEffects().forEach((type, level) -> {
                sb.append(type.getName()).append(":").append(level).append("|");
            });
            pdc.set(POTIONS_KEY, PersistentDataType.STRING, sb.toString());
        } else {
            pdc.remove(POTIONS_KEY);
        }

        item.setItemMeta(meta);
    }

    public void applyLoreStats(ItemStack item, ItemAttribute attr) {
        if (item == null || attr == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        lore.add(" ");
        lore.add("§f§l--- Item Stats ---");

        for (ItemAttributeType type : ItemAttributeType.values()) {
            double val = getAttributeValueFromAttrObject(attr, type);
            if (type == ItemAttributeType.ATTACK_ELEMENT || type == ItemAttributeType.DEFENSE_ELEMENT) {
                if (val != -1) {
                    int elemIndex = (int) val;
                    if (elemIndex >= 0 && elemIndex < org.rostats.engine.element.Element.values().length) {
                        String eName = org.rostats.engine.element.Element.values()[elemIndex].name();
                        lore.add(type.getDisplayName() + ": §f" + eName);
                    }
                }
            } else if (val != 0) {
                String valStr = String.format(type.getFormat(), val);
                if (val > 0 && !valStr.startsWith("+") && !valStr.startsWith("-")) {
                    valStr = "+" + valStr;
                }
                lore.add(type.getDisplayName() + " §f" + valStr);
            }
        }

        if (!attr.getPotionEffects().isEmpty()) {
            lore.add(" ");
            lore.add("§e§l--- Effects ---");
            attr.getPotionEffects().forEach((type, level) -> {
                String pName = type.getName().toLowerCase().replace("_", " ");
                pName = pName.substring(0, 1).toUpperCase() + pName.substring(1);
                lore.add("§7" + pName + " Lv." + level);
            });
        }

        if (!attr.getSkillBindings().isEmpty()) {
            lore.add(" ");
            lore.add("§6§l--- Skills ---");
            for (ItemSkillBinding binding : attr.getSkillBindings()) {
                SkillData skill = plugin.getSkillManager().getSkill(binding.getSkillId());
                String skillName;
                if (skill != null && skill.getDisplayName() != null && !skill.getDisplayName().isEmpty()) {
                    skillName = skill.getDisplayName().replace("&", "§");
                } else {
                    skillName = binding.getSkillId();
                }
                lore.add("§eSkill: " + skillName + " §7(Lv." + binding.getLevel() + ")");
                lore.add("§7Condition: §f" + binding.getTrigger().name() + " §7(" + String.format("%.0f%%", binding.getChance() * 100) + ")");
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public ItemAttribute readFromItem(ItemStack item) {
        ItemAttribute attr = new ItemAttribute();
        if (item == null || !item.hasItemMeta()) return attr;

        ItemMeta meta = item.getItemMeta();
        attr.setUnbreakable(meta.isUnbreakable());
        if (meta.hasCustomModelData()) attr.setCustomModelData(meta.getCustomModelData());
        if (meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) attr.setRemoveVanillaAttribute(true);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (ItemAttributeType type : ItemAttributeType.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "RO_STAT_" + type.getKey().toUpperCase());
            if (pdc.has(key, PersistentDataType.DOUBLE)) {
                Double val = pdc.get(key, PersistentDataType.DOUBLE);
                setAttributeToObj(attr, type, val != null ? val : 0.0);
            } else if (type == ItemAttributeType.ATTACK_ELEMENT || type == ItemAttributeType.DEFENSE_ELEMENT) {
                // Default to -1 if not present
                setAttributeToObj(attr, type, -1.0);
            }
        }

        if (pdc.has(SKILLS_KEY, PersistentDataType.STRING)) {
            String raw = pdc.get(SKILLS_KEY, PersistentDataType.STRING);
            if (raw != null && !raw.isEmpty()) {
                String[] split = raw.split("\\|");
                List<ItemSkillBinding> bindings = new ArrayList<>();
                for (String s : split) {
                    try {
                        String[] parts = s.split(":");
                        if (parts.length == 4) {
                            bindings.add(new ItemSkillBinding(parts[0], TriggerType.valueOf(parts[1]), Integer.parseInt(parts[2]), Double.parseDouble(parts[3])));
                        }
                    } catch (Exception ignored) {}
                }
                attr.setSkillBindings(bindings);
            }
        }

        if (pdc.has(POTIONS_KEY, PersistentDataType.STRING)) {
            String raw = pdc.get(POTIONS_KEY, PersistentDataType.STRING);
            if (raw != null && !raw.isEmpty()) {
                String[] split = raw.split("\\|");
                for (String s : split) {
                    try {
                        String[] parts = s.split(":");
                        if (parts.length == 2) {
                            PotionEffectType type = PotionEffectType.getByName(parts[0]);
                            if (type != null) attr.getPotionEffects().put(type, Integer.parseInt(parts[1]));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return attr;
    }

    public double getAttributeValueFromAttrObject(ItemAttribute attr, ItemAttributeType type) {
        if (attr == null || type == null) return 0;

        switch (type) {
            // ... (Previous Cases Omitted for Brevity - they are unchanged) ...
            // Just copying the new cases here for the sake of completeness in "Full Code" context,
            // but effectively I need to paste the WHOLE switch from before + new cases.
            // Since I modified ItemAttributeType to include ATTACK_ELEMENT/DEFENSE_ELEMENT,
            // I need to add them here.

            // ... [Assume all previous cases exist] ...
            case STR_GEAR: return attr.getStrGear();
            case AGI_GEAR: return attr.getAgiGear();
            case VIT_GEAR: return attr.getVitGear();
            case INT_GEAR: return attr.getIntGear();
            case DEX_GEAR: return attr.getDexGear();
            case LUK_GEAR: return attr.getLukGear();
            case MAXHP_PERCENT: return attr.getMaxHPPercent();
            case MAXSP_PERCENT: return attr.getMaxSPPercent();
            case HIT_BONUS_FLAT: return attr.getHitFlat();
            case FLEE_BONUS_FLAT: return attr.getFleeFlat();
            case BASE_MSPD: return attr.getBaseMSPD();
            case WEAPON_PATK: return attr.getWeaponPAtk();
            case WEAPON_MATK: return attr.getWeaponMAtk();
            case PATK_FLAT: return attr.getPAtkFlat();
            case MATK_FLAT: return attr.getMAtkFlat();
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
            case SKILL_CD_PERCENT: return attr.getSkillCooldownPercent();
            case SKILL_CD_FLAT: return attr.getSkillCooldownFlat();
            case GLOBAL_CD_PERCENT: return attr.getGlobalCooldownPercent();
            case GLOBAL_CD_FLAT: return attr.getGlobalCooldownFlat();
            case ACD_PERCENT: return attr.getAcdPercent();
            case ACD_FLAT: return attr.getAcdFlat();
            case ASPD_PERCENT: return attr.getASpdPercent();
            case MSPD_PERCENT: return attr.getMSpdPercent();
            case CRIT_RATE: return attr.getCritRate();
            case CRIT_DMG_PERCENT: return attr.getCritDmgPercent();
            case CRIT_RES: return attr.getCritRes();
            case CRIT_DMG_RES_PERCENT: return attr.getCritDmgResPercent();
            case PDMG_PERCENT: return attr.getPDmgPercent();
            case PDMG_FLAT: return attr.getPDmgFlat();
            case PDMG_REDUCTION_PERCENT: return attr.getPDmgReductionPercent();
            case MDMG_PERCENT: return attr.getMDmgPercent();
            case MDMG_FLAT: return attr.getMDmgFlat();
            case MDMG_REDUCTION_PERCENT: return attr.getMDmgReductionPercent();
            case FINAL_DMG_PERCENT: return attr.getFinalDmgPercent();
            case FINAL_PDMG_PERCENT: return attr.getFinalPDmgPercent();
            case FINAL_MDMG_PERCENT: return attr.getFinalMDmgPercent();
            case FINAL_DMG_RES_PERCENT: return attr.getFinalDmgResPercent();
            case TRUE_DMG: return attr.getTrueDamageFlat();
            case MELEE_PDMG_PERCENT: return attr.getMeleePDmgPercent();
            case MELEE_PDMG_REDUCTION_PERCENT: return attr.getMeleePDReductionPercent();
            case RANGE_PDMG_PERCENT: return attr.getRangePDmgPercent();
            case RANGE_PDMG_REDUCTION_PERCENT: return attr.getRangePDReductionPercent();
            case PVE_DMG_PERCENT: return attr.getPveDmgPercent();
            case PVE_DMG_REDUCTION_PERCENT: return attr.getPveDmgReductionPercent();
            case PVP_DMG_PERCENT: return attr.getPvpDmgPercent();
            case PVP_DMG_REDUCTION_PERCENT: return attr.getPvpDmgReductionPercent();
            case HEALING_EFFECT_PERCENT: return attr.getHealingEffectPercent();
            case HEALING_RECEIVED_PERCENT: return attr.getHealingReceivedPercent();
            case LIFESTEAL_P_PERCENT: return attr.getLifestealPPercent();
            case LIFESTEAL_M_PERCENT: return attr.getLifestealMPercent();
            case SHIELD_VALUE_FLAT: return attr.getShieldValueFlat();
            case SHIELD_RATE_PERCENT: return attr.getShieldRatePercent();

            // NEW ELEMENTS
            case ATTACK_ELEMENT: return attr.getAttackElement();
            case DEFENSE_ELEMENT: return attr.getDefenseElement();

            default: return 0.0;
        }
    }

    public void setAttributeToObj(ItemAttribute attr, ItemAttributeType type, double value) {
        if (attr == null || type == null) return;

        switch (type) {
            // ... (Previous Setters) ...
            case STR_GEAR: attr.setStrGear((int) value); break;
            case AGI_GEAR: attr.setAgiGear((int) value); break;
            case VIT_GEAR: attr.setVitGear((int) value); break;
            case INT_GEAR: attr.setIntGear((int) value); break;
            case DEX_GEAR: attr.setDexGear((int) value); break;
            case LUK_GEAR: attr.setLukGear((int) value); break;
            case MAXHP_PERCENT: attr.setMaxHPPercent(value); break;
            case MAXSP_PERCENT: attr.setMaxSPPercent(value); break;
            case HIT_BONUS_FLAT: attr.setHitFlat(value); break;
            case FLEE_BONUS_FLAT: attr.setFleeFlat(value); break;
            case BASE_MSPD: attr.setBaseMSPD(value); break;
            case WEAPON_PATK: attr.setWeaponPAtk(value); break;
            case WEAPON_MATK: attr.setWeaponMAtk(value); break;
            case PATK_FLAT: attr.setPAtkFlat(value); break;
            case MATK_FLAT: attr.setMAtkFlat(value); break;
            case P_PEN_FLAT: attr.setPPenFlat(value); break;
            case P_PEN_PERCENT: attr.setPPenPercent(value); break;
            case IGNORE_PDEF_FLAT: attr.setIgnorePDefFlat(value); break;
            case IGNORE_PDEF_PERCENT: attr.setIgnorePDefPercent(value); break;
            case M_PEN_FLAT: attr.setMPenFlat(value); break;
            case M_PEN_PERCENT: attr.setMPenPercent(value); break;
            case IGNORE_MDEF_FLAT: attr.setIgnoreMDefFlat(value); break;
            case IGNORE_MDEF_PERCENT: attr.setIgnoreMDefPercent(value); break;
            case VAR_CT_PERCENT: attr.setVarCTPercent(value); break;
            case VAR_CT_FLAT: attr.setVarCTFlat(value); break;
            case FIXED_CT_PERCENT: attr.setFixedCTPercent(value); break;
            case FIXED_CT_FLAT: attr.setFixedCTFlat(value); break;
            case SKILL_CD_PERCENT: attr.setSkillCooldownPercent(value); break;
            case SKILL_CD_FLAT: attr.setSkillCooldownFlat(value); break;
            case GLOBAL_CD_PERCENT: attr.setGlobalCooldownPercent(value); break;
            case GLOBAL_CD_FLAT: attr.setGlobalCooldownFlat(value); break;
            case ACD_PERCENT: attr.setAcdPercent(value); break;
            case ACD_FLAT: attr.setAcdFlat(value); break;
            case ASPD_PERCENT: attr.setASpdPercent(value); break;
            case MSPD_PERCENT: attr.setMSpdPercent(value); break;
            case CRIT_RATE: attr.setCritRate(value); break;
            case CRIT_DMG_PERCENT: attr.setCritDmgPercent(value); break;
            case CRIT_RES: attr.setCritRes(value); break;
            case CRIT_DMG_RES_PERCENT: attr.setCritDmgResPercent(value); break;
            case PDMG_PERCENT: attr.setPDmgPercent(value); break;
            case PDMG_FLAT: attr.setPDmgFlat(value); break;
            case PDMG_REDUCTION_PERCENT: attr.setPDmgReductionPercent(value); break;
            case MDMG_PERCENT: attr.setMDmgPercent(value); break;
            case MDMG_FLAT: attr.setMDmgFlat(value); break;
            case MDMG_REDUCTION_PERCENT: attr.setMDmgReductionPercent(value); break;
            case FINAL_DMG_PERCENT: attr.setFinalDmgPercent(value); break;
            case FINAL_PDMG_PERCENT: attr.setFinalPDmgPercent(value); break;
            case FINAL_MDMG_PERCENT: attr.setFinalMDmgPercent(value); break;
            case FINAL_DMG_RES_PERCENT: attr.setFinalDmgResPercent(value); break;
            case TRUE_DMG: attr.setTrueDamageFlat(value); break;
            case MELEE_PDMG_PERCENT: attr.setMeleePDmgPercent(value); break;
            case MELEE_PDMG_REDUCTION_PERCENT: attr.setMeleePDReductionPercent(value); break;
            case RANGE_PDMG_PERCENT: attr.setRangePDmgPercent(value); break;
            case RANGE_PDMG_REDUCTION_PERCENT: attr.setRangePDReductionPercent(value); break;
            case PVE_DMG_PERCENT: attr.setPveDmgPercent(value); break;
            case PVE_DMG_REDUCTION_PERCENT: attr.setPveDmgReductionPercent(value); break;
            case PVP_DMG_PERCENT: attr.setPvpDmgPercent(value); break;
            case PVP_DMG_REDUCTION_PERCENT: attr.setPvpDmgReductionPercent(value); break;
            case HEALING_EFFECT_PERCENT: attr.setHealingEffectPercent(value); break;
            case HEALING_RECEIVED_PERCENT: attr.setHealingReceivedPercent(value); break;
            case LIFESTEAL_P_PERCENT: attr.setLifestealPPercent(value); break;
            case LIFESTEAL_M_PERCENT: attr.setLifestealMPercent(value); break;
            case SHIELD_VALUE_FLAT: attr.setShieldValueFlat(value); break;
            case SHIELD_RATE_PERCENT: attr.setShieldRatePercent(value); break;

            // NEW SETTERS
            case ATTACK_ELEMENT: attr.setAttackElement((int) value); break;
            case DEFENSE_ELEMENT: attr.setDefenseElement((int) value); break;
        }
    }
}