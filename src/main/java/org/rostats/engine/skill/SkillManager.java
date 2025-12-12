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

    private static final double BASE_GCD_SECONDS = 0.5;

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

        // Status Check
        if (plugin.getEffectManager().hasEffect(caster, EffectType.CROWD_CONTROL, "STUN")) {
            if (caster instanceof Player) caster.sendMessage("§cYou are stunned!");
            return;
        }
        if (plugin.getEffectManager().hasEffect(caster, EffectType.CROWD_CONTROL, "SILENCE")) {
            if (caster instanceof Player) caster.sendMessage("§cYou are silenced!");
            return;
        }

        // Range Check
        if (!isPassive && target != null && !target.equals(caster)) {
            double range = skill.getCastRange();
            if (range > 0 && caster.getLocation().distance(target.getLocation()) > range) {
                if (caster instanceof Player) caster.sendMessage("§cTarget is out of range! (Max: " + range + "m)");
                return;
            }
        }

        List<SkillAction> actionsToRun = new LinkedList<>(skill.getActions()); // Actions original

        // Resource, Cooldown & Delay Checks
        if (!isPassive && caster instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

            // 1. Level Check
            if (data.getBaseLevel() < skill.getRequiredLevel()) {
                player.sendMessage("§cLevel too low! Required: " + skill.getRequiredLevel());
                return;
            }

            long now = System.currentTimeMillis();

            // 2. Global Delay Check
            long globalDelayEnd = data.getGlobalDelayEndTime();
            if (now < globalDelayEnd) {
                return;
            }

            // 3. Skill Cooldown Check
            long lastUse = data.getSkillCooldown(skillId);
            double baseCooldownSeconds = skill.getCooldown(level);

            double flatRed = data.getSkillCooldownReductionFlat() + data.getEffectBonus("SKILL_CD_FLAT");
            double pctRed = data.getSkillCooldownReductionPercent() + data.getEffectBonus("SKILL_CD_PERCENT");

            double cdAfterFlat = Math.max(0.0, baseCooldownSeconds - flatRed);
            double finalCooldownSeconds = cdAfterFlat * Math.max(0.0, 1.0 - (pctRed / 100.0));

            long cooldownMillis = (long) (finalCooldownSeconds * 1000);

            if (now - lastUse < cooldownMillis) {
                double remainingSeconds = (cooldownMillis - (now - lastUse)) / 1000.0;
                player.sendMessage("§cSkill is on cooldown! Remaining: " + String.format("%.1f", remainingSeconds) + "s");
                return;
            }

            // 4. SP Cost Check
            int spCost = skill.getSpCost(level);
            if (data.getCurrentSP() < spCost) {
                player.sendMessage("§cNot enough SP!");
                return;
            }

            // --- Timeline Construction (Fixed Cast Time Logic) ---
            double preMotion = skill.getPreMotion();
            double finalCastTimeSeconds = data.calculateTotalCastTime(
                    skill.getVariableCastTime(),
                    skill.getVariableCastTimeReduction(),
                    skill.getFixedCastTime(),
                    skill.getFixedCastTimeReduction()
            );

            // Action List Modifier (Adding Delays at the start of the final list)
            List<SkillAction> timelineModifiers = new LinkedList<>();

            // 1. Add Cast Time Delay
            if (finalCastTimeSeconds > 0.0) {
                long castTicks = (long) (finalCastTimeSeconds * 20.0);
                timelineModifiers.add(new DelayAction(castTicks));
            }
            // 2. Add Pre-Motion Delay
            if (preMotion > 0.0) {
                long motionTicks = (long) (preMotion * 20.0);
                timelineModifiers.add(new DelayAction(motionTicks));
            }

            // Combine modifiers and original actions
            actionsToRun.addAll(0, timelineModifiers);

            // --- Apply Costs & Delays ---
            data.setCurrentSP(data.getCurrentSP() - spCost);
            data.setSkillCooldown(skillId, now);

            // Priority Lock Calculation
            double motion = 0.0;
            double postMotion = skill.getPostMotion();

            double baseACD = skill.getAfterCastDelayBase();
            double acdRedPct = data.getAcdReductionPercent();
            double acdRedFlat = data.getAcdReductionFlat();
            double finalACD = Math.max(0.0, baseACD * Math.max(0.0, 1.0 - (acdRedPct / 100.0)) - acdRedFlat);

            double baseGCD = BASE_GCD_SECONDS;
            double gcdRedPct = data.getGcdReductionPercent();
            double gcdRedFlat = data.getGcdReductionFlat();
            double finalGCD = Math.max(0.0, baseGCD * Math.max(0.0, 1.0 - (gcdRedPct / 100.0)) - gcdRedFlat);

            double lockTimeSeconds = Math.max(motion, Math.max(postMotion, Math.max(finalACD, finalGCD)));

            if (lockTimeSeconds > 0) {
                data.setGlobalDelayEndTime(now + (long)(lockTimeSeconds * 1000.0));
            }

            plugin.getManaManager().updateBar(player);
        }

        // Execute Actions
        // [UPDATE]: ส่ง skillId เข้าไปใน SkillRunner
        SkillRunner runner = new SkillRunner(plugin, caster, target, level, actionsToRun, skillId);
        // [FIX] Start the runner ASYNCHRONOUSLY to prevent main thread blocking
        // and ensure the DelayAction is processed correctly by runNext()'s task schedule.
        plugin.getServer().getScheduler().runTask(plugin, runner::runNext);
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

            if (section.contains("lore")) {
                skill.setLore(section.getStringList("lore"));
            }

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

            // Meta Data
            skill.setSkillType(section.getString("type", "PHYSICAL"));
            skill.setAttackType(section.getString("attack-type", "MELEE"));
            skill.setCastRange(section.getDouble("range", 5.0));

            ConfigurationSection cond = section.getConfigurationSection("conditions");
            if (cond != null) {
                skill.setCooldownBase(cond.getDouble("cooldown", 0));
                skill.setCooldownPerLevel(cond.getDouble("cooldown-per-level", 0));
                skill.setSpCostBase(cond.getInt("sp-cost", 0));
                skill.setSpCostPerLevel(cond.getInt("sp-cost-per-level", 0));

                skill.setVariableCastTime(cond.getDouble("variable-cast-time", 0.0));
                skill.setVariableCastTimeReduction(cond.getDouble("variable-ct-pct", 0.0));
                skill.setFixedCastTime(cond.getDouble("fixed-cast-time", 0.0));
                skill.setFixedCastTimeReduction(cond.getDouble("fixed-ct-pct", 0.0));

                skill.setPreMotion(cond.getDouble("pre-motion", 0.0));
                skill.setPostMotion(cond.getDouble("post-motion", 0.0));
                skill.setAfterCastDelayBase(cond.getDouble("acd-base", 0.0));

                skill.setRequiredLevel(cond.getInt("required-level", 1));
            }

            if (section.contains("actions")) {
                List<Map<?, ?>> actionList = section.getMapList("actions");
                for (Map<?, ?> rawMap : actionList) {
                    try {
                        Map<String, Object> actionMap = (Map<String, Object>) rawMap;
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
                    // [FIX] Added 'bypass-def' argument (default false)
                    return new DamageAction(plugin,
                            String.valueOf(map.getOrDefault("formula", "ATK")),
                            String.valueOf(map.getOrDefault("element", "NEUTRAL")),
                            (boolean) map.getOrDefault("bypass-def", false));

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
                    return new ParticleAction(plugin,
                            (String) map.getOrDefault("particle", "VILLAGER_HAPPY"),
                            String.valueOf(map.getOrDefault("count", "5")),
                            String.valueOf(map.getOrDefault("speed", "0.1")),
                            (String) map.getOrDefault("shape", "POINT"),
                            String.valueOf(map.getOrDefault("radius", "0.5")),
                            String.valueOf(map.getOrDefault("points", "20")),
                            (String) map.getOrDefault("color", "0,0,0"),
                            (String) map.getOrDefault("rotation", "0,0,0"),
                            (String) map.getOrDefault("offset", "0,0,0")
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

                case RAYCAST:
                    String rangeExpr = String.valueOf(map.getOrDefault("range", "10.0"));
                    String subSkillId = (String) map.getOrDefault("sub-skill", "none");
                    String targetType = (String) map.getOrDefault("target-type", "SINGLE");
                    return new RaycastAction(plugin, rangeExpr, subSkillId, targetType);

                case SPAWN_ENTITY:
                    String entityType = (String) map.getOrDefault("entity-type", "LIGHTNING_BOLT");
                    String onSpawnSkill = (String) map.getOrDefault("skill-id", "none");
                    return new SpawnEntityAction(plugin, entityType, onSpawnSkill);

                case SELECT_TARGET:
                    String modeStr = String.valueOf(map.getOrDefault("mode", "SELF"));
                    double tRadius = map.containsKey("radius") ? ((Number) map.get("radius")).doubleValue() : 10.0;
                    TargetSelectorAction.SelectorMode mode = TargetSelectorAction.SelectorMode.SELF;
                    try { mode = TargetSelectorAction.SelectorMode.valueOf(modeStr); } catch(Exception e){}
                    return new TargetSelectorAction(mode, tRadius);

                case CONDITION:
                    String formula = String.valueOf(map.getOrDefault("formula", "true"));
                    List<SkillAction> success = new ArrayList<>();
                    if (map.containsKey("success")) {
                        List<Map<?, ?>> sList = (List<Map<?, ?>>) map.get("success");
                        for (Map<?, ?> m : sList) success.add(parseAction((Map<String, Object>)m));
                    }
                    List<SkillAction> fail = new ArrayList<>();
                    if (map.containsKey("fail")) {
                        List<Map<?, ?>> fList = (List<Map<?, ?>>) map.get("fail");
                        for (Map<?, ?> m : fList) fail.add(parseAction((Map<String, Object>)m));
                    }
                    return new ConditionAction(plugin, formula, success, fail);

                case SET_VARIABLE:
                    String vName = String.valueOf(map.getOrDefault("var", "temp"));
                    String vVal = String.valueOf(map.getOrDefault("val", "0"));
                    return new SetVariableAction(plugin, vName, vVal);

                default:
                    return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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

    public void createSkill(File parent, String fileName) {
        String name = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        File file = new File(parent, name);
        if (file.exists()) return;

        try {
            file.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = name.replace(".yml", "").toLowerCase().replace(" ", "_");
            writeDefaultSkill(config, id, name.replace(".yml", ""));
            config.save(file);
            loadSkills();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addSkillToFile(File file, String skillName) {
        if (!file.exists()) return;
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            String id = skillName.toLowerCase().replace(" ", "_");

            if (config.contains(id)) return; // กันซ้ำ

            writeDefaultSkill(config, id, skillName);
            config.save(file);
            loadSkills();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeDefaultSkill(YamlConfiguration config, String id, String displayName) {
        config.set(id + ".display-name", displayName);
        config.set(id + ".lore", Arrays.asList("&7Description line 1", "&7Description line 2"));
        config.set(id + ".icon", "BOOK");
        config.set(id + ".max-level", 1);
        config.set(id + ".trigger", "CAST");
        config.set(id + ".conditions.cooldown", 1.0);
        config.set(id + ".conditions.sp-cost", 0);
        config.set(id + ".conditions.required-level", 1);
        config.set(id + ".actions", new ArrayList<>());
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
        config.set(key + ".lore", skill.getLore());
        config.set(key + ".icon", skill.getIcon().name());
        config.set(key + ".max-level", skill.getMaxLevel());
        config.set(key + ".trigger", skill.getTrigger().name());

        config.set(key + ".type", skill.getSkillType());
        config.set(key + ".attack-type", skill.getAttackType());
        config.set(key + ".range", skill.getCastRange());

        config.set(key + ".conditions.cooldown", skill.getCooldownBase());
        config.set(key + ".conditions.cooldown-per-level", skill.getCooldownPerLevel());
        config.set(key + ".conditions.sp-cost", skill.getSpCostBase());
        config.set(key + ".conditions.sp-cost-per-level", skill.getSpCostPerLevel());

        config.set(key + ".conditions.variable-cast-time", skill.getVariableCastTime());
        config.set(key + ".conditions.variable-ct-pct", skill.getVariableCastTimeReduction());
        config.set(key + ".conditions.fixed-cast-time", skill.getFixedCastTime());
        config.set(key + ".conditions.fixed-ct-pct", skill.getFixedCastTimeReduction());

        config.set(key + ".conditions.pre-motion", skill.getPreMotion());
        config.set(key + ".conditions.post-motion", skill.getPostMotion());
        config.set(key + ".conditions.acd-base", skill.getAfterCastDelayBase());

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
            writeDefaultSkill(config, "fireball", "Fireball");

            List<Map<String, Object>> actions = new ArrayList<>();
            Map<String, Object> sound = new HashMap<>(); sound.put("type", "SOUND"); sound.put("sound", "ENTITY_GHAST_SHOOT"); actions.add(sound);
            Map<String, Object> delay = new HashMap<>(); delay.put("type", "DELAY"); delay.put("ticks", 10); actions.add(delay);
            Map<String, Object> proj = new HashMap<>(); proj.put("type", "PROJECTILE"); proj.put("projectile", "SMALL_FIREBALL"); proj.put("speed", 1.5); proj.put("on-hit", "fireball_explode"); actions.add(proj);
            config.set("fireball.actions", actions);

            config.save(example);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}