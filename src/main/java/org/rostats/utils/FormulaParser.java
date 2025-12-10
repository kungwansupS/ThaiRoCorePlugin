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
        try {
            // 1. Replace Placeholders
            String parsed = parsePlaceholders(expression, caster, target, level, context, plugin);
            // 2. Parse Logic & Math
            return new Object() {
                int pos = -1, ch;

                void nextChar() { ch = (++pos < parsed.length()) ? parsed.charAt(pos) : -1; }
                boolean eat(int charToEat) {
                    while (ch == ' ') nextChar();
                    if (ch == charToEat) { nextChar(); return true; }
                    return false;
                }

                double parse() {
                    nextChar();
                    double x = parseLogic();
                    if (pos < parsed.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                    return x;
                }

                // Logic: OR, AND
                double parseLogic() {
                    double x = parseComparison();
                    for (;;) {
                        if      (eat('&')) { eat('&'); x = (parseComparison() != 0 && x != 0) ? 1.0 : 0.0; } // &&
                        else if (eat('|')) { eat('|'); x = (parseComparison() != 0 || x != 0) ? 1.0 : 0.0; } // ||
                        else return x;
                    }
                }

                // Comparison: <, >, <=, >=, ==, !=
                double parseComparison() {
                    double x = parseExpression();
                    for (;;) {
                        if      (eat('=')) { eat('='); x = (x == parseExpression()) ? 1.0 : 0.0; }
                        else if (eat('!')) { eat('='); x = (x != parseExpression()) ? 1.0 : 0.0; }
                        else if (eat('<')) {
                            if (eat('=')) x = (x <= parseExpression()) ? 1.0 : 0.0;
                            else          x = (x <  parseExpression()) ? 1.0 : 0.0;
                        }
                        else if (eat('>')) {
                            if (eat('=')) x = (x >= parseExpression()) ? 1.0 : 0.0;
                            else          x = (x >  parseExpression()) ? 1.0 : 0.0;
                        }
                        else return x;
                    }
                }

                // Math: +, -
                double parseExpression() {
                    double x = parseTerm();
                    for (;;) {
                        if      (eat('+')) x += parseTerm();
                        else if (eat('-')) x -= parseTerm();
                        else return x;
                    }
                }

                // Math: *, /, %
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
                    if (eat('!')) return (parseFactor() == 0) ? 1.0 : 0.0; // NOT operator

                    double x;
                    int startPos = this.pos;
                    if (eat('(')) { x = parseLogic(); eat(')'); }
                    else if ((ch >= '0' && ch <= '9') || ch == '.') {
                        while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                        x = Double.parseDouble(parsed.substring(startPos, this.pos));
                    } else if (ch >= 'a' && ch <= 'z') {
                        while (ch >= 'a' && ch <= 'z') nextChar();
                        String func = parsed.substring(startPos, this.pos);
                        x = parseFactor();
                        if (func.equals("sqrt")) x = Math.sqrt(x);
                        else if (func.equals("sin")) x = Math.sin(Math.toRadians(x));
                        else if (func.equals("cos")) x = Math.cos(Math.toRadians(x));
                        else if (func.equals("tan")) x = Math.tan(Math.toRadians(x));
                        else if (func.equals("abs")) x = Math.abs(x);
                        else if (func.equals("random")) x = Math.random() * x;
                        else if (func.equals("floor")) x = Math.floor(x);
                        else if (func.equals("ceil")) x = Math.ceil(x);
                    } else {
                        x = 0;
                    }
                    return x;
                }
            }.parse();
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static String parsePlaceholders(String text, LivingEntity caster, LivingEntity target, int level, Map<String, Double> context, ThaiRoCorePlugin plugin) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase();
            String value = "0";

            if (context != null && context.containsKey(key)) {
                value = String.valueOf(context.get(key));
            } else if (key.startsWith("player_")) {
                value = getEntityData(caster, key.substring(7), plugin);
            } else if (key.startsWith("target_")) {
                value = getEntityData(target, key.substring(7), plugin);
            } else if (key.equals("level")) {
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
            case "hp": return String.valueOf(entity.getHealth());
            case "max_hp": return String.valueOf(entity.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        }

        if (entity instanceof Player player) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            switch (param) {
                case "sp": return String.valueOf(data.getCurrentSP());
                case "max_sp": return String.valueOf(data.getMaxSP());
                case "str": return String.valueOf(data.getStat("STR"));
                case "agi": return String.valueOf(data.getStat("AGI"));
                case "vit": return String.valueOf(data.getStat("VIT"));
                case "int": return String.valueOf(data.getStat("INT"));
                case "dex": return String.valueOf(data.getStat("DEX"));
                case "luk": return String.valueOf(data.getStat("LUK"));
                case "patk": return String.valueOf(plugin.getStatManager().getPhysicalAttack(player));
                case "matk": return String.valueOf(plugin.getStatManager().getMagicAttack(player));
            }
        }
        return "0";
    }
}