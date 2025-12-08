package org.rostats.utils;

import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

public class FormulaParser {

    public static double eval(String expression, Player caster, ThaiRoCorePlugin plugin) {
        // 1. Replace Variables
        if (caster != null) {
            PlayerData data = plugin.getStatManager().getData(caster.getUniqueId());
            // [FIX] ใช้ replaceAll พร้อมกับ \\b (Word Boundary) เพื่อป้องกันการแทนค่าคำที่ซ้อนกัน
            // เช่น "MATK" จะไม่ถูก "ATK" แทนค่าทับจนเพี้ยน
            expression = expression.replaceAll("\\bMATK\\b", String.valueOf(plugin.getStatManager().getMagicAttack(caster)));
            expression = expression.replaceAll("\\bATK\\b", String.valueOf(plugin.getStatManager().getPhysicalAttack(caster)));
            expression = expression.replaceAll("\\bSTR\\b", String.valueOf(data.getStat("STR")));
            expression = expression.replaceAll("\\bINT\\b", String.valueOf(data.getStat("INT")));
            expression = expression.replaceAll("\\bDEX\\b", String.valueOf(data.getStat("DEX")));
            expression = expression.replaceAll("\\bLUK\\b", String.valueOf(data.getStat("LUK")));
            expression = expression.replaceAll("\\bAGI\\b", String.valueOf(data.getStat("AGI")));
            expression = expression.replaceAll("\\bVIT\\b", String.valueOf(data.getStat("VIT")));
            expression = expression.replaceAll("\\bLEVEL\\b", String.valueOf(data.getBaseLevel()));
        }

        // Remove spaces
        final String finalExpression = expression.replaceAll("\\s+", "");

        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < finalExpression.length()) ? finalExpression.charAt(pos) : -1;
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
                if (pos < finalExpression.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(finalExpression.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }
}