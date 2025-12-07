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
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.itemeditor.ItemAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class AttributeHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    public AttributeHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updatePlayerStats(event.getPlayer());
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

    // --- NEW: Passive Effect Logic ---
    public void runPassiveEffectsTask() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Collect all equipment
            List<ItemStack> items = new ArrayList<>();
            if (player.getEquipment() != null) {
                items.addAll(Arrays.asList(player.getEquipment().getArmorContents()));
                items.add(player.getEquipment().getItemInMainHand());
                items.add(player.getEquipment().getItemInOffHand());
            }

            for (ItemStack item : items) {
                if (item != null && item.getType() != Material.AIR) {
                    // Read attributes from item
                    ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);

                    // Apply Potion Effects if present
                    if (attr != null && !attr.getPotionEffects().isEmpty()) {
                        for (Map.Entry<PotionEffectType, Integer> entry : attr.getPotionEffects().entrySet()) {
                            PotionEffectType type = entry.getKey();
                            int level = entry.getValue(); // This is the level (e.g. 1)

                            if (level > 0 && type != null) {
                                // Apply for 60 ticks (3 seconds) to ensure it stays active without flickering
                                // Amplifier = Level - 1 (e.g. Lv 1 = Amp 0)
                                // boolean ambient=true (makes particles less visible), particles=false (hide bubbles), icon=true
                                player.addPotionEffect(new PotionEffect(type, 60, level - 1, true, false, true));
                            }
                        }
                    }
                }
            }
        }
    }
    // ---------------------------------

    public void applyAllEquipmentAttributes(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        // 1. Reset old gear bonuses
        data.resetGearBonuses();

        // 2. Collect items
        List<ItemStack> items = new ArrayList<>();
        if (player.getEquipment() != null) {
            items.addAll(Arrays.asList(player.getEquipment().getArmorContents()));
            items.add(player.getEquipment().getItemInMainHand());
            items.add(player.getEquipment().getItemInOffHand());
        }

        // 3. Loop and apply
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                // Read attribute object from item PDC via Manager
                ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
                applyItemAttributes(player, attr);
            }
        }
    }

    public void applyItemAttributes(Player player, ItemAttribute attr) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

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
    }

    public void updatePlayerStats(Player player) {
        // 1. Recalculate gear bonuses
        applyAllEquipmentAttributes(player);

        // 2. Fetch data
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        // 3. Update Vanilla Attributes
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