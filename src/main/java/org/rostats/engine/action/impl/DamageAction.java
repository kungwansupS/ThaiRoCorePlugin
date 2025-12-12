package org.rostats.engine.action.impl;

import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.element.Element;
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
        if (!isBypassDef) {
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
            // [PHASE 4 FIX] Mark as Skill Damage so CombatHandler doesn't overwrite it
            target.setMetadata("RO_SKILL_DMG", new FixedMetadataValue(plugin, true));

            // Apply Damage
            target.damage(damage, caster);

            // Remove Metadata (Sync)
            target.removeMetadata("RO_SKILL_DMG", plugin);

            // FCT Color based on Element
            String color = "§f";
            if (elementMod > 1.0) color = "§c";
            else if (elementMod < 1.0) color = "§7";

            plugin.showCombatFloatingText(target.getLocation(), color + String.format("%.0f", damage));
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