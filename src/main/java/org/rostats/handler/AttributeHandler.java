package org.rostats.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.ItemAttribute;
import org.rostats.itemeditor.ItemSkillBinding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AttributeHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    // Cache สำหรับเก็บ Passive Effect/Skill ของผู้เล่นแต่ละคน
    private final Map<UUID, List<ItemSkillBinding>> cachedPassiveSkills = new ConcurrentHashMap<>();
    private final Map<UUID, Map<PotionEffectType, Integer>> cachedPassivePotions = new ConcurrentHashMap<>();

    // [FIX] เพิ่ม Cache สำหรับ Trigger Skills (ON_HIT, ON_DEFEND, ON_KILL)
    // Map<UUID, Map<TriggerType, List<ItemSkillBinding>>>
    private final Map<UUID, Map<TriggerType, List<ItemSkillBinding>>> cachedTriggers = new ConcurrentHashMap<>();

    public AttributeHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updatePlayerStats(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cachedPassiveSkills.remove(uuid);
        cachedPassivePotions.remove(uuid);
        cachedTriggers.remove(uuid); // [FIX] Clear Trigger Cache
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerStats(player));
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerStats(event.getPlayer()));
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> updatePlayerStats(event.getPlayer()));
    }

    // --- TRIGGER LOGIC ---
    // [FIX] เมธอดสำหรับดึง Trigger จาก Cache (ใช้โดย CombatHandler)
    public List<ItemSkillBinding> getCachedTriggers(Player player, TriggerType type) {
        Map<TriggerType, List<ItemSkillBinding>> map = cachedTriggers.get(player.getUniqueId());
        if (map == null) return Collections.emptyList();
        return map.getOrDefault(type, Collections.emptyList());
    }

    // --- PASSIVE EFFECT LOGIC ---
    public void runPassiveEffectsTask() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // 1. Apply Cached Potions
            Map<PotionEffectType, Integer> potions = cachedPassivePotions.get(uuid);
            if (potions != null && !potions.isEmpty()) {
                for (Map.Entry<PotionEffectType, Integer> entry : potions.entrySet()) {
                    player.addPotionEffect(new PotionEffect(entry.getKey(), 60, entry.getValue() - 1, true, false, true));
                }
            }

            // 2. Apply Cached Passive Skills (PASSIVE_TICK)
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

    public void applyAllEquipmentAttributes(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        data.resetGearBonuses();

        UUID uuid = player.getUniqueId();
        List<ItemSkillBinding> newPassiveSkills = new ArrayList<>();
        Map<PotionEffectType, Integer> newPassivePotions = new HashMap<>();
        Map<TriggerType, List<ItemSkillBinding>> newTriggers = new HashMap<>(); // [FIX] Temp storage for triggers

        List<ItemStack> items = new ArrayList<>();
        if (player.getEquipment() != null) {
            items.addAll(Arrays.asList(player.getEquipment().getArmorContents()));
            items.add(player.getEquipment().getItemInMainHand());
            items.add(player.getEquipment().getItemInOffHand());
        }

        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
                // Apply Stats
                applyItemAttributes(player, attr);

                // Collect Potions
                if (!attr.getPotionEffects().isEmpty()) {
                    for (Map.Entry<PotionEffectType, Integer> entry : attr.getPotionEffects().entrySet()) {
                        newPassivePotions.merge(entry.getKey(), entry.getValue(), Math::max);
                    }
                }

                // Collect Skills
                if (!attr.getSkillBindings().isEmpty()) {
                    for (ItemSkillBinding binding : attr.getSkillBindings()) {
                        TriggerType t = binding.getTrigger();
                        if (t == TriggerType.PASSIVE_TICK) {
                            newPassiveSkills.add(binding);
                        } else if (t == TriggerType.PASSIVE_APPLY) {
                            plugin.getSkillManager().castSkill(player, binding.getSkillId(), binding.getLevel(), player, true);
                        } else if (t == TriggerType.ON_HIT || t == TriggerType.ON_DEFEND || t == TriggerType.ON_KILL) {
                            // [FIX] Cache Combat Triggers
                            newTriggers.computeIfAbsent(t, k -> new ArrayList<>()).add(binding);
                        }
                    }
                }
            }
        }

        // Update Caches
        cachedPassiveSkills.put(uuid, newPassiveSkills);
        cachedPassivePotions.put(uuid, newPassivePotions);
        cachedTriggers.put(uuid, newTriggers); // [FIX] Save to Trigger Cache
    }

    public void applyItemAttributes(Player player, ItemAttribute attr) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        // [คงเดิม: โค้ดส่วนการบวก Stat ทั้งหมดยังเหมือนเดิม]
        data.setSTRBonusGear(data.getSTRBonusGear() + attr.getStrGear());
        data.setAGIBonusGear(data.getAGIBonusGear() + attr.getAgiGear());
        data.setVITBonusGear(data.getVITBonusGear() + attr.getVitGear());
        data.setINTBonusGear(data.getINTBonusGear() + attr.getIntGear());
        data.setDEXBonusGear(data.getDEXBonusGear() + attr.getDexGear());
        data.setLUKBonusGear(data.getLUKBonusGear() + attr.getLukGear());

        data.setWeaponPAtk(data.getWeaponPAtk() + attr.getWeaponPAtk());
        data.setWeaponMAtk(data.getWeaponMAtk() + attr.getWeaponMAtk());

        data.setPAtkBonusFlat(data.getPAtkBonusFlat() + attr.getPAtkFlat());
        data.setMAtkBonusFlat(data.getMAtkBonusFlat() + attr.getMAtkFlat());

        data.setPDmgBonusPercent(data.getPDmgBonusPercent() + attr.getPDmgPercent());
        data.setMDmgBonusPercent(data.getMDmgBonusPercent() + attr.getMDmgPercent());
        data.setPDmgBonusFlat(data.getPDmgBonusFlat() + attr.getPDmgFlat());
        data.setMDmgBonusFlat(data.getMDmgBonusFlat() + attr.getMDmgFlat());

        data.setCritDmgPercent(data.getCritDmgPercent() + attr.getCritDmgPercent());
        data.setCritDmgResPercent(data.getCritDmgResPercent() + attr.getCritDmgResPercent());
        data.setCritRes(data.getCritRes() + attr.getCritRes());

        data.setPPenFlat(data.getPPenFlat() + attr.getPPenFlat());
        data.setMPenFlat(data.getMPenFlat() + attr.getMPenFlat());
        data.setPPenPercent(data.getPPenPercent() + attr.getPPenPercent());
        data.setMPenPercent(data.getMPenPercent() + attr.getMPenPercent());

        data.setFinalDmgPercent(data.getFinalDmgPercent() + attr.getFinalDmgPercent());
        data.setFinalDmgResPercent(data.getFinalDmgResPercent() + attr.getFinalDmgResPercent());
        data.setFinalPDmgPercent(data.getFinalPDmgPercent() + attr.getFinalPDmgPercent());
        data.setFinalMDmgPercent(data.getFinalMDmgPercent() + attr.getFinalMDmgPercent());

        data.setPveDmgBonusPercent(data.getPveDmgBonusPercent() + attr.getPveDmgPercent());
        data.setPvpDmgBonusPercent(data.getPvpDmgBonusPercent() + attr.getPvpDmgPercent());
        data.setPveDmgReductionPercent(data.getPveDmgReductionPercent() + attr.getPveDmgReductionPercent());
        data.setPvpDmgReductionPercent(data.getPvpDmgReductionPercent() + attr.getPvpDmgReductionPercent());

        data.setMaxHPPercent(data.getMaxHPPercent() + attr.getMaxHPPercent());
        data.setMaxSPPercent(data.getMaxSPPercent() + attr.getMaxSPPercent());

        data.setShieldValueFlat(data.getShieldValueFlat() + attr.getShieldValueFlat());
        data.setShieldRatePercent(data.getShieldRatePercent() + attr.getShieldRatePercent());

        data.setASpdPercent(data.getASpdPercent() + attr.getASpdPercent());
        data.setMSpdPercent(data.getMSpdPercent() + attr.getMSpdPercent());
        data.setBaseMSPD(data.getBaseMSPD() + attr.getBaseMSPD());

        data.setVarCTPercent(data.getVarCTPercent() + attr.getVarCTPercent());
        data.setVarCTFlat(data.getVarCTFlat() + attr.getVarCTFlat());
        data.setFixedCTPercent(data.getFixedCTPercent() + attr.getFixedCTPercent());
        data.setFixedCTFlat(data.getFixedCTFlat() + attr.getFixedCTFlat());

        data.setHealingEffectPercent(data.getHealingEffectPercent() + attr.getHealingEffectPercent());
        data.setHealingReceivedPercent(data.getHealingReceivedPercent() + attr.getHealingReceivedPercent());

        data.setLifestealPPercent(data.getLifestealPPercent() + attr.getLifestealPPercent());
        data.setLifestealMPercent(data.getLifestealMPercent() + attr.getLifestealMPercent());

        data.setHitBonusFlat(data.getHitBonusFlat() + attr.getHitFlat());
        data.setFleeBonusFlat(data.getFleeBonusFlat() + attr.getFleeFlat());

        data.setPDmgReductionPercent(data.getPDmgReductionPercent() + attr.getPDmgReductionPercent());
        data.setMDmgReductionPercent(data.getMDmgReductionPercent() + attr.getMDmgReductionPercent());

        data.setIgnorePDefFlat(data.getIgnorePDefFlat() + attr.getIgnorePDefFlat());
        data.setIgnoreMDefFlat(data.getIgnoreMDefFlat() + attr.getIgnoreMDefFlat());
        data.setIgnorePDefPercent(data.getIgnorePDefPercent() + attr.getIgnorePDefPercent());
        data.setIgnoreMDefPercent(data.getIgnoreMDefPercent() + attr.getIgnoreMDefPercent());

        data.setMeleePDmgPercent(data.getMeleePDmgPercent() + attr.getMeleePDmgPercent());
        data.setRangePDmgPercent(data.getRangePDmgPercent() + attr.getRangePDmgPercent());
        data.setMeleePDReductionPercent(data.getMeleePDReductionPercent() + attr.getMeleePDReductionPercent());
        data.setRangePDReductionPercent(data.getRangePDReductionPercent() + attr.getRangePDReductionPercent());

        data.setTrueDamageFlat(data.getTrueDamageFlat() + attr.getTrueDamageFlat());
    }

    public void updatePlayerStats(Player player) {
        applyAllEquipmentAttributes(player);

        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        double finalMaxHealth = data.getMaxHP();
        if (finalMaxHealth > 2048.0) finalMaxHealth = 2048.0;
        setAttribute(player, Attribute.GENERIC_MAX_HEALTH, finalMaxHealth);

        double speedBonus = data.getBaseMSPD() + (data.getMSpdPercent() / 100.0);
        double finalSpeed = Math.min(1.0, speedBonus);
        setAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, finalSpeed);

        double aspdMultiplier = plugin.getStatManager().getAspdBonus(player);
        setAttribute(player, Attribute.GENERIC_ATTACK_SPEED, 4.0 * aspdMultiplier);

        double softDef = plugin.getStatManager().getSoftDef(player);
        setAttribute(player, Attribute.GENERIC_ARMOR, softDef);

        if (player.getHealth() > finalMaxHealth) {
            player.setHealth(finalMaxHealth);
        }
    }

    private void setAttribute(Player player, Attribute attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) instance.setBaseValue(value);
    }
}