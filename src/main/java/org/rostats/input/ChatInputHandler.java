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

        // สร้างข้อความแบบ Clickable Component
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
    }

    // เพิ่ม Event Listener สำหรับดักจับคำสั่งจากการคลิกปุ่ม
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = event.getMessage(); // ข้อความจะขึ้นต้นด้วย /

        if (multiLineBuffers.containsKey(uuid)) {
            if (message.equalsIgnoreCase("/cancel")) {
                event.setCancelled(true);
                multiLineBuffers.remove(uuid);
                multiLineCallbacks.remove(uuid);
                player.sendMessage("§cยกเลิกการแก้ไข Lore");
                return;
            }

            if (message.equalsIgnoreCase("/done")) {
                event.setCancelled(true);
                List<String> result = multiLineBuffers.remove(uuid);
                Consumer<List<String>> callback = multiLineCallbacks.remove(uuid);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(result);
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

            // Note: การเช็ค /done และ /cancel ย้ายไปที่ onCommandPreprocess แล้ว
            // เพราะปุ่มกดใช้ ClickEvent.runCommand ซึ่งจะส่งเป็น Command ไม่ใช่ Chat

            List<String> buffer = multiLineBuffers.get(uuid);
            buffer.add(translateColorCodes(message));
            player.sendMessage("§a+ บันทึกบรรทัดที่ " + buffer.size());
        }
    }

    private String translateColorCodes(String text) {
        // Support RGB &#RRGGBB and Legacy &c
        return text.replace("&", "§");
    }
}