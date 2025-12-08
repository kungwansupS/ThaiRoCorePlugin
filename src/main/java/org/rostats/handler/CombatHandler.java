package org.rostats.handler;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.ItemAttribute;
import org.rostats.itemeditor.ItemSkillBinding;

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

    // --- Active Skill Trigger (Clicks) ---
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // (ส่วนนี้เหมือนเดิม ไม่ต้องแก้)
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
        if (attr == null) return;

        TriggerType type = null;
        Action action = event.getAction();
        boolean isSneaking = player.isSneaking();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            type = isSneaking ? TriggerType.SHIFT_RIGHT_CLICK : TriggerType.RIGHT_CLICK;
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            type = isSneaking ? TriggerType.SHIFT_LEFT_CLICK : TriggerType.LEFT_CLICK;
        }

        if (type != null) {
            if (type == TriggerType.RIGHT_CLICK) {
                checkAndCast(player, attr, TriggerType.CAST, null);
            }
            checkAndCast(player, attr, type, null);
        }
    }

    private void checkAndCast(Player player, ItemAttribute attr, TriggerType type, LivingEntity target) {
        for (ItemSkillBinding binding : attr.getSkillBindings()) {
            if (binding.getTrigger() == type) {
                if (random.nextDouble() < binding.getChance()) {
                    plugin.getSkillManager().castSkill(player, binding.getSkillId(), binding.getLevel(), target);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // (ส่วนนี้เหมือนเดิม)
        if (!(event.getEntity().getKiller() instanceof Player killer)) return;
        LivingEntity victim = event.getEntity();
        PlayerData data = plugin.getStatManager().getData(killer.getUniqueId());

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

        double vanillaDamage = event.getDamage();
        Player attackerPlayer = null;
        LivingEntity defenderEntity = victim;
        boolean isRanged = false;
        boolean isMagic = (event.getCause() == EntityDamageEvent.DamageCause.MAGIC);
        Entity damagerEntity = event.getDamager();

        // ระบุตัวคนโจมตี
        if (damagerEntity instanceof Player p) {
            attackerPlayer = p;
        } else if (damagerEntity instanceof Projectile proj) {
            isRanged = true;
            if (proj.getShooter() instanceof Player p) attackerPlayer = p;
        }

        StatManager stats = plugin.getStatManager();
        // A = Attacker Data (null ถ้าเป็นมอนสเตอร์), D = Defender Data (null ถ้าเป้าหมายเป็นมอนสเตอร์)
        PlayerData A = (attackerPlayer != null) ? stats.getData(attackerPlayer.getUniqueId()) : null;
        PlayerData D = (defenderEntity instanceof Player) ? stats.getData(defenderEntity.getUniqueId()) : null;

        // --- 1. HIT / FLEE Calculation ---
        // ถ้าคนตีเป็น Player ใช้ HIT ของ Player, ถ้าเป็นมอนสเตอร์ให้โอกาสโดน 50% + Base
        int attackerHit = (A != null) ? stats.getHit(attackerPlayer) : 100; // มอนสเตอร์ Hit 100 สมมติ
        // ถ้าคนโดนเป็น Player ใช้ FLEE ของ Player, ถ้าเป็นมอนสเตอร์ Flee 0
        int defenderFlee = (D != null) ? stats.getFlee((Player) defenderEntity) : 0;

        if (!isMagic) {
            double hitRate;
            if (attackerPlayer != null) {
                // สูตร Player vs Entity
                hitRate = (double) attackerHit / (attackerHit + defenderFlee);
            } else {
                // สูตร Mob vs Player (User's Logic เดิม)
                // Mob Hit สมมติ 50, สูตรเดิม: random * 100 > (80 + 50 - Flee)
                // ปรับให้เข้ากับ Flow ใหม่:
                int mobHit = 50;
                if (random.nextDouble() * 100 > (80 + mobHit - defenderFlee)) {
                    hitRate = 0.0; // Miss
                } else {
                    hitRate = 1.0; // Hit
                }
            }

            // Cap hit rate
            hitRate = Math.max(0.05, Math.min(1.0, hitRate));

            if (random.nextDouble() > hitRate) {
                event.setCancelled(true);
                plugin.showCombatFloatingText(defenderEntity.getLocation(), "§7MISS");
                return;
            }
        }

        // --- 2. Base Damage Calculation ---
        double damage;
        if (attackerPlayer != null) {
            // คำนวณดาเมจจาก Stat ถ้าผู้โจมตีเป็น Player
            damage = event.getDamage(); // Weapon base
            if (isMagic) {
                damage += stats.getMagicAttack(attackerPlayer);
            } else {
                damage += stats.getPhysicalAttack(attackerPlayer);
            }
        } else {
            // ถ้าผู้โจมตีเป็น Monster ใช้ดาเมจเดิมจาก Vanilla
            damage = vanillaDamage;
        }

        double calculatedDamage = damage * SKILL_POWER;

        // --- 3. Apply Attacker Bonuses (เฉพาะกรณี Attacker เป็น Player) ---
        if (A != null) {
            checkTriggers(attackerPlayer, defenderEntity, TriggerType.ON_HIT);

            if (isMagic) {
                calculatedDamage *= (1 + A.getMDmgBonusPercent() / 100.0);
                calculatedDamage += A.getMDmgBonusFlat();
            } else {
                calculatedDamage *= (1 + A.getPDmgBonusPercent() / 100.0);
                calculatedDamage += A.getPDmgBonusFlat();
                if (isRanged) {
                    calculatedDamage *= (1 + A.getRangePDmgPercent() / 100.0);
                } else {
                    calculatedDamage *= (1 + A.getMeleePDmgPercent() / 100.0);
                }
            }
        }

        // --- 4. Apply Defender Reductions (ทำงานแล้วแม้โดนมอนสเตอร์ตี) ---
        if (D != null && defenderEntity instanceof Player defenderPlayer) {
            checkTriggers(defenderPlayer, damagerEntity instanceof LivingEntity ? (LivingEntity)damagerEntity : null, TriggerType.ON_DEFEND);

            // Apply Soft DEF / MDEF
            calculatedDamage = isMagic ?
                    applyMagicDEF(A, D, calculatedDamage, defenderPlayer) : // A can be null, method handles it
                    applyPhysicalDEF(A, D, calculatedDamage, defenderPlayer); // A can be null, method handles it

            // Apply Reductions % (จุดที่แก้ไขเพื่อให้ PDMG Reduction 1000% ทำงาน)
            if (isMagic) {
                calculatedDamage *= (1 - D.getMDmgReductionPercent() / 100.0);
                calculatedDamage *= (1 - D.getMDmgReductionPercent() / 100.0); // Check duplicate line in original
            } else {
                calculatedDamage *= (1 - D.getPDmgReductionPercent() / 100.0);
            }

            if (!isMagic) {
                if (isRanged) {
                    calculatedDamage *= (1 - D.getRangePDReductionPercent() / 100.0);
                } else {
                    calculatedDamage *= (1 - D.getMeleePDReductionPercent() / 100.0);
                }
            }

            // Final Damage Resistance
            calculatedDamage *= (1 - D.getFinalDmgResPercent() / 100.0);
        }

        // --- 5. Critical ---
        boolean isCritical = false;
        if (!isMagic && A != null) { // Critical เฉพาะ Player โจมตี
            double effectiveCritChance = calculateCritChance(attackerPlayer, defenderEntity);
            if (random.nextDouble() < effectiveCritChance) {
                isCritical = true;
                double critMultiplier = calculateCritMultiplier(A, D);
                calculatedDamage *= critMultiplier;
            }
        }

        // --- 6. PVP / PVE Modifications ---
        if (D != null) {
            // Player ถูกตี (PVP หรือ PVE)
            if (A != null) {
                // PVP
                double attackerPvpRaw = A.getPvpDmgBonusPercent() - A.getPvpDmgReductionPercent();
                double defenderPvpRaw = D.getPvpDmgBonusPercent() - D.getPvpDmgReductionPercent();
                calculatedDamage *= getTierMultiplier(attackerPvpRaw - defenderPvpRaw);
            } else {
                // PVE (Mob ตี Player) -> ใช้ PVE Reduction ของผู้เล่น
                // Monster ไม่มี PVE Bonus เก็บไว้ (ถือเป็น 0)
                double pveDiff = 0.0 - D.getPveDmgReductionPercent(); // Monster PVE(0) - Player Reduce
                // หรือถ้าใช้ Logic แบบ RAW:
                // calculatedDamage *= getTierMultiplier(pveDiff);
                // แต่ปกติ PVE Reduction มักเป็น % ตรงๆ หรือ Flat, ถ้าใช้ระบบ Tier ให้ปรับตามต้องการ
            }
        } else if (A != null) {
            // Player ตี Mob (PVE)
            double attackerPveRaw = A.getPveDmgBonusPercent() - A.getPveDmgReductionPercent();
            calculatedDamage *= getTierMultiplier(attackerPveRaw); // เทียบกับ 0
        }

        // --- 7. Final Output Bonuses ---
        if (A != null) {
            calculatedDamage *= (1 + A.getFinalDmgPercent() / 100.0);
            if (isMagic) {
                calculatedDamage *= (1 + A.getFinalMDmgPercent() / 100.0);
            } else {
                calculatedDamage *= (1 + A.getFinalPDmgPercent() / 100.0);
            }
        }

        // --- 8. Shield ---
        if (D != null && D.getShieldValueFlat() > 0) {
            double absorb = Math.min(D.getShieldValueFlat(), calculatedDamage);
            calculatedDamage -= absorb;
            // TODO: ลดค่า Shield ใน PlayerData หรือ EffectManager ด้วยถ้าต้องการให้โล่แตก
        }

        // Apply Final Damage
        double finalDamage = Math.max(1.0, calculatedDamage); // Minimum 1 damage (หรือ 0 ถ้าต้องการให้กันได้หมด)
        event.setDamage(finalDamage);

        // Visuals
        if (isCritical && attackerPlayer != null) {
            showCritEffects(attackerPlayer, defenderEntity, finalDamage);
        } else if (finalDamage > 0) {
            plugin.showDamageFCT(defenderEntity.getLocation(), finalDamage);
        }

        // True Damage (Only if attacker is Player)
        if (A != null && A.getTrueDamageFlat() > 0.0) {
            double trueDmg = A.getTrueDamageFlat();
            // True Damage bypasses armor/defense, apply directly
            // Note: be careful of loop if damaging again via event
            if (defenderEntity.getHealth() > trueDmg) {
                defenderEntity.setHealth(defenderEntity.getHealth() - trueDmg);
            } else {
                defenderEntity.setHealth(0);
            }
            plugin.showTrueDamageFCT(defenderEntity.getLocation().add(0, 0.5, 0), trueDmg);
        }
    }

    // [FIX] ใช้ Cache จาก AttributeHandler
    private void checkTriggers(Player player, LivingEntity target, TriggerType type) {
        if (player == null) return;
        List<ItemSkillBinding> bindings = plugin.getAttributeHandler().getCachedTriggers(player, type);
        if (bindings == null || bindings.isEmpty()) return;

        for (ItemSkillBinding binding : bindings) {
            if (random.nextDouble() < binding.getChance()) {
                plugin.getSkillManager().castSkill(player, binding.getSkillId(), binding.getLevel(), target);
            }
        }
    }

    private double calculateCritChance(Player attacker, LivingEntity defender) {
        double attackerTotalCrit = plugin.getStatManager().getCritChance(attacker);
        double defenderCritRes = 0.0;
        if (defender instanceof Player defenderPlayer) {
            PlayerData D = plugin.getStatManager().getData(defenderPlayer.getUniqueId());
            defenderCritRes = D.getCritRes(); // ใช้ค่ารวมที่คำนวณแล้วใน PlayerData หรือคำนวณใหม่ตามสูตร
            // สูตร: (LUK * 0.2) + CritRes
            int totalLuk = D.getStat("LUK") + D.getPendingStat("LUK") + D.getLUKBonusGear();
            defenderCritRes += (totalLuk * 0.2);
        }
        return Math.max(0, (attackerTotalCrit - defenderCritRes) / 100.0);
    }

    // ปรับปรุงให้รองรับ A = null (Mob Attacker)
    private double applyPhysicalDEF(PlayerData A, PlayerData D, double damage, Player defenderPlayer) {
        StatManager stats = plugin.getStatManager();
        double softPDef = stats.getSoftDef(defenderPlayer);

        double ignoreDefFlat = (A != null) ? A.getIgnorePDefFlat() : 0;
        double ignoreDefPerc = (A != null) ? A.getIgnorePDefPercent() : 0;

        double def1 = Math.max(0, softPDef - ignoreDefFlat);
        double effectiveDef = def1 * (1 - ignoreDefPerc / 100.0);

        // สูตร Reduction แบบ RO (DEF / (DEF + K))
        double defReduction = effectiveDef / (effectiveDef + K_DEFENSE);
        return damage * (1 - defReduction);
    }

    // ปรับปรุงให้รองรับ A = null (Mob Attacker)
    private double applyMagicDEF(PlayerData A, PlayerData D, double damage, Player defenderPlayer) {
        StatManager stats = plugin.getStatManager();
        double softMDef = stats.getSoftMDef(defenderPlayer);

        double ignoreDefFlat = (A != null) ? A.getIgnoreMDefFlat() : 0;
        double ignoreDefPerc = (A != null) ? A.getIgnoreMDefPercent() : 0;

        double def1 = Math.max(0, softMDef - ignoreDefFlat);
        double effectiveMDef = def1 * (1 - ignoreDefPerc / 100.0);

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
        // สามารถเพิ่มการหักลบ Crit Res ของ D ตรงนี้ได้ถ้าต้องการ
        return 1.0 + bonusCrit; // Base Crit Damage 1.5 หรือเปล่า? ปกติ RO คือ 1.4 (40%) หรือ 1.5
    }

    private void showCritEffects(Player attacker, LivingEntity victim, double finalDamage) {
        plugin.showCombatFloatingText(victim.getLocation().add(0, 0.5, 0), "§c§lCRITICAL " + String.format("%.0f", finalDamage));
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20);
    }
}