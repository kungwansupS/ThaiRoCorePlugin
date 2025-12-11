package org.rostats.engine.action.impl;

import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.utils.FormulaParser;

import java.util.HashMap;
import java.util.Map;

public class HealAction implements SkillAction {

    private final ThaiRoCorePlugin plugin;
    private final String formula;
    private final boolean isMana;
    private final boolean isSelfOnly;

    public HealAction(ThaiRoCorePlugin plugin, String formula, boolean isMana, boolean isSelfOnly) {
        this.plugin = plugin;
        this.formula = formula;
        this.isMana = isMana;
        this.isSelfOnly = isSelfOnly;
    }

    @Override
    public ActionType getType() {
        return ActionType.HEAL;
    }

    @Override
    public void execute(LivingEntity caster, LivingEntity target, int level, Map<String, Double> context) {
        if (isSelfOnly || target == null) {
            target = caster;
        }

        double amount = 0.0;
        try {
            amount = FormulaParser.eval(formula, caster, target, level, context, plugin);
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
                target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5);
            }
        } else {
            double maxHP;
            if (target instanceof Player player) {
                maxHP = plugin.getStatManager().getData(player.getUniqueId()).getMaxHP();
            } else {
                maxHP = target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            }
            if (maxHP > 2048.0) maxHP = 2048.0;

            double newHP = Math.min(maxHP, target.getHealth() + amount);
            target.setHealth(newHP);
            plugin.showHealHPFCT(target.getLocation(), amount);
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "HEAL");
        map.put("formula", formula);
        map.put("is-mana", isMana);
        map.put("self-only", isSelfOnly);
        return map;
    }
}