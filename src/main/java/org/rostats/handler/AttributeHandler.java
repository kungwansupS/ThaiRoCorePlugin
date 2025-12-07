package org.rostats.handler;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
// แก้ไข: เปลี่ยนจาก ROStatsPlugin เป็น ThaiRoCorePlugin
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;

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

        // --- NEW: Req 3.1 & 3.2: Placeholder for Aggregating Core Stat Bonuses from Equipment ---
        // (ในระบบจริงต้องมีการวนลูปอ่านค่าจาก ItemAttributeManager สำหรับอุปกรณ์ทั้งหมด)

        // Placeholder Logic: ใช้ WeaponPAtk เป็นตัวแทนค่า STR Bonus ชั่วคราว (เพื่อแสดงให้เห็นว่าโค้ดทำงาน)
        // Note: ในโค้ดจริง ควรวนลูปอ่านค่า ItemAttribute.STR_BONUS_GEAR จากอุปกรณ์ทั้งหมด
        int strBonus = (int) data.getWeaponPAtk();
        int agiBonus = 0;
        int vitBonus = 0;
        int intBonus = 0;
        int dexBonus = 0;
        int lukBonus = 0;

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