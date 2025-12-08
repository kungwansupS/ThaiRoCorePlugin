package org.rostats.itemeditor;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.itemeditor.AttributeEditorGUI.Page;
import org.rostats.itemeditor.EffectEnchantGUI.Mode;

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
            if (event.getClickedInventory() == event.getView().getBottomInventory()) {
                String relativePath = title.substring(9);
                handleImportItem(event, player, relativePath);
            } else {
                String relativePath = title.substring(9);
                handleLibraryClick(event, player, relativePath);
            }
        }
        // 2. Editor Logic
        else if (title.startsWith("Editor: ")) {
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) return;

            // Handle Effect/Enchant GUI
            if (title.contains("EFFECT Select]") || title.contains("ENCHANT Select]")) {
                handleEffectEnchantClick(event, player, title);
                return;
            }

            handleEditorClick(event, player, title);
        }
        // 3. Confirm Logic
        else if (title.startsWith("Confirm Delete: ")) {
            event.setCancelled(true);
            handleConfirmDeleteClick(event, player, title);
        }
    }

    private void handleEffectEnchantClick(InventoryClickEvent event, Player player, String title) {
        int lastBracket = title.lastIndexOf(" [");
        if (lastBracket == -1) return;
        String fileName = title.substring(8, lastBracket);
        File itemFile = findFileByName(plugin.getItemManager().getRootDir(), fileName);
        if (itemFile == null) return;

        Mode mode = title.contains("EFFECT") ? Mode.EFFECT : Mode.ENCHANT;
        String metaKey = "RO_EDITOR_SEL_" + mode.name();
        String selected = player.hasMetadata(metaKey) ? player.getMetadata(metaKey).get(0).asString() : null;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        int slot = event.getSlot();

        // 1. Selection (Slots 0-44)
        if (slot < 45) {
            if (clicked.getType() == Material.AIR) return;
            // Name format: "§aName" or "§7Name". Substring(2) removes color code.
            String dp = clicked.getItemMeta().getDisplayName();
            String key = dp.length() > 2 ? dp.substring(2) : dp;

            player.setMetadata(metaKey, new FixedMetadataValue(plugin, key));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

            // Refresh GUI
            new EffectEnchantGUI(plugin, itemFile, mode).open(player);
        }

        // 2. Actions (Slots 49-53)
        else if (slot == 50 && selected != null) { // Set Level
            plugin.getChatInputHandler().awaitInput(player, "Enter Level for " + selected + ":", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    applyEffectEnchant(player, itemFile, mode, selected, lvl, true);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid Number");
                    new BukkitRunnableWrapper(plugin, () -> new EffectEnchantGUI(plugin, itemFile, mode).open(player));
                }
            });
        }
        else if (slot == 51 && selected != null) { // Add/Update (Use default 1 or prompt? Let's prompt if not set, or set 1)
            // For simplicity, Button 51 will imply adding Level 1 if not used via Anvil, OR confirms input.
            // Let's make it just apply Level 1 for quick add
            applyEffectEnchant(player, itemFile, mode, selected, 1, true);
        }
        else if (slot == 52 && selected != null) { // Remove
            applyEffectEnchant(player, itemFile, mode, selected, 0, false);
        }
        else if (slot == 53) { // Back
            player.removeMetadata(metaKey, plugin);
            new AttributeEditorGUI(plugin, itemFile).open(player, Page.GENERAL);
        }
    }

    private void applyEffectEnchant(Player player, File file, Mode mode, String key, int level, boolean add) {
        // Helper wrapper to run on main thread if coming from async chat
        new BukkitRunnableWrapper(plugin, () -> {
            ItemAttribute attr = plugin.getItemManager().loadAttribute(file);
            ItemStack stack = plugin.getItemManager().loadItemStack(file);
            boolean changed = false;

            if (mode == Mode.EFFECT) {
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type != null) {
                    if (add) {
                        attr.getPotionEffects().put(type, level);
                        player.sendMessage("§aSet Effect: " + type.getName() + " Lv." + level);
                    } else {
                        attr.getPotionEffects().remove(type);
                        player.sendMessage("§cRemoved Effect: " + type.getName());
                    }
                    plugin.getItemManager().saveItem(file, attr, stack);
                    changed = true;
                }
            } else {
                Enchantment ench = null;
                for (Enchantment e : Enchantment.values()) {
                    if (e.getKey().getKey().equalsIgnoreCase(key)) {
                        ench = e;
                        break;
                    }
                }
                if (ench != null) {
                    ItemMeta meta = stack.getItemMeta();
                    if (add) {
                        meta.addEnchant(ench, level, true);
                        player.sendMessage("§aSet Enchant: " + key + " Lv." + level);
                    } else {
                        meta.removeEnchant(ench);
                        player.sendMessage("§cRemoved Enchant: " + key);
                    }
                    stack.setItemMeta(meta);
                    plugin.getItemManager().saveItem(file, attr, stack);
                    changed = true;
                }
            }

            if (changed) {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                new EffectEnchantGUI(plugin, file, mode).open(player);
            }
        });
    }

    // ... (Keep existing handleConfirmDeleteClick, handleImportItem, handleLibraryClick, handleEditorClick, findFileByName, getPageFromTitle, BukkitRunnableWrapper) ...
    // DO NOT REMOVE the other methods from the previous GUIListener file.
    // Ensure you paste the FULL GUIListener file content combining the above method with the old ones.

    // For completeness of this answer, here is the REST of GUIListener methods to ensure you have the full file structure:

    private void handleConfirmDeleteClick(InventoryClickEvent event, Player player, String title) {
        String fileName = title.substring("Confirm Delete: ".length());
        File target = findFileByName(plugin.getItemManager().getRootDir(), fileName);

        if (event.getCurrentItem() == null) return;
        String dp = event.getCurrentItem().getItemMeta().getDisplayName();

        if (dp.contains("CONFIRM DELETE")) {
            if (target != null && target.exists()) {
                File parent = target.getParentFile();
                plugin.getItemManager().deleteFile(target);
                player.sendMessage("§cDeleted: " + fileName);
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, parent).open(player));
            } else {
                player.sendMessage("§cFile not found.");
                player.closeInventory();
            }
        } else if (dp.contains("CANCEL")) {
            if (target != null && target.exists()) {
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, target.getParentFile()).open(player));
            } else {
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, plugin.getItemManager().getRootDir()).open(player));
            }
        }
    }

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

        if (clicked.getType() == Material.ARROW && clicked.getItemMeta().getDisplayName().contains("Back")) {
            new ItemLibraryGUI(plugin, currentDir.getParentFile()).open(player);
            return;
        }

        if (clicked.getType() == Material.CHEST && clicked.getItemMeta().getDisplayName().contains("New Folder")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อ Folder ใหม่:", (str) -> {
                plugin.getItemManager().createFolder(finalCurrentDir, str);
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
            });
            return;
        }

        if (clicked.getType() == Material.EMERALD && clicked.getItemMeta().getDisplayName().contains("New Item")) {
            plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อ Item ใหม่:", (str) -> {
                plugin.getItemManager().createItem(finalCurrentDir, str, Material.STONE);
                new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
            });
            return;
        }

        File target = new File(currentDir, name + (clicked.getType() == Material.CHEST ? "" : ".yml"));
        final File finalTarget = target;

        if (!target.exists() && clicked.getType() != Material.CHEST) {
            target = new File(currentDir, name + ".yml");
            if (!target.exists()) return;
        }

        if (target.isDirectory()) {
            if (event.getClick().isLeftClick() && !event.isShiftClick()) {
                new ItemLibraryGUI(plugin, target).open(player);
            } else if (event.isShiftClick() && event.isLeftClick()) {
                new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
            } else if (event.isShiftClick() && event.isRightClick()) {
                plugin.getChatInputHandler().awaitInput(player, "พิมพ์ชื่อใหม่ของ Folder:", (str) -> {
                    plugin.getItemManager().renameFile(finalTarget, str);
                    new BukkitRunnableWrapper(plugin, () -> new ItemLibraryGUI(plugin, finalCurrentDir).open(player));
                });
            }
        } else {
            if (event.getClick() == ClickType.LEFT) {
                new AttributeEditorGUI(plugin, target).open(player, Page.GENERAL);
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                ItemStack item = plugin.getItemManager().loadItemStack(target);
                player.getInventory().addItem(item);
                player.sendMessage("§aได้รับไอเทมแล้ว!");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                new ItemLibraryGUI(plugin, currentDir).openConfirmDelete(player, target);
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

        if (dp.contains("Edit Effects")) {
            new EffectEnchantGUI(plugin, finalItemFile, Mode.EFFECT).open(player);
            return;
        }
        if (dp.contains("Edit Enchantments")) {
            new EffectEnchantGUI(plugin, finalItemFile, Mode.ENCHANT).open(player);
            return;
        }

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

        // Handle normal attribute clicks
        for (ItemAttributeType type : ItemAttributeType.values()) {
            if (dp.equals(type.getDisplayName())) {
                ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
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