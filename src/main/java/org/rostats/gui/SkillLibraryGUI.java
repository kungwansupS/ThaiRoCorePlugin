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

    // [FIX] เพิ่มเมธอด openConfirmDelete กลับเข้ามาเพื่อให้ GUIListener เรียกใช้ได้
    public void openConfirmDelete(Player player, File target) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Delete: " + target.getName()));

        inv.setItem(3, createGuiItem(Material.LIME_CONCRETE, "§a§lCONFIRM DELETE", "§7Target: " + target.getName()));
        inv.setItem(5, createGuiItem(Material.RED_CONCRETE, "§c§lCANCEL", "§7Return to library"));

        player.openInventory(inv);
    }

    private void invOpen(Player player, String titlePrefix, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(titlePrefix + " #P" + page));
        SkillManager manager = plugin.getSkillManager();
        List<File> contents = manager.listContents(currentDir);

        int start = page * 45;
        // Logic เพื่อรวมสกิลจากไฟล์ใน Select Mode หรือแสดงไฟล์ในโหมดปกติ
        // เพื่อความง่ายและประสิทธิภาพใน FULLCODE นี้ จะแสดงเป็น File/Folder structure ก่อน
        // แต่ถ้าเป็น Select Mode และเจอไฟล์ .yml จะพยายามแตก Skill ข้างในออกมาโชว์ (Optional enhancement)

        List<ItemStack> displayItems = new ArrayList<>();

        for (File file : contents) {
            if (file.isDirectory()) {
                displayItems.add(createGuiItem(Material.CHEST, "§aFolder: " + file.getName()));
            } else if (file.getName().endsWith(".yml")) {
                if (isSelectMode) {
                    // ในโหมดเลือก: แตกสกิลในไฟล์ออกมาแสดงเป็นไอเทม
                    try {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        for (String key : config.getKeys(false)) {
                            SkillData skill = manager.getSkill(key);
                            if (skill != null) {
                                displayItems.add(createSkillItem(skill));
                            }
                        }
                    } catch (Exception e) {
                        // ถ้าอ่านไฟล์ไม่ได้ ให้แสดงเป็นไฟล์ Error หรือข้ามไป
                    }
                } else {
                    // โหมดปกติ: แสดงเป็น Skill Pack
                    displayItems.add(createGuiItem(Material.PAPER, "§bSkill Pack: §f" + file.getName()));
                }
            }
        }

        // Pagination
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

        // Context Info / Controls
        if (isSelectMode) {
            ItemStack info = createGuiItem(Material.BOOK, "§eSelecting Skill", "§7Target Item: " + targetItemId);
            // ซ่อนข้อมูล Context ไว้ใน Lore เพื่อให้ GUIListener อ่านกลับไปได้
            ItemMeta meta = info.getItemMeta();
            List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
            lore.add("§0INDEX:" + bindingIndex);
            meta.setLore(lore);
            info.setItemMeta(meta);

            // ปุ่ม Back กลับไปหน้า Binding
            ItemStack backBtn = createGuiItem(Material.RED_BED, "§cBack to Skill Binding", "§7(Skill: " + targetItemId + ")");
            ItemMeta backMeta = backBtn.getItemMeta();
            backMeta.setLore(lore); // ส่ง Context กลับไปด้วย
            backBtn.setItemMeta(backMeta);

            inv.setItem(53, backBtn);
        } else {
            // โหมด Admin ปกติ: ปุ่มสร้างไฟล์/โฟลเดอร์
            inv.setItem(49, createGuiItem(Material.PAPER, "§eNew Skill", "§7Create new skill file"));
            inv.setItem(50, createGuiItem(Material.CHEST, "§6New Folder", "§7Create new folder"));
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

        // [IMPORTANT] Hidden ID for GUIListener to identify skill correctly
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