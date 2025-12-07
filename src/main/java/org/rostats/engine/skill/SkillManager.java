package org.rostats.engine.skill;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity; // <--- เพิ่มบรรทัดนี้ครับ
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.DamageAction;
import org.rostats.engine.action.impl.EffectAction;
import org.rostats.engine.action.impl.HealAction;
import org.rostats.engine.effect.EffectType;
import org.rostats.engine.trigger.TriggerType;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class SkillManager {

    private final ThaiRoCorePlugin plugin;
    private final Map<String, SkillData> skillMap = new HashMap<>();
    private final File skillFolder;

    public SkillManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.skillFolder = new File(plugin.getDataFolder(), "skills");
        if (!skillFolder.exists()) {
            skillFolder.mkdirs();
            createExampleSkill();
        }
        loadSkills();
    }

    // Core Cast Logic
    public void castSkill(LivingEntity caster, String skillId, int level, LivingEntity target) {
        SkillData skill = skillMap.get(skillId);
        if (skill == null) {
            return;
        }

        // Execute all actions
        for (SkillAction action : skill.getActions()) {
            try {
                action.execute(caster, target, level);
            } catch (Exception e) {
                plugin.getLogger().warning("Error executing action for skill " + skillId);
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadSkills() {
        skillMap.clear();
        File[] files = skillFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(false)) {
                try {
                    ConfigurationSection section = config.getConfigurationSection(key);
                    if (section == null) continue;

                    SkillData skill = new SkillData(key);
                    skill.setDisplayName(section.getString("display-name", key));

                    String iconName = section.getString("icon", "BOOK");
                    Material icon = Material.getMaterial(iconName);
                    skill.setIcon(icon != null ? icon : Material.BOOK);

                    skill.setMaxLevel(section.getInt("max-level", 1));

                    String triggerStr = section.getString("trigger", "CAST");
                    try {
                        skill.setTrigger(TriggerType.valueOf(triggerStr));
                    } catch (IllegalArgumentException e) {
                        skill.setTrigger(TriggerType.CAST);
                        plugin.getLogger().warning("Invalid trigger for skill " + key + ": " + triggerStr);
                    }

                    // Conditions
                    ConfigurationSection cond = section.getConfigurationSection("conditions");
                    if (cond != null) {
                        skill.setCooldownBase(cond.getDouble("cooldown", 0));
                        skill.setCooldownPerLevel(cond.getDouble("cooldown-per-level", 0));
                        skill.setSpCostBase(cond.getInt("sp-cost", 0));
                        skill.setSpCostPerLevel(cond.getInt("sp-cost-per-level", 0));
                        skill.setCastTime(cond.getDouble("cast-time", 0));
                    }

                    // Load Actions
                    if (section.contains("actions")) {
                        List<Map<?, ?>> actionList = section.getMapList("actions");
                        for (Map<?, ?> rawMap : actionList) {
                            try {
                                Map<String, Object> actionMap = (Map<String, Object>) rawMap;

                                String typeStr = (String) actionMap.get("type");
                                ActionType type = ActionType.valueOf(typeStr);
                                SkillAction action = parseAction(type, actionMap);
                                if (action != null) skill.addAction(action);
                            } catch (Exception e) {
                                plugin.getLogger().warning("Invalid action structure in skill " + key);
                            }
                        }
                    }

                    skillMap.put(key, skill);

                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error loading skill: " + key + " in file " + file.getName(), e);
                }
            }
        }
        plugin.getLogger().info("Loaded " + skillMap.size() + " skills.");
    }

    private SkillAction parseAction(ActionType type, Map<String, Object> map) {
        switch (type) {
            case DAMAGE:
                String formula = (String) map.getOrDefault("formula", "ATK");
                String element = (String) map.getOrDefault("element", "NEUTRAL");
                return new DamageAction(plugin, formula, element);

            case HEAL:
                String healFormula = (String) map.getOrDefault("formula", "10");
                boolean isMana = (boolean) map.getOrDefault("is-mana", false);
                return new HealAction(plugin, healFormula, isMana);

            case APPLY_EFFECT:
                String effectId = (String) map.getOrDefault("effect-id", "unknown");
                String effTypeStr = (String) map.getOrDefault("effect-type", "STAT_MODIFIER");
                EffectType effType = EffectType.valueOf(effTypeStr);

                int level = map.containsKey("level") ? ((Number)map.get("level")).intValue() : 1;
                double power = map.containsKey("power") ? ((Number)map.get("power")).doubleValue() : 0.0;
                long duration = map.containsKey("duration") ? ((Number)map.get("duration")).longValue() : 100L;
                double chance = map.containsKey("chance") ? ((Number)map.get("chance")).doubleValue() : 1.0;

                String statKey = (String) map.getOrDefault("stat-key", null);
                return new EffectAction(plugin, effectId, effType, level, power, duration, chance, statKey);

            default:
                return null;
        }
    }

    public SkillData getSkill(String id) {
        return skillMap.get(id);
    }

    public Map<String, SkillData> getSkills() {
        return skillMap;
    }

    private void createExampleSkill() {
        File example = new File(skillFolder, "example_skill.yml");
        if (example.exists()) return;
        try {
            example.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(example);

            String key = "fireball";
            config.set(key + ".display-name", "Fireball");
            config.set(key + ".icon", "BLAZE_POWDER");
            config.set(key + ".max-level", 10);
            config.set(key + ".trigger", "CAST");

            config.set(key + ".conditions.cooldown", 5.0);
            config.set(key + ".conditions.sp-cost", 20);

            List<Map<String, Object>> actions = new java.util.ArrayList<>();

            Map<String, Object> damage = new java.util.HashMap<>();
            damage.put("type", "DAMAGE");
            damage.put("formula", "(MATK * 1.5) + (INT * 5)");
            damage.put("element", "FIRE");
            actions.add(damage);

            Map<String, Object> burn = new java.util.HashMap<>();
            burn.put("type", "APPLY_EFFECT");
            burn.put("effect-id", "burn_dot");
            burn.put("effect-type", "PERIODIC_DAMAGE");
            burn.put("power", 10.0);
            burn.put("duration", 100);
            burn.put("chance", 0.5);
            actions.add(burn);

            config.set(key + ".actions", actions);

            config.save(example);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}