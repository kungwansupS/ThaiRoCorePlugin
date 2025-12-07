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
import org.bukkit.inventory.meta.ItemMeta; // **FIXED: ADDED IMPORT**
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
            handleLibraryClick(event, player, title.replace("Library: ", ""));
        }
        // 2. Editor Logic
        else if (title.startsWith("Editor: ")) {
            event.setCancelled(true);
            handleEditorClick(event, player, title);
        }
    }

    private void handleLibraryClick(InventoryClickEvent event, Player player, String dirName) {
        File currentDir = new File(plugin.getItemManager().getRootDir().getParent(), "items/" + dirName);
        if ("Root".equals(dirName)) currentDir = plugin.getItemManager().getRootDir();

        // Create FINAL variables for Lambda use
        final File finalCurrentDir = currentDir;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        String name = clicked.getItemMeta().getDisplayName().replace("§6§l", "").replace("§f", "");

        // Navigation Buttons
        if (clicked.getType() == Material.ARROW) { // Back
            new ItemLibraryGUI(plugin, currentDir.getParentFile()).open(player);
            return;
        }
        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("New Folder")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อ Folder ใหม่:", (str) -> {
                plugin.getItemManager().createFolder(finalCurrentDir, str);
                new ItemLibraryGUI(plugin, finalCurrentDir).open(player);
            });
            return;
        }
        if (clicked.getType() == Material.EMERALD && clicked.getItemMeta().getDisplayName().contains("New Item")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อ Item ใหม่:", (str) -> {
                plugin.getItemManager().createItem(finalCurrentDir, str, Material.STONE);
                new ItemLibraryGUI(plugin, finalCurrentDir).open(player);
            });
            return;
        }

        // File/Folder Interaction
        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        final File finalTarget = target;

        if (target.isDirectory()) {
            if (event.getClick().isLeftClick() && !event.isShiftClick()) {
                new ItemLibraryGUI(plugin, target).open(player);
            } else if (event.isShiftClick() && event.isLeftClick()) { // Delete
                plugin.getItemManager().deleteFile(target);
                new ItemLibraryGUI(plugin, currentDir).open(player);
            } else if (event.isShiftClick() && event.isRightClick()) { // Rename
                plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อใหม่ของ Folder:", (str) -> {
                    plugin.getItemManager().renameFile(finalTarget, str);
                    new ItemLibraryGUI(plugin, finalCurrentDir).open(player);
                });
            }
        } else { // Item File
            if (event.getClick() == ClickType.LEFT) { // Edit
                new AttributeEditorGUI(plugin, target).open(player, Page.GENERAL);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) { // Give
                ItemStack item = plugin.getItemManager().loadItemStack(target);
                player.getInventory().addItem(item);
                player.sendMessage("§aได้รับไอเทมแล้ว!");
            } else if (event.getClick() == ClickType.SHIFT_LEFT) { // Delete
                plugin.getItemManager().deleteFile(target);
                new ItemLibraryGUI(plugin, currentDir).open(player);
            } else if (event.getClick() == ClickType.MIDDLE) { // Duplicate
                plugin.getItemManager().duplicateItem(target);
                new ItemLibraryGUI(plugin, currentDir).open(player);
            }
        }
    }

    private void handleEditorClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring(8, title.indexOf(" ["));
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) {
            player.sendMessage("§cError: File not found.");
            player.closeInventory();
            return;
        }

        final File finalItemFile = itemFile;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        String dp = clicked.getItemMeta().getDisplayName();

        // 1. Navigation
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

        // 2. Actions (General Page)
        if (dp.contains("Rename Item")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อใหม่ (รองรับสี &#RRGGBB):", (str) -> {
                ItemStack stack = plugin.getItemManager().loadItemStack(finalItemFile);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) meta.setDisplayName(str.replace("&", "§"));
                stack.setItemMeta(meta);

                ItemAttribute attr = plugin.getItemManager().loadAttribute(finalItemFile);
                plugin.getItemManager().saveItem(finalItemFile, attr, stack);
                new AttributeEditorGUI(plugin, finalItemFile).open(player, Page.GENERAL);
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
                new AttributeEditorGUI(plugin, finalItemFile).open(player, Page.GENERAL);
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

        // 3. Stat Logic (+/- and Middle Click)
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
                            new AttributeEditorGUI(plugin, finalItemFile).open(player, getPageFromTitle(finalTitle));
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cค่าไม่ถูกต้อง");
                        }
                    });
                    return;
                }

                double current = plugin.getItemAttributeManager().getAttributeValueFromAttrObject(attr, type);
                double change = 0;
                if (event.getClick() == ClickType.LEFT) change = type.getClickStep();
                else if (event.getClick() == ClickType.RIGHT) change = -type.getClickStep();
                else if (event.getClick() == ClickType.SHIFT_LEFT) change = type.getRightClickStep();
                else if (event.getClick() == ClickType.SHIFT_RIGHT) change = -type.getRightClickStep();

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
                } else if (f.getName().equals(name)) {
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
}