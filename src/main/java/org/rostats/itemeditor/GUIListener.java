package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
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

            // เช็คว่าคลิกที่ Inventory ของผู้เล่นหรือไม่?
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                // คลิกของในตัว -> เข้าสู่โหมด Import Item
                String relativePath = title.substring(9);
                handleImportItem(event, player, relativePath);
            } else {
                // คลิกใน Library GUI
                String relativePath = title.substring(9);
                handleLibraryClick(event, player, relativePath);
            }
        }
        // 2. Editor Logic
        else if (title.startsWith("Editor: ")) {
            event.setCancelled(true);
            // คลิกเฉพาะใน Editor ด้านบน
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                handleEditorClick(event, player, title);
            }
        }
        // 3. Confirm Logic (NEW)
        else if (title.startsWith("Confirm Delete: ")) {
            event.setCancelled(true);
            handleConfirmDeleteClick(event, player, title);
        }
    }

    // --- NEW: Handle Confirm Delete Click ---
    private void handleConfirmDeleteClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring("Confirm Delete: ".length());

        // Find the file again (Recursive search to be safe)
        File target = findFileByName(plugin.getItemManager().getRootDir(), fileName);

        if (event.getCurrentItem() == null) return;
        String dp = event.getCurrentItem().getItemMeta().getDisplayName();

        if (dp.contains("CONFIRM DELETE")) {
            if (target != null && target.exists()) {
                File parent = target.getParentFile();
                plugin.getItemManager().deleteFile(target);
                player.sendMessage("§cDeleted: " + fileName);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);

                // Go back to library parent
                new BukkitRunnableWrapper(plugin, () ->
                        new ItemLibraryGUI(plugin, parent).open(player));
            } else {
                player.sendMessage("§cFile not found.");
                player.closeInventory();
            }
        } else if (dp.contains("CANCEL")) {
            if (target != null && target.exists()) {
                // Go back to library
                new BukkitRunnableWrapper(plugin, () ->
                        new ItemLibraryGUI(plugin, target.getParentFile()).open(player));
            } else {
                // Return to root if lost
                new BukkitRunnableWrapper(plugin, () ->
                        new ItemLibraryGUI(plugin, plugin.getItemManager().getRootDir()).open(player));
            }
        }
    }

    // --- Import Logic ---
    private void handleImportItem(InventoryClickEvent event, Player player, String relativePath) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getItemManager().getRootDir();
        final File finalCurrentDir = currentDir;

        player.closeInventory();
        plugin.getChatInputHandler().awaitInput(player, "§eตั้งชื่อไฟล์สำหรับไอเทมนี้ (ไม่ต้องใส่ .yml):", (name) -> {
            String fileName = name.endsWith(".yml") ? name : name + ".yml";
            File newFile = new File(finalCurrentDir, fileName);

            if (newFile.exists()) {
                player.sendMessage("§cไฟล์ชื่อนี้มีอยู่แล้ว!");
                return;
            }

            // อ่าน Attribute จากไอเทมจริง
            ItemAttribute attr = plugin.getItemAttributeManager().readFromItem(item);
            plugin.getItemManager().saveItem(newFile, attr, item);

            player.sendMessage("§aนำเข้าไอเทมเรียบร้อย!");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);

            new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
        });
    }

    private void handleLibraryClick(InventoryClickEvent event, Player player, String relativePath) {
        File currentDir = plugin.getItemManager().getFileFromRelative(relativePath);
        if (!currentDir.exists()) currentDir = plugin.getItemManager().getRootDir();
        final File finalCurrentDir = currentDir;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
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

        // New Item
        if (clicked.getType() == Material.EMERALD && clicked.getItemMeta().getDisplayName().contains("New Item")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อ Item ใหม่:", (str) -> {
                plugin.getItemManager().createItem(finalCurrentDir, str, Material.STONE);
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
            });
            return;
        }

        // Handle File/Folder
        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        final File finalTarget = target;

        // Fallback search if name mismatch
        if (!target.exists() && clicked.getType() != Material.CHEST) {
            target = new File(currentDir, name + ".yml");
            if (!target.exists()) return;
        }

        if (target.isDirectory()) {
            if (event.getClick().isLeftClick() && !event.isShiftClick()) {
                new ItemLibraryGUI(plugin, target).open(player);
            } else if (event.isShiftClick() && event.isLeftClick()) {
                // FIX: Open Confirmation for folder delete
                new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
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
                // FIX: Open Confirmation for item delete
                new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            }
            // REMOVED: Middle Click (Duplicate) Logic
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

        for (ItemAttributeType type : ItemAttributeType.values()) {
            if (dp.equals(type.getDisplayName())) {
                ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);

                double current = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(attr, type);
                double change = 0;

                if (event.getClick() == ClickType.LEFT) {
                    change = type.getClickStep();
                } else if (event.getClick() == ClickType.RIGHT) {
                    change = -type.getClickStep();
                } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                    // FIX: Use getRightClickStep (large step) to match lore
                    change = type.getRightClickStep();
                } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                    // FIX: Use getRightClickStep (large step) to match lore
                    change = -type.getRightClickStep();
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