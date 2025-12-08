package org.rostats.engine.action.impl;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

import java.util.HashMap;
import java.util.Map;

public class DamageAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String formula;
    private final String element;

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
        // Find target if active skill (target is null)
        if (target == null) {
            target = findTarget(caster, 10); // RayTrace 10 blocks
        }

        if (target == null) return; // No target found, fail silently

        double damage = 0.0;
        try {
            String calcFormula = formula.replace("LVL", String.valueOf(level));
            if (caster instanceof Player) {
                damage = FormulaParser.eval(calcFormula, (Player) caster, plugin);
            } else {
                damage = 10 * level;
            }
        } catch (Exception e) {
            damage = 1.0;
        }
        target.damage(damage, caster);
        plugin.showDamageFCT(target.getLocation(), damage);
    }

    private LivingEntity findTarget(LivingEntity caster, int range) {
        for (Entity e : caster.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity && e != caster) {
                // Simple LoS check: Is it roughly in front? (Angle < 30 degrees approx)
                Vector toTarget = e.getLocation().toVector().subtract(caster.getLocation().toVector());
                if (caster.getLocation().getDirection().angle(toTarget) < 0.5) {
                    return (LivingEntity) e;
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "DAMAGE");
        map.put("formula", formula);
        map.put("element", element);
        return map;
    }
}