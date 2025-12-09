package org.rostats.handler;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.itemeditor.ItemAttribute;
import org.rostats.itemeditor.ItemSkillBinding;
import org.rostats.engine.trigger.TriggerType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    private final Map<UUID, List<ItemSkillBinding>> cachedPassiveSkills = new ConcurrentHashMap<>();
    private final Map<UUID, Map<PotionEffectType, Integer>> cachedPassivePotions = new ConcurrentHashMap<>();
    private final Map<UUID, Map<TriggerType, List<ItemSkillBinding>>> cachedTriggers = new ConcurrentHashMap<>();

    public AttributeHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // --- Events to Trigger Update ---
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerStats(event.getPlayer()), 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerStats(event.getPlayer()), 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerStats(event.getPlayer()), 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            updatePlayerStats((Player) event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerStats(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerStats(event.getPlayer()));
    }

    // --- Caching Logic for CombatHandler ---
    public List<ItemSkillBinding> getCachedTriggers(Player player, TriggerType type) {
        Map<TriggerType, List<ItemSkillBinding>> map = cachedTriggers.get(player.getUniqueId());
        if (map == null) return Collections.emptyList();
        return map.getOrDefault(type, Collections.emptyList());
    }

    // --- Passive Effects Task ---
    public void runPassiveEffectsTask() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // 1. Potions
            Map<PotionEffectType, Integer> potions = cachedPassivePotions.get(uuid);
            if (potions != null && !potions.isEmpty()) {
                for (Map.Entry<PotionEffectType, Integer> entry : potions.entrySet()) {
                    player.addPotionEffect(new PotionEffect(entry.getKey(), 60, entry.getValue() - 1, true, false, true));
                }
            }

            // 2. Passive Tick Skills
            List<ItemSkillBinding> skills = cachedPassiveSkills.get(uuid);
            if (skills != null && !skills.isEmpty()) {
                for (ItemSkillBinding binding : skills) {
                    if (binding.getTrigger() == TriggerType.PASSIVE_TICK) {
                        plugin.getSkillManager().castSkill(player, binding.getSkillId(), binding.getLevel(), player, true);
                    }
                }
            }
        }
    }

    // --- Main Update Logic ---
    public void updatePlayerStats(Player player) {
        if (player == null) return;

        // [FIX] Use getStatManager().getData(...)
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        if (data == null) return;

        // 1. Reset all gear bonuses
        data.resetGearBonuses();

        // 2. Apply attributes from all equipment & Rebuild Caches
        applyAllEquipmentAttributes(player);

        // 3. Recalculate Logic
        data.calculateFinalStats();

        // 4. Apply to Bukkit Player
        applyToBukkitPlayer(player, data);
    }

    private void applyAllEquipmentAttributes(Player player) {
        UUID uuid = player.getUniqueId();
        List<ItemSkillBinding> newPassiveSkills = new ArrayList<>();
        Map<PotionEffectType, Integer> newPassivePotions = new HashMap<>();
        Map<TriggerType, List<ItemSkillBinding>> newTriggers = new HashMap<>();

        List<ItemStack> items = new ArrayList<>();
        if (player.getEquipment() != null) {
            items.addAll(Arrays.asList(player.getEquipment().getArmorContents()));
            items.add(player.getEquipment().getItemInMainHand());
            items.add(player.getEquipment().getItemInOffHand());
        }

        PlayerData data = plugin.getStatManager().getData(uuid);

        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);

                // Add Stats
                applyItemAttributes(data, attr);

                // Add Potions to Temp Cache
                if (!attr.getPotionEffects().isEmpty()) {
                    for (Map.Entry<PotionEffectType, Integer> entry : attr.getPotionEffects().entrySet()) {
                        newPassivePotions.merge(entry.getKey(), entry.getValue(), Math::max);
                    }
                }

                // Add Skills to Temp Cache
                if (!attr.getSkillBindings().isEmpty()) {
                    for (ItemSkillBinding binding : attr.getSkillBindings()) {
                        TriggerType t = binding.getTrigger();
                        if (t == TriggerType.PASSIVE_TICK) {
                            newPassiveSkills.add(binding);
                        } else if (t == TriggerType.PASSIVE_APPLY) {
                            plugin.getSkillManager().castSkill(player, binding.getSkillId(), binding.getLevel(), player, true);
                        } else if (t == TriggerType.ON_HIT || t == TriggerType.ON_DEFEND || t == TriggerType.ON_KILL) {
                            newTriggers.computeIfAbsent(t, k -> new ArrayList<>()).add(binding);
                        }
                    }
                }
            }
        }

        // Update Caches
        cachedPassiveSkills.put(uuid, newPassiveSkills);
        cachedPassivePotions.put(uuid, newPassivePotions);
        cachedTriggers.put(uuid, newTriggers);
    }

    private void applyItemAttributes(PlayerData data, ItemAttribute attr) {
        // Base
        data.addStr(attr.getStrGear());
        data.addAgi(attr.getAgiGear());
        data.addVit(attr.getVitGear());
        data.addInt(attr.getIntGear());
        data.addDex(attr.getDexGear());
        data.addLuk(attr.getLukGear());

        data.addMaxHpFlat(attr.getMaxHPFlat());
        data.addMaxHpPercent(attr.getMaxHPPercent());
        data.addMaxSpFlat(attr.getMaxSPFlat());
        data.addMaxSpPercent(attr.getMaxSPPercent());
        data.addHpRecovery(attr.getHpRecovery());
        data.addSpRecovery(attr.getSpRecovery());
        data.addHit(attr.getHitFlat());
        data.addFlee(attr.getFleeFlat());

        // Combat
        data.addWeaponPAtk(attr.getWeaponPAtk());
        data.addWeaponMAtk(attr.getWeaponMAtk());
        data.addRefinePAtk(attr.getRefinePAtk());
        data.addRefineMAtk(attr.getRefineMAtk());

        // [FIX] Use getPAtkFlat which is now available in ItemAttribute
        data.addPAtkBonusFlat(attr.getPAtkFlat());
        data.addMAtkBonusFlat(attr.getMAtkFlat());

        data.addPDef(attr.getPDefBonus());
        data.addMDef(attr.getMDefBonus());
        data.addRefinePDef(attr.getRefinePDef());
        data.addRefineMDef(attr.getRefineMDef());

        // Penetration
        data.addPPenFlat(attr.getPPenFlat());
        data.addPPenPercent(attr.getPPenPercent());
        data.addIgnorePDefFlat(attr.getIgnorePDefFlat());
        data.addIgnorePDefPercent(attr.getIgnorePDefPercent());
        data.addMPenFlat(attr.getMPenFlat());
        data.addMPenPercent(attr.getMPenPercent());
        data.addIgnoreMDefFlat(attr.getIgnoreMDefFlat());
        data.addIgnoreMDefPercent(attr.getIgnoreMDefPercent());

        // Casting
        data.addVarCastPercent(attr.getVarCTPercent());
        data.addVarCastFlat(attr.getVarCTFlat());
        data.addFixedCastPercent(attr.getFixedCTPercent());
        data.addFixedCastFlat(attr.getFixedCTFlat());

        // Cooldown
        data.addSkillCooldownPercent(attr.getSkillCDPercent());
        data.addSkillCooldownFlat(attr.getSkillCDFlat());
        data.addFinalCDPercent(attr.getFinalCDPercent());
        data.addGlobalCDPercent(attr.getGlobalCDPercent());
        data.addAfterCastDelayPercent(attr.getAfterCastDelayPercent());
        data.addAfterCastDelayFlat(attr.getAfterCastDelayFlat());
        data.addPreMotion(attr.getPreMotion());
        data.addPostMotion(attr.getPostMotion());
        data.addCancelMotion(attr.getCancelMotion());

        // Speed
        data.addASpdPercent(attr.getASpdPercent());
        data.addMSpdPercent(attr.getMSpdPercent());
        if (attr.getBaseMSPD() > 0) data.addBaseMSPD(attr.getBaseMSPD());
        data.addAtkIntervalPercent(attr.getAtkIntervalPercent());

        // Critical
        data.addCrit(attr.getCrit());
        data.addCritDmgPercent(attr.getCritDmgPercent());
        data.addFinalCritDmgPercent(attr.getFinalCritDmgPercent());
        data.addPerfectHit(attr.getPerfectHit());
        data.addCritRes(attr.getCritRes());
        data.addCritDmgResPercent(attr.getCritDmgResPercent());
        data.addPerfectDodge(attr.getPerfectDodge());

        // Universal DMG
        data.addPDmgPercent(attr.getPDmgPercent());
        data.addPDmgFlat(attr.getPDmgFlat());
        data.addPDmgReductionPercent(attr.getPDmgReductionPercent());
        data.addMDmgPercent(attr.getMDmgPercent());
        data.addMDmgFlat(attr.getMDmgFlat());
        data.addMDmgReductionPercent(attr.getMDmgReductionPercent());
        data.addTrueDamageFlat(attr.getTrueDamageFlat());
        data.addFinalDmgPercent(attr.getFinalDmgPercent());
        data.addFinalDmgResPercent(attr.getFinalDmgResPercent());

        // [FIX] Use the newly added getters in ItemAttribute
        data.addFinalPDmgPercent(attr.getFinalPDmgPercent());
        data.addFinalMDmgPercent(attr.getFinalMDmgPercent());

        // Distance / Content
        data.addMeleePDmgPercent(attr.getMeleePDmgPercent());
        data.addMeleePDReductionPercent(attr.getMeleePDReductionPercent());
        data.addRangePDmgPercent(attr.getRangePDmgPercent());
        data.addRangePDReductionPercent(attr.getRangePDReductionPercent());
        data.addPveDmgPercent(attr.getPveDmgPercent());
        data.addPveDmgReductionPercent(attr.getPveDmgReductionPercent());
        data.addPvpDmgPercent(attr.getPvpDmgPercent());
        data.addPvpDmgReductionPercent(attr.getPvpDmgReductionPercent());

        // Healing / Shield
        data.addHealingEffectPercent(attr.getHealingEffectPercent());
        data.addHealingFlat(attr.getHealingFlat());
        data.addHealingReceivedPercent(attr.getHealingReceivedPercent());
        data.addHealingReceivedFlat(attr.getHealingReceivedFlat());
        data.addLifestealPPercent(attr.getLifestealPPercent());
        data.addLifestealMPercent(attr.getLifestealMPercent());
        data.addShieldValueFlat(attr.getShieldValueFlat());
        data.addShieldRatePercent(attr.getShieldRatePercent());
    }

    private void applyToBukkitPlayer(Player player, PlayerData data) {
        // Apply Max HP
        double finalMaxHealth = data.getMaxHP();
        if (finalMaxHealth > 2048.0) finalMaxHealth = 2048.0;

        AttributeInstance maxHpAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHpAttr != null) maxHpAttr.setBaseValue(finalMaxHealth);

        // Apply Speed
        double speedBonus = data.getBaseMSPD() + (data.getMSpdPercent() / 100.0);
        double finalSpeed = Math.max(0.0, Math.min(1.0, speedBonus));

        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(finalSpeed);

        // Apply ASPD
        double baseAspd = 4.0;
        double finalAspd = baseAspd * (1 + data.getASpdPercent() / 100.0);

        AttributeInstance aspdAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (aspdAttr != null) aspdAttr.setBaseValue(finalAspd);

        // Apply Visual Armor (Soft DEF)
        double softDef = data.getPDefBonus() + data.getRefinePDef();
        AttributeInstance armorAttr = player.getAttribute(Attribute.GENERIC_ARMOR);
        if (armorAttr != null) armorAttr.setBaseValue(Math.min(30, softDef));

        if (player.getHealth() > finalMaxHealth) {
            player.setHealth(finalMaxHealth);
        }
    }
}