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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillBindingGUI {

    private final ThaiRoCorePlugin plugin;
    private final File itemFile;

    public SkillBindingGUI(ThaiRoCorePlugin plugin, File itemFile) {
        this.plugin = plugin;
        this.itemFile = itemFile;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Editor: " + itemFile.getName() + " [SKILLS]"));

        ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
        List<ItemSkillBinding> bindings = attr.getSkillBindings();

        int slot = 0;
        for (ItemSkillBinding binding : bindings) {
            if (slot >= 45) break;

            List<String> lore = new ArrayList<>();
            lore.add("§7Skill ID: §f" + binding.getSkillId());
            lore.add("§7Trigger: §e" + binding.getTrigger().name());
            lore.add("§7Level: §a" + binding.getLevel());
            lore.add("§7Chance: §d" + (binding.getChance() * 100) + "%");
            lore.add("§8---------------");
            lore.add("§cRight Click to Remove / คลิกขวาเพื่อลบ");

            inv.setItem(slot++, createGuiItem(Material.ENCHANTED_BOOK, "§6" + binding.getSkillId(), lore));
        }

        // Add Button
        inv.setItem(49, createGuiItem(Material.LIME_DYE, "§a§lAdd Skill / เพิ่มสกิล",
                "§7Bind a new skill to this item.",
                "§7Requires Skill ID, Trigger, Level, Chance."
        ));

        // Back Button
        inv.setItem(53, createGuiItem(Material.ARROW, "§eBack / กลับ", "§7Return to General Page"));

        // Fill empty
        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        player.openInventory(inv);
    }

    public void openAddSkillSelector(Player player) {
        // Simple implementation: Use Chat Input for all 4 parameters sequentially
        // 1. Skill ID
        plugin.getChatInputHandler().awaitInput(player, "Enter Skill ID (must exist in /roskilleditor):", (skillId) -> {
            // 2. Trigger Type
            StringBuilder triggerList = new StringBuilder();
            for (TriggerType t : TriggerType.values()) triggerList.append(t.name()).append(", ");

            plugin.getChatInputHandler().awaitInput(player, "Enter Trigger Type (" + triggerList + "):", (triggerStr) -> {
                try {
                    TriggerType trigger = TriggerType.valueOf(triggerStr.toUpperCase());

                    // 3. Level
                    plugin.getChatInputHandler().awaitInput(player, "Enter Skill Level:", (lvlStr) -> {
                        try {
                            int level = Integer.parseInt(lvlStr);

                            // 4. Chance
                            plugin.getChatInputHandler().awaitInput(player, "Enter Chance (0.0 - 1.0):", (chanceStr) -> {
                                try {
                                    double chance = Double.parseDouble(chanceStr);

                                    // Save
                                    ItemAttribute attr = plugin.getItemManager().loadAttribute(itemFile);
                                    ItemStack stack = plugin.getItemManager().loadItemStack(itemFile);
                                    attr.getSkillBindings().add(new ItemSkillBinding(skillId, trigger, level, chance));
                                    plugin.getItemManager().saveItem(itemFile, attr, stack);

                                    player.sendMessage("§aSkill bound successfully!");

                                    // Re-open GUI (Sync)
                                    plugin.getServer().getScheduler().runTask(plugin, () -> open(player));

                                } catch (Exception e) { player.sendMessage("§cInvalid Chance"); }
                            });
                        } catch (Exception e) { player.sendMessage("§cInvalid Level"); }
                    });
                } catch (Exception e) { player.sendMessage("§cInvalid Trigger Type"); }
            });
        });
    }

    private ItemStack createGuiItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material mat, String name, String... lore) {
        return createGuiItem(mat, name, Arrays.asList(lore));
    }
}