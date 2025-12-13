package org.rostats.handler;

import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.rostats.ThaiRoCorePlugin;

import java.util.Random;

public class FloatingTextManager {

    private final ThaiRoCorePlugin plugin;
    private final Random random = new Random();

    public FloatingTextManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void spawn(Location location, String text) {
        spawn(location, text, 0.25);
    }

    public void spawn(Location location, String text, double verticalOffset) {
        if (location == null || location.getWorld() == null) return;

        // สุ่ม Offset เล็กน้อยเพื่อไม่ให้ตัวเลขทับกันเป๊ะๆ
        double xRnd = (random.nextDouble() - 0.5) * 0.5;
        double zRnd = (random.nextDouble() - 0.5) * 0.5;
        Location startLoc = location.clone().add(xRnd, verticalOffset, zRnd);

        // ใช้ Bukkit API Spawn TextDisplay (Safe & Stable)
        // ทำงานบน Main Thread เสมอ (Plugin เรียกใช้จาก Event ซึ่งมักเป็น Main Thread อยู่แล้ว)
        // หากมีการเรียก Async ต้องตบกลับมา Sync ก่อน
        if (!plugin.getServer().isPrimaryThread()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> spawnLogic(startLoc, text));
        } else {
            spawnLogic(startLoc, text);
        }
    }

    private void spawnLogic(Location location, String text) {
        location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(Component.text(text));
            display.setBillboard(Display.Billboard.CENTER); // หันหน้าหาผู้เล่นเสมอ
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); // พื้นหลังใส
            display.setShadowed(false); // ปิดเงา (แล้วแต่ชอบ)
            display.setViewRange(0.5f); // ระยะมองเห็น (ปรับตามเหมาะสม)

            // --- Animation Logic (Client-Side Interpolation) ---
            // ตั้งค่า Transformation เริ่มต้น
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f(0, 0, 0, 1)
            ));

            // กำหนดระยะเวลา Animation (20 Ticks = 1 วินาที)
            display.setInterpolationDuration(20);
            display.setInterpolationDelay(0);

            // กำหนด Transformation ปลายทาง (ลอยขึ้น 1.5 บล็อก)
            // Client จะคำนวณการเคลื่อนที่ให้เอง สมูทมาก และ Server ไม่ต้องคำนวณทุก Tick
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                display.setTransformation(new Transformation(
                        new Vector3f(0, 1.5f, 0), // Move UP
                        new AxisAngle4f(0, 0, 0, 1),
                        new Vector3f(1, 1, 1),
                        new AxisAngle4f(0, 0, 0, 1)
                ));
            }, 1L); // Delay 1 tick to ensure client registers initial state

            // ลบทิ้งเมื่ออนิเมชั่นจบ
            plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, 20L);
        });
    }

    public void shutdown() {
        // No cleanup needed for Bukkit Entities as they persist/despawn naturally or via logic
        // But for safety, we could track and remove, but since they are short-lived, it's fine.
    }
}