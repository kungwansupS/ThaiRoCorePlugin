package org.rostats.engine.skill;

import org.bukkit.Material;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.trigger.TriggerType;

import java.util.ArrayList;
import java.util.List;

public class SkillData {

    private final String id;
    private String displayName;
    private Material icon;
    private int maxLevel;
    private TriggerType trigger;

    // Conditions / Costs
    private double cooldownBase;
    private double cooldownPerLevel;

    private int spCostBase;
    private int spCostPerLevel;

    // Cast Time (Variable & Fixed)
    private double variableCastTime;
    private double variableCastTimeReduction; // Skill specific reduction %
    private double fixedCastTime;
    private double fixedCastTimeReduction; // Skill specific reduction %

    // Motion & Global Cooldown
    private double preMotion;
    private double postMotion;

    // Required Level
    private int requiredLevel = 1;

    // Actions List (Sequence of logic)
    private final List<SkillAction> actions = new ArrayList<>();

    public SkillData(String id) {
        this.id = id;
    }

    // --- Calculation Methods ---

    public double getCooldown(int level) {
        return Math.max(0, cooldownBase + (cooldownPerLevel * (level - 1)));
    }

    public int getSpCost(int level) {
        return Math.max(0, spCostBase + (spCostPerLevel * (level - 1)));
    }

    // --- Getters & Setters ---

    public String getId() { return id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }

    public int getMaxLevel() { return maxLevel; }
    public void setMaxLevel(int maxLevel) { this.maxLevel = maxLevel; }

    public TriggerType getTrigger() { return trigger; }
    public void setTrigger(TriggerType trigger) { this.trigger = trigger; }

    public double getCooldownBase() { return cooldownBase; }
    public void setCooldownBase(double cooldownBase) { this.cooldownBase = cooldownBase; }

    public double getCooldownPerLevel() { return cooldownPerLevel; }
    public void setCooldownPerLevel(double cooldownPerLevel) { this.cooldownPerLevel = cooldownPerLevel; }

    public int getSpCostBase() { return spCostBase; }
    public void setSpCostBase(int spCostBase) { this.spCostBase = spCostBase; }

    public int getSpCostPerLevel() { return spCostPerLevel; }
    public void setSpCostPerLevel(int spCostPerLevel) { this.spCostPerLevel = spCostPerLevel; }

    // Cast Time Getters/Setters
    public double getVariableCastTime() { return variableCastTime; }
    public void setVariableCastTime(double variableCastTime) { this.variableCastTime = variableCastTime; }

    public double getVariableCastTimeReduction() { return variableCastTimeReduction; }
    public void setVariableCastTimeReduction(double variableCastTimeReduction) { this.variableCastTimeReduction = variableCastTimeReduction; }

    public double getFixedCastTime() { return fixedCastTime; }
    public void setFixedCastTime(double fixedCastTime) { this.fixedCastTime = fixedCastTime; }

    public double getFixedCastTimeReduction() { return fixedCastTimeReduction; }
    public void setFixedCastTimeReduction(double fixedCastTimeReduction) { this.fixedCastTimeReduction = fixedCastTimeReduction; }

    // Motion Getters/Setters
    public double getPreMotion() { return preMotion; }
    public void setPreMotion(double preMotion) { this.preMotion = preMotion; }

    public double getPostMotion() { return postMotion; }
    public void setPostMotion(double postMotion) { this.postMotion = postMotion; }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }

    public List<SkillAction> getActions() { return actions; }
    public void addAction(SkillAction action) { this.actions.add(action); }
}