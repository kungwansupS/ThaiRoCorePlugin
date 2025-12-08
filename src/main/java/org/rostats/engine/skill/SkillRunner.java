package org.rostats.engine.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.DelayAction;

import java.util.*;

public class SkillRunner {

    private final ThaiRoCorePlugin plugin;
    private final LivingEntity caster;
    private final LivingEntity target;
    private final int level;

    // Queue now holds Entry containing Action + Context
    private final LinkedList<QueueEntry> actionQueue = new LinkedList<>();

    public SkillRunner(ThaiRoCorePlugin plugin, LivingEntity caster, LivingEntity target, int level, List<SkillAction> actions) {
        this.plugin = plugin;
        this.caster = caster;
        this.target = target;
        this.level = level;

        // Initial load with empty context
        for (SkillAction action : actions) {
            actionQueue.add(new QueueEntry(action, new HashMap<>()));
        }
    }

    public void injectActions(List<QueueEntry> entries) {
        // Insert new actions at the front (for Loops/Sub-skills)
        for (int i = entries.size() - 1; i >= 0; i--) {
            actionQueue.addFirst(entries.get(i));
        }
    }

    public void runNext() {
        if (actionQueue.isEmpty()) return;

        QueueEntry entry = actionQueue.poll();
        SkillAction action = entry.action;
        Map<String, Double> context = entry.context;

        if (action.getType() == ActionType.DELAY) {
            // Calculate delay using context (allows dynamic delay)
            long delayTicks = ((DelayAction) action).getDelay(); // Or calculate using context if updated
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
            // LoopAction handles injection
            action.execute(caster, target, level, context);
            // Don't forget to pass the runner instance to LoopAction if needed,
            // but simpler: LoopAction calls plugin.getSkillManager() or casts caster to get runner?
            // Actually, we need to pass this runner to LoopAction.
            // *Hack*: We will handle Loop logic *inside* LoopAction via a callback or by passing runner.
            // See LoopAction implementation below.
            runNext();
        }
        else {
            try {
                action.execute(caster, target, level, context);
            } catch (Exception e) {
                e.printStackTrace();
            }
            runNext();
        }
    }

    // Wrapper class to hold action and its variables
    public static class QueueEntry {
        public final SkillAction action;
        public final Map<String, Double> context;

        public QueueEntry(SkillAction action, Map<String, Double> context) {
            this.action = action;
            this.context = context;
        }
    }
}