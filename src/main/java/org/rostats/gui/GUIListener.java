package org.rostats.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
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
    // เก็บสถานะการแก้ไขรายการ Action (เช่น กำลังแก้ Loop/Condition)
    private final Map<UUID, List<SkillAction>> currentEditingList = new HashMap<>();
    // เก็บค่า Property ชั่วคราวก่อนบันทึก
    private final Map<UUID, Map<String, Object>> editingProperties = new HashMap<>();
    // เก็บ Callback สำหรับโหมดเลือกสกิล
    private static final Map<UUID, Consumer<String>> selectionCallbacks = new HashMap<>();

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // Static Method ให้ Class อื่นเรียกใช้โหมดเลือกสกิลได้
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

        // ถ้ามีการ Switch หน้าต่าง (เช่นไปหน้า Input) ไม่ต้องลบสถานะ
        if (player.hasMetadata("ROSTATS_SWITCH")) {
            player.removeMetadata("ROSTATS_SWITCH", plugin);
            return;
        }

        // ถ้าปิดหน้าต่างโดยสมบูรณ์ ให้ลบ Callback ทิ้ง
        if (!player.hasMetadata("ROSTATS_SWITCH")) {
            selectionCallbacks.remove(player.getUniqueId());
            // ลบสถานะการแก้ไขค้างไว้ด้วยเพื่อความสะอาด
            currentEditingList.remove(player.getUniqueId());
            editingProperties.remove(player.getUniqueId());
        }

        // Logic ของ Character Status GUI
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

        // ป้องกันการขโมยของจาก GUI
        if (title.contains(CharacterGUI.TITLE_HEADER) ||
                title.startsWith("Skill") ||
                title.startsWith("Action") ||
                title.startsWith("Lib:") ||
                title.startsWith("Pack:") ||
                title.startsWith("Delete:")) {

            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                event.setCancelled(true);
                return;
            }
        }

        // --- 1. Library Navigation (Folders & Packs) ---
        if (title.startsWith("Lib:") || title.startsWith("Pack:")) {
            event.setCancelled(true);
            handleLibraryNavigation(event, player, title);
            return;
        }

        // --- 2. Delete Confirmation ---
        if (title.startsWith("Delete:")) {
            event.setCancelled(true);
            handleSkillDeleteClick(event, player, title);
            return;
        }

        // --- 3. Skill Editor (Main & Nested) ---
        if (title.startsWith("SkillEditor:")) {
            event.setCancelled(true);
            String[] parts = title.substring(12).trim().split(" #P");
            String skillId = parts[0];
            int page = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            handleSkillEditorClick(event, player, skillId, page);
            return;
        }

        // --- 4. Action Selector (Choose Action Type) ---
        if (title.startsWith("ActionSelector:")) {
            event.setCancelled(true);
            handleActionSelectorClick(event, player, title.substring(16));
            return;
        }

        // --- 5. Action Property Editor (Edit Values) ---
        if (title.startsWith("ActionEdit:")) {
            event.setCancelled(true);
            String[] parts = title.substring(11).trim().split(" #");
            if (parts.length == 2) {
                handleActionPropertyClick(event, player, parts[0], Integer.parseInt(parts[1]));
            }
            return;
        }

        // --- 6. Character Status GUI ---
        if (title.contains(CharacterGUI.TITLE_HEADER)) {
            event.setCancelled(true);
            handleCharacterStatusClick(event, player, title);
            return;
        }
    }

    // ==========================================
    //           LIBRARY NAVIGATION
    // ==========================================
    private void handleLibraryNavigation(InventoryClickEvent event, Player player, String title) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // คำนวณหาตำแหน่งปัจจุบัน (Current Path)
        File currentDir;
        if (title.startsWith("Lib: ")) {
            String pathString = title.substring(5).trim();
            if (pathString.startsWith("...")) pathString = "/"; // Fallback
            currentDir = plugin.getSkillManager().getFileFromRelative(pathString);
        } else { // Pack: filename.yml
            String fileName = title.substring(6).trim();
            if (fileName.startsWith("...")) fileName = ""; // Fallback
            currentDir = findFileRecursive(plugin.getSkillManager().getRootDir(), fileName);
        }

        if (currentDir == null || !currentDir.exists()) currentDir = plugin.getSkillManager().getRootDir();

        // 1. เข้า Folder (CHEST)
        if (clicked.getType() == Material.CHEST && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("📂")) {
            String folderName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).replace("📂 ", "").trim();
            File target = new File(currentDir, folderName);
            new SkillLibraryGUI(plugin, target).open(player);
            return;
        }

        // 2. เข้า Skill Pack (ENDER_CHEST)
        if (clicked.getType() == Material.ENDER_CHEST && clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("📦")) {
            String fileName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName()).replace("📦 ", "").trim();
            File target = new File(currentDir, fileName);
            new SkillLibraryGUI(plugin, target).open(player); // เปิด Pack View
            return;
        }

        // 3. ปุ่มย้อนกลับ (Back)
        if (clicked.getType() == Material.ARROW && clicked.getItemMeta().getDisplayName().contains("BACK")) {
            File parent = currentDir.getParentFile();
            // ถ้าเป็นไฟล์ (Pack) -> กลับไป Folder ที่มันอยู่
            // ถ้าเป็น Folder -> กลับไป Parent Folder
            if (parent != null && parent.getAbsolutePath().startsWith(plugin.getSkillManager().getRootDir().getAbsolutePath())) {
                new SkillLibraryGUI(plugin, parent).open(player);
            } else {
                new SkillLibraryGUI(plugin).open(player); // กลับ Root
            }
            return;
        }

        // 4. Create New (Buttons)
        if (title.startsWith("Lib:")) {
            // สร้าง Folder
            if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("New Folder")) {
                promptCreate(player, currentDir, false, false);
                return;
            }
            // สร้าง Skill เดี่ยว
            if (clicked.getType() == Material.PAPER && clicked.getItemMeta().getDisplayName().contains("New Skill")) {
                promptCreate(player, currentDir, true, false);
                return;
            }
            // สร้าง Pack
            if (clicked.getType() == Material.ENDER_CHEST && clicked.getItemMeta().getDisplayName().contains("New Pack")) {
                promptCreate(player, currentDir, true, true);
                return;
            }
        }

        // 5. Add Skill to Pack
        if (title.startsWith("Pack:") && clicked.getType() == Material.LIME_DYE) {
            final File packFile = currentDir;
            player.sendMessage("§eType new skill name:");
            player.closeInventory();
            plugin.getChatInputHandler().awaitInput(player, "Skill Name:", (name) -> {
                plugin.getSkillManager().addSkillToFile(packFile, name);
                runSync(() -> new SkillLibraryGUI(plugin, packFile).open(player));
            });
            return;
        }

        // 6. Click Skill Item (Edit / Select)
        if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
            List<String> lore = clicked.getItemMeta().getLore();
            if (lore != null && !lore.isEmpty() && lore.get(0).startsWith("§8ID: ")) {
                String skillId = lore.get(0).replace("§8ID: ", "");

                // ตรวจสอบ Selection Mode
                if (selectionCallbacks.containsKey(player.getUniqueId())) {
                    Consumer<String> callback = selectionCallbacks.remove(player.getUniqueId());
                    player.sendMessage("§aSelected: " + skillId);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    player.closeInventory();
                    callback.accept(skillId);
                    return;
                }

                if (event.isRightClick()) {
                    // ส่งไปหน้า Confirm Delete
                    new SkillLibraryGUI(plugin, currentDir).openConfirmDelete(player, new File(currentDir, skillId + ".yml"));
                } else {
                    new SkillEditorGUI(plugin, skillId).open(player);
                }
            }
        }
    }

    private void promptCreate(Player player, File dir, boolean isFile, boolean isPack) {
        player.sendMessage("§eEnter name:");
        player.closeInventory();
        plugin.getChatInputHandler().awaitInput(player, "Name:", (input) -> {
            if (isFile) {
                String name = input.endsWith(".yml") ? input : input + ".yml";
                plugin.getSkillManager().createSkill(dir, name);
            } else {
                plugin.getSkillManager().createFolder(dir, input);
            }
            runSync(() -> new SkillLibraryGUI(plugin, dir).open(player));
        });
    }

    private void handleSkillDeleteClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring("Delete: ".length());
        File target = findFileRecursive(plugin.getSkillManager().getRootDir(), fileName);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;

        if (clicked.getType() == Material.LIME_CONCRETE && target != null) {
            File parent = target.getParentFile();
            plugin.getSkillManager().deleteFile(target);
            player.sendMessage("§cDeleted.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1f, 1f);
            runSync(() -> new SkillLibraryGUI(plugin, parent).open(player));
        } else if (clicked.getType() == Material.RED_CONCRETE) {
            if (target != null) new SkillLibraryGUI(plugin, target.getParentFile()).open(player);
            else new SkillLibraryGUI(plugin).open(player);
        }
    }

    // ==========================================
    //           SKILL EDITOR (MAIN)
    // ==========================================
    private void handleSkillEditorClick(InventoryClickEvent event, Player player, String skillId, int page) {
        SkillData rootSkill = plugin.getSkillManager().getSkill(skillId);
        if (rootSkill == null) { player.closeInventory(); return; }

        List<SkillAction> activeList = currentEditingList.getOrDefault(player.getUniqueId(), rootSkill.getActions());
        int slot = event.getSlot();

        // Pagination
        if (slot == 45) { if (page > 0) refreshGUI(player, skillId, page - 1); return; }
        if (slot == 53) { if ((page + 1) * 27 < activeList.size()) refreshGUI(player, skillId, page + 1); return; }

        // Back Button
        if (slot == 48) {
            boolean isNested = currentEditingList.containsKey(player.getUniqueId());
            if (!isNested) {
                // หาไฟล์ของสกิลนี้เพื่อกลับไป Folder ที่ถูกต้อง
                File f = findFileRecursive(plugin.getSkillManager().getRootDir(), skillId + ".yml");
                if(f == null) f = findFileRecursive(plugin.getSkillManager().getRootDir(), skillId); // Try pack name logic?

                // Fallback to root if unknown
                new SkillLibraryGUI(plugin).open(player);
            } else {
                currentEditingList.remove(player.getUniqueId()); // ออกจาก Nested List
                new SkillEditorGUI(plugin, skillId, 0).open(player);
            }
            return;
        }

        // Save
        if (slot == 49) {
            plugin.getSkillManager().saveSkill(rootSkill);
            player.sendMessage("§aSaved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
            return;
        }

        // Add Action
        if (slot == 50) {
            new SkillActionSelectorGUI(plugin, skillId).open(player);
            return;
        }

        // Action Click
        if (slot >= 18 && slot <= 44) {
            int index = (page * 27) + (slot - 18);
            if (index < activeList.size()) {
                SkillAction action = activeList.get(index);

                // Nested Editing (Right Click)
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

                // Reordering & Deleting
                if (event.isRightClick() && !event.isShiftClick()) {
                    if (index < activeList.size() - 1) { Collections.swap(activeList, index, index + 1); refreshGUI(player, skillId, page); }
                }
                else if (event.isLeftClick() && event.isShiftClick()) {
                    if (index > 0) { Collections.swap(activeList, index, index - 1); refreshGUI(player, skillId, page); }
                }
                else if (event.isRightClick() && event.isShiftClick()) {
                    activeList.remove(index); refreshGUI(player, skillId, page);
                }
                else if (event.isLeftClick()) {
                    editingProperties.put(player.getUniqueId(), new HashMap<>(action.serialize()));
                    new SkillActionPropertyGUI(plugin, skillId, index, action).open(player);
                }
            }
            return;
        }

        // Meta Data Edit
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

    // ==========================================
    //           ACTION SELECTOR
    // ==========================================
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
            // [FIXED] Updated Constructor for Particle
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

    // ==========================================
    //           PROPERTY EDITOR
    // ==========================================
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
            case SELECT_TARGET: return new TargetSelectorAction(TargetSelectorAction.SelectorMode.valueOf(String.valueOf(data.getOrDefault("mode", "SELF"))), Double.parseDouble(String.valueOf(data.getOrDefault("radius", "10.0"))));
            case DAMAGE: return new DamageAction(plugin, String.valueOf(data.getOrDefault("formula","ATK")), String.valueOf(data.getOrDefault("element","NEUTRAL")));
            case HEAL: return new HealAction(plugin, String.valueOf(data.getOrDefault("formula","10")), Boolean.parseBoolean(String.valueOf(data.getOrDefault("is-mana", false))), Boolean.parseBoolean(String.valueOf(data.getOrDefault("self-only", true))));
            case CONDITION: return new ConditionAction(plugin, String.valueOf(data.getOrDefault("formula", "true")), ((ConditionAction)oldAction).getSuccessActions(), ((ConditionAction)oldAction).getFailActions());
            case SET_VARIABLE: return new SetVariableAction(plugin, String.valueOf(data.getOrDefault("var", "temp")), String.valueOf(data.getOrDefault("val", "0")));
            case LOOP: return new LoopAction(plugin, String.valueOf(data.getOrDefault("start","0")), String.valueOf(data.getOrDefault("end","5")), String.valueOf(data.getOrDefault("step","1")), String.valueOf(data.getOrDefault("var","i")), ((LoopAction)oldAction).getSubActions());
            case SOUND: return new SoundAction(String.valueOf(data.get("sound")), Float.parseFloat(String.valueOf(data.get("volume"))), Float.parseFloat(String.valueOf(data.get("pitch"))));
            case APPLY_EFFECT: return new EffectAction(plugin, String.valueOf(data.get("effect-id")), EffectType.valueOf(String.valueOf(data.get("effect-type"))), Integer.parseInt(String.valueOf(data.get("level"))), Double.parseDouble(String.valueOf(data.get("power"))), Long.parseLong(String.valueOf(data.get("duration"))), Double.parseDouble(String.valueOf(data.get("chance"))), (String)data.get("stat-key"));

            // [FIXED] Full Particle Reconstruct
            case PARTICLE:
                return new ParticleAction(plugin,
                        String.valueOf(data.getOrDefault("particle", "VILLAGER_HAPPY")),
                        String.valueOf(data.getOrDefault("count", "5")),
                        String.valueOf(data.getOrDefault("speed", "0.1")),
                        String.valueOf(data.getOrDefault("shape", "POINT")),
                        String.valueOf(data.getOrDefault("radius", "0.5")),
                        String.valueOf(data.getOrDefault("points", "20")),
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

    private boolean isIntegerKey(String key) { return key.equals("level") || key.equals("duration") || key.equals("count") || key.equals("amplifier") || key.equals("max-targets"); }
    private boolean isDoubleKey(String key) { return key.equals("power") || key.equals("chance") || key.equals("speed") || key.equals("offset") || key.equals("range") || key.equals("volume") || key.equals("pitch") || key.equals("radius") || key.equals("x") || key.equals("y") || key.equals("z"); }

    private void reopenPropertyGUI(Player player, String skillId, int index, Map<String, Object> data, ActionType type) {
        SkillAction tempAction = new SkillAction() {
            public ActionType getType() { return type; }
            public void execute(org.bukkit.entity.LivingEntity c, org.bukkit.entity.LivingEntity t, int l, Map<String, Double> ctx) {}
            public Map<String, Object> serialize() { return data; }
        };
        new SkillActionPropertyGUI(plugin, skillId, index, tempAction).open(player);
    }

    // ... (MetaData Editor & Status GUI - เหมือนเดิม) ...
    private void handleMetaDataEdit(InventoryClickEvent event, Player player, String skillId, int page, SkillData skill) {
        // [NOTE] Copy Logic MetaDataEdit จากโค้ดเก่ามาวางที่นี่ (ยาวเกินข้อความ)
        // หรือถ้าคุณมีอยู่แล้วก็ใช้ได้เลยครับ มันเหมือนเดิม
        int slot = event.getSlot();
        if (slot == 0) { plugin.getChatInputHandler().awaitInput(player, "Name:", (str) -> { skill.setDisplayName(str.replace("&", "§")); runSync(() -> refreshGUI(player, skillId, page)); }); }
        // ... (ต่อ) ...
    }

    private void handleCharacterStatusClick(InventoryClickEvent event, Player player, String title) {
        // [NOTE] Copy Logic CharacterStatus มาวางที่นี่
    }

    // ... (Helper Methods) ...
    private File findFileRecursive(File dir, String name) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().equals(name)) return f;
            if (f.isDirectory()) { File found = findFileRecursive(f, name); if (found != null) return found; }
        }
        return null;
    }

    private void runSync(Runnable r) {
        plugin.getServer().getScheduler().runTask(plugin, r);
    }
}