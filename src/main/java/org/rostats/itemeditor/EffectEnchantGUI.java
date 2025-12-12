package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EffectEnchantGUI {

    public enum Mode {
        EFFECT, ENCHANT
    }

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final Mode mode;
    private final int page;

    public EffectEnchantGUI(ThaiRoCorePlugin plugin, File itemFile, Mode mode) {
        this(plugin, itemFile, mode, 0);
    }

    public EffectEnchantGUI(ThaiRoCorePlugin plugin, File itemFile, Mode mode, int page) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        this.mode = mode;
        this.page = page;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + itemFile.getName() + " [" + mode.name() + " Select]"));

        // Load current data
        ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
        ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);

        // Selection State
        String metaKey = "RO_EDITOR_SEL_" + mode.name();
        String selectedKey = null;
        if (player.hasMetadata(metaKey)) {
            selectedKey = player.getMetadata(metaKey).get(0).asString();
        }

        // Prepare List
        List<String> items = new ArrayList<>();
        if (mode == Mode.EFFECT) {
            items = Arrays.stream(PotionEffectType.values())
                    .filter(type -> type != null)
                    .map(PotionEffectType::getName)
                    .sorted()
                    .collect(Collectors.toList());
        } else {
            items = Arrays.stream(Enchantment.values())
                    .filter(e -> e != null)
                    .map(e -> e.getKey().getKey().toUpperCase())
                    .sorted()
                    .collect(Collectors.toList());
        }

        // Pagination
        int itemsPerPage = 45;
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, items.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            String key = items.get(i);
            boolean has = false;
            int lvl = 0;

            if (mode == Mode.EFFECT) {
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type != null && attr.getPotionEffects().containsKey(type)) {
                    has = true;
                    lvl = attr.getPotionEffects().get(type);
                }
            } else {
                Enchantment ench = getEnchantment(key);
                if (ench != null && stack.containsEnchantment(ench)) {
                    has = true;
                    lvl = stack.getEnchantmentLevel(ench);
                }
            }

            boolean isSelected = key.equals(selectedKey);
            Material mat;
            if (mode == Mode.EFFECT) {
                mat = has ? Material.LIME_STAINED_GLASS_PANE : (isSelected ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
            } else {
                mat = has ? Material.ENCHANTED_BOOK : (isSelected ? Material.WRITABLE_BOOK : Material.BOOK);
            }

            inv.setItem(slot++, createOptionItem(mat, key, has, lvl, isSelected));
        }

        // Controls
        updateControlPanel(inv, selectedKey, items.size(), itemsPerPage);

        player.openInventory(inv);
    }

    private Enchantment getEnchantment(String key) {
        for (Enchantment e : Enchantment.values()) {
            if (e.getKey().getKey().equalsIgnoreCase(key)) return e;
        }
        return null;
    }

    private void updateControlPanel(Inventory inv, String selectedKey, int totalItems, int itemsPerPage) {
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "§ePrevious Page", "prev_page", String.valueOf(page - 1), "§7Go to page " + page));
        } else {
            inv.setItem(45, createGuiItem(Material.ARROW, "§eBack", "back", "editor", "§7Return to Editor"));
        }

        if (totalItems > (page + 1) * itemsPerPage) {
            inv.setItem(53, createGuiItem(Material.ARROW, "§eNext Page", "next_page", String.valueOf(page + 1), "§7Go to page " + (page + 2)));
        }

        if (selectedKey != null) {
            inv.setItem(48, createGuiItem(Material.PAPER, "§eSelected: §f" + selectedKey, "info", selectedKey, "§7Click options above to change"));
            inv.setItem(49, createGuiItem(Material.ANVIL, "§eSet Level", "set_level", selectedKey, "§7Click to input level via Chat"));
            inv.setItem(50, createGuiItem(Material.LIME_CONCRETE, "§a§lADD / UPDATE", "add", selectedKey, "§7Apply level to item"));
            inv.setItem(51, createGuiItem(Material.RED_CONCRETE, "§c§lREMOVE", "remove", selectedKey, "§7Remove from item"));
        } else {
            inv.setItem(49, createGuiItem(Material.BARRIER, "§cNo Selection", "info", "none", "§7Click an option above"));
        }

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", "bg", "");
        for (int i = 45; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }
    }

    private ItemStack createOptionItem(Material mat, String name, boolean active, int level, boolean selected) {
        String displayName = (active ? "§a" : "§7") + name;
        String status = active ? "§a[ADDED] Lv." + level : "§7[NOT ADDED]";
        String selectStatus = selected ? "§e▶ SELECTED ◀" : "§eClick to Select";
        return createGuiItem(mat, displayName, "select_item", name, status, selectStatus);
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
}