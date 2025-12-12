package org.rostats.input;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatInputHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, Consumer<String>> singleInputs = new HashMap<>();
    private final Map<UUID, List<String>> multiLineBuffers = new HashMap<>();
    private final Map<UUID, Consumer<List<String>>> multiLineCallbacks = new HashMap<>();

    // [NEW] Cancel Callbacks
    private final Map<UUID, Runnable> cancelCallbacks = new HashMap<>();

    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public ChatInputHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // [UPDATED] Add onCancel
    public void awaitInput(Player player, String prompt, Consumer<String> callback, Runnable onCancel) {
        player.closeInventory();
        player.sendMessage("§e[Input] " + prompt);
        player.sendMessage("§7(Type /cancel to cancel)");
        singleInputs.put(player.getUniqueId(), callback);
        if (onCancel != null) cancelCallbacks.put(player.getUniqueId(), onCancel);
    }

    // Default overload for compatibility (if any)
    public void awaitInput(Player player, String prompt, Consumer<String> callback) {
        awaitInput(player, prompt, callback, null);
    }

    // [UPDATED] Add onCancel
    public void awaitMultiLineInput(Player player, String prompt, Consumer<List<String>> callback, Runnable onCancel) {
        player.closeInventory();
        player.sendMessage("§e[Multi-Line] " + prompt);

        Component message = Component.text("พิมพ์ข้อความทีละบรรทัด กดที่ ", NamedTextColor.GRAY)
                .append(Component.text("[DONE]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/done"))
                        .hoverEvent(HoverEvent.showText(Component.text("คลิกเพื่อบันทึกและเสร็จสิ้น", NamedTextColor.GREEN))))
                .append(Component.text(" เพื่อเสร็จสิ้น หรือ กดที่ ", NamedTextColor.GRAY))
                .append(Component.text("[CANCEL]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/cancel"))
                        .hoverEvent(HoverEvent.showText(Component.text("คลิกเพื่อยกเลิก", NamedTextColor.RED))));

        player.sendMessage(message);

        multiLineBuffers.put(player.getUniqueId(), new ArrayList<>());
        multiLineCallbacks.put(player.getUniqueId(), callback);
        if (onCancel != null) cancelCallbacks.put(player.getUniqueId(), onCancel);
    }

    // Default overload
    public void awaitMultiLineInput(Player player, String prompt, Consumer<List<String>> callback) {
        awaitMultiLineInput(player, prompt, callback, null);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();

        // Handle /cancel
        if (message.equalsIgnoreCase("/cancel")) {
            if (singleInputs.containsKey(uuid) || multiLineBuffers.containsKey(uuid)) {
                event.setCancelled(true);
                cleanup(uuid);
                player.sendMessage("§cยกเลิกการป้อนข้อมูล");
            }
            return;
        }

        if (multiLineBuffers.containsKey(uuid)) {
            if (message.equalsIgnoreCase("/done")) {
                event.setCancelled(true);
                List<String> result = multiLineBuffers.remove(uuid);
                Consumer<List<String>> callback = multiLineCallbacks.remove(uuid);
                cancelCallbacks.remove(uuid); // Clear cancel callback as success

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (callback != null) callback.accept(result);
                    }
                }.runTask(plugin);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage();

        if (singleInputs.containsKey(uuid)) {
            event.setCancelled(true);
            Consumer<String> callback = singleInputs.remove(uuid);
            cancelCallbacks.remove(uuid); // Clear cancel callback as success

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (callback != null) callback.accept(translateColorCodes(message));
                }
            }.runTask(plugin);
            return;
        }

        if (multiLineBuffers.containsKey(uuid)) {
            event.setCancelled(true);
            List<String> buffer = multiLineBuffers.get(uuid);
            buffer.add(translateColorCodes(message));
            player.sendMessage("§a+ บันทึกบรรทัดที่ " + buffer.size());
        }
    }

    // [NEW] Handle Quit to prevent leaks
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanup(event.getPlayer().getUniqueId());
    }

    private void cleanup(UUID uuid) {
        singleInputs.remove(uuid);
        multiLineBuffers.remove(uuid);
        multiLineCallbacks.remove(uuid);

        // Trigger cancel callback if exists
        Runnable onCancel = cancelCallbacks.remove(uuid);
        if (onCancel != null) {
            // Run sync if possible, but safely
            try {
                // If player quit, this might not show GUI, but cleans up logic
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        onCancel.run();
                    }
                }.runTask(plugin);
            } catch (Exception ignored) {}
        }
    }

    private String translateColorCodes(String text) {
        Matcher matcher = hexPattern.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            StringBuilder sb = new StringBuilder("§x");
            for (char c : color.toCharArray()) {
                sb.append("§").append(c);
            }
            matcher.appendReplacement(buffer, sb.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace("&", "§");
    }
}