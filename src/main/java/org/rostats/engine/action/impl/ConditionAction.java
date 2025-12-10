package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConditionAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String formula;
    private final List<SkillAction> successActions;
    private final List<SkillAction> failActions;

    public ConditionAction(ThaiRoCorePlugin plugin, String formula, List<SkillAction> successActions, List<SkillAction> failActions) {
        this.plugin = plugin;
        this.formula = formula;
        this.successActions = successActions == null ? new ArrayList<>() : successActions;
        this.failActions = failActions == null ? new ArrayList<>() : failActions;
    }

    @Override
    public ActionType getType() {
        return ActionType.CONDITION;
    }

    public boolean check(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        return FormulaParser.eval(formula, caster, target, level, context, plugin) > 0;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // Handled by SkillRunner directly
    }

    public List<SkillAction> getSuccessActions() { return successActions; }
    public List<SkillAction> getFailActions() { return failActions; }
    public String getFormula() { return formula; }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getType().name());
        map.put("formula", formula);
        // Warning: Recursive serialization might be heavy if deeply nested, but necessary for save
        if (!successActions.isEmpty()) {
            map.put("success", successActions.stream().map(SkillAction::serialize).collect(Collectors.toList()));
        }
        if (!failActions.isEmpty()) {
            map.put("fail", failActions.stream().map(SkillAction::serialize).collect(Collectors.toList()));
        }
        return map;
    }
}