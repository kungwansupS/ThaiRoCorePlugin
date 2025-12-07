package org.rostats.handler;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.rostats.ROStatsPlugin;
import org.rostats.data.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaManager implements Listener {

    private final ROStatsPlugin plugin;
    private final Map<UUID, BossBar> playerSpBars = new HashMap<>(); // Renamed SP bar map
    private final Map<UUID, BossBar> playerBaseExpBars = new HashMap<>(); // NEW Base EXP Bar Map
    private final Map<UUID, BossBar> playerJobExpBars = new HashMap<>();  // NEW Job EXP Bar Map

    public ManaManager(ROStatsPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::regenTask, 40L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        createBar(player); // SP (calls updateBar internally)
        createBaseExpBar(player); // Base EXP (removed internal update)
        createJobExpBar(player);  // Job EXP (removed internal update)
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeBar(player);
        removeBaseExpBar(player);
        removeJobExpBar(player);
    }

    // --- SP Bar Logic (Original) ---

    public void createBar(Player player) {
        BossBar bar = Bukkit.createBossBar("SP", BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(player);
        playerSpBars.put(player.getUniqueId(), bar);
        updateBar(player);
    }

    public void removeBar(Player player) {
        BossBar bar = playerSpBars.remove(player.getUniqueId());
        if (bar != null) bar.removePlayer(player);
    }

    public void updateBar(Player player) {
        BossBar bar = playerSpBars.get(player.getUniqueId());
        if (bar == null) return;

        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        double current = data.getCurrentSP();
        double max = data.getMaxSP();
        double progress = current / max;
        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;

        bar.setProgress(progress);
        bar.setTitle("§bSP: " + String.format("%.0f", current) + " / " + String.format("%.0f", max));
    }

    // --- NEW Base EXP Bar Logic ---

    public void createBaseExpBar(Player player) {
        // Use different color (e.g., GREEN)
        BossBar bar = Bukkit.createBossBar("Base EXP", BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(player);
        playerBaseExpBars.put(player.getUniqueId(), bar);
        // updateBaseExpBar(player); // **ลบการเรียกใช้งานนี้ออก**
    }

    public void removeBaseExpBar(Player player) {
        BossBar bar = playerBaseExpBars.remove(player.getUniqueId());
        if (bar != null) bar.removePlayer(player);
    }

    public void updateBaseExpBar(Player player) {
        BossBar bar = playerBaseExpBars.get(player.getUniqueId());
        if (bar == null) return;

        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        long current = data.getBaseExp();
        long max = data.getBaseExpReq();
        int level = data.getBaseLevel();

        String maxStr = String.valueOf(max);
        double progress;

        // Handle Max Level Cap
        if (level >= data.getMaxBaseLevel()) {
            maxStr = "MAX";
            // If already at max level, show full progress, even if current EXP overflows.
            // Using maxExpReq as the bar max allows overflowed current EXP to show 1.0 progress
            progress = (max > 0) ? (double) current / max : 0.0;
        } else {
            progress = (max > 0) ? (double) current / max : 0.0;
        }

        // Clamp progress between 0.0 and 1.0
        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;

        // Title: Base Lv 100 EXP: 12345/MAX
        bar.setProgress(progress);
        bar.setTitle("§aBase Lv " + level + ": §f" + current + " / " + maxStr);
    }

    // --- NEW Job EXP Bar Logic ---

    public void createJobExpBar(Player player) {
        // Use different color (e.g., YELLOW)
        BossBar bar = Bukkit.createBossBar("Job EXP", BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        playerJobExpBars.put(player.getUniqueId(), bar);
        // updateJobExpBar(player); // **ลบการเรียกใช้งานนี้ออก**
    }

    public void removeJobExpBar(Player player) {
        BossBar bar = playerJobExpBars.remove(player.getUniqueId());
        if (bar != null) bar.removePlayer(player);
    }

    public void updateJobExpBar(Player player) {
        BossBar bar = playerJobExpBars.get(player.getUniqueId());
        if (bar == null) return;

        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        long current = data.getJobExp();
        long max = data.getJobExpReq();
        int level = data.getJobLevel();

        String maxStr = String.valueOf(max);
        double progress;

        // Handle Max Level Cap
        if (level >= data.getMaxJobLevel()) {
            maxStr = "MAX";
            progress = (max > 0) ? (double) current / max : 0.0;
        } else {
            progress = (max > 0) ? (double) current / max : 0.0;
        }

        // Clamp progress between 0.0 and 1.0
        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;


        // Title: Job Lv 10 EXP: 12345/MAX
        bar.setProgress(progress);
        bar.setTitle("§eJob Lv " + level + ": §f" + current + " / " + maxStr);
    }

    // --- Regen Task ---

    private void regenTask() {
        for (UUID uuid : playerSpBars.keySet()) { // Iterate over SP keys
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getStatManager().getData(uuid).regenSP();
                updateBar(player);
                // EXP bars only update when EXP is gained via addBaseExp/addJobExp
            }
        }
    }
}