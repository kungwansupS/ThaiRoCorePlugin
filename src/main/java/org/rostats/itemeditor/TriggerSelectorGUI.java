package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.trigger.TriggerType;

import java.util.Arrays;

public class TriggerSelectorGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;

    public TriggerSelectorGUI(ThaiRoCorePlugin plugin, String skillId) {
        this.plugin = plugin;
        this.skillId = skillId;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, Component.text("Select Trigger: " + skillId));

        // Active Triggers
        inv.setItem(10, createItem(Material.IRON_SWORD, TriggerType.LEFT_CLICK, "§cLeft Click", "§7โจมตีปกติ / คลิกซ้าย"));
        inv.setItem(11, createItem(Material.SHIELD, TriggerType.RIGHT_CLICK, "§eRight Click", "§7คลิกขวา (ถือของ/กดใช้)"));
        inv.setItem(19, createItem(Material.GOLDEN_SWORD, TriggerType.SHIFT_LEFT_CLICK, "§cShift + Left Click", "§7ย่อแล้วคลิกซ้าย"));
        // [FIX] เปลี่ยนจาก DIAMOND_SHIELD เป็น DIAMOND (หรือไอเทมอื่นที่ต้องการ)
        inv.setItem(20, createItem(Material.DIAMOND, TriggerType.SHIFT_RIGHT_CLICK, "§eShift + Right Click", "§7ย่อแล้วคลิกขวา"));

        // Reactive Triggers
        inv.setItem(13, createItem(Material.REDSTONE, TriggerType.ON_HIT, "§4On Hit", "§7เมื่อโจมตีโดนเป้าหมาย"));
        inv.setItem(14, createItem(Material.CHAINMAIL_CHESTPLATE, TriggerType.ON_DEFEND, "§9On Defend", "§7เมื่อถูกโจมตี"));
        inv.setItem(22, createItem(Material.SKELETON_SKULL, TriggerType.ON_KILL, "§8On Kill", "§7เมื่อฆ่าศัตรูได้"));

        // Passive Triggers
        inv.setItem(16, createItem(Material.CLOCK, TriggerType.PASSIVE_TICK, "§aPassive Tick", "§7ทำงานวนลูปทุกวินาที (เมื่อถือ/ใส่)"));
        inv.setItem(25, createItem(Material.POTION, TriggerType.PASSIVE_APPLY, "§bPassive Apply", "§7ทำงานครั้งเดียวเมื่อถือ/ใส่ (บัฟ)"));

        // Back
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName("§cCancel");
        back.setItemMeta(meta);
        inv.setItem(44, back);

        // BG
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        bgMeta.setDisplayName(" ");
        bg.setItemMeta(bgMeta);
        for(int i=0; i<45; i++) {
            if(inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    private ItemStack createItem(Material mat, TriggerType type, String title, String desc) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(Arrays.asList(desc, "§8---------------", "§7Type: §f" + type.name()));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }
}