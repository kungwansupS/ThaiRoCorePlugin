package org.rostats.data;

import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.storage.PlayerDataStorage;
import org.rostats.data.storage.SqlStorage;
import org.rostats.data.storage.YamlStorage;

public class DataManager {

    private final ThaiRoCorePlugin plugin;
    private PlayerDataStorage storage;

    public DataManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        initializeStorage();
    }

    public void initializeStorage() {
        String type = plugin.getConfig().getString("storage.type", "YAML").toUpperCase();

        if (storage != null) {
            storage.shutdown();
        }

        switch (type) {
            case "MYSQL":
            case "SQLITE":
                try {
                    storage = new SqlStorage(plugin);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to init SQL Storage, falling back to YAML: " + e.getMessage());
                    storage = new YamlStorage(plugin);
                }
                break;
            default:
                storage = new YamlStorage(plugin);
                break;
        }

        plugin.getLogger().info("Storage initialized: " + storage.getClass().getSimpleName());
    }

    public void loadPlayerData(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            if (storage != null) storage.loadData(player);
        });
    }

    public void savePlayerData(Player player, boolean async) {
        Runnable saveTask = () -> {
            if (storage != null) storage.saveData(player);
        };

        if (async) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, saveTask);
        } else {
            saveTask.run();
        }
    }

    public void savePlayerData(Player player) {
        savePlayerData(player, true);
    }

    public void shutdown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    public void reload() {
        initializeStorage();
    }
}