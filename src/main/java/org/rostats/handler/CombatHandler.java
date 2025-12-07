package org.rostats.handler;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.ItemAttribute;
import org.rostats.itemeditor.ItemSkillBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class CombatHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Random random = new Random();
    private final double SKILL_POWER = 1.0;
    private final int K_DEFENSE = 400;
    private double jobExpRatio;

    public CombatHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        loadValues();
    }

    public void loadValues() {
        this.jobExpRatio = plugin.getConfig().getDouble("exp-formula.job-exp-ratio", 0.75);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;
        LivingEntity victim = event.getEntity();
        PlayerData data = plugin.getStatManager().getData(killer.getUniqueId());

        // Check for ON_KILL triggers on killer's equipment
        checkTriggers(killer, victim, TriggerType.ON_KILL);

        int rawBaseExp = event.getDroppedExp();
        event.setDroppedExp(0);

        if (victim instanceof Player) return;
        if (rawBaseExp <= 0) return;

        long rawJobExp = (long) Math.floor(rawBaseExp * jobExpRatio);
        long finalBaseExp = rawBaseExp;
        long finalJobExp = rawJobExp;

        if (finalBaseExp > 0) data.addBaseExp(finalBaseExp, killer.getUniqueId());
        if (finalJobExp > 0) data.addJobExp(finalJobExp, killer.getUniqueId());
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        double weaponDamage = event.getDamage();
        Player attackerPlayer = null;
        LivingEntity defenderEntity = victim;
        boolean isRanged = false;
        boolean isMagic = (event.getCause() == EntityDamageEvent.DamageCause.MAGIC);
        Entity damagerEntity = event.getDamager();

        if (damagerEntity instanceof Player p) {
            attackerPlayer = p;
        } else if (damagerEntity instanceof Projectile proj) {
            isRanged = true;
            if (proj.getShooter() instanceof Player p) attackerPlayer = p;
        }

        if (attackerPlayer == null) {
            if (defenderEntity instanceof Player victimPlayer) {
                int victimFlee = plugin.getStatManager().getFlee(victimPlayer);
                int mobHit = 50;
                if (random.nextDouble() * 100 > (80 + mobHit - victimFlee)) {
                    event.setCancelled(true);
                    plugin.showCombatFloatingText(victim.getLocation(), "§7MISS");
                    return;
                }
            }
            return;
        }

        // --- NEW: Trigger Checks ---
        // 1. ON_HIT (Attacker)
        checkTriggers(attackerPlayer, defenderEntity, TriggerType.ON_HIT);

        // 2. ON_DEFEND (Defender)
        if (defenderEntity instanceof Player defenderPlayer) {
            checkTriggers(defenderPlayer, attackerPlayer, TriggerType.ON_DEFEND);
        }
        // ---------------------------

        PlayerData A = plugin.getStatManager().getData(attackerPlayer.getUniqueId());
        StatManager stats = plugin.getStatManager();
        PlayerData D = (defenderEntity instanceof Player) ? plugin.getStatManager().getData(defenderEntity.getUniqueId()) : null;

        // 1. HIT check (Hit vs Flee)
        if (!isMagic) {
            int attackerHit = stats.getHit(attackerPlayer);
            int defenderFlee = (defenderEntity instanceof Player) ? stats.getFlee((Player) defenderEntity) : 0;
            double hitRate = (double) attackerHit / (attackerHit + defenderFlee);
            hitRate = Math.max(0.05, Math.min(1.0, hitRate));

            if (random.nextDouble() > hitRate) {
                event.setCancelled(true);
                plugin.showCombatFloatingText(defenderEntity.getLocation(), "§7MISS");
                return;
            }
        }

        // 2. Determine base ATK
        double damage = weaponDamage;
        if (isMagic) {
            damage += stats.getMagicAttack(attackerPlayer);
        } else {
            damage += stats.getPhysicalAttack(attackerPlayer);
        }

        double preDefDamage = damage * SKILL_POWER;

        if (isMagic) {
            preDefDamage *= (1 + A.getMDmgBonusPercent() / 100.0);
        } else {
            preDefDamage *= (1 + A.getPDmgBonusPercent() / 100.0);
        }

        if (!isMagic) {
            if (isRanged) {
                preDefDamage *= (1 + A.getRangePDmgPercent() / 100.0);
            } else {
                preDefDamage *= (1 + A.getMeleePDmgPercent() / 100.0);
            }
        }

        if (isMagic) {
            preDefDamage += A.getMDmgBonusFlat();
        } else {
            preDefDamage += A.getPDmgBonusFlat();
        }

        double damageAfterDEF = preDefDamage;
        if (D != null && defenderEntity instanceof Player defenderPlayer) {
            damageAfterDEF = isMagic ?
                    applyMagicDEF(A, D, preDefDamage, defenderPlayer) :
                    applyPhysicalDEF(A, D, preDefDamage, defenderPlayer);
        }

        double damageTaken = damageAfterDEF;

        if (D != null && defenderEntity instanceof Player defenderPlayer) {
            if (isMagic) {
                damageTaken *= (1 - D.getMDmgReductionPercent() / 100.0);
            } else {
                damageTaken *= (1 - D.getPDmgReductionPercent() / 100.0);
            }

            if (!isMagic) {
                if (isRanged) {
                    damageTaken *= (1 - D.getRangePDReductionPercent() / 100.0);
                } else {
                    damageTaken *= (1 - D.getMeleePDReductionPercent() / 100.0);
                }
            }
        }

        // 9. Apply Critical
        boolean isCritical = false;
        if (!isMagic) {
            double effectiveCritChance = calculateCritChance(attackerPlayer, defenderEntity);
            if (random.nextDouble() < effectiveCritChance) {
                isCritical = true;
                double critMultiplier = calculateCritMultiplier(A, D);
                damageTaken *= critMultiplier;
            }
        }

        if (D != null) {
            double attackerPvpRaw = A.getPvpDmgBonusPercent() - A.getPvpDmgReductionPercent();
            double defenderPvpRaw = D.getPvpDmgBonusPercent() - D.getPvpDmgReductionPercent();
            double pvpDiff = attackerPvpRaw - defenderPvpRaw;
            damageTaken *= getTierMultiplier(pvpDiff);
        } else {
            double attackerPveRaw = A.getPveDmgBonusPercent() - A.getPveDmgReductionPercent();
            double monsterPveRaw = 0.0;
            double pveDiff = attackerPveRaw - monsterPveRaw;
            damageTaken *= getTierMultiplier(pveDiff);
        }

        if (D != null) {
            damageTaken *= (1 + A.getFinalDmgPercent() / 100.0);
            if (isMagic) {
                damageTaken *= (1 + A.getFinalMDmgPercent() / 100.0);
            } else {
                damageTaken *= (1 + A.getFinalPDmgPercent() / 100.0);
            }
            damageTaken *= (1 - D.getFinalDmgResPercent() / 100.0);
        }

        if (D != null && D.getShieldValueFlat() > 0) {
            double absorb = Math.min(D.getShieldValueFlat(), damageTaken);
            damageTaken -= absorb;
        }

        double finalDamage = Math.max(1.0, damageTaken);
        event.setDamage(finalDamage);

        if (isCritical && attackerPlayer != null) {
            showCritEffects(attackerPlayer, defenderEntity, finalDamage);
        } else if (finalDamage > 0) {
            plugin.showDamageFCT(defenderEntity.getLocation(), finalDamage);
        }

        if (A.getTrueDamageFlat() > 0.0) {
            double trueDmg = A.getTrueDamageFlat();
            plugin.showTrueDamageFCT(defenderEntity.getLocation().add(0, 0.5, 0), trueDmg);
        }
    }

    // --- NEW: Check Triggers Logic ---
    private void checkTriggers(Player player, LivingEntity target, TriggerType type) {
        List<ItemStack> items = new ArrayList<>();
        if (player.getEquipment() != null) {
            items.add(player.getEquipment().getItemInMainHand());
            items.add(player.getEquipment().getItemInOffHand());
            items.addAll(Arrays.asList(player.getEquipment().getArmorContents()));
        }

        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
            if (attr == null) continue;

            for (ItemSkillBinding binding : attr.getSkillBindings()) {
                if (binding.getTrigger() == type) {
                    if (random.nextDouble() < binding.getChance()) {
                        // Cast Skill
                        plugin.getSkillManager().castSkill(player, binding.getSkillId(), binding.getLevel(), target);
                    }
                }
            }
        }
    }

    // ==========================================
    // COMBAT HELPER METHODS
    // ==========================================

    private double calculateCritChance(Player attacker, LivingEntity defender) {
        double attackerTotalCrit = plugin.getStatManager().getCritChance(attacker);
        double defenderCritRes = 0.0;
        if (defender instanceof Player defenderPlayer) {
            PlayerData D = plugin.getStatManager().getData(defenderPlayer.getUniqueId());
            int totalLuk = D.getStat("LUK") + D.getPendingStat("LUK") + D.getLUKBonusGear();
            defenderCritRes = (totalLuk * 0.2) + D.getCritRes();
        }
        return Math.max(0, (attackerTotalCrit - defenderCritRes) / 100.0);
    }

    private double applyPhysicalDEF(PlayerData A, PlayerData D, double damage, Player defenderPlayer) {
        StatManager stats = plugin.getStatManager();
        double softPDef = stats.getSoftDef(defenderPlayer);
        double def1 = Math.max(0, softPDef - A.getIgnorePDefFlat());
        double effectiveDef = def1 * (1 - A.getIgnorePDefPercent() / 100.0);
        double defReduction = effectiveDef / (effectiveDef + K_DEFENSE);
        return damage * (1 - defReduction);
    }

    private double applyMagicDEF(PlayerData A, PlayerData D, double damage, Player defenderPlayer) {
        StatManager stats = plugin.getStatManager();
        double softMDef = stats.getSoftMDef(defenderPlayer);
        double def1 = Math.max(0, softMDef - A.getIgnoreMDefFlat());
        double effectiveMDef = def1 * (1 - A.getIgnoreMDefPercent() / 100.0);
        double mDefReduction = effectiveMDef / (effectiveMDef + K_DEFENSE);
        return damage * (1 - mDefReduction);
    }

    private double getTierMultiplier(double diff) {
        if (diff >= 4000) return 1.55;
        if (diff >= 3000) return 1.50;
        if (diff >= 2000) return 1.40;
        if (diff >= 1000) return 1.25;
        if (diff >= 500) return 1.175;
        if (diff <= -4000) return 0.45;
        if (diff <= -3000) return 0.50;
        if (diff <= -2000) return 0.60;
        if (diff <= -1000) return 0.75;
        if (diff <= -500) return 0.825;
        return 1.00;
    }

    private double calculateCritMultiplier(PlayerData A, PlayerData D) {
        double bonusCrit = A.getCritDmgPercent() / 100.0;
        return 1.0 + bonusCrit;
    }

    private void showCritEffects(Player attacker, LivingEntity victim, double finalDamage) {
        plugin.showCombatFloatingText(victim.getLocation().add(0, 0.5, 0), "§c§lCRITICAL " + String.format("%.0f", finalDamage));
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20);
    }
}