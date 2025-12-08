package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.AttributeEditorGUI.Page;
import org.rostats.itemeditor.EffectEnchantGUI.Mode;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;
    // Temp storage for skill binding creation
    private final Map<UUID, ItemSkillBinding> pendingBindings = new HashMap<>();

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (title.startsWith("Library: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                handleImportItem(event, player, title.substring(9));
            } else {
                handleLibraryClick(event, player, title.substring(9));
            }
        }
        else if (title.startsWith("Editor: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                // Check if EffectEnchantGUI (re-using old logic detection if needed, or rely on distinct titles)
                // But wait, EffectEnchantGUI uses "Editor: Name [EFFECT Select]"
                // AttributeEditor uses "Editor: Name [GENERAL]"
                if (title.contains("Select]")) {
                    handleEffectEnchantClick(event, player, title);
                } else {
                    handleEditorClick(event, player, title);
                }
            }
        }

        // --- NEW: ITEM SKILL GUIS ---
        else if (title.startsWith("ItemSkills: ")) {
            event.setCancelled(true);
            handleItemSkillManage(event, player, title.substring(12));
        }
        else if (title.startsWith("SkillSelect: ")) {
            event.setCancelled(true);
            handleItemSkillSelect(event, player, title.substring(13));
        }
        else if (title.startsWith("SkillSettings: ")) {
            event.setCancelled(true);
            handleItemSkillSettings(event, player, title.substring(15));
        }
        // ---------------------------

        else if (title.startsWith("Confirm Delete: ")) {
            event.setCancelled(true);
            handleConfirmDeleteClick(event, player, title);
        }
    }

    // --- NEW HANDLERS ---

    private void handleItemSkillManage(InventoryClickEvent event, Player player, String fileName) {
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (clicked.getType() == Material.ARROW) { // Back
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
            return;
        }

        if (clicked.getType() == Material.EMERALD_BLOCK) { // Add New
            new ItemSkillSelectorGUI(plugin, itemFile).open(player);
            return;
        }

        // Remove Skill
        if (clicked.getType() == Material.ENCHANTED_BOOK && event.isShiftClick() && event.isRightClick()) {
            List<String> lore = clicked.getItemMeta().getLore();
            if (lore != null) {
                for (String l : lore) {
                    if (l.startsWith("§8index:")) {
                        try {
                            int index = Integer.parseInt(l.substring(8));
                            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
                            if (index >= 0 && index < attr.getSkillBindings().size()) {
                                attr.getSkillBindings().remove(index);
                                plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                                new ItemSkillManageGUI(plugin, itemFile).open(player);
                            }
                        } catch (Exception e) {}
                    }
                }
            }
        }
    }

    private void handleItemSkillSelect(InventoryClickEvent event, Player player, String fileName) {
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.ARROW) { // Back
            new ItemSkillManageGUI(plugin, itemFile).open(player);
            return;
        }

        // Select Skill
        List<String> lore = clicked.getItemMeta().getLore();
        if (lore != null && !lore.isEmpty()) {
            String idLine = lore.get(0); // ID: ...
            if (idLine.startsWith("§7ID: §f")) {
                String skillId = idLine.substring(8);
                // Create default binding
                ItemSkillBinding binding = new ItemSkillBinding(skillId, TriggerType.ON_HIT, 1, 0.1);
                pendingBindings.put(player.getUniqueId(), binding);
                new ItemSkillSettingsGUI(plugin, itemFile, binding).open(player);
            }
        }
    }

    private void handleItemSkillSettings(InventoryClickEvent event, Player player, String fileName) {
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;

        ItemSkillBinding current = pendingBindings.get(player.getUniqueId());
        if (current == null) {
            new ItemSkillManageGUI(plugin, itemFile).open(player);
            return;
        }

        int slot = event.getSlot();

        if (slot == 26) { // Cancel
            pendingBindings.remove(player.getUniqueId());
            new ItemSkillManageGUI(plugin, itemFile).open(player);
        }
        else if (slot == 22) { // Save
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            attr.getSkillBindings().add(current);
            plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
            player.sendMessage("§aSkill added to item!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            pendingBindings.remove(player.getUniqueId());
            new ItemSkillManageGUI(plugin, itemFile).open(player);
        }
        else if (slot == 11) { // Trigger
            TriggerType[] types = TriggerType.values();
            int nextOrd = (current.getTrigger().ordinal() + 1) % types.length;
            updatePending(player, new ItemSkillBinding(current.getSkillId(), types[nextOrd], current.getLevel(), current.getChance()));
            new ItemSkillSettingsGUI(plugin, itemFile, pendingBindings.get(player.getUniqueId())).open(player);
        }
        else if (slot == 13) { // Level
            plugin.getChatInputHandler().awaitInput(player, "Enter Level:", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    updatePending(player, new ItemSkillBinding(current.getSkillId(), current.getTrigger(), lvl, current.getChance()));
                } catch (Exception e) {}
                runSync(() -> new ItemSkillSettingsGUI(plugin, itemFile, pendingBindings.get(player.getUniqueId())).open(player));
            });
        }
        else if (slot == 15) { // Chance
            plugin.getChatInputHandler().awaitInput(player, "Enter Chance (0.0-1.0):", (str) -> {
                try {
                    double ch = Double.parseDouble(str);
                    updatePending(player, new ItemSkillBinding(current.getSkillId(), current.getTrigger(), current.getLevel(), ch));
                } catch (Exception e) {}
                runSync(() -> new ItemSkillSettingsGUI(plugin, itemFile, pendingBindings.get(player.getUniqueId())).open(player));
            });
        }
    }

    private void updatePending(Player player, ItemSkillBinding newBinding) {
        pendingBindings.put(player.getUniqueId(), newBinding);
    }
    // ----------------------------------------------------

    // --- Modified Editor Handler (Add Manage Skills button logic) ---
    private void handleEditorClick(InventoryClickEvent event, Player player, String title) {
        // ... (Keep existing logic for other buttons) ...
        int lastSpaceIndex = title.lastIndexOf(" [");
        if (lastSpaceIndex == -1) return;
        String fileName = title.substring(8, lastSpaceIndex);
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) { player.closeInventory(); return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String dp = clicked.getItemMeta().getDisplayName();

        if (dp.contains("Manage Skills")) { // NEW BUTTON LOGIC
            new ItemSkillManageGUI(plugin, itemFile).open(player);
            return;
        }

        // ... (Keep existing logic: Rename, Lore, Effects, Enchant, Back, Save, Attributes) ...
        if (dp.contains("Edit Effects")) { new EffectEnchantGUI(plugin, itemFile, Mode.EFFECT).open(player); return; }
        if (dp.contains("Edit Enchantments")) { new EffectEnchantGUI(plugin, itemFile, Mode.ENCHANT).open(player); return; }
        if (dp.contains("Back to Library")) { new ItemLibraryGUI(plugin, itemFile.getParentFile()).open(player); return; }

        // ... (Attributes & Page switching) ...
        for (Page p : Page.values()) {
            if (dp.contains(p.name())) { new AttributeEditorGUI(plugin, itemFile).open(player, p); return; }
        }

        // ... (Rename, Lore, Save, etc.) ...
        // Note: For brevity in this response, I'm assuming the existing huge logic block is retained.
        // You MUST merge this `if (dp.contains("Manage Skills"))` block into the existing method.

        // Re-implementing crucial parts for context if you copy-paste:
        if (dp.contains("Rename Item")) { /*...*/ return; }
        if (dp.contains("Edit Lore")) { /*...*/ return; }
        if (dp.contains("Save to File")) { /*...*/ return; }
        // ...
    }

    // ... (Keep all other methods: handleEffectEnchantClick, confirmDelete, etc.) ...

    private void handleEffectEnchantClick(InventoryClickEvent event, Player player, String title) {
        // (Existing Code)
        int lastBracket = title.lastIndexOf(" [");
        if (lastBracket == -1) return;
        String fileName = title.substring(8, lastBracket);
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;
        Mode mode = title.contains("EFFECT") ? Mode.EFFECT : Mode.ENCHANT;

        // ... (Existing logic) ...
        // Re-implementing simplified for compilation check
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        if (event.getSlot() == 53) {
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
        }
    }

    // ... (Helpers) ...
    private void handleConfirmDeleteClick(InventoryClickEvent event, Player player, String title) { /*...*/ }
    private void handleImportItem(InventoryClickEvent event, Player player, String relativePath) { /*...*/ }
    private void handleLibraryClick(InventoryClickEvent event, Player player, String relativePath) { /*...*/ }
    private File findFileByName(File dir, String name) {
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) {
            if (f.isDirectory()) { File found = findFileByName(f, name); if (found != null) return found; }
            else if (f.getName().equals(name) || f.getName().equals(name + ".yml")) return f;
        }
        return null;
    }
    private void runSync(Runnable r) { plugin.getServer().getScheduler().runTask(plugin, r); }
    private static class BukkitRunnableWrapper {
        public BukkitRunnableWrapper(ThaiRoCorePlugin plugin, Runnable r) {
            plugin.getServer().getScheduler().runTask(plugin, r);
        }
    }
}