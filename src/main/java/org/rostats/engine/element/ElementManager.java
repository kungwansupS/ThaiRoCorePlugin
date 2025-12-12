package org.rostats.engine.element;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

import java.util.HashMap;
import java.util.Map;

public class ElementManager {

    private final ThaiRoCorePlugin plugin;
    // ตารางธาตุ [Attacker][Defender]
    private final double[][] elementTable = new double[10][10];
    private final Map<EntityType, Element> mobElementMap = new HashMap<>();

    public ElementManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        initializeTable();
        initializeMobElements();
    }

    private void initializeMobElements() {
        // Mapping Vanilla Mobs to RO Elements (Basic set)
        mobElementMap.put(EntityType.ZOMBIE, Element.UNDEAD);
        mobElementMap.put(EntityType.SKELETON, Element.UNDEAD);
        mobElementMap.put(EntityType.ZOMBIE_VILLAGER, Element.UNDEAD);
        mobElementMap.put(EntityType.WITHER_SKELETON, Element.UNDEAD);
        mobElementMap.put(EntityType.PHANTOM, Element.UNDEAD);
        mobElementMap.put(EntityType.DROWNED, Element.WATER); // Or Undead/Water

        mobElementMap.put(EntityType.BLAZE, Element.FIRE);
        mobElementMap.put(EntityType.MAGMA_CUBE, Element.FIRE);
        mobElementMap.put(EntityType.GHAST, Element.FIRE);

        mobElementMap.put(EntityType.ENDERMAN, Element.SHADOW);
        mobElementMap.put(EntityType.ENDER_DRAGON, Element.SHADOW);
        mobElementMap.put(EntityType.WITHER, Element.UNDEAD);

        mobElementMap.put(EntityType.GUARDIAN, Element.WATER);
        mobElementMap.put(EntityType.ELDER_GUARDIAN, Element.WATER);
        mobElementMap.put(EntityType.DOLPHIN, Element.WATER);
        mobElementMap.put(EntityType.SQUID, Element.WATER);

        mobElementMap.put(EntityType.IRON_GOLEM, Element.EARTH);
        mobElementMap.put(EntityType.TURTLE, Element.EARTH);

        mobElementMap.put(EntityType.VEX, Element.GHOST);
        mobElementMap.put(EntityType.ALLAY, Element.HOLY); // Maybe?

        // Add more as needed
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

        // --- 0. Neutral Attack ---
        set(Element.NEUTRAL, Element.GHOST, 0.0);   // Miss

        // --- 1. Water Attack ---
        set(Element.WATER, Element.WIND, 0.90);
        set(Element.WATER, Element.WATER, 0.25);
        set(Element.WATER, Element.FIRE, 1.75);
        set(Element.WATER, Element.HOLY, 0.75);

        // --- 2. Earth Attack ---
        set(Element.EARTH, Element.WIND, 1.75);
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
        set(Element.WIND, Element.WATER, 1.75);
        set(Element.WIND, Element.EARTH, 0.80);
        set(Element.WIND, Element.WIND, 0.25);
        set(Element.WIND, Element.HOLY, 0.75);

        // --- 5. Poison Attack ---
        set(Element.POISON, Element.POISON, 0.00);
        set(Element.POISON, Element.UNDEAD, 0.50);
        set(Element.POISON, Element.GHOST, 0.50);
        set(Element.POISON, Element.HOLY, 0.75);

        // --- 6. Holy Attack ---
        set(Element.HOLY, Element.HOLY, 0.00);
        set(Element.HOLY, Element.SHADOW, 1.25);
        set(Element.HOLY, Element.UNDEAD, 1.50);
        set(Element.HOLY, Element.GHOST, 1.00);
        set(Element.HOLY, Element.POISON, 1.00);
        set(Element.HOLY, Element.NEUTRAL, 1.00);

        // --- 7. Shadow Attack ---
        set(Element.SHADOW, Element.SHADOW, 0.00);
        set(Element.SHADOW, Element.HOLY, 1.25);
        set(Element.SHADOW, Element.UNDEAD, 0.00); // Heal in RO, 0 here
        set(Element.SHADOW, Element.GHOST, 1.00);
        set(Element.SHADOW, Element.POISON, 0.50);

        // --- 8. Ghost Attack ---
        set(Element.GHOST, Element.NEUTRAL, 0.70);
        set(Element.GHOST, Element.GHOST, 1.75);

        // --- 9. Undead Attack ---
        set(Element.UNDEAD, Element.UNDEAD, 0.00);
        set(Element.UNDEAD, Element.GHOST, 1.00);
        set(Element.UNDEAD, Element.POISON, 0.50);
        set(Element.UNDEAD, Element.HOLY, 0.50);
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

    public Element getDefenseElement(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            if (data != null) return data.getDefenseElement();
        }
        return mobElementMap.getOrDefault(entity.getType(), Element.NEUTRAL);
    }

    public Element getAttackElement(LivingEntity entity) {
        if (entity instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            if (data != null) return data.getAttackElement();
        }
        return mobElementMap.getOrDefault(entity.getType(), Element.NEUTRAL);
    }
}