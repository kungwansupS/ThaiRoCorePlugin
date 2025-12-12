package org.rostats.engine.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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
    private final String skillId; // [DEBUG] เพิ่ม field สำหรับเก็บ Skill ID

    public SkillRunner(ThaiRoCorePlugin plugin, LivingEntity caster, LivingEntity target, int level, List<SkillAction> actions, String skillId) {
        this.plugin = plugin;
        this.caster = caster;
        this.originalTarget = target;
        this.currentTarget = target; // Start with original target (can be null for Right-Click skills)
        this.level = level;
        this.skillId = skillId; // [DEBUG] กำหนด Skill ID

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
        // [FIX]: Check caster validity FIRST before processing
        if (caster == null || !caster.isValid()) {
            // Debugging output on termination
            if (plugin.isSkillDebugEnabled()) {
                plugin.getLogger().info(String.format("[SkillDBG] %s: Runner terminated (Caster invalid).", skillId));
            }
            return;
        }

        if (actionQueue.isEmpty()) {
            if (plugin.isSkillDebugEnabled() && caster instanceof Player) {
                ((Player)caster).sendMessage("§7[DBG] §b" + skillId + "§7: Finished. (Queue Empty)");
            }
            return;
        }

        SkillAction action = actionQueue.poll();

        String actionInfo;
        if (action.getType() == ActionType.DELAY) {
            actionInfo = action.getType().name() + " (" + ((DelayAction)action).getDelay() + " ticks)";
        } else {
            actionInfo = action.getType().name();
        }

        // [DEBUG TRACE - CONSOLE]
        if (plugin.isSkillDebugEnabled()) {
            String targetName = currentTarget != null ? currentTarget.getName() : "None";
            String casterName = caster != null ? caster.getName() : "Unknown";

            plugin.getLogger().info(String.format("[SkillDBG] %s: Caster=%s, Action=%s, Target=%s, QueueSize=%d",
                    skillId,
                    casterName,
                    actionInfo,
                    targetName,
                    actionQueue.size()));
        }

        // [DEBUG TRACE - PLAYER]
        if (caster instanceof Player player) {
            String targetName = currentTarget != null ? currentTarget.getName() : "None";
            String msg = String.format("§7[DBG] §b%s§7: Executing §e%s§7, Target: §a%s",
                    skillId,
                    actionInfo,
                    targetName);
            player.sendMessage(msg);
        }

        // 1. Check for Target Switching
        if (action.getType() == ActionType.SELECT_TARGET) {
            TargetSelectorAction selector = (TargetSelectorAction) action;
            LivingEntity newTarget = selector.resolveTarget(caster, originalTarget);

            if (newTarget != null) {
                this.currentTarget = newTarget;
            } else if (selector.getMode() == TargetSelectorAction.SelectorMode.CURSOR) {
                this.currentTarget = null;
            }

            // [FIX]: For instant actions like SELECT_TARGET, we call runNext() immediately.
            runNext();
            return;
        }

        // 2. Handle Logic Actions
        if (action.getType() == ActionType.DELAY) {
            long delayTicks = ((DelayAction) action).getDelay();

            if (delayTicks <= 0) {
                runNext(); // Skip delay if invalid
                return;
            }

            // Schedule the continuation on the Main Thread after the delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    // Re-run runNext() on the main thread after delay
                    plugin.getServer().getScheduler().runTask(plugin, SkillRunner.this::runNext);
                }
            }.runTaskLater(plugin, delayTicks);

            // STOP execution of the synchronous chain until the delay completes.
            // Do NOT call runNext() here.
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
                if (plugin.isSkillDebugEnabled()) plugin.getLogger().severe("Error executing action " + action.getType().name() + " in skill " + skillId + ": " + e.getMessage());
                e.printStackTrace();
            }
            runNext();
        }
    }
}