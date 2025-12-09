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
import org.rostats.engine.action.impl.DelayAction; // ADDED

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SkillManager {

    private final ThaiRoCorePlugin plugin;
    private final Map<String, SkillData> skillMap = new HashMap<>();
    private final File skillFolder;

    // [NEW] Global Cooldown Constant (0.5 seconds = 500ms)
    private static final long GLOBAL_COOLDOWN_MILLIS = 500L;

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

        List<SkillAction> finalActions = new LinkedList<>(skill.getActions()); // Create mutable list of actions

        // Resource & Cooldown Check (Only for Players)
        if (!isPassive && caster instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

            // 1. Level Check
            if (data.getBaseLevel() < skill.getRequiredLevel()) {
                player.sendMessage("§cLevel too low! Required: " + skill.getRequiredLevel());
                return;
            }

            // 2. Cooldown Check (Skill CD & Global CD)
            long now = System.currentTimeMillis();

            // --- NEW: Global Cooldown Check ---
            long lastGlobalUse = data.getLastGlobalSkillUse();
            if (now - lastGlobalUse < GLOBAL_COOLDOWN_MILLIS) {
                // Calculate remaining time for display
                double remainingSeconds = (GLOBAL_COOLDOWN_MILLIS - (now - lastGlobalUse)) / 1000.0;
                player.sendMessage("§cGlobal Cooldown active! Remaining: " + String.format("%.2f", remainingSeconds) + "s");
                return; // Global Cooldown (GCD) active
            }
            // ----------------------------------

            // --- MODIFIED: Skill Cooldown Check (Applying Player Reduction) ---
            long lastUse = data.getSkillCooldown(skillId);
            double baseCooldownSeconds = skill.getCooldown(level);

            // Calculate final skill cooldown applying player's CD reduction (from base stats + gear/effect)
            double skillCDReduction = data.getSkillCooldownReductionPercent() + data.getEffectBonus("SKILL_CD_PERCENT");
            double finalCooldownSeconds = baseCooldownSeconds * Math.max(0.0, 1.0 - (skillCDReduction / 100.0));
            long cooldownMillis = (long) (finalCooldownSeconds * 1000);

            if (now - lastUse < cooldownMillis) {
                double remainingSeconds = (cooldownMillis - (now - lastUse)) / 1000.0;
                player.sendMessage("§cSkill is on cooldown! Remaining: " + String.format("%.2f", remainingSeconds) + "s");
                return; // On cooldown
            }
            // -----------------------------------------------------------------

            // 3. SP Cost Check
            int spCost = skill.getSpCost(level);
            if (data.getCurrentSP() < spCost) {
                player.sendMessage("§cNot enough SP!");
                return;
            }

            // 4. CAST TIME CALCULATION (FIX for INT CT Reduction)
            double baseCastTimeSeconds = skill.getCastTime();
            if (baseCastTimeSeconds > 0.0) {
                double finalCastTimeSeconds = data.getFinalCastTime(baseCastTimeSeconds); // Use new method

                if (finalCastTimeSeconds > 0.0) {
                    long castTimeTicks = (long) (finalCastTimeSeconds * 20.0);
                    // Prepend the calculated delay to the action list.
                    finalActions.add(0, new DelayAction(castTimeTicks));
                }
            }

            // 5. Deduct SP & Set Cooldown
            data.setCurrentSP(data.getCurrentSP() - spCost);
            data.setSkillCooldown(skillId, now);
            data.setLastGlobalSkillUse(now); // [NEW] Set Global Cooldown
            plugin.getManaManager().updateBar(player);
        }

        // Execute via SkillRunner (using finalActions)
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
                skill.setSpCostBase(cond.getInt("sp-cost", 0));
                skill.setSpCostPerLevel(cond.getInt("sp-cost-per-level", 0));
                skill.setCastTime(cond.getDouble("cast-time", 0));
                skill.setRequiredLevel(cond.getInt("required-level", 1));
            }

            if (section.contains("actions")) {
                List<Map<?, ?>> actionList = section.getMapList("actions");
                for (Map<?, ?> rawMap : actionList) {
                    try {
                        Map<String, Object> actionMap = (Map<String, Object>) rawMap;
                        String typeStr = (String) actionMap.get("type");
                        ActionType type = ActionType.valueOf(typeStr);
                        SkillAction action = parseAction(actionMap);
                        if (action != null) skill.addAction(action);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Invalid action structure in skill " + key + ": " + e.getMessage());
                    }
                }
            }

            skillMap.put(key, skill);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load skill key: " + key);
        }
    }

    private SkillAction parseAction(Map<String, Object> map) {
        try {
            String typeStr = (String) map.get("type");
            ActionType type = ActionType.valueOf(typeStr);

            switch (type) {
                case DAMAGE:
                    return new DamageAction(plugin,
                            String.valueOf(map.getOrDefault("formula", "ATK")),
                            String.valueOf(map.getOrDefault("element", "NEUTRAL")));

                case HEAL:
                    return new HealAction(plugin,
                            String.valueOf(map.getOrDefault("formula", "10")),
                            (boolean) map.getOrDefault("is-mana", false),
                            (boolean) map.getOrDefault("self-only", true));

                case APPLY_EFFECT:
                    String eid = (String) map.getOrDefault("effect-id", "unknown");
                    String effTypeStr = (String) map.getOrDefault("effect-type", "STAT_MODIFIER");
                    EffectType effType = EffectType.valueOf(effTypeStr);
                    int lv = map.containsKey("level") ? ((Number) map.get("level")).intValue() : 1;
                    double pw = map.containsKey("power") ? ((Number) map.get("power")).doubleValue() : 0.0;
                    long dr = map.containsKey("duration") ? ((Number) map.get("duration")).longValue() : 100L;
                    double ch = map.containsKey("chance") ? ((Number) map.get("chance")).doubleValue() : 1.0;
                    String sk = (String) map.getOrDefault("stat-key", null);
                    return new EffectAction(plugin, eid, effType, lv, pw, dr, ch, sk);

                case SOUND:
                    String soundName = (String) map.getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
                    float volume = map.containsKey("volume") ? ((Number) map.get("volume")).floatValue() : 1.0f;
                    float pitch = map.containsKey("pitch") ? ((Number) map.get("pitch")).floatValue() : 1.0f;
                    return new SoundAction(soundName, volume, pitch);

                case PARTICLE:
                    // Supports Placeholder & Shapes (Updated)
                    return new ParticleAction(plugin,
                            (String) map.getOrDefault("particle", "VILLAGER_HAPPY"),
                            String.valueOf(map.getOrDefault("count", "5")),
                            String.valueOf(map.getOrDefault("speed", "0.1")),
                            (String) map.getOrDefault("shape", "POINT"),
                            String.valueOf(map.getOrDefault("radius", "0.5")),
                            String.valueOf(map.getOrDefault("points", "20"))
                    );

                case POTION:
                    String potion = (String) map.getOrDefault("potion", "SPEED");
                    int pDuration = map.containsKey("duration") ? ((Number) map.get("duration")).intValue() : 60;
                    int amp = map.containsKey("amplifier") ? ((Number) map.get("amplifier")).intValue() : 0;
                    boolean selfOnly = (boolean) map.getOrDefault("self-only", true);
                    return new PotionAction(potion, pDuration, amp, selfOnly);

                case TELEPORT:
                    double range = map.containsKey("range") ? ((Number) map.get("range")).doubleValue() : 5.0;
                    boolean toTarget = (boolean) map.getOrDefault("to-target", false);
                    return new TeleportAction(range, toTarget);

                case PROJECTILE:
                    String projType = (String) map.getOrDefault("projectile", "ARROW");
                    double projSpeed = map.containsKey("speed") ? ((Number) map.get("speed")).doubleValue() : 1.0;
                    String onHit = (String) map.getOrDefault("on-hit", "none");
                    return new ProjectileAction(plugin, projType, projSpeed, onHit);

                case AREA_EFFECT:
                    double radius = map.containsKey("radius") ? ((Number) map.get("radius")).doubleValue() : 5.0;
                    String tType = (String) map.getOrDefault("target-type", "ENEMY");
                    String subSkill = (String) map.getOrDefault("sub-skill", "none");
                    int maxT = map.containsKey("max-targets") ? ((Number) map.get("max-targets")).intValue() : 10;
                    return new AreaAction(plugin, radius, tType, subSkill, maxT);

                case DELAY:
                    long ticks = map.containsKey("ticks") ? ((Number) map.get("ticks")).longValue() : 20L;
                    return new DelayAction(ticks);

                case VELOCITY:
                    double vx = map.containsKey("x") ? ((Number) map.get("x")).doubleValue() : 0.0;
                    double vy = map.containsKey("y") ? ((Number) map.get("y")).doubleValue() : 0.0;
                    double vz = map.containsKey("z") ? ((Number) map.get("z")).doubleValue() : 0.0;
                    boolean vAdd = (boolean) map.getOrDefault("add", true);
                    return new VelocityAction(vx, vy, vz, vAdd);

                case LOOP:
                    String start = String.valueOf(map.getOrDefault("start", "0"));
                    String end = String.valueOf(map.getOrDefault("end", "10"));
                    String step = String.valueOf(map.getOrDefault("step", "1"));
                    String var = (String) map.getOrDefault("var", "i");

                    List<SkillAction> subActions = new ArrayList<>();
                    if (map.containsKey("actions")) {
                        List<Map<?, ?>> subs = (List<Map<?, ?>>) map.get("actions");
                        for (Map<?, ?> subMap : subs) {
                            SkillAction sub = parseAction((Map<String, Object>) subMap);
                            if (sub != null) subActions.add(sub);
                        }
                    }
                    return new LoopAction(plugin, start, end, step, var, subActions);

                case COMMAND:
                    String cmd = (String) map.getOrDefault("command", "say Hi %player%");
                    boolean console = (boolean) map.getOrDefault("as-console", false);
                    return new CommandAction(cmd, console);

                case RAYCAST: // [NEW] RaycastAction
                    String rangeExpr = String.valueOf(map.getOrDefault("range", "10.0"));
                    String subSkillId = (String) map.getOrDefault("sub-skill", "none");
                    String targetType = (String) map.getOrDefault("target-type", "SINGLE");
                    return new RaycastAction(plugin, rangeExpr, subSkillId, targetType);

                case SPAWN_ENTITY: // [NEW] SpawnEntityAction
                    String entityType = (String) map.getOrDefault("entity-type", "LIGHTNING_BOLT");
                    String onSpawnSkill = (String) map.getOrDefault("skill-id", "none");
                    return new SpawnEntityAction(plugin, entityType, onSpawnSkill);

                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- File Utils (Same as before) ---

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
            config.set(key + ".conditions.required-level", 1);

            List<Map<String, Object>> actions = new ArrayList<>();

            Map<String, Object> sound = new HashMap<>();
            sound.put("type", "SOUND");
            sound.put("sound", "ENTITY_GHAST_SHOOT");
            actions.add(sound);

            Map<String, Object> delay = new HashMap<>();
            delay.put("type", "DELAY");
            delay.put("ticks", 10);
            actions.add(delay);

            Map<String, Object> proj = new HashMap<>();
            proj.put("type", "PROJECTILE");
            proj.put("projectile", "SMALL_FIREBALL");
            proj.put("speed", 1.5);
            proj.put("on-hit", "fireball_explode");
            actions.add(proj);

            config.set(key + ".actions", actions);

            config.save(example);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}