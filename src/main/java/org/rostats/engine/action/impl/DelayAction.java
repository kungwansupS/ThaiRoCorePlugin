package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

import java.util.HashMap;
import java.util.Map;

public class DelayAction implements SkillAction {

    private final String delayExpr; // [MODIFIED] เปลี่ยนจาก long เป็น String

    public DelayAction(String delayExpr) { // [MODIFIED] รับ String
        this.delayExpr = delayExpr;
    }

    public long getDelay(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context, ThaiRoCorePlugin plugin) { // [MODIFIED] คำนวณ Ticks
        try {
            return (long) FormulaParser.eval(delayExpr, caster, target, level, context, plugin);
        } catch (Exception e) {
            return 20L; // Default 1 second delay
        }
    }

    @Override
    public ActionType getType() {
        return ActionType.DELAY;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // Logic การรออยู่ที่ SkillRunner
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "DELAY");
        map.put("ticks", delayExpr); // [MODIFIED] เก็บเป็น String
        return map;
    }
}