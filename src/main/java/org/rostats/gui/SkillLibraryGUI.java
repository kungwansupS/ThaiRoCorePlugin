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
import java.util.ArrayList; // สำหรับ new ArrayList<>()
import java.util.Arrays;    // [FIX] เพิ่มบรรทัดนี้เพื่อแก้ Error Arrays.asList()
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SkillLibraryGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentEntry; // เป็นได้ทั้ง Folder จริง หรือ File (Pack)

    // Constructor 1: สำหรับเรียกจากหน้า Root หรือ GUIListener (แก้ Error Expected 2 arguments but found 1)
    public SkillLibraryGUI(ThaiRoCorePlugin plugin) {
        this(plugin, plugin.getSkillManager().getRootDir());
    }

    // Constructor 2: สำหรับเปิด Folder หรือ File เฉพาะเจาะจง
    public SkillLibraryGUI(ThaiRoCorePlugin plugin, File currentEntry) {
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

    public void openSelectMode(Player player, Consumer<String> onSelect) {
        GUIListener.setSelectionCallback(player, onSelect);
        player.sendMessage("§ePlease select a skill...");
        open(player);
    }

    public void openConfirmDelete(Player player, File target) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Delete: " + target.getName()));
        inv.setItem(3, createGuiItem(Material.LIME_CONCRETE, "§a§lCONFIRM DELETE",
                "§7Target: " + target.getName(), "§c§lWARNING: Cannot be undone!"));
        inv.setItem(5, createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL", "§7Return."));
        player.openInventory(inv);
    }

    // --- View 1: Folder จริง ---
    private void openDirectoryView(Player player, File dir) {
        String path = plugin.getSkillManager().getRelativePath(dir);
        // แสดง Path ใน Title (ตัดให้สั้นถ้ายาวเกิน)
        String titlePath = path.length() > 32 ? "..." + path.substring(path.length() - 28) : path;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Lib: " + titlePath));

        List<File> files = plugin.getSkillManager().listContents(dir);

        for (File file : files) {
            if (file.isDirectory()) {
                inv.addItem(createGuiItem(Material.CHEST, "§6📂 " + file.getName(),
                        "§7Type: Folder", "§eClick to open."));
            }
            else if (file.getName().endsWith(".yml")) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                Set<String> keys = config.getKeys(false);
                int count = keys.size();

                if (count > 1) {
                    // [Multi-Skill] แสดงเป็นหีบพิเศษ (Folder ปลอม)
                    inv.addItem(createGuiItem(Material.ENDER_CHEST, "§d📦 " + file.getName(),
                            "§7Type: Skill Pack",
                            "§7Contains: §f" + count + " skills",
                            "§eClick to open pack."));
                } else if (count == 1) {
                    // [Single Skill] แสดงเป็นไอคอนสกิล
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

        if (!path.equals("/")) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§c§l< BACK", "§7Go to parent folder"));
        }

        // ปุ่มสร้างต่างๆ
        inv.setItem(48, createGuiItem(Material.CHEST, "§6+ New Folder", "§7Create a sub-folder"));
        inv.setItem(49, createGuiItem(Material.PAPER, "§e+ New Skill", "§7Create a single skill file"));
        inv.setItem(50, createGuiItem(Material.ENDER_CHEST, "§d+ New Pack", "§7Create a multi-skill pack"));

        player.openInventory(inv);
    }

    // --- View 2: ภายในไฟล์ .yml (Skill Pack) ---
    private void openPackView(Player player, File file) {
        String path = plugin.getSkillManager().getRelativePath(file);
        String titlePath = path.length() > 30 ? "..." + path.substring(path.length() - 26) : path;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Pack: " + titlePath));

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String skillId : config.getKeys(false)) {
            SkillData skill = plugin.getSkillManager().getSkill(skillId);
            if (skill != null) {
                inv.addItem(createSkillItem(skill, "ID: " + skillId));
            } else {
                inv.addItem(createGuiItem(Material.BARRIER, "§c" + skillId, "§7Error loading skill."));
            }
        }

        inv.setItem(45, createGuiItem(Material.ARROW, "§c§l< BACK", "§7Return to folder"));
        inv.setItem(53, createGuiItem(Material.LIME_DYE, "§a+ Add Skill", "§7Add another skill to this pack"));

        player.openInventory(inv);
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
            meta.setLore(Arrays.asList(lore)); // ใช้ Arrays.asList ตรงนี้ได้แล้ว
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }
}