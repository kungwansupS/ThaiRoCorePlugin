package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

public class DamageAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String formula;
    private final String element; // Reserved for Phase 6

    public DamageAction(ThaiRoCorePlugin plugin, String formula, String element) {
        this.plugin = plugin;
        this.formula = formula;
        this.element = element;
    }

    @Override
    public ActionType getType() {
        return ActionType.DAMAGE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        if (target == null) return;

        double damage = 0.0;
        try {
            // Replace level var first
            String calcFormula = formula.replace("LVL", String.valueOf(level));

            if (caster instanceof Player) {
                damage = FormulaParser.eval(calcFormula, (Player) caster, plugin);
            } else {
                // Mob logic (Basic fallback)
                damage = 10 * level;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error calculating damage formula: " + formula);
            damage = 1.0;
        }

        // Apply Damage
        // ใน Phase 6 เราจะ Mark ว่าดาเมจนี้มาจาก Skill เพื่อไม่ให้ CombatHandler คำนวณซ้ำซ้อน
        // แต่ตอนนี้ให้ทำงานแบบ Direct Damage ไปก่อน
        target.damage(damage, caster);

        // Show FCT
        plugin.showDamageFCT(target.getLocation(), damage);
    }
}