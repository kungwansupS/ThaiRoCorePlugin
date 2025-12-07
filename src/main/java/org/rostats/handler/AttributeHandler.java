package org.rostats.handler;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material; // Import Material
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;
import org.rostats.itemeditor.ItemAttribute;
import java.util.List; // Import List
import java.util.ArrayList; // Import ArrayList
import java.util.Arrays; // Import Arrays

public class AttributeHandler implements Listener {

    private final ThaiRoCorePlugin plugin;

    public AttributeHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        updatePlayerStats(event.getPlayer());
    }

    public void updatePlayerStats(Player player) {
        StatManager stats = plugin.getStatManager();
        PlayerData data = stats.getData(player.getUniqueId());

        // --- NEW: Implement Aggregation of Core Stat Bonuses from Equipment (Fix for 'ค่าไม่เพิ่ม') ---

        int strBonus = 0;
        int agiBonus = 0;
        int vitBonus = 0;
        int intBonus = 0;
        int dexBonus = 0;
        int lukBonus = 0;

        // 1. Collect all worn equipment items (Armor + Hands)
        List<ItemStack> wornItems = new ArrayList<>();
        if (player.getEquipment() != null) {
            // Add armor contents (Helmet, Chestplate, Leggings, Boots)
            wornItems.addAll(Arrays.asList(player.getEquipment().getArmorContents()));
            // Add items in hands (MainHand and OffHand)
            wornItems.add(player.getEquipment().getItemInMainHand());
            wornItems.add(player.getEquipment().getItemInOffHand());
        }

        // Loop through collected items
        for (ItemStack item : wornItems) {
            if (item != null && item.getType() != Material.AIR) { // Check for null and air explicitly
                // Sum bonuses from Persistent Data Container (PDC)
                // Note: The attribute manager is accessed via the plugin instance
                strBonus += (int) plugin.getItemAttributeManager().getAttribute(item, ItemAttribute.STR_BONUS_GEAR);
                agiBonus += (int) plugin.getItemAttributeManager().getAttribute(item, ItemAttribute.AGI_BONUS_GEAR);
                vitBonus += (int) plugin.getItemAttributeManager().getAttribute(item, ItemAttribute.VIT_BONUS_GEAR);
                intBonus += (int) plugin.getItemAttributeManager().getAttribute(item, ItemAttribute.INT_BONUS_GEAR);
                dexBonus += (int) plugin.getItemAttributeManager().getAttribute(item, ItemAttribute.DEX_BONUS_GEAR);
                lukBonus += (int) plugin.getItemAttributeManager().getAttribute(item, ItemAttribute.LUK_BONUS_GEAR);
            }
        }

        // Set the aggregated bonuses into PlayerData
        data.setSTRBonusGear(strBonus);
        data.setAGIBonusGear(agiBonus);
        data.setVITBonusGear(vitBonus);
        data.setINTBonusGear(intBonus);
        data.setDEXBonusGear(dexBonus);
        data.setLUKBonusGear(lukBonus);
        // -------------------------------------------------------------------------------------

        int vit = data.getStat("VIT");
        int agi = data.getStat("AGI");
        int dex = data.getStat("DEX");
        int baseLevel = data.getBaseLevel();

        // 1. VIT -> Max HP
        // ใช้สูตร MaxHP ที่ถูกแก้ไขใน PlayerData.java (ซึ่งรวม Gear Bonus แล้ว)
        double finalMaxHealth = data.getMaxHP();

        if (finalMaxHealth > 2048.0) finalMaxHealth = 2048.0;
        setAttribute(player, Attribute.GENERIC_MAX_HEALTH, finalMaxHealth);

        // 2. AGI -> Movement Speed
        // ใช้ BaseMSPD + MSpdPercent
        double speedBonus = data.getBaseMSPD() + (data.getMSpdPercent() / 100.0);
        double finalSpeed = speedBonus;
        if (finalSpeed > 1.0) finalSpeed = 1.0;
        setAttribute(player, Attribute.GENERIC_MOVEMENT_SPEED, finalSpeed);

        // 3. ASPD
        // ใช้ค่า ASPD Multiplier ที่ถูกแก้ไขใน StatManager.java
        double aspdMultiplier = stats.getAspdBonus(player);
        setAttribute(player, Attribute.GENERIC_ATTACK_SPEED, 4.0 * aspdMultiplier);

        // 4. Soft DEF
        // ใช้สูตร SoftPDEF ที่ถูกแก้ไขใน StatManager.java (VIT * 0.5 + AGI * 0.2)
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