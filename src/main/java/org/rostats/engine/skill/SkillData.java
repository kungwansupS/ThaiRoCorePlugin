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

    // [NEW] Global Cooldown
    private double globalCooldownBase;
    private double globalCooldownPerLevel;

    private int spCostBase;
    private int spCostPerLevel;

    private double castTime;

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

    public double getGlobalCooldown(int level) {
        return Math.max(0, globalCooldownBase + (globalCooldownPerLevel * (level - 1)));
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

    // [NEW] Global Cooldown Getters/Setters
    public double getGlobalCooldownBase() { return globalCooldownBase; }
    public void setGlobalCooldownBase(double globalCooldownBase) { this.globalCooldownBase = globalCooldownBase; }

    public double getGlobalCooldownPerLevel() { return globalCooldownPerLevel; }
    public void setGlobalCooldownPerLevel(double globalCooldownPerLevel) { this.globalCooldownPerLevel = globalCooldownPerLevel; }

    public int getSpCostBase() { return spCostBase; }
    public void setSpCostBase(int spCostBase) { this.spCostBase = spCostBase; }

    public int getSpCostPerLevel() { return spCostPerLevel; }
    public void setSpCostPerLevel(int spCostPerLevel) { this.spCostPerLevel = spCostPerLevel; }

    public double getCastTime() { return castTime; }
    public void setCastTime(double castTime) { this.castTime = castTime; }

    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int requiredLevel) { this.requiredLevel = requiredLevel; }

    public List<SkillAction> getActions() { return actions; }
    public void addAction(SkillAction action) { this.actions.add(action); }
}