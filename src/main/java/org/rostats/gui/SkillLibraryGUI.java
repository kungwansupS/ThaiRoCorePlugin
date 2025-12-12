package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.skill.SkillData;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SkillLibraryGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentEntry; // เป็นได้ทั้ง Folder จริง หรือ File (Pack)
    private final int page;

    public SkillLibraryGUI(ThaiRoCorePlugin plugin) {
        this(plugin, plugin.getSkillManager().getRootDir(), 0);
    }

    public SkillLibraryGUI(ThaiRoCorePlugin plugin, File currentEntry) {
        this(plugin, currentEntry, 0);
    }

    public SkillLibraryGUI(ThaiRoCorePlugin plugin, File currentEntry, int page) {
        this.plugin = plugin;
        this.currentEntry = currentEntry != null ? currentEntry : plugin.getSkillManager().getRootDir();
        this.page = page;
    }

    public void open(Player player) {
        if (currentEntry.isDirectory()) {
            openDirectoryView(player, currentEntry);
        } else if (currentEntry.isFile()) {
            openPackView(player, currentEntry);
        }
    }

    // [Select Mode] - เรียกใช้ผ่าน GUIListener
    public void openSelectMode(Player player) {
        player.sendMessage("§ePlease select a skill from the library...");
        open(player);
    }

    public void openConfirmDelete(Player player, File target) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Delete: " + target.getName()));

        inv.setItem(3, createGuiItem(Material.LIME_CONCRETE, "§a§lCONFIRM DELETE",
                "§7Target: " + target.getName(),
                "§c§lWARNING: Cannot be undone!"));

        inv.setItem(5, createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL", "§7Return."));

        player.openInventory(inv);
    }

    // --- View 1: Folder จริง ---
    private void openDirectoryView(Player player, File dir) {
        String path = plugin.getSkillManager().getRelativePath(dir);
        // Truncate path for title if too long
        String titlePath = path.length() > 24 ? "..." + path.substring(path.length() - 20) : path;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Lib: " + titlePath + " #P" + page));

        List<File> files = plugin.getSkillManager().listContents(dir);

        // Pagination Logic
        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, files.size());

        for (int i = startIndex; i < endIndex; i++) {
            File file = files.get(i);
            if (file.isDirectory()) {
                inv.addItem(createGuiItem(Material.CHEST, "§6📂 " + file.getName(),
                        "§7Type: Folder", "§eClick to open."));
            }
            else if (file.getName().endsWith(".yml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Set<String> keys = config.getKeys(false);
                int count = keys.size();

                if (count > 1) {
                    inv.addItem(createGuiItem(Material.ENDER_CHEST, "§d📦 " + file.getName(),
                            "§7Type: Skill Pack",
                            "§7Contains: §f" + count + " skills",
                            "§eClick to open pack."));
                } else if (count == 1) {
                    if (!keys.isEmpty()) {
                        String skillId = keys.iterator().next();
                        SkillData skill = plugin.getSkillManager().getSkill(skillId);
                        if (skill != null) {
                            inv.addItem(createSkillItem(skill, "File: " + file.getName()));
                        } else {
                            inv.addItem(createGuiItem(Material.BARRIER, "§c" + file.getName(), "§7Error loading data"));
                        }
                    }
                } else {
                    inv.addItem(createGuiItem(Material.PAPER, "§7" + file.getName(), "§7(Empty File)"));
                }
            }
        }

        addNavigationButtons(inv, path, files.size(), itemsPerPage);

        // Action Buttons (Bottom Row)
        inv.setItem(48, createGuiItem(Material.CHEST, "§6+ New Folder", "§7Create a sub-folder"));
        inv.setItem(49, createGuiItem(Material.PAPER, "§e+ New Skill", "§7Create a single skill file"));
        inv.setItem(50, createGuiItem(Material.ENDER_CHEST, "§d+ New Pack", "§7Create a multi-skill pack"));

        player.openInventory(inv);
    }

    // --- View 2: ภายในไฟล์ (Skill Pack) ---
    private void openPackView(Player player, File file) {
        String path = plugin.getSkillManager().getRelativePath(file);
        String titlePath = path.length() > 24 ? "..." + path.substring(path.length() - 20) : path;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Pack: " + titlePath + " #P" + page));

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> skillKeys = new ArrayList<>(config.getKeys(false));
        Collections.sort(skillKeys); // Sort A-Z

        // Pagination Logic
        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, skillKeys.size());

        for (int i = startIndex; i < endIndex; i++) {
            String skillId = skillKeys.get(i);
            SkillData skill = plugin.getSkillManager().getSkill(skillId);
            if (skill != null) {
                inv.addItem(createSkillItem(skill, "ID: " + skillId));
            } else {
                inv.addItem(createGuiItem(Material.BARRIER, "§c" + skillId, "§7Error loading skill."));
            }
        }

        addNavigationButtons(inv, path, skillKeys.size(), itemsPerPage);

        inv.setItem(53, createGuiItem(Material.LIME_DYE, "§a+ Add Skill", "§7Add another skill to this pack"));

        player.openInventory(inv);
    }

    private void addNavigationButtons(Inventory inv, String currentPath, int totalItems, int itemsPerPage) {
        // Prev Page
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§ePrevious Page", "§7Go to page " + page));
        } else {
            // Back Button (Only on Page 0)
            if (!currentPath.equals("/")) {
                inv.setItem(45, createGuiItem(Material.ARROW, "§c§l< BACK", "§7Go to parent folder"));
            }
        }

        // Next Page
        if (totalItems > (page + 1) * itemsPerPage) {
            inv.setItem(53, createGuiItem(Material.ARROW, "§eNext Page", "§7Go to page " + (page + 2)));
        }
    }

    private ItemStack createSkillItem(SkillData skill, String subInfo) {
        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + skill.getDisplayName());
            List<String> lore = new ArrayList<>();
            lore.add("§8ID: " + skill.getId());
            lore.add("§7" + subInfo);
            lore.add("");
            lore.add("§eClick to Edit/Select");
            lore.add("§cRight-Click to Delete");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}