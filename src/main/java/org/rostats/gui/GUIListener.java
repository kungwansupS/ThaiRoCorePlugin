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
        if (skill == null || index >= skill.getActions().size()) { player.closeInventory(); return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        Map<String, Object> tempMap = editingActions.getOrDefault(player.getUniqueId(), new HashMap<>());
        if (tempMap.isEmpty()) {
            tempMap = new HashMap<>(skill.getActions().get(index).serialize());
            editingActions.put(player.getUniqueId(), tempMap);
        }
        final Map<String, Object> data = tempMap;

        if (clicked.getType() == Material.RED_CONCRETE) {
            editingActions.remove(player.getUniqueId());
            new SkillEditorGUI(plugin, skillId).open(player);
            return;
        }

        if (clicked.getType() == Material.EMERALD_BLOCK) {
            try {
                String typeStr = (String) data.get("type");
                ActionType type = ActionType.valueOf(typeStr);
                SkillAction newAction = null;

                switch (type) {
                    case DAMAGE: newAction = new DamageAction(plugin, String.valueOf(data.getOrDefault("formula","ATK")), String.valueOf(data.getOrDefault("element","NEUTRAL"))); break;
                    case HEAL:
                        boolean hSelf = Boolean.parseBoolean(String.valueOf(data.getOrDefault("self-only", true)));
                        boolean isMana = Boolean.parseBoolean(String.valueOf(data.getOrDefault("is-mana", false)));
                        newAction = new HealAction(plugin, String.valueOf(data.getOrDefault("formula","10")), isMana, hSelf);
                        break;
                    case APPLY_EFFECT:
                        String eid = String.valueOf(data.getOrDefault("effect-id", "unknown"));
                        EffectType et = EffectType.valueOf(String.valueOf(data.getOrDefault("effect-type", "STAT_MODIFIER")));
                        int lv = Integer.parseInt(String.valueOf(data.getOrDefault("level", "1")));
                        double pw = Double.parseDouble(String.valueOf(data.getOrDefault("power", "0.0")));
                        long dr = Long.parseLong(String.valueOf(data.getOrDefault("duration", "100")));
                        double ch = Double.parseDouble(String.valueOf(data.getOrDefault("chance", "1.0")));
                        String sk = (String)data.getOrDefault("stat-key", null);
                        if(sk != null && sk.equals("None")) sk = null;
                        newAction = new EffectAction(plugin, eid, et, lv, pw, dr, ch, sk);
                        break;
                    case SOUND:
                        String snd = String.valueOf(data.getOrDefault("sound", "ENTITY_EXPERIENCE_ORB_PICKUP"));
                        float vol = Float.parseFloat(String.valueOf(data.getOrDefault("volume", "1.0")));
                        float pit = Float.parseFloat(String.valueOf(data.getOrDefault("pitch", "1.0")));
                        newAction = new SoundAction(snd, vol, pit);
                        break;
                    case PARTICLE:
                        String par = String.valueOf(data.getOrDefault("particle", "VILLAGER_HAPPY"));
                        int cnt = Integer.parseInt(String.valueOf(data.getOrDefault("count", "5")));
                        double spd = Double.parseDouble(String.valueOf(data.getOrDefault("speed", "0.1")));
                        double off = Double.parseDouble(String.valueOf(data.getOrDefault("offset", "0.5")));
                        newAction = new ParticleAction(par, cnt, spd, off);
                        break;
                    case POTION:
                        String pot = String.valueOf(data.getOrDefault("potion", "SPEED"));
                        int dur = Integer.parseInt(String.valueOf(data.getOrDefault("duration", "60")));
                        int amp = Integer.parseInt(String.valueOf(data.getOrDefault("amplifier", "0")));
                        boolean pSelf = Boolean.parseBoolean(String.valueOf(data.getOrDefault("self-only", true)));
                        newAction = new PotionAction(pot, dur, amp, pSelf);
                        break;
                    case TELEPORT:
                        double rng = Double.parseDouble(String.valueOf(data.getOrDefault("range", "5.0")));
                        boolean tgt = Boolean.parseBoolean(String.valueOf(data.getOrDefault("to-target", false)));
                        newAction = new TeleportAction(rng, tgt);
                        break;
                    case PROJECTILE:
                        String proj = String.valueOf(data.getOrDefault("projectile", "ARROW"));
                        double pSpd = Double.parseDouble(String.valueOf(data.getOrDefault("speed", "1.0")));
                        String hitSkill = String.valueOf(data.getOrDefault("on-hit", "none"));
                        newAction = new ProjectileAction(plugin, proj, pSpd, hitSkill);
                        break;
                    case AREA_EFFECT:
                        double rad = Double.parseDouble(String.valueOf(data.getOrDefault("radius", "5.0")));
                        String tType = String.valueOf(data.getOrDefault("target-type", "ENEMY"));
                        String sub = String.valueOf(data.getOrDefault("sub-skill", "none"));
                        int maxT = Integer.parseInt(String.valueOf(data.getOrDefault("max-targets", "10")));
                        newAction = new AreaAction(plugin, rad, tType, sub, maxT);
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
                e.printStackTrace();
            }
            editingActions.remove(player.getUniqueId());
            new SkillEditorGUI(plugin, skillId).open(player);
            return;
        }

        List<String> lore = clicked.getItemMeta().getLore();
        String key = null;
        if (lore != null) {
            for (String l : lore) {
                if (l.startsWith("§0Key:")) { key = l.substring(6); break; }
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
                        if (fKey.equals("level") || fKey.equals("duration") || fKey.equals("count") || fKey.equals("amplifier") || fKey.equals("max-targets")) {
                            int intVal = Integer.parseInt(str);
                            if (intVal < 0) throw new NumberFormatException("Negative");
                            data.put(fKey, intVal);
                        } else if (fKey.equals("power") || fKey.equals("chance") || fKey.equals("speed") || fKey.equals("offset") || fKey.equals("range") || fKey.equals("volume") || fKey.equals("pitch") || fKey.equals("radius")) {
                            double dVal = Double.parseDouble(str);
                            if (dVal < 0) throw new NumberFormatException("Negative");
                            data.put(fKey, dVal);
                        } else {
                            data.put(fKey, str);
                        }
                        runSync(() -> reopenPropertyGUI(player, skillId, index, data, skill.getActions().get(index).getType()));
                    } catch (Exception e) {
                        player.sendMessage("§cInvalid input! Must be a positive number.");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
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
        if (clicked.getType() == Material.ARROW) { new SkillEditorGUI(plugin, skillId).open(player); return; }

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
                        case HEAL: action = new HealAction(plugin, "10", false, true); break;
                        case APPLY_EFFECT: action = new EffectAction(plugin, "unknown", EffectType.STAT_MODIFIER, 1, 10, 100, 1.0, "STR"); break;
                        case SOUND: action = new SoundAction("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f); break;
                        case PARTICLE: action = new ParticleAction("VILLAGER_HAPPY", 5, 0.1, 0.5); break;
                        case POTION: action = new PotionAction("SPEED", 60, 0, true); break;
                        case TELEPORT: action = new TeleportAction(5.0, false); break;
                        case PROJECTILE: action = new ProjectileAction(plugin, "ARROW", 1.5, "none"); break;
                        case AREA_EFFECT: action = new AreaAction(plugin, 5.0, "ENEMY", "none", 10); break;
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
                List<String> lore = item.getItemMeta().getLore();
                int index = -1;
                if (lore != null) {
                    for (String l : lore) {
                        if (l.startsWith("§7Index: ")) {
                            try { index = Integer.parseInt(l.substring(9)); } catch (Exception e) {}
                            break;
                        }
                    }
                }

                if (index != -1 && index < skill.getActions().size()) {
                    // [UPDATED] Reordering Logic
                    if (event.isShiftClick() && event.isRightClick()) {
                        // Remove
                        skill.getActions().remove(index);
                        plugin.getSkillManager().saveSkill(skill);
                        new SkillEditorGUI(plugin, skillId).open(player);
                    } else if (event.isShiftClick() && event.isLeftClick()) {
                        // Move Up/Left (Index - 1)
                        if (index > 0) {
                            Collections.swap(skill.getActions(), index, index - 1);
                            plugin.getSkillManager().saveSkill(skill);
                            new SkillEditorGUI(plugin, skillId).open(player);
                        }
                    } else if (event.isRightClick()) {
                        // Move Down/Right (Index + 1)
                        if (index < skill.getActions().size() - 1) {
                            Collections.swap(skill.getActions(), index, index + 1);
                            plugin.getSkillManager().saveSkill(skill);
                            new SkillEditorGUI(plugin, skillId).open(player);
                        }
                    } else if (event.isLeftClick()) {
                        // Edit
                        editingActions.put(player.getUniqueId(), new HashMap<>(skill.getActions().get(index).serialize()));
                        new SkillActionPropertyGUI(plugin, skillId, index, skill.getActions().get(index)).open(player);
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
        // [NEW] Slot 7: Required Level
        else if (slot == 7) {
            plugin.getChatInputHandler().awaitInput(player, "Required Level:", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    if (lvl < 1) lvl = 1;
                    skill.setRequiredLevel(lvl);
                } catch (Exception e) {}
                runSync(() -> new SkillEditorGUI(plugin, skillId).open(player));
            });
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

    private void runSync(Runnable r) {
        plugin.getServer().getScheduler().runTask(plugin, r);
    }
}