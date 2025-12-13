package org.rostats.util;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

public final class DataWatcherSerializers {

    private DataWatcherSerializers() {}

    /**
     * ดึง Serializer สำหรับ Byte (ProtocolLib 5.x จำเป็นต้องใช้แบบนี้)
     */
    @SuppressWarnings("deprecation")
    public static WrappedDataWatcher.Serializer BYTE() {
        return WrappedDataWatcher.Registry.get(Byte.class);
    }

    /**
     * ดึง Serializer สำหรับ Integer (ProtocolLib 5.x จำเป็นต้องใช้แบบนี้)
     */
    @SuppressWarnings("deprecation")
    public static WrappedDataWatcher.Serializer INT() {
        return WrappedDataWatcher.Registry.get(Integer.class);
    }

    /**
     * ดึง Serializer สำหรับ Chat Component (Text)
     */
    public static WrappedDataWatcher.Serializer CHAT() {
        return WrappedDataWatcher.Registry.getChatComponentSerializer(true);
    }
}