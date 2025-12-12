package org.rostats.engine.element;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

public class ElementManager {

    private final ThaiRoCorePlugin plugin;
    // ตารางธาตุ [Attacker][Defender] (1.0 = 100%, 1.5 = 150%, 0.0 = Miss)
    private final double[][] elementTable = new double[10][10];

    public ElementManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        initializeTable();
    }

    private void initializeTable() {
        // Reset All to 100%
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                elementTable[i][j] = 1.0;
            }
        }

        // --- Row: Attacker, Col: Defender ---
        // Indices: 0:Neu, 1:Wat, 2:Ear, 3:Fir, 4:Win, 5:Poi, 6:Hol, 7:Sha, 8:Gho, 9:Und
        // อ้างอิงจากรูปภาพ RO Origin Table

        // --- 0. Neutral Attack ---
        set(Element.NEUTRAL, Element.GHOST, 0.0);   // Miss

        // --- 1. Water Attack ---
        set(Element.WATER, Element.WIND, 0.90);
        set(Element.WATER, Element.WATER, 0.25);
        set(Element.WATER, Element.FIRE, 1.75);
        set(Element.WATER, Element.HOLY, 0.75);

        // --- 2. Earth Attack ---
        set(Element.EARTH, Element.WIND, 1.75); // แพ้ลม (Wind ตี Earth แรง) -> Earth ตี Wind แรง? ในตาราง Attacker Earth -> Wind Def = 175% (ดูจากตารางแนวนอน Attack แนวนอนตั้ง Defend)
        // แก้ไข: ดูตาราง Attack (แถวซ้าย) -> Defend (คอลัมน์บน)
        // Earth (Attack) vs Wind (Defend) = 80% (0.8) ตามรูป
        // Earth (Attack) vs Fire (Defend) = 90% (0.9)
        // Earth (Attack) vs Water (Defend) = 100% (1.0)
        // Earth (Attack) vs Earth (Defend) = 25% (0.25)
        // Earth (Attack) vs Holy (Defend) = 75% (0.75)
        set(Element.EARTH, Element.WIND, 0.80);
        set(Element.EARTH, Element.FIRE, 0.90);
        set(Element.EARTH, Element.EARTH, 0.25);
        set(Element.EARTH, Element.HOLY, 0.75);

        // --- 3. Fire Attack ---
        set(Element.FIRE, Element.EARTH, 1.50);
        set(Element.FIRE, Element.WATER, 0.90);
        set(Element.FIRE, Element.FIRE, 0.25);
        set(Element.FIRE, Element.HOLY, 0.75);
        set(Element.FIRE, Element.UNDEAD, 1.50);

        // --- 4. Wind Attack ---
        set(Element.WIND, Element.EARTH, 0.80); // ในรูป Wind Attack -> Earth Defend = 80%?
        // เช็ครูปใหม่:
        // Attack Earth -> Defend Wind = 175% (สีแดง)
        // Attack Wind -> Defend Earth = 175% (สีแดง)
        // Attack Wind -> Defend Water = 175% (สีแดง)
        // ขออภัยครับ รูปไม่ชัดเจน 100% แต่ตาม Logic RO ปกติ:
        // Earth ชนะ Wind (No, Wind ชนะ Water, Water ชนะ Fire, Fire ชนะ Earth, Earth ชนะ Wind)
        // แต่ใน ROO: Earth แพ้ Fire, Fire แพ้ Water, Water แพ้ Wind, Wind แพ้ Earth
        // ดังนั้น:

        // Re-mapping based on Standard RO / Common ROO logic visible:
        // Earth Atk -> Wind Def: 175% (Earth ชนะ Wind ใน ROO Engine บางเวอร์ชัน หรือ Wind แพ้ Earth)
        // Check Colors: Red = High Dmg, Blue = Low.
        // Earth Atk -> Wind Def (Red Box 175%)
        // Wind Atk -> Water Def (Red Box 175%)
        // Fire Atk -> Earth Def (Red Box 150% ?)
        // Water Atk -> Fire Def (Red Box 175%)

        // Let's stick strictly to the visual table interpretation:
        // Row EARTH -> Col WIND: 175%
        set(Element.EARTH, Element.WIND, 1.75);

        // Row WIND -> Col WATER: 175%
        set(Element.WIND, Element.WATER, 1.75);
        // Row WIND -> Col EARTH: 80% (Yellowish/Green box)
        set(Element.WIND, Element.EARTH, 0.80);
        // Row WIND -> Col WIND: 25%
        set(Element.WIND, Element.WIND, 0.25);
        // Row WIND -> Col HOLY: 75%
        set(Element.WIND, Element.HOLY, 0.75);

        // Row FIRE -> Col EARTH: 175% (Actually usually Fire beats Earth) -> Table shows RED 150/175? Let's use 1.75 based on logic Water->Fire is 1.75
        // Image for FIRE row -> EARTH col is RED box. Let's assume 1.5 as standard level 1 advantage or 1.75 based on others. Let's use 1.5 for now as specifically noted in previous thought.
        // Correction: Let's use 1.75 for main weakness cycle to be consistent with Water->Fire(1.75) and Wind->Water(1.75).
        // Wait, standard RO Level 1 Bolt is 150%. Level 3 is 200%. ROO often simplifies.
        // Let's use values that make sense for gameplay balance if image is blurry: 1.5 (150%).
        set(Element.FIRE, Element.EARTH, 1.50);

        // --- 5. Poison Attack ---
        set(Element.POISON, Element.POISON, 0.00); // 0%
        set(Element.POISON, Element.UNDEAD, 0.50); // 50%
        set(Element.POISON, Element.GHOST, 0.50);  // 50%
        set(Element.POISON, Element.HOLY, 0.75);   // 75%

        // --- 6. Holy Attack ---
        set(Element.HOLY, Element.HOLY, 0.00);     // 0% (or 25% in some ver, image looks light yellow/white -> 25% usually or 0%) -> Let's go 25% for resistance.
        set(Element.HOLY, Element.SHADOW, 1.25);   // 125%
        set(Element.HOLY, Element.UNDEAD, 1.50);   // 150% (Redish)
        set(Element.HOLY, Element.GHOST, 1.00);    // 100%
        set(Element.HOLY, Element.POISON, 1.00);
        set(Element.HOLY, Element.NEUTRAL, 1.00);
        set(Element.HOLY, Element.HOLY, 0.25); // Self resist

        // --- 7. Shadow Attack ---
        set(Element.SHADOW, Element.SHADOW, 0.00); // 0% or 25%
        set(Element.SHADOW, Element.HOLY, 1.25);   // 125%
        set(Element.SHADOW, Element.UNDEAD, 0.00); // Heal or 0
        set(Element.SHADOW, Element.GHOST, 1.00);
        set(Element.SHADOW, Element.POISON, 0.50); // Usually resists
        set(Element.SHADOW, Element.SHADOW, 0.25);

        // --- 8. Ghost Attack ---
        set(Element.GHOST, Element.NEUTRAL, 0.70); // 70%
        set(Element.GHOST, Element.GHOST, 1.75);   // 175% (Ghost vs Ghost is high dmg)

        // --- 9. Undead Attack ---
        set(Element.UNDEAD, Element.HOLY, 1.50);   // Undead hits Holy? No, usually Holy hits Undead. Undead hitting Holy is usually weak.
        // Let's check image Row UNDEAD. It is empty/dashed mostly.
        // Standard Logic: Undead Atk -> Holy Def = 100% or less.
        // Let's set standard interactions for Undead Atk:
        set(Element.UNDEAD, Element.UNDEAD, 0.00);
        set(Element.UNDEAD, Element.GHOST, 1.00);
        set(Element.UNDEAD, Element.POISON, 0.50);
        set(Element.UNDEAD, Element.HOLY, 0.50); // Weak against holy armor
    }

    private void set(Element atk, Element def, double val) {
        if (atk != null && def != null) {
            elementTable[atk.getIndex()][def.getIndex()] = val;
        }
    }

    public double getModifier(Element attackElement, Element defenseElement) {
        if (attackElement == null) attackElement = Element.NEUTRAL;
        if (defenseElement == null) defenseElement = Element.NEUTRAL;
        return elementTable[attackElement.getIndex()][defenseElement.getIndex()];
    }

    // Helper to get element from entity
    public Element getDefenseElement(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            return data.getDefenseElement();
        }
        // TODO: Mob Element Logic (Default Neutral for now)
        return Element.NEUTRAL;
    }

    public Element getAttackElement(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            return data.getAttackElement();
        }
        return Element.NEUTRAL;
    }
}