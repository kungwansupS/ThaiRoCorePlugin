package org.rostats.handler;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
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
import org.rostats.ROStatsPlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

public class CombatHandler implements Listener {

    private final ROStatsPlugin plugin;
    private final Random random = new Random();
    private final double SKILL_POWER = 1.0; // Default skill power for basic attacks
    private final double BASE_CRIT = 1.5; // Keeping old BASE_CRIT as constant, but unused in new formula
    private final int K_DEFENSE = 400; // Constant K for defense formula (Section G)
    private final double JOB_EXP_RATIO; // New field

    public CombatHandler(ROStatsPlugin plugin) {
        this.plugin = plugin;
        this.JOB_EXP_RATIO = plugin.getConfig().getDouble("exp-formula.job-exp-ratio", 0.75); // Initialize ratio
    }

    // --- EXP Gain Logic (EntityDeathEvent) ---
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;
        LivingEntity victim = event.getEntity();
        PlayerData data = plugin.getStatManager().getData(killer.getUniqueId());

        // 1. Read vanilla EXP (RawBaseEXP)
        int rawBaseExp = event.getDroppedExp();

        // 2. Anti-Exploit Rule: Disable XP Orb Drop
        event.setDroppedExp(0);

        // 3. Anti-Exploit Rule: Skip if victim is player or no exp is dropped
        if (victim instanceof Player) return;
        if (rawBaseExp <= 0) return;

        // 4. Calculate RawJobEXP
        long rawJobExp = (long) Math.floor(rawBaseExp * JOB_EXP_RATIO);
        long finalBaseExp = rawBaseExp;
        long finalJobExp = rawJobExp;

        // 5. Apply Level Gap Penalty (Skipped, requires external monster level data)
        double penaltyFactor = 1.0;

        // 6. Apply EXP Bonuses (Skipped complex party/damage bonus logic for now)
        double totalBonusMultiplier = 1.0;

        finalBaseExp = (long) Math.floor(finalBaseExp * penaltyFactor * totalBonusMultiplier);
        finalJobExp = (long) Math.floor(finalJobExp * penaltyFactor * totalBonusMultiplier);


