package org.rostats.engine.element;

public enum Element {
    NEUTRAL(0),
    WATER(1),
    EARTH(2),
    FIRE(3),
    WIND(4),
    POISON(5),
    HOLY(6),
    SHADOW(7),
    GHOST(8),
    UNDEAD(9);

    private final int index;

    Element(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static Element fromName(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}