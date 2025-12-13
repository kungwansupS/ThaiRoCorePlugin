package org.rostats;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.rostats.command.AdminCommand;
import org.rostats.command.PlayerCommand;
import org.rostats.command.SkillCommand;
import org.rostats.command.SkillDebugCommand;
import org.rostats.data.DataManager;
import org.rostats.data.StatManager;
import org.rostats.engine.effect.EffectManager;
import org.rostats.engine.element.ElementManager;
import org.rostats.engine.skill.SkillManager;
import org.rostats.gui.GUIListener;
import org.rostats.handler.*;
import org.rostats.hook.PAPIHook;
import org.rostats.input.ChatInputHandler;
import org.rostats.itemeditor.ItemAttributeManager;
import org.rostats.itemeditor.ItemEditorCommand;
import org.rostats.itemeditor.ItemManager;

import java.util.UUID;

public class ThaiRoCorePlugin extends JavaPlugin implements Listener {

    private StatManager statManager;
    private AttributeHandler attributeHandler;
    private CombatHandler combatHandler;
    private ManaManager manaManager;
    private DataManager dataManager;

    private ProjectileHandler projectileHandler;
    private StatusHandler statusHandler;

    private ItemAttributeManager itemAttributeManager;
    private ItemManager itemManager;
    private ChatInputHandler chatInputHandler;

    private EffectManager effectManager;
    private SkillManager skillManager;
    private ElementManager elementManager;

    // [NEW] TextDisplay Floating Text Manager
    private FloatingTextManager floatingTextManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize Managers
        this.floatingTextManager = new FloatingTextManager(this);

        this.statManager = new StatManager(this);
        this.dataManager = new DataManager(this);
        this.manaManager = new ManaManager(this);
        this.attributeHandler = new AttributeHandler(this);
        this.combatHandler = new CombatHandler(this);

        this.projectileHandler = new ProjectileHandler(this);
        this.statusHandler = new StatusHandler(this);

        this.effectManager = new EffectManager(this);
        this.elementManager = new ElementManager(this);
        this.skillManager = new SkillManager(this);

        this.itemAttributeManager = new ItemAttributeManager(this);
        this.itemManager = new ItemManager(this);
        this.chatInputHandler = new ChatInputHandler(this);

        // Register Events
        getServer().getPluginManager().registerEvents(attributeHandler, this);
        getServer().getPluginManager().registerEvents(combatHandler, this);
        getServer().getPluginManager().registerEvents(manaManager, this);
        getServer().getPluginManager().registerEvents(projectileHandler, this);
        getServer().getPluginManager().registerEvents(statusHandler, this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getPluginManager().registerEvents(new org.rostats.itemeditor.GUIListener(this), this);
        getServer().getPluginManager().registerEvents(chatInputHandler, this);

        // Register Commands
        PluginCommand statusCmd = getCommand("status");
        if (statusCmd != null) statusCmd.setExecutor(new PlayerCommand(this));

        PluginCommand adminCmd = getCommand("roadmin");
        if (adminCmd != null) {
            AdminCommand adminExecutor = new AdminCommand(this);
            adminCmd.setExecutor(adminExecutor);
            adminCmd.setTabCompleter(adminExecutor);
        }

        PluginCommand itemEditCmd = getCommand("roitemedit");
        if (itemEditCmd != null) {
            itemEditCmd.setExecutor(new ItemEditorCommand(this));
        }

        PluginCommand skillCmd = getCommand("roskilleditor");
        if (skillCmd != null) {
            skillCmd.setExecutor(new SkillCommand(this));
        }

        PluginCommand skillDebugCmd = getCommand("skilldebug");
        if (skillDebugCmd != null) {
            SkillDebugCommand debugExecutor = new SkillDebugCommand(this);
            skillDebugCmd.setExecutor(debugExecutor);
            skillDebugCmd.setTabCompleter(debugExecutor);
        }

        // PAPI Hook
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIHook(this).register();
        }

