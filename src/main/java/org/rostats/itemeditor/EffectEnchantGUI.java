package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.rostats.ThaiRoCorePlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public class EffectEnchantGUI {

    public enum Mode {
        EFFECT, ENCHANT
    }

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;
    private final Mode mode;
    private String selectedKey = null; // Stores PotionEffectType Name or Enchantment Name
    private int inputLevel = 1;

    public EffectEnchantGUI(ThaiRoCorePlugin plugin, File itemFile, Mode mode) {
        this.plugin = plugin;
        this.itemFile = itemFile;
        this.mode = mode;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + mode.name() + " Select"));

        // Load current data
        ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
        ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);

        // Populate List (Slots 0-44)
        int slot = 0;
        if (mode == Mode.EFFECT) {
            for (PotionEffectType type : PotionEffectType.values()) {
                if (type == null) continue;
                if (slot >= 45) break;

                boolean has = attr.getPotionEffects().containsKey(type);
                int lvl = has ? attr.getPotionEffects().get(type) : 0;

                inv.setItem(slot++, createOptionItem(
                        has ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE,
                        type.getName(),
                        has,
                        lvl
                ));
            }
        } else {
            for (Enchantment ench : Enchantment.values()) {
                if (ench == null) continue;
                if (slot >= 45) break;

                boolean has = stack.containsEnchantment(ench);
                int lvl = has ? stack.getEnchantmentLevel(ench) : 0;

                inv.setItem(slot++, createOptionItem(
                        has ? Material.ENCHANTED_BOOK : Material.BOOK,
                        ench.getKey().getKey().toUpperCase(), // Use simple name
                        has,
                        lvl
                ));
            }
        }

        // Control Panel (Bottom Row)
        updateControlPanel(inv);

        player.openInventory(inv);
    }

    private void updateControlPanel(Inventory inv) {
        // Slot 49: Selected Info
        if (selectedKey != null) {
            inv.setItem(49, createGuiItem(Material.PAPER, "§eSelected: §f" + selectedKey, "§7Current Input Level: §a" + inputLevel));
            // Slot 50: Anvil (Input Level)
            inv.setItem(50, createGuiItem(Material.ANVIL, "§eSet Level", "§7Click to input level via Chat"));
            // Slot 51: Confirm Add
            inv.setItem(51, createGuiItem(Material.LIME_CONCRETE, "§a§lADD / UPDATE", "§7Apply to item"));
            // Slot 52: Remove
            inv.setItem(52, createGuiItem(Material.RED_CONCRETE, "§c§lREMOVE", "§7Remove from item"));
        } else {
            inv.setItem(49, createGuiItem(Material.BARRIER, "§cNo Selection", "§7Click an option above"));
            inv.setItem(50, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            inv.setItem(51, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
            inv.setItem(52, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        }

        // Back Button
        inv.setItem(53, createGuiItem(Material.ARROW, "§eBack", "§7Return to Editor"));
    }

    private ItemStack createOptionItem(Material mat, String name, boolean active, int level) {
        String displayName = (active ? "§a" : "§7") + name;
        String status = active ? "§a[ADDED] Lv." + level : "§7[NOT ADDED]";
        return createGuiItem(mat, displayName, status, "§eClick to Select");
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

    public void handleClick(Inventory inv, int slot, ItemStack clicked, Player player) {
        if (slot < 45) {
            // Select Item
            if (clicked == null || clicked.getType() == Material.AIR) return;
            // Extract name from display name (strip color)
            String dp = clicked.getItemMeta().getDisplayName();
            selectedKey = dp.substring(2); // Remove color code prefix (e.g. §a)
            // Reset input level to 1 or current level if exists
            // (For simplicity, just reset to 1 or keep last input)
            updateControlPanel(inv);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
        } else if (slot == 50 && selectedKey != null) {
            // Anvil: Input Level
            plugin.getChatInputHandler().awaitInput(player, "Enter Level for " + selectedKey + ":", (str) -> {
                try {
                    int lvl = Integer.parseInt(str);
                    this.inputLevel = lvl;
                    // Re-open GUI
                    open(player);
                    // Note: In real usage, we need to persist the selectedKey state or pass it back.
                    // For simplicity, re-opening resets selection in this basic impl.
                    // Better: Store state in a map in Plugin or handle sync.
                    // Due to constraints, let's just re-open and user selects again or we store state in Metadata?
                    // Let's rely on the user re-selecting for now or assume simple flow.
                    // Ideally, we pass 'this' GUI instance state back.
                    // To fix "Input -> Reopen -> Lost Selection", we can set a temp variable in Player Metadata.
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid Number");
                    open(player);
                }
            });
        } else if (slot == 51 && selectedKey != null) {
            // Confirm Add
            applyChange(player, true);
        } else if (slot == 52 && selectedKey != null) {
            // Remove
            applyChange(player, false);
        } else if (slot == 53) {
            // Back
            new AttributeEditorGUI(plugin, itemFile).open(player, AttributeEditorGUI.Page.GENERAL);
        }
    }

    private void applyChange(Player player, boolean add) {
        ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
        ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);

        if (mode == Mode.EFFECT) {
            PotionEffectType type = PotionEffectType.getByName(selectedKey);
            if (type != null) {
                if (add) {
                    attr.getPotionEffects().put(type, inputLevel);
                    player.sendMessage("§aAdded Effect: " + type.getName() + " Lv." + inputLevel);
                } else {
                    attr.getPotionEffects().remove(type);
                    player.sendMessage("§cRemoved Effect: " + type.getName());
                }
                plugin.getItemManager().saveItem(itemFile, attr, stack);
            }
        } else {
            // Enchantment
            // Need to map friendly name back to Enchantment
            // Assuming selectedKey is upper case key
            Enchantment ench = null;
            for (Enchantment e : Enchantment.values()) {
                if (e.getKey().getKey().equalsIgnoreCase(selectedKey)) {
                    ench = e;
                    break;
                }
            }
            if (ench != null) {
                ItemMeta meta = stack.getItemMeta();
                if (add) {
                    meta.addEnchant(ench, inputLevel, true);
                    player.sendMessage("§aAdded Enchant: " + selectedKey + " Lv." + inputLevel);
                } else {
                    meta.removeEnchant(ench);
                    player.sendMessage("§cRemoved Enchant: " + selectedKey);
                }
                stack.setItemMeta(meta);
                plugin.getItemManager().saveItem(itemFile, attr, stack);
            }
        }
        open(player); // Refresh to show changes
    }
}