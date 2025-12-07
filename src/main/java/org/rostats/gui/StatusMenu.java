package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ROStatsPlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;

import java.util.ArrayList;
import java.util.List;

public class StatusMenu {

    private final ROStatsPlugin plugin;

    public StatusMenu(ROStatsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("§0§lCharacter Status"));
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        ItemStack info = createItem(Material.PLAYER_HEAD, "§e§l" + player.getName(),
                "§7Base Lv: §a" + data.getBaseLevel(),
                // ลบ: "§7Job Lv: §a" + data.getJobLevel(),
                "§7Points Left: §6" + data.getStatPoints(),
                "§bSP: " + String.format("%.0f", data.getCurrentSP()) + "/" + String.format("%.0f", data.getMaxSP()));
        inv.setItem(4, info);

        inv.setItem(10, createStatIcon(data, "STR", Material.IRON_SWORD, "§c§lSTR", "Melee Atk, Weight"));
        inv.setItem(11, createStatIcon(data, "AGI", Material.FEATHER, "§b§lAGI", "Flee, ASPD"));
        inv.setItem(12, createStatIcon(data, "VIT", Material.IRON_CHESTPLATE, "§a§lVIT", "Max HP, Def"));
        inv.setItem(13, createStatIcon(data, "INT", Material.ENCHANTED_BOOK, "§d§lINT", "Matk, Max SP"));
        inv.setItem(14, createStatIcon(data, "DEX", Material.BOW, "§6§lDEX", "Hit, Cast Time"));
        inv.setItem(15, createStatIcon(data, "LUK", Material.RABBIT_FOOT, "§e§lLUK", "Critical, P.Dodge"));

        StatManager stats = plugin.getStatManager();
        inv.setItem(22, createItem(Material.PAPER, "§f§lCombat Stats",
                "§7P.ATK: §c" + String.format("%.0f", stats.getPhysicalAttack(player)),
                "§7M.ATK: §d" + String.format("%.0f", stats.getMagicAttack(player)),
                "§7HIT: §6" + stats.getHit(player) + " §7| FLEE: §b" + stats.getFlee(player),
                "§7CRIT: §e" + String.format("%.1f", stats.getCritChance(player)) + "%",
                "§7ASPD: §f" + String.format("%.0f%%", stats.getAspdBonus(player) * 100)
        ));

        // [BACK BUTTON]
        ItemStack backBtn = createItem(Material.ARROW, "§c§lBack", "§7Return to Main Menu");
        inv.setItem(26, backBtn);

        player.openInventory(inv);
    }

    private ItemStack createStatIcon(PlayerData data, String statKey, Material mat, String displayName, String desc) {
        int currentVal = data.getStat(statKey);
        int cost = plugin.getStatManager().getStatCost(currentVal);
        boolean canAfford = data.getStatPoints() >= cost;
        String color = canAfford ? "§a" : "§c";

        return createItem(mat, displayName,
                "§7" + desc, " ",
                "§7Current: §f" + currentVal,
                "§7Cost: " + color + cost + " Pts", " ",
                "§eLeft-Click: §7+1 Stat",
                "§eRight-Click: §7+10 Stats (Bulk)"
        );
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) loreList.add(Component.text(line));
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
}