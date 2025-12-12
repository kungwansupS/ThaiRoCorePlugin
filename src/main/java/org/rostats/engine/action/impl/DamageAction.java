package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.util.RayTraceResult;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.element.Element; // Import
import org.rostats.utils.FormulaParser;

import java.util.HashMap;
import java.util.Map;

public class DamageAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String formula;
    private final String element;
    private final boolean isBypassDef;

    public DamageAction(ThaiRoCorePlugin plugin, String formula, String element, boolean isBypassDef) {
        this.plugin = plugin;
        this.formula = formula;
        this.element = element;
        this.isBypassDef = isBypassDef;
    }

    @Override
    public ActionType getType() {
        return ActionType.DAMAGE;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        if (target == null) {
            target = findTarget(caster, 10);
        }

        if (target == null) return;

        double damage = 0.0;
        try {
            damage = FormulaParser.eval(formula, caster, target, level, context, plugin);
        } catch (Exception e) {
            damage = 1.0;
        }

        if (damage <= 0) return;

        // --- Element Logic ---
        double elementMod = 1.0;
        if (!isBypassDef) { // True Damage usually bypasses element too, standard logic
            Element skillElement = Element.fromName(element);
            Element targetDefElement = plugin.getElementManager().getDefenseElement(target);
            elementMod = plugin.getElementManager().getModifier(skillElement, targetDefElement);
            damage *= elementMod;
        }

        if (isBypassDef) {
            double newHealth = Math.max(0, target.getHealth() - damage);
            target.setHealth(newHealth);
            plugin.showTrueDamageFCT(target.getLocation(), damage);
        } else {
            // Apply Damage
            target.damage(damage, caster);

            // FCT Color based on Element
            String color = "§f";
            if (elementMod > 1.0) color = "§c";
            else if (elementMod < 1.0) color = "§7";

            // Note: CombatHandler will also show damage from the event.
            // Since target.damage() triggers EntityDamageByEntityEvent, CombatHandler handles the final FCT.
            // But for Skill Damage specifically calculated here, we might want to suppress CombatHandler's FCT
            // or rely on it. Since formula is arbitrary, CombatHandler might recalculate based on stats again
            // which duplicates logic.
            // Assuming this DamageAction is RAW damage that bypasses standard auto-attack formula but goes through armor:
            // The cleanest way is to let the Event handle mitigation, but we injected Element Mod here.
            // If CombatHandler re-applies element mod, it double dips.
            // FIX: CombatHandler mostly handles Left-Click (Physical).
            // For Skill Damage via API `damage()`, CombatHandler sees it.
            // We need a way to tell CombatHandler "This is a Skill, don't apply auto-attack formulas, just defense".
            // That requires metadata. For now, let's keep it simple:
            // This multiplier applies to the BASE damage sent to the event.
        }
    }

    private LivingEntity findTarget(LivingEntity caster, int range) {
        RayTraceResult result = caster.getWorld().rayTraceEntities(
                caster.getEyeLocation(),
                caster.getEyeLocation().getDirection(),
                range,
                0.5,
                e -> e instanceof LivingEntity && !e.equals(caster)
        );

        if (result != null && result.getHitEntity() instanceof LivingEntity) {
            return (LivingEntity) result.getHitEntity();
        }
        return null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "DAMAGE");
        map.put("formula", formula);
        map.put("element", element);
        map.put("bypass-def", isBypassDef);
        return map;
    }
}