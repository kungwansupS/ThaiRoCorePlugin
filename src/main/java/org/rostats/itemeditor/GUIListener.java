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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.AttributeEditorGUI.Page;
import org.rostats.itemeditor.EffectEnchantGUI.Mode;
import org.rostats.gui.SkillLibraryGUI; // Import SkillLibrary

import java.io.File;
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
                new ItemLibraryGUI(plugin, currentDir.getParentFile()).open(player);
                return;
            }
            if (fileName.equals("root")) {
                new ItemLibraryGUI(plugin, plugin.getItemManager().getRootDir()).open(player);
                return;
            }
            if (fileName.equals("new_folder")) {
                plugin.getChatInputHandler().awaitInput(player, "Folder Name:", (str) -> {
                    plugin.getItemManager().createFolder(finalCurrentDir, str);
                    new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
                });
                return;
            }
            if (fileName.equals("new_item")) {
                plugin.getChatInputHandler().awaitInput(player, "Item Name:", (str) -> {
                    plugin.getItemManager().createItem(finalCurrentDir, str, Material.STONE);
                    new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
                });
                return;
            }
        }

        if (fileName == null) return;

        File target = new File(currentDir, fileName);
        final File finalTarget = target;

        if (target.isDirectory()) {
            if (event.getClick().isLeftClick() && !event.isShiftClick()) {
                new ItemLibraryGUI(plugin, target).open(player);
            } else if (event.isShiftClick() && event.isLeftClick()) {
                new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            } else if (event.isShiftClick() && event.isRightClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Rename:", (str) -> {
                    plugin.getItemManager().renameFile(finalTarget, str);
                    new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
                });
            }
        } else {
            if (event.getClick() == ClickType.LEFT) {
                new AttributeEditorGUI(plugin, target).open(player, Page.GENERAL);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                ItemStack item = plugin.getItemManager().loadItemStack(target);
                player.getInventory().addItem(item);
                player.sendMessage("§aItem given!");
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
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
            new ItemLibraryGUI(plugin, itemFile.getParentFile()).open(player);
            return;
        }
        for (Page p : Page.values()) {
            if (dp.contains(p.name())) {
                new AttributeEditorGUI(plugin, itemFile).open(player, p);
                return;
            }
        }

        if (dp.contains("Change Type")) {
            new ItemTypeSelectorGUI(plugin, itemFile).open(player);
            return;
        }
        if (dp.contains("Edit Effects")) {
            new EffectEnchantGUI(plugin, itemFile, Mode.EFFECT).open(player);
            return;
        }
        if (dp.contains("Edit Enchantments")) {
            new EffectEnchantGUI(plugin, itemFile, Mode.ENCHANT).open(player);
            return;
        }
        if (dp.contains("Edit Skills")) {
            new SkillBindingGUI(plugin, itemFile).open(player);
            return;
        }
        if (dp.contains("Rename Item")) {
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
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
            return;
        }

        if (dp.contains("Unbreakable")) {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            attr.setUnbreakable(!attr.isUnbreakable());
            ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);
            plugin.getItemAttributeManager().applyAttributesToItem(stack, attr);
            plugin.getItemManager().saveItem(itemFile, attr, stack);
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
            new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
        });
    }

    private void handleTriggerSelectClick(InventoryClickEvent event, Player player, String skillId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (clicked.getType() == Material.ARROW) {
            Map<String, Object> flowData = skillBindingFlow.get(player.getUniqueId());
            if (flowData != null && flowData.containsKey("itemFile")) {
                new SkillBindingGUI(plugin, (File) flowData.get("itemFile")).open(player);
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
            Map<String, Object> flowData = skillBindingFlow.get(player.getUniqueId());
            if (flowData == null) return;
            flowData.put("trigger", trigger);

            plugin.getChatInputHandler().awaitInput(player, "Enter Skill Level (ตัวเลข):", (lvlStr) -> {
                try {
                    int level = Integer.parseInt(lvlStr);
                    if (level < 1) level = 1;

                    int finalLevel = level;
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

                            skillBindingFlow.remove(player.getUniqueId());
                            new BukkitRunnableWrapper(plugin, () -> new SkillBindingGUI(plugin, itemFile).open(player));

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
            Map<String, Object> flowData = new HashMap<>();
            flowData.put("itemFile", itemFile);
            skillBindingFlow.put(player.getUniqueId(), flowData);

            // [FIXED] เรียก openSelectMode (3 args)
            new SkillLibraryGUI(plugin).openSelectMode(player,
                    (selectedSkillId) -> {
                        // เมื่อเลือกเสร็จ -> ไปหน้า Trigger
                        Map<String, Object> flow = skillBindingFlow.get(player.getUniqueId());
                        if (flow != null) {
                            flow.put("skillId", selectedSkillId);
                            new TriggerSelectorGUI(plugin, selectedSkillId).open(player);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                        }
                    },
                    () -> {
                        // เมื่อยกเลิก -> กลับหน้าเดิม
                        skillBindingFlow.remove(player.getUniqueId());
                        new SkillBindingGUI(plugin, itemFile).open(player);
                    }
            );

        } else if (clicked.getType() == Material.ARROW) {
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
            new EffectEnchantGUI(plugin, itemFile, mode).open(player);
        }
        else if (slot == 50 && selected != null) {
            plugin.getChatInputHandler().awaitInput(player, "Enter Level:", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    applyEffectEnchant(player, itemFile, mode, selected, lvl, true);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid Number");
                    new BukkitRunnableWrapper(plugin, () -> new EffectEnchantGUI(plugin, itemFile, mode).open(player));
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
            player.removeMetadata(metaKey, plugin);
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
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, parent).open(player));
            } else {
                player.sendMessage("§cFile not found.");
                player.closeInventory();
            }
        } else if (dp.contains("CANCEL")) {
            if (target != null && target.exists()) {
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, target.getParentFile()).open(player));
            } else {
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, plugin.getItemManager().getRootDir()).open(player));
            }
        }
    }

    private void handleMaterialSelectClick(InventoryClickEvent event, Player player, String fileName) {
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if(itemFile == null) return;
        ItemStack clicked = event.getCurrentItem();
        if(clicked == null) return;
        if(clicked.getType() == Material.ARROW) {
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
        new BukkitRunnableWrapper(plugin, () -> new AttributeEditorGUI(plugin, file).open(player, Page.GENERAL));
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