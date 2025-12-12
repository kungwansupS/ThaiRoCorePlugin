package org.rostats.data;

import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.storage.PlayerDataStorage;
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
                // SQL Logic will be implemented in Phase 2
                plugin.getLogger().warning("SQL Storage selected but logic is in Phase 2. Using YAML temporarily.");
                storage = new YamlStorage(plugin);
                break;
            default:
                storage = new YamlStorage(plugin);
                break;
        }

        plugin.getLogger().info("Storage initialized: " + storage.getClass().getSimpleName());
    }

    public void loadPlayerData(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            storage.loadData(player);
        });
    }

    public void savePlayerData(Player player, boolean async) {
        Runnable saveTask = () -> storage.saveData(player);

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