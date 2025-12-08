package org.rostats.engine.skill;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.*;
import org.rostats.engine.effect.EffectType;
import org.rostats.engine.trigger.TriggerType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // --- File Management ---
    public File getRootDir() { return skillFolder; }

    public String getRelativePath(File file) {
        String rootPath = skillFolder.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (filePath.equals(rootPath)) return "/";
        if (filePath.startsWith(rootPath)) {
            String rel = filePath.substring(rootPath.length()).replace("\\", "/");
            if (rel.startsWith("/")) return rel;
            return "/" + rel;
        }
        return "/" + file.getName();
    }

    public File getFileFromRelative(String relativePath) {
        if (relativePath == null || relativePath.equals("/") || relativePath.isEmpty()) return skillFolder;
        if (relativePath.startsWith("/")) relativePath = relativePath.substring(1);
        return new File(skillFolder, relativePath);
    }

    public List<File> listContents(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return new ArrayList<>();
        return Arrays.stream(files)
                .sorted((f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareTo(f2.getName());
                })
                .collect(Collectors.toList());
    }

    public void createFolder(File parent, String name) {
        File newFolder = new File(parent, name);
        if (!newFolder.exists()) newFolder.mkdirs();
    }

    public void createSkill(File parent, String skillName) {
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
            config.save(file);
            loadSkills();
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteFile(File file) {
        if (file.isDirectory()) { File[] contents = file.listFiles(); if (contents != null) for (File sub : contents) deleteFile(sub); }
        file.delete(); loadSkills();
    }

    public void renameFile(File file, String newName) {
        String finalName = file.isDirectory() ? newName : (newName.endsWith(".yml") ? newName : newName + ".yml");
        File dest = new File(file.getParentFile(), finalName);
        file.renameTo(dest); loadSkills();
    }

    // --- Save Skill ---
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
        config.set(key + ".conditions.sp-cost", skill.getSpCostBase());
        config.set(key + ".conditions.sp-cost-per-level", skill.getSpCostPerLevel());
        config.set(key + ".conditions.cast-time", skill.getCastTime());

        List<Map<String, Object>> serializedActions = new ArrayList<>();
        for (SkillAction action : skill.getActions()) {
            serializedActions.add(action.serialize());
        }
        config.set(key + ".actions", serializedActions);

        try {
            config.save(file);
            skillMap.put(key, skill);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File findFileBySkillId(String id) {
        return findFileBySkillIdRecursive(skillFolder, id);
    }

    private File findFileBySkillIdRecursive(File dir, String id) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findFileBySkillIdRecursive(f, id);
                if (found != null) return found;
            } else if (f.getName().endsWith(".yml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(f);
                if (config.contains(id)) return f;
            }
        }
        return null;
    }

    public void castSkill(LivingEntity caster, String skillId, int level, LivingEntity target) {
        SkillData skill = skillMap.get(skillId);
        if (skill == null) return;
        for (SkillAction action : skill.getActions()) {
            try { action.execute(caster, target, level); } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadSkills() {
        skillMap.clear();
        loadSkillsRecursive(skillFolder);
        plugin.getLogger().info("Loaded " + skillMap.size() + " skills.");
    }

    private void loadSkillsRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) { loadSkillsRecursive(file); continue; }
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
            try { skill.setTrigger(TriggerType.valueOf(section.getString("trigger", "CAST"))); } catch(Exception e) { skill.setTrigger(TriggerType.CAST); }

            ConfigurationSection cond = section.getConfigurationSection("conditions");
            if (cond != null) {
                skill.setCooldownBase(cond.getDouble("cooldown", 0));
                skill.setCooldownPerLevel(cond.getDouble("cooldown-per-level", 0));
                skill.setSpCostBase(cond.getInt("sp-cost", 0));
                skill.setSpCostPerLevel(cond.getInt("sp-cost-per-level", 0));
                skill.setCastTime(cond.getDouble("cast-time", 0));
            }

            if (section.contains("actions")) {
                List<Map<?, ?>> actionList = section.getMapList("actions");
                for (Map<?, ?> rawMap : actionList) {
                    try {
                        Map<String, Object> actionMap = (Map<String, Object>) rawMap;
                        String typeStr = (String) actionMap.get("type");
                        ActionType type = ActionType.valueOf(typeStr);
                        SkillAction action = parseAction(type, actionMap);
                        if (action != null) skill.addAction(action);
                    } catch (Exception e) {}
                }
            }
            skillMap.put(key, skill);
        } catch (Exception e) {}
    }

    private SkillAction parseAction(ActionType type, Map<String, Object> map) {
        switch (type) {
            case DAMAGE:
                return new DamageAction(plugin, (String) map.getOrDefault("formula", "ATK"), (String) map.getOrDefault("element", "NEUTRAL"));
            case HEAL:
                return new HealAction(plugin, (String) map.getOrDefault("formula", "10"), (boolean) map.getOrDefault("is-mana", false));
            case APPLY_EFFECT:
                String effId = (String) map.getOrDefault("effect-id", "unknown");
                EffectType effType = EffectType.valueOf((String) map.getOrDefault("effect-type", "STAT_MODIFIER"));
                int lvl = map.containsKey("level") ? ((Number)map.get("level")).intValue() : 1;
                double pwr = map.containsKey("power") ? ((Number)map.get("power")).doubleValue() : 0.0;
                long dur = map.containsKey("duration") ? ((Number)map.get("duration")).longValue() : 100L;
                double chance = map.containsKey("chance") ? ((Number)map.get("chance")).doubleValue() : 1.0;
                String sk = (String) map.getOrDefault("stat-key", null);
                return new EffectAction(plugin, effId, effType, lvl, pwr, dur, chance, sk);

            // --- NEW ACTIONS ---
            case SOUND:
                Sound sound = Sound.valueOf((String) map.getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP"));
                float vol = ((Number) map.getOrDefault("volume", 1.0)).floatValue();
                float pit = ((Number) map.getOrDefault("pitch", 1.0)).floatValue();
                return new SoundAction(sound, vol, pit);

            case PARTICLE:
                Particle part = Particle.valueOf((String) map.getOrDefault("particle", "VILLAGER_HAPPY"));
                int count = ((Number) map.getOrDefault("count", 10)).intValue();
                double speed = ((Number) map.getOrDefault("speed", 0.1)).doubleValue();
                double yOff = ((Number) map.getOrDefault("y-offset", 1.0)).doubleValue();
                return new ParticleAction(part, count, speed, yOff);

            case PROJECTILE:
                EntityType pType = EntityType.valueOf((String) map.getOrDefault("projectile-type", "ARROW"));
                double pSpeed = ((Number) map.getOrDefault("speed", 1.0)).doubleValue();
                List<SkillAction> onHit = new ArrayList<>();
                if (map.containsKey("on-hit")) {
                    List<Map<String, Object>> hitList = (List<Map<String, Object>>) map.get("on-hit");
                    for (Map<String, Object> hitMap : hitList) {
                        String hitTypeStr = (String) hitMap.get("type");
                        SkillAction hitAction = parseAction(ActionType.valueOf(hitTypeStr), hitMap);
                        if (hitAction != null) onHit.add(hitAction);
                    }
                }
                return new ProjectileAction(plugin, pType, pSpeed, onHit);

            default: return null;
        }
    }

    public SkillData getSkill(String id) { return skillMap.get(id); }
    public Map<String, SkillData> getSkills() { return skillMap; }

    private void createExampleSkill() { /* Keep existing implementation */ }
}