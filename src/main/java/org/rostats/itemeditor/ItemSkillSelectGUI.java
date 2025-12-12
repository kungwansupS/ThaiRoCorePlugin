package org.rostats.itemeditor;

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

/**
 * GUI สำหรับการเลือก Skill เพื่อผูกกับ Item Attribute ใน Item Editor
 * มีลักษณะคล้ายกับ SkillLibraryGUI ในโหมด isSelectMode แต่มีบริบทของ Item Editor
 */
public class ItemSkillSelectGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentDir;
    private final int page;
    private final String itemTemplateId; // ID ของ Item Template ที่กำลังแก้ไข (คือชื่อไฟล์ item.yml)
    private final int bindingIndex;     // Index ของ Skill Binding ที่กำลังจะสร้าง

    public ItemSkillSelectGUI(ThaiRoCorePlugin plugin, File currentDir, int page, String itemTemplateId, int bindingIndex) {
        this.plugin = plugin;
        this.currentDir = currentDir;
        this.page = page;
        this.itemTemplateId = itemTemplateId;
        this.bindingIndex = bindingIndex;
    }

    public void open(Player player) {
        // Title format: "ItemSkillSelect: [Relative Path] #P[Page]"
        String title = "ItemSkillSelect:";
        String rootPath = plugin.getSkillManager().getRootDir().getAbsolutePath();
        String path = currentDir.getAbsolutePath().substring(rootPath.length()).replace("\\", "/");
        if (path.isEmpty()) path = "/";
        else if (path.startsWith("/")) path = path.substring(1);

        invOpen(player, title + " " + path, page);
    }

    private void invOpen(Player player, String titlePrefix, int page) {
        // สร้าง Inventory ขนาด 54 ช่อง
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(titlePrefix + " #P" + page));
        SkillManager manager = plugin.getSkillManager();
        List<File> contents = manager.listContents(currentDir);

        List<ItemStack> displayItems = new ArrayList<>();

        // 1. วนลูปเพื่อเก็บ Folder และ Skill ทั้งหมด
        for (File file : contents) {
            if (file.isDirectory()) {
                // แสดง Folder
                displayItems.add(createGuiItem(Material.CHEST, "§aFolder: " + file.getName()));
            } else if (file.getName().endsWith(".yml")) {
                // โหลดไฟล์ .yml และแสดง Skills ทั้งหมดในไฟล์นั้น
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    for (String key : config.getKeys(false)) {
                        SkillData skill = manager.getSkill(key);
                        if (skill != null) displayItems.add(createSkillItem(skill));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error reading skill file for ItemSkillSelectGUI: " + file.getName());
                }
            }
        }

        // 2. Pagination - แสดงรายการในหน้าปัจจุบัน (45 ช่อง)
        int itemStart = page * 45;
        int itemMax = Math.min(displayItems.size(), itemStart + 45);

        for (int i = itemStart; i < itemMax; i++) {
            inv.setItem(i - itemStart, displayItems.get(i));
        }

        // 3. Footer (45-53)
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, bg);

        // ปุ่มย้อนกลับ (ขึ้น Folder) - Slot 45
        if (!currentDir.equals(manager.getRootDir())) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§cBack", "§7Up one level"));
        }

        // ปุ่มเปลี่ยนหน้า - Slot 48 & 50
        if (page > 0) inv.setItem(48, createGuiItem(Material.ARROW, "§ePrevious Page"));
        if (displayItems.size() > itemMax) inv.setItem(50, createGuiItem(Material.ARROW, "§eNext Page"));


        // ปุ่มยกเลิก/กลับไปหน้า Skill Binding - Slot 53
        ItemStack backToBindingBtn = createGuiItem(Material.RED_BED, "§cBack to Skill Binding",
                "§7Item: " + (itemTemplateId != null ? itemTemplateId : "Unknown"),
                "§7Slot: " + (bindingIndex != -1 ? String.valueOf(bindingIndex + 1) : "Unknown")
        );

        // ใส่ข้อมูล Context ที่ซ่อนไว้ใน Lore เพื่อให้ GUIListener ดึงไปใช้
        ItemMeta backMeta = backToBindingBtn.getItemMeta();
        List<String> backLore = backMeta.getLore();
        backLore.add("§0ITEM_ID:" + (itemTemplateId != null ? itemTemplateId : "N/A"));
        backLore.add("§0INDEX:" + bindingIndex); // ใช้ -1 เพื่อบอกว่าไม่ได้อยู่ใน flow การเพิ่มใหม่
        backMeta.setLore(backLore);
        backToBindingBtn.setItemMeta(backMeta);

        inv.setItem(53, backToBindingBtn);

        player.openInventory(inv);
    }

    /**
     * สร้าง ItemStack สำหรับการเลือก Skill โดยฝัง ID ของสกิล, ID ของไอเท็ม และ Index ของ Binding
     */
    private ItemStack createSkillItem(SkillData skill) {
        Material icon = skill.getIcon() != null ? skill.getIcon() : Material.BOOK;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();

        String displayName = skill.getDisplayName() != null ? skill.getDisplayName().replace("&", "§") : skill.getId();
        meta.setDisplayName("§e[Select] " + displayName);

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: " + skill.getId());

        // ข้อมูล Context ที่สำคัญสำหรับการเลือก Item
        lore.add("§0ITEM_ID:" + (itemTemplateId != null ? itemTemplateId : "N/A"));
        lore.add("§0INDEX:" + bindingIndex);
        lore.add("§0SKILL_ID:" + skill.getId()); // ID ของสกิลที่ถูกเลือก

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Helper method เพื่อสร้าง ItemStack ทั่วไป
     */
    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }
}