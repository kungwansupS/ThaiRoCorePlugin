package org.rostats.input;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.utils.ComponentUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, InputSession> activeSessions = new ConcurrentHashMap<>();

    public ChatInputHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void awaitInput(Player player, Consumer<String> callback) {
        activeSessions.put(player.getUniqueId(), new InputSession(callback));
        player.sendMessage(ComponentUtil.info("Type your input in chat (or type 'cancel' to abort):"));
    }

    // MODERN EVENT for Paper 1.21+
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!activeSessions.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        InputSession session = activeSessions.remove(player.getUniqueId());

        // Convert Component message to String safely
        String input = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage(ComponentUtil.error("Input cancelled."));
            return;
        }

        // Run callback on main thread because most Bukkit APIs require it
        plugin.getServer().getScheduler().runTask(plugin, () -> session.callback.accept(input));
    }

    private record InputSession(Consumer<String> callback) {}
}