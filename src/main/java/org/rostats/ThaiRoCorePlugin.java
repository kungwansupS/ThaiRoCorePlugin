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
import org.rostats.data.DataManager;
import org.rostats.data.StatManager;
import org.rostats.gui.GUIListener;
import org.rostats.handler.AttributeHandler;
import org.rostats.handler.CombatHandler;
import org.rostats.handler.ManaManager;
import org.rostats.hook.PAPIHook;
// NEW IMPORTS for Item Editor
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
    // NEW FIELDS for Item Editor
    private ItemAttributeManager itemAttributeManager;
    private ItemManager itemManager;
    private ChatInputHandler chatInputHandler;

    @Override
    public void onEnable() {
        // 0. Load Config
        saveDefaultConfig();

        // 1. Initialize Managers
        this.statManager = new StatManager(this);
        this.dataManager = new DataManager(this);
        this.manaManager = new ManaManager(this);
        this.attributeHandler = new AttributeHandler(this);
        this.combatHandler = new CombatHandler(this);

        // NEW: Initialize ItemAttributeManager & Utils
        this.itemAttributeManager = new ItemAttributeManager(this);
        this.itemManager = new ItemManager(this);
        this.chatInputHandler = new ChatInputHandler(this);

        // 2. Register Events
        getServer().getPluginManager().registerEvents(attributeHandler, this);
        getServer().getPluginManager().registerEvents(combatHandler, this);
        getServer().getPluginManager().registerEvents(manaManager, this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);

        // NEW: Register Item Editor Listener & Chat Input
        getServer().getPluginManager().registerEvents(new org.rostats.itemeditor.GUIListener(this), this);
        getServer().getPluginManager().registerEvents(chatInputHandler, this);

        // 3. Register Commands
        PluginCommand statusCmd = getCommand("status");
        if (statusCmd != null) statusCmd.setExecutor(new PlayerCommand(this));

        PluginCommand adminCmd = getCommand("roadmin");
        if (adminCmd != null) {
            AdminCommand adminExecutor = new AdminCommand(this);
            adminCmd.setExecutor(adminExecutor);
            adminCmd.setTabCompleter(adminExecutor);
        }

        // NEW: Register Item Editor Command
        PluginCommand itemEditCmd = getCommand("roitemedit");
        if (itemEditCmd != null) {
            itemEditCmd.setExecutor(new ItemEditorCommand(this));
        }

        // 4. PAPI Hook
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PAPIHook(this).register();
        }

        // 5. Auto-Save Task
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                dataManager.savePlayerData(player);
            }
            getLogger().info("💾 Auto-Saved all player data.");
        }, 6000L, 6000L);

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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        dataManager.loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        dataManager.savePlayerData(event.getPlayer());
    }

    // MODIFIED: Helper method for Floating Text
    public void showFloatingText(UUID playerUUID, String text, double verticalOffset) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) return;

        Location startLoc = player.getLocation().add(0, 2.0 + verticalOffset, 0);
        showAnimatedText(startLoc, text);
    }

    public void showFloatingText(UUID playerUUID, String text) {
        showFloatingText(playerUUID, text, 0.25);
    }

    public void showCombatFloatingText(Location loc, String text) {
        showAnimatedText(loc.add(0, 1.5, 0), text);
    }

    // NEW: Core Animation Logic
    private void showAnimatedText(Location startLoc, String text) {
        getServer().getScheduler().runTask(this, () -> {
            // *** FIX: Declare as FINAL to be safely used in the inner anonymous class ***
            final ArmorStand stand = startLoc.getWorld().spawn(startLoc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setCustomNameVisible(true);
            stand.customName(Component.text(text));
            stand.setSmall(true);

            // Animation Task
            BukkitTask[] task = new BukkitTask[1];
            task[0] = getServer().getScheduler().runTaskTimer(this, new Runnable() {
                private int ticks = 0;
                private final Location currentLocation = stand.getLocation();
                private final double distance = 0.5;
                private final double step = distance / 20.0;

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

    public void showDamageFCT(Location loc, double damage) {
        showCombatFloatingText(loc, "§f" + String.format("%.0f", damage));
    }

    public void showTrueDamageFCT(Location loc, double damage) {
        showCombatFloatingText(loc, "§6" + String.format("%.0f", damage));
    }

    public void showHealHPFCT(Location loc, double value) {
        showCombatFloatingText(loc, "§a+" + String.format("%.0f", value) + " HP");
    }

    public void showHealSPFCT(Location loc, double value) {
        showCombatFloatingText(loc, "§b+" + String.format("%.0f", value) + " SP");
    }

    public void showStatusDamageFCT(Location loc, String status, double value) {
        String color = switch (status.toLowerCase()) {
            case "poison" -> "§2";
            case "burn" -> "§c";
            case "bleed" -> "§4";
            default -> "§7";
        };
        showCombatFloatingText(loc, color + "-" + String.format("%.0f", value));
    }

    public StatManager getStatManager() { return statManager; }
    public ManaManager getManaManager() { return manaManager; }
    public AttributeHandler getAttributeHandler() { return attributeHandler; }
    public DataManager getDataManager() { return dataManager; }
    public ItemAttributeManager getItemAttributeManager() { return itemAttributeManager; }
    public ItemManager getItemManager() { return itemManager; }
    public ChatInputHandler getChatInputHandler() { return chatInputHandler; }
}