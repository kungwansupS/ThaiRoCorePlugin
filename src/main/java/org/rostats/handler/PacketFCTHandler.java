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
import org.bukkit.scheduler.BukkitRunnable;
import org.rostats.ThaiRoCorePlugin;
// [NEW] Import Utility Class
import org.rostats.util.DataWatcherSerializers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PacketFCTHandler {

    private final ThaiRoCorePlugin plugin;
    private final ProtocolManager protocolManager;

    public PacketFCTHandler(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void show(Location location, String text) {
        // 1. Prepare Entity Data
        int entityId = ThreadLocalRandom.current().nextInt(1000000000, 2000000000);
        UUID uuid = UUID.randomUUID();

        // 2. Filter Viewers
        double range = 48.0;
        double rangeSq = range * range;
        List<Player> viewers = location.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distanceSquared(location) <= rangeSq)
                .collect(Collectors.toList());

        if (viewers.isEmpty()) return;

        try {
            // --- 3. Construct Packets ---

            // A. Spawn Packet
            PacketContainer spawnPacket = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            spawnPacket.getIntegers().write(0, entityId);
            spawnPacket.getUUIDs().write(0, uuid);
            spawnPacket.getEntityTypeModifier().write(0, EntityType.TEXT_DISPLAY);
            spawnPacket.getDoubles()
                    .write(0, location.getX())
                    .write(1, location.getY())
                    .write(2, location.getZ());

            // B. Metadata Packet
            PacketContainer metaPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            metaPacket.getIntegers().write(0, entityId);

            WrappedDataWatcher watcher = new WrappedDataWatcher();

            // [UPDATED] เรียกใช้ Serializer ผ่าน Utility Class (Clean Code!)
            WrappedDataWatcher.Serializer byteSerializer = DataWatcherSerializers.BYTE();
            WrappedDataWatcher.Serializer intSerializer = DataWatcherSerializers.INT();
            WrappedDataWatcher.Serializer chatSerializer = DataWatcherSerializers.CHAT();

            // Index 14: Billboard (Byte) -> 3 = CENTER
            watcher.setObject(14, byteSerializer, (byte) 3);

            // Index 23: Text (Optional Component)
            Object chatComponent = WrappedChatComponent.fromLegacyText(text).getHandle();
            watcher.setObject(23, chatSerializer, Optional.of(chatComponent));

            // Index 25: Background Color (Int) -> 0 = Transparent
            watcher.setObject(25, intSerializer, 0);

            // Index 27: Flags (Byte) -> 1 = Shadow
            watcher.setObject(27, byteSerializer, (byte) 1);

            metaPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

            // 4. Send Initial Packets
            for (Player p : viewers) {
                if (p.isOnline()) {
                    protocolManager.sendServerPacket(p, spawnPacket);
                    protocolManager.sendServerPacket(p, metaPacket);
                }
            }

            // 5. Animation Task
            new BukkitRunnable() {
                private int ticks = 0;
                private double currentY = location.getY();
                private final double step = 1.0 / 20.0;

                @Override
                public void run() {
                    if (ticks >= 20) {
                        PacketContainer destroyPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                        destroyPacket.getIntLists().write(0, Collections.singletonList(entityId));

                        for (Player p : viewers) {
                            if (p.isOnline()) {
                                try {
                                    protocolManager.sendServerPacket(p, destroyPacket);
                                } catch (Exception ignored) {}
                            }
                        }
                        this.cancel();
                        return;
                    }

                    currentY += step;
                    ticks++;

                    PacketContainer teleportPacket = protocolManager.createPacket(PacketType.Play.Server.ENTITY_TELEPORT);
                    teleportPacket.getIntegers().write(0, entityId);
                    teleportPacket.getDoubles()
                            .write(0, location.getX())
                            .write(1, currentY)
                            .write(2, location.getZ());

                    teleportPacket.getBytes()
                            .write(0, (byte) 0)
                            .write(1, (byte) 0);
                    teleportPacket.getBooleans().write(0, false);

                    for (Player p : viewers) {
                        if (p.isOnline()) {
                            try {
                                protocolManager.sendServerPacket(p, teleportPacket);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 1L);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}