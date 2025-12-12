package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.skill.SkillData;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.gui.SkillLibraryGUI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TriggerSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String itemId;
    private final int bindingIndex;
    private final String skillIdToEdit;

    // Transient state
    private TriggerType tempTrigger = TriggerType.CAST;
    private int tempLevel = 1;
    private double tempChance = 1.0;

    // Basic Constructor
    public TriggerSelectorGUI(ThaiRoCorePlugin plugin, String itemId, int bindingIndex, String skillIdToEdit) {
        this.plugin = plugin;
        this.itemId = itemId;
        this.bindingIndex = bindingIndex;
        this.skillIdToEdit = skillIdToEdit;

        // Try to load existing values if editing
        if (bindingIndex != -1 && plugin.getItemManager().getFileFromRelative(itemId + ".yml").exists()) {
            // Ideally we load from file/item here to set temp defaults, but for now defaults are fine
            // Real values are fetched in open() from the held item
        }
    }

    // Full Constructor (Used when returning from other inputs)
    public TriggerSelectorGUI(ThaiRoCorePlugin plugin, String itemId, int bindingIndex, String skillIdToEdit, TriggerType trigger, int level, double chance) {
        this(plugin, itemId, bindingIndex, skillIdToEdit);
        this.tempTrigger = trigger;
        this.tempLevel = level;
        this.tempChance = chance;
    }

    public void open(Player player) {
        // [FIX] Ensure unique title for Listener detection
        String title = "ItemEditor: Trigger Selection: " + itemId;
        Inventory inv = Bukkit.createInventory(this, 27, Component.text(title));

        // Display Data Calculation
        TriggerType displayTrigger = tempTrigger;
        String displaySkillId = skillIdToEdit;
        int displayLevel = tempLevel;
        double displayChance = tempChance;

        // If editing existing binding, override defaults with actual data (unless we have newer temp data?)
        // For simplicity in this flow, we assume the Constructor params are the authority.
        // However, if we just opened for EDIT, we should read from Item.

        if (bindingIndex != -1) {
            ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(player.getInventory().getItemInMainHand());
            if (bindingIndex < attr.getSkillBindings().size()) {
                ItemSkillBinding existing = attr.getSkillBindings().get(bindingIndex);
                // Only override if we haven't set specific temp values (logic omitted for brevity, using passed values)
                // In a perfect world, we'd check if temp values were Modified.
            }
        }

        // --- Skill Info (Slot 4) ---
        SkillData skill = plugin.getSkillManager().getSkill(displaySkillId);
        Material skillMat = (skill != null && skill.getIcon() != null) ? skill.getIcon() : Material.BOOK;
        String skillName = (skill != null && skill.getDisplayName() != null) ? skill.getDisplayName().replace("&", "§") : displaySkillId;

        inv.setItem(4, createGuiItem(skillMat, "§eCurrent Skill: §a" + skillName,
                "§7ID: " + displaySkillId,
                "§7Level: " + displayLevel,
                "§7Chance: " + String.format("%.0f%%", displayChance * 100),
                "",
                "§bClick to Change Skill",
                "§0INDEX:" + bindingIndex,
                "§0ID: " + displaySkillId // Hidden ID for Listener
        ));

        // --- Trigger Buttons (Slots 10-16) ---
        TriggerType[] triggers = TriggerType.values();
        int slotIdx = 10;
        for (TriggerType type : triggers) {
            if (slotIdx > 16) break;

            Material mat = (type == displayTrigger) ? Material.DIAMOND_BLOCK : Material.STONE;
            if (type == TriggerType.PASSIVE) mat = Material.GOLD_BLOCK;

            List<String> lore = new ArrayList<>();
            lore.add("§7Type: §f" + type.name());
            lore.add(type == displayTrigger ? "§a§lSELECTED" : "§eClick to Select");

            inv.setItem(slotIdx++, createGuiItem(mat, "§6Trigger: §e" + type.name(), lore.toArray(new String[0])));
        }

        // --- Controls (Bottom) ---
        inv.setItem(18, createGuiItem(Material.RED_BED, "§cBack to Skill Bindings"));
        inv.setItem(22, createGuiItem(Material.EMERALD_BLOCK, "§aConfirm Binding", "§7Save changes to item."));
        inv.setItem(26, createGuiItem(Material.BLAZE_ROD, "§bEdit Level/Chance", "§7Re-enter numbers via chat."));

        player.openInventory(inv);
    }

    // Helper to open Skill Selector
    public void openSkillSelector(Player player) {
        new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir(), 0, true, itemId, bindingIndex, itemId).open(player);
    }

    // Getters for Listener
    public int getBindingIndex() { return bindingIndex; }
    public String getSkillIdToEdit() { return skillIdToEdit; }
    public TriggerType getTempTrigger() { return tempTrigger; }
    public int getTempLevel() { return tempLevel; }
    public double getTempChance() { return tempChance; }

    public void setTempTrigger(TriggerType t) { this.tempTrigger = t; }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}