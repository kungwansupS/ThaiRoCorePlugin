package org.rostats.handler;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerItemHeldEvent; // For holding item changes
import org.bukkit.event.player.PlayerSwapHandItemsEvent; // For off-hand changes
import org.bukkit.event.inventory.InventoryClickEvent; // For equipping/unequipping
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material; // Import Material
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;
import org.rostats.itemeditor.ItemAttribute; // Use the new POJO
import org.rostats.itemeditor.ItemAttributeManager;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List; // Import List
import java.util.ArrayList; // Import ArrayList
import java.util.Arrays; // Import Arrays

public class AttributeHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    public AttributeHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // --- Listener for stat recalculation trigger (Requirement 5) ---

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updatePlayerStats(event.getPlayer());
    }

    @EventHandler
    public void onSlotChange(PlayerItemHeldEvent event) {
        updatePlayerStats(event.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        updatePlayerStats(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check for clicks that might change equipment:
        // 1. Placing an item in an equipment slot
        // 2. Taking an item out of an equipment slot
        if (event.getSlotType().name().contains("ARMOR") ||
                (event.getSlot() == player.getInventory().getHeldItemSlot() && event.getClickedInventory() == player.getInventory()) ||
                (event.getSlot() == 40 && event.getClickedInventory() == player.getInventory())) { // Off-hand slot

            // Recalculate after a slight delay to ensure inventory updates fully
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> updatePlayerStats(player), 1L);
        }
    }

    // --- Core Attribute Application Logic ---

    public void updatePlayerStats(Player player) {
        // 1. Reset all PlayerData Gear Bonuses
        resetPlayerDataGearBonuses(player);

        // 2. Apply all equipped item attributes
        applyAllEquipmentAttributes(player);

        // 3. Recalculate Derived Attributes (MaxHP/MSPD/ASPD/DEF)
        recalculateDerivedAttributes(player);

        // 4. Refresh GUI (if open) - Assuming CharacterGUI has a refresh method
        if (plugin.getStatPanel() != null) {
            plugin.getStatPanel().refresh(player);
        }
    }

    private void resetPlayerDataGearBonuses(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        // Core Stat Bonuses
        data.setSTRBonusGear(0);
        data.setAGIBonusGear(0);
        data.setVITBonusGear(0);
        data.setINTBonusGear(0);
        data.setDEXBonusGear(0);
        data.setLUKBonusGear(0);

        // All other attributes (Reset to 0.0, except base values like CritDmgPercent/BaseMSPD)
        data.setWeaponPAtk(0.0);
        data.setWeaponMAtk(0.0);
        data.setPAtkBonusFlat(0.0);
        data.setMAtkBonusFlat(0.0);
        data.setCritRes(0.0);
        data.setCritDmgPercent(50.0); // Base value
        data.setCritDmgResPercent(0.0);
        data.setPDmgBonusPercent(0.0);
        data.setMDmgBonusPercent(0.0);
        data.setPDmgBonusFlat(0.0);
        data.setMDmgBonusFlat(0.0);
        data.setPDmgReductionPercent(0.0);
        data.setMDmgReductionPercent(0.0);
        data.setMeleePDmgPercent(0.0);
        data.setRangePDmgPercent(0.0);
        data.setMeleePDReductionPercent(0.0);
        data.setRangePDReductionPercent(0.0);
        data.setPPenFlat(0.0);
        data.setMPenFlat(0.0);
        data.setPPenPercent(0.0);
        data.setMPenPercent(0.0);
        data.setIgnorePDefFlat(0.0);
        data.setIgnoreMDefFlat(0.0);
        data.setIgnorePDefPercent(0.0);
        data.setIgnoreMDefPercent(0.0);
        data.setASpdPercent(0.0);
        data.setMSpdPercent(0.0);
        data.setBaseMSPD(0.1); // Base value
        data.setVarCTPercent(0.0);
        data.setVarCTFlat(0.0);
        data.setFixedCTPercent(0.0);
        data.setFixedCTFlat(0.0);
        data.setHealingEffectPercent(0.0);
        data.setHealingReceivedPercent(0.0);
        data.setFinalDmgPercent(0.0);
        data.setFinalDmgResPercent(0.0);
        data.setFinalPDmgPercent(0.0);
        data.setFinalMDmgPercent(0.0);
        data.setPveDmgBonusPercent(0.0);
        data.setPvpDmgBonusPercent(0.0);
        data.setPveDmgReductionPercent(0.0);
        data.setPvpDmgReductionPercent(0.0);
        data.setMaxHPPercent(0.0);
        data.setMaxSPPercent(0.0);
        data.setLifestealPPercent(0.0);
        data.setLifestealMPercent(0.0);
        data.setTrueDamageFlat(0.0);
        data.setShieldValueFlat(0.0);
        data.setShieldRatePercent(0.0);
        data.setHitBonusFlat(0.0);
        data.setFleeBonusFlat(0.0);
    }

    /**
     * Applies attributes from all equipped items (Requirement 3).
     */
    public void applyAllEquipmentAttributes(Player player) {
        ItemAttributeManager attributeManager = plugin.getItemAttributeManager();
        List<ItemStack> wornItems = new ArrayList<>();

        if (player.getEquipment() != null) {
            // Add armor contents (Helmet, Chestplate, Leggings, Boots)
            wornItems.addAll(Arrays.asList(player.getEquipment().getArmorContents()));
            // Add items in hands (MainHand and OffHand)
            wornItems.add(player.getEquipment().getItemInMainHand());
            wornItems.add(player.getEquipment().getItemInOffHand());
        }

        for (ItemStack item : wornItems) {
            if (item == null || item.getType() == Material.AIR) continue;

            ItemAttribute attr = attributeManager.getAttributesFromItem(item);

            // Check for vanilla removal flag (Requirement 4 check)
            if (attr.isRemoveVanillaAttribute()) {
                attributeManager.removeVanillaAttributes(item); // Remove if flag is set
            }

            // Apply attributes to PlayerData (Requirement 3)
            applyItemAttributes(player, attr);
        }
    }

    /**
     * Applies a single ItemAttribute POJO to the PlayerData object (Requirement 3).
     */
    public void applyItemAttributes(Player player, ItemAttribute attr) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        // Core Stat Bonuses (int)
        data.setSTRBonusGear(data.getSTRBonusGear() + attr.getStrGear());
        data.setAGIBonusGear(data.getAGIBonusGear() + attr.getAgiGear());
        data.setVITBonusGear(data.getVITBonusGear() + attr.getVitGear());
        data.setINTBonusGear(data.getINTBonusGear() + attr.getIntGear());
        data.setDEXBonusGear(data.getDEXBonusGear() + attr.getDexGear());
        data.setLUKBonusGear(data.getLUKBonusGear() + attr.getLukGear());

        // Flat ATK
        data.setWeaponPAtk(data.getWeaponPAtk() + attr.getWeaponPAtk());
        data.setWeaponMAtk(data.getWeaponMAtk() + attr.getWeaponMAtk());
        data.setPAtkBonusFlat(data.getPAtkBonusFlat() + attr.getPAtkFlat());
        data.setMAtkBonusFlat(data.getMAtkBonusFlat() + attr.getMAtkFlat());

        // Damage %/Flat (P/M DMg)
        data.setPDmgBonusPercent(data.getPDmgBonusPercent() + attr.getPDmgPercent());
        data.setMDmgBonusPercent(data.getMDmgBonusPercent() + attr.getMDmgPercent());
        data.setPDmgBonusFlat(data.getPDmgBonusFlat() + attr.getPDmgFlat());
        data.setMDmgBonusFlat(data.getMDmgBonusFlat() + attr.getMDmgFlat());

        // Critical
        data.setCritDmgPercent(data.getCritDmgPercent() + attr.getCritDmgPercent());
        data.setCritDmgResPercent(data.getCritDmgResPercent() + attr.getCritDmgResPercent());
        data.setCritRes(data.getCritRes() + attr.getCritRes());

        // Penetration / Ignore Def
        data.setPPenFlat(data.getPPenFlat() + attr.getPPenFlat());
        data.setMPenFlat(data.getMPenFlat() + attr.getMPenFlat());
        data.setPPenPercent(data.getPPenPercent() + attr.getPPenPercent());
        data.setMPenPercent(data.getMPenPercent() + attr.getMPenPercent());
        data.setIgnorePDefFlat(data.getIgnorePDefFlat() + attr.getIgnorePDefFlat());
        data.setIgnoreMDefFlat(data.getIgnoreMDefFlat() + attr.getIgnoreMDefFlat());
        data.setIgnorePDefPercent(data.getIgnorePDefPercent() + attr.getIgnorePDefPercent());
        data.setIgnoreMDefPercent(data.getIgnoreMDefPercent() + attr.getIgnoreMDefPercent());

        // Final Damage
        data.setFinalDmgPercent(data.getFinalDmgPercent() + attr.getFinalDmgPercent());
        data.setFinalDmgResPercent(data.getFinalDmgResPercent() + attr.getFinalDmgResPercent());
        data.setFinalPDmgPercent(data.getFinalPDmgPercent() + attr.getFinalPDmgPercent());
        data.setFinalMDmgPercent(data.getFinalMDmgPercent() + attr.getFinalMDmgPercent());

        // PVE/PVP
        data.setPveDmgBonusPercent(data.getPveDmgBonusPercent() + attr.getPveDmgPercent());
        data.setPvpDmgBonusPercent(data.getPvpDmgBonusPercent() + attr.getPvpDmgPercent());
        data.setPveDmgReductionPercent(data.getPveDmgReductionPercent() + attr.getPveDmgReductionPercent());
        data.setPvpDmgReductionPercent(data.getPvpDmgReductionPercent() + attr.getPvpDmgReductionPercent());

        // Max HP/SP %
        data.setMaxHPPercent(data.getMaxHPPercent() + attr.getMaxHPPercent());
        data.setMaxSPPercent(data.getMaxSPPercent() + attr.getMaxSPPercent());

        // Shield
        data.setShieldValueFlat(data.getShieldValueFlat() + attr.getShieldValueFlat());
        data.setShieldRatePercent(data.getShieldRatePercent() + attr.getShieldRatePercent());

        // Speed / Cast
        data.setASpdPercent(data.getASpdPercent() + attr.getASpdPercent());
        data.setMSpdPercent(data.getMSpdPercent() + attr.getMSpdPercent());
        data.setBaseMSPD(data.getBaseMSPD() + attr.getBaseMSPD());
        data.setVarCTPercent(data.getVarCTPercent() + attr.getVarCTPercent());
        data.setVarCTFlat(data.getVarCTFlat() + attr.getVarCTFlat());
        data.setFixedCTPercent(data.getFixedCTPercent() + attr.getFixedCTPercent());
        data.setFixedCTFlat(data.getFixedCTFlat() + attr.getFixedCTFlat());

        // Healing / Lifesteal
        data.setHealingEffectPercent(data.getHealingEffectPercent() + attr.getHealingEffectPercent());
        data.setHealingReceivedPercent(data.getHealingReceivedPercent() + attr.getHealingReceivedPercent());
        data.setLifestealPPercent(data.getLifestealPPercent() + attr.getLifestealPPercent());
        data.setLifestealMPercent(data.getLifestealMPercent() + attr.getLifestealMPercent());
        data.setTrueDamageFlat(data.getTrueDamageFlat() + attr.getTrueDamageFlat());

        // Hit/Flee
        data.setHitBonusFlat(data.getHitBonusFlat() + attr.getHitFlat());
        data.setFleeBonusFlat(data.getFleeBonusFlat() + attr.getFleeFlat());

        // Update player's native attributes (MaxHP, Speed, etc.)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            recalculateDerivedAttributes(player);
        }, 1L);
    }

    private void recalculateDerivedAttributes(Player player) {
        StatManager stats = plugin.getStatManager();
        PlayerData data = stats.getData(player.getUniqueId());

        // Max HP
        double finalMaxHealth = data.getMaxHP();
        if (finalMaxHealth > 2048.0) finalMaxHealth = 2048.0;
        setAttribute(player, Attribute.GENERIC_MAX_HEALTH, finalMaxHealth);

        // Movement Speed
        double speedBonus = data.getBaseMSPD() + (data.getMSpdPercent() / 100.0);
        double finalSpeed = speedBonus;
        if (finalSpeed > 1.0) finalSpeed = 1.0;
        setAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, finalSpeed);

        // ASPD
        double aspdMultiplier = stats.getAspdBonus(player);
        setAttribute(player, Attribute.GENERIC_ATTACK_SPEED, 4.0 * aspdMultiplier);

        // Soft DEF
        double softDef = stats.getSoftDef(player);
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