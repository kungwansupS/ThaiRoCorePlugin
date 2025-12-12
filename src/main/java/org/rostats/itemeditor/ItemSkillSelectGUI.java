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
    private final File currentEntry; // Can be a Folder or a File (Skill Pack)

    public ItemSkillSelectGUI(ThaiRoCorePlugin plugin, File currentEntry) {
        this.plugin = plugin;
        this.currentEntry = currentEntry != null ? currentEntry : plugin.getSkillManager().getRootDir();
    }

    public void open(Player player) {
        if (currentEntry.isDirectory()) {
            openDirectoryView(player, currentEntry);
        } else if (currentEntry.isFile()) {
            openPackView(player, currentEntry);
        }
    }

    private void openDirectoryView(Player player, File dir) {
        String path = plugin.getSkillManager().getRelativePath(dir);
        String titlePath = path.length() > 24 ? "..." + path.substring(path.length() - 20) : path;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ItemSkillSelect: " + titlePath));

        List<File> files = plugin.getSkillManager().listContents(dir);

        for (File file : files) {
            if (inv.firstEmpty() == -1) break;

            if (file.isDirectory()) {
                inv.addItem(createGuiItem(Material.CHEST, "§6📂 " + file.getName(), "folder", file.getName(),
                        "§eClick to open folder"));
            }
            else if (file.getName().endsWith(".yml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Set<String> keys = config.getKeys(false);
                int count = keys.size();

                if (count > 1) {
                    // Skill Pack
                    inv.addItem(createGuiItem(Material.ENDER_CHEST, "§d📦 " + file.getName(), "pack", file.getName(),
                            "§7Type: Skill Pack",
                            "§7Contains: §f" + count + " skills",
                            "§eClick to browse pack"));
                } else if (count == 1) {
                    // Single Skill File
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
                    // Empty File
                    inv.addItem(createGuiItem(Material.PAPER, "§7" + file.getName(), "error", "", "§7(Empty File)"));
                }
            }
        }

        // Navigation Controls
        if (!path.equals("/")) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§c§l< BACK", "back", "dir", "§7Go to parent folder"));
        } else {
            inv.setItem(45, createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL", "cancel", "", "§7Cancel selection"));
        }

        fillBackground(inv);
        player.openInventory(inv);
    }

    private void openPackView(Player player, File file) {
        String path = plugin.getSkillManager().getRelativePath(file);
        String titlePath = path.length() > 24 ? "..." + path.substring(path.length() - 20) : path;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ItemSkillSelect: " + titlePath));

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String skillId : config.getKeys(false)) {
            SkillData skill = plugin.getSkillManager().getSkill(skillId);
            if (skill != null) {
                inv.addItem(createSkillItem(skill, "pack_skill", skillId));
            } else {
                inv.addItem(createGuiItem(Material.BARRIER, "§c" + skillId, "error", "", "§7Error loading skill."));
            }
        }

        // Back to Folder
        inv.setItem(45, createGuiItem(Material.ARROW, "§c§l< BACK", "back", "pack", "§7Return to folder"));

        fillBackground(inv);
        player.openInventory(inv);
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

            // PDC Data
            NamespacedKey keyType = new NamespacedKey(plugin, "icon_type");
            NamespacedKey keyValue = new NamespacedKey(plugin, "icon_value");
            meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type);
            meta.getPersistentDataContainer().set(keyValue, PersistentDataType.STRING, value);

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

            NamespacedKey keyType = new NamespacedKey(plugin, "icon_type");
            NamespacedKey keyValue = new NamespacedKey(plugin, "icon_value");
            meta.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, type);
            meta.getPersistentDataContainer().set(keyValue, PersistentDataType.STRING, value);

            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBackground(Inventory inv) {
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "bg", "");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }
    }
}