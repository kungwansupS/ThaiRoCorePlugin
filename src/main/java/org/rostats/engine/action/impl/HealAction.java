package org.rostats.engine.action.impl;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Particle;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

public class HealAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String formula;
    private final boolean isMana; // True = SP, False = HP

    public HealAction(ThaiRoCorePlugin plugin, String formula, boolean isMana) {
        this.plugin = plugin;
        this.formula = formula;
        this.isMana = isMana;
    }

    @Override
    public ActionType getType() {
        return ActionType.HEAL;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level) {
        if (target == null) target = caster; // Default to self heal if no target

        double amount = 0.0;
        try {
            String calcFormula = formula.replace("LVL", String.valueOf(level));
            if (caster instanceof Player) {
                amount = FormulaParser.eval(calcFormula, (Player) caster, plugin);
            } else {
                amount = 10 * level;
            }
        } catch (Exception e) {
            amount = 1.0;
        }

        if (amount <= 0) return;

        if (isMana) {
            if (target instanceof Player player) {
                PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
                double newSP = Math.min(data.getMaxSP(), data.getCurrentSP() + amount);
                data.setCurrentSP(newSP);
                plugin.getManaManager().updateBar(player);
                plugin.showHealSPFCT(player.getLocation(), amount);

                // NEW: Show Heart Particle for SP Heal (Blue Hearts)
                target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5);
            }
        } else {
            double maxHP = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double newHP = Math.min(maxHP, target.getHealth() + amount);
            target.setHealth(newHP);
            plugin.showHealHPFCT(target.getLocation(), amount);

            // NEW: Show Heart Particle for HP Heal
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5);
        }
    }
}