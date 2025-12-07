package org.rostats;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.rostats.command.AdminCommand;
import org.rostats.command.PlayerCommand;
// NEW IMPORT
import org.rostats.command.SkillCommand;
import org.rostats.data.DataManager;
import org.rostats.data.StatManager;
import org.rostats.gui.GUIListener;
import org.rostats.handler.AttributeHandler;
import org.rostats.handler.CombatHandler;
import org.rostats.handler.ManaManager;
import org.rostats.hook.PAPIHook;
import org.rostats.input.ChatInputHandler;
import org.rostats.itemeditor.ItemAttributeManager;
import org.rostats.itemeditor.ItemEditorCommand;
import org.rostats.itemeditor.ItemManager;
import org.rostats.engine.effect.EffectManager;
import org.rostats.engine.skill.SkillManager;

import java.util.UUID;

public class ThaiRoCorePlugin extends JavaPlugin implements Listener {

    private StatManager statManager;
    private AttributeHandler attributeHandler;
    private CombatHandler combatHandler;
    private ManaManager manaManager;
    private DataManager dataManager;

    private ItemAttributeManager itemAttributeManager;
    private ItemManager itemManager;
    private ChatInputHandler chatInputHandler;

    private EffectManager effectManager;
    private SkillManager skillManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 1. Initialize Core Managers
        this.statManager = new StatManager(this);
        this.dataManager = new DataManager(this);
        this.manaManager = new ManaManager(this);
        this.attributeHandler = new AttributeHandler(this);
        this.combatHandler = new CombatHandler(this);

        // 2. Initialize Engine Managers
        this.effectManager = new EffectManager(this);
        this.skillManager = new SkillManager(this);

        // 3. Initialize Item Editor Managers
        this.itemAttributeManager = new ItemAttributeManager(this);
        this.itemManager = new ItemManager(this);
        this.chatInputHandler = new ChatInputHandler(this);

        // 4. Register Events
        getServer().getPluginManager().registerEvents(attributeHandler, this);
        getServer().getPluginManager().registerEvents(combatHandler, this);
        getServer().getPluginManager().registerEvents(manaManager, this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getPluginManager().registerEvents(new org.rostats.itemeditor.GUIListener(this), this);
        getServer().getPluginManager().registerEvents(chatInputHandler, this);

        // 5. Register Commands
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

        // NEW: Register Skill Command
        PluginCommand skillCmd = getCommand("roskill");
        if (skillCmd != null) {
            skillCmd.setExecutor(new SkillCommand(this));
        }

        // 6. PAPI Hook
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIHook(this).register();
        }

        // 7. Tasks
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                dataManager.savePlayerData(player);
            }
            getLogger().info("💾 Auto-Saved all player data.");
        }, 6000L, 6000L);

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
                dataManager.savePlayerData(player);
                if (manaManager != null) manaManager.removeBar(player);
            }
        }
        getLogger().info("❌ ThaiRoCorePlugin Disabled");
    }

    public void reload() {
        reloadConfig();
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
        dataManager.savePlayerData(event.getPlayer());
    }

    // --- Floating Text Helpers (Keep existing code) ---
    public void showFloatingText(UUID playerUUID, String text, double verticalOffset) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) return;
        Location startLoc = player.getLocation().add(0, 2.0 + verticalOffset, 0);
        showAnimatedText(startLoc, text);
    }
    public void showFloatingText(UUID playerUUID, String text) { showFloatingText(playerUUID, text, 0.25); }
    public void showCombatFloatingText(Location loc, String text) { showAnimatedText(loc.add(0, 1.5, 0), text); }
    private void showAnimatedText(Location startLoc, String text) {
        getServer().getScheduler().runTask(this, () -> {
            final ArmorStand stand = startLoc.getWorld().spawn(startLoc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setCustomNameVisible(true);
            stand.customName(Component.text(text));
            stand.setSmall(true);
            BukkitTask[] task = new BukkitTask[1];
            task[0] = getServer().getScheduler().runTaskTimer(this, new Runnable() {
                private int ticks = 0;
                private final Location currentLocation = stand.getLocation();
                private final double step = 0.5 / 20.0;
                @Override
                public void run() {
                    if (stand.isDead() || ticks >= 20) {
                        stand.remove();
                        if (task[0] != null) task[0].cancel();
                        return;
                    }
                    currentLocation.add(0, step, 0);
                    stand.teleport(currentLocation);
                    ticks++;
                }
            }, 0L, 1L);
        });
    }
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
}