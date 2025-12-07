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
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaManager implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, BossBar> playerSpBars = new HashMap<>();
    private final Map<UUID, BossBar> playerBaseExpBars = new HashMap<>();
    private final Map<UUID, BossBar> playerJobExpBars = new HashMap<>();

    public ManaManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        // Fix: Use lambda () -> regenTask() to avoid argument mismatch error
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> regenTask(), 40L, 40L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        createBar(player);
        createBaseExpBar(player);
        createJobExpBar(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        removeBar(player);
        removeBaseExpBar(player);
        removeJobExpBar(player);
    }

    // --- SP Bar Logic ---

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
        double progress = (max > 0) ? current / max : 0;

        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;

        bar.setProgress(progress);
        bar.setTitle("§bSP: " + String.format("%.0f", current) + " / " + String.format("%.0f", max));
    }

    // --- Base EXP Bar Logic ---

    public void createBaseExpBar(Player player) {
        BossBar bar = Bukkit.createBossBar("Base EXP", BarColor.GREEN, BarStyle.SOLID);
        bar.addPlayer(player);
        playerBaseExpBars.put(player.getUniqueId(), bar);
        updateBaseExpBar(player);
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

        if (level >= data.getMaxBaseLevel()) {
            maxStr = "MAX";
            progress = 1.0;
        } else {
            progress = (max > 0) ? (double) current / max : 0.0;
        }

        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;

        bar.setProgress(progress);
        bar.setTitle("§aBase Lv " + level + ": §f" + current + " / " + maxStr);
    }

    // --- Job EXP Bar Logic ---

    public void createJobExpBar(Player player) {
        BossBar bar = Bukkit.createBossBar("Job EXP", BarColor.YELLOW, BarStyle.SOLID);
        bar.addPlayer(player);
        playerJobExpBars.put(player.getUniqueId(), bar);
        updateJobExpBar(player);
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

        if (level >= data.getMaxJobLevel()) {
            maxStr = "MAX";
            progress = 1.0;
        } else {
            progress = (max > 0) ? (double) current / max : 0.0;
        }

        if (progress < 0.0) progress = 0.0;
        if (progress > 1.0) progress = 1.0;

        bar.setProgress(progress);
        bar.setTitle("§eJob Lv " + level + ": §f" + current + " / " + maxStr);
    }

    // --- Regen Task ---

    private void regenTask() {
        for (UUID uuid : playerSpBars.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                plugin.getStatManager().getData(uuid).regenSP();
                updateBar(player);
            }
        }
    }
}