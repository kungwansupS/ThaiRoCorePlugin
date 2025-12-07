package org.rostats.handler;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.rostats.ROStatsPlugin;
import org.rostats.data.PlayerData;

import java.util.HashMap;
import java.util.Map;

public class DeathHandler implements Listener {

    private final ROStatsPlugin plugin;
    private static final Map<EntityType, Integer> VANILLA_EXP_MAP = new HashMap<>();

    static {
        // Vanilla EXP values based on the user's table (These values are the base to be divided by 10)
        VANILLA_EXP_MAP.put(EntityType.ZOMBIE, 5);
        VANILLA_EXP_MAP.put(EntityType.SKELETON, 5);
        VANILLA_EXP_MAP.put(EntityType.SPIDER, 5);
        VANILLA_EXP_MAP.put(EntityType.CREEPER, 5);
        VANILLA_EXP_MAP.put(EntityType.ENDERMAN, 5);
        VANILLA_EXP_MAP.put(EntityType.WITCH, 5);
        VANILLA_EXP_MAP.put(EntityType.GUARDIAN, 10);
        VANILLA_EXP_MAP.put(EntityType.BLAZE, 10);
        VANILLA_EXP_MAP.put(EntityType.GHAST, 5);
        VANILLA_EXP_MAP.put(EntityType.RAVAGER, 20);

        // Slime and Magma Cube base value (will be adjusted by size below)
        VANILLA_EXP_MAP.put(EntityType.SLIME, 4);
        VANILLA_EXP_MAP.put(EntityType.MAGMA_CUBE, 4);

        // Bosses
        VANILLA_EXP_MAP.put(EntityType.WITHER, 50);
        VANILLA_EXP_MAP.put(EntityType.ENDER_DRAGON, 12000);
    }

    public DeathHandler(ROStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();

        // 1. Check if the killer is a Player and if the mob has a defined EXP
        if (killer == null) {
            return;
        }

        // Reset vanilla EXP drop to 0 to prevent double leveling
        event.setDroppedExp(0);

        int vanillaExp = VANILLA_EXP_MAP.getOrDefault(victim.getType(), 0);

        // Adjust EXP for Slime/Magma Cube based on size
        if (victim instanceof Slime slime) {
            int size = slime.getSize();
            if (victim.getType() == EntityType.SLIME || victim.getType() == EntityType.MAGMA_CUBE) {
                // Slime (ใหญ่/กลาง/เล็ก) 4/2/1. Size 4 is large, Size 2 is medium, Size 1 is small
                if (size == 1) vanillaExp = 1;
                else if (size == 2) vanillaExp = 2;
                else if (size >= 4) vanillaExp = 4;
                else vanillaExp = 0; // Ignore tiny or unexpected sizes
            }
        }

        if (vanillaExp == 0) {
            return; // Not a configured monster
        }

        // 2. Calculate EXP (Vanilla EXP / 10, minimum 1)
        long calculatedBaseExp = Math.max(1, (long) Math.floor(vanillaExp / 10.0));
        long calculatedJobExp = calculatedBaseExp; // Assume Job EXP is the same as Base EXP for now

        PlayerData playerData = plugin.getStatManager().getData(killer.getUniqueId());

        // 3. Show FCT (Base EXP and Job EXP)
        // Base EXP (Blue): §b{value} Base
        plugin.showFloatingText(killer.getUniqueId(), "§b" + calculatedBaseExp + " Base");
        // Job EXP (Yellow): §e{value} Job
        plugin.showFloatingText(killer.getUniqueId(), "§e" + calculatedJobExp + " Job");

        // 4. Add Base EXP to PlayerData (addBaseExp handles level up logic)
        playerData.addBaseExp(calculatedBaseExp, killer.getUniqueId());
    }
}