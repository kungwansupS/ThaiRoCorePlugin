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
import java.util.ArrayList; // [FIX] เพิ่มบรรทัดนี้ เพื่อแก้ Error
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SkillLibraryGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentFolder;

    // Constructor รับค่าเดียว (สำหรับเรียกจาก GUIListener)
    public SkillLibraryGUI(ThaiRoCorePlugin plugin) {
        this(plugin, null);
    }

    // Constructor รับ 2 ค่า (สำหรับเปิด Folder)
    public SkillLibraryGUI(ThaiRoCorePlugin plugin, File folder) {
        this.plugin = plugin;
        this.currentFolder = folder;
    }

    public void open(Player player) {
        if (currentFolder == null) {
            openRoot(player);
        } else {
            openFolder(player, currentFolder);
        }
    }

    public void openSelectMode(Player player, Consumer<String> onSelect) {
        GUIListener.setSelectionCallback(player, onSelect);
        player.sendMessage("§ePlease select a skill from the library...");
        open(player);
    }

    public void openConfirmDelete(Player player, File target) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Skill Delete: " + target.getName()));

        inv.setItem(3, createGuiItem(Material.LIME_CONCRETE, "§a§lCONFIRM DELETE",
                "§7File: " + target.getName(),
                "§c§lWARNING: §7Cannot be undone!"));

        inv.setItem(5, createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL", "§7Return to library."));

        player.openInventory(inv);
    }

    private void openRoot(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Skill Library"));

        List<File> files = plugin.getSkillManager().listContents(plugin.getSkillManager().getRootDir());

        for (File file : files) {
            if (file.isDirectory()) continue;
            if (!file.getName().endsWith(".yml")) continue;

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            Set<String> keys = config.getKeys(false);
            int skillCount = keys.size();

            if (skillCount > 1) {
                ItemStack item = createGuiItem(Material.CHEST, "§6📂 " + file.getName(),
                        "§7Contains " + skillCount + " skills.",
                        "§eClick to open folder.");
                inv.addItem(item);
            } else if (skillCount == 1) {
                if (!keys.isEmpty()) {
                    String skillId = keys.iterator().next();
                    SkillData skill = plugin.getSkillManager().getSkill(skillId);
                    if (skill != null) {
                        inv.addItem(createSkillItem(skill, file.getName()));
                    } else {
                        inv.addItem(createGuiItem(Material.PAPER, "§c" + skillId, "§7Error loading skill data"));
                    }
                }
            } else {
                inv.addItem(createGuiItem(Material.PAPER, "§7" + file.getName(), "§7(Empty File)"));
            }
        }

        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§l+ CREATE NEW FILE", "§7Create a new skill file."));
        player.openInventory(inv);
    }

    private void openFolder(Player player, File file) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Folder: " + file.getName()));

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String skillId : config.getKeys(false)) {
            SkillData skill = plugin.getSkillManager().getSkill(skillId);
            if (skill != null) {
                inv.addItem(createSkillItem(skill, "ID: " + skillId));
            } else {
                inv.addItem(createGuiItem(Material.BARRIER, "§c" + skillId, "§7Error loading skill."));
            }
        }

        inv.setItem(45, createGuiItem(Material.ARROW, "§c§l< BACK", "§7Return to file list."));
        inv.setItem(53, createGuiItem(Material.LIME_DYE, "§a+ Add Skill Here", "§7Add another skill to this file."));

        player.openInventory(inv);
    }

    private ItemStack createSkillItem(SkillData skill, String subInfo) {
        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e" + skill.getDisplayName());
            List<String> lore = new ArrayList<>(); // [FIX] ใช้ ArrayList ตรงนี้
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