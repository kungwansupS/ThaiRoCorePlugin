package org.rostats.itemeditor;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.rostats.ROStatsPlugin;

public class ItemEditorPlugin extends JavaPlugin {

    private ROStatsPlugin corePlugin;
    private ItemAttributeManager attributeManager;

    @Override
    public void onEnable() {
        // 1. Check for Core Plugin dependency
        // ThaiRoStats-Core is the name in plugin.yml
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ThaiRoStats-Core");
        if (plugin instanceof ROStatsPlugin roStatsPlugin) {
            this.corePlugin = roStatsPlugin;
            getLogger().info("Found ThaiRoStats-Core, integrating item editor.");
        } else {
            getLogger().severe("ThaiRoStats-Core not found! Disabling Item Editor.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Initialize Manager
        this.attributeManager = new ItemAttributeManager(corePlugin);

        // 3. Register Command
        PluginCommand command = getCommand("roitemedit");
        if (command != null) {
            command.setExecutor(new ItemEditorCommand(this, attributeManager));
        }

        // 4. Register Listener
        getServer().getPluginManager().registerEvents(new GUIListener(this, attributeManager), this);

        getLogger().info("✅ ThaiRoStats Item Editor Enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("❌ ThaiRoStats Item Editor Disabled!");
    }

    public ROStatsPlugin getCorePlugin() {
        return corePlugin;
    }

    public ItemAttributeManager getAttributeManager() {
        return attributeManager;
    }
}