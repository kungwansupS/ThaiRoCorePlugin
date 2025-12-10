package org.rostats.engine.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.ConditionAction;
import org.rostats.engine.action.impl.DelayAction;
import org.rostats.engine.action.impl.LoopAction;

import java.util.*;

public class SkillRunner {

    private final ThaiRoCorePlugin plugin;
    private final LivingEntity caster;
    private final LivingEntity target;
    private final int level;

    // Queue of actions to execute
    private final LinkedList<SkillAction> actionQueue = new LinkedList<>();

    // [Phase 2] Shared Context for the entire skill run (Variable Persistence)
    private final Map<String, Double> globalContext = new HashMap<>();

    public SkillRunner(ThaiRoCorePlugin plugin, LivingEntity caster, LivingEntity target, int level, List<SkillAction> actions) {
        this.plugin = plugin;
        this.caster = caster;
        this.target = target;
        this.level = level;

        // Load initial actions
        if (actions != null) {
            this.actionQueue.addAll(actions);
        }
    }

    public void injectActions(List<SkillAction> entries) {
        if (entries == null || entries.isEmpty()) return;
        // Insert new actions at the front (Stack behavior for nesting)
        for (int i = entries.size() - 1; i >= 0; i--) {
            actionQueue.addFirst(entries.get(i));
        }
    }

    public void runNext() {
        if (actionQueue.isEmpty()) return;

        SkillAction action = actionQueue.poll();

        if (action.getType() == ActionType.DELAY) {
            long delayTicks = ((DelayAction) action).getDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (caster != null && caster.isValid()) {
                        runNext();
                    }
                }
            }.runTaskLater(plugin, delayTicks);
        }
        else if (action.getType() == ActionType.LOOP) {
            // LoopAction executes and potentially injects more actions or runs logic
            ((LoopAction) action).executeWithRunner(this, caster, target, level, globalContext);
            runNext();
        }
        else if (action.getType() == ActionType.CONDITION) {
            // [Phase 2] Condition Logic
            ConditionAction condition = (ConditionAction) action;
            boolean result = condition.check(caster, target, level, globalContext);

            List<SkillAction> outcome = result ? condition.getSuccessActions() : condition.getFailActions();
            if (outcome != null && !outcome.isEmpty()) {
                injectActions(outcome);
            }
            runNext();
        }
        else {
            try {
                action.execute(caster, target, level, globalContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
            runNext();
        }
    }
}