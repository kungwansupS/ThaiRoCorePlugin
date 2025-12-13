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
import org.rostats.engine.element.Element;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.ItemAttribute;
import org.rostats.itemeditor.ItemSkillBinding;

import java.util.List;
import java.util.Random;

public class CombatHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Random random = new Random();
    private final double DEFAULT_SKILL_ATK_PERCENT = 100.0; // Normal Attack = 100%
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

        // [PHASE 4 FIX] เช็คว่าดาเมจนี้มาจาก SkillSystem (DamageAction) หรือไม่
        // ถ้าใช่ ให้ข้ามการคำนวณ Stats (ATK/DEF/CRIT) ของระบบ Auto-Attack
        if (victim.hasMetadata("RO_SKILL_DMG")) {
            return;
        }

        double vanillaDamage = event.getDamage();
        Player attackerPlayer = null;
        LivingEntity defenderEntity = victim;
        boolean isRanged = false;
        boolean isMagic = (event.getCause() == EntityDamageEvent.DamageCause.MAGIC);
        Entity damagerEntity = event.getDamager();

        boolean isCritical = false;

        if (damagerEntity instanceof Player p) {
            attackerPlayer = p;
        } else if (damagerEntity instanceof Projectile proj) {
            isRanged = true;
            if (proj.getShooter() instanceof Player p) attackerPlayer = p;
        }

        StatManager stats = plugin.getStatManager();
        PlayerData A = (attackerPlayer != null) ? stats.getData(attackerPlayer.getUniqueId()) : null;
        PlayerData D = (defenderEntity instanceof Player) ? stats.getData(defenderEntity.getUniqueId()) : null;

        // HIT / FLEE Logic
        int attackerHit = (A != null) ? stats.getHit(attackerPlayer) : 100;
        int defenderFlee = (D != null) ? stats.getFlee((Player) defenderEntity) : 0;

        if (!isMagic) {
            double hitRate;
            if (attackerPlayer != null) {
                hitRate = (double) attackerHit / (attackerHit + defenderFlee);
            } else {
                int mobHit = 50;
                if (random.nextDouble() * 100 > (80 + mobHit - defenderFlee)) {
                    hitRate = 0.0;
                } else {
                    hitRate = 1.0;
                }
            }
            hitRate = Math.max(0.05, Math.min(1.0, hitRate));

            if (random.nextDouble() > hitRate) {
                event.setCancelled(true);
                plugin.showCombatFloatingText(defenderEntity.getLocation(), "§7MISS");
                return;
            }
        }

        double finalDamage = 0.0;
        double elementModifier = 1.0; // Default

        if (attackerPlayer != null && A != null) {
            checkTriggers(attackerPlayer, defenderEntity, TriggerType.ON_HIT);

            // --- STEP 1: Basic Attack Calculation ---
            double baseATK = isMagic ? stats.calculateBaseMAtk(attackerPlayer) : stats.calculateBasePAtk(attackerPlayer);
            double equipFlat = isMagic ? (A.getWeaponMAtk() + A.getMAtkBonusFlat()) : (A.getWeaponPAtk() + A.getPAtkBonusFlat());
            double refineFlat = 0.0;
            double totalFlat = baseATK + equipFlat + refineFlat;
            double equipPercent = isMagic ? A.getMDmgBonusPercent() : A.getPDmgBonusPercent();
            double totalATK = totalFlat * (1 + equipPercent / 100.0);

            // --- STEP 2: Element Calculation (Phase 4 Implemented) ---
            Element atkElement = plugin.getElementManager().getAttackElement(attackerPlayer);
            Element defElement = plugin.getElementManager().getDefenseElement(defenderEntity);
            elementModifier = plugin.getElementManager().getModifier(atkElement, defElement);

            // Apply Element Multiplier to ATK (In RO, Element applies to damage, effectively ATK here)
            totalATK *= elementModifier;

            double skillATKPercent = DEFAULT_SKILL_ATK_PERCENT;
            double skillBase = totalATK * (skillATKPercent / 100.0);

            double skillDMGPercent = 0.0;
            if (!isMagic) {
                skillDMGPercent += isRanged ? A.getRangePDmgPercent() : A.getMeleePDmgPercent();
            }
            double afterSkillBonus = skillBase * (1 + skillDMGPercent / 100.0);

            double rawDefense = 0.0;
            if (D != null && defenderEntity instanceof Player defP) {
                checkTriggers(defP, attackerPlayer, TriggerType.ON_DEFEND);
                rawDefense = isMagic ? stats.getSoftMDef(defP) : stats.getSoftDef(defP);
            }

            double penPercent = isMagic ? A.getIgnoreMDefPercent() : A.getIgnorePDefPercent();
            double penFlat = isMagic ? A.getIgnoreMDefFlat() : A.getIgnorePDefFlat();

            double effectiveDef = (rawDefense * (1 - penPercent / 100.0)) - penFlat;
            effectiveDef = Math.max(0, effectiveDef);

            double defScale = 500.0;
            double damageAfterDEF = afterSkillBonus * (defScale / (defScale + effectiveDef));

            double finalDMGPercent = isMagic ? A.getFinalMDmgPercent() : A.getFinalPDmgPercent();
            finalDMGPercent += A.getFinalDmgPercent();

            if (D != null) {
                double pvpBonus = A.getPvpDmgBonusPercent();
                double pvpReduce = D.getPvpDmgReductionPercent();
                damageAfterDEF *= getTierMultiplier(pvpBonus - pvpReduce);
            } else {
                double pveBonus = A.getPveDmgBonusPercent();
                damageAfterDEF *= getTierMultiplier(pveBonus);
            }

            double reducePercent = 0.0;
            if (D != null) {
                reducePercent += isMagic ? D.getMDmgReductionPercent() : D.getPDmgReductionPercent();
                reducePercent += D.getFinalDmgResPercent();
                if (!isMagic) {
                    reducePercent += isRanged ? D.getRangePDReductionPercent() : D.getMeleePDReductionPercent();
                }
            }
            double totalFinalMultiplier = (1 + finalDMGPercent / 100.0) * (1 - Math.min(100, reducePercent) / 100.0);
            double preFlatDamage = damageAfterDEF * totalFinalMultiplier;

            // Critical Logic
            boolean canCrit = true;
            if (isMagic) {
                canCrit = false;
                if (checkMagicCritCondition(attackerPlayer, A)) {
                    canCrit = true;
                }
            }

            if (canCrit) {
                double critChance = calculateCritChance(attackerPlayer, defenderEntity);
                if (random.nextDouble() < critChance) {
                    isCritical = true;
                    double critDmg = A.getCritDmgPercent();
                    if (D != null) critDmg -= D.getCritDmgResPercent();

                    double critMult = 1.5 + (critDmg / 100.0);
                    preFlatDamage *= critMult;

                    showCritEffects(attackerPlayer, defenderEntity, preFlatDamage, isMagic);
                }
            }

            double finalFlat = A.getTrueDamageFlat();
            finalDamage = preFlatDamage + finalFlat;

        } else {
            // MOB ATTACKER LOGIC
            finalDamage = vanillaDamage;
            if (D != null && defenderEntity instanceof Player defP) {
                // Apply Element Def Logic for Mobs hitting Players
                Element mobElement = Element.NEUTRAL; // Todo: Get from mob config if possible
                Element playerDefElement = D.getDefenseElement();
                double mod = plugin.getElementManager().getModifier(mobElement, playerDefElement);
                finalDamage *= mod;
                elementModifier = mod; // Track for FCT color

                double def = isMagic ? stats.getSoftMDef(defP) : stats.getSoftDef(defP);
                finalDamage = Math.max(0, finalDamage - def);
                double reduce = isMagic ? D.getMDmgReductionPercent() : D.getPDmgReductionPercent();
                reduce += D.getPveDmgReductionPercent();
                finalDamage *= (1 - Math.min(100, reduce) / 100.0);
            }
        }

        if (D != null && D.getShieldValueFlat() > 0) {
            double absorb = Math.min(D.getShieldValueFlat(), finalDamage);
            finalDamage -= absorb;
        }

        finalDamage = Math.max(0, finalDamage);
        event.setDamage(finalDamage);

        // FCT Handling (Normal Hit)
        if (finalDamage > 0 && attackerPlayer != null) {
            if (!isCritical) {
                String dmgText = formatDamage(finalDamage);
                String colorPrefix = "§f"; // White

                // Color coding based on element effectiveness
                if (elementModifier > 1.0) colorPrefix = "§c"; // Red (Strong)
                else if (elementModifier < 1.0 && elementModifier > 0) colorPrefix = "§7"; // Gray (Weak)
                else if (elementModifier == 0) colorPrefix = "§8"; // Miss/Immune logic

                plugin.showCombatFloatingText(defenderEntity.getLocation(), colorPrefix + dmgText);
            }
        }
    }

    // --- Helper Methods ---
    private String formatDamage(double damage) {
        if (damage >= 1_000_000_000.0) return String.format("%.2fB", damage / 1_000_000_000.0);
        if (damage >= 1_000_000.0) return String.format("%.2fM", damage / 1_000_000.0);
        return String.format("%.0f", damage);
    }

    private boolean checkMagicCritCondition(Player player, PlayerData data) {
        return player.hasPermission("rostats.magiccrit");
    }

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
            defenderCritRes = D.getCritRes();
            int totalLuk = D.getStat("LUK") + D.getPendingStat("LUK") + D.getLUKBonusGear();
            defenderCritRes += (totalLuk * 0.2);
        }
        return Math.max(0, (attackerTotalCrit - defenderCritRes) / 100.0);
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

    private void showCritEffects(Player attacker, LivingEntity victim, double finalDamage, boolean isMagic) {
        String damageText = formatDamage(finalDamage);
        String color = isMagic ? "§5" : "§c"; // Purple vs Red
        plugin.showCombatFloatingText(victim.getLocation().add(0, 0.5, 0),  color + "§l" + damageText + "💥 ");
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        if (isMagic) {
            attacker.getWorld().spawnParticle(Particle.WITCH, victim.getLocation().add(0, 1, 0), 20);
        } else {
            attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 20);
        }
    }
}