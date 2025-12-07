package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.itemeditor.AttributeEditorGUI.Page;

import java.io.File;

public class GUIListener implements Listener {

    private final ThaiRoCorePlugin plugin;

    public GUIListener(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // 1. Library Logic
        if (title.startsWith("Library: ")) {
            event.setCancelled(true);

            // FIX: เช็คว่าคลิกที่ไหน?
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                // คลิกที่ช่องเก็บของตัวเอง -> ต้องการ Import/Create File จากไอเทมนี้
                handleImportItem(event, player, title.substring(9));
            } else {
                // คลิกที่ GUI -> ต้องการเปิด Folder/File
                String relativePath = title.substring(9);
                handleLibraryClick(event, player, relativePath);
            }
        }
        // 2. Editor Logic
        else if (title.startsWith("Editor: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                handleEditorClick(event, player, title);
            }
        }
    }

    // --- NEW: Handle creating file from inventory item ---
    private void handleImportItem(InventoryClickEvent event, Player player, String relativePath) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        // ตรวจสอบตำแหน่งปัจจุบัน
        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getItemManager().getRootDir();
        final File finalCurrentDir = currentDir;

        // ปิด GUI และถามชื่อไฟล์
        player.closeInventory();
        plugin.getChatInputHandler().awaitInput(player, "§eตั้งชื่อไฟล์สำหรับไอเทมชิ้นนี้:", (name) -> {
            // สร้างไฟล์ใหม่
            String fileName = name.endsWith(".yml") ? name : name + ".yml";
            File newFile = new File(finalCurrentDir, fileName);

            if (newFile.exists()) {
                player.sendMessage("§cไฟล์ชื่อนี้มีอยู่แล้ว!");
                return;
            }

            // อ่าน Attribute ปัจจุบัน (ถ้ามี) หรือสร้างใหม่
            ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
            plugin.getItemManager().saveItem(newFile, attr, item);

            player.sendMessage("§aบันทึกไอเทมลงใน " + fileName + " เรียบร้อยแล้ว!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);

            // เปิดหน้านั้นใหม่
            new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
        });
    }

    private void handleLibraryClick(InventoryClickEvent event, Player player, String relativePath) {
        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);

        if (!currentDir.exists()) {
            currentDir = plugin.getItemManager().getRootDir();
        }

        final File finalCurrentDir = currentDir;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        // ป้องกัน Error กรณีคลิกที่ว่างๆ
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        String name = clicked.getItemMeta().getDisplayName().replace("§6§l", "").replace("§f", "");

        // Navigation Buttons
        if (clicked.getType() == Material.ARROW && clicked.getItemMeta().getDisplayName().contains("Back")) {
            new ItemLibraryGUI(plugin, currentDir.getParentFile()).open(player);
            return;
        }

        // New Folder
        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("New Folder")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อ Folder ใหม่:", (str) -> {
                plugin.getItemManager().createFolder(finalCurrentDir, str);
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
            });
            return;
        }

        // New Item (Empty)
        if (clicked.getType() == Material.EMERALD && clicked.getItemMeta().getDisplayName().contains("New Item")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อ Item ใหม่:", (str) -> {
                plugin.getItemManager().createItem(finalCurrentDir, str, Material.STONE);
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
            });
            return;
        }

        // Handle File/Folder Click
        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        final File finalTarget = target;

        // ป้องกัน Error: File not found ถ้าชื่อไฟล์ไม่ตรงหรือหาไม่เจอ
        if (!target.exists() && clicked.getType() != Material.CHEST) {
            // พยายามหาแบบ Case Insensitive หรือลองเติม .yml
            target = new File(currentDir, name + ".yml");
            if (!target.exists()) {
                // ถ้ายังไม่เจอ อาจเป็นเพราะสีในชื่อ
                // ข้ามไปก่อนเพื่อป้องกัน Error
                return;
            }
        }

        if (target.isDirectory()) {
            if (event.getClick().isLeftClick() && !event.isShiftClick()) {
                new ItemLibraryGUI(plugin, target).open(player);
            } else if (event.isShiftClick() && event.isLeftClick()) {
                plugin.getItemManager().deleteFile(target);
                new ItemLibraryGUI(plugin, currentDir).open(player);
            } else if (event.isShiftClick() && event.isRightClick()) {
                plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อใหม่ของ Folder:", (str) -> {
                    plugin.getItemManager().renameFile(finalTarget, str);
                    new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
                });
            }
        } else { // Item File
            if (event.getClick() == ClickType.LEFT) {
                new AttributeEditorGUI(plugin, target).open(player, Page.GENERAL);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                ItemStack item = plugin.getItemManager().loadItemStack(target);
                player.getInventory().addItem(item);
                player.sendMessage("§aได้รับไอเทมแล้ว!");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                plugin.getItemManager().deleteFile(target);
                new ItemLibraryGUI(plugin, currentDir).open(player);
            } else if (event.getClick() == ClickType.MIDDLE) {
                plugin.getItemManager().duplicateItem(target);
                new ItemLibraryGUI(plugin, currentDir).open(player);
            }
        }
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, String title) {
        int lastSpaceIndex = title.lastIndexOf(" [");
        if (lastSpaceIndex == -1) return;
        String fileName = title.substring(8, lastSpaceIndex);

        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null || !itemFile.exists()) {
            player.sendMessage("§cError: File not found: " + fileName);
            player.closeInventory();
            return;
        }

        final File finalItemFile = itemFile;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String dp = clicked.getItemMeta().getDisplayName();

        if (dp.contains("Back to Library")) {
            new ItemLibraryGUI(plugin, itemFile.getParentFile()).open(player);
            return;
        }
        for (Page p : Page.values()) {
            if (dp.contains(p.name())) {
                new AttributeEditorGUI(plugin, itemFile).open(player, p);
                return;
            }
        }

        // Actions
        if (dp.contains("Rename Item")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อใหม่ (รองรับสี &#RRGGBB):", (str) -> {
                ItemStack stack = plugin.getItemManager().loadItemStack(finalItemFile);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) meta.setDisplayName(str.replace("&", "§"));
                stack.setItemMeta(meta);
                ItemAttribute attr = plugin.getItemManager().loadAttribute(finalItemFile);
                plugin.getItemManager().saveItem(finalItemFile, attr, stack);
                new BukkitRunnableWrapper(plugin, () -> new AttributeEditorGUI(plugin, finalItemFile).open(player, Page.GENERAL));
            });
            return;
        }
        if (dp.contains("Edit Lore")) {
            plugin.getChatInputHandler().awaitMultiLineInput(player, "แก้ไข Lore:", (lines) -> {
                ItemStack stack = plugin.getItemManager().loadItemStack(finalItemFile);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) meta.setLore(lines);
                stack.setItemMeta(meta);
                ItemAttribute attr = plugin.getItemManager().loadAttribute(finalItemFile);
                plugin.getItemManager().saveItem(finalItemFile, attr, stack);
                new BukkitRunnableWrapper(plugin, () -> new AttributeEditorGUI(plugin, finalItemFile).open(player, Page.GENERAL));
            });
            return;
        }
        if (dp.contains("Remove Vanilla")) {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
            attr.setRemoveVanillaAttribute(!attr.isRemoveVanillaAttribute());
            plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
            return;
        }
        if (dp.contains("Save to File")) {
            player.sendMessage("§aSaved!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1, 1);
            return;
        }

        // Stat Logic (+/- and Middle Click)
        for (ItemAttributeType type : ItemAttributeType.values()) {
            if (dp.equals(type.getDisplayName())) {
                ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);

                final ItemAttributeType finalType = type;
                final ItemAttribute finalAttr = attr;
                final String finalTitle = title;

                if (event.getClick() == ClickType.MIDDLE) {
                    plugin.getChatInputHandler().awaitInput(player, "พิมพ์ค่าของ " + type.getKey() + ":", (str) -> {
                        try {
                            double val = Double.parseDouble(str);
                            plugin.getItemAttributeManager().setAttributeToObj(finalAttr, finalType, val);
                            plugin.getItemManager().saveItem(finalItemFile, finalAttr, plugin.getItemManager().loadItemStack(finalItemFile));
                            new BukkitRunnableWrapper(plugin, () -> new AttributeEditorGUI(plugin, finalItemFile).open(player, getPageFromTitle(finalTitle)));
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cค่าไม่ถูกต้อง");
                        }
                    });
                    return;
                }

                double current = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(attr, type);
                double change = 0;

                // *** FIX: Shift +/- 10 Logic ***
                if (event.getClick() == ClickType.LEFT) {
                    change = type.getClickStep(); // +1
                } else if (event.getClick() == ClickType.RIGHT) {
                    change = -type.getClickStep(); // -1
                } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    change = type.getClickStep() * 10; // +10
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    change = -type.getClickStep() * 10; // -10
                }

                plugin.getItemAttributeManager().setAttributeToObj(attr, type, current + change);
                plugin.getItemManager().saveItem(itemFile, attr, plugin.getItemManager().loadItemStack(itemFile));

                new AttributeEditorGUI(plugin, itemFile).open(player, getPageFromTitle(title));
                return;
            }
        }
    }

    private File findFileByName(File dir, String name) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    File found = findFileByName(f, name);
                    if (found != null) return found;
                } else if (f.getName().equals(name) || f.getName().equals(name + ".yml")) {
                    return f;
                }
            }
        }
        return null;
    }

    private Page getPageFromTitle(String title) {
        for (Page p : Page.values()) {
            if (title.contains(p.name())) return p;
        }
        return Page.GENERAL;
    }

    private static class BukkitRunnableWrapper {
        public BukkitRunnableWrapper(ThaiRoCorePlugin plugin, Runnable r) {
            plugin.getServer().getScheduler().runTask(plugin, r);
        }
    }
}