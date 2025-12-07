package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
// Import the new main plugin class
import org.rostats.ThaiRoCorePlugin;

import java.util.ArrayList;
import java.util.List;

public class AttributeEditorGUI {

    // Change type from ItemEditorPlugin to ThaiRoCorePlugin
    private final ThaiRoCorePlugin plugin;
    private final ItemAttributeManager attributeManager;
    private static final int INVENTORY_SIZE = 54;
    public static final String GUI_TITLE = "§0§lItem Attribute Editor";

    public AttributeEditorGUI(ThaiRoCorePlugin plugin, ItemAttributeManager attributeManager) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
    }

    public void open(Player player, ItemStack item) {
        Inventory inv = Bukkit.createInventory(player, INVENTORY_SIZE, Component.text(GUI_TITLE));

        // Store the item being edited in the GUI (Slot 0)
        inv.setItem(0, item.clone());

        // Filler/Background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (i > 8 && i != 0) inv.setItem(i, filler); // Fill slots 9-53
        }

        // Control Row (R0) - NEW DESIGN
        // [R0, C8] EXIT BUTTON
        inv.setItem(8, createItem(Material.BARRIER, "§cClose", "§7Close the editor and apply changes."));
        // [R0, C7] NEW: SAVE & COPY (Req 4)
        inv.setItem(7, createItem(Material.ENDER_CHEST, "§d§lSave & Copy",
                "§7คลิกเพื่อบันทึก Attribute ทั้งหมดของไอเทมนี้",
                "§7เป็น Template และคัดลอกไอเทมที่เสร็จสมบูรณ์",
                "§eคลิกซ้าย: §7Save & Get Copy"
        ));
        // [R0, C6] NEW: REMOVE VANILLA (Req 1 & 2)
        inv.setItem(6, createItem(Material.REDSTONE_BLOCK, "§c§lRemove Vanilla Attributes",
                "§7(Vanilla Damage/Armor)",
                "§cคลิกเพื่อลบ Attribute Modifiers ของ Minecraft ทั้งหมด"
        ));
        // [R0, C5] NEW: LOAD TEMPLATE (Req 4)
        inv.setItem(5, createItem(Material.BOOK, "§b§lLoad Template",
                "§7คลิกเพื่อโหลด Attribute จาก Template ที่บันทึกไว้",
                "§e(ฟีเจอร์นี้ต้องใช้งานร่วมกับระบบ Template Manager)"
        ));

        inv.setItem(1, createItem(Material.PAPER, "§bCustom Lore", "§7Custom Lore lines are preserved when editing."));


        // Placeholders for ItemAttribute rows (Slots 9+)
        int slot = 9;
        for (ItemAttribute attribute : ItemAttribute.values()) {
            if (slot >= INVENTORY_SIZE) break;
            inv.setItem(slot, createAttributeIcon(item, attribute));
            slot++;
        }

        player.openInventory(inv);
    }

    /**
     * Creates an icon for an attribute, showing its current value and modification steps.
     */
    public ItemStack createAttributeIcon(ItemStack item, ItemAttribute attribute) {
        double currentValue = attributeManager.getAttribute(item, attribute);

        List<String> lore = new ArrayList<>();
        lore.add("§7Key: §f" + attribute.getKey());
        lore.add("§7Current Value: " + (currentValue != 0 ? "§a" : "§7") + String.format(attribute.getFormat(), currentValue));
        lore.add(" ");
        lore.add("§eLeft-Click: §7+" + String.format(attribute.getFormat(), attribute.getClickStep()));
        lore.add("§eRight-Click: §7-" + String.format(attribute.getFormat(), attribute.getClickStep()));
        lore.add("§eShift+Left-Click: §7+" + String.format(attribute.getFormat(), attribute.getRightClickStep()));
        lore.add("§eShift+Right-Click: §7-" + String.format(attribute.getFormat(), attribute.getRightClickStep()));
        lore.add("§eMiddle-Click: §7Reset to 0");

        return createItem(attribute.getMaterial(), attribute.getDisplayName(), lore.toArray(new String[0]));
    }

    /**
     * Helper to create a basic item stack.
     */
    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // Hide vanilla lore/attributes
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) loreList.add(Component.text(line));
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Maps an inventory slot to its corresponding ItemAttribute enum.
     */
    public ItemAttribute getAttributeBySlot(int slot) {
        if (slot < 9 || slot >= INVENTORY_SIZE) return null;
        int index = slot - 9;
        ItemAttribute[] values = ItemAttribute.values();
        if (index < values.length) {
            return values[index];
        }
        return null;
    }
}