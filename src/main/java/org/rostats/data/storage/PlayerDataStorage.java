package org.rostats.data.storage;

import org.bukkit.entity.Player;

public interface PlayerDataStorage {

    // โหลดข้อมูลผู้เล่น
    void loadData(Player player);

    // บันทึกข้อมูลผู้เล่น
    void saveData(Player player);

    // ปิดการทำงาน (เช่น ปิด Connection Database)
    void shutdown();
}