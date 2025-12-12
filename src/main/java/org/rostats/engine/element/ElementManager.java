package org.rostats.engine.element;

import org.rostats.ThaiRoCorePlugin;

public class ElementManager {

    private final ThaiRoCorePlugin plugin;
    // ตารางธาตุ [Attacker][Defender] (1.0 = 100%, 1.5 = 150%, 0.0 = Miss)
    private final double[][] elementTable = new double[10][10];

    public ElementManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        initializeTable();
    }

    private void initializeTable() {
        // Default Fill 100%
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                elementTable[i][j] = 1.0;
            }
        }

        // --- Row: Attacker, Col: Defender ---
        // Indices: 0:Neu, 1:Wat, 2:Ear, 3:Fir, 4:Win, 5:Poi, 6:Hol, 7:Sha, 8:Gho, 9:Und

        // 0. Neutral Attack
        set(Element.NEUTRAL, Element.GHOST, 0.0);

        // 1. Water Attack
        set(Element.WATER, Element.WATER, 0.25);
        set(Element.WATER, Element.EARTH, 1.0);
        set(Element.WATER, Element.FIRE, 1.5);
        set(Element.WATER, Element.WIND, 0.9);
        set(Element.WATER, Element.HOLY, 0.75);
        set(Element.WATER, Element.SHADOW, 1.0);
        set(Element.WATER, Element.GHOST, 1.0);
        set(Element.WATER, Element.UNDEAD, 1.0);

        // 2. Earth Attack
        set(Element.EARTH, Element.WATER, 1.0);
        set(Element.EARTH, Element.EARTH, 0.25);
        set(Element.EARTH, Element.FIRE, 0.9);
        set(Element.EARTH, Element.WIND, 1.5);
        set(Element.EARTH, Element.HOLY, 0.75);
        set(Element.EARTH, Element.SHADOW, 1.0);
        set(Element.EARTH, Element.GHOST, 1.0);
        set(Element.EARTH, Element.UNDEAD, 1.0);

        // 3. Fire Attack
        set(Element.FIRE, Element.WATER, 0.9);
        set(Element.FIRE, Element.EARTH, 1.5);
        set(Element.FIRE, Element.FIRE, 0.25);
        set(Element.FIRE, Element.WIND, 1.0);
        set(Element.FIRE, Element.HOLY, 0.75);
        set(Element.FIRE, Element.SHADOW, 1.0);
        set(Element.FIRE, Element.GHOST, 1.0);
        set(Element.FIRE, Element.UNDEAD, 1.25); // Lv1 Undead takes 125%

        // 4. Wind Attack
        set(Element.WIND, Element.WATER, 1.75); // Wind vs Water (Actually 175% in some versions, 200% in others, using 1.75 for Lv1)
        set(Element.WIND, Element.EARTH, 0.9);
        set(Element.WIND, Element.FIRE, 1.0);
        set(Element.WIND, Element.WIND, 0.25);
        set(Element.WIND, Element.HOLY, 0.75);
        set(Element.WIND, Element.SHADOW, 1.0);
        set(Element.WIND, Element.GHOST, 1.0);
        set(Element.WIND, Element.UNDEAD, 1.0);

        // 5. Poison Attack
        set(Element.POISON, Element.POISON, 0.0);
        set(Element.POISON, Element.GHOST, 0.5);
        set(Element.POISON, Element.UNDEAD, 0.5);
        // Others roughly 1.0

        // 6. Holy Attack
        set(Element.HOLY, Element.NEUTRAL, 1.0);
        set(Element.HOLY, Element.HOLY, 0.0);
        set(Element.HOLY, Element.SHADOW, 1.25);
        set(Element.HOLY, Element.GHOST, 1.0);
        set(Element.HOLY, Element.UNDEAD, 1.50);

        // 7. Shadow Attack
        set(Element.SHADOW, Element.NEUTRAL, 1.0);
        set(Element.SHADOW, Element.HOLY, 1.25);
        set(Element.SHADOW, Element.SHADOW, 0.0);
        set(Element.SHADOW, Element.GHOST, 1.0);
        set(Element.SHADOW, Element.UNDEAD, 0.0); // Shadow heals undead usually, but here 0 or -1

        // 8. Ghost Attack
        set(Element.GHOST, Element.NEUTRAL, 0.7);
        set(Element.GHOST, Element.GHOST, 1.25);

        // 9. Undead Attack
        set(Element.UNDEAD, Element.HOLY, 1.50); // Undead atk vs Holy def? No, usually reverse.
        set(Element.UNDEAD, Element.UNDEAD, 0.0);
        set(Element.UNDEAD, Element.GHOST, 1.0);
    }

    private void set(Element atk, Element def, double val) {
        elementTable[atk.getIndex()][def.getIndex()] = val;
    }

    public double getModifier(Element attackElement, Element defenseElement) {
        if (attackElement == null) attackElement = Element.NEUTRAL;
        if (defenseElement == null) defenseElement = Element.NEUTRAL;
        return elementTable[attackElement.getIndex()][defenseElement.getIndex()];
    }
}