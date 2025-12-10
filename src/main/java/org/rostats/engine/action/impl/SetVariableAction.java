package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

import java.util.HashMap;
import java.util.Map;

public class SetVariableAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String varName;
    private final String expression;

    public SetVariableAction(ThaiRoCorePlugin plugin, String varName, String expression) {
        this.plugin = plugin;
        this.varName = varName;
        this.expression = expression;
    }

    @Override
    public ActionType getType() {
        return ActionType.SET_VARIABLE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        double val = FormulaParser.eval(expression, caster, target, level, context, plugin);
        context.put(varName.toLowerCase(), val);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getType().name());
        map.put("var", varName);
        map.put("val", expression);
        return map;
    }
}