package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.skill.SkillRunner;
import org.rostats.utils.FormulaParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoopAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String startExpr;
    private final String endExpr;
    private final String stepExpr;
    private final String variableName;
    private final List<SkillAction> subActions;

    // This field is injected via SkillManager/SkillRunner just before execution.
    private SkillRunner currentRunner;

    public LoopAction(ThaiRoCorePlugin plugin, String start, String end, String step, String variableName, List<SkillAction> subActions) {
        this.plugin = plugin;
        this.startExpr = start;
        this.endExpr = end;
        this.stepExpr = step;
        this.variableName = variableName;
        this.subActions = subActions;
    }

    public void setRunner(SkillRunner runner) {
        this.currentRunner = runner;
    }

    @Override
    public ActionType getType() {
        return ActionType.LOOP;
    }

    // [NEW METHOD] Getter ที่ GUIListener ต้องการ
    public List<SkillAction> getSubActions() {
        return subActions;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // Calculate bounds, resolving placeholders
        double start = FormulaParser.eval(startExpr, caster, target, level, context, plugin);
        double end = FormulaParser.eval(endExpr, caster, target, level, context, plugin);
        double step = FormulaParser.eval(stepExpr, caster, target, level, context, plugin);

        if (Math.abs(step) < 0.0001) step = 1.0; // Prevent infinite loop if step is near zero

        List<SkillRunner.QueueEntry> newEntries = new ArrayList<>();

        if (start <= end) {
            for (double i = start; i <= end; i += step) {
                // Ensure no overstep due to floating point arithmetic
                if (step > 0 && i > end) break;
                addEntries(i, context, newEntries);
            }
        } else {
            // Backward loop
            for (double i = start; i >= end; i -= Math.abs(step)) {
                // Ensure no overstep due to floating point arithmetic
                if (step < 0 && i < end) break;
                addEntries(i, context, newEntries);
            }
        }

        // Inject generated actions at the front of the queue
        if (currentRunner != null) {
            currentRunner.injectActions(newEntries);
        }
    }

    private void addEntries(double val, Map<String, Double> parentContext, List<SkillRunner.QueueEntry> entries) {
        // Create new context for this iteration
        Map<String, Double> loopContext = new HashMap<>(parentContext);
        loopContext.put(variableName, val);

        // Clone actions with new context
        for (SkillAction sub : subActions) {
            entries.add(new SkillRunner.QueueEntry(sub, loopContext));
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "LOOP");
        map.put("start", startExpr);
        map.put("end", endExpr);
        map.put("step", stepExpr);
        map.put("var", variableName);

        List<Map<String, Object>> subs = new ArrayList<>();
        for (SkillAction a : subActions) subs.add(a.serialize());
        map.put("actions", subs);

        return map;
    }
}