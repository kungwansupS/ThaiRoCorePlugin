package org.rostats.engine.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.ConditionAction;
import org.rostats.engine.action.impl.DelayAction;
import org.rostats.engine.action.impl.LoopAction;
import org.rostats.engine.action.impl.TargetSelectorAction;

import java.util.*;

public class SkillRunner {

    private final ThaiRoCorePlugin plugin;
    private final LivingEntity caster;

    // Dynamic Targeting System
    private final LivingEntity originalTarget;
    private LivingEntity currentTarget;

    private final int level;
    private final LinkedList<SkillAction> actionQueue = new LinkedList<>();
    private final Map<String, Double> globalContext = new HashMap<>();

    public SkillRunner(ThaiRoCorePlugin plugin, LivingEntity caster, LivingEntity target, int level, List<SkillAction> actions, Map<String, Double> parentContext) {
        this.plugin = plugin;
        this.caster = caster;
        this.originalTarget = target;
        this.currentTarget = target; // Start with original target (can be null for Right-Click skills)
        this.level = level;

        if (actions != null) {
            this.actionQueue.addAll(actions);
        }

        // [MODIFIED] Context Inheritance: Merge parent context if provided (for sub-skills)
        if (parentContext != null) {
            this.globalContext.putAll(parentContext);
        }
    }

    public void injectActions(List<SkillAction> entries) {
        if (entries == null || entries.isEmpty()) return;
        for (int i = entries.size() - 1; i >= 0; i--) {
            actionQueue.addFirst(entries.get(i));
        }
    }

    public void runNext() {
        if (actionQueue.isEmpty()) return;

        SkillAction action = actionQueue.poll();

        // 1. Check for Target Switching
        if (action.getType() == ActionType.SELECT_TARGET) {
            TargetSelectorAction selector = (TargetSelectorAction) action;
            LivingEntity newTarget = selector.resolveTarget(caster, originalTarget);

            // If found, update current target
            if (newTarget != null) {
                this.currentTarget = newTarget;
            }
            runNext();
            return;
        }

        // 2. Handle Logic Actions
        if (action.getType() == ActionType.DELAY) {
            // [MODIFIED] คำนวณ Delay Ticks จาก Expression
            DelayAction delayAction = (DelayAction) action;
            long delayTicks = delayAction.getDelay(caster, currentTarget, level, globalContext, plugin);

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
            ((LoopAction) action).executeWithRunner(this, caster, currentTarget, level, globalContext);
            runNext();
        }
        else if (action.getType() == ActionType.CONDITION) {
            ConditionAction condition = (ConditionAction) action;
            boolean result = condition.check(caster, currentTarget, level, globalContext);
            List<SkillAction> outcome = result ? condition.getSuccessActions() : condition.getFailActions();
            if (outcome != null && !outcome.isEmpty()) {
                injectActions(outcome);
            }
            runNext();
        }
        // 3. Execute Standard Actions
        else {
            try {
                action.execute(caster, currentTarget, level, globalContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
            runNext();
        }
    }
}