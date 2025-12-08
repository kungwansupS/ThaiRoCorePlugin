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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillLibraryGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentDir;

    public SkillLibraryGUI(ThaiRoCorePlugin plugin, File currentDir) {
        this.plugin = plugin;
        this.currentDir = currentDir;
    }

    public void open(Player player) {
        open(player, false);
    }

    public void openSelectMode(Player player) {
        open(player, true);
    }

    private void open(Player player, boolean selectMode) {
        String pathDisplay = plugin.getSkillManager().getRelativePath(currentDir);
        // ถ้าเป็น selectMode ให้ใช้ Title ต่างออกไป เพื่อให้ GUIListener ดักจับได้
        String title = (selectMode ? "SkillSelect: " : "SkillLib: ") + pathDisplay;
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title));

        // 1. Navigation
        if (!currentDir.equals(plugin.getSkillManager().getRootDir())) {
            inv.setItem(0, createGuiItem(Material.ARROW, "§eBack / ย้อนกลับ",
                    "§7Go back to previous folder.",
                    "§8---------------",
                    "§7ย้อนกลับไปโฟลเดอร์ก่อนหน้า"
            ));
        } else {
            inv.setItem(0, createGuiItem(Material.BOOKSHELF, "§eRoot / หน้าแรก",
                    "§7You are at root folder.",
                    "§8---------------",
                    "§7คุณอยู่ที่หน้าหลักของคลังสกิล"
            ));
        }

        if (!selectMode) {
            inv.setItem(4, createGuiItem(Material.CHEST, "§aNew Folder / สร้างโฟลเดอร์",
                    "§7Create a new folder.",
                    "§8---------------",
                    "§7สร้างโฟลเดอร์ใหม่เพื่อจัดหมวดหมู่"
            ));

            inv.setItem(8, createGuiItem(Material.WRITABLE_BOOK, "§aNew Skill / สร้างสกิล",
                    "§7Create a new skill file.",
                    "§8---------------",
                    "§7สร้างไฟล์สกิลใหม่ในโฟลเดอร์นี้"
            ));
        } else {
            inv.setItem(4, createGuiItem(Material.COMPASS, "§bSelect Mode",
                    "§7Click a skill to select it.",
                    "§7คลิกที่สกิลเพื่อเลือก"
            ));
        }

        // 2. Content Grid
        List<File> files = plugin.getSkillManager().listContents(currentDir);
        int slot = 9;

        for (File file : files) {
            if (slot >= 54) break;

            if (file.isDirectory()) {
                inv.setItem(slot, createGuiItem(Material.CHEST, "§6§l" + file.getName(),
                        "§eLEFT CLICK §7to Open",
                        selectMode ? "" : "§bSHIFT+RIGHT §7to Rename",
                        selectMode ? "" : "§cSHIFT+LEFT §7to Delete",
                        "§8---------------",
                        "§eคลิกซ้าย §7เพื่อเปิดโฟลเดอร์"
                ));
            } else {
                // Load Skill Data for preview
                String skillId = file.getName().replace(".yml", "");
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

                // Try to find the root key which is usually the skillId
                String displayName = config.getString(skillId + ".display-name", skillId);
                String iconName = config.getString(skillId + ".icon", "BOOK");
                Material iconMat = Material.getMaterial(iconName);
                if (iconMat == null) iconMat = Material.BOOK;

                double cooldown = config.getDouble(skillId + ".conditions.cooldown", 0);
                int spCost = config.getInt(skillId + ".conditions.sp-cost", 0);
                int reqLevel = config.getInt(skillId + ".conditions.required-level", 1);
                String trigger = config.getString(skillId + ".trigger", "CAST");

                List<String> lore = new ArrayList<>();
                lore.add("§8ID: " + skillId);
                lore.add("§7Type: §f" + trigger);
                lore.add("§7Cooldown: §f" + cooldown + "s");
                lore.add("§7SP Cost: §f" + spCost);
                lore.add("§7Req Lv: §f" + reqLevel);
                lore.add("§8---------------");

                if (selectMode) {
                    lore.add("§a§lCLICK TO SELECT / คลิกเพื่อเลือก");
                } else {
                    lore.add("§eLEFT CLICK §7to Edit");
                    lore.add("§cSHIFT+LEFT §7to Delete");
                }

                inv.setItem(slot, createGuiItem(iconMat, "§f" + displayName, lore));
            }
            slot++;
        }

        // Fill bg
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    // ... (confirmDelete method same as before) ...
    public void openConfirmDelete(Player player, File fileToDelete) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Skill Delete: " + fileToDelete.getName()));
        ItemStack yes = createGuiItem(Material.LIME_CONCRETE, "§a§lCONFIRM DELETE", "§7File: " + fileToDelete.getName());
        ItemStack no = createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL");
        inv.setItem(11, yes);
        inv.setItem(15, no);
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i=0; i<27; i++) { if (inv.getItem(i) == null) inv.setItem(i, bg); }
        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        return createGuiItem(mat, name, Arrays.asList(lore));
    }

    private ItemStack createGuiItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        // [FIX] ใช้ HIDE_ADDITIONAL_TOOLTIP แทน HIDE_POTION_EFFECTS สำหรับ 1.21
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }
}