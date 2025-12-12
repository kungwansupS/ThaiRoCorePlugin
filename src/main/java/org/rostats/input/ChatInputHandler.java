package org.rostats.input;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.trigger.TriggerType;
import org.rostats.itemeditor.ItemAttribute;
import org.rostats.itemeditor.ItemSkillBinding;
import org.rostats.itemeditor.TriggerSelectorGUI;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, Consumer<String>> inputCallbacks = new HashMap<>();
    private static final Map<UUID, SkillInputState> skillInputMap = new HashMap<>();

    // State class to hold skill binding information during chat input
    private static class SkillInputState {
        final String skillId;
        final String itemId;
        final int bindingIndex;
        final TriggerType trigger;
        final Consumer<Integer> finalCallback;

        public SkillInputState(String skillId, String itemId, int bindingIndex, TriggerType trigger, Consumer<Integer> finalCallback) {
            this.skillId = skillId;
            this.itemId = itemId;
            this.bindingIndex = bindingIndex;
            this.trigger = trigger;
            this.finalCallback = finalCallback;
        }
    }

    public ChatInputHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // --- General Input ---
    public void awaitInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage("§e[Input] " + prompt);
        player.sendMessage("§7(Type 'cancel' to abort)");
        inputCallbacks.put(player.getUniqueId(), callback);
    }

    public void awaitMultiLineInput(Player player, String prompt, Consumer<List<String>> callback) {
        // Simple implementation for single line acting as multi-line for now,
        // or you can implement full multi-line logic.
        // For brevity in this fix, we reuse awaitInput and wrap list.
        awaitInput(player, prompt, (str) -> callback.accept(List.of(str.split("\\\\n"))));
    }

    // --- Skill Binding Input Logic ---

    // Entry point from SkillLibraryGUI (New Binding)
    public static void awaitSkillLevel(ThaiRoCorePlugin plugin, Player player, String skillId, String itemId, Consumer<Integer> finalCallback) {
        awaitSkillLevel(plugin, player, skillId, itemId, -1, null, finalCallback);
    }

    // Entry point with full context
    public static void awaitSkillLevel(ThaiRoCorePlugin plugin, Player player, String skillId, String itemId, int bindingIndex, TriggerType trigger, Consumer<Integer> finalCallback) {
        player.closeInventory();
        player.sendMessage("§e---------------------------------");
        player.sendMessage("§e[Skill Binding] Enter Skill Level and Chance.");
        player.sendMessage("§7Format: <Level> <Chance%> (e.g., 5 10 for Level 5, 10% chance)");
        player.sendMessage("§7Enter 'cancel' to abort.");
        player.sendMessage("§e---------------------------------");

        skillInputMap.put(player.getUniqueId(), new SkillInputState(skillId, itemId, bindingIndex, trigger, finalCallback));

        // Timeout (1 minute)
        new BukkitRunnable() {
            @Override
            public void run() {
                if (skillInputMap.containsKey(player.getUniqueId())) {
                    skillInputMap.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        player.sendMessage("§c[Skill Binding] Input timed out.");
                    }
                }
            }
        }.runTaskLater(plugin, 20 * 60);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 1. Handle General Input
        if (inputCallbacks.containsKey(playerId)) {
            event.setCancelled(true);
            Consumer<String> callback = inputCallbacks.remove(playerId);
            String msg = event.getMessage().trim();
            if (msg.equalsIgnoreCase("cancel")) {
                player.sendMessage("§cCancelled.");
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(msg);
                    }
                }.runTask(plugin);
            }
            return;
        }

        // 2. Handle Skill Input
        if (skillInputMap.containsKey(playerId)) {
            event.setCancelled(true);
            SkillInputState state = skillInputMap.remove(playerId);
            String message = event.getMessage().trim();

            if (message.equalsIgnoreCase("cancel")) {
                player.sendMessage("§c[Skill Binding] Operation cancelled.");
                // Try to reopen GUI on main thread
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        new TriggerSelectorGUI(plugin, state.itemId, state.bindingIndex, state.skillId).open(player);
                    }
                }.runTask(plugin);
                return;
            }

            try {
                String[] parts = message.split(" ");
                if (parts.length != 2) {
                    player.sendMessage("§c[Skill Binding] Invalid format. Use: <Level> <Chance%> (e.g., 5 10)");
                    // Re-add state to let user try again
                    skillInputMap.put(playerId, state);
                    return;
                }

                int level = Integer.parseInt(parts[0]);
                double chancePct = Double.parseDouble(parts[1]);
                double chance = Math.max(0.0, Math.min(1.0, chancePct / 100.0));

                if (state.trigger == null) {
                    // Case 1: Need to pick Trigger next
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            new TriggerSelectorGUI(plugin, state.itemId, state.bindingIndex, state.skillId, TriggerType.CAST, level, chance).open(player);
                        }
                    }.runTask(plugin);
                } else {
                    // Case 2: Finalize
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            finalizeSkillBinding(player, state.itemId, state.skillId, state.bindingIndex, state.trigger, level, chance);
                            if (state.finalCallback != null) {
                                state.finalCallback.accept(level);
                            }
                        }
                    }.runTask(plugin);
                }

            } catch (NumberFormatException e) {
                player.sendMessage("§c[Skill Binding] Level or Chance must be a valid number.");
                skillInputMap.put(playerId, state);
            }
        }
    }

    public void finalizeSkillBinding(Player player, String itemId, String skillId, int bindingIndex, TriggerType trigger, int level, double chance) {
        ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(player.getInventory().getItemInMainHand());
        List<ItemSkillBinding> bindings = attr.getSkillBindings();

        ItemSkillBinding newBinding = new ItemSkillBinding(skillId, trigger, level, chance);

        if (bindingIndex == -1) {
            bindings.add(newBinding);
        } else if (bindingIndex >= 0 && bindingIndex < bindings.size()) {
            bindings.set(bindingIndex, newBinding);
        } else {
            bindings.add(newBinding); // Fallback
        }

        attr.setSkillBindings(bindings);
        plugin.getItemAttributeManager().applyAttributesToItem(player.getInventory().getItemInMainHand(), attr);
        plugin.getItemAttributeManager().applyLoreStats(player.getInventory().getItemInMainHand(), attr);

        File itemFile = plugin.getItemManager().getFileFromRelative(itemId + ".yml");
        if (itemFile.exists()) {
            plugin.getItemManager().saveItem(itemFile, attr, player.getInventory().getItemInMainHand());
        }
        player.sendMessage("§a[Skill Binding] Saved: " + skillId + " (Lv." + level + ")");
    }
}