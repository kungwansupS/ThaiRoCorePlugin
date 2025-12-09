package org.rostats.engine.skill;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.*;
import org.rostats.engine.effect.EffectType;
import org.rostats.engine.trigger.TriggerType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

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

    // --- Core Methods ---

    public void castSkill(LivingEntity caster, String skillId, int level, LivingEntity target) {
        castSkill(caster, skillId, level, target, false);
    }

    public void castSkill(LivingEntity caster, String skillId, int level, LivingEntity target, boolean isPassive) {
        SkillData skill = skillMap.get(skillId);
        if (skill == null) return;

        // Status Check (Crowd Control)
        if (plugin.getEffectManager().hasEffect(caster, EffectType.CROWD_CONTROL, "STUN")) {
            if (caster instanceof Player) caster.sendMessage("§cYou are stunned!");
            return;
        }
        if (plugin.getEffectManager().hasEffect(caster, EffectType.CROWD_CONTROL, "SILENCE")) {
            if (caster instanceof Player) caster.sendMessage("§cYou are silenced!");
            return;
        }

        List<SkillAction> finalActions = new LinkedList<>(skill.getActions());

        // Resource & Cooldown Check (Only for Players)
        if (!isPassive && caster instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

            // 1. Level Check
            if (data.getBaseLevel() < skill.getRequiredLevel()) {
                player.sendMessage("§cLevel too low! Required: " + skill.getRequiredLevel());
                return;
            }

            // 2. Global Cooldown Check
            if (data.isOnGlobalCooldown()) {
                double remaining = data.getRemainingGlobalCooldown();
                player.sendMessage(String.format("§cGlobal cooldown! Wait %.1fs", remaining));
                return;
            }

            // 3. Skill Cooldown Check
            double baseCooldown = skill.getCooldown(level);
            double finalCooldown = data.getFinalSkillCooldown(baseCooldown);

            if (data.isSkillOnCooldown(skillId, finalCooldown)) {
                double remaining = data.getRemainingSkillCooldown(skillId, finalCooldown);
                player.sendMessage(String.format("§cSkill on cooldown! Wait %.1fs", remaining));
                return;
            }

            // 4. SP Cost Check
            int spCost = skill.getSpCost(level);
            if (data.getCurrentSP() < spCost) {
                player.sendMessage("§cNot enough SP!");
                return;
            }

            // 5. Cast Time Calculation
            double baseCastTimeSeconds = skill.getCastTime();
            if (baseCastTimeSeconds > 0.0) {
                double finalCastTimeSeconds = data.getFinalCastTime(baseCastTimeSeconds);

                if (finalCastTimeSeconds > 0.0) {
                    long castTimeTicks = (long) (finalCastTimeSeconds * 20.0);
                    finalActions.add(0, new DelayAction(castTimeTicks));
                }
            }

            // 6. Deduct SP & Set Cooldowns
            data.setCurrentSP(data.getCurrentSP() - spCost);

            // Set skill-specific cooldown
            long now = System.currentTimeMillis();
            data.setSkillCooldown(skillId, now);

            // Apply Global Cooldown
            double baseGcd = skill.getGlobalCooldown(level);
            if (baseGcd > 0) {
                data.applyGlobalCooldown(baseGcd);
            }

            // [NEW] Track last used skill for cooldown display
            if (plugin.getCooldownDisplay() != null) {
                plugin.getCooldownDisplay().setLastUsedSkill(player, skillId);
            }

            plugin.getManaManager().updateBar(player);
        }

        // Execute via SkillRunner
        SkillRunner runner = new SkillRunner(plugin, caster, target, level, finalActions);

        // Setup Runner for LoopActions
        for (SkillAction action : finalActions) {
            if (action instanceof LoopAction) {
                ((LoopAction) action).setRunner(runner);
            }
        }

        runner.runNext();
    }

    public SkillData getSkill(String id) {
        return skillMap.get(id);
    }

    public Map<String, SkillData> getSkills() {
        return skillMap;
    }

    public void loadSkills() {
        skillMap.clear();
        loadSkillsRecursive(skillFolder);
        plugin.getLogger().info("Loaded " + skillMap.size() + " skills.");
    }

    // --- Loading & Parsing ---

    private void loadSkillsRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                loadSkillsRecursive(file);
                continue;
            }

            if (!file.getName().endsWith(".yml")) continue;

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String key : config.getKeys(false)) {
                    loadSingleSkill(key, config);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading skill file: " + file.getName(), e);
            }
        }
    }

    private void loadSingleSkill(String key, YamlConfiguration config) {
        try {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) return;

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
            }

            ConfigurationSection cond = section.getConfigurationSection("conditions");
            if (cond != null) {
                skill.setCooldownBase(cond.getDouble("cooldown", 0));
                skill.setCooldownPerLevel(cond.getDouble("cooldown-per-level", 0));

                skill.setGlobalCooldownBase(cond.getDouble("global-cooldown", 0.5));
                skill.setGlobalCooldownPerLevel(cond.getDouble("global-cooldown-per-level", 0));

                skill.setSpCostBase(cond.getInt("sp-cost", 0));
                skill.setSpCostPerLevel(cond.getInt("sp-cost-per-level", 0));
                skill.setCastTime(cond.getDouble("cast-time", 0));
                skill.setRequiredLevel(cond.getInt("required-level", 1));
            }

            List<?> actionList = section.getList("actions", new ArrayList<>());
            for (Object obj : actionList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> actionMap = (Map<String, Object>) obj;
                    SkillAction action = parseAction(actionMap);
                    if (action != null) skill.addAction(action);
                }
            }

            skillMap.put(key, skill);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading skill: " + key, e);
        }
    }

    private SkillAction parseAction(Map<String, Object> map) {
        String typeStr = (String) map.get("type");
        if (typeStr == null) return null;

        try {
            ActionType type = ActionType.valueOf(typeStr);
            switch (type) {
                case DAMAGE:
                    return new DamageAction(
                            plugin,
                            getDouble(map, "base-damage"),
                            getDouble(map, "damage-per-level"),
                            getString(map, "damage-type", "PHYSICAL")
                    );

                case HEAL:
                    return new HealAction(
                            plugin,
                            getDouble(map, "base-heal"),
                            getDouble(map, "heal-per-level"),
                            getString(map, "heal-type", "HEALTH")
                    );

                case APPLY_EFFECT:
                    return new ApplyEffectAction(
                            plugin,
                            getString(map, "effect-type", "BUFF"),
                            getString(map, "effect-id", "UNKNOWN"),
                            getInt(map, "duration"),
                            getDouble(map, "power")
                    );

                case SOUND:
                    return new SoundAction(
                            getString(map, "sound", "ENTITY_PLAYER_LEVELUP"),
                            getDouble(map, "volume", 1.0),
                            getDouble(map, "pitch", 1.0)
                    );

                case PARTICLE:
                    return new ParticleAction(
                            getString(map, "particle", "FLAME"),
                            getInt(map, "count", 10)
                    );

                case PROJECTILE:
                    return new ProjectileAction(
                            plugin,
                            getString(map, "projectile-type", "ARROW"),
                            getDouble(map, "velocity", 1.0),
                            getString(map, "on-hit-skill", null)
                    );

                case MESSAGE:
                    return new MessageAction(getString(map, "message", ""));

                case DELAY:
                    return new DelayAction(getLong(map, "ticks", 20L));

                case CONDITIONAL:
                    return new ConditionalAction(
                            plugin,
                            getString(map, "condition-type", "HEALTH_BELOW"),
                            getDouble(map, "value", 50.0),
                            getString(map, "if-true-skill", null),
                            getString(map, "if-false-skill", null)
                    );

                case LOOP:
                    return new LoopAction(
                            plugin,
                            getInt(map, "iterations", 1),
                            getString(map, "loop-skill", null)
                    );

                case AREA_EFFECT:
                    return new AreaAction(
                            plugin,
                            getDouble(map, "radius", 5.0),
                            getString(map, "target-type", "ENEMY"),
                            getString(map, "sub-skill", null),
                            getInt(map, "max-targets", 10)
                    );

                default:
                    plugin.getLogger().warning("Unknown action type: " + type);
                    return null;
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid action type: " + typeStr);
            return null;
        }
    }

    // Helper Methods
    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return 0.0;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultValue;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultValue;
    }

    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).longValue();
        return defaultValue;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return (val instanceof String) ? (String) val : defaultValue;
    }

    // --- File Management ---

    public File getSkillFolder() {
        return skillFolder;
    }

    public List<File> getAllFiles() {
        return getAllFilesRecursive(skillFolder);
    }

    private List<File> getAllFilesRecursive(File dir) {
        List<File> result = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            if (file.isDirectory()) {
                result.add(file);
                result.addAll(getAllFilesRecursive(file));
            } else if (file.getName().endsWith(".yml")) {
                result.add(file);
            }
        }
        return result;
    }

    public void createNewSkill(File parent, String skillName) {
        String fileName = skillName.endsWith(".yml") ? skillName : skillName + ".yml";
        File file = new File(parent, fileName);
        if (file.exists()) return;

        try {
            file.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = skillName.replace(".yml", "").toLowerCase().replace(" ", "_");

            config.set(id + ".display-name", skillName);
            config.set(id + ".icon", "BOOK");
            config.set(id + ".max-level", 1);
            config.set(id + ".trigger", "CAST");

            config.set(id + ".conditions.cooldown", 1.0);
            config.set(id + ".conditions.global-cooldown", 0.5);
            config.set(id + ".conditions.sp-cost", 0);
            config.set(id + ".conditions.required-level", 1);

            config.set(id + ".actions", new ArrayList<>());

            config.save(file);
            loadSkills();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File sub : contents) deleteFile(sub);
            }
        }
        file.delete();
        loadSkills();
    }

    public void renameFile(File file, String newName) {
        String finalName = file.isDirectory() ? newName : (newName.endsWith(".yml") ? newName : newName + ".yml");
        File dest = new File(file.getParentFile(), finalName);
        file.renameTo(dest);
        loadSkills();
    }

    public void saveSkill(SkillData skill) {
        File file = findFileBySkillId(skill.getId());
        if (file == null) {
            plugin.getLogger().warning("Could not find file for skill: " + skill.getId());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = skill.getId();

        config.set(key + ".display-name", skill.getDisplayName());
        config.set(key + ".icon", skill.getIcon().name());
        config.set(key + ".max-level", skill.getMaxLevel());
        config.set(key + ".trigger", skill.getTrigger().name());

        config.set(key + ".conditions.cooldown", skill.getCooldownBase());
        config.set(key + ".conditions.cooldown-per-level", skill.getCooldownPerLevel());

        config.set(key + ".conditions.global-cooldown", skill.getGlobalCooldownBase());
        config.set(key + ".conditions.global-cooldown-per-level", skill.getGlobalCooldownPerLevel());

        config.set(key + ".conditions.sp-cost", skill.getSpCostBase());
        config.set(key + ".conditions.sp-cost-per-level", skill.getSpCostPerLevel());
        config.set(key + ".conditions.cast-time", skill.getCastTime());
        config.set(key + ".conditions.required-level", skill.getRequiredLevel());

        List<Map<String, Object>> serializedActions = new ArrayList<>();
        for (SkillAction action : skill.getActions()) {
            serializedActions.add(action.serialize());
        }
        config.set(key + ".actions", serializedActions);

        try {
            config.save(file);
            plugin.getLogger().info("Saved skill: " + key);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save skill: " + key, e);
        }
    }

    private File findFileBySkillId(String skillId) {
        return findFileRecursive(skillFolder, skillId);
    }

    private File findFileRecursive(File dir, String skillId) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findFileRecursive(file, skillId);
                if (found != null) return found;
            } else if (file.getName().endsWith(".yml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (config.contains(skillId)) return file;
            }
        }
        return null;
    }

    private void createExampleSkill() {
        File exampleFile = new File(skillFolder, "example_fireball.yml");
        if (exampleFile.exists()) return;

        try {
            exampleFile.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(exampleFile);

            config.set("fireball.display-name", "Fireball");
            config.set("fireball.icon", "FIRE_CHARGE");
            config.set("fireball.max-level", 5);
            config.set("fireball.trigger", "CAST");
            config.set("fireball.conditions.cooldown", 3.0);
            config.set("fireball.conditions.global-cooldown", 0.5);
            config.set("fireball.conditions.sp-cost", 20);
            config.set("fireball.conditions.cast-time", 1.0);
            config.set("fireball.conditions.required-level", 10);

            List<Map<String, Object>> actions = new ArrayList<>();

            Map<String, Object> damage = new HashMap<>();
            damage.put("type", "DAMAGE");
            damage.put("base-damage", 50.0);
            damage.put("damage-per-level", 10.0);
            damage.put("damage-type", "MAGIC");
            actions.add(damage);

            Map<String, Object> particle = new HashMap<>();
            particle.put("type", "PARTICLE");
            particle.put("particle", "FLAME");
            particle.put("count", 20);
            actions.add(particle);

            config.set("fireball.actions", actions);
            config.save(exampleFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create example skill", e);
        }
    }
}