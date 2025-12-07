package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
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
        String pathDisplay = plugin.getSkillManager().getRelativePath(currentDir);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("SkillLib: " + pathDisplay));

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

        // 2. Content Grid
        List<File> files = plugin.getSkillManager().listContents(currentDir);
        int slot = 9;

        for (File file : files) {
            if (slot >= 54) break;

            if (file.isDirectory()) {
                inv.setItem(slot, createGuiItem(Material.CHEST, "§6§l" + file.getName(),
                        "§eLEFT CLICK §7to Open",
                        "§bSHIFT+RIGHT §7to Rename",
                        "§cSHIFT+LEFT §7to Delete",
                        "§8---------------",
                        "§eคลิกซ้าย §7เพื่อเปิดโฟลเดอร์",
                        "§bShift+ขวา §7เพื่อเปลี่ยนชื่อ",
                        "§cShift+ซ้าย §7เพื่อลบ"
                ));
            } else {
                inv.setItem(slot, createGuiItem(Material.BOOK, "§f" + file.getName().replace(".yml", ""),
                        "§eLEFT CLICK §7to Edit",
                        "§cSHIFT+LEFT §7to Delete",
                        "§8---------------",
                        "§eคลิกซ้าย §7เพื่อแก้ไขสกิล",
                        "§cShift+ซ้าย §7เพื่อลบ"
                ));
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

    public void openConfirmDelete(Player player, File fileToDelete) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Skill Delete: " + fileToDelete.getName()));

        ItemStack yes = createGuiItem(Material.LIME_CONCRETE, "§a§lCONFIRM DELETE / ยืนยัน",
                "§7File: " + fileToDelete.getName(),
                "§cCannot be undone!",
                "§8---------------",
                "§7ไฟล์: " + fileToDelete.getName(),
                "§cไม่สามารถกู้คืนได้!"
        );

        ItemStack no = createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL / ยกเลิก",
                "§7Return to library",
                "§8---------------",
                "§7กลับไปหน้าคลังสกิล"
        );

        inv.setItem(11, yes);
        inv.setItem(15, no);

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i=0; i<27; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}