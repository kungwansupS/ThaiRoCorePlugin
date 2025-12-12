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
import org.rostats.engine.trigger.TriggerType;

import java.util.ArrayList;
import java.util.List;

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

        // Apply Unbreakable
        meta.setUnbreakable(attr.isUnbreakable());

        // 2. Store Stats into PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        for (ItemAttributeType type : ItemAttributeType.values()) {
            double value = getAttributeValueFromAttrObject(attr, type);
            NamespacedKey key = new NamespacedKey(plugin, "RO_STAT_" + type.getKey().toUpperCase());

            if (value != 0) {
                pdc.set(key, PersistentDataType.DOUBLE, value);
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
            if (val != 0) {
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
                lore.add("§eSkill: " + binding.getSkillId() + " §7(Lv." + binding.getLevel() + ")");
                lore.add("§7Condition: §f" + binding.getTrigger().name() + " §7(" + String.format("%.0f%%", binding.getChance()*100) + ")");
            }
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    public ItemAttribute readFromItem(ItemStack item) {
        ItemAttribute attr = new ItemAttribute();
        if (item == null || !item.hasItemMeta()) return attr;

        ItemMeta meta = item.getItemMeta();

        // [FIX] Read Unbreakable from Item
        attr.setUnbreakable(meta.isUnbreakable());

        // [FIX] Read CustomModelData from Item (แก้ปัญหา Icon หาย)
        if (meta.hasCustomModelData()) {
            attr.setCustomModelData(meta.getCustomModelData());
        }

        // [FIX] Read Vanilla Hide Flags
        if (meta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) {
            attr.setRemoveVanillaAttribute(true);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // 1. Read Stats
        for (ItemAttributeType type : ItemAttributeType.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "RO_STAT_" + type.getKey().toUpperCase());
            if (pdc.has(key, PersistentDataType.DOUBLE)) {
                Double val = pdc.get(key, PersistentDataType.DOUBLE);
                setAttributeToObj(attr, type, val != null ? val : 0.0);
            }
        }

        // 2. Read Skills
        if (pdc.has(SKILLS_KEY, PersistentDataType.STRING)) {
            String raw = pdc.get(SKILLS_KEY, PersistentDataType.STRING);
            if (raw != null && !raw.isEmpty()) {
                String[] split = raw.split("\\|");
                List<ItemSkillBinding> bindings = new ArrayList<>();
                for (String s : split) {
                    try {
                        String[] parts = s.split(":");
                        if (parts.length == 4) {
                            String id = parts[0];
                            TriggerType trigger = TriggerType.valueOf(parts[1]);
                            int level = Integer.parseInt(parts[2]);
                            double chance = Double.parseDouble(parts[3]);
                            bindings.add(new ItemSkillBinding(id, trigger, level, chance));
                        }
                    } catch (Exception ignored) {}
                }
                attr.setSkillBindings(bindings);
            }
        }

        // 3. Read Potions
        if (pdc.has(POTIONS_KEY, PersistentDataType.STRING)) {
            String raw = pdc.get(POTIONS_KEY, PersistentDataType.STRING);
            if (raw != null && !raw.isEmpty()) {
                String[] split = raw.split("\\|");
                for (String s : split) {
                    try {
                        String[] parts = s.split(":");
                        if (parts.length == 2) {
                            PotionEffectType type = PotionEffectType.getByName(parts[0]);
                            int level = Integer.parseInt(parts[1]);
                            if (type != null) {
                                attr.getPotionEffects().put(type, level);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return attr;
    }

    public double getAttributeValueFromAttrObject(ItemAttribute attr, ItemAttributeType type) {
        if (attr == null || type == null) return 0;

        return switch (type) {
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

            case WEAPON_PATK -> attr.getWeaponPAtk();
            case WEAPON_MATK -> attr.getWeaponMAtk();
            case PATK_FLAT -> attr.getPAtkFlat();
            case MATK_FLAT -> attr.getMAtkFlat();

            case P_PEN_FLAT -> attr.getPPenFlat();
            case P_PEN_PERCENT -> attr.getPPenPercent();
            case IGNORE_PDEF_FLAT -> attr.getIgnorePDefFlat();
            case IGNORE_PDEF_PERCENT -> attr.getIgnorePDefPercent();
            case M_PEN_FLAT -> attr.getMPenFlat();
            case M_PEN_PERCENT -> attr.getMPenPercent();
            case IGNORE_MDEF_FLAT -> attr.getIgnoreMDefFlat();
            case IGNORE_MDEF_PERCENT -> attr.getIgnoreMDefPercent();

            case VAR_CT_PERCENT -> attr.getVarCTPercent();
            case VAR_CT_FLAT -> attr.getVarCTFlat();
            case FIXED_CT_PERCENT -> attr.getFixedCTPercent();
            case FIXED_CT_FLAT -> attr.getFixedCTFlat();
            case SKILL_CD_PERCENT -> attr.getSkillCooldownPercent();
            case SKILL_CD_FLAT -> attr.getSkillCooldownFlat();

            case ACD_PERCENT -> attr.getAcdPercent();
            case ACD_FLAT -> attr.getAcdFlat();
            case GLOBAL_CD_PERCENT -> attr.getGlobalCooldownPercent();
            case GLOBAL_CD_FLAT -> attr.getGlobalCooldownFlat();

            case ASPD_PERCENT -> attr.getASpdPercent();
            case MSPD_PERCENT -> attr.getMSpdPercent();

            case CRIT_RATE -> attr.getCritRate();
            case CRIT_DMG_PERCENT -> attr.getCritDmgPercent();
            case CRIT_RES -> attr.getCritRes();
            case CRIT_DMG_RES_PERCENT -> attr.getCritDmgResPercent();

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

            case MELEE_PDMG_PERCENT -> attr.getMeleePDmgPercent();
            case MELEE_PDMG_REDUCTION_PERCENT -> attr.getMeleePDReductionPercent();
            case RANGE_PDMG_PERCENT -> attr.getRangePDmgPercent();
            case RANGE_PDMG_REDUCTION_PERCENT -> attr.getRangePDReductionPercent();

            case PVE_DMG_PERCENT -> attr.getPveDmgPercent();
            case PVE_DMG_REDUCTION_PERCENT -> attr.getPveDmgReductionPercent();
            case PVP_DMG_PERCENT -> attr.getPvpDmgPercent();
            case PVP_DMG_REDUCTION_PERCENT -> attr.getPvpDmgReductionPercent();

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
            case SKILL_CD_PERCENT -> attr.setSkillCooldownPercent(value);
            case SKILL_CD_FLAT -> attr.setSkillCooldownFlat(value);

            case ACD_PERCENT -> attr.setAcdPercent(value);
            case ACD_FLAT -> attr.setAcdFlat(value);
            case GLOBAL_CD_PERCENT -> attr.setGlobalCooldownPercent(value);
            case GLOBAL_CD_FLAT -> attr.setGlobalCooldownFlat(value);

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