package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.element.Element;
import org.rostats.utils.FormulaParser;

import java.util.HashMap;
import java.util.Map;

public class DamageAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String formula;
    private final String element;
    private final boolean isBypassDef;

    // Debug Mode Toggle (Global Static)
    private static boolean DEBUG_MODE = false;

    public DamageAction(ThaiRoCorePlugin plugin, String formula, String element, boolean isBypassDef) {
        this.plugin = plugin;
        this.formula = formula;
        this.element = element;
        this.isBypassDef = isBypassDef;
    }

    // Static method to toggle debug
    public static void setDebugMode(boolean enabled) {
        DEBUG_MODE = enabled;
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE;
    }

    @Override
    public ActionType getType() {
        return ActionType.DAMAGE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {

        // ============ DEBUG LOGGING ============
        if (DEBUG_MODE && caster instanceof Player p) {
            p.sendMessage("§e━━━━━━ [DEBUG] DamageAction ━━━━━━");
            p.sendMessage("§e│ Caster: §f" + caster.getName() + " §7(Type: " + caster.getClass().getSimpleName() + ")");
            p.sendMessage("§e│ Target: §f" + (target != null ? target.getName() : "§cNULL"));
            p.sendMessage("§e│ Level: §f" + level);
            p.sendMessage("§e│ Formula (raw): §f" + formula);
            p.sendMessage("§e│ Element: §f" + element);
            p.sendMessage("§e│ Bypass DEF: §f" + isBypassDef);

            if (caster instanceof Player player) {
                double matk = plugin.getStatManager().getMagicAttack(player);
                double patk = plugin.getStatManager().getPhysicalAttack(player);
                p.sendMessage("§e│ Caster MATK: §a" + matk);
                p.sendMessage("§e│ Caster PATK: §a" + patk);
            }

            if (context != null && !context.isEmpty()) {
                p.sendMessage("§e│ Context values:");
                context.forEach((key, value) -> p.sendMessage("§e│  - §f" + key + " = §a" + value));
            } else {
                p.sendMessage("§e│ Context: §cEMPTY or NULL");
            }
        }
        // =======================================

        if (target == null) {
            if (DEBUG_MODE && caster instanceof Player p) {
                p.sendMessage("§c│ ❌ Target is NULL! Aborting.");
                p.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
            return;
        }

        double damage = 0.0;
        try {
            damage = FormulaParser.eval(formula, caster, target, level, context, plugin);

            if (DEBUG_MODE && caster instanceof Player p) {
                p.sendMessage("§a│ ✓ Damage calculated: §f" + damage);
            }
        } catch (Exception e) {
            if (DEBUG_MODE && caster instanceof Player p) {
                p.sendMessage("§c│ ❌ Formula Error: " + e.getMessage());
            }
            damage = 1.0;
        }

        if (damage <= 0) {
            if (DEBUG_MODE && caster instanceof Player p) {
                p.sendMessage("§c│ ❌ Damage <= 0! Aborting.");
                p.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
            return;
        }

        // --- Element Logic ---
        double elementMod = 1.0;
        if (!isBypassDef) {
            Element skillElement = Element.fromName(element);
            Element targetDefElement = plugin.getElementManager().getDefenseElement(target);
            elementMod = plugin.getElementManager().getModifier(skillElement, targetDefElement);
            damage *= elementMod;

            if (DEBUG_MODE && caster instanceof Player p) {
                p.sendMessage("§e│ Element Modifier: §f" + elementMod + " §7(" + skillElement + " vs " + targetDefElement + ")");
                p.sendMessage("§e│ Final Damage (after element): §f" + damage);
            }
        }

        if (isBypassDef) {
            double newHealth = Math.max(0, target.getHealth() - damage);
            target.setHealth(newHealth);
            plugin.showTrueDamageFCT(target.getLocation(), damage);

            if (DEBUG_MODE && caster instanceof Player p) {
                p.sendMessage("§d│ ⚡ TRUE DAMAGE dealt: §f" + damage);
            }
        } else {
            target.setMetadata("RO_SKILL_DMG", new FixedMetadataValue(plugin, true));
            target.damage(damage, caster);
            target.removeMetadata("RO_SKILL_DMG", plugin);

            String color = "§f";
            if (elementMod > 1.0) color = "§c";
            else if (elementMod < 1.0) color = "§7";

            plugin.showCombatFloatingText(target.getLocation(), color + String.format("%.0f", damage));

            if (DEBUG_MODE && caster instanceof Player p) {
                p.sendMessage("§a│ ✓ Normal damage dealt: §f" + damage);
            }
        }

        if (DEBUG_MODE && caster instanceof Player p) {
            p.sendMessage("§e━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "DAMAGE");
        map.put("formula", formula);
        map.put("element", element);
        map.put("bypass-def", isBypassDef);
        return map;
    }
}