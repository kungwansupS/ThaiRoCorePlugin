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
import org.rostats.input.ChatInputHandler;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, List<SkillAction>> currentEditingList = new HashMap<>();
    private final Map<UUID, Map<String, Object>> editingProperties = new HashMap<>();
    private static final Map<UUID, Consumer<String>> selectionCallbacks = new HashMap<>();

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    public static void setSelectionCallback(Player p, Consumer<String> cb) {
        selectionCallbacks.put(p.getUniqueId(), cb);
    }

    private void openGUI(Player player, Tab tab) {
        player.setMetadata("ROSTATS_SWITCH", new FixedMetadataValue(plugin, true));
        new CharacterGUI(plugin).open(player, tab);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getView().title() == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (player.hasMetadata("ROSTATS_SWITCH")) {
            player.removeMetadata("ROSTATS_SWITCH", plugin);
            return;
        }

        if (!player.hasMetadata("ROSTATS_SWITCH")) {
            selectionCallbacks.remove(player.getUniqueId());
        }

        if (!title.contains(CharacterGUI.TITLE_HEADER)) return;

        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        if (plugin.getStatManager().getTotalPendingCost(data) > 0) {
            data.clearAllPendingStats();
            plugin.getAttributeHandler().updatePlayerStats(player);
            plugin.getManaManager().updateBar(player);
            player.sendMessage("§e[System] Cancelled pending stats.");
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getView().title() == null) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (title.contains(CharacterGUI.TITLE_HEADER) || title.startsWith("Skill") || title.startsWith("Action") || title.startsWith("Folder:")) {
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                event.setCancelled(true);
                return;
            }
        }

        if (title.equals("Skill Library") || title.startsWith("Folder:")) {
            event.setCancelled(true);
            handleNewLibraryClick(event, player, title);
            return;
        }

        if (title.startsWith("Skill Delete: ")) {
            event.setCancelled(true);
            handleSkillDeleteClick(event, player, title);
            return;
        }
        if (title.startsWith("SkillEditor: ")) {
            event.setCancelled(true);
            String[] parts = title.substring(13).split(" #P");
            String skillId = parts[0];
            int page = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            handleSkillEditorClick(event, player, skillId, page);
            return;
        }
        if (title.startsWith("ActionSelector: ")) {
            event.setCancelled(true);
            handleActionSelectorClick(event, player, title.substring(16));
            return;
        }
        if (title.startsWith("ActionEdit: ")) {
            event.setCancelled(true);
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

    private void handleNewLibraryClick(InventoryClickEvent event, Player player, String title) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // 1. Enter Folder
        if (clicked.getType() == Material.CHEST && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("📂")) {
            String fileName = clicked.getItemMeta().getDisplayName().replace("§6📂 ", "");
            File folderFile = new File(plugin.getSkillManager().getRootDir(), fileName);
            new SkillLibraryGUI(plugin, folderFile).open(player);
            return;
        }

        // 2. Back Button
        if (clicked.getType() == Material.ARROW && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("BACK")) {
            // [FIX] ใช้ Constructor แบบเต็ม (2 ค่า) เสมอ เพื่อกัน Error
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
            return;
        }

        // 3. Create New File
        if (clicked.getType() == Material.EMERALD_BLOCK) {
            player.sendMessage("§ePlease type the new skill file name in chat:");
            player.closeInventory();
            plugin.getChatInputHandler().awaitInput(player, "File Name:", (input) -> {
                plugin.getSkillManager().createSkill(plugin.getSkillManager().getRootDir(), input);
                runSync(() -> new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player));
            });
            return;
        }

        // 4. Click Skill Item
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            List<String> lore = clicked.getItemMeta().getLore();
            if (lore != null && !lore.isEmpty()) {
                String idLine = lore.get(0);
                if (idLine.startsWith("§8ID: ")) {
                    String skillId = idLine.replace("§8ID: ", "");

                    if (selectionCallbacks.containsKey(player.getUniqueId())) {
                        Consumer<String> callback = selectionCallbacks.remove(player.getUniqueId());
                        player.sendMessage("§aSelected skill: " + skillId);
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                        player.closeInventory();
                        callback.accept(skillId);
                        return;
                    }

                    if (event.isRightClick()) {
                        player.sendMessage("§cTo delete, please delete the file manually for now.");
                    } else {
                        // [FIX] ใช้ Constructor แบบเต็ม (3 ค่า) เสมอ
                        new SkillEditorGUI(plugin, skillId, 0).open(player);
                    }
                }
            }
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
            player.sendMessage("§cFile Deleted.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
            runSync(() -> new SkillLibraryGUI(plugin, parent).open(player));
        } else if (clicked.getType() == Material.RED_CONCRETE) {
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
        }
    }

    private void handleSkillEditorClick(InventoryClickEvent event, Player player, String skillId, int page) {
        SkillData rootSkill = plugin.getSkillManager().getSkill(skillId);
        if (rootSkill == null) { player.closeInventory(); return; }

        List<SkillAction> activeList = currentEditingList.getOrDefault(player.getUniqueId(), rootSkill.getActions());
        int slot = event.getSlot();

        if (slot == 45) { if (page > 0) refreshGUI(player, skillId, page - 1); return; }
        if (slot == 53) { if ((page + 1) * 27 < activeList.size()) refreshGUI(player, skillId, page + 1); return; }

        if (slot == 48) { // Back
            boolean isNested = currentEditingList.containsKey(player.getUniqueId());
            if (!isNested) {
                // [FIX] ใช้ Constructor แบบเต็ม
                new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
            } else {
                currentEditingList.remove(player.getUniqueId());
                new SkillEditorGUI(plugin, skillId, 0).open(player);
            }
            return;
        }

        if (slot == 49) { // Save
            plugin.getSkillManager().saveSkill(rootSkill);
            player.sendMessage("§aSkill Structure Saved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            return;
        }
        if (slot == 50) { // Add Action
            new SkillActionSelectorGUI(plugin, skillId).open(player);
            return;
        }

        if (slot >= 18 && slot <= 44) {
            int index = (page * 27) + (slot - 18);
            if (index < activeList.size()) {
                SkillAction action = activeList.get(index);

                if (action instanceof ConditionAction) {
                    ConditionAction cond = (ConditionAction) action;
                    if (event.isRightClick()) {
                        if (event.isShiftClick()) {
                            currentEditingList.put(player.getUniqueId(), cond.getFailActions());
                            new SkillEditorGUI(plugin, skillId, cond.getFailActions(), 0, "Condition -> FAIL").open(player);
                        } else {
                            currentEditingList.put(player.getUniqueId(), cond.getSuccessActions());
                            new SkillEditorGUI(plugin, skillId, cond.getSuccessActions(), 0, "Condition -> SUCCESS").open(player);
                        }
                        return;
                    }
                }

                if (action instanceof LoopAction) {
                    LoopAction loop = (LoopAction) action;
                    if (event.isRightClick() && !event.isShiftClick()) {
                        currentEditingList.put(player.getUniqueId(), loop.getSubActions());
                        new SkillEditorGUI(plugin, skillId, loop.getSubActions(), 0, "Loop -> Body").open(player);
                        return;
                    }
                }

                if (event.isRightClick() && !event.isShiftClick()) {
                    if (index < activeList.size() - 1) {
                        Collections.swap(activeList, index, index + 1);
                        refreshGUI(player, skillId, page);
                    }
                }
                else if (event.isLeftClick() && event.isShiftClick()) {
                    if (index > 0) {
                        Collections.swap(activeList, index, index - 1);
                        refreshGUI(player, skillId, page);
                    }
                }
                else if (event.isRightClick() && event.isShiftClick()) {
                    activeList.remove(index);
                    refreshGUI(player, skillId, page);
                }
                else if (event.isLeftClick()) {
                    editingProperties.put(player.getUniqueId(), new HashMap<>(action.serialize()));
                    new SkillActionPropertyGUI(plugin, skillId, index, action).open(player);
                }
            }
            return;
        }

        if (!currentEditingList.containsKey(player.getUniqueId())) {
            handleMetaDataEdit(event, player, skillId, page, rootSkill);
        }
    }

    private void refreshGUI(Player player, String skillId, int page) {
        SkillData root = plugin.getSkillManager().getSkill(skillId);
        List<SkillAction> list = currentEditingList.getOrDefault(player.getUniqueId(), root.getActions());
        String name = currentEditingList.containsKey(player.getUniqueId()) ? "Nested List" : "Main";
        new SkillEditorGUI(plugin, skillId, list, page, name).open(player);
    }

    private void handleActionSelectorClick(InventoryClickEvent event, Player player, String skillId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        if (clicked.getType() == Material.ARROW || clicked.getType() == Material.BOOK) {
            refreshGUI(player, skillId, 0);
            return;
        }

        List<String> lore = clicked.getItemMeta().getLore();
        if (lore != null) {
            String typeStr = lore.stream().filter(l -> l.startsWith("ActionType: ")).findFirst().orElse(null);

            if (typeStr != null) {
                try {
                    ActionType type = ActionType.valueOf(typeStr.substring(12));
                    SkillAction action = createDefaultAction(type);

                    if (action != null) {
                        SkillData root = plugin.getSkillManager().getSkill(skillId);
                        List<SkillAction> activeList = currentEditingList.getOrDefault(player.getUniqueId(), root.getActions());
                        activeList.add(action);
                        int lastPage = Math.max(0, (activeList.size() - 1) / 27);
                        refreshGUI(player, skillId, lastPage);
                    }
                } catch (Exception e) {
                    player.sendMessage("§cError: " + e.getMessage());
                }
            }
        }
    }

    private SkillAction createDefaultAction(ActionType type) {
        switch(type) {
            case DAMAGE: return new DamageAction(plugin, "ATK * 1.0", "NEUTRAL");
            case HEAL: return new HealAction(plugin, "10", false, true);
            case CONDITION: return new ConditionAction(plugin, "hp < 50", new ArrayList<>(), new ArrayList<>());
            case SET_VARIABLE: return new SetVariableAction(plugin, "temp", "1");
            case LOOP: return new LoopAction(plugin, "0", "5", "1", "i", new ArrayList<>());
            case SELECT_TARGET: return new TargetSelectorAction(TargetSelectorAction.SelectorMode.SELF, 10.0);
            case SOUND: return new SoundAction("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f);
            case APPLY_EFFECT: return new EffectAction(plugin, "unknown", EffectType.STAT_MODIFIER, 1, 10, 100, 1.0, "STR");
            case PARTICLE: return new ParticleAction(plugin, "VILLAGER_HAPPY", "5", "0.1", "POINT", "0.5", "20", "0,0,0", "0,0,0", "0,0,0");
            case POTION: return new PotionAction("SPEED", 60, 0, true);
            case TELEPORT: return new TeleportAction(5.0, false);
            case PROJECTILE: return new ProjectileAction(plugin, "ARROW", 1.5, "none");
            case AREA_EFFECT: return new AreaAction(plugin, 5.0, "ENEMY", "none", 10);
            case VELOCITY: return new VelocityAction(0.0, 0.0, 0.0, true);
            case COMMAND: return new CommandAction("say Hi", false);
            case RAYCAST: return new RaycastAction(plugin, "10.0", "none", "SINGLE");
            case SPAWN_ENTITY: return new SpawnEntityAction(plugin, "LIGHTNING_BOLT", "none");
            default: return null;
        }
    }

    private void handleActionPropertyClick(InventoryClickEvent event, Player player, String skillId, int index) {
        SkillData root = plugin.getSkillManager().getSkill(skillId);
        List<SkillAction> activeList = currentEditingList.getOrDefault(player.getUniqueId(), root.getActions());

        if (index >= activeList.size()) { player.closeInventory(); return; }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        Map<String, Object> data = editingProperties.get(player.getUniqueId());
        if (data == null) {
            data = new HashMap<>(activeList.get(index).serialize());
            editingProperties.put(player.getUniqueId(), data);
        }

        if (clicked.getType() == Material.RED_CONCRETE) {
            editingProperties.remove(player.getUniqueId());
            refreshGUI(player, skillId, index / 27);
            return;
        }

        if (clicked.getType() == Material.EMERALD_BLOCK) {
            try {
                String typeStr = (String) data.get("type");
                ActionType type = ActionType.valueOf(typeStr);
                SkillAction newAction = reconstructAction(type, data, activeList.get(index));

                if (newAction != null) {
                    activeList.set(index, newAction);
                    player.sendMessage("§aProperty Updated!");
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                }
            } catch (Exception e) {
                player.sendMessage("§cError: " + e.getMessage());
                e.printStackTrace();
            }
            editingProperties.remove(player.getUniqueId());
            refreshGUI(player, skillId, index / 27);
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
            final Map<String, Object> fData = data;
            Object val = fData.get(fKey);

            if (val instanceof Boolean) {
                fData.put(fKey, !((Boolean) val));
                reopenPropertyGUI(player, skillId, index, fData, activeList.get(index).getType());
            } else {
                plugin.getChatInputHandler().awaitInput(player, "Enter " + key + ":", (str) -> {
                    try {
                        if (isIntegerKey(fKey)) fData.put(fKey, Integer.parseInt(str));
                        else if (isDoubleKey(fKey)) fData.put(fKey, Double.parseDouble(str));
                        else if (fKey.equals("mode")) fData.put(fKey, str.toUpperCase());
                        else fData.put(fKey, str);

                        runSync(() -> reopenPropertyGUI(player, skillId, index, fData, activeList.get(index).getType()));
                    } catch (Exception e) {
                        player.sendMessage("§cInvalid format!");
                        runSync(() -> reopenPropertyGUI(player, skillId, index, fData, activeList.get(index).getType()));
                    }
                });
            }
        }
    }

    private SkillAction reconstructAction(ActionType type, Map<String, Object> data, SkillAction oldAction) {
        switch(type) {
            case SELECT_TARGET:
                return new TargetSelectorAction(
                        TargetSelectorAction.SelectorMode.valueOf(String.valueOf(data.getOrDefault("mode", "SELF"))),
                        Double.parseDouble(String.valueOf(data.getOrDefault("radius", "10.0")))
                );
            case DAMAGE: return new DamageAction(plugin, String.valueOf(data.getOrDefault("formula","ATK")), String.valueOf(data.getOrDefault("element","NEUTRAL")));
            case HEAL: return new HealAction(plugin, String.valueOf(data.getOrDefault("formula","10")), Boolean.parseBoolean(String.valueOf(data.getOrDefault("is-mana", false))), Boolean.parseBoolean(String.valueOf(data.getOrDefault("self-only", true))));
            case CONDITION:
                ConditionAction oldCond = (oldAction instanceof ConditionAction) ? (ConditionAction)oldAction : null;
                List<SkillAction> success = (oldCond != null) ? oldCond.getSuccessActions() : new ArrayList<>();
                List<SkillAction> fail = (oldCond != null) ? oldCond.getFailActions() : new ArrayList<>();
                return new ConditionAction(plugin, String.valueOf(data.getOrDefault("formula", "true")), success, fail);
            case SET_VARIABLE: return new SetVariableAction(plugin, String.valueOf(data.getOrDefault("var", "temp")), String.valueOf(data.getOrDefault("val", "0")));
            case LOOP:
                LoopAction oldLoop = (oldAction instanceof LoopAction) ? (LoopAction)oldAction : null;
                List<SkillAction> sub = (oldLoop != null) ? oldLoop.getSubActions() : new ArrayList<>();
                return new LoopAction(plugin, String.valueOf(data.getOrDefault("start","0")), String.valueOf(data.getOrDefault("end","5")), String.valueOf(data.getOrDefault("step","1")), String.valueOf(data.getOrDefault("var","i")), sub);
            case SOUND: return new SoundAction(String.valueOf(data.get("sound")), Float.parseFloat(String.valueOf(data.get("volume"))), Float.parseFloat(String.valueOf(data.get("pitch"))));
            case APPLY_EFFECT: return new EffectAction(plugin, String.valueOf(data.get("effect-id")), EffectType.valueOf(String.valueOf(data.get("effect-type"))), Integer.parseInt(String.valueOf(data.get("level"))), Double.parseDouble(String.valueOf(data.get("power"))), Long.parseLong(String.valueOf(data.get("duration"))), Double.parseDouble(String.valueOf(data.get("chance"))), (String)data.get("stat-key"));
            case PARTICLE:
                return new ParticleAction(plugin,
                        String.valueOf(data.get("particle")),
                        String.valueOf(data.get("count")),
                        String.valueOf(data.get("speed")),
                        String.valueOf(data.get("shape")),
                        String.valueOf(data.get("radius")),
                        String.valueOf(data.get("points")),
                        String.valueOf(data.getOrDefault("color", "0,0,0")),
                        String.valueOf(data.getOrDefault("rotation", "0,0,0")),
                        String.valueOf(data.getOrDefault("offset", "0,0,0"))
                );
            case POTION: return new PotionAction(String.valueOf(data.get("potion")), Integer.parseInt(String.valueOf(data.get("duration"))), Integer.parseInt(String.valueOf(data.get("amplifier"))), Boolean.parseBoolean(String.valueOf(data.get("self-only"))));
            case TELEPORT: return new TeleportAction(Double.parseDouble(String.valueOf(data.get("range"))), Boolean.parseBoolean(String.valueOf(data.get("to-target"))));
            case PROJECTILE: return new ProjectileAction(plugin, String.valueOf(data.get("projectile")), Double.parseDouble(String.valueOf(data.get("speed"))), String.valueOf(data.get("on-hit")));
            case AREA_EFFECT: return new AreaAction(plugin, Double.parseDouble(String.valueOf(data.get("radius"))), String.valueOf(data.get("target-type")), String.valueOf(data.get("sub-skill")), Integer.parseInt(String.valueOf(data.get("max-targets"))));
            case VELOCITY: return new VelocityAction(Double.parseDouble(String.valueOf(data.get("x"))), Double.parseDouble(String.valueOf(data.get("y"))), Double.parseDouble(String.valueOf(data.get("z"))), Boolean.parseBoolean(String.valueOf(data.get("add"))));
            case COMMAND: return new CommandAction(String.valueOf(data.get("command")), Boolean.parseBoolean(String.valueOf(data.get("as-console"))));
            case RAYCAST: return new RaycastAction(plugin, String.valueOf(data.get("range")), String.valueOf(data.get("sub-skill")), String.valueOf(data.get("target-type")));
            case SPAWN_ENTITY: return new SpawnEntityAction(plugin, String.valueOf(data.get("entity-type")), String.valueOf(data.get("skill-id")));
            default: return oldAction;
        }
    }

    private boolean isIntegerKey(String key) {
        return key.equals("level") || key.equals("duration") || key.equals("count") || key.equals("amplifier") || key.equals("max-targets");
    }
    private boolean isDoubleKey(String key) {
        return key.equals("power") || key.equals("chance") || key.equals("speed") || key.equals("offset") || key.equals("range") || key.equals("volume") || key.equals("pitch") || key.equals("radius") || key.equals("x") || key.equals("y") || key.equals("z");
    }

    private void reopenPropertyGUI(Player player, String skillId, int index, Map<String, Object> data, ActionType type) {
        SkillAction tempAction = new SkillAction() {
            public ActionType getType() { return type; }
            public void execute(org.bukkit.entity.LivingEntity c, org.bukkit.entity.LivingEntity t, int l, Map<String, Double> ctx) {}
            public Map<String, Object> serialize() { return data; }
        };
        new SkillActionPropertyGUI(plugin, skillId, index, tempAction).open(player);
    }

    private void handleMetaDataEdit(InventoryClickEvent event, Player player, String skillId, int page, SkillData skill) {
        int slot = event.getSlot();
        if (slot == 0) {
            plugin.getChatInputHandler().awaitInput(player, "Name:", (str) -> {
                skill.setDisplayName(str.replace("&", "§"));
                runSync(() -> refreshGUI(player, skillId, page));
            });
        }
        else if (slot == 1) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                skill.setIcon(cursor.getType());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.setCursor(cursor);
                refreshGUI(player, skillId, page);
            }
        }
        else if (slot == 2) {
            String[] types = {"PHYSICAL", "MAGIC", "MIXED", "HEAL", "SUPPORT"};
            String current = skill.getSkillType();
            int idx = 0; for(int i=0; i<types.length; i++) if(types[i].equals(current)) idx=i;
            skill.setSkillType(types[(idx+1)%types.length]);
            refreshGUI(player, skillId, page);
        }
        else if (slot == 3) {
            skill.setAttackType(skill.getAttackType().equals("MELEE") ? "RANGED" : "MELEE");
            refreshGUI(player, skillId, page);
        }
        else if (slot == 4) {
            plugin.getChatInputHandler().awaitInput(player, "Cast Range:", (str) -> {
                try { skill.setCastRange(Double.parseDouble(str)); } catch(Exception e){}
                runSync(() -> refreshGUI(player, skillId, page));
            });
        }
        else if (slot == 5) {
            TriggerType[] types = TriggerType.values();
            skill.setTrigger(types[(skill.getTrigger().ordinal() + 1) % types.length]);
            refreshGUI(player, skillId, page);
        }
        else if (slot == 6) {
            if(event.isLeftClick()) plugin.getChatInputHandler().awaitInput(player, "Base CD:", (str)->{try{skill.setCooldownBase(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
            else plugin.getChatInputHandler().awaitInput(player, "CD Per Lvl:", (str)->{try{skill.setCooldownPerLevel(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
        }
        else if (slot == 7) {
            plugin.getChatInputHandler().awaitInput(player, "Req Level:", (str)->{try{skill.setRequiredLevel(Integer.parseInt(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
        }
        else if (slot == 8) {
            if(event.isLeftClick()) plugin.getChatInputHandler().awaitInput(player, "Base SP:", (str)->{try{skill.setSpCostBase(Integer.parseInt(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
            else plugin.getChatInputHandler().awaitInput(player, "SP Per Lvl:", (str)->{try{skill.setSpCostPerLevel(Integer.parseInt(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
        }
        // Row 2
        else if (slot == 10) {
            if(event.isLeftClick()) plugin.getChatInputHandler().awaitInput(player, "Var Cast:", (str)->{try{skill.setVariableCastTime(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
            else plugin.getChatInputHandler().awaitInput(player, "Reduct %:", (str)->{try{skill.setVariableCastTimeReduction(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
        }
        else if (slot == 11) {
            plugin.getChatInputHandler().awaitInput(player, "Fixed Cast:", (str)->{try{skill.setFixedCastTime(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
        }
        else if (slot == 12) {
            if(event.isLeftClick()) plugin.getChatInputHandler().awaitInput(player, "Pre Motion:", (str)->{try{skill.setPreMotion(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
            else plugin.getChatInputHandler().awaitInput(player, "Post Motion:", (str)->{try{skill.setPostMotion(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
        }
        else if (slot == 13) {
            plugin.getChatInputHandler().awaitInput(player, "ACD:", (str)->{try{skill.setAfterCastDelayBase(Double.parseDouble(str));}catch(Exception e){} runSync(()->refreshGUI(player, skillId, page));});
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