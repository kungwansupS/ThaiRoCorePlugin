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
 * [FIXED] เพิ่มการฝัง Context (Item ID, Index) ลงในปุ่ม Navigation ทุกปุ่มป้องกัน Context Lost
 */
public class ItemSkillSelectGUI {

    private final ThaiRoCorePlugin plugin;
    private final File currentDir;
    private final int page;
    private final String itemTemplateId; // ID ของ Item Template ที่กำลังแก้ไข
    private final int bindingIndex;     // Index ของ Skill Binding ที่กำลังจะสร้าง

    public ItemSkillSelectGUI(ThaiRoCorePlugin plugin, File currentDir, int page, String itemTemplateId, int bindingIndex) {
        this.plugin = plugin;
        this.currentDir = currentDir;
        this.page = page;
        this.itemTemplateId = itemTemplateId;
        this.bindingIndex = bindingIndex;
    }

    public void open(Player player) {
        String title = "ItemSkillSelect:";
        String rootPath = plugin.getSkillManager().getRootDir().getAbsolutePath();
        String path = currentDir.getAbsolutePath().substring(rootPath.length()).replace("\\", "/");
        if (path.isEmpty()) path = "/";
        else if (path.startsWith("/")) path = path.substring(1);

        invOpen(player, title + " " + path, page);
    }

    private void invOpen(Player player, String titlePrefix, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(titlePrefix + " #P" + page));
        SkillManager manager = plugin.getSkillManager();
        List<File> contents = manager.listContents(currentDir);

        List<ItemStack> displayItems = new ArrayList<>();

        // 1. วนลูปเพื่อเก็บ Folder และ Skill ทั้งหมด
        for (File file : contents) {
            if (file.isDirectory()) {
                // [FIX] สร้าง Folder และฝัง Context
                ItemStack folder = createGuiItem(Material.CHEST, "§aFolder: " + file.getName());
                folder = addHiddenContext(folder, null);
                displayItems.add(folder);
            } else if (file.getName().endsWith(".yml")) {
                try {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    for (String key : config.getKeys(false)) {
                        SkillData skill = manager.getSkill(key);
                        if (skill != null) displayItems.add(createSkillItem(skill));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error reading skill file: " + file.getName());
                }
            }
        }

        // 2. Pagination
        int itemStart = page * 45;
        int itemMax = Math.min(displayItems.size(), itemStart + 45);

        for (int i = itemStart; i < itemMax; i++) {
            inv.setItem(i - itemStart, displayItems.get(i));
        }

        // 3. Footer
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, bg);

        // ปุ่มย้อนกลับ (ขึ้น Folder) - Slot 45
        if (!currentDir.equals(manager.getRootDir())) {
            ItemStack backBtn = createGuiItem(Material.ARROW, "§cBack", "§7Up one level");
            inv.setItem(45, addHiddenContext(backBtn, null)); // [FIX] ใส่ Context
        }

        // ปุ่มเปลี่ยนหน้า - Slot 48 & 50
        if (page > 0) {
            ItemStack prevBtn = createGuiItem(Material.ARROW, "§ePrevious Page");
            inv.setItem(48, addHiddenContext(prevBtn, null)); // [FIX] ใส่ Context
        }
        if (displayItems.size() > itemMax) { // Check if there are more items for next page
            ItemStack nextBtn = createGuiItem(Material.ARROW, "§eNext Page");
            inv.setItem(50, addHiddenContext(nextBtn, null)); // [FIX] ใส่ Context
        }

        // ปุ่มยกเลิก/กลับไปหน้า Skill Binding - Slot 53
        ItemStack backToBindingBtn = createGuiItem(Material.RED_BED, "§cBack to Skill Binding",
                "§7Item: " + (itemTemplateId != null ? itemTemplateId : "Unknown"),
                "§7Slot: " + (bindingIndex != -1 ? String.valueOf(bindingIndex + 1) : "Unknown")
        );
        inv.setItem(53, addHiddenContext(backToBindingBtn, null));

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

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        // [FIX] ใช้ Helper เพื่อฝัง Context + Skill ID
        return addHiddenContext(item, skill.getId());
    }

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

    /**
     * [FIX] Helper method หลักสำหรับฝัง Context ข้อมูลลงใน Lore
     * เพื่อให้ GUIListener สามารถดึงไปใช้ตอนเปลี่ยนหน้าหรือเลือก Folder ได้
     */
    private ItemStack addHiddenContext(ItemStack item, String skillId) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        // ฝังข้อมูล Item Template ID
        lore.add("§0ITEM_ID:" + (itemTemplateId != null ? itemTemplateId : "N/A"));
        // ฝังข้อมูล Binding Index
        lore.add("§0INDEX:" + bindingIndex);

        // ฝังข้อมูล Skill ID (ถ้ามี)
        if (skillId != null) {
            lore.add("§0SKILL_ID:" + skillId);
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}