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

        // Prevent clicking in bottom inventory while in GUI
        if (title.contains(CharacterGUI.TITLE_HEADER) || title.startsWith("Skill")) {
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                event.setCancelled(true);
                return;
            }
        }

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
            // Parse: "SkillEditor: <ID> #P<Page>"
            String raw = title.substring(13);
            String skillId;
            int page = 0;
            if (raw.contains(" #P")) {
                String[] parts = raw.split(" #P");
                skillId = parts[0];
                try {
                    page = Integer.parseInt(parts[1]);
                } catch (NumberFormatException e) {
                    page = 0;
                }
            } else {
                skillId = raw;
            }
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

        // Calculate Page for return
        int page = index / 27;

        if (clicked.getType() == Material.RED_CONCRETE) {
            editingActions.remove(player.getUniqueId());
            new SkillEditorGUI(plugin, skillId, page).open(player);
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
                        newAction = new ParticleAction(plugin,
                                (String)data.getOrDefault("particle", "VILLAGER_HAPPY"),
                                String.valueOf(data.getOrDefault("count", "5")),
                                String.valueOf(data.getOrDefault("speed", "0.1")),
                                (String)data.getOrDefault("shape", "POINT"),
                                String.valueOf(data.getOrDefault("radius", "0.5")),
                                String.valueOf(data.getOrDefault("points", "20"))
                        );
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
                    case VELOCITY:
                        double vx = Double.parseDouble(String.valueOf(data.getOrDefault("x", "0.0")));
                        double vy = Double.parseDouble(String.valueOf(data.getOrDefault("y", "0.0")));
                        double vz = Double.parseDouble(String.valueOf(data.getOrDefault("z", "0.0")));
                        boolean add = Boolean.parseBoolean(String.valueOf(data.getOrDefault("add", true)));
                        newAction = new VelocityAction(vx, vy, vz, add);
                        break;
                    case LOOP:
                        String startExpr = String.valueOf(data.getOrDefault("start", "0"));
                        String endExpr = String.valueOf(data.getOrDefault("end", "10"));
                        String stepExpr = String.valueOf(data.getOrDefault("step", "1"));
                        String varName = String.valueOf(data.getOrDefault("var", "i"));
                        SkillAction originalAction = skill.getActions().get(index);
                        if (originalAction instanceof LoopAction loop) {
                            newAction = new LoopAction(plugin, startExpr, endExpr, stepExpr, varName, loop.getSubActions());
                        } else {
                            player.sendMessage("§cError: Cannot save LOOP without sub-actions.");
                            return;
                        }
                        break;
                    case COMMAND:
                        String command = String.valueOf(data.getOrDefault("command", "say Hi %player%"));
                        boolean console = Boolean.parseBoolean(String.valueOf(data.getOrDefault("as-console", false)));
                        newAction = new CommandAction(command, console);
                        break;
                    case RAYCAST:
                        String rangeExpr = String.valueOf(data.getOrDefault("range", "10.0"));
                        String subSkillId = String.valueOf(data.getOrDefault("sub-skill", "none"));
                        String targetType = String.valueOf(data.getOrDefault("target-type", "SINGLE"));
                        newAction = new RaycastAction(plugin, rangeExpr, subSkillId, targetType);
                        break;
                    case SPAWN_ENTITY:
                        String entityType = String.valueOf(data.getOrDefault("entity-type", "LIGHTNING_BOLT"));
                        String onSpawnSkill = String.valueOf(data.getOrDefault("skill-id", "none"));
                        newAction = new SpawnEntityAction(plugin, entityType, onSpawnSkill);
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
            new SkillEditorGUI(plugin, skillId, page).open(player);
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
                runSync(() -> reopenPropertyGUI(player, skillId, index, data, skill.getActions().get(index).getType()));
            } else {
                plugin.getChatInputHandler().awaitInput(player, "Enter value for " + fKey + ":", (str) -> {
                    try {
                        // Basic validation logic
                        if (fKey.equals("level") || fKey.equals("duration") || fKey.equals("count") || fKey.equals("amplifier") || fKey.equals("max-targets")) {
                            int intVal = Integer.parseInt(str);
                            if (intVal < 0) throw new NumberFormatException("Negative");
                            data.put(fKey, intVal);
                        } else if (fKey.equals("power") || fKey.equals("chance") || fKey.equals("speed") || fKey.equals("offset") || fKey.equals("range") || fKey.equals("volume") || fKey.equals("pitch") || fKey.equals("radius") || fKey.equals("x") || fKey.equals("y") || fKey.equals("z")) {
                            double dVal = Double.parseDouble(str);
                            if (dVal < 0 && !fKey.equals("x") && !fKey.equals("y") && !fKey.equals("z")) throw new NumberFormatException("Negative");
                            data.put(fKey, dVal);
                        } else {
                            data.put(fKey, str);
                        }
                        runSync(() -> reopenPropertyGUI(player, skillId, index, data, skill.getActions().get(index).getType()));
                    } catch (Exception e) {
                        player.sendMessage("§cInvalid input! Must be a valid number/string.");
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
            public void execute(org.bukkit.entity.LivingEntity c, org.bukkit.entity.LivingEntity t, int l, Map<String, Double> context) {}
            public Map<String, Object> serialize() { return data; }
        };
        new SkillActionPropertyGUI(plugin, skillId, index, tempAction).open(player);
    }

    private void handleActionSelectorClick(InventoryClickEvent event, Player player, String skillId) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        int lastPage = Math.max(0, (skill.getActions().size()) / 27); // New action goes to end, so last page

        if (clicked.getType() == Material.ARROW) {
            new SkillEditorGUI(plugin, skillId, lastPage).open(player);
            return;
        }

        List<String> lore = clicked.getItemMeta().getLore();
        if (lore != null && !lore.isEmpty()) {
            String last = lore.stream().filter(l -> l.startsWith("ActionType: ")).findFirst().orElse(null);
            if (last != null) {
                try {
                    ActionType type = ActionType.valueOf(last.substring(12));
                    SkillAction action = null;
                    switch (type) {
                        case DAMAGE: action = new DamageAction(plugin, "ATK * 1.0", "NEUTRAL"); break;
                        case HEAL: action = new HealAction(plugin, "10", false, true); break;
                        case APPLY_EFFECT: action = new EffectAction(plugin, "unknown", EffectType.STAT_MODIFIER, 1, 10, 100, 1.0, "STR"); break;
                        case SOUND: action = new SoundAction("ENTITY_EXPERIENCE_ORB_PICKUP", 1.0f, 1.0f); break;
                        case PARTICLE: action = new ParticleAction(plugin, "VILLAGER_HAPPY", "5", "0.1", "POINT", "0.5", "20"); break;
                        case POTION: action = new PotionAction("SPEED", 60, 0, true); break;
                        case TELEPORT: action = new TeleportAction(5.0, false); break;
                        case PROJECTILE: action = new ProjectileAction(plugin, "ARROW", 1.5, "none"); break;
                        case AREA_EFFECT: action = new AreaAction(plugin, 5.0, "ENEMY", "none", 10); break;
                        case VELOCITY: action = new VelocityAction(0.0, 0.0, 0.0, true); break;
                        case LOOP: action = new LoopAction(plugin, "0", "10", "1", "i", Collections.emptyList()); break;
                        case COMMAND: action = new CommandAction("say Hi %player%", false); break;
                        case RAYCAST: action = new RaycastAction(plugin, "10.0", "none", "SINGLE"); break;
                        case SPAWN_ENTITY: action = new SpawnEntityAction(plugin, "LIGHTNING_BOLT", "none"); break;

                        // New Types Support (Stub for future logic, prevents crash if clicked)
                        case CONDITION:
                        case SET_VARIABLE:
                        case SELECT_TARGET:
                            player.sendMessage("§eWork in Progress: Logic Action");
                            return;
                    }
                    if (action != null) {
                        skill.addAction(action);
                        player.sendMessage("§aAdded: " + type);
                        plugin.getSkillManager().saveSkill(skill);
                        // Recalculate page as size increased
                        lastPage = (skill.getActions().size() - 1) / 27;
                        new SkillEditorGUI(plugin, skillId, lastPage).open(player);
                    }
                } catch (Exception e) {
                    player.sendMessage("§cError creating action: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleSkillEditorClick(InventoryClickEvent event, Player player, String skillId, int page) {
        SkillData skill = plugin.getSkillManager().getSkill(skillId);
        if (skill == null) { player.closeInventory(); return; }
        int slot = event.getSlot();

        // --- Controls Row (45-53) ---
        if (slot == 45) { // Previous Page
            if (page > 0) {
                new SkillEditorGUI(plugin, skillId, page - 1).open(player);
            }
            return;
        }
        if (slot == 48) { // Back to Library
            new SkillLibraryGUI(plugin, plugin.getSkillManager().getRootDir()).open(player);
            return;
        }
        if (slot == 49) { // Save
            plugin.getSkillManager().saveSkill(skill);
            player.sendMessage("§aSkill saved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            return;
        }
        if (slot == 50) { // Add Action
            new SkillActionSelectorGUI(plugin, skillId).open(player);
            return;
        }
        if (slot == 53) { // Next Page
            if ((page + 1) * 27 < skill.getActions().size()) {
                new SkillEditorGUI(plugin, skillId, page + 1).open(player);
            }
            return;
        }

        // --- Action List (18-44) ---
        if (slot >= 18 && slot <= 44) {
            ItemStack item = event.getCurrentItem();
            if (item != null && item.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                int relativeIndex = slot - 18;
                int realIndex = (page * 27) + relativeIndex;

                if (realIndex < skill.getActions().size()) {
                    if (event.isShiftClick() && event.isRightClick()) {
                        // Remove
                        skill.getActions().remove(realIndex);
                        plugin.getSkillManager().saveSkill(skill);
                        new SkillEditorGUI(plugin, skillId, page).open(player);
                    } else if (event.isShiftClick() && event.isLeftClick()) {
                        // Move Up
                        if (realIndex > 0) {
                            Collections.swap(skill.getActions(), realIndex, realIndex - 1);
                            plugin.getSkillManager().saveSkill(skill);
                            new SkillEditorGUI(plugin, skillId, page).open(player);
                        }
                    } else if (event.isRightClick()) {
                        // Move Down
                        if (realIndex < skill.getActions().size() - 1) {
                            Collections.swap(skill.getActions(), realIndex, realIndex + 1);
                            plugin.getSkillManager().saveSkill(skill);
                            new SkillEditorGUI(plugin, skillId, page).open(player);
                        }
                    } else if (event.isLeftClick()) {
                        // Edit
                        editingActions.put(player.getUniqueId(), new HashMap<>(skill.getActions().get(realIndex).serialize()));
                        new SkillActionPropertyGUI(plugin, skillId, realIndex, skill.getActions().get(realIndex)).open(player);
                    }
                }
            }
            return;
        }

        // --- Meta Data (0-13) ---
        handleMetaDataEdit(event, player, skillId, page, skill);
    }

    private void handleMetaDataEdit(InventoryClickEvent event, Player player, String skillId, int page, SkillData skill) {
        int slot = event.getSlot();

        if (slot == 0) {
            plugin.getChatInputHandler().awaitInput(player, "Name:", (str) -> {
                skill.setDisplayName(str.replace("&", "§"));
                runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
            });
        }
        else if (slot == 1) { // Icon
            // Check Cursor for item
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                skill.setIcon(cursor.getType());
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                event.setCursor(cursor); // Return item to cursor? Or consume? Usually consume visual only.
                new SkillEditorGUI(plugin, skillId, page).open(player);
            }
        }
        else if (slot == 2) { // Skill Type
            String[] types = {"PHYSICAL", "MAGIC", "MIXED", "HEAL", "SUPPORT"};
            String current = skill.getSkillType();
            int idx = 0;
            for(int i=0; i<types.length; i++) if(types[i].equals(current)) idx = i;
            skill.setSkillType(types[(idx+1) % types.length]);
            new SkillEditorGUI(plugin, skillId, page).open(player);
        }
        else if (slot == 3) { // Attack Type
            String current = skill.getAttackType();
            skill.setAttackType(current.equals("MELEE") ? "RANGED" : "MELEE");
            new SkillEditorGUI(plugin, skillId, page).open(player);
        }
        else if (slot == 4) { // Cast Range
            plugin.getChatInputHandler().awaitInput(player, "Cast Range:", (str) -> {
                try { skill.setCastRange(Double.parseDouble(str)); } catch(Exception e){}
                runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
            });
        }
        else if (slot == 5) { // Trigger
            TriggerType[] types = TriggerType.values();
            skill.setTrigger(types[(skill.getTrigger().ordinal() + 1) % types.length]);
            new SkillEditorGUI(plugin, skillId, page).open(player);
        }
        else if (slot == 6) { // Cooldown
            if (event.isLeftClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Base Cooldown:", (str) -> {
                    try { skill.setCooldownBase(Double.parseDouble(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            } else if (event.isRightClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Cooldown Per Level:", (str) -> {
                    try { skill.setCooldownPerLevel(Double.parseDouble(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            }
        }
        else if (slot == 7) { // Req Level
            plugin.getChatInputHandler().awaitInput(player, "Required Level:", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    if (lvl < 1) lvl = 1;
                    skill.setRequiredLevel(lvl);
                } catch (Exception e) {}
                runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
            });
        }
        else if (slot == 8) { // SP Cost
            if (event.isLeftClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Base SP Cost:", (str) -> {
                    try { skill.setSpCostBase(Integer.parseInt(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            } else if (event.isRightClick()) {
                plugin.getChatInputHandler().awaitInput(player, "SP Cost Per Level:", (str) -> {
                    try { skill.setSpCostPerLevel(Integer.parseInt(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            }
        }
        // Row 2
        else if (slot == 10) { // Variable Cast
            if (event.isLeftClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Variable CastTime:", (str) -> {
                    try { skill.setVariableCastTime(Double.parseDouble(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            } else {
                plugin.getChatInputHandler().awaitInput(player, "Reduction %:", (str) -> {
                    try { skill.setVariableCastTimeReduction(Double.parseDouble(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            }
        }
        else if (slot == 11) { // Fixed Cast
            plugin.getChatInputHandler().awaitInput(player, "Fixed CastTime:", (str) -> {
                try { skill.setFixedCastTime(Double.parseDouble(str)); } catch(Exception e){}
                runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
            });
        }
        else if (slot == 12) { // Motion
            if (event.isLeftClick()) {
                plugin.getChatInputHandler().awaitInput(player, "Pre-Motion:", (str) -> {
                    try { skill.setPreMotion(Double.parseDouble(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            } else {
                plugin.getChatInputHandler().awaitInput(player, "Post-Motion:", (str) -> {
                    try { skill.setPostMotion(Double.parseDouble(str)); } catch(Exception e){}
                    runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
                });
            }
        }
        else if (slot == 13) { // ACD
            plugin.getChatInputHandler().awaitInput(player, "ACD (After-Cast Delay):", (str) -> {
                try { skill.setAfterCastDelayBase(Double.parseDouble(str)); } catch(Exception e){}
                runSync(() -> new SkillEditorGUI(plugin, skillId, page).open(player));
            });
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
                new SkillEditorGUI(plugin, skillId, 0).open(player); // Open Page 0
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