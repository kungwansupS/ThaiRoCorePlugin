package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.skill.SkillData;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ItemSkillSelectGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentEntry; // Folder or Skill Pack File
    private final int page;

    // Default constructor (Page 0)
    public ItemSkillSelectGUI(ThaiRoCorePlugin plugin, File currentEntry) {
        this(plugin, currentEntry, 0);
    }

    public ItemSkillSelectGUI(ThaiRoCorePlugin plugin, File currentEntry, int page) {
        this.plugin = plugin;
        this.currentEntry = currentEntry != null ? currentEntry : plugin.getSkillManager().getRootDir();
        this.page = page;
    }

    public void open(Player player) {
        if (currentEntry.isDirectory()) {
            openDirectoryView(player);
        } else if (currentEntry.isFile()) {
            openPackView(player);
        }
    }

    private void openDirectoryView(Player player) {
        String relativePath = plugin.getSkillManager().getRelativePath(currentEntry);
        // Display path in title (Truncated if too long, but we won't rely on it for logic)
        String displayPath = relativePath.length() > 30 ? "..." + relativePath.substring(relativePath.length() - 27) : relativePath;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ItemSkillSelect: " + displayPath + " #P" + page));

        List<File> allFiles = plugin.getSkillManager().listContents(currentEntry);

        // Pagination Logic
        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allFiles.size());

        // Fill Items
        for (int i = startIndex; i < endIndex; i++) {
            File file = allFiles.get(i);
            if (file.isDirectory()) {
                inv.addItem(createGuiItem(Material.CHEST, "§6📂 " + file.getName(), "folder", file.getName(),
                        "§eClick to open folder"));
            } else if (file.getName().endsWith(".yml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Set<String> keys = config.getKeys(false);
                int count = keys.size();

                if (count > 1) {
                    inv.addItem(createGuiItem(Material.ENDER_CHEST, "§d📦 " + file.getName(), "pack", file.getName(),
                            "§7Type: Skill Pack",
                            "§7Contains: §f" + count + " skills",
                            "§eClick to browse pack"));
                } else if (count == 1) {
                    if (!keys.isEmpty()) {
                        String skillId = keys.iterator().next();
                        SkillData skill = plugin.getSkillManager().getSkill(skillId);
                        if (skill != null) {
                            inv.addItem(createSkillItem(skill, "file_skill", skill.getId()));
                        } else {
                            inv.addItem(createGuiItem(Material.BARRIER, "§c" + file.getName(), "error", "", "§7Error loading data"));
                        }
                    }
                } else {
                    inv.addItem(createGuiItem(Material.PAPER, "§7" + file.getName(), "error", "", "§7(Empty File)"));
                }
            }
        }

        // Navigation Bar
        addNavigationButtons(inv, relativePath, allFiles.size(), itemsPerPage);

        fillBackground(inv);
        player.openInventory(inv);
    }

    private void openPackView(Player player) {
        String relativePath = plugin.getSkillManager().getRelativePath(currentEntry);
        String displayPath = relativePath.length() > 30 ? "..." + relativePath.substring(relativePath.length() - 27) : relativePath;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ItemSkillSelect: " + displayPath + " #P" + page));

        YamlConfiguration config = YamlConfiguration.loadConfiguration(currentEntry);
        List<String> skillIds = new ArrayList<>(config.getKeys(false));
        skillIds.sort(String::compareTo);

        // Pagination Logic
        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, skillIds.size());

        for (int i = startIndex; i < endIndex; i++) {
            String skillId = skillIds.get(i);
            SkillData skill = plugin.getSkillManager().getSkill(skillId);
            if (skill != null) {
                inv.addItem(createSkillItem(skill, "pack_skill", skillId));
            } else {
                inv.addItem(createGuiItem(Material.BARRIER, "§c" + skillId, "error", "", "§7Error loading skill."));
            }
        }

        // Navigation Bar
        addNavigationButtons(inv, relativePath, skillIds.size(), itemsPerPage);

        fillBackground(inv);
        player.openInventory(inv);
    }

    private void addNavigationButtons(Inventory inv, String currentPath, int totalItems, int itemsPerPage) {
        // Previous Page
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§ePrevious Page", "prev_page", String.valueOf(page - 1), "§7Go to page " + page));
        } else {
            // Back Button (Only on Page 0)
            if (!currentPath.equals("/")) {
                // Determine Parent Path
                String parentPath;
                if (currentEntry.getParentFile() != null) {
                    parentPath = plugin.getSkillManager().getRelativePath(currentEntry.getParentFile());
                } else {
                    parentPath = "/";
                }

                inv.setItem(45, createGuiItem(Material.OAK_DOOR, "§c§l< BACK", "back", parentPath, "§7Go to parent folder"));
            } else {
                inv.setItem(45, createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL", "cancel", "", "§7Cancel selection"));
            }
        }

        // Next Page
        if (totalItems > (page + 1) * itemsPerPage) {
            inv.setItem(53, createGuiItem(Material.ARROW, "§eNext Page", "next_page", String.valueOf(page + 1), "§7Go to page " + (page + 2)));
        }

        // Info Item
        inv.setItem(49, createGuiItem(Material.BOOK, "§eCurrent: " + currentEntry.getName(), "current_path_info", currentPath,
                "§7Page: " + (page + 1), "§7Total: " + totalItems));
    }

    private ItemStack createSkillItem(SkillData skill, String type, String value) {
        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + skill.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("§8ID: " + skill.getId());
            lore.add("");
            lore.add("§aClick to Select");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            setPDC(meta, type, value);
            // Store context
            String currentRelPath = plugin.getSkillManager().getRelativePath(currentEntry);
            NamespacedKey keyContext = new NamespacedKey(plugin, "ctx_path");
            meta.getPersistentDataContainer().set(keyContext, PersistentDataType.STRING, currentRelPath);

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGuiItem(Material mat, String name, String type, String value, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

            setPDC(meta, type, value);

            // Store context
            String currentRelPath = plugin.getSkillManager().getRelativePath(currentEntry);
            NamespacedKey keyContext = new NamespacedKey(plugin, "ctx_path");
            meta.getPersistentDataContainer().set(keyContext, PersistentDataType.STRING, currentRelPath);

            item.setItemMeta(meta);
        }
        return item;
    }

    private void setPDC(ItemMeta meta, String type, String value) {
        NamespacedKey keyType = new NamespacedKey(plugin, "icon_type");
        NamespacedKey keyValue = new NamespacedKey(plugin, "icon_value");
        meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type);
        meta.getPersistentDataContainer().set(keyValue, PersistentDataType.STRING, value);
    }

    private void fillBackground(Inventory inv) {
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "bg", "");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }
    }
}