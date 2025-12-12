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

    public SkillRunner(ThaiRoCorePlugin plugin, LivingEntity caster, LivingEntity target, int level, List<SkillAction> actions) {
        this.plugin = plugin;
        this.caster = caster;
        this.originalTarget = target;
        this.currentTarget = target; // Start with original target (can be null for Right-Click skills)
        this.level = level;

        if (actions != null) {
            this.actionQueue.addAll(actions);
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
            // selector.resolveTarget จะ handle logic ภายในเอง
            LivingEntity newTarget = selector.resolveTarget(caster, originalTarget);

            // If found, update current target
            if (newTarget != null) {
                this.currentTarget = newTarget;
            } else if (selector.getMode() == TargetSelectorAction.SelectorMode.CURSOR) {
                // [FIXED] ถ้าหา CURSOR ไม่เจอ (เล็งอากาศ) ให้ currentTarget เป็น null
                this.currentTarget = null;
            }
            // ถ้า Mode ไม่ใช่ CURSOR และหาไม่เจอ ให้อิงตาม Target เดิม

            runNext();
            return;
        }

        // 2. Handle Logic Actions
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
                // Actions like Raycast, Sound, Particle do not need a target to run.
                // Individual Actions (like Damage) should handle null checks internally if required.
                action.execute(caster, currentTarget, level, globalContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
            runNext();
        }
    }
}