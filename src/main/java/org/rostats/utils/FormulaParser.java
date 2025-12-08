package org.rostats.utils;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaParser {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");

    public static double eval(String expression, LivingEntity caster, LivingEntity target, int level, Map<String, Double> context, ThaiRoCorePlugin plugin) {
        if (expression == null || expression.isEmpty()) return 0.0;

        // 1. Replace Placeholders
        String parsed = parsePlaceholders(expression, caster, target, level, context, plugin);

        // 2. Calculate Math
        return parseMath(parsed);
    }

    public static String parsePlaceholders(String text, LivingEntity caster, LivingEntity target, int level, Map<String, Double> context, ThaiRoCorePlugin plugin) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value = "0";

            // Context Variables (from Loop)
            if (context != null && context.containsKey(key)) {
                value = String.valueOf(context.get(key));
            }
            // Caster Stats
            else if (key.startsWith("player_")) {
                value = getEntityData(caster, key.substring(7), plugin);
            }
            // Target Stats
            else if (key.startsWith("target_")) {
                value = getEntityData(target, key.substring(7), plugin);
            }
            // Global
            else if (key.equals("level")) {
                value = String.valueOf(level);
            }

            matcher.appendReplacement(sb, value);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String getEntityData(LivingEntity entity, String param, ThaiRoCorePlugin plugin) {
        if (entity == null) return "0";
        Location loc = entity.getLocation();

        switch (param) {
            case "x": return String.valueOf(loc.getX());
            case "y": return String.valueOf(loc.getY());
            case "z": return String.valueOf(loc.getZ());
            case "yaw": return String.valueOf(loc.getYaw());
            case "pitch": return String.valueOf(loc.getPitch());
            case "uuid": return entity.getUniqueId().toString();
            case "name": return entity.getName();
        }

        if (entity instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            switch (param) {
                case "hp": return String.valueOf(player.getHealth());
                case "max_hp": return String.valueOf(data.getMaxHP());
                case "sp": return String.valueOf(data.getCurrentSP());
                case "str": return String.valueOf(data.getStat("STR"));
                // Add more stats as needed

                // [FIXED] Add P.ATK and M.ATK from StatManager
                case "patk": return String.valueOf(plugin.getStatManager().getPhysicalAttack(player));
                case "matk": return String.valueOf(plugin.getStatManager().getMagicAttack(player));
            }
        }
        return "0";
    }

    // Simple Recursive Descent Parser for Math
    private static double parseMath(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) return x; // Ignore trailing garbage
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else if (eat('%')) x %= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    // Function support (sin, cos, etc)
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                    else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                    else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                    else if (func.equals("abs")) x = Math.abs(x);
                    else if (func.equals("random")) x = Math.random() * x;
                } else {
                    x = 0; // Unexpected char
                }
                return x;
            }
        }.parse();
    }
}