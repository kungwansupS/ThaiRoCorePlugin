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

    // [Phase 3] Dynamic Targeting System
    private final LivingEntity originalTarget; // เป้าหมายแรกสุด (เก็บไว้เผื่อกลับมาใช้)
    private LivingEntity currentTarget;        // เป้าหมายปัจจุบัน (เปลี่ยนไปเรื่อยๆ)

    private final int level;
    private final LinkedList<SkillAction> actionQueue = new LinkedList<>();
    private final Map<String, Double> globalContext = new HashMap<>();

    public SkillRunner(ThaiRoCorePlugin plugin, LivingEntity caster, LivingEntity target, int level, List<SkillAction> actions) {
        this.plugin = plugin;
        this.caster = caster;
        this.originalTarget = target;
        this.currentTarget = target; // เริ่มต้นด้วยเป้าหมายแรก
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
            LivingEntity newTarget = selector.resolveTarget(caster, originalTarget);

            // ถ้าหาเป้าไม่เจอ (null) ให้คงเป้าเดิมไว้ หรือจะให้หยุดสกิล?
            // ในที่นี้ให้คงเป้าเดิมไว้เพื่อความต่อเนื่อง (หรือจะให้เป็น null เพื่อหยุดก็ได้)
            if (newTarget != null) {
                this.currentTarget = newTarget;
            }
            runNext(); // ทำคำสั่งถัดไปทันที
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
            // ส่ง currentTarget ไปให้ Loop
            ((LoopAction) action).executeWithRunner(this, caster, currentTarget, level, globalContext);
            runNext();
        }
        else if (action.getType() == ActionType.CONDITION) {
            ConditionAction condition = (ConditionAction) action;
            // เช็คเงื่อนไขโดยใช้ currentTarget
            boolean result = condition.check(caster, currentTarget, level, globalContext);
            List<SkillAction> outcome = result ? condition.getSuccessActions() : condition.getFailActions();
            if (outcome != null && !outcome.isEmpty()) {
                injectActions(outcome);
            }
            runNext();
        }
        // 3. Execute Standard Actions (Damage, Heal, etc.)
        else {
            try {
                // IMPORTANT: ใช้ currentTarget แทน target เดิม
                if (currentTarget != null && currentTarget.isValid()) {
                    action.execute(caster, currentTarget, level, globalContext);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            runNext();
        }
    }
}