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
import org.rostats.itemeditor.ItemLibraryGUI;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<java.util.UUID, Map<String, Object>> editingActions = new HashMap<>();

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    private void openGUI(Player player, Tab tab) {
        player.setMetadata("ROSTATS_SWITCH", new FixedMetadataValue(plugin, true));
        new CharacterGUI(plugin).open(player, tab);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!title.contains("Character Status (ROO)")) return;

        if (player.hasMetadata("ROSTATS_SWITCH")) {
            player.removeMetadata("ROSTATS_SWITCH", plugin);
            return;
        }

        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        if (plugin.getStatManager().getTotalPendingCost(data) > 0) {
            data.clearAllPendingStats();
            plugin.getAttributeHandler().updatePlayerStats(player);
            plugin.getManaManager().updateBar(player);
            player.sendMessage("§e[System] ยกเลิกค่า Stat ที่ยังไม่ได้ยืนยัน (Allocate)");
            player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1f);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // --- SKILL SYSTEM LOGIC ---
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
        if (title.startsWith("SkillEditor: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            handleSkillEditorClick(event, player, title.substring(13));
            return;
        }
        if (title.startsWith("ActionSelector: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            handleActionSelectorClick(event, player, title.substring(16));
            return;
        }
        if (title.startsWith("ActionEdit: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            String[] parts = title.substring(12).split(" #");
            if (parts.length == 2) {
                handleActionPropertyClick(event, player, parts[0], Integer.parseInt(parts[1]));
            }
            return;
        }

        // --- ITEM EDITOR LOGIC ---
        if (title.startsWith("Library: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                handleImportItem(event, player, title.substring(9));
            } else {
                handleLibraryClick(event, player, title.substring(9));
            }
            return;
        }
        if (title.startsWith("Editor: ")) {
            event.setCancelled(true);
            if (title.contains("EFFECT Select]") || title.contains("ENCHANT Select]")) {
                handleEffectEnchantClick(event, player, title);
                return;
            }
            handleEditorClick(event, player, title);
            return;
        }
        if (title.startsWith("Confirm Delete: ")) {
            event.setCancelled(true);
            handleConfirmDeleteClick(event, player, title);
            return;
        }

        // --- CHARACTER STATUS LOGIC ---
        if (title.contains("Character Status (ROO)")) {
            handleCharacterStatusClick(event, player, title);
            return;
        }
    }

    // ====================================================================================
    // SKILL SYSTEM HANDLERS
    // ====================================================================================

    private void handleActionPropertyClick(InventoryClickEvent event, Player player, String skillId, int index) {
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null || index >= skill.getActions().size()) { player.closeInventory(); return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        Map<String, Object> tempMap = editingActions.getOrDefault(player.getUniqueId(), new HashMap<>());
        if (tempMap.isEmpty()) {
            tempMap = new HashMap<>(skill.getActions().get(index).serialize());
            editingActions.put(player.getUniqueId(), tempMap);
        }
        final Map<String, Object> data = tempMap;

        if (clicked.getType() == Material.RED_CONCRETE) { // Cancel
            editingActions.remove(player.getUniqueId());
            new SkillEditorGUI(plugin, skillId).open(player);
            return;
        }

        if (clicked.getType() == Material.EMERALD_BLOCK) { // Save
            try {
                String typeStr = (String) data.get("type");
                ActionType type = ActionType.valueOf(typeStr);

                SkillAction newAction = null;
                switch (type) {
                    case DAMAGE: newAction = new DamageAction(plugin, (String)data.getOrDefault("formula","ATK"), (String)data.getOrDefault("element","NEUTRAL")); break;
                    case HEAL: newAction = new HealAction(plugin, (String)data.getOrDefault("formula","10"), (boolean)data.getOrDefault("is-mana", false)); break;
                    case APPLY_EFFECT:
                        String eid = (String)data.getOrDefault("effect-id", "unknown");
                        EffectType et = EffectType.valueOf((String)data.getOrDefault("effect-type", "STAT_MODIFIER"));
                        int lv = data.get("level") instanceof Number ? ((Number)data.get("level")).intValue() : 1;
                        double pw = data.get("power") instanceof Number ? ((Number)data.get("power")).doubleValue() : 0.0;
                        long dr = data.get("duration") instanceof Number ? ((Number)data.get("duration")).longValue() : 100;
                        double ch = data.get("chance") instanceof Number ? ((Number)data.get("chance")).doubleValue() : 1.0;
                        String sk = (String)data.getOrDefault("stat-key", null);
                        newAction = new EffectAction(plugin, eid, et, lv, pw, dr, ch, sk);
                        break;
                }

                if (newAction != null) {
                    skill.getActions().set(index, newAction);
                    plugin.getSkillManager().saveSkill(skill);
                    player.sendMessage("§aAction updated!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                }
            } catch (Exception e) {
                player.sendMessage("§cError saving action: " + e.getMessage());
            }
            editingActions.remove(player.getUniqueId());
            new SkillEditorGUI(plugin, skillId).open(player);
            return;
        }

        List<String> lore = clicked.getItemMeta().getLore();
        String key = null;
        if (lore != null) {
            for (String l : lore) {
                if (l.startsWith("§0Key:")) {
                    key = l.substring(6);
                    break;
                }
            }
        }

        if (key != null) {
            final String fKey = key;
            Object val = data.get(fKey);

            if (val instanceof Boolean) {
                data.put(fKey, !((Boolean) val));
                reopenPropertyGUI(player, skillId, index, data, skill.getActions().get(index).getType());
            } else {
                plugin.getChatInputHandler().awaitInput(player, "Enter value for " + fKey + ":", (str) -> {
                    try {
                        if (val instanceof Integer) data.put(fKey, Integer.parseInt(str));
                        else if (val instanceof Double) data.put(fKey, Double.parseDouble(str));
                        else if (val instanceof Long) data.put(fKey, Long.parseLong(str));
                        else data.put(fKey, str);
                        runSync(() -> reopenPropertyGUI(player, skillId, index, data, skill.getActions().get(index).getType()));
                    } catch (Exception e) {
                        player.sendMessage("§cInvalid format.");
                        runSync(() -> reopenPropertyGUI(player, skillId, index, data, skill.getActions().get(index).getType()));
                    }
                });
            }
        }
    }

    private void reopenPropertyGUI(Player player, String skillId, int index, Map<String, Object> data, ActionType type) {
        SkillAction tempAction = new SkillAction() {
            public ActionType getType() { return type; }
            public void execute(org.bukkit.entity.LivingEntity c, org.bukkit.entity.LivingEntity t, int l) {}
            public Map<String, Object> serialize() { return data; }
        };
        new SkillActionPropertyGUI(plugin, skillId, index, tempAction).open(player);
    }

    private void handleActionSelectorClick(InventoryClickEvent event, Player player, String skillId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.ARROW) {
            new SkillEditorGUI(plugin, skillId).open(player);
            return;
        }

        List<String> lore = clicked.getItemMeta().getLore();
        if (lore != null && !lore.isEmpty()) {
            String last = lore.get(lore.size() - 1);
            if (last.startsWith("ActionType: ")) {
                try {
                    ActionType type = ActionType.valueOf(last.substring(12));
                    SkillData skill = plugin.getSkillManager().getSkill(skillId);
                    SkillAction action = null;
                    switch (type) {
                        case DAMAGE: action = new DamageAction(plugin, "ATK * 1.0", "NEUTRAL"); break;
                        case HEAL: action = new HealAction(plugin, "10", false); break;
                        case APPLY_EFFECT: action = new EffectAction(plugin, "unknown", EffectType.STAT_MODIFIER, 1, 10, 100, 1.0, "STR"); break;
                    }
                    if (action != null) {
                        skill.addAction(action);
                        player.sendMessage("§aAdded: " + type);
                        plugin.getSkillManager().saveSkill(skill);
                    }
                    new SkillEditorGUI(plugin, skillId).open(player);
                } catch (Exception e) { player.sendMessage("§cError creating action."); }
            }
        }
    }

    private void handleSkillEditorClick(InventoryClickEvent event, Player player, String skillId) {
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) { player.closeInventory(); return; }
        int slot = event.getSlot();

        if (slot == 49) {
            plugin.getSkillManager().saveSkill(skill);
            player.sendMessage("§aSkill saved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        }
        else if (slot == 50) {
            new SkillActionSelectorGUI(plugin, skillId).open(player);
        }
        else if (slot == 53) {
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
        }
        else if (slot >= 18 && slot <= 44) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                if (event.isShiftClick() && event.isRightClick()) {
                    // Remove Logic
                    List<String> lore = item.getItemMeta().getLore();
                    if (lore != null) {
                        for (String l : lore) {
                            if (l.startsWith("§7Index: ")) {
                                try {
                                    int index = Integer.parseInt(l.substring(9));
                                    if (index >= 0 && index < skill.getActions().size()) {
                                        skill.getActions().remove(index);
                                        plugin.getSkillManager().saveSkill(skill);
                                        new SkillEditorGUI(plugin, skillId).open(player);
                                    }
                                } catch (Exception e) {}
                            }
                        }
                    }
                } else if (event.isLeftClick()) {
                    List<String> lore = item.getItemMeta().getLore();
                    if (lore != null) {
                        for (String l : lore) {
                            if (l.startsWith("§7Index: ")) {
                                try {
                                    int index = Integer.parseInt(l.substring(9));
                                    editingActions.put(player.getUniqueId(), new HashMap<>(skill.getActions().get(index).serialize()));
                                    new SkillActionPropertyGUI(plugin, skillId, index, skill.getActions().get(index)).open(player);
                                } catch (Exception e) {}
                            }
                        }
                    }
                }
            }
        }
        else if (slot == 0) {
            plugin.getChatInputHandler().awaitInput(player, "Name:", (str) -> {
                skill.setDisplayName(str.replace("&", "§")); runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
        }
        else if (slot == 2) {
            TriggerType[] types = TriggerType.values();
            skill.setTrigger(types[(skill.getTrigger().ordinal() + 1) % types.length]);
            new SkillEditorGUI(plugin, skillId).open(player);
        }
        else if (slot == 4) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                skill.setIcon(cursor.getType());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.setCursor(cursor);
                new SkillEditorGUI(plugin, skillId).open(player);
            }
        }
        else if (slot == 6) {
            if (event.isLeftClick()) plugin.getChatInputHandler().awaitInput(player, "Cooldown:", (str) -> { try { skill.setCooldownBase(Double.parseDouble(str)); } catch(Exception e){} runSync(() -> new SkillEditorGUI(plugin, skillId).open(player)); });
            else plugin.getChatInputHandler().awaitInput(player, "CastTime:", (str) -> { try { skill.setCastTime(Double.parseDouble(str)); } catch(Exception e){} runSync(() -> new SkillEditorGUI(plugin, skillId).open(player)); });
        }
        else if (slot == 8) {
            plugin.getChatInputHandler().awaitInput(player, "SP Cost:", (str) -> { try { skill.setSpCostBase(Integer.parseInt(str)); } catch(Exception e){} runSync(() -> new SkillEditorGUI(plugin, skillId).open(player)); });
        }
    }

    private void handleSkillLibraryClick(InventoryClickEvent event, Player player, String relativePath) {
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
                plugin.getSkillManager().createFolder(finalDir, str); runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player));
            }); return;
        }
        if (clicked.getType() == Material.WRITABLE_BOOK) {
            plugin.getChatInputHandler().awaitInput(player, "Skill ID:", (str) -> {
                plugin.getSkillManager().createSkill(finalDir, str); runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player));
            }); return;
        }
        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        if (target.isDirectory()) {
            if (event.isLeftClick() && !event.isShiftClick()) new SkillLibraryGUI(plugin, target).open(player);
            else if (event.isShiftClick() && event.isLeftClick()) new SkillLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            else if (event.isRightClick()) { plugin.getChatInputHandler().awaitInput(player, "Rename:", (str) -> { plugin.getSkillManager().renameFile(target, str); runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player)); }); }
        } else {
            if (event.isLeftClick() && !event.isShiftClick()) {
                // FIX: Normalize name to ID (lowercase, no spaces)
                String skillId = name.toLowerCase().replace(" ", "_");
                new SkillEditorGUI(plugin, skillId).open(player);
            }
            else if (event.isShiftClick() && event.isLeftClick()) new SkillLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
        }
    }

    private void handleSkillDeleteClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring("Skill Delete: ".length());
        File target = findFileRecursive(plugin.getSkillManager().getRootDir(), fileName);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        if (clicked.getType() == Material.LIME_CONCRETE && target != null) {
            File parent = target.getParentFile();
            plugin.getSkillManager().deleteFile(target);
            player.sendMessage("§cDeleted.");
            runSync(() -> new SkillLibraryGUI(plugin, parent).open(player));
        } else if (clicked.getType() == Material.RED_CONCRETE) {
            player.closeInventory();
        }
    }

    // ====================================================================================
    // CHARACTER STATUS & ITEM EDITOR HANDLERS
    // ====================================================================================

    private void handleCharacterStatusClick(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        String name = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().displayName());
        int slot = event.getSlot();

        if (slot == 2) openGUI(player, Tab.BASIC_INFO);
        else if (slot == 3) openGUI(player, Tab.GENERAL);
        else if (slot == 4) openGUI(player, Tab.ADVANCED);
        else if (slot == 5) openGUI(player, Tab.SPECIAL);
        else if (slot == 34) openGUI(player, Tab.RESET_CONFIRM);
        else if (slot == 44) openGUI(player, Tab.RESET_CONFIRM);
        else if (slot == 8) player.closeInventory();
        else if (slot == 29 && name.contains("[ยืนยัน]")) { performReset(player); player.closeInventory(); }
        else if (slot == 31 && name.contains("[ยกเลิก]")) openGUI(player, Tab.BASIC_INFO);
        else if (slot == 52 && name.contains("Allocate")) { plugin.getStatManager().allocateStats(player); openGUI(player, Tab.BASIC_INFO); }
        else if (slot == 42 && name.contains("Reset Select")) { plugin.getStatManager().getData(player.getUniqueId()).clearAllPendingStats(); openGUI(player, Tab.BASIC_INFO); }

        String plusKey = getStatKey(slot, 36);
        if (plusKey != null) handleStatUpgrade(player, plusKey, event.isLeftClick(), event.isRightClick());
        String minusKey = getStatKey(slot, 45);
        if (minusKey != null) handleStatDowngrade(player, minusKey, event.isLeftClick(), event.isRightClick());
    }

    private void performReset(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        int freeResets = plugin.getConfig().getInt("reset-system.free-resets", 3);
        int usedResets = data.getResetCount();
        Material resetItem = Material.getMaterial(plugin.getConfig().getString("reset-system.reset-item", "NETHER_STAR"));

        if (usedResets < freeResets) {
            data.resetStats(); data.incrementResetCount();
            player.sendMessage("§eFree Reset used! (" + (usedResets + 1) + "/" + freeResets + ")");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else if (resetItem != null && player.getInventory().contains(resetItem)) {
            player.getInventory().removeItem(new ItemStack(resetItem, 1));
            data.resetStats(); data.incrementResetCount();
            player.sendMessage("§bUsed 1x " + resetItem.name() + " to reset!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else {
            player.sendMessage("§cNo free resets!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        plugin.getAttributeHandler().updatePlayerStats(player);
        plugin.getManaManager().updateBar(player);
    }

    private String getStatKey(int slot, int startSlot) {
        if (slot < startSlot || slot > startSlot + 5) return null;
        return switch (slot - startSlot) {
            case 0 -> "STR"; case 1 -> "AGI"; case 2 -> "VIT"; case 3 -> "INT"; case 4 -> "DEX"; case 5 -> "LUK"; default -> null;
        };
    }
    private void handleStatUpgrade(Player player, String statKey, boolean isLeftClick, boolean isRightClick) {
        boolean success = false;
        if (isLeftClick) success = plugin.getStatManager().upgradeStat(player, statKey);
        else if (isRightClick) { int c=0; while(c<10 && plugin.getStatManager().upgradeStat(player, statKey)) { c++; success=true; } }
        if (success) { plugin.getAttributeHandler().updatePlayerStats(player); plugin.getManaManager().updateBar(player); openGUI(player, Tab.BASIC_INFO); }
    }
    private void handleStatDowngrade(Player player, String statKey, boolean isLeftClick, boolean isRightClick) {
        boolean success = false;
        if (isLeftClick) success = plugin.getStatManager().downgradeStat(player, statKey);
        else if (isRightClick) { int c=0; while(c<10 && plugin.getStatManager().downgradeStat(player, statKey)) { c++; success=true; } }
        if (success) { plugin.getAttributeHandler().updatePlayerStats(player); plugin.getManaManager().updateBar(player); openGUI(player, Tab.BASIC_INFO); }
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, String title) {
        int lastSpaceIndex = title.lastIndexOf(" [");
        if (lastSpaceIndex == -1) return;
        String fileName = title.substring(8, lastSpaceIndex);
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null || !itemFile.exists()) { player.closeInventory(); return; }
        final File finalItemFile = itemFile;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String dp = clicked.getItemMeta().getDisplayName();

        if (dp.contains("Edit Effects")) { new EffectEnchantGUI(plugin, finalItemFile, EffectEnchantGUI.Mode.EFFECT).open(player); return; }
        if (dp.contains("Edit Enchantments")) { new EffectEnchantGUI(plugin, finalItemFile, EffectEnchantGUI.Mode.ENCHANT).open(player); return; }
        if (dp.contains("Back to Library")) { new ItemLibraryGUI(plugin, itemFile.getParentFile()).open(player); return; }
        for (AttributeEditorGUI.Page p : AttributeEditorGUI.Page.values()) {
            if (dp.contains(p.name())) { new AttributeEditorGUI(plugin, itemFile).open(player, p); return; }
        }
        if (dp.contains("Rename Item")) {
            plugin.getChatInputHandler().awaitInput(player, "New Name:", (str) -> {
                ItemStack stack = plugin.getItemManager().loadItemStack(finalItemFile);
                org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
                if (meta != null) meta.setDisplayName(str.replace("&", "§"));
                stack.setItemMeta(meta);
                ItemAttribute attr = plugin.getItemManager().loadAttribute(finalItemFile);
                plugin.getItemManager().saveItem(finalItemFile, attr, stack);
                runSync(() -> new AttributeEditorGUI(plugin, finalItemFile).open(player, AttributeEditorGUI.Page.GENERAL));
            }); return;
        }
        if (dp.contains("Edit Lore")) {
            plugin.getChatInputHandler().awaitMultiLineInput(player, "Edit Lore:", (lines) -> {
                ItemStack stack = plugin.getItemManager().loadItemStack(finalItemFile);
                org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
                if (meta != null) meta.setLore(lines);
                stack.setItemMeta(meta);
                ItemAttribute attr = plugin.getItemManager().loadAttribute(finalItemFile);
                plugin.getItemManager().saveItem(finalItemFile, attr, stack);
                runSync(() -> new AttributeEditorGUI(plugin, finalItemFile).open(player, AttributeEditorGUI.Page.GENERAL));
            }); return;
        }
        if (dp.contains("Remove Vanilla")) {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            attr.setRemoveVanillaAttribute(!attr.isRemoveVanillaAttribute());
            plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
            new AttributeEditorGUI(plugin, itemFile).open(player, AttributeEditorGUI.Page.GENERAL);
            return;
        }
        if (dp.contains("Save to File")) {
            player.sendMessage("§aSaved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
            return;
        }
        for (ItemAttributeType type : ItemAttributeType.values()) {
            if (dp.equals(type.getDisplayName())) {
                ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
                double current = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(attr, type);
                double change = 0;
                if (event.isLeftClick()) change = type.getClickStep();
                else if (event.isRightClick()) change = -type.getClickStep();
                else if (event.isShiftClick() && event.isLeftClick()) change = type.getRightClickStep();
                else if (event.isShiftClick() && event.isRightClick()) change = -type.getRightClickStep();
                plugin.getItemAttributeManager().setAttributeToObj(attr, type, current + change);
                plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
                new AttributeEditorGUI(plugin, itemFile).open(player, getPageFromTitle(title));
                return;
            }
        }
    }

    private void handleEffectEnchantClick(InventoryClickEvent event, Player player, String title) {
        int lastBracket = title.lastIndexOf(" [");
        if (lastBracket == -1) return;
        String fileName = title.substring(8, lastBracket);
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;
        EffectEnchantGUI.Mode mode = title.contains("EFFECT") ? EffectEnchantGUI.Mode.EFFECT : EffectEnchantGUI.Mode.ENCHANT;
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
        } else if (slot == 50 && selected != null) {
            plugin.getChatInputHandler().awaitInput(player, "Enter Level:", (str) -> {
                try { int lvl = Integer.parseInt(str); applyEffectEnchant(player, itemFile, mode, selected, lvl, true); }
                catch (NumberFormatException e) { player.sendMessage("§cInvalid"); runSync(() -> new EffectEnchantGUI(plugin, itemFile, mode).open(player)); }
            });
        } else if (slot == 51 && selected != null) {
            applyEffectEnchant(player, itemFile, mode, selected, 1, true);
        } else if (slot == 52 && selected != null) {
            applyEffectEnchant(player, itemFile, mode, selected, 0, false);
        } else if (slot == 53) {
            player.removeMetadata(metaKey, plugin);
            new AttributeEditorGUI(plugin, itemFile).open(player, AttributeEditorGUI.Page.GENERAL);
        }
    }

    private void applyEffectEnchant(Player player, File file, EffectEnchantGUI.Mode mode, String key, int level, boolean add) {
        runSync(() -> {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(file);
            ItemStack stack = plugin.getItemManager().loadItemStack(file);
            boolean changed = false;
            if (mode == EffectEnchantGUI.Mode.EFFECT) {
                org.bukkit.potion.PotionEffectType type = org.bukkit.potion.PotionEffectType.getByName(key);
                if (type != null) {
                    if (add) attr.getPotionEffects().put(type, level); else attr.getPotionEffects().remove(type);
                    plugin.getItemManager().saveItem(file, attr, stack); changed = true;
                }
            } else {
                org.bukkit.enchantments.Enchantment ench = null;
                for (org.bukkit.enchantments.Enchantment e : org.bukkit.enchantments.Enchantment.values()) {
                    if (e.getKey().getKey().equalsIgnoreCase(key)) { ench = e; break; }
                }
                if (ench != null) {
                    org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
                    if (add) meta.addEnchant(ench, level, true); else meta.removeEnchant(ench);
                    stack.setItemMeta(meta);
                    plugin.getItemManager().saveItem(file, attr, stack); changed = true;
                }
            }
            if (changed) {
                player.sendMessage("§aUpdated!");
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
                player.sendMessage("§cDeleted.");
                runSync(() -> new ItemLibraryGUI(plugin, parent).open(player));
            }
        } else if (dp.contains("CANCEL")) {
            if (target != null) runSync(() -> new ItemLibraryGUI(plugin, target.getParentFile()).open(player));
        }
    }

    private void handleImportItem(InventoryClickEvent event, Player player, String relativePath) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getItemManager().getRootDir();
        final File finalDir = currentDir;
        player.closeInventory();
        plugin.getChatInputHandler().awaitInput(player, "File Name:", (name) -> {
            String fileName = name.endsWith(".yml") ? name : name + ".yml";
            File newFile = new File(finalDir, fileName);
            if (newFile.exists()) { player.sendMessage("§cExists!"); return; }
            ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
            plugin.getItemManager().saveItem(newFile, attr, item);
            runSync(() -> new ItemLibraryGUI(plugin, finalDir).open(player));
        });
    }

    private void handleLibraryClick(InventoryClickEvent event, Player player, String relativePath) {
        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getItemManager().getRootDir();
        final File finalDir = currentDir;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        String name = clicked.getItemMeta().getDisplayName().replace("§6§l", "").replace("§f", "");
        if (clicked.getType() == Material.ARROW) { new ItemLibraryGUI(plugin, currentDir.getParentFile()).open(player); return; }
        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("New Folder")) {
            plugin.getChatInputHandler().awaitInput(player, "Folder:", (str) -> {
                plugin.getItemManager().createFolder(finalDir, str);
                runSync(() -> new ItemLibraryGUI(plugin, finalDir).open(player));
            }); return;
        }
        if (clicked.getType() == Material.EMERALD) {
            plugin.getChatInputHandler().awaitInput(player, "Item Name:", (str) -> {
                plugin.getItemManager().createItem(finalDir, str, Material.STONE);
                runSync(() -> new ItemLibraryGUI(plugin, finalDir).open(player));
            }); return;
        }
        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        if (target.isDirectory()) {
            if (event.isLeftClick() && !event.isShiftClick()) new ItemLibraryGUI(plugin, target).open(player);
            else if (event.isShiftClick() && event.isLeftClick()) new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            else if (event.isRightClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Rename:", (str) -> {
                    plugin.getItemManager().renameFile(target, str);
                    runSync(() -> new ItemLibraryGUI(plugin, finalDir).open(player));
                });
            }
        } else {
            if (event.isLeftClick()) new AttributeEditorGUI(plugin, target).open(player, AttributeEditorGUI.Page.GENERAL);
            else if (event.isShiftClick() && event.isLeftClick()) new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            else if (event.isShiftClick() && event.isRightClick()) {
                ItemStack item = plugin.getItemManager().loadItemStack(target);
                player.getInventory().addItem(item);
                player.sendMessage("§aGiven!");
            }
        }
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

    private File findFileRecursive(File dir, String name) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().equals(name)) return f;
            if (f.isDirectory()) {
                File found = findFileRecursive(f, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private AttributeEditorGUI.Page getPageFromTitle(String title) {
        for (AttributeEditorGUI.Page p : AttributeEditorGUI.Page.values()) {
            if (title.contains(p.name())) return p;
        }
        return AttributeEditorGUI.Page.GENERAL;
    }

    private void runSync(Runnable r) {
        plugin.getServer().getScheduler().runTask(plugin, r);
    }
}