package org.rostats.input;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;

import java.util.*;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, Consumer<String>> singleInputs = new HashMap<>();
    private final Map<UUID, List<String>> multiLineBuffers = new HashMap<>();
    private final Map<UUID, Consumer<List<String>>> multiLineCallbacks = new HashMap<>();

    public ChatInputHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void awaitInput(Player player, String prompt, Consumer<String> callback) {
        player.closeInventory();
        player.sendMessage("§e[Input] " + prompt);
        singleInputs.put(player.getUniqueId(), callback);
    }

    public void awaitMultiLineInput(Player player, String prompt, Consumer<List<String>> callback) {
        player.closeInventory();
        player.sendMessage("§e[Multi-Line] " + prompt);
        player.sendMessage("§7พิมพ์ข้อความทีละบรรทัด พิมพ์ §a/done §7เพื่อเสร็จสิ้น หรือ §c/cancel §7เพื่อยกเลิก");
        multiLineBuffers.put(player.getUniqueId(), new ArrayList<>());
        multiLineCallbacks.put(player.getUniqueId(), callback);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();

        // 1. Handle Single Line Input
        if (singleInputs.containsKey(uuid)) {
            event.setCancelled(true);
            Consumer<String> callback = singleInputs.remove(uuid);

            // Sync execution to main thread
            new BukkitRunnable() {
                @Override
                public void run() {
                    callback.accept(translateColorCodes(message));
                }
            }.runTask(plugin);
            return;
        }

        // 2. Handle Multi Line Input
        if (multiLineBuffers.containsKey(uuid)) {
            event.setCancelled(true);

            if (message.equalsIgnoreCase("/cancel")) {
                multiLineBuffers.remove(uuid);
                multiLineCallbacks.remove(uuid);
                player.sendMessage("§cยกเลิกการแก้ไข Lore");
                return;
            }

            if (message.equalsIgnoreCase("/done")) {
                List<String> result = multiLineBuffers.remove(uuid);
                Consumer<List<String>> callback = multiLineCallbacks.remove(uuid);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(result);
                    }
                }.runTask(plugin);
                return;
            }

            List<String> buffer = multiLineBuffers.get(uuid);
            buffer.add(translateColorCodes(message));
            player.sendMessage("§a+ บันทึกบรรทัดที่ " + buffer.size());
        }
    }

    private String translateColorCodes(String text) {
        // Support RGB &#RRGGBB and Legacy &c
        // For simplicity using LegacyComponentSerializer logic or Regex replacement
        // This is a basic hex translator:
        if (text.contains("&#")) {
            for (int i = 0; i < text.length() - 7; i++) {
                if (text.substring(i, i + 2).equals("&#")) {
                    // This is a placeholder logic for hex support
                    // Ideally use Adventure or BungeeCord Hex utils
                }
            }
        }
        return text.replace("&", "§");
    }
}