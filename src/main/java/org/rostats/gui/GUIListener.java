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
import org.rostats.engine.action.impl.*;
import org.rostats.engine.effect.EffectType;
import org.rostats.engine.skill.SkillData;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.gui.CharacterGUI.Tab;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        if (!title.contains(CharacterGUI.TITLE_HEADER)) return;

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

        if (title.contains(CharacterGUI.TITLE_HEADER)) {
            handleCharacterStatusClick(event, player, title);
            return;
        }
    }

    private void handleActionPropertyClick(InventoryClickEvent event, Player player, String skillId, int index) {
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null || index >= skill.getActions().size()) {
            player.sendMessage("§cAction not found!");
            player.closeInventory();
            return;
        }

        int slot = event.getSlot();
        SkillAction action = skill.getActions().get(index);
        Map<String, Object> tempData = editingActions.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());

        if (slot == 49) {
            Map<String, Object> serialized = new HashMap<>(tempData);
            SkillAction updated = deserializeAction(action.getType(), serialized);
            if (updated != null) {
                skill.getActions().set(index, updated);
                plugin.getSkillManager().saveSkill(skill);
                player.sendMessage("§aAction updated!");
                editingActions.remove(player.getUniqueId());
            }
            new SkillEditorGUI(plugin, skillId).open(player);
        } else if (slot == 53) {
            editingActions.remove(player.getUniqueId());
            new SkillEditorGUI(plugin, skillId).open(player);
        } else {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                List<String> lore = clicked.getItemMeta().getLore();
                if (lore != null && !lore.isEmpty()) {
                    String firstLore = PlainTextComponentSerializer.plainText().serialize(
                            clicked.getItemMeta().lore().get(0));
                    String key = extractKey(firstLore);
                    if (key != null) {
                        plugin.getChatInputHandler().awaitInput(player, "Enter value for " + key + ":", (str) -> {
                            tempData.put(key, parseValue(str));
                            runSync(() -> new SkillActionPropertyGUI(plugin, skillId, index, action).open(player));
                        });
                    }
                }
            }
        }
    }

    private void handleActionSelectorClick(InventoryClickEvent event, Player player, String skillId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        String name = PlainTextComponentSerializer.plainText().serialize(clicked.displayName());
        try {
            ActionType type = ActionType.valueOf(name.replace(" ", "_").toUpperCase());
            SkillData skill = plugin.getSkillManager().getSkill(skillId);
            if (skill != null) {
                SkillAction newAction = createDefaultAction(type);
                if (newAction != null) {
                    skill.addAction(newAction);
                    plugin.getSkillManager().saveSkill(skill);
                    player.sendMessage("§aAdded action: " + type.name());
                }
            }
            new SkillEditorGUI(plugin, skillId).open(player);
        } catch (Exception e) {
            player.sendMessage("§cInvalid action type!");
        }
    }

    private void handleSkillEditorClick(InventoryClickEvent event, Player player, String skillId) {
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) {
            player.sendMessage("§cSkill not found!");
            return;
        }

        int slot = event.getSlot();

        if (slot == 1) {
            plugin.getChatInputHandler().awaitInput(player, "Max Level:", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    if (lvl < 1) lvl = 1;
                    skill.setMaxLevel(lvl);
                } catch (Exception e) {
                    player.sendMessage("§cInvalid number!");
                }
                runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
        } else if (slot == 45) {
            new SkillActionSelectorGUI(plugin, skillId).open(player);
        } else if (slot == 49) {
            plugin.getSkillManager().saveSkill(skill);
            player.sendMessage("§aSaved skill: " + skill.getDisplayName());
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else if (slot == 53) {
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
        } else if (slot >= 18 && slot <= 44) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                List<String> lore = item.getItemMeta().getLore();
                int index = -1;
                if (lore != null) {
                    for (String l : lore) {
                        if (l.startsWith("§7Index: ")) {
                            try {
                                index = Integer.parseInt(l.substring(9));
                            } catch (Exception e) {
                            }
                            break;
                        }
                    }
                }

                if (index != -1 && index < skill.getActions().size()) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        skill.getActions().remove(index);
                        plugin.getSkillManager().saveSkill(skill);
                        new SkillEditorGUI(plugin, skillId).open(player);
                    } else if (event.isShiftClick() && event.isLeftClick()) {
                        if (index > 0) {
                            Collections.swap(skill.getActions(), index, index - 1);
                            plugin.getSkillManager().saveSkill(skill);
                            new SkillEditorGUI(plugin, skillId).open(player);
                        }
                    } else if (event.isRightClick()) {
                        if (index < skill.getActions().size() - 1) {
                            Collections.swap(skill.getActions(), index, index + 1);
                            plugin.getSkillManager().saveSkill(skill);
                            new SkillEditorGUI(plugin, skillId).open(player);
                        }
                    } else if (event.isLeftClick()) {
                        editingActions.put(player.getUniqueId(), new HashMap<>(skill.getActions().get(index).serialize()));
                        new SkillActionPropertyGUI(plugin, skillId, index, skill.getActions().get(index)).open(player);
                    }
                }
            }
        } else if (slot == 0) {
            plugin.getChatInputHandler().awaitInput(player, "Name:", (str) -> {
                skill.setDisplayName(str.replace("&", "§"));
                runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
        } else if (slot == 2) {
            TriggerType[] types = TriggerType.values();
            skill.setTrigger(types[(skill.getTrigger().ordinal() + 1) % types.length]);
            new SkillEditorGUI(plugin, skillId).open(player);
        } else if (slot == 4) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                skill.setIcon(cursor.getType());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.setCursor(cursor);
                new SkillEditorGUI(plugin, skillId).open(player);
            }
        }
        // ⭐ [UPDATED] Slot 6: Timing (Cooldown, Global CD, Cast Time)
        else if (slot == 6) {
            if (event.isShiftClick() && event.isLeftClick()) {
                // Shift + Left Click = Edit Global Cooldown
                plugin.getChatInputHandler().awaitInput(player, "Global Cooldown (seconds):", (str) -> {
                    try {
                        double gcd = Double.parseDouble(str);
                        if (gcd < 0) gcd = 0;
                        skill.setGlobalCooldownBase(gcd);
                    } catch (Exception e) {
                        player.sendMessage("§cInvalid number!");
                    }
                    runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
                });
            } else if (event.isLeftClick()) {
                // Left Click = Edit Skill Cooldown
                plugin.getChatInputHandler().awaitInput(player, "Cooldown (seconds):", (str) -> {
                    try {
                        double cd = Double.parseDouble(str);
                        if (cd < 0) cd = 0;
                        skill.setCooldownBase(cd);
                    } catch (Exception e) {
                        player.sendMessage("§cInvalid number!");
                    }
                    runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
                });
            } else if (event.isRightClick()) {
                // Right Click = Edit Cast Time
                plugin.getChatInputHandler().awaitInput(player, "Cast Time (seconds):", (str) -> {
                    try {
                        double ct = Double.parseDouble(str);
                        if (ct < 0) ct = 0;
                        skill.setCastTime(ct);
                    } catch (Exception e) {
                        player.sendMessage("§cInvalid number!");
                    }
                    runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
                });
            }
        }
        // Slot 7: Required Level
        else if (slot == 7) {
            plugin.getChatInputHandler().awaitInput(player, "Required Level:", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    if (lvl < 1) lvl = 1;
                    skill.setRequiredLevel(lvl);
                } catch (Exception e) {
                    player.sendMessage("§cInvalid number!");
                }
                runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
        }
        // Slot 8: SP Cost
        else if (slot == 8) {
            plugin.getChatInputHandler().awaitInput(player, "SP Cost:", (str) -> {
                try {
                    int cost = Integer.parseInt(str);
                    if (cost < 0) cost = 0;
                    skill.setSpCostBase(cost);
                } catch (Exception e) {
                    player.sendMessage("§cInvalid number!");
                }
                runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
        }
    }

    private void handleSkillDeleteClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring(14);
        File file = findFileByName(plugin.getSkillManager().getRootDir(), fileName);
        if (file == null) return;
        File parent = file.getParentFile();

        ItemStack clicked = event.getCurrentItem();
        if (clicked != null) {
            if (clicked.getType() == Material.LIME_CONCRETE) {
                plugin.getSkillManager().deleteFile(file);
                player.sendMessage("§cDeleted: " + fileName);
                runSync(() -> new SkillLibraryGUI(plugin, parent).open(player));
            } else if (clicked.getType() == Material.RED_CONCRETE) {
                player.closeInventory();
            }
        }
    }

    private void handleSkillLibraryClick(InventoryClickEvent event, Player player, String relativePath) {
        File currentDir = plugin.getSkillManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getSkillManager().getRootDir();
        final File finalDir = currentDir;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        String name = clicked.getItemMeta().getDisplayName().replace("§6§l", "").replace("§f", "");

        if (clicked.getType() == Material.ARROW) {
            new SkillLibraryGUI(plugin, currentDir.getParentFile()).open(player);
            return;
        }
        if (clicked.getType() == Material.BOOKSHELF) {
            new SkillLibraryGUI(plugin, currentDir).open(player);
            return;
        }
        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("New Folder")) {
            plugin.getChatInputHandler().awaitInput(player, "Folder:", (str) -> {
                plugin.getSkillManager().createFolder(finalDir, str);
                runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player));
            });
            return;
        }
        if (clicked.getType() == Material.WRITABLE_BOOK) {
            plugin.getChatInputHandler().awaitInput(player, "Skill ID:", (str) -> {
                plugin.getSkillManager().createSkill(finalDir, str);
                runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player));
            });
            return;
        }
        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        if (target.isDirectory()) {
            new SkillLibraryGUI(plugin, target).open(player);
        } else if (target.exists()) {
            if (event.isRightClick()) {
                new SkillLibraryGUI(plugin, currentDir).openDeleteConfirm(player, target.getName());
            } else if (event.isShiftClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Rename to:", (str) -> {
                    plugin.getSkillManager().renameFile(target, str);
                    runSync(() -> new SkillLibraryGUI(plugin, finalDir).open(player));
                });
            } else {
                String skillId = name.replace(".yml", "");
                SkillData skill = plugin.getSkillManager().getSkill(skillId);
                if (skill != null) {
                    new SkillEditorGUI(plugin, skillId).open(player);
                } else {
                    player.sendMessage("§cSkill not loaded: " + skillId);
                }
            }
        }
    }

    private void handleCharacterStatusClick(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;
        String name = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().displayName());
        int slot = event.getSlot();

        if (slot == 2) openGUI(player, Tab.BASIC_INFO);
        else if (slot == 3) openGUI(player, Tab.GENERAL);
        else if (slot == 4) openGUI(player, Tab.ADVANCED);
        else if (slot == 5) openGUI(player, Tab.SPECIAL);
        else if (slot == 34) openGUI(player, Tab.RESET_CONFIRM);
        else if (slot == 44) openGUI(player, Tab.RESET_CONFIRM);
        else if (slot == 8) player.closeInventory();
        else if (slot == 29 && name.contains("[ยืนยัน]")) {
            performReset(player);
            player.closeInventory();
        } else if (slot == 31 && name.contains("[ยกเลิก]")) openGUI(player, Tab.BASIC_INFO);
        else if (slot == 52 && name.contains("Allocate")) {
            plugin.getStatManager().allocateStats(player);
            openGUI(player, Tab.BASIC_INFO);
        } else if (slot == 42 && name.contains("Reset Select")) {
            plugin.getStatManager().getData(player.getUniqueId()).clearAllPendingStats();
            openGUI(player, Tab.BASIC_INFO);
        }

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
            data.resetStats();
            data.incrementResetCount();
            player.sendMessage("§eFree Reset used! (" + (usedResets + 1) + "/" + freeResets + ")");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else if (resetItem != null && player.getInventory().contains(resetItem)) {
            player.getInventory().removeItem(new ItemStack(resetItem, 1));
            data.resetStats();
            data.incrementResetCount();
            player.sendMessage("§bUsed 1x " + resetItem.name() + " to reset!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else {
            player.sendMessage("§cNo free resets! Need " + (resetItem != null ? resetItem.name() : "item"));
        }
        plugin.getAttributeHandler().updatePlayerStats(player);
        plugin.getManaManager().updateBar(player);
    }

    private String getStatKey(int slot, int baseSlot) {
        String[] stats = {"STR", "AGI", "VIT", "INT", "DEX", "LUK"};
        int index = slot - baseSlot;
        if (index >= 0 && index < stats.length) return stats[index];
        return null;
    }

    private void handleStatUpgrade(Player player, String stat, boolean isLeft, boolean isRight) {
        if (isLeft) {
            if (plugin.getStatManager().upgradeStat(player, stat)) {
                openGUI(player, Tab.BASIC_INFO);
            } else {
                player.sendMessage("§cNot enough stat points!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        } else if (isRight) {
            PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
            int current = data.getStat(stat);
            int pending = data.getPendingStat(stat);
            int maxAdd = 99 - current - pending;
            int pointsAvailable = data.getStatPoints() - plugin.getStatManager().getTotalPendingCost(data);
            int canAdd = 0;
            int costSum = 0;
            for (int i = 0; i < maxAdd; i++) {
                int cost = plugin.getStatManager().getStatCost(current + pending + i);
                if (costSum + cost <= pointsAvailable) {
                    costSum += cost;
                    canAdd++;
                } else break;
            }
            if (canAdd > 0) {
                data.setPendingStat(stat, pending + canAdd);
                openGUI(player, Tab.BASIC_INFO);
            } else {
                player.sendMessage("§cCannot add more!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            }
        }
    }

    private void handleStatDowngrade(Player player, String stat, boolean isLeft, boolean isRight) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        int pending = data.getPendingStat(stat);
        if (isLeft && pending > 0) {
            data.setPendingStat(stat, pending - 1);
            openGUI(player, Tab.BASIC_INFO);
        } else if (isRight && pending > 0) {
            data.setPendingStat(stat, 0);
            openGUI(player, Tab.BASIC_INFO);
        }
    }

    private void runSync(Runnable task) {
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    private File findFileByName(File root, String fileName) {
        if (root.getName().equals(fileName)) return root;
        File[] files = root.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.getName().equals(fileName)) return file;
            if (file.isDirectory()) {
                File found = findFileByName(file, fileName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String extractKey(String loreText) {
        if (loreText.contains(": ")) {
            return loreText.substring(0, loreText.indexOf(": ")).replace("§7", "").trim();
        }
        return null;
    }

    private Object parseValue(String str) {
        try {
            if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
                return Boolean.parseBoolean(str);
            }
            if (str.contains(".")) {
                return Double.parseDouble(str);
            }
            return Integer.parseInt(str);
        } catch (Exception e) {
            return str;
        }
    }

    private SkillAction createDefaultAction(ActionType type) {
        return switch (type) {
            case DAMAGE -> new DamageAction(10.0, 5.0, "PHYSICAL");
            case HEAL -> new HealAction(10.0, 5.0);
            case APPLY_EFFECT -> new EffectAction("buff", EffectType.BUFF, 100, 1.0, null);
            case SOUND -> new SoundAction("ENTITY_PLAYER_LEVELUP", 1.0f, 1.0f);
            case PARTICLE -> new ParticleAction("FLAME", 10, 0.5, 0.5, 0.5, 0.1);
            case PROJECTILE -> new ProjectileAction("ARROW", 1.5);
            case AREA_EFFECT -> new AreaAction(5.0, "area_damage");
            case VELOCITY -> new VelocityAction(0.0, 1.0, 0.0, 1.0);
            case COMMAND -> new CommandAction("say Hello", false);
            case DELAY -> new DelayAction(20L);
            case TELEPORT -> new TeleportAction(0.0, 64.0, 0.0, null);
            case APPLY_POTION -> new PotionAction("SPEED", 100, 0);
            case RAYCAST -> new RaycastAction(10.0, null);
            case SPAWN_ENTITY -> new SpawnEntityAction("ZOMBIE");
            case LOOP -> new LoopAction(3, 20L, List.of());
        };
    }

    private SkillAction deserializeAction(ActionType type, Map<String, Object> data) {
        try {
            return switch (type) {
                case DAMAGE -> {
                    double base = ((Number) data.getOrDefault("base-damage", 10.0)).doubleValue();
                    double perLevel = ((Number) data.getOrDefault("damage-per-level", 0.0)).doubleValue();
                    String damageType = (String) data.getOrDefault("damage-type", "PHYSICAL");
                    yield new DamageAction(base, perLevel, damageType);
                }
                case HEAL -> {
                    double base = ((Number) data.getOrDefault("base-heal", 10.0)).doubleValue();
                    double perLevel = ((Number) data.getOrDefault("heal-per-level", 0.0)).doubleValue();
                    yield new HealAction(base, perLevel);
                }
                case APPLY_EFFECT -> {
                    String effectId = (String) data.getOrDefault("effect-id", "buff");
                    EffectType effectType = EffectType.valueOf((String) data.getOrDefault("effect-type", "BUFF"));
                    int duration = ((Number) data.getOrDefault("duration", 100)).intValue();
                    double power = ((Number) data.getOrDefault("power", 1.0)).doubleValue();
                    String statKey = (String) data.get("stat-key");
                    yield new EffectAction(effectId, effectType, duration, power, statKey);
                }
                case SOUND -> {
                    String sound = (String) data.getOrDefault("sound", "ENTITY_PLAYER_LEVELUP");
                    float volume = ((Number) data.getOrDefault("volume", 1.0)).floatValue();
                    float pitch = ((Number) data.getOrDefault("pitch", 1.0)).floatValue();
                    yield new SoundAction(sound, volume, pitch);
                }
                case PARTICLE -> {
                    String particle = (String) data.getOrDefault("particle", "FLAME");
                    int count = ((Number) data.getOrDefault("count", 10)).intValue();
                    double offsetX = ((Number) data.getOrDefault("offset-x", 0.5)).doubleValue();
                    double offsetY = ((Number) data.getOrDefault("offset-y", 0.5)).doubleValue();
                    double offsetZ = ((Number) data.getOrDefault("offset-z", 0.5)).doubleValue();
                    double speed = ((Number) data.getOrDefault("speed", 0.1)).doubleValue();
                    yield new ParticleAction(particle, count, offsetX, offsetY, offsetZ, speed);
                }
                default -> null;
            };
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}