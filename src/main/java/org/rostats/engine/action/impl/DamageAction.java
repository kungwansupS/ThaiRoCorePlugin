package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
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
    private final boolean isBypassDef; // [NEW FIELD]

    // [NEW CONSTRUCTOR]
    public DamageAction(ThaiRoCorePlugin plugin, String formula, String element, boolean isBypassDef) {
        this.plugin = plugin;
        this.formula = formula;
        this.element = element;
        this.isBypassDef = isBypassDef;
    }

    // [OLD CONSTRUCTOR for backward compatibility]
    public DamageAction(ThaiRoCorePlugin plugin, String formula, String element) {
        this(plugin, formula, element, false); // Default to false
    }

    @Override
    public ActionType getType() {
        return ActionType.DAMAGE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        if (target == null) {
            target = findTarget(caster, 10);
        }

        if (target == null) return;

        double damage = 0.0;
        try {
            // ใช้ FormulaParser แบบใหม่ที่รองรับ context
            damage = FormulaParser.eval(formula, caster, target, level, context, plugin);
        } catch (Exception e) {
            damage = 1.0;
        }

        if (damage <= 0) return;

        if (isBypassDef) { // [NEW LOGIC] Bypass Defense (True Damage)
            double newHealth = Math.max(0, target.getHealth() - damage);
            target.setHealth(newHealth);
            plugin.showTrueDamageFCT(target.getLocation(), damage); // ใช้ FCT สี True Damage (ส้ม/ทอง)
        } else {
            target.damage(damage, caster);
            plugin.showDamageFCT(target.getLocation(), damage);
        }
    }

    private LivingEntity findTarget(LivingEntity caster, int range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                range,
                0.5,
                e -> e instanceof LivingEntity && !e.equals(caster)
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }
        return null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "DAMAGE");
        map.put("formula", formula);
        map.put("element", element);
        map.put("bypass-def", isBypassDef); // [NEW SERIALIZATION]
        return map;
    }
}