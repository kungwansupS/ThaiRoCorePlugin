package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder; // [FIX] Import InventoryHolder
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

// [FIX] Implement InventoryHolder เพื่อให้ส่ง 'this' เข้า Bukkit.createInventory ได้
public class TriggerSelectorGUI implements InventoryHolder {

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

        // Try to load existing values if editing (Logic omitted for brevity as discussed)
    }

    // Full Constructor (Used when returning from other inputs)
    public TriggerSelectorGUI(ThaiRoCorePlugin plugin, String itemId, int bindingIndex, String skillIdToEdit, TriggerType trigger, int level, double chance) {
        this(plugin, itemId, bindingIndex, skillIdToEdit);
        this.tempTrigger = trigger;
        this.tempLevel = level;
        this.tempChance = chance;
    }

    // [FIX] Required method for InventoryHolder
    @Override
    public Inventory getInventory() {
        return null; // ไม่จำเป็นต้องคืนค่า Inventory จริงๆ ก็ได้สำหรับการใช้งานนี้
    }

    public void open(Player player) {
        String title = "ItemEditor: Trigger Selection: " + itemId;
        // [FIX] ตอนนี้ 'this' เป็น InventoryHolder แล้ว
        Inventory inv = Bukkit.createInventory(this, 27, Component.text(title));

        // Display Data Calculation
        TriggerType displayTrigger = tempTrigger;
        String displaySkillId = skillIdToEdit;
        int displayLevel = tempLevel;
        double displayChance = tempChance;

        if (bindingIndex != -1) {
            ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(player.getInventory().getItemInMainHand());
            if (bindingIndex < attr.getSkillBindings().size()) {
                // Logic to load existing binding values if needed (currently using passed values)
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
                "§0ID: " + displaySkillId // Hidden ID for Listener fallback
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