        // 7. Add EXP directly to player fields (PlayerData handles level-up loop and floating text)
        if (finalBaseExp > 0) {
            data.addBaseExp(finalBaseExp, killer.getUniqueId());
        }
        if (finalJobExp > 0) {
            data.addJobExp(finalJobExp, killer.getUniqueId());
        }
    }
    // --- END EXP LOGIC ---

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        double weaponDamage = event.getDamage();
        Player attackerPlayer = null;
        LivingEntity defenderEntity = victim;
        boolean isRanged = false;
        boolean isMagic = (event.getCause() == EntityDamageEvent.DamageCause.MAGIC);
        Entity damagerEntity = event.getDamager();

        // 0. Identify Attacker/Defender
        if (damagerEntity instanceof Player p) {
            attackerPlayer = p;
        } else if (damagerEntity instanceof Projectile proj) {
            isRanged = true;
            if (proj.getShooter() instanceof Player p) attackerPlayer = p;
        }

        // If Attacker is Mob: Use simple Flee/Defense check and exit
        if (attackerPlayer == null) {
            if (defenderEntity instanceof Player victimPlayer) {
                int victimFlee = plugin.getStatManager().getFlee(victimPlayer);
                int mobHit = 50; // Placeholder Mob HIT
                if (random.nextDouble() * 100 > (80 + mobHit - victimFlee)) {
                    event.setCancelled(true);
                    plugin.showCombatFloatingText(victim.getLocation(), "§7MISS"); // ใช้ centralized FCT
                    return;
                }
            }
            return;
        }

        // Attacker is Player (A)
        PlayerData A = plugin.getStatManager().getData(attackerPlayer.getUniqueId());
        StatManager stats = plugin.getStatManager();

        // Defender (D)
        PlayerData D = (defenderEntity instanceof Player) ? plugin.getStatManager().getData(defenderEntity.getUniqueId()) : null;


        // ==========================================
        // COMBAT PIPELINE (Section 8 - Re-implemented)
        // ==========================================

        // 1. HIT check (Hit vs Flee)
        if (!isMagic) {
            int attackerHit = stats.getHit(attackerPlayer);
            int defenderFlee = (defenderEntity instanceof Player) ? stats.getFlee((Player) defenderEntity) : 0; // Monster Flee is 0

            // Formula: ChanceToHit = clamp(Hit / (Hit + TargetFLEE), 5%, 95%)
            double hitRate = (double) attackerHit / (attackerHit + defenderFlee);
            hitRate = Math.max(0.05, Math.min(0.95, hitRate));

            if (random.nextDouble() > hitRate) {
                event.setCancelled(true);
                plugin.showCombatFloatingText(defenderEntity.getLocation(), "§7MISS"); // ใช้ centralized FCT
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

        // 3. Compute BaseDamage (ATK * SkillPower) -> Result is preDefDamage
        double preDefDamage = damage * SKILL_POWER;

        // 4. Apply Damage Bonus % (Section 6)
        if (isMagic) {
            preDefDamage *= (1 + A.getMDmgBonusPercent() / 100.0);
        } else {
            preDefDamage *= (1 + A.getPDmgBonusPercent() / 100.0);
        }

        // 5. Apply Melee/Range Bonus% (Section 6)
        if (!isMagic) {
            if (isRanged) {
                preDefDamage *= (1 + A.getRangePDmgPercent() / 100.0);
            } else { // Melee
                preDefDamage *= (1 + A.getMeleePDmgPercent() / 100.0);
            }
        }

        // 6. Add flat bonuses (P/M_DMG_FLAT) (Section 6)
        if (isMagic) {
            preDefDamage += A.getMDmgBonusFlat();
        } else {
            preDefDamage += A.getPDmgBonusFlat();
        }

        // 7. Apply Defense Pipeline (Section G, Soft DEF Reduction)
        double damageAfterDEF = preDefDamage;
        if (D != null && defenderEntity instanceof Player defenderPlayer) {
            damageAfterDEF = isMagic ?
                    applyMagicDEF(A, D, preDefDamage, defenderPlayer) :
                    applyPhysicalDEF(A, D, preDefDamage, defenderPlayer);
        }

        double damageTaken = damageAfterDEF;

        // 8. Apply Damage Reduction% (P/M, Melee/Range) (Section 6)
        if (D != null && defenderEntity instanceof Player defenderPlayer) {
            // General P/M Reduction
            if (isMagic) {
                damageTaken *= (1 - D.getMDmgReductionPercent() / 100.0);
            } else {
                damageTaken *= (1 - D.getPDmgReductionPercent() / 100.0);
            }

            // Melee/Range Reduction
            if (!isMagic) {
                if (isRanged) {
                    damageTaken *= (1 - D.getRangePDReductionPercent() / 100.0);
                } else { // Melee
                    damageTaken *= (1 - D.getMeleePDReductionPercent() / 100.0);
                }
            }
        }

        // 9. Apply Critical if triggered (Section 1.7) - Must be before RAW Multiplier
        boolean isCritical = false;
        if (!isMagic) {
            double effectiveCritChance = calculateCritChance(A, D);
            if (random.nextDouble() < effectiveCritChance) {
                isCritical = true;
                double critMultiplier = calculateCritMultiplier(A, D);
                damageTaken *= critMultiplier;
            }
        }

        // 10. Apply RAW Difference Multiplier (Section 7/8 Step 7) - NEW
        if (D != null) { // PVP
            // PVP_RAW = PVP_BONUS - PVP_REDUCE. (Using Percent getters as flat RAW values as a proxy)
            double attackerPvpRaw = A.getPvpDmgBonusPercent() - A.getPvpDmgReductionPercent();
            double defenderPvpRaw = D.getPvpDmgBonusPercent() - D.getPvpDmgReductionPercent();
            double pvpDiff = attackerPvpRaw - defenderPvpRaw;
            damageTaken *= getTierMultiplier(pvpDiff);
        } else { // PVE (Defender is Monster/Entity)
            // PVE_RAW = PVE_BONUS - PVE_REDUCE. (Monster RAW is assumed to be 0)
            double attackerPveRaw = A.getPveDmgBonusPercent() - A.getPveDmgReductionPercent();
            double monsterPveRaw = 0.0;
            double pveDiff = attackerPveRaw - monsterPveRaw;
            damageTaken *= getTierMultiplier(pveDiff); // <-- CORRECTED: Was incorrectly using pvpDiff
        }

        // 11. Apply Final/True DMG (Keeping original Final DMG steps from existing code)
        if (D != null) {
            // Final DMG%
            damageTaken *= (1 + A.getFinalDmgPercent() / 100.0);

            // Final P/M DMG%
            if (isMagic) {
                damageTaken *= (1 + A.getFinalMDmgPercent() / 100.0);
            } else {
                damageTaken *= (1 + A.getFinalPDmgPercent() / 100.0);
            }

            // Final DMG RES%
            damageTaken *= (1 - D.getFinalDmgResPercent() / 100.0);
        }

        // 12. Apply Shield absorption (Section J)
        if (D != null && D.getShieldValueFlat() > 0) {
            double absorb = Math.min(D.getShieldValueFlat(), damageTaken);
            damageTaken -= absorb;
            // State change for D.ShieldValue must be handled with care
        }

        // Final Output
        double finalDamage = Math.max(1.0, damageTaken);
        event.setDamage(finalDamage);

        // 13. Display Damage FCT
        if (isCritical && attackerPlayer != null) {
            // CRITICAL FCT
            showCritEffects(attackerPlayer, defenderEntity, finalDamage);
        } else if (finalDamage > 0) {
            // NORMAL DAMAGE FCT (7)
            plugin.showDamageFCT(defenderEntity.getLocation(), finalDamage);
        }

        // True Damage FCT (8) - Assume True Damage is a separate, additional amount
        if (A.getTrueDamageFlat() > 0.0) {
            double trueDmg = A.getTrueDamageFlat();
            // True damage FCT
            plugin.showTrueDamageFCT(defenderEntity.getLocation().add(0, 0.5, 0), trueDmg); // Show True DMG slightly higher than Normal DMG
            // Note: True damage must also be applied to the event/victim's health in actual combat loop.
        }
    }

    // ==========================================
    // COMBAT HELPER METHODS (Ensure these are present inside the class)
    // ==========================================

    private double applyPhysicalDEF(PlayerData A, PlayerData D, double damage, Player defenderPlayer) {
        StatManager stats = plugin.getStatManager();
        double softPDef = stats.getSoftDef(defenderPlayer);

        // EffectivePDEF = max(0, (SoftPDEF - IgnorePDEF_flat) × (1 - IgnorePDEF% / 100))
        double def1 = Math.max(0, softPDef - A.getIgnorePDefFlat());
        double effectiveDef = def1 * (1 - A.getIgnorePDefPercent() / 100.0);

        // Convert DEF -> Reduction (RO Formula: DEF / (DEF + K))
        double defReduction = effectiveDef / (effectiveDef + K_DEFENSE);

        // Apply
        return damage * (1 - defReduction);
    }

    private double applyMagicDEF(PlayerData A, PlayerData D, double damage, Player defenderPlayer) {
        StatManager stats = plugin.getStatManager();
        double softMDef = stats.getSoftMDef(defenderPlayer);

        // EffectiveMDEF = max(0, (SoftMDef - IgnoreMDEF_flat) × (1 - IgnoreMDEF% / 100))
        double def1 = Math.max(0, softMDef - A.getIgnoreMDefFlat());
        double effectiveMDef = def1 * (1 - A.getIgnoreMDefPercent() / 100.0);

        // Convert DEF -> Reduction (RO Formula: DEF / (DEF + K))
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

    private double calculateCritChance(PlayerData A, PlayerData D) {
        // CriticalChance = max(0, CRIT - Target.CRIT_RES)
        int luk = A.getStat("LUK"); // Get LUK stat from PlayerData A
        double rawCritValue = luk * 0.3; // Raw CRIT value

        // TargetCritRes = LUK × 0.2 + BonusCritRes
        double totalCritResValue = (D != null) ? (D.getStat("LUK") * 0.2) + D.getCritRes() : 0.0;

        // Final Crit Chance (as a probability) = max(0, (RawCritValue - TotalCritResValue) / 100.0)
        double effectiveCritValue = Math.max(0, rawCritValue - totalCritResValue);

        return effectiveCritValue / 100.0;
    }

    private double calculateCritMultiplier(PlayerData A, PlayerData D) {
        // CritMultiplier = 1 + (CritDMG% / 100)
        double bonusCrit = A.getCritDmgPercent() / 100.0;
        return 1.0 + bonusCrit;
    }

    // MODIFIED: Helper for visual effects (Uses centralized FCT method)
    private void showCritEffects(Player attacker, LivingEntity victim, double finalDamage) {
        // CRITICAL FCT
        plugin.showCombatFloatingText(victim.getLocation().add(0, 0.5, 0), "§c§lCRITICAL " + String.format("%.0f", finalDamage)); // Show Critical slightly higher

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20);
    }
}