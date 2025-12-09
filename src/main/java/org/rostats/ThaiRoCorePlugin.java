package org.rostats;

import org.bukkit.plugin.java.JavaPlugin;
import org.rostats.cmd.*;
import org.rostats.engine.effect.EffectManager;
import org.rostats.engine.skill.SkillManager;
import org.rostats.gui.GUIListener;
import org.rostats.handler.AttributeHandler;
import org.rostats.handler.ChatInputHandler;
import org.rostats.handler.CooldownDisplayManager;
import org.rostats.handler.ManaManager;
import org.rostats.manager.StatManager;

public class ThaiRoCorePlugin extends JavaPlugin {

    private StatManager statManager;
    private AttributeHandler attributeHandler;
    private SkillManager skillManager;
    private EffectManager effectManager;
    private ManaManager manaManager;
    private ChatInputHandler chatInputHandler;
    private CooldownDisplayManager cooldownDisplay;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize Managers
        this.statManager = new StatManager(this);
        this.attributeHandler = new AttributeHandler(this);
        this.skillManager = new SkillManager(this);
        this.effectManager = new EffectManager(this);
        this.manaManager = new ManaManager(this);
        this.chatInputHandler = new ChatInputHandler(this);

        // [NEW] Initialize Cooldown Display Manager
        this.cooldownDisplay = new CooldownDisplayManager(this);

        // Register Events
        getServer().getPluginManager().registerEvents(statManager, this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(manaManager, this);
        getServer().getPluginManager().registerEvents(chatInputHandler, this);

        // Register Commands
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("resetstats").setExecutor(new ResetStatsCommand(this));
        getCommand("addstat").setExecutor(new AddStatCommand(this));
        getCommand("skill").setExecutor(new SkillCommand(this));
        getCommand("skilleditor").setExecutor(new SkillEditorCommand(this));
        getCommand("castskill").setExecutor(new CastSkillCommand(this));
        getCommand("giveexp").setExecutor(new GiveExpCommand(this));
        getCommand("setlevel").setExecutor(new SetLevelCommand(this));
        getCommand("applyeffect").setExecutor(new ApplyEffectCommand(this));
        getCommand("cleareffects").setExecutor(new ClearEffectsCommand(this));
        getCommand("attributetest").setExecutor(new AttributeTestCommand(this));

        // [NEW] Register Cooldown Display Command
        getCommand("cooldown").setExecutor(new CooldownCommand(this));

        getLogger().info("ThaiRoCore Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown Cooldown Display Task
        if (cooldownDisplay != null) {
            cooldownDisplay.shutdown();
        }

        getLogger().info("ThaiRoCore Plugin Disabled!");
    }

    // Getters
    public StatManager getStatManager() { return statManager; }
    public AttributeHandler getAttributeHandler() { return attributeHandler; }
    public SkillManager getSkillManager() { return skillManager; }
    public EffectManager getEffectManager() { return effectManager; }
    public ManaManager getManaManager() { return manaManager; }
    public ChatInputHandler getChatInputHandler() { return chatInputHandler; }
    public CooldownDisplayManager getCooldownDisplay() { return cooldownDisplay; }
}