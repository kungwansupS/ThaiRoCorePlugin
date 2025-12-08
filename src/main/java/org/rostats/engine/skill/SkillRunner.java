package org.rostats.engine.skill;

import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.DelayAction;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SkillRunner {

    private final ThaiRoCorePlugin plugin;
    private final LivingEntity caster;
    private final LivingEntity target;
    private final int level;
    private final Queue<SkillAction> actionQueue;

    public SkillRunner(ThaiRoCorePlugin plugin, LivingEntity caster, LivingEntity target, int level, List<SkillAction> actions) {
        this.plugin = plugin;
        this.caster = caster;
        this.target = target;
        this.level = level;
        // สร้างคิวใหม่เพื่อไม่ให้กระทบ List ต้นฉบับ
        this.actionQueue = new LinkedList<>(actions);
    }

    public void runNext() {
        // ถ้าคิวหมด ให้จบการทำงาน
        if (actionQueue.isEmpty()) return;

        // ดึง Action ตัวถัดไปออกมา
        SkillAction action = actionQueue.poll();

        if (action.getType() == ActionType.DELAY) {
            // ถ้าเป็น Delay ให้หยุดรอ แล้วค่อยเรียก runNext() ใหม่
            long delayTicks = ((DelayAction) action).getDelay();
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (caster != null && caster.isValid()) {
                        runNext();
                    }
                }
            }.runTaskLater(plugin, delayTicks);
        } else {
            // ถ้าเป็น Action ปกติ ให้รันเลย
            try {
                action.execute(caster, target, level);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // ทำตัวถัดไปทันที (Recursive)
            runNext();
        }
    }
}