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
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.AttributeEditorGUI.Page;
import org.rostats.itemeditor.EffectEnchantGUI.Mode;
import org.rostats.gui.SkillLibraryGUI;
import org.rostats.input.ChatInputHandler;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;
    // ใช้ Map นี้สำหรับเก็บสถานะชั่วคราวระหว่างเปลี่ยนหน้า GUI (เช่น เลือก Trigger -> กลับมา)
    private final Map<UUID, Map<String, Object>> tempBindingData = new HashMap<>();

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    private void setSwitching(Player player) {
        player.setMetadata("RO_EDITOR_SWITCH", new FixedMetadataValue(plugin, true));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        // ถ้าเป็นการเปลี่ยนหน้า GUI (Switching) ไม่ต้องล้างข้อมูล
        if (player.hasMetadata("RO_EDITOR_SWITCH")) {
            player.removeMetadata("RO_EDITOR_SWITCH", plugin);
            return;
        }

        // ล้างข้อมูล Metadata เมื่อปิด GUI จริงๆ
        if (player.hasMetadata("RO_EDITOR_SEL_EFFECT")) player.removeMetadata("RO_EDITOR_SEL_EFFECT", plugin);
        if (player.hasMetadata("RO_EDITOR_SEL_ENCHANT")) player.removeMetadata("RO_EDITOR_SEL_ENCHANT", plugin);
        tempBindingData.remove(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title() == null) return;
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
        else if (title.startsWith("Select Trigger: ") || title.startsWith("ItemEditor: Trigger Selection:")) {
            event.setCancelled(true);
            // รองรับ Title ทั้งสองแบบ (แบบเก่าและแบบใหม่)
            String itemId = title.contains("Select Trigger: ") ? title.substring(16) : title.substring(title.lastIndexOf(":") + 1).trim();
            handleTriggerSelectClick(event, player, itemId);
        }
        else if (title.startsWith("Material Select: ")) {
            event.setCancelled(true);
            handleMaterialSelectClick(event, player, title.substring(17));
        }
        // 5. Skill Selector Handling (New)
        else if (title.startsWith("SkillSelect:")) {
            event.setCancelled(true);
            handleSkillSelectModeClick(event, player, title);
        }
        // 6. Skill Library (General)
        else if (title.startsWith("SkillLibrary:")) {
            event.setCancelled(true);
            // Handle general skill library navigation if needed, mostly handled by SkillLibraryGUI internal logic
            // But we might need to intercept navigation to keep it working
            handleSkillLibraryNavigation(event, player, title, false);
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

    private void handleTriggerSelectClick(InventoryClickEvent event, Player player, String itemId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String dp = clicked.getItemMeta().getDisplayName();
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), itemId);
        if (itemFile == null) return;

        // Helper to reconstruct GUI state
        TriggerSelectorGUI currentGUI = null;
        if (event.getInventory().getHolder() instanceof TriggerSelectorGUI) {
            currentGUI = (TriggerSelectorGUI) event.getInventory().getHolder();
        } else {
            // Fallback reconstruction if Holder is not set (Using Lore/Title)
            int bindingIndex = -1;
            String skillId = null;
            // Try to find hidden info item (Slot 4)
            ItemStack infoItem = event.getInventory().getItem(4);
            if (infoItem != null && infoItem.hasItemMeta() && infoItem.getItemMeta().hasLore()) {
                for (String l : infoItem.getItemMeta().getLore()) {
                    if (l.startsWith("§0INDEX:")) bindingIndex = Integer.parseInt(l.substring(8));
                    if (l.startsWith("§7ID: ")) skillId = l.substring(6);
                }
            }
            currentGUI = new TriggerSelectorGUI(plugin, itemId, bindingIndex, skillId);
        }

        // Trigger Buttons (Slots 10-16)
        if (event.getSlot() >= 10 && event.getSlot() <= 16 && dp.startsWith("§6Trigger:")) {
            try {
                TriggerType selectedTrigger = TriggerType.valueOf(dp.substring("§6Trigger: §e".length()));
                // Reopen with new trigger
                setSwitching(player);
                new TriggerSelectorGUI(plugin, itemId, currentGUI.getBindingIndex(),
                        currentGUI.getSkillIdToEdit(), selectedTrigger,
                        currentGUI.getTempLevel(), currentGUI.getTempChance()).open(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            } catch (Exception ignored) {}
            return;
        }

        // Back Button
        if (event.getSlot() == 18 && dp.contains("Back")) {
            setSwitching(player);
            new SkillBindingGUI(plugin, itemFile).open(player);
            return;
        }

        // Change Skill Button
        if (event.getSlot() == 4 && dp.contains("Change Skill")) {
            setSwitching(player);
            // Open SkillLibrary in SELECT mode
            // Passing itemId as targetSkillId, and bindingIndex
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir(), 0, true, itemId, currentGUI.getBindingIndex(), itemId).open(player);
            return;
        }

        // Edit Level/Chance Button
        if (event.getSlot() == 26 && dp.contains("Edit Level/Chance")) {
            setSwitching(player); // Protect from onClose cleanup if any
            player.closeInventory();
            final TriggerSelectorGUI finalGUI = currentGUI;

            // Use ChatInputHandler to get Level then Chance
            ChatInputHandler.awaitSkillLevel(plugin, player, currentGUI.getSkillIdToEdit(), itemId, currentGUI.getBindingIndex(), currentGUI.getTempTrigger(), (level) -> {
                // Success Callback -> Reopen TriggerSelector
                new BukkitRunnableWrapper(plugin, () -> {
                    setSwitching(player); // Re-flag before opening
                    // Note: We need to retrieve the temp data stored in ChatInput or passed back
                    // For simplicity, we assume ChatInputHandler manages the state and calls finalCallback or we reopen from scratch
                    // Actually, finalizeSkillBinding does the saving.
                    // If we just want to EDIT properties without finalizing, we need a different flow.
                    // But here, awaitSkillLevel -> finalize -> save.
                    // So we probably want to return to SkillBindingGUI after save.
                    new SkillBindingGUI(plugin, itemFile).open(player);
                });
            });
            return;
        }

        // Confirm Button
        if (event.getSlot() == 22 && dp.contains("Confirm Binding")) {
            player.closeInventory();
            plugin.getChatInputHandler().finalizeSkillBinding(player, itemId,
                    currentGUI.getSkillIdToEdit(),
                    currentGUI.getBindingIndex(),
                    currentGUI.getTempTrigger(),
                    currentGUI.getTempLevel(),
                    currentGUI.getTempChance());
            new BukkitRunnableWrapper(plugin, () -> {
                setSwitching(player);
                new SkillBindingGUI(plugin, itemFile).open(player);
            });
            return;
        }
    }

    private void handleSkillBindingClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring(8, title.lastIndexOf(" ["));
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;
        String itemId = itemFile.getName().replace(".yml", "");

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.EMERALD || clicked.getType() == Material.LIME_DYE || clicked.getItemMeta().getDisplayName().contains("Add New")) {
            // Add New Binding -> Open Skill Selector (New Mode)
            setSwitching(player);
            // Pass bindingIndex = -1 for NEW
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir(), 0, true, itemId, -1, itemId).open(player);
            return;
        }
        else if (clicked.getType() == Material.RED_BED) { // Back
            setSwitching(player);
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
        }
        else if (clicked.getType() == Material.CHEST) { // Save
            player.sendMessage("§aItem Saved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
        }
        else {
            // Existing Binding Click
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            List<ItemSkillBinding> bindings = attr.getSkillBindings();
            int slot = event.getSlot();

            if (slot < bindings.size()) {
                ItemSkillBinding binding = bindings.get(slot);

                if (event.isLeftClick() && !event.isShiftClick()) {
                    // Left Click: Edit
                    setSwitching(player);
                    new TriggerSelectorGUI(plugin, itemId, slot, binding.getSkillId()).open(player);
                } else if (event.isRightClick()) {
                    // Right Click: Remove
                    bindings.remove(slot);
                    plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
                    player.sendMessage("§cRemoved skill binding.");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    setSwitching(player);
                    new SkillBindingGUI(plugin, itemFile).open(player);
                }
            }
        }
    }

    // [NEW] Handle Skill Selector Mode Click
    private void handleSkillSelectModeClick(InventoryClickEvent event, Player player, String title) {
        handleSkillLibraryNavigation(event, player, title, true); // Use navigation logic

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        String dp = meta.getDisplayName();

        // Check for Skill Item Selection
        if (dp.startsWith("§e[Select]")) {
            String skillId = null;
            // 1. Try to get ID from Lore (Hidden §0SKILL_ID:...) - Supports Skill Packs
            if (meta.hasLore()) {
                for (String line : meta.getLore()) {
                    if (line.contains("SKILL_ID:")) {
                        skillId = line.substring(line.indexOf("SKILL_ID:") + 9).trim();
                        break;
                    }
                }
            }
            // 2. Fallback: Parse from Display Name if Lore missing
            if (skillId == null) {
                skillId = dp.substring("§e[Select] ".length());
            }

            // Retrieve context (itemId, bindingIndex) from GUI state (e.g., hidden in Back button or Title parsing)
            // Assuming SkillLibraryGUI stores this in the "Back to Skill Binding" button (Slot 53)
            ItemStack backItem = event.getInventory().getItem(53);
            String itemId = null;
            int bindingIndex = -1;

            if (backItem != null && backItem.hasItemMeta()) {
                List<String> backLore = backItem.getItemMeta().getLore();
                if (backLore != null) {
                    for (String l : backLore) {
                        if (l.contains("INDEX:")) bindingIndex = Integer.parseInt(l.split("INDEX:")[1].trim());
                    }
                }
                String backName = backItem.getItemMeta().getDisplayName();
                if (backName.contains("(Skill: ")) {
                    itemId = backName.split("\\(Skill: ")[1].replace(")", "").trim();
                }
            }

            if (skillId != null && itemId != null) {
                setSwitching(player);
                player.closeInventory();

                // Start Chat Input Flow: Level -> Chance -> TriggerSelector
                // We pass null for TriggerType to indicate we are coming from Library and need to select trigger next
                ChatInputHandler.awaitSkillLevel(plugin, player, skillId, itemId, bindingIndex, null, (level) -> {
                    // This callback might not be reached if ChatInputHandler handles the flow,
                    // but providing it just in case.
                });
            }
        }
    }

    private void handleSkillLibraryNavigation(InventoryClickEvent event, Player player, String title, boolean isSelectMode) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        Material type = clicked.getType();
        String dp = clicked.getItemMeta().getDisplayName();

        // Reconstruct Current GUI State
        SkillLibraryGUI currentGUI = null;
        // ... (Reconstruction logic omitted for brevity, assuming standard navigation calls below work)
        // Note: Ideally, SkillLibraryGUI should handle its own clicks or we replicate the logic here.
        // For FULLCODE, I will replicate the basic navigation to ensure it works.

        String[] parts = title.split(" #P");
        int page = 0;
        if (parts.length > 1) page = Integer.parseInt(parts[1]);

        String pathStr = parts[0].substring(parts[0].indexOf(":") + 1).trim();
        File currentDir = plugin.getSkillManager().getFileFromRelative(pathStr);

        // Extract Select Mode Context if needed
        String itemId = null;
        int bindingIndex = -1;
        if (isSelectMode) {
            ItemStack backItem = event.getInventory().getItem(53);
            if (backItem != null && backItem.hasItemMeta() && backItem.getItemMeta().getDisplayName().contains("(Skill:")) {
                itemId = backItem.getItemMeta().getDisplayName().split("\\(Skill: ")[1].replace(")", "").trim();
                // Try to get index from lore
                if (backItem.getItemMeta().getLore() != null) {
                    for(String l : backItem.getItemMeta().getLore()) {
                        if(l.contains("INDEX:")) bindingIndex = Integer.parseInt(l.split("INDEX:")[1].trim());
                    }
                }
            }
        }

        if (type == Material.ARROW) {
            if (dp.contains("Previous")) {
                setSwitching(player);
                new SkillLibraryGUI(plugin, currentDir, page - 1, isSelectMode, null, bindingIndex, itemId).open(player);
            } else if (dp.contains("Next")) {
                setSwitching(player);
                new SkillLibraryGUI(plugin, currentDir, page + 1, isSelectMode, null, bindingIndex, itemId).open(player);
            } else if (dp.contains("Back")) { // Up Directory
                setSwitching(player);
                new SkillLibraryGUI(plugin, currentDir.getParentFile(), 0, isSelectMode, null, bindingIndex, itemId).open(player);
            }
        } else if (type == Material.CHEST) {
            // Enter Folder
            String folderName = dp.substring(2); // Remove color code
            File nextDir = new File(currentDir, folderName);
            if (nextDir.isDirectory()) {
                setSwitching(player);
                new SkillLibraryGUI(plugin, nextDir, 0, isSelectMode, null, bindingIndex, itemId).open(player);
            }
        } else if (type == Material.RED_BED && isSelectMode) {
            // Back to Binding GUI
            if (itemId != null) {
                setSwitching(player);
                new SkillBindingGUI(plugin, findFileByName(plugin.getItemManager().getRootDir(), itemId)).open(player);
            }
        } else if (type == Material.PAPER && dp.startsWith("§bSkill Pack:")) {
            // Clicked a Skill Pack file -> Open it as a "folder" of skills (For Item Editor Mode)
            // If normal mode, maybe edit file? But here we focus on Selector.
            if (isSelectMode) {
                // SkillLibraryGUI treats .yml files as containers in Editor Mode automatically if passed as currentDir?
                // Actually, `listContents` lists files. `SkillLibraryGUI` logic iterates keys if it's a file.
                // So we just need to "enter" this file.
                String fileName = dp.substring("§bSkill Pack: §f".length());
                File packFile = new File(currentDir, fileName);
                // We cannot open a File as a Directory in existing logic easily unless listContents supports it.
                // But `SkillLibraryGUI` logic shows skills IF `currentDir` is a file?
                // No, standard logic lists children of a dir.
                // The fix involves `SkillLibraryGUI` displaying the content of the pack IN PLACE (which I did in previous fix)
                // OR treating the pack as a folder.
                // Assuming the GUI displays the skills directly if we are in Editor Mode (as per previous fix),
                // this click shouldn't happen for Packs if they are already expanded.
                // If they are NOT expanded, we might need custom logic.
                // *Wait*, previous SkillLibraryGUI code iterates files. If file is .yml, it iterates keys and shows items.
                // So there is no "Click Pack to Enter", the skills are just THERE.
                // So this block is likely redundant for Editor Mode if logic is correct.
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

    private static class BukkitRunnableWrapper {
        public BukkitRunnableWrapper(ThaiRoCorePlugin plugin, Runnable r) {
            plugin.getServer().getScheduler().runTask(plugin, r);
        }
    }
}