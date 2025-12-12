package org.rostats.data.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.effect.ActiveEffect;
import org.rostats.engine.effect.EffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

public class YamlStorage implements PlayerDataStorage {

    private final ThaiRoCorePlugin plugin;
    private final File folder;

    public YamlStorage(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "userdata");
        if (!folder.exists()) folder.mkdirs();
    }

    @Override
    public void loadData(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            saveData(player);
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            PlayerData data = plugin.getStatManager().getData(uuid);

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

            data.getActiveEffects().clear();
            if (config.contains("active-effects")) {
                ConfigurationSection effSec = config.getConfigurationSection("active-effects");
                if (effSec != null) {
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
            }

            plugin.getAttributeHandler().updatePlayerStats(player);
            plugin.getManaManager().updateBar(player);
            plugin.getManaManager().updateBaseExpBar(player);
            plugin.getManaManager().updateJobExpBar(player);
            plugin.getLogger().info("📄 [YAML] Loaded data for " + player.getName());
        });
    }

    @Override
    public void saveData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getStatManager().getData(uuid);
        if (data == null) return;

        YamlConfiguration config = new YamlConfiguration();

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

        int idx = 0;
        for (ActiveEffect effect : new ArrayList<>(data.getActiveEffects())) {
            String path = "active-effects." + idx;
            config.set(path + ".id", effect.getId());
            config.set(path + ".type", effect.getType().name());
            config.set(path + ".level", effect.getLevel());
            config.set(path + ".power", effect.getPower());
            config.set(path + ".duration", effect.getDurationTicks());
            if (effect.getSource() != null) config.set(path + ".source", effect.getSource().toString());
            if (effect.getStatKey() != null) config.set(path + ".stat-key", effect.getStatKey());
            idx++;
        }

        try {
            File file = new File(folder, uuid + ".yml");
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save YAML data for " + player.getName(), e);
        }
    }

    @Override
    public void shutdown() {
        // Nothing for YAML
    }
}