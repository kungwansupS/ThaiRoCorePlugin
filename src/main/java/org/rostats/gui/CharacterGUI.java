package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;

import java.util.ArrayList;
import java.util.List;

public class CharacterGUI {

    public static final String TITLE_HEADER = "§0§lCharacter Status (ROO)";

    public enum Tab {
        BASIC_INFO, GENERAL, ADVANCED, SPECIAL, RESET_CONFIRM
    }

    private final ThaiRoCorePlugin plugin;

    public CharacterGUI(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Tab tab) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(TITLE_HEADER));
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        // 1. Header & Tabs
        displayHeader(inv, player, tab, data);

        // Background Filler
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        // 2. Content Display
        if (tab == Tab.BASIC_INFO) {
            displayAllocationMatrix(inv, player, data);
        } else if (tab == Tab.GENERAL) {
            inv.setItem(18, createItem(Material.BOOK, "§9§lGeneral Attribute", getGeneralLore(player, data)));
        } else if (tab == Tab.ADVANCED) {
            inv.setItem(18, createItem(Material.ENCHANTED_BOOK, "§6§lAttribute ระดับสูง", getAdvancedLore(player, data)));
        } else if (tab == Tab.SPECIAL) {
            inv.setItem(18, createItem(Material.NETHER_STAR, "§d§lAttribute พิเศษ", getSpecialLore(player, data)));
        } else if (tab == Tab.RESET_CONFIRM) {
            displayResetConfirm(inv, player, data);
        }

        player.openInventory(inv);
    }

    private void displayHeader(Inventory inv, Player player, Tab activeTab, PlayerData data) {
        // R0: Header Info
        inv.setItem(0, createItem(Material.PLAYER_HEAD, "§e§lCharacter Details", "§7ชื่อ: §f" + player.getName()));
        inv.setItem(1, createItem(Material.GOLDEN_HELMET, "§e§lJob/Class Info", "§7อาชีพ: NOVICE"));

        // Tabs
        inv.setItem(2, createTabItem(Tab.BASIC_INFO, activeTab, Material.DIAMOND, "§aBasic Info.", getBasicStatusLore(player, data)));
        inv.setItem(3, createTabItem(Tab.GENERAL, activeTab, Material.BOOK, "§9General Attribute", getGeneralLore(player, data)));
        inv.setItem(4, createTabItem(Tab.ADVANCED, activeTab, Material.ENCHANTED_BOOK, "§6Attribute ระดับสูง", getAdvancedLore(player, data)));
        inv.setItem(5, createTabItem(Tab.SPECIAL, activeTab, Material.NETHER_STAR, "§dAttribute พิเศษ", getSpecialLore(player, data)));

        // Presets (Placeholder)
        inv.setItem(6, createItem(Material.LIME_DYE, "§aPreset 1", "§7(ยังไม่เปิดใช้งาน)"));
        inv.setItem(7, createItem(Material.BLUE_DYE, "§bPreset 2", "§7(ยังไม่เปิดใช้งาน)"));

        // Exit
        inv.setItem(8, createItem(Material.BARRIER, "§c§lX", "§7ปิดหน้าต่าง"));

        // Status Points Info
        StatManager stats = plugin.getStatManager();
        int totalPendingCost = stats.getTotalPendingCost(data);
        int remainingPoints = data.getStatPoints() - totalPendingCost;

        inv.setItem(33, createItem(Material.GOLD_NUGGET, "§6§lแต้มคงเหลือ",
                "§7Points Available: §e" + data.getStatPoints(),
                "§7Pending Cost: §c" + totalPendingCost,
                "§7Remaining: §a" + remainingPoints
        ));

        // Buttons
        inv.setItem(34, createItem(Material.REDSTONE_BLOCK, "§c§lReset Point", "§7คลิกเพื่อเข้าสู่หน้ายืนยันการรีเซ็ตแต้ม"));
        inv.setItem(42, createItem(Material.ANVIL, "§c§lReset Select", "§7(ยังไม่เปิดใช้งาน) ยกเลิกแต้มที่เลือกไว้"));
        inv.setItem(44, createItem(Material.REDSTONE, "§c§lReset All", "§7คลิกเพื่อเข้าสู่หน้ายืนยันการรีเซ็ตแต้มทั้งหมด"));
        inv.setItem(52, createItem(Material.LIME_CONCRETE, "§a§lAllocate", "§7ยืนยันการอัพเกรด Stat ทั้งหมด"));
    }

    // 1. Basic Status Lore
    private String[] getBasicStatusLore(Player player, PlayerData data) {
        StatManager stats = plugin.getStatManager();
        double maxHP = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHP = player.getHealth();
        int baseLevel = data.getBaseLevel();
        int jobLevel = data.getJobLevel();
        double power = stats.calculatePower(player);
        double maxPower = 5000;

        // Level Cap Logic
        int maxBaseLevel = data.getMaxBaseLevel();
        int maxJobLevel = data.getMaxJobLevel();
        boolean baseIsMax = baseLevel >= maxBaseLevel;
        long baseExpReq = data.getBaseExpReq();
        String baseExpReqStr = baseIsMax ? "MAX" : String.valueOf(baseExpReq);
        boolean jobIsMax = jobLevel >= maxJobLevel;
        long jobExpReq = data.getJobExpReq();
        String jobExpReqStr = jobIsMax ? "MAX" : String.valueOf(jobExpReq);

        double baseExpForBar = data.getBaseExp();
        double jobExpForBar = data.getJobExp();
        if (baseIsMax) baseExpForBar = Math.max(baseExpForBar, baseExpReq);
        if (jobIsMax) jobExpForBar = Math.max(jobExpForBar, jobExpReq);

        List<String> lore = new ArrayList<>();
        lore.add("§7--- สถานะปัจจุบัน ---");
        lore.add("§aHP: §f" + String.format("%.0f/%.0f", currentHP, maxHP));
        lore.add(createBar(currentHP, maxHP, "HP"));
        lore.add("§bSP: §f" + String.format("%.0f/%.0f", data.getCurrentSP(), data.getMaxSP()));
        lore.add(createBar(data.getCurrentSP(), data.getMaxSP(), "SP"));
        lore.add("§9Base Lv§a" + baseLevel + " §8(" + data.getBaseExp() + "/" + baseExpReqStr + ")");
        lore.add(createBar(baseExpForBar, baseExpReq, "BASE_LV"));
        lore.add("§eJob Lv§a" + jobLevel + " §8(" + data.getJobExp() + "/" + jobExpReqStr + ")");
        lore.add(createBar(jobExpForBar, jobExpReq, "JOB_LV"));
        lore.add("§cStamina: §a100/100");
        lore.add(createBar(100.0, 100.0, "STAMINA"));
        lore.add("§9Power: §f" + String.format("%.0f", power));
        lore.add(createBar(power, maxPower, "POWER"));
        lore.add("§7--------------------");
        lore.add("§7คลิกเพื่อเปิดหน้าจออัพเกรดสเตตัส");

        return lore.toArray(new String[0]);
    }

    // 2. General Attribute
    private String[] getGeneralLore(Player player, PlayerData data) {
        StatManager stats = plugin.getStatManager();
        double maxHP = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();

        double regenIntDivisor = plugin.getConfig().getDouble("sp-regen.regen-int-divisor", 6.0);
        double currentSPRegen = 1 + ((data.getStat("INT") + data.getINTBonusGear()) / regenIntDivisor);
        currentSPRegen *= (1 + data.getHealingReceivedPercent() / 100.0);
        double currentHPRegen = data.getHPRegen();

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("   §6§lGeneral Attribute");
        lore.add(" ");

        lore.add(formatTwoColumns("§7HP: §e" + String.format("%.0f", maxHP), "§7SP: §e" + String.format("%.0f", data.getMaxSP())));
        lore.add(formatTwoColumns("§7P.ATK: §a" + String.format("%.0f", stats.getPhysicalAttack(player)), "§7P.DEF: §a" + String.format("%.0f", stats.getSoftDef(player))));
        lore.add(formatTwoColumns("§7M.ATK: §b" + String.format("%.0f", stats.getMagicAttack(player)), "§7M.DEF: §b" + String.format("%.0f", stats.getSoftMDef(player))));

        // Refine stats placeholder removed as requested

        lore.add(formatTwoColumns("§7ฟื้นฟู HP: §e" + String.format("%.0f", currentHPRegen), "§7SP Recovery: §e" + String.format("%.0f", currentSPRegen)));
        lore.add(formatTwoColumns("§7HIT: §f" + stats.getHit(player), "§7FLEE: §f" + stats.getFlee(player)));

        lore.add(" ");
        lore.add("§7--------------------");
        lore.add("§7คลิกเพื่อเปิดหน้าจอข้อมูลทั่วไป");

        return lore.toArray(new String[0]);
    }

    // 3. Advanced Attribute
    private String[] getAdvancedLore(Player player, PlayerData data) {
        StatManager stats = plugin.getStatManager();
        double totalCritResRaw = ((data.getStat("LUK") + data.getLUKBonusGear()) * 0.2) + data.getCritRes();

        // คำนวณ Variable Casting Total Reduction (Stats + Gear)
        double totalVarCTRed = stats.getVariableCastTimeReduction(data);

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("   §9§lAttribute ระดับสูง");
        lore.add(" ");

        // ASPD | MSPD%
        lore.add(formatTwoColumns("§7ASPD: §a" + String.format("%.0f%%", (stats.getAspdBonus(player) * 100.0)), "§7MSPD%: §a" + String.format("%.0f%%", data.getMSpdPercent())));

        // Variable CT | Variable Casting%
        // [FIXED] เพิ่มเครื่องหมายลบ (-) เพื่อแสดงว่าเป็นการลดเวลา
        lore.add(formatTwoColumns("§7Variable CT: §d" + String.format("%.0f", data.getVarCTFlat()), "§7Variable Casting%: §d" + String.format("-%.2f%%", totalVarCTRed)));

        // Fixed CT | Fixed Casting
        lore.add(formatTwoColumns("§7Fixed CT: §d" + String.format("%.0f", data.getFixedCTFlat()), "§7Fixed Casting: §d" + String.format("%.0f%%", data.getFixedCTPercent())));

        // Healing Effect% | Healing Received%
        lore.add(formatTwoColumns("§7Healing Effect%: §a" + String.format("%.2f%%", data.getHealingEffectPercent()), "§7Healing Received%: §a" + String.format("%.0f%%", data.getHealingReceivedPercent())));

        // CRIT | CRIT RES
        lore.add(formatTwoColumns("§7CRIT: §e" + String.format("%.0f", stats.getCritChance(player)), "§7CRIT RES: §e" + String.format("%.0f", totalCritResRaw)));

        // CRIT DMG | CRIT DMG RES
        lore.add(formatTwoColumns("§7CRIT DMG: §c" + String.format("%.0f%%", data.getCritDmgPercent()), "§7CRIT DMG RES: §c" + String.format("%.0f%%", data.getCritDmgResPercent())));

        // P.DMG Bonus | P.DMG Reduction (Standard)
        lore.add(formatTwoColumns("§7P.DMG Bonus: §a" + String.format("%.2f%%", data.getPDmgBonusPercent()), "§7P.DMG Reduction: §c" + String.format("%.0f%%", data.getPDmgReductionPercent())));

        // M.DMG Bonus | M.DMG Reduction (Standard)
        lore.add(formatTwoColumns("§7M.DMG Bonus: §b" + String.format("%.2f%%", data.getMDmgBonusPercent()), "§7M.DMG Reduction: §c" + String.format("%.0f%%", data.getMDmgReductionPercent())));

        // Ignore P.DEF | Ignore P.DEF%
        lore.add(formatTwoColumns("§7Ignore P.DEF: §6" + String.format("%.0f", data.getIgnorePDefFlat()), "§7Ignore P.DEF%: §6" + String.format("%.0f%%", data.getIgnorePDefPercent())));

        // Ignore M.DEF | Ignore M.DEF%
        lore.add(formatTwoColumns("§7Ignore M.DEF: §6" + String.format("%.0f", data.getIgnoreMDefFlat()), "§7Ignore M.DEF%: §6" + String.format("%.0f%%", data.getIgnoreMDefPercent())));

        // Melee P.DMG | Ranged P.DMG
        lore.add(formatTwoColumns("§7Melee P.DMG: §a" + String.format("%.0f%%", data.getMeleePDmgPercent()), "§7Ranged P.DMG: §a" + String.format("%.0f%%", data.getRangePDmgPercent())));

        // Melee P.DMG Reduction% | Range P.DMG Reduction%
        lore.add(formatTwoColumns("§7Melee P.DMG Reduction%: §c" + String.format("%.0f%%", data.getMeleePDReductionPercent()), "§7Range P.DMG Reduction%: §c" + String.format("%.0f%%", data.getRangePDReductionPercent())));

        // P.DMG Reduction% | M.DMG Reduction% (Final Reduction)
        lore.add(formatTwoColumns("§7P.DMG Reduction%: §c" + String.format("%.0f%%", data.getFinalDmgResPercent()), "§7M.DMG Reduction%: §c" + String.format("%.0f%%", data.getFinalDmgResPercent())));

        // P.DMG Bonus (Flat) | M.DMG Bonus (Flat)
        lore.add(formatTwoColumns("§7P.DMG Bonus: §e" + String.format("%.0f", data.getPDmgBonusFlat()), "§7M.DMG Bonus: §e" + String.format("%.0f", data.getMDmgBonusFlat())));

        // PVE DMG Reduction | PVE DMG Bonus
        lore.add(formatTwoColumns("§7PVE DMG Reduction: §c" + String.format("%.0f", data.getPveDmgReductionPercent()), "§7PVE DMG Bonus: §a" + String.format("%.0f", data.getPveDmgBonusPercent())));

        // PVP DMG Reduction | PVP DMG Bonus
        lore.add(formatTwoColumns("§7PVP DMG Reduction: §c" + String.format("%.0f", data.getPvpDmgReductionPercent()), "§7PVP DMG Bonus: §a" + String.format("%.0f", data.getPvpDmgBonusPercent())));

        lore.add("§7--------------------");
        lore.add("§7คลิกเพื่อเปิดหน้าจอข้อมูลการต่อสู้ขั้นสูง");

        return lore.toArray(new String[0]);
    }

    // 4. Special Attribute
    private String[] getSpecialLore(Player player, PlayerData data) {
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("   §d§lAttribute พิเศษ");
        lore.add(" ");

        // [FIXED] Add missing stats found in HTML
        lore.add(formatTwoColumns("§7Max HP%: §e" + String.format("%.2f%%", data.getMaxHPPercent()), "§7Max SP%: §e" + String.format("%.2f%%", data.getMaxSPPercent())));

        // Equipment ATK/MATK %
        lore.add(formatTwoColumns("§7Equipment P.ATK%: §a" + String.format("%.2f%%", data.getPDmgBonusPercent()), "§7Equipment M.ATK%: §b" + String.format("%.2f%%", data.getMDmgBonusPercent())));

        // Equipment P.DEF% (Mapping to PDmgReductionPercent as closest proxy in this context)
        lore.add(formatTwoColumns("§7Equipment P.DEF%: §c" + String.format("%.2f%%", data.getPDmgReductionPercent()), "§7Global CD%: §f0.00%"));

        lore.add("§7--------------------");
        lore.add("§7คลิกเพื่อเปิดหน้าจอข้อมูลพิเศษ");

        return lore.toArray(new String[0]);
    }

    private void displayAllocationMatrix(Inventory inv, Player player, PlayerData data) {
        createStatRow(inv, player, data, "STR", Material.IRON_SWORD, 9, 18, 27, 36, 45, "§cSTR");
        createStatRow(inv, player, data, "AGI", Material.FEATHER, 10, 19, 28, 37, 46, "§bAGI");
        createStatRow(inv, player, data, "VIT", Material.IRON_CHESTPLATE, 11, 20, 29, 38, 47, "§aVIT");
        createStatRow(inv, player, data, "INT", Material.ENCHANTED_BOOK, 12, 21, 30, 39, 48, "§dINT");
        createStatRow(inv, player, data, "DEX", Material.BOW, 13, 22, 31, 40, 49, "§6DEX");
        createStatRow(inv, player, data, "LUK", Material.RABBIT_FOOT, 14, 23, 32, 41, 50, "§eLUK");
    }

    private void displayResetConfirm(Inventory inv, Player player, PlayerData data) {
        int freeResets = plugin.getConfig().getInt("reset-system.free-resets", 3);
        int usedResets = data.getResetCount();
        String itemReq = plugin.getConfig().getString("reset-system.reset-item", "NETHER_STAR");
        Material resetItem = Material.getMaterial(itemReq);

        inv.setItem(29, createItem(Material.LIME_CONCRETE, "§a§l[ยืนยัน] รีเซ็ตสเตตัส",
                "§cคำเตือน: การกระทำนี้ไม่สามารถย้อนกลับได้!",
                "§7---",
                "§7การรีเซ็ตจะคืนแต้ม Stat ทั้งหมดตาม Base Level",
                "§7แต้มที่ใช้รีเซ็ตฟรี: §f" + Math.max(0, freeResets - usedResets) + " / " + freeResets,
                (usedResets >= freeResets) ? "§cต้องใช้: 1x " + (resetItem != null ? resetItem.name() : itemReq) : "§eเป็นการรีเซ็ตฟรี!",
                "§7---",
                "§eคลิกเพื่อดำเนินการต่อ"
        ));

        inv.setItem(31, createItem(Material.RED_CONCRETE, "§c§l[ยกเลิก]",
                "§7คลิกเพื่อกลับไปหน้าจอ Basic Info",
                "§eคลิกเพื่อยกเลิก"));

        inv.setItem(12, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(13, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(14, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
    }

    private void createStatRow(Inventory inv, Player player, PlayerData data, String statKey, Material mat, int statSlot, int bonusSlot, int reqSlot, int addSlot, int minusSlot, String statFullName) {
        StatManager stats = plugin.getStatManager();
        int currentVal = data.getStat(statKey);
        int pendingCount = data.getPendingStat(statKey);

        int bonusVal = switch (statKey) {
            case "STR" -> data.getSTRBonusGear();
            case "AGI" -> data.getAGIBonusGear();
            case "VIT" -> data.getVITBonusGear();
            case "INT" -> data.getINTBonusGear();
            case "DEX" -> data.getDEXBonusGear();
            case "LUK" -> data.getLUKBonusGear();
            default -> 0;
        };

        int totalVal = currentVal + bonusVal + pendingCount;
        int costNextPoint = stats.getStatCost(currentVal + pendingCount);
        int costPreviousPoint = stats.getStatCost(currentVal + pendingCount - 1);

        List<String> statLines = new ArrayList<>();
        statLines.add("§7Stats: §e" + currentVal + (pendingCount > 0 ? " §a(+" + pendingCount + ")" : ""));
        statLines.add("§7Total: §a" + totalVal);
        statLines.add("§7");
        statLines.addAll(getStatDescriptionLore(statKey));

        inv.setItem(statSlot, createItem(mat, statFullName, statLines.toArray(new String[0])));

        inv.setItem(bonusSlot, createItem(Material.DIAMOND, "§b§lBonus (" + statKey + ")",
                "§7Bonus: §b" + bonusVal,
                "§8(รวมในการคำนวณ Stat ปลายทาง)"
        ));

        inv.setItem(reqSlot, createItem(Material.IRON_NUGGET, "§6§lRequired Points",
                "§7Pending: §e" + pendingCount,
                "§7Total Cost: §c" + stats.getPendingCost(data, statKey)
        ));

        inv.setItem(addSlot, createItem(Material.GREEN_STAINED_GLASS_PANE, "§a§l+" + statKey,
                "§7Cost: §c" + costNextPoint + " แต้ม",
                "§7---",
                "§eคลิกซ้าย: §7+1 แต้ม",
                "§eคลิกขวา: §7+10 แต้ม"
        ));

        inv.setItem(minusSlot, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l-" + statKey,
                "§7Return: §a" + (pendingCount > 0 ? costPreviousPoint : 0),
                "§7---",
                "§eคลิกซ้าย: §7-1 แต้ม",
                "§eคลิกขวา: §7-10 แต้ม"
        ));
    }

    private List<String> getStatDescriptionLore(String statKey) {
        List<String> lines = new ArrayList<>();
        switch (statKey) {
            case "STR":
                lines.add("§cSTR §7- เพิ่มพลังโจมตีทางกายภาพ");
                lines.add("§7• P.ATK: §f+1");
                lines.add("§7• Ranged P.ATK: §f+0.2");
                break;
            case "AGI":
                lines.add("§bAGI §7- เพิ่มความเร็วและความคล่องตัว");
                lines.add("§7• FLEE: §f+1");
                lines.add("§7• Soft P.DEF: §f+0.2");
                lines.add("§7• ASPD: §fเพิ่มขึ้นเล็กน้อย");
                break;
            case "VIT":
                lines.add("§aVIT §7- เพิ่มความทนทานและการป้องกัน");
                lines.add("§7• Max HP: §f+1%");
                lines.add("§7• Soft P.DEF: §f+0.5");
                lines.add("§7• Soft M.DEF: §f+0.2");
                lines.add("§7• HP Recovery: §fเพิ่มขึ้น");
                break;
            case "INT":
                lines.add("§dINT §7- เพิ่มพลังเวทและประสิทธิภาพการใช้สกิล");
                lines.add("§7• M.ATK: §f+1.5");
                lines.add("§7• Soft M.DEF: §f+1");
                lines.add("§7• Max SP: §f+1%");
                lines.add("§7• SP Recovery: §fเพิ่มขึ้น");
                lines.add("§7• Variable Casting: §fลดลงเล็กน้อย");
                break;
            case "DEX":
                lines.add("§6DEX §7- เพิ่มความแม่นยำและพลังโจมตีระยะไกล");
                lines.add("§7• Ranged P.ATK: §f+1");
                lines.add("§7• Melee P.ATK: §f+0.2");
                lines.add("§7• Soft M.DEF: §f+0.2");
                lines.add("§7• HIT: §f+1");
                lines.add("§7• ASPD: §fเพิ่มขึ้นเล็กน้อย");
                lines.add("§7• Variable Casting: §fลดลงเล็กน้อย");
                break;
            case "LUK":
                lines.add("§eLUK §7- เพิ่มค่าคริติคอลและความหลากหลาย");
                lines.add("§7• Crit Rate: §f+0.3");
                lines.add("§7• Crit Resist: §f+0.2");
                lines.add("§7• P.ATK: §f+0.2");
                lines.add("§7• M.ATK: §f+0.3");
                break;
        }
        return lines;
    }

    private ItemStack createTabItem(Tab currentTab, Tab activeTab, Material mat, String name, String[] desc) {
        ItemStack item = createItem(mat, (currentTab == activeTab ? "§a§l" : "§f") + name, desc);
        if (currentTab == activeTab) item.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private String createBar(double current, double max, String type) {
        int length = plugin.getConfig().getInt("bar-length", 10);
        String[] colors = plugin.getConfig().getString("bar-colors." + type, "§f|§8").split("\\|");
        String fill = colors[0];
        String empty = colors[1];

        if (max == 0) return empty + "██████████";

        double effectiveMax = (max < 1 && max > 0) ? 1.0 : max;
        double effectiveCurrent = Math.min(current, effectiveMax);
        if (current > max && max > 0) effectiveCurrent = max;

        int fillAmount = (int) Math.round((effectiveCurrent / effectiveMax) * length);

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i < fillAmount) bar.append(fill);
            else bar.append(empty);
            bar.append("█");
        }
        return bar.toString();
    }

    private String formatTwoColumns(String left, String right) {
        final String separator = "§8| §7";
        final int MAX_LENGTH = 45;
        String strippedLeft = left.replaceAll("§[0-9a-fk-or]", "");
        String strippedRight = right.replaceAll("§[0-9a-fk-or]", "");

        int currentLength = strippedLeft.length() + separator.replaceAll("§[0-9a-fk-or]", "").length() + strippedRight.length();
        int padding = MAX_LENGTH - currentLength;
        if (padding < 1) padding = 1;

        return left + " ".repeat(padding) + separator + right;
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) loreList.add(Component.text(line));
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
}