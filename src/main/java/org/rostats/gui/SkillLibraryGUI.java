package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.YamlConfiguration;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.skill.SkillData;
import org.rostats.engine.skill.SkillManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillLibraryGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentDir;
    private final int page;

    // Editor State
    private final boolean isSelectMode;
    private final String currentEditingSkillId; // Not used in Select Mode, but kept for compatibility
    private final int bindingIndex;
    private final String targetItemId;

    // Normal Constructor
    public SkillLibraryGUI(ThaiRoCorePlugin plugin, File currentDir, int page) {
        this(plugin, currentDir, page, false, null, -1, null);
    }

    // Full Constructor
    public SkillLibraryGUI(ThaiRoCorePlugin plugin, File currentDir, int page, boolean isSelectMode, String currentEditingSkillId, int bindingIndex, String targetItemId) {
        this.plugin = plugin;
        this.currentDir = currentDir;
        this.page = page;
        this.isSelectMode = isSelectMode;
        this.currentEditingSkillId = currentEditingSkillId;
        this.bindingIndex = bindingIndex;
        this.targetItemId = targetItemId;
    }

    public void open(Player player) {
        String title = isSelectMode ? "SkillSelect:" : "SkillLibrary:";
        // Shorten path for title limit
        String path = currentDir.getName().equals("skills") ? "/" : currentDir.getName();
        invOpen(player, title + " " + path, page);
    }

    private void invOpen(Player player, String titlePrefix, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(titlePrefix + " #P" + page));
        SkillManager manager = plugin.getSkillManager();
        List<File> contents = manager.listContents(currentDir);

        int start = page * 45;
        int max = Math.min(contents.size(), start + 45);

        // If Select Mode -> We might want to list ALL skills inside files if we are inside a pack?
        // Current logic: List Files. If user clicks a .yml pack, we handle it?
        // BETTER LOGIC for Select Mode:
        // 1. If currentDir contains .yml files, list them as Packs.
        // 2. Ideally, we want to SEE the skills to select them.

        // Mixed View: Show Folders + Show Skills from files in this folder
        // This is complex. Let's stick to File browsing.
        // BUT, if isSelectMode is true, we must allow clicking a Pack to see its skills?
        // Or simply listing all skills from all files in currentDir?

        // Let's implement: List Files/Folders.
        // AND if a file is a .yml, list the SKILLS inside it instead of the file itself?
        // OR list the file, and when clicked, enter it like a folder? -> "Pseudo-folder" logic

        // Simplified Logic for Full Code: List Files.
        // If it's a .yml file, we display it.
        // In GUIListener, checking "SkillSelect" mode, if they click a .yml, we treat it as a container.

        // Actually, to make "Skill Packs" work seamlessly:
        // We will iterate files. If it's a directory, show Chest.
        // If it's a .yml, parse it and show ALL skills inside it as individual items.

        List<ItemStack> displayItems = new ArrayList<>();

        for (File file : contents) {
            if (file.isDirectory()) {
                displayItems.add(createGuiItem(Material.CHEST, "§aFolder: " + file.getName()));
            } else if (file.getName().endsWith(".yml")) {
                if (isSelectMode) {
                    // Expand Skills
                    try {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        for (String key : config.getKeys(false)) {
                            SkillData skill = manager.getSkill(key);
                            if (skill != null) {
                                displayItems.add(createSkillItem(skill));
                            }
                        }
                    } catch (Exception e) {}
                } else {
                    // Normal Mode: Show Pack
                    displayItems.add(createGuiItem(Material.PAPER, "§bSkill Pack: §f" + file.getName()));
                }
            }
        }

        // Pagination logic for displayItems
        int itemStart = page * 45;
        int itemMax = Math.min(displayItems.size(), itemStart + 45);

        for (int i = itemStart; i < itemMax; i++) {
            inv.setItem(i - itemStart, displayItems.get(i));
        }

        // --- Footer ---
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, bg);

        // Navigation
        if (page > 0) inv.setItem(48, createGuiItem(Material.ARROW, "§ePrevious Page"));
        if (displayItems.size() > itemMax) inv.setItem(50, createGuiItem(Material.ARROW, "§eNext Page"));

        if (!currentDir.equals(manager.getRootDir())) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§cBack", "§7Up one level"));
        }

        // Context Info for Select Mode
        if (isSelectMode) {
            ItemStack info = createGuiItem(Material.BOOK, "§eSelecting Skill", "§7Target Item: " + targetItemId);
            // Hide context data in lore for GUIListener to reconstruct state
            ItemMeta meta = info.getItemMeta();
            List<String> lore = meta.getLore();
            lore.add("§0INDEX:" + bindingIndex); // Hidden
            meta.setLore(lore);
            info.setItemMeta(meta);

            // Put in slot 53 (Back/Info)
            ItemStack backBtn = createGuiItem(Material.RED_BED, "§cBack to Skill Binding", "§7(Skill: " + targetItemId + ")");
            ItemMeta backMeta = backBtn.getItemMeta();
            backMeta.setLore(lore); // Pass the hidden lore
            backBtn.setItemMeta(backMeta);

            inv.setItem(53, backBtn);
        }

        player.openInventory(inv);
    }

    private ItemStack createSkillItem(SkillData skill) {
        Material icon = skill.getIcon() != null ? skill.getIcon() : Material.BOOK;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        String displayName = skill.getDisplayName() != null ? skill.getDisplayName().replace("&", "§") : skill.getId();
        meta.setDisplayName("§e[Select] " + displayName);

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: " + skill.getId());
        lore.add("§7Type: " + skill.getSkillType());
        lore.add("§7Cooldown: " + skill.getCooldownBase());
        lore.add("");
        lore.add("§aClick to Select");

        // [IMPORTANT] Hidden ID for GUIListener
        lore.add("§0SKILL_ID:" + skill.getId());

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
}