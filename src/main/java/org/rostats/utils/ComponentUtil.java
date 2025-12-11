package org.rostats.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Utility class for creating Adventure Components in Minecraft 1.21+
 */
public class ComponentUtil {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    /**
     * Create a simple colored component
     */
    public static Component text(String text, NamedTextColor color) {
        return Component.text(text).color(color).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Create a plain text component
     */
    public static Component text(String text) {
        return Component.text(text).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Parse MiniMessage format (e.g. <red>Hello <gradient:red:blue>World)
     */
    public static Component mm(String miniMessage) {
        return mm.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false);
    }

    public static Component error(String message) {
        return Component.text("❌ ").color(NamedTextColor.RED)
                .append(Component.text(message).color(NamedTextColor.RED));
    }

    public static Component success(String message) {
        return Component.text("✅ ").color(NamedTextColor.GREEN)
                .append(Component.text(message).color(NamedTextColor.GREEN));
    }

    public static Component info(String message) {
        return Component.text("ℹ ").color(NamedTextColor.YELLOW)
                .append(Component.text(message).color(NamedTextColor.YELLOW));
    }
}