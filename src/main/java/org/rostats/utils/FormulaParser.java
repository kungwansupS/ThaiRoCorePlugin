package org.rostats.utils;

import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;

public class FormulaParser {

    public static double eval(final String expression, final Player caster, final ThaiRoCorePlugin plugin) {
        // ลบช่องว่างออกทั้งหมดก่อนเริ่ม Parse
        final String str = expression.replaceAll("\\s+", "");

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
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
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
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) { // functions or variables
                    while ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || ch == '_') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseVariable(func);
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                return x;
            }

            // ฟังก์ชันสำหรับอ่านค่าตัวแปรโดยตรง ไม่ต้องใช้ replaceAll
            double parseVariable(String name) {
                if (caster == null) return 0;
                PlayerData data = plugin.getStatManager().getData(caster.getUniqueId());

                switch (name.toUpperCase()) {
                    case "MATK": return plugin.getStatManager().getMagicAttack(caster);
                    case "ATK": return plugin.getStatManager().getPhysicalAttack(caster);
                    case "STR": return data.getStat("STR");
                    case "AGI": return data.getStat("AGI");
                    case "VIT": return data.getStat("VIT");
                    case "INT": return data.getStat("INT");
                    case "DEX": return data.getStat("DEX");
                    case "LUK": return data.getStat("LUK");
                    case "LEVEL": return data.getBaseLevel();
                    default: return 0;
                }
            }
        }.parse();
    }
}