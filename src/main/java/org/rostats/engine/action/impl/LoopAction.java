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
import java.util.stream.Collectors;

public class LoopAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String startExpr;
    private final String endExpr;
    private final String stepExpr;
    private final String varName;
    private final List<SkillAction> subActions;

    public LoopAction(ThaiRoCorePlugin plugin, String startExpr, String endExpr, String stepExpr, String varName, List<SkillAction> subActions) {
        this.plugin = plugin;
        this.startExpr = startExpr;
        this.endExpr = endExpr;
        this.stepExpr = stepExpr;
        this.varName = varName;
        this.subActions = subActions == null ? new ArrayList<>() : subActions;
    }

    @Override
    public ActionType getType() {
        return ActionType.LOOP;
    }

    /**
     * เมธอดนี้จะถูกเรียกโดย SkillRunner เพื่อจัดการลูป
     * เราจะไม่ unroll ลูปทั้งหมดทีเดียว แต่จะ Inject รอบต่อรอบ เพื่อให้ Delay ทำงานถูกต้อง
     */
    public void executeWithRunner(SkillRunner runner, LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // 1. คำนวณค่า Start, End, Step
        double start = FormulaParser.eval(startExpr, caster, target, level, context, plugin);
        double end = FormulaParser.eval(endExpr, caster, target, level, context, plugin);
        double step = FormulaParser.eval(stepExpr, caster, target, level, context, plugin); // Step calculated once per loop start or dynamic? Let's keep it dynamic if needed, or simplified.

        // 2. เช็ค/Initalize ตัวแปรลูป
        // ถ้ายังไม่มีตัวแปรนี้ใน Context ให้เริ่มที่ Start
        if (!context.containsKey(varName)) {
            context.put(varName, start);
        }

        double currentVal = context.get(varName);

        // 3. เช็คเงื่อนไขลูป (current < end หรือ current > end กรณี step ติดลบ)
        boolean continueLoop;
        if (step >= 0) {
            continueLoop = currentVal < end; // ใช้ < หรือ <= ตาม Logic ที่ต้องการ (ปกติ Loop programming มักจะเป็น < end ถ้า end เป็น exclusive, แต่ถ้า inclusive ใช้ <=)
            // สมมติเป็น inclusive เหมือน for(i=start; i<=end; i++)
            continueLoop = currentVal <= end + 0.0001;
        } else {
            continueLoop = currentVal >= end - 0.0001;
        }

        if (continueLoop) {
            // สร้าง Batch ของ Action ที่จะทำในรอบนี้
            List<SkillAction> batch = new ArrayList<>(subActions);

            // เพิ่ม Action พิเศษต่อท้ายเพื่อ "เพิ่มค่าตัวแปร" และ "วนลูปซ้ำ"
            batch.add(new LoopIncrementAction(plugin, this, runner));

            // Inject เข้าไปที่หัวแถวของ Queue
            runner.injectActions(batch);
        } else {
            // ลูปจบแล้ว ลบตัวแปรออกเพื่อความสะอาด
            context.remove(varName);
        }
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        // เมธอดนี้จะไม่ถูกเรียกโดยตรงจาก Runner สำหรับ LoopAction (จะเรียก executeWithRunner แทน)
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", getType().name());
        map.put("start", startExpr);
        map.put("end", endExpr);
        map.put("step", stepExpr);
        map.put("var", varName);
        map.put("actions", subActions.stream().map(SkillAction::serialize).collect(Collectors.toList()));
        return map;
    }

    // Getters
    public List<SkillAction> getSubActions() { return subActions; }
    public String getStartExpr() { return startExpr; }
    public String getEndExpr() { return endExpr; }
    public String getStepExpr() { return stepExpr; }
    public String getVarName() { return varName; }

    // --- Internal Action for Recursion ---
    private static class LoopIncrementAction implements SkillAction {
        private final ThaiRoCorePlugin plugin;
        private final LoopAction parentLoop;
        private final SkillRunner runner;

        public LoopIncrementAction(ThaiRoCorePlugin plugin, LoopAction parentLoop, SkillRunner runner) {
            this.plugin = plugin;
            this.parentLoop = parentLoop;
            this.runner = runner;
        }

        @Override
        public ActionType getType() {
            return ActionType.COMMAND; // ใช้ Type หลอกๆ เพราะมันทำงานภายใน
        }

        @Override
        public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
            // 1. คำนวณ Step
            double step = FormulaParser.eval(parentLoop.getStepExpr(), caster, target, level, context, plugin);

            // 2. อัปเดตค่าตัวแปรใน Context
            String var = parentLoop.getVarName();
            double current = context.getOrDefault(var, 0.0);
            context.put(var, current + step);

            // 3. เรียก LoopAction ตัวแม่ ให้เช็คเงื่อนไขรอบถัดไป
            parentLoop.executeWithRunner(runner, caster, target, level, context);
        }

        @Override
        public Map<String, Object> serialize() {
            return new HashMap<>();
        }
    }
}