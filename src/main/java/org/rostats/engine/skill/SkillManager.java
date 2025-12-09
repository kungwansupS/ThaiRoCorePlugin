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

            // 2. Global Cooldown Check (NEW)
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

            // 6. Deduct SP & Set Cooldowns (NEW: includes GCD)
            data.setCurrentSP(data.getCurrentSP() - spCost);

            // Set skill-specific cooldown
            long now = System.currentTimeMillis();
            data.setSkillCooldown(skillId, now);

            // Apply Global Cooldown
            double baseGcd = skill.getGlobalCooldown(level);
            if (baseGcd > 0) {
                data.applyGlobalCooldown(baseGcd);
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

                // [NEW] Load Global Cooldown
                skill.setGlobalCooldownBase(cond.getDouble("global-cooldown", 0.5));
                skill.setGlobalCooldownPerLevel(cond.getDouble("global-cooldown-per-level", 0));

                skill.setSpCostBase(cond.getInt("sp-cost", 0));
                skill.setSpCostPerLevel(cond.getInt("sp-cost-per-level", 0));
                skill.setCastTime(cond.getDouble("cast-time", 0));
                skill.setRequiredLevel(cond.getInt("required-level", 1));
            }

            if (section.contains("actions")) {
                List<Map<?, ?>> actionList = section.getMapList("actions");
                for (Map<?, ?> actionMap : actionList) {
                    SkillAction action = parseAction(actionMap);
                    if (action != null) skill.addAction(action);
                }
            }

            skillMap.put(key, skill);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error parsing skill: " + key, e);
        }
    }

    private SkillAction parseAction(Map<?, ?> map) {
        String typeStr = (String) map.get("type");
        if (typeStr == null) return null;

        try {
            ActionType type = ActionType.valueOf(typeStr.toUpperCase());
            return switch (type) {
                case DAMAGE -> parseDamageAction(map);
                case HEAL -> parseHealAction(map);
                case APPLY_EFFECT -> parseEffectAction(map);
                case SOUND -> parseSoundAction(map);
                case PARTICLE -> parseParticleAction(map);
                case PROJECTILE -> parseProjectileAction(map);
                case AREA_EFFECT -> parseAreaAction(map);
                case VELOCITY -> parseVelocityAction(map);
                case COMMAND -> parseCommandAction(map);
                case DELAY -> parseDelayAction(map);
                case TELEPORT -> parseTeleportAction(map);
                case APPLY_POTION -> parsePotionAction(map);
                case RAYCAST -> parseRaycastAction(map);
                case SPAWN_ENTITY -> parseSpawnEntityAction(map);
                case LOOP -> parseLoopAction(map);
            };
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse action: " + typeStr);
            return null;
        }
    }

    // ... (keep all existing parse methods like parseDamageAction, parseHealAction, etc.)

    private DamageAction parseDamageAction(Map<?, ?> map) {
        double baseDamage = ((Number) map.getOrDefault("base-damage", 10.0)).doubleValue();
        double damagePerLevel = ((Number) map.getOrDefault("damage-per-level", 0.0)).doubleValue();
        String damageType = (String) map.getOrDefault("damage-type", "PHYSICAL");
        return new DamageAction(baseDamage, damagePerLevel, damageType);
    }

    private HealAction parseHealAction(Map<?, ?> map) {
        double baseHeal = ((Number) map.getOrDefault("base-heal", 10.0)).doubleValue();
        double healPerLevel = ((Number) map.getOrDefault("heal-per-level", 0.0)).doubleValue();
        return new HealAction(baseHeal, healPerLevel);
    }

    private EffectAction parseEffectAction(Map<?, ?> map) {
        String effectId = (String) map.getOrDefault("effect-id", "generic_buff");
        String effectTypeStr = (String) map.getOrDefault("effect-type", "BUFF");
        int duration = ((Number) map.getOrDefault("duration", 100)).intValue();
        double power = ((Number) map.getOrDefault("power", 1.0)).doubleValue();
        String statKey = (String) map.get("stat-key");

        EffectType effectType;
        try {
            effectType = EffectType.valueOf(effectTypeStr.toUpperCase());
        } catch (Exception e) {
            effectType = EffectType.BUFF;
        }

        return new EffectAction(effectId, effectType, duration, power, statKey);
    }

    private SoundAction parseSoundAction(Map<?, ?> map) {
        String sound = (String) map.getOrDefault("sound", "ENTITY_PLAYER_LEVELUP");
        float volume = ((Number) map.getOrDefault("volume", 1.0)).floatValue();
        float pitch = ((Number) map.getOrDefault("pitch", 1.0)).floatValue();
        return new SoundAction(sound, volume, pitch);
    }

    private ParticleAction parseParticleAction(Map<?, ?> map) {
        String particle = (String) map.getOrDefault("particle", "FLAME");
        int count = ((Number) map.getOrDefault("count", 10)).intValue();
        double offsetX = ((Number) map.getOrDefault("offset-x", 0.5)).doubleValue();
        double offsetY = ((Number) map.getOrDefault("offset-y", 0.5)).doubleValue();
        double offsetZ = ((Number) map.getOrDefault("offset-z", 0.5)).doubleValue();
        double speed = ((Number) map.getOrDefault("speed", 0.1)).doubleValue();
        return new ParticleAction(particle, count, offsetX, offsetY, offsetZ, speed);
    }

    private ProjectileAction parseProjectileAction(Map<?, ?> map) {
        String projectileType = (String) map.getOrDefault("projectile-type", "ARROW");
        double speed = ((Number) map.getOrDefault("speed", 1.5)).doubleValue();
        return new ProjectileAction(projectileType, speed);
    }

    private AreaAction parseAreaAction(Map<?, ?> map) {
        double radius = ((Number) map.getOrDefault("radius", 5.0)).doubleValue();
        String effectId = (String) map.getOrDefault("effect-id", "area_damage");
        return new AreaAction(radius, effectId);
    }

    private VelocityAction parseVelocityAction(Map<?, ?> map) {
        double x = ((Number) map.getOrDefault("x", 0.0)).doubleValue();
        double y = ((Number) map.getOrDefault("y", 1.0)).doubleValue();
        double z = ((Number) map.getOrDefault("z", 0.0)).doubleValue();
        double multiplier = ((Number) map.getOrDefault("multiplier", 1.0)).doubleValue();
        return new VelocityAction(x, y, z, multiplier);
    }

    private CommandAction parseCommandAction(Map<?, ?> map) {
        String command = (String) map.getOrDefault("command", "say Hello");
        boolean asOp = (Boolean) map.getOrDefault("as-op", false);
        return new CommandAction(command, asOp);
    }

    private DelayAction parseDelayAction(Map<?, ?> map) {
        long ticks = ((Number) map.getOrDefault("ticks", 20)).longValue();
        return new DelayAction(ticks);
    }

    private TeleportAction parseTeleportAction(Map<?, ?> map) {
        double x = ((Number) map.getOrDefault("x", 0.0)).doubleValue();
        double y = ((Number) map.getOrDefault("y", 64.0)).doubleValue();
        double z = ((Number) map.getOrDefault("z", 0.0)).doubleValue();
        String world = (String) map.get("world");
        return new TeleportAction(x, y, z, world);
    }

    private PotionAction parsePotionAction(Map<?, ?> map) {
        String potionType = (String) map.getOrDefault("potion-type", "SPEED");
        int duration = ((Number) map.getOrDefault("duration", 100)).intValue();
        int amplifier = ((Number) map.getOrDefault("amplifier", 0)).intValue();
        return new PotionAction(potionType, duration, amplifier);
    }

    private RaycastAction parseRaycastAction(Map<?, ?> map) {
        double maxDistance = ((Number) map.getOrDefault("max-distance", 10.0)).doubleValue();
        String onHitEffect = (String) map.get("on-hit-effect");
        return new RaycastAction(maxDistance, onHitEffect);
    }

    private SpawnEntityAction parseSpawnEntityAction(Map<?, ?> map) {
        String entityType = (String) map.getOrDefault("entity-type", "ZOMBIE");
        return new SpawnEntityAction(entityType);
    }

    private LoopAction parseLoopAction(Map<?, ?> map) {
        int iterations = ((Number) map.getOrDefault("iterations", 3)).intValue();
        long delayTicks = ((Number) map.getOrDefault("delay-ticks", 20)).longValue();

        List<SkillAction> subActions = new ArrayList<>();
        if (map.containsKey("actions")) {
            List<Map<?, ?>> actionList = (List<Map<?, ?>>) map.get("actions");
            for (Map<?, ?> actionMap : actionList) {
                SkillAction action = parseAction(actionMap);
                if (action != null) subActions.add(action);
            }
        }

        return new LoopAction(iterations, delayTicks, subActions);
    }

    // --- File Management ---

    public File getRootDir() {
        return skillFolder;
    }

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

        // [NEW] Save Global Cooldown
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