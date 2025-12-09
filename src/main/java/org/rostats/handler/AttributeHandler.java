package org.rostats.handler;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

public class AttributeHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    public AttributeHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerStats(player);
    }

    public void updatePlayerStats(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        if (data == null) return;

        // Update Max Health based on VIT
        int baseStat = data.getStat("VIT");
        int pendingStat = data.getPendingStat("VIT");
        int gearBonus = data.getVITBonusGear();
        int effectBonus = (int) data.getEffectBonus("VIT");
        int totalVIT = baseStat + pendingStat + gearBonus + effectBonus;

        double maxHP = 100 + (totalVIT * 10);
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHP);

        // Heal to max if current HP exceeds new max
        if (player.getHealth() > maxHP) {
            player.setHealth(maxHP);
        }
    }
}