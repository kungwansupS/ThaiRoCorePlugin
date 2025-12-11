package org.rostats.handler;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.effect.EffectType;

public class StatusHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    public StatusHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // --- 1. Movement Check (ROOT / STUN) ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getX() == event.getTo().getX() &&
                event.getFrom().getZ() == event.getTo().getZ()) return; // Ignored rotation

        Player player = event.getPlayer();

        // Check STUN
        if (plugin.getEffectManager().hasEffect(player, EffectType.CROWD_CONTROL, "STUN")) {
            event.setCancelled(true);
            return;
        }

        // Check ROOT
        if (plugin.getEffectManager().hasEffect(player, EffectType.CROWD_CONTROL, "ROOT")) {
            // Cancel movement but allow rotation?
            // Simple way: Cancel event
            // Better way: Teleport back to from (can be jittery)
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setCancelled(true);
            }
        }
    }

    // --- 2. Action Check (STUN) ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (plugin.getEffectManager().hasEffect(event.getPlayer(), EffectType.CROWD_CONTROL, "STUN")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou are stunned!");
        }
    }

    // --- 3. Combat Check (STUN - Can't Attack) ---
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity attacker) {
            if (plugin.getEffectManager().hasEffect(attacker, EffectType.CROWD_CONTROL, "STUN")) {
                event.setCancelled(true);
                if (attacker instanceof Player) {
                    attacker.sendMessage("§cYou are stunned!");
                }
            }
        }
    }
}