        // Auto Save Task
        long autoSaveTicks = getConfig().getLong("storage.auto-save", 300) * 20L;
        if (autoSaveTicks > 0) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                for (Player player : getServer().getOnlinePlayers()) {
                    dataManager.savePlayerData(player, true);
                }
                getLogger().info("💾 Auto-Saved all player data.");
            }, autoSaveTicks, autoSaveTicks);
        }

        // Passive Effects Task
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (attributeHandler != null) {
                attributeHandler.runPassiveEffectsTask();
            }
        }, 20L, 20L);

        getLogger().info("✅ ThaiRoCorePlugin Enabled!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                dataManager.savePlayerData(player, false);
                if (manaManager != null) manaManager.removeBar(player);
            }
            dataManager.shutdown();
        }

        if (floatingTextManager != null) {
            floatingTextManager.shutdown();
        }

        getLogger().info("❌ ThaiRoCorePlugin Disabled");
    }

    public void reload() {
        reloadConfig();
        if (dataManager != null) dataManager.reload();
        if (combatHandler != null) combatHandler.loadValues();
        skillManager.loadSkills();
        getLogger().info("Configuration Reloaded.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        dataManager.loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dataManager.savePlayerData(event.getPlayer(), true);

        statManager.removeData(event.getPlayer().getUniqueId());
        if (manaManager != null) {
            manaManager.removeBar(event.getPlayer());
            manaManager.removeBaseExpBar(event.getPlayer());
            manaManager.removeJobExpBar(event.getPlayer());
        }
    }

    // --- Floating Text API ---

    public void showFloatingText(UUID playerUUID, String text, double verticalOffset) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) return;

        if (floatingTextManager != null) {
            floatingTextManager.spawn(player.getLocation(), text, 2.0 + verticalOffset);
        }
    }

    public void showFloatingText(UUID playerUUID, String text) {
        showFloatingText(playerUUID, text, 0.25);
    }

    public void showCombatFloatingText(Location loc, String text) {
        if (floatingTextManager != null) {
            floatingTextManager.spawn(loc, text, 1.5);
        }
    }

    // --- Helper Methods ---

    public void showDamageFCT(Location loc, double damage) { showCombatFloatingText(loc, "§f" + String.format("%.0f", damage)); }
    public void showTrueDamageFCT(Location loc, double damage) { showCombatFloatingText(loc, "§6" + String.format("%.0f", damage)); }
    public void showHealHPFCT(Location loc, double value) { showCombatFloatingText(loc, "§a+" + String.format("%.0f", value) + " HP"); }
    public void showHealSPFCT(Location loc, double value) { showCombatFloatingText(loc, "§b+" + String.format("%.0f", value) + " SP"); }
    public void showStatusDamageFCT(Location loc, String status, double value) {
        String color = switch (status.toLowerCase()) {
            case "poison" -> "§2";
            case "burn" -> "§c";
            case "bleed" -> "§4";
            default -> "§7";
        };
        showCombatFloatingText(loc, color + "-" + String.format("%.0f", value));
    }

    // --- Getters ---

    public StatManager getStatManager() { return statManager; }
    public ManaManager getManaManager() { return manaManager; }
    public AttributeHandler getAttributeHandler() { return attributeHandler; }
    public CombatHandler getCombatHandler() { return combatHandler; }
    public DataManager getDataManager() { return dataManager; }
    public ItemAttributeManager getItemAttributeManager() { return itemAttributeManager; }
    public ItemManager getItemManager() { return itemManager; }
    public ChatInputHandler getChatInputHandler() { return chatInputHandler; }
    public EffectManager getEffectManager() { return effectManager; }
    public SkillManager getSkillManager() { return skillManager; }
    public ProjectileHandler getProjectileHandler() { return projectileHandler; }
    public ElementManager getElementManager() { return elementManager; }
    public FloatingTextManager getFloatingTextManager() { return floatingTextManager; }
}