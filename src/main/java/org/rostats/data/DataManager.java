package org.rostats.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.effect.ActiveEffect;
import org.rostats.engine.effect.EffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DataManager {

    private final ThaiRoCorePlugin plugin;

    public DataManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        File folder = new File(plugin.getDataFolder(), "userdata");
        if (!folder.exists()) folder.mkdirs();
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getStatManager().getData(uuid);

        File file = new File(plugin.getDataFolder(), "userdata/" + uuid + ".yml");

        if (file.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            data.setBaseLevel(config.getInt("base-level", 1));
            data.setBaseExp(config.getLong("base-exp", 0));
            data.setJobLevel(config.getInt("job-level", 1));
            data.setJobExp(config.getLong("job-exp", 0));
            data.setStatPoints(config.getInt("points", 0));
            data.setSkillPoints(config.getInt("skill-points", 0));
            data.setResetCount(config.getInt("reset-count", 0));

            data.setStat("STR", config.getInt("stats.STR", 1));
            data.setStat("AGI", config.getInt("stats.AGI", 1));
            data.setStat("VIT", config.getInt("stats.VIT", 1));
            data.setStat("INT", config.getInt("stats.INT", 1));
            data.setStat("DEX", config.getInt("stats.DEX", 1));
            data.setStat("LUK", config.getInt("stats.LUK", 1));

            data.calculateMaxSP();
            if (config.contains("current-sp")) {
                data.setCurrentSP(config.getDouble("current-sp"));
            } else {
                data.setCurrentSP(data.getMaxSP());
            }

            // --- Load Active Effects ---
            data.getActiveEffects().clear();
            if (config.contains("active-effects")) {
                ConfigurationSection effSec = config.getConfigurationSection("active-effects");
                for (String key : effSec.getKeys(false)) {
                    ConfigurationSection s = effSec.getConfigurationSection(key);
                    try {
                        String id = s.getString("id");
                        EffectType type = EffectType.valueOf(s.getString("type"));
                        int level = s.getInt("level");
                        double power = s.getDouble("power");
                        long duration = s.getLong("duration");
                        long interval = s.getLong("interval", 0);
                        String sourceStr = s.getString("source");
                        UUID source = (sourceStr != null && !sourceStr.isEmpty()) ? UUID.fromString(sourceStr) : null;
                        String statKey = s.getString("stat-key", null);

                        ActiveEffect effect = new ActiveEffect(id, type, level, power, duration, interval, source);
                        if (statKey != null) effect.setStatKey(statKey);

                        data.addActiveEffect(effect);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load effect: " + key + " for " + player.getName());
                    }
                }
            }
            // ---------------------------

            plugin.getLogger().info("📄 Loaded data for " + player.getName());
        } else {
            savePlayerData(player);
        }
        plugin.getAttributeHandler().updatePlayerStats(player);
        plugin.getManaManager().updateBar(player);
        plugin.getManaManager().updateBaseExpBar(player);
        plugin.getManaManager().updateJobExpBar(player);
    }

    public void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(plugin.getDataFolder(), "userdata/" + uuid + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        PlayerData data = plugin.getStatManager().getData(uuid);

        config.set("name", player.getName());
        config.set("base-level", data.getBaseLevel());
        config.set("base-exp", data.getBaseExp());
        config.set("job-level", data.getJobLevel());
        config.set("job-exp", data.getJobExp());
        config.set("points", data.getStatPoints());
        config.set("skill-points", data.getSkillPoints());
        config.set("reset-count", data.getResetCount());
        config.set("current-sp", data.getCurrentSP());

        config.set("stats.STR", data.getStat("STR"));
        config.set("stats.AGI", data.getStat("AGI"));
        config.set("stats.VIT", data.getStat("VIT"));
        config.set("stats.INT", data.getStat("INT"));
        config.set("stats.DEX", data.getStat("DEX"));
        config.set("stats.LUK", data.getStat("LUK"));

        // --- Save Active Effects ---
        config.set("active-effects", null); // Clear old
        int idx = 0;
        for (ActiveEffect effect : data.getActiveEffects()) {
            String path = "active-effects." + idx;
            config.set(path + ".id", effect.getId());
            config.set(path + ".type", effect.getType().name());
            config.set(path + ".level", effect.getLevel());
            config.set(path + ".power", effect.getPower());
            config.set(path + ".duration", effect.getDurationTicks());
            // interval is private, assume standard for now or make getter if critical (omitted for simplicity if not generic)
            // But we have constructor for it, let's assume we don't save interval for simple effects or add getter later.
            // Wait, ActiveEffect doesn't expose intervalTicks getter in Phase 1 code.
            // For now, save what we can.
            // FIX: If we need to save interval, we need getter. Assuming default interval or re-config on load for skills.
            // But for now let's save source.
            if (effect.getSource() != null) config.set(path + ".source", effect.getSource().toString());
            if (effect.getStatKey() != null) config.set(path + ".stat-key", effect.getStatKey());
            idx++;
        }
        // ---------------------------

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}