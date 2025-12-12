package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.skill.SkillManager;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.AttributeEditorGUI.Page;
import org.rostats.itemeditor.EffectEnchantGUI.Mode;
// import org.rostats.gui.SkillLibraryGUI; // Removed - replaced by ItemSkillSelectGUI

import java.io.File;
import java.io.IOException; // [FIX] Added import for IOException
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, Map<String, Object>> skillBindingFlow = new HashMap<>();

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    private void setSwitching(Player player) {
        player.setMetadata("RO_EDITOR_SWITCH", new FixedMetadataValue(plugin, true));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // [FIX Phase 2] Check for Switch Flag to preserve state during navigation
        if (player.hasMetadata("RO_EDITOR_SWITCH")) {
            player.removeMetadata("RO_EDITOR_SWITCH", plugin);
            return;
        }

        // [FIX Phase 2] Cleanup Metadata and Temporary Data on real close
        if (player.hasMetadata("RO_EDITOR_SEL_EFFECT")) player.removeMetadata("RO_EDITOR_SEL_EFFECT", plugin);
        if (player.hasMetadata("RO_EDITOR_SEL_ENCHANT")) player.removeMetadata("RO_EDITOR_SEL_ENCHANT", plugin);
        skillBindingFlow.remove(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 1. Library Handling
        if (title.startsWith("Library: ")) {
            event.setCancelled(true);
            String relativePath = title.substring(9);
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                handleImportItem(event, player, relativePath);
            } else {
                handleLibraryClick(event, player, relativePath);
            }
        }
        // 2. Editor Handling
        else if (title.startsWith("Editor: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;

            if (title.contains("[SKILLS]")) {
                handleSkillBindingClick(event, player, title);
                return;
            }
            if (title.contains("EFFECT Select]") || title.contains("ENCHANT Select]")) {
                handleEffectEnchantClick(event, player, title);
                return;
            }
            handleEditorClick(event, player, title);
        }
        // 3. Confirm Dialog
        else if (title.startsWith("Confirm Delete: ")) {
            event.setCancelled(true);
            handleConfirmDeleteClick(event, player, title);
        }
        // 4. Selectors
        else if (title.startsWith("Select Trigger: ")) {
            event.setCancelled(true);
            handleTriggerSelectClick(event, player, title.substring(16));
        }
        else if (title.startsWith("Material Select: ")) {
            event.setCancelled(true);
            handleMaterialSelectClick(event, player, title.substring(17));
        }
        // [NEW] 5. Item Skill Selector
        else if (title.startsWith("ItemSkillSelect:")) {
            event.setCancelled(true);
            handleItemSkillSelectClick(event, player, title);
        }
    }

    // --- Library Logic ---
    private void handleLibraryClick(InventoryClickEvent event, Player player, String relativePath) {
        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getItemManager().getRootDir();
        final File finalCurrentDir = currentDir;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        String fileName = null;
        if (clicked.hasItemMeta()) {
            NamespacedKey key = new NamespacedKey(plugin, "filename");
            if (clicked.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                fileName = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
            }
        }

        if (fileName != null) {
            if (fileName.equals("back")) {
                setSwitching(player);
                new ItemLibraryGUI(plugin, currentDir.getParentFile()).open(player);
                return;
            }
            if (fileName.equals("root")) {
                setSwitching(player);
                new ItemLibraryGUI(plugin, plugin.getItemManager().getRootDir()).open(player);
                return;
            }
            if (fileName.equals("new_folder")) {
                setSwitching(player);
                plugin.getChatInputHandler().awaitInput(player, "Folder Name:", (str) -> {
                    plugin.getItemManager().createFolder(finalCurrentDir, str);
                    new BukkitRunnableWrapper(plugin, () -> {
                        setSwitching(player);
                        new ItemLibraryGUI(plugin, finalCurrentDir).open(player);
                    });
                });
                return;
            }
            if (fileName.equals("new_item")) {
                setSwitching(player);
                plugin.getChatInputHandler().awaitInput(player, "Item Name:", (str) -> {
                    plugin.getItemManager().createItem(finalCurrentDir, str, Material.STONE);
                    new BukkitRunnableWrapper(plugin, () -> {
                        setSwitching(player);
                        new ItemLibraryGUI(plugin, finalCurrentDir).open(player);
                    });
                });
                return;
            }
        }

        if (fileName == null) return;

        File target = new File(currentDir, fileName);
        final File finalTarget = target;

        if (target.isDirectory()) {
            if (event.getClick().isLeftClick() && !event.isShiftClick()) {
                setSwitching(player);
                new ItemLibraryGUI(plugin, target).open(player);
            } else if (event.isShiftClick() && event.isLeftClick()) {
                setSwitching(player);
                new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            } else if (event.isShiftClick() && event.isRightClick()) {
                setSwitching(player);
                plugin.getChatInputHandler().awaitInput(player, "Rename:", (str) -> {
                    plugin.getItemManager().renameFile(finalTarget, str);
                    new BukkitRunnableWrapper(plugin, () -> {
                        setSwitching(player);
                        new ItemLibraryGUI(plugin, finalCurrentDir).open(player);
                    });
                });
            }
        } else {
            if (event.getClick() == ClickType.LEFT) {
                setSwitching(player);
                new AttributeEditorGUI(plugin, target).open(player, Page.GENERAL);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                ItemStack item = plugin.getItemManager().loadItemStack(target);
                player.getInventory().addItem(item);
                player.sendMessage("§aItem given!");
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                setSwitching(player);
                new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            }
        }
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, String title) {
        int lastSpaceIndex = title.lastIndexOf(" [");
        if (lastSpaceIndex == -1) return;
        String fileName = title.substring(8, lastSpaceIndex);

        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null || !itemFile.exists()) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String dp = clicked.getItemMeta().getDisplayName();

        if (dp.contains("Back to Library")) {
            setSwitching(player);
            new ItemLibraryGUI(plugin, itemFile.getParentFile()).open(player);
            return;
        }
        for (Page p : Page.values()) {
            if (dp.contains(p.name())) {
                setSwitching(player);
                new AttributeEditorGUI(plugin, itemFile).open(player, p);
                return;
            }
        }

        if (dp.contains("Change Type")) {
            setSwitching(player);
            new ItemTypeSelectorGUI(plugin, itemFile).open(player);
            return;
        }
        if (dp.contains("Edit Effects")) {
            setSwitching(player);
            new EffectEnchantGUI(plugin, itemFile, Mode.EFFECT).open(player);
            return;
        }
        if (dp.contains("Edit Enchantments")) {
            setSwitching(player);
            new EffectEnchantGUI(plugin, itemFile, Mode.ENCHANT).open(player);
            return;
        }
        if (dp.contains("Edit Skills")) {
            setSwitching(player);
            new SkillBindingGUI(plugin, itemFile).open(player);
            return;
        }
        if (dp.contains("Rename Item")) {
            setSwitching(player);
            plugin.getChatInputHandler().awaitInput(player, "New Name:", (str) -> {
                ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) meta.setDisplayName(str);
                stack.setItemMeta(meta);
                saveAndRefresh(player, itemFile, stack);
            });
            return;
        }
        if (dp.contains("Edit Lore")) {
            setSwitching(player);
            plugin.getChatInputHandler().awaitMultiLineInput(player, "Edit Lore:", (lines) -> {
                ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) meta.setLore(lines);
                stack.setItemMeta(meta);
                saveAndRefresh(player, itemFile, stack);
            });
            return;
        }
        if (dp.contains("Remove Vanilla")) {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            attr.setRemoveVanillaAttribute(!attr.isRemoveVanillaAttribute());
            plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
            setSwitching(player);
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
            return;
        }

        if (dp.contains("Unbreakable")) {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            attr.setUnbreakable(!attr.isUnbreakable());
            ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);
            plugin.getItemAttributeManager().applyAttributesToItem(stack, attr);
            plugin.getItemManager().saveItem(itemFile, attr, stack);
            setSwitching(player);
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        if (dp.contains("Save to File")) {
            player.sendMessage("§aItem Saved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
            return;
        }

        // Stats
        for (ItemAttributeType type : ItemAttributeType.values()) {
            if (dp.equals(type.getDisplayName())) {
                ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
                double current = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(attr, type);
                double change = 0;
                if (event.getClick() == ClickType.LEFT) change = type.getClickStep();
                else if (event.getClick() == ClickType.RIGHT) change = -type.getClickStep();
                else if (event.getClick() == ClickType.SHIFT_LEFT) change = type.getRightClickStep();
                else if (event.getClick() == ClickType.SHIFT_RIGHT) change = -type.getRightClickStep();

                plugin.getItemAttributeManager().setAttributeToObj(attr, type, current + change);
                plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
                setSwitching(player);
                new AttributeEditorGUI(plugin, itemFile).open(player, getPageFromTitle(title));
                return;
            }
        }
    }

    private void handleImportItem(InventoryClickEvent event, Player player, String relativePath) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getItemManager().getRootDir();
        final File finalCurrentDir = currentDir;

        setSwitching(player);
        player.closeInventory();
        plugin.getChatInputHandler().awaitInput(player, "Item Name (No .yml):", (name) -> {
            String fileName = name.endsWith(".yml") ? name : name + ".yml";
            File newFile = new File(finalCurrentDir, fileName);
            if (newFile.exists()) {
                player.sendMessage("§cFile exists!");
                return;
            }
            ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
            plugin.getItemManager().saveItem(newFile, attr, item);
            player.sendMessage("§aImported!");
            new BukkitRunnableWrapper(plugin, () -> {
                setSwitching(player);
                new ItemLibraryGUI(plugin, finalCurrentDir).open(player);
            });
        });
    }

    // [NEW] Handler for ItemSkillSelectGUI interactions
    private void handleItemSkillSelectClick(InventoryClickEvent event, Player player, String title) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // Extract context (e.g., current path and page)
        String[] parts = title.substring("ItemSkillSelect: ".length()).split(" #P");
        String path = parts[0].trim();
        int page = parts.length > 1 ? Integer.parseInt(parts[1].substring(1)) : 0;

        SkillManager skillManager = plugin.getSkillManager();
        File rootDir = skillManager.getRootDir();
        File currentDir = skillManager.getFileFromRelative(path);
        if (!currentDir.exists()) currentDir = rootDir;

        // Helper to extract hidden data from Lore (Context data is always present on functional items)
        String skillId = getHiddenLore(clicked, "SKILL_ID:");
        String itemTemplateId = getHiddenLore(clicked, "ITEM_ID:");
        String bindingIndexStr = getHiddenLore(clicked, "INDEX:");
        int bindingIndex = bindingIndexStr != null && !bindingIndexStr.equals("-1") ? Integer.parseInt(bindingIndexStr) : -1;

        File itemFile = itemTemplateId != null ? findFileByName(plugin.getItemManager().getRootDir(), itemTemplateId) : null;

        // Navigation Logic
        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().startsWith("§aFolder: ")) {
            // Folder click (Navigate into folder)
            String folderName = clicked.getItemMeta().getDisplayName().substring("§aFolder: ".length());
            File nextDir = new File(currentDir, folderName);
            if (nextDir.exists() && nextDir.isDirectory()) {
                setSwitching(player);
                new ItemSkillSelectGUI(plugin, nextDir, 0, itemTemplateId, bindingIndex).open(player);
            }
            return;
        }

        if (event.getSlot() == 45) { // Back button (Up one level)
            File parentDir = currentDir.getParentFile();

            // [FIX] Replaced the erroneous rootDir.isAncestor(parentDir) call
            // with a robust canonical path check.
            boolean isWithinRoot = false;
            if (parentDir != null) {
                try {
                    String rootPath = rootDir.getCanonicalPath();
                    String parentPath = parentDir.getCanonicalPath();

                    // Check if parentDir is the rootDir itself or a direct child folder/file of the rootDir.
                    if (parentPath.equals(rootPath) || parentPath.startsWith(rootPath + File.separator)) {
                        isWithinRoot = true;
                    }
                } catch (IOException ignored) {
                    // Fallback to basic equals check if canonical path resolution fails.
                    isWithinRoot = parentDir.equals(rootDir);
                }
            }

            if (isWithinRoot) {
                setSwitching(player);
                new ItemSkillSelectGUI(plugin, parentDir, 0, itemTemplateId, bindingIndex).open(player);
            }
            return;
        }

        if (event.getSlot() == 48 && page > 0) { // Previous Page
            setSwitching(player);
            new ItemSkillSelectGUI(plugin, currentDir, page - 1, itemTemplateId, bindingIndex).open(player);
            return;
        }

        // Slot 50 is Next Page in ItemSkillSelectGUI (Check if it has the display name "Next Page")
        if (event.getSlot() == 50 && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("Next Page")) {
            setSwitching(player);
            new ItemSkillSelectGUI(plugin, currentDir, page + 1, itemTemplateId, bindingIndex).open(player);
            return;
        }

        // Slot 53 is Back to Skill Binding
        if (event.getSlot() == 53 && itemFile != null) {
            skillBindingFlow.remove(player.getUniqueId());
            setSwitching(player);
            new SkillBindingGUI(plugin, itemFile).open(player);
            return;
        }

        // If a Skill Item is clicked (Identified by non-null skillId hidden in lore)
        if (skillId != null && itemFile != null) {
            // Setup flow context for the subsequent TriggerSelectorGUI and Chat Input phase
            Map<String, Object> flowData = new HashMap<>();
            flowData.put("itemFile", itemFile);
            flowData.put("skillId", skillId); // Store selected skill ID
            flowData.put("bindingIndex", bindingIndex);
            skillBindingFlow.put(player.getUniqueId(), flowData);

            setSwitching(player);
            new TriggerSelectorGUI(plugin, skillId).open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }
    }

    private void handleTriggerSelectClick(InventoryClickEvent event, Player player, String skillId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        Map<String, Object> flowData = skillBindingFlow.get(player.getUniqueId());

        // Navigation back to skill selector
        if (clicked.getType() == Material.ARROW) {
            if (flowData != null && flowData.containsKey("itemFile")) {
                File itemFile = (File) flowData.get("itemFile");
                String itemTemplateId = itemFile.getName();
                int bindingIndex = (int) flowData.getOrDefault("bindingIndex", -1);

                // Get the current directory from ItemSkillSelectGUI
                // We rely on the navigation logic in ItemSkillSelectGUI to handle the path properly.
                // Since the flow data doesn't explicitly store the *last viewed folder* in ItemSkillSelectGUI,
                // we return to the root folder of skills.

                File rootDir = plugin.getSkillManager().getRootDir();

                setSwitching(player);
                // Open ItemSkillSelectGUI to the root directory for simplicity.
                new ItemSkillSelectGUI(plugin, rootDir, 0, itemTemplateId, bindingIndex).open(player);
            } else {
                player.sendMessage("§cBinding flow context lost. Returning to main editor.");
                setSwitching(player);
                player.closeInventory();
            }
            return;
        }

        List<String> lore = clicked.getItemMeta().getLore();
        TriggerType trigger = null;
        if (lore != null) {
            for (String l : lore) {
                if (l.startsWith("§7Type: §f")) {
                    try {
                        trigger = TriggerType.valueOf(l.substring(10));
                    } catch (Exception ignored) {}
                    break;
                }
            }
        }

        if (trigger != null) {
            if (flowData == null || !flowData.containsKey("itemFile") || !flowData.containsKey("skillId")) {
                player.sendMessage("§cBinding flow interrupted. Please try again.");
                return;
            }
            flowData.put("trigger", trigger);

            setSwitching(player);
            plugin.getChatInputHandler().awaitInput(player, "Enter Skill Level (ตัวเลข):", (lvlStr) -> {
                try {
                    int level = Integer.parseInt(lvlStr);
                    if (level < 1) level = 1;

                    int finalLevel = level;
                    setSwitching(player); // Protect flow for next input
                    plugin.getChatInputHandler().awaitInput(player, "Enter Chance (0.0 - 1.0):", (chanceStr) -> {
                        try {
                            double chance = Double.parseDouble(chanceStr);
                            if (chance < 0) chance = 0; if (chance > 1) chance = 1;

                            File itemFile = (File) flowData.get("itemFile");
                            String sId = (String) flowData.get("skillId");
                            TriggerType trig = (TriggerType) flowData.get("trigger");

                            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
                            ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);

                            attr.getSkillBindings().add(new ItemSkillBinding(sId, trig, finalLevel, chance));
                            plugin.getItemManager().saveItem(itemFile, attr, stack);

                            player.sendMessage("§aSkill bound successfully!");
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                            skillBindingFlow.remove(player.getUniqueId()); // Explicit remove on success
                            new BukkitRunnableWrapper(plugin, () -> {
                                setSwitching(player);
                                new SkillBindingGUI(plugin, itemFile).open(player);
                            });

                        } catch (Exception e) { player.sendMessage("§cInvalid Chance"); }
                    });
                } catch (Exception e) { player.sendMessage("§cInvalid Level"); }
            });
        }
    }

    private void handleSkillBindingClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring(8, title.lastIndexOf(" ["));
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.LIME_DYE) { // Add Skill Button
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            int bindingIndex = attr.getSkillBindings().size();
            String itemTemplateId = itemFile.getName();

            // Store the flow context for the multi-step operation
            Map<String, Object> flowData = new HashMap<>();
            flowData.put("itemFile", itemFile);
            flowData.put("itemTemplateId", itemTemplateId);
            flowData.put("bindingIndex", bindingIndex);
            skillBindingFlow.put(player.getUniqueId(), flowData);

            setSwitching(player);
            // Open the new ItemSkillSelectGUI
            new ItemSkillSelectGUI(plugin, plugin.getSkillManager().getRootDir(), 0, itemTemplateId, bindingIndex).open(player);
            return;
        } else if (clicked.getType() == Material.ARROW) {
            setSwitching(player);
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
        } else if (clicked.getType() == Material.ENCHANTED_BOOK) {
            if (event.isRightClick()) {
                ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
                List<ItemSkillBinding> bindings = attr.getSkillBindings();
                int slot = event.getSlot();
                if (slot < bindings.size()) {
                    bindings.remove(slot);
                    plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
                    player.sendMessage("§cRemoved skill binding.");
                    setSwitching(player);
                    new SkillBindingGUI(plugin, itemFile).open(player);
                }
            }
        }
    }

    private void handleEffectEnchantClick(InventoryClickEvent event, Player player, String title) {
        int lastBracket = title.lastIndexOf(" [");
        if (lastBracket == -1) return;
        String fileName = title.substring(8, lastBracket);
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;

        Mode mode = title.contains("EFFECT") ? Mode.EFFECT : Mode.ENCHANT;
        String metaKey = "RO_EDITOR_SEL_" + mode.name();
        String selected = player.hasMetadata(metaKey) ? player.getMetadata(metaKey).get(0).asString() : null;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        int slot = event.getSlot();

        if (slot < 45) {
            if (clicked.getType() == Material.AIR) return;
            String dp = clicked.getItemMeta().getDisplayName();
            String key = dp.length() > 2 ? dp.substring(2) : dp;

            player.setMetadata(metaKey, new FixedMetadataValue(plugin, key));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            setSwitching(player);
            new EffectEnchantGUI(plugin, itemFile, mode).open(player);
        }
        else if (slot == 50 && selected != null) {
            setSwitching(player); // Protect metadata from being cleared on close
            plugin.getChatInputHandler().awaitInput(player, "Enter Level:", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    applyEffectEnchant(player, itemFile, mode, selected, lvl, true);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid Number");
                    new BukkitRunnableWrapper(plugin, () -> {
                        setSwitching(player);
                        new EffectEnchantGUI(plugin, itemFile, mode).open(player);
                    });
                }
            });
        }
        else if (slot == 51 && selected != null) {
            applyEffectEnchant(player, itemFile, mode, selected, 1, true);
        }
        else if (slot == 52 && selected != null) {
            applyEffectEnchant(player, itemFile, mode, selected, 0, false);
        }
        else if (slot == 53) {
            player.removeMetadata(metaKey, plugin); // Clean up explicitly
            setSwitching(player);
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
        }
    }

    private void applyEffectEnchant(Player player, File file, Mode mode, String key, int level, boolean add) {
        new BukkitRunnableWrapper(plugin, () -> {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(file);
            ItemStack stack = plugin.getItemManager().loadItemStack(file);
            boolean changed = false;

            if (mode == Mode.EFFECT) {
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type != null) {
                    if (add) attr.getPotionEffects().put(type, level);
                    else attr.getPotionEffects().remove(type);
                    plugin.getItemManager().saveItem(file, attr, stack);
                    changed = true;
                }
            } else {
                Enchantment ench = null;
                for (Enchantment e : Enchantment.values()) {
                    if (e.getKey().getKey().equalsIgnoreCase(key)) { ench = e; break; }
                }
                if (ench != null) {
                    ItemMeta meta = stack.getItemMeta();
                    if (add) meta.addEnchant(ench, level, true);
                    else meta.removeEnchant(ench);
                    stack.setItemMeta(meta);
                    plugin.getItemManager().saveItem(file, attr, stack);
                    changed = true;
                }
            }

            if (changed) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                setSwitching(player);
                new EffectEnchantGUI(plugin, file, mode).open(player);
            }
        });
    }

    private void handleConfirmDeleteClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring("Confirm Delete: ".length());
        File target = findFileByName(plugin.getItemManager().getRootDir(), fileName);

        if (event.getCurrentItem() == null) return;
        String dp = event.getCurrentItem().getItemMeta().getDisplayName();

        if (dp.contains("CONFIRM DELETE")) {
            if (target != null && target.exists()) {
                File parent = target.getParentFile();
                plugin.getItemManager().deleteFile(target);
                player.sendMessage("§cDeleted: " + fileName);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                new BukkitRunnableWrapper(plugin, () -> {
                    setSwitching(player);
                    new ItemLibraryGUI(plugin, parent).open(player);
                });
            } else {
                player.sendMessage("§cFile not found.");
                player.closeInventory();
            }
        } else if (dp.contains("CANCEL")) {
            if (target != null && target.exists()) {
                new BukkitRunnableWrapper(plugin, () -> {
                    setSwitching(player);
                    new ItemLibraryGUI(plugin, target.getParentFile()).open(player);
                });
            } else {
                new BukkitRunnableWrapper(plugin, () -> {
                    setSwitching(player);
                    new ItemLibraryGUI(plugin, plugin.getItemManager().getRootDir()).open(player);
                });
            }
        }
    }

    private void handleMaterialSelectClick(InventoryClickEvent event, Player player, String fileName) {
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if(itemFile == null) return;
        ItemStack clicked = event.getCurrentItem();
        if(clicked == null) return;
        if(clicked.getType() == Material.ARROW) {
            setSwitching(player);
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
            return;
        }
        if(clicked.getType() != Material.AIR) {
            ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);
            stack.setType(clicked.getType());
            saveAndRefresh(player, itemFile, stack);
        }
    }

    private void saveAndRefresh(Player player, File file, ItemStack stack) {
        ItemAttribute attr = plugin.getItemManager().loadAttribute(file);
        plugin.getItemManager().saveItem(file, attr, stack);
        new BukkitRunnableWrapper(plugin, () -> {
            setSwitching(player);
            new AttributeEditorGUI(plugin, file).open(player, Page.GENERAL);
        });
    }

    private File findFileByName(File dir, String name) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    File found = findFileByName(f, name);
                    if (found != null) return found;
                } else if (f.getName().equals(name) || f.getName().equals(name + ".yml")) {
                    return f;
                }
            }
        }
        return null;
    }

    private Page getPageFromTitle(String title) {
        for (Page p : Page.values()) {
            if (title.contains(p.name())) return p;
        }
        return Page.GENERAL;
    }

    // [NEW] Helper method to extract hidden data from Item Lore
    private String getHiddenLore(ItemStack item, String prefix) {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasLore()) {
            for (String lore : item.getItemMeta().getLore()) {
                if (lore.startsWith("§0" + prefix)) {
                    return lore.substring(("§0" + prefix).length());
                }
            }
        }
        return null;
    }

    private static class BukkitRunnableWrapper {
        public BukkitRunnableWrapper(ThaiRoCorePlugin plugin, Runnable r) {
            plugin.getServer().getScheduler().runTask(plugin, r);
        }
    }
}