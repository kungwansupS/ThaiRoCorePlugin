package org.rostats.gui;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.gui.CharacterGUI.Tab;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    // Helper Method: เปิด GUI ใหม่พร้อมตั้งค่า Metadata เพื่อป้องกันการ Clear Stats ผิดพลาด
    private void openGUI(Player player, Tab tab) {
        // ตั้งค่า Flag ว่ากำลังสลับหน้าจอ (เพื่อไม่ให้ onClose ทำงานล้างค่า)
        player.setMetadata("ROSTATS_SWITCH", new FixedMetadataValue(plugin, true));
        new CharacterGUI(plugin).open(player, tab);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        // ตรวจสอบว่าเป็นหน้าต่าง Character Status หรือไม่
        if (!title.contains("Character Status (ROO)")) return;

        // ถ้ามี Metadata "ROSTATS_SWITCH" แสดงว่าเป็นการเปลี่ยน Tab หรือ Refresh หน้าจอ -> ไม่ต้องล้างค่า
        if (player.hasMetadata("ROSTATS_SWITCH")) {
            player.removeMetadata("ROSTATS_SWITCH", plugin);
            return;
        }

        // กรณีปิดหน้าต่างจริงๆ (กด E หรือ ESC) -> ล้างค่า Pending ทั้งหมด
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        // ตรวจสอบว่ามีค่าที่ยังไม่ได้ Allocate หรือไม่
        if (plugin.getStatManager().getTotalPendingCost(data) > 0) {
            data.clearAllPendingStats();
            plugin.getAttributeHandler().updatePlayerStats(player); // รีเซ็ต Attribute ของผู้เล่นกลับเป็นค่าจริง
            plugin.getManaManager().updateBar(player);
            player.sendMessage("§e[System] ยกเลิกค่า Stat ที่ยังไม่ได้ยืนยัน (Allocate)");
            // แก้ไขชื่อ Sound เป็นตัวพิมพ์ใหญ่
            player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1f, 1f);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (!title.contains("Character Status (ROO)")) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        String name = PlainTextComponentSerializer.plainText().serialize(event.getCurrentItem().displayName());
        int slot = event.getSlot();

        // 1. Handle Tab Clicks (R0 C2, C3, C4, C5)
        // เปลี่ยนไปใช้ openGUI เพื่อป้องกันการล้างค่าตอนเปลี่ยน Tab
        if (slot == 2) openGUI(player, Tab.BASIC_INFO);
        else if (slot == 3) openGUI(player, Tab.GENERAL);
        else if (slot == 4) openGUI(player, Tab.ADVANCED);
        else if (slot == 5) openGUI(player, Tab.SPECIAL);
        else if (slot == 34) openGUI(player, Tab.RESET_CONFIRM);
        else if (slot == 44) openGUI(player, Tab.RESET_CONFIRM);

            // 2. Handle Close/Exit Button (Slot 8)
        else if (slot == 8) {
            player.closeInventory(); // onClose จะทำงานและล้างค่าให้เอง
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        }

        // 3. Handle Reset/Confirm Buttons (in RESET_CONFIRM tab)
        else if (slot == 29 && name.contains("[ยืนยัน]")) {
            performReset(player);
            player.closeInventory();
        }
        else if (slot == 31 && name.contains("[ยกเลิก]")) {
            openGUI(player, Tab.BASIC_INFO);
        }

        // 4. Handle Allocate (Slot 52)
        else if (slot == 52 && name.contains("Allocate")) {
            plugin.getStatManager().allocateStats(player);
            openGUI(player, Tab.BASIC_INFO);
        }

        // 5. Handle Reset Select (Slot 42)
        else if (slot == 42 && name.contains("Reset Select")) {
            plugin.getStatManager().getData(player.getUniqueId()).clearAllPendingStats();
            player.sendMessage("§e[System] Pending Stat Changes have been cleared.");
            openGUI(player, Tab.BASIC_INFO);
        }

        // 6. Handle Stat Allocation (+ and - Buttons)
        String plusKey = getStatKey(slot, 36);
        if (plusKey != null && event.getCurrentItem().getType() == Material.GREEN_STAINED_GLASS_PANE) {
            handleStatUpgrade(player, plusKey, event.isLeftClick(), event.isRightClick());
        }

        String minusKey = getStatKey(slot, 45);
        if (minusKey != null && event.getCurrentItem().getType() == Material.RED_STAINED_GLASS_PANE) {
            handleStatDowngrade(player, minusKey, event.isLeftClick(), event.isRightClick());
        }
    }

    private String getStatKey(int slot, int startSlot) {
        if (slot < startSlot || slot > startSlot + 5) return null;
        return switch (slot - startSlot) {
            case 0 -> "STR";
            case 1 -> "AGI";
            case 2 -> "VIT";
            case 3 -> "INT";
            case 4 -> "DEX";
            case 5 -> "LUK";
            default -> null;
        };
    }

    private void handleStatUpgrade(Player player, String statKey, boolean isLeftClick, boolean isRightClick) {
        boolean success = false;
        if (isLeftClick) {
            success = plugin.getStatManager().upgradeStat(player, statKey);
        } else if (isRightClick) {
            int count = 0;
            while (count < 10 && plugin.getStatManager().upgradeStat(player, statKey)) {
                count++;
                success = true;
            }
        }

        if (success) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            plugin.getAttributeHandler().updatePlayerStats(player);
            plugin.getManaManager().updateBar(player);
            openGUI(player, Tab.BASIC_INFO); // ใช้ openGUI เพื่อ Refresh หน้าจอโดยไม่ล้างค่า
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            player.sendMessage("§cNot enough points to reserve this upgrade!");
        }
    }

    private void handleStatDowngrade(Player player, String statKey, boolean isLeftClick, boolean isRightClick) {
        boolean success = false;
        if (isLeftClick) {
            success = plugin.getStatManager().downgradeStat(player, statKey);
        } else if (isRightClick) {
            int count = 0;
            while (count < 10 && plugin.getStatManager().downgradeStat(player, statKey)) {
                count++;
                success = true;
            }
        }

        if (success) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            plugin.getAttributeHandler().updatePlayerStats(player);
            plugin.getManaManager().updateBar(player);
            openGUI(player, Tab.BASIC_INFO); // ใช้ openGUI เพื่อ Refresh หน้าจอโดยไม่ล้างค่า
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 0.5f);
            player.sendMessage("§cCannot reduce stat below base value (1) or pending points are 0!");
        }
    }

    private void performReset(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        int freeResets = plugin.getConfig().getInt("reset-system.free-resets", 3);
        int usedResets = data.getResetCount();
        Material resetItem = Material.getMaterial(plugin.getConfig().getString("reset-system.reset-item", "NETHER_STAR"));

        if (usedResets < freeResets) {
            data.resetStats();
            data.incrementResetCount();
            player.sendMessage("§eFree Reset used! (" + (usedResets + 1) + "/" + freeResets + ")");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else if (resetItem != null && player.getInventory().contains(resetItem)) {
            player.getInventory().removeItem(new ItemStack(resetItem, 1));
            data.resetStats();
            data.incrementResetCount();
            player.sendMessage("§bUsed 1x " + resetItem.name() + " to reset!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        } else {
            player.sendMessage("§cNo free resets! You need 1x " + (resetItem != null ? resetItem.name() : "NETHER_STAR") + ".");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        plugin.getAttributeHandler().updatePlayerStats(player);
        plugin.getManaManager().updateBar(player);
    }
}