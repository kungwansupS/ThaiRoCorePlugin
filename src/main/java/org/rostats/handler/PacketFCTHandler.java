package org.rostats.handler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PacketFCTHandler {

    private final ThaiRoCorePlugin plugin;
    private final ProtocolManager protocolManager;

    public PacketFCTHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void show(Location location, String text) {
        // Find players nearby to send packet to
        double range = 48.0; // Visibility range
        double rangeSq = range * range;

        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= rangeSq) {
                spawnForPlayer(player, location, text);
            }
        }
    }

    private void spawnForPlayer(Player player, Location location, String text) {
        int entityId = ThreadLocalRandom.current().nextInt(1000000000, 2000000000); // Random ID to avoid conflict
        UUID uuid = UUID.randomUUID();

        try {
            // 1. Spawn Packet
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, uuid);
            spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
            spawnPacket.getDoubles()
                    .write(0, location.getX())
                    .write(1, location.getY())
                    .write(2, location.getZ());

            // 2. Metadata Packet (TextDisplay properties)
            PacketContainer metaPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metaPacket.getIntegers().write(0, entityId);

            WrappedDataWatcher watcher = new WrappedDataWatcher();

            // Fix Warning: ใช้ getByteSerializer() แทน get(Byte.class)
            WrappedDataWatcher.Serializer byteSerializer = WrappedDataWatcher.Registry.getByteSerializer();

            // Index 14: Display Billboard (Byte) -> 3 = CENTER (Look at player)
            watcher.setObject(14, byteSerializer, (byte) 3);

            // Index 23: Text (Optional Component)
            WrappedDataWatcher.Serializer chatSerializer = WrappedDataWatcher.Registry.getChatComponentSerializer(true);
            watcher.setObject(23, chatSerializer, Optional.of(WrappedChatComponent.fromLegacyText(text).getHandle()));

            // Fix Warning: ใช้ getIntegerSerializer() แทน get(Integer.class)
            WrappedDataWatcher.Serializer intSerializer = WrappedDataWatcher.Registry.getIntegerSerializer();

            // Index 25: Background Color (Int) -> 0 = Transparent
            watcher.setObject(25, intSerializer, 0);

            // Index 27: TextDisplay Flags (Byte) -> Shadow (1)
            watcher.setObject(27, byteSerializer, (byte) 1);

            metaPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

            // Send packets
            protocolManager.sendServerPacket(player, spawnPacket);
            protocolManager.sendServerPacket(player, metaPacket);

            // 3. Animation Task (Client-side movement via Teleport Packet)
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                private int ticks = 0;
                private final double step = 1.0 / 20.0; // Speed
                private double currentY = location.getY();

                @Override
                public void run() {
                    try {
                        while (ticks < 20) { // 1 Second
                            if (!player.isOnline()) return;

                            currentY += step;
                            ticks++;

                            PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
                            teleportPacket.getIntegers().write(0, entityId);
                            teleportPacket.getDoubles()
                                    .write(0, location.getX())
                                    .write(1, currentY)
                                    .write(2, location.getZ());
                            teleportPacket.getBytes()
                                    .write(0, (byte) 0)  // Yaw
                                    .write(1, (byte) 0); // Pitch
                            teleportPacket.getBooleans().write(0, false); // OnGround

                            protocolManager.sendServerPacket(player, teleportPacket);

                            // Thread.sleep(50); is not a reliable way to delay for 1 tick in Bukkit/Paper environment
                            // For asynchronous tasks, Thread.sleep(50) is used as a workaround to approximate 1 tick (50ms).
                            Thread.sleep(50);
                        }

                        // 4. Destroy Packet
                        PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                        destroyPacket.getIntLists().write(0, java.util.Collections.singletonList(entityId));
                        protocolManager.sendServerPacket(player, destroyPacket);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}