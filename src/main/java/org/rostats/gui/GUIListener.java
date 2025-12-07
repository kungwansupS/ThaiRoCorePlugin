package org.rostats.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;
import org.rostats.engine.action.impl.DamageAction;
import org.rostats.engine.action.impl.EffectAction;
import org.rostats.engine.action.impl.HealAction;
import org.rostats.engine.effect.EffectType;
import org.rostats.engine.skill.SkillData;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.gui.CharacterGUI.Tab;
import org.rostats.itemeditor.AttributeEditorGUI;
import org.rostats.itemeditor.EffectEnchantGUI;
import org.rostats.itemeditor.ItemAttribute;
import org.rostats.itemeditor.ItemAttributeType;

import java.io.File;
import java.util.List;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // ... (Keep existing openGUI, onClose) ...
    private void openGUI(Player player, Tab tab) {
        player.setMetadata("ROSTATS_SWITCH", new FixedMetadataValue(plugin, true));
        new CharacterGUI(plugin).open(player, tab);
    }
    @EventHandler
    public void onClose(InventoryCloseEvent event) { /* Keep Existing Code */ }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // ... (Keep SkillLib & SkillDelete logic) ...
        if (title.startsWith("SkillLib: ")) {
            event.setCancelled(true);
            handleSkillLibraryClick(event, player, title.substring(10));
            return;
        }
        if (title.startsWith("Skill Delete: ")) {
            event.setCancelled(true);
            handleSkillDeleteClick(event, player, title);
            return;
        }

        // --- SKILL EDITOR LOGIC ---
        if (title.startsWith("SkillEditor: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            String skillId = title.substring(13);
            handleSkillEditorClick(event, player, skillId);
            return;
        }

        // --- NEW: ACTION SELECTOR LOGIC ---
        if (title.startsWith("ActionSelector: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            String skillId = title.substring(16);
            handleActionSelectorClick(event, player, skillId);
            return;
        }

        // ... (Keep remaining existing checks for Editor:, Character Status, Confirm Delete) ...
        if (title.startsWith("Editor: ")) {
            if (title.contains("EFFECT Select]") || title.contains("ENCHANT Select]")) {
                handleEffectEnchantClick(event, player, title);
                return;
            }
            handleEditorClick(event, player, title);
            return;
        }
        if (title.contains("Character Status (ROO)")) {
            handleCharacterStatusClick(event, player, title);
            return;
        }
        if (title.startsWith("Confirm Delete: ")) {
            event.setCancelled(true);
            handleConfirmDeleteClick(event, player, title);
        }
    }

    // --- NEW: Action Selector Handler ---
    private void handleActionSelectorClick(InventoryClickEvent event, Player player, String skillId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.ARROW) { // Back
            new SkillEditorGUI(plugin, skillId).open(player);
            return;
        }

        // Parse Action Type from Lore or Name? Name has "DAMAGE / ..."
        // Let's use lore hidden tag if possible, or string parsing.
        // Simplest: Check lore last line "ActionType: X" which I added in GUI
        List<String> lore = clicked.getItemMeta().getLore();
        if (lore != null && !lore.isEmpty()) {
            String last = lore.get(lore.size() - 1);
            if (last.startsWith("ActionType: ")) {
                String typeStr = last.substring(12);
                try {
                    ActionType type = ActionType.valueOf(typeStr);
                    SkillData skill = plugin.getSkillManager().getSkill(skillId);

                    // Add Default Action
                    SkillAction action = null;
                    switch (type) {
                        case DAMAGE: action = new DamageAction(plugin, "ATK * 1.0", "NEUTRAL"); break;
                        case HEAL: action = new HealAction(plugin, "10", false); break;
                        case APPLY_EFFECT: action = new EffectAction(plugin, "unknown", EffectType.STAT_MODIFIER, 1, 10, 100, 1.0, "STR"); break;
                        // For others, we can add placeholders or null
                    }

                    if (action != null) {
                        skill.addAction(action);
                        player.sendMessage("§aAdded action: " + type);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                        // Auto Save? Let's rely on manual save button for now, or save now.
                        // Saving now is safer for "Part 2" completeness.
                        plugin.getSkillManager().saveSkill(skill);
                    } else {
                        player.sendMessage("§cThis action type is not fully implemented yet.");
                    }

                    // Go back to Editor
                    new SkillEditorGUI(plugin, skillId).open(player);

                } catch (Exception e) {
                    player.sendMessage("§cError creating action.");
                }
            }
        }
    }

    private void handleSkillEditorClick(InventoryClickEvent event, Player player, String skillId) {
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) { player.closeInventory(); return; }
        int slot = event.getSlot();

        // 49: Save
        if (slot == 49) {
            plugin.getSkillManager().saveSkill(skill);
            player.sendMessage("§aSkill saved to file!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        }
        // 50: Add Action
        else if (slot == 50) {
            new SkillActionSelectorGUI(plugin, skillId).open(player);
        }
        // Action Slots (18-44) - Remove Logic
        else if (slot >= 18 && slot <= 44) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                if (event.isShiftClick() && event.isRightClick()) {
                    // Remove Action
                    // How to identify index? In Lore "Index: X"
                    List<String> lore = item.getItemMeta().getLore();
                    if (lore != null) {
                        for (String l : lore) {
                            if (l.startsWith("§7Index: ")) {
                                try {
                                    int index = Integer.parseInt(l.substring(9));
                                    if (index >= 0 && index < skill.getActions().size()) {
                                        skill.getActions().remove(index);
                                        plugin.getSkillManager().saveSkill(skill); // Auto save on modification
                                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                                        new SkillEditorGUI(plugin, skillId).open(player); // Refresh
                                    }
                                } catch (Exception e) {}
                            }
                        }
                    }
                }
            }
        }
        // ... (Keep existing rename/trigger/icon/cooldown logic from previous Phase) ...
        else if (slot == 0) { /* Rename Logic */
            plugin.getChatInputHandler().awaitInput(player, "Enter name:", (str) -> {
                skill.setDisplayName(str.replace("&", "§"));
                runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
        }
        else if (slot == 2) { /* Trigger Logic */
            TriggerType[] types = TriggerType.values();
            int currentOrd = skill.getTrigger().ordinal();
            int nextOrd = (currentOrd + 1) % types.length;
            skill.setTrigger(types[nextOrd]);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            new SkillEditorGUI(plugin, skillId).open(player);
        }
        else if (slot == 4) { /* Icon Logic */
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                skill.setIcon(cursor.getType());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.setCursor(cursor);
                new SkillEditorGUI(plugin, skillId).open(player);
            }
        }
        else if (slot == 6) { /* Cooldown */
            if (event.isLeftClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Cooldown:", (str) -> {
                    try { skill.setCooldownBase(Double.parseDouble(str)); } catch (Exception e) {}
                    runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
                });
            } else {
                plugin.getChatInputHandler().awaitInput(player, "CastTime:", (str) -> {
                    try { skill.setCastTime(Double.parseDouble(str)); } catch (Exception e) {}
                    runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
                });
            }
        }
        else if (slot == 8) { /* Cost */
            plugin.getChatInputHandler().awaitInput(player, "SP Cost:", (str) -> {
                try { skill.setSpCostBase(Integer.parseInt(str)); } catch (Exception e) {}
                runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
        }
        else if (slot == 53) { /* Back */
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
        }
    }

    // ... (Keep ALL other methods: handleSkillLibraryClick, handleSkillDeleteClick, runSync, findFileByName, etc.) ...
    // Note: Due to size limits, assume the rest of the file is identical to the previous Phase output.
    // You MUST retain `handleSkillLibraryClick`, `handleSkillDeleteClick`, `runSync` etc.

    private void handleSkillLibraryClick(InventoryClickEvent event, Player player, String relativePath) {
        // ... (Code from Phase 1 GUI)
        // Re-implementing briefly to ensure context:
        File currentDir = plugin.getSkillManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getSkillManager().getRootDir();
        final File finalDir = currentDir;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        String name = clicked.getItemMeta().getDisplayName().replace("§6§l", "").replace("§f", "");

        if (clicked.getType() == Material.ARROW) { new SkillLibraryGUI(plugin, currentDir.getParentFile()).open(player); return; }
        if (clicked.getType() == Material.BOOKSHELF) { new SkillLibraryGUI(plugin, currentDir).open(player); return; }
        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("New Folder")) {
            plugin.getChatInputHandler().awaitInput(player, "Folder:", (str) -> {
                plugin.getSkillManager().createFolder(finalDir, str);
                runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player));
            }); return;
        }
        if (clicked.getType() == Material.WRITABLE_BOOK) {
            plugin.getChatInputHandler().awaitInput(player, "Skill ID:", (str) -> {
                plugin.getSkillManager().createSkill(finalDir, str);
                runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player));
            }); return;
        }
        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        if (target.isDirectory()) {
            if (event.isLeftClick() && !event.isShiftClick()) new SkillLibraryGUI(plugin, target).open(player);
            else if (event.isShiftClick() && event.isLeftClick()) new SkillLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
        } else {
            if (event.isLeftClick()) new SkillEditorGUI(plugin, name).open(player);
            else if (event.isShiftClick()) new SkillLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
        }
    }

    private void handleSkillDeleteClick(InventoryClickEvent event, Player player, String title) {
        // ... (Code from Phase 1 GUI)
        String fileName = title.substring("Skill Delete: ".length());
        File target = findFileRecursive(plugin.getSkillManager().getRootDir(), fileName);
        if (event.getCurrentItem().getType() == Material.LIME_CONCRETE && target != null) {
            plugin.getSkillManager().deleteFile(target);
            player.sendMessage("§cDeleted.");
            runSync(() -> new SkillLibraryGUI(plugin, target.getParentFile()).open(player));
        } else if (event.getCurrentItem().getType() == Material.RED_CONCRETE) {
            player.closeInventory();
        }
    }

    // Placeholder helpers
    private File findFileRecursive(File dir, String name) { /* ... */ return null; } // Use previous implementation
    private void runSync(Runnable r) { plugin.getServer().getScheduler().runTask(plugin, r); }

    // Existing Item Editor methods (placeholders)
    private void handleEditorClick(InventoryClickEvent event, Player player, String title) {}
    private void handleEffectEnchantClick(InventoryClickEvent event, Player player, String title) {}
    private void handleConfirmDeleteClick(InventoryClickEvent event, Player player, String title) {}
    private void handleImportItem(InventoryClickEvent event, Player player, String relativePath) {}
    private void handleLibraryClick(InventoryClickEvent event, Player player, String relativePath) {}
    private void handleCharacterStatusClick(InventoryClickEvent event, Player player, String title) {}
    private File findFileByName(File dir, String name) { return null; }
    private void handleStatUpgrade(Player player, String statKey, boolean isLeftClick, boolean isRightClick) {}
    private void handleStatDowngrade(Player player, String statKey, boolean isLeftClick, boolean isRightClick) {}
    private void performReset(Player player) {}
    private String getStatKey(int slot, int startSlot) { return null; }
}