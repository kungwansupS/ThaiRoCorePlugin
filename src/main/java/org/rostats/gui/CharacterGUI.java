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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CharacterGUI {

    public enum Tab {
        BASIC_INFO, GENERAL, ADVANCED, SPECIAL, RESET_CONFIRM
    }

    private final ROStatsPlugin plugin;

    public CharacterGUI(ROStatsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Tab tab) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("§0§lCharacter Status (ROO)"));
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());

        // 1. Header & Tabs (Common to all screens)
        displayHeader(inv, player, tab, data);

        // Background & Filler
        ItemStack bg = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            if (inv.getItem(i) == null) inv.setItem(i, bg);
        }

        // 2. Content Display
        if (tab == Tab.BASIC_INFO) {
            displayAllocationMatrix(inv, player, data);
        } else if (tab == Tab.GENERAL) {
            inv.setItem(18, createItem(Material.BOOK, "§9§lGeneral Attribute Data", getGeneralLore(player, data)));
        } else if (tab == Tab.ADVANCED) {
            inv.setItem(18, createItem(Material.ENCHANTED_BOOK, "§6§lAdvanced Attribute Data", getAdvancedLore(player, data)));
        } else if (tab == Tab.SPECIAL) {
            inv.setItem(18, createItem(Material.NETHER_STAR, "§d§lSpecial Attribute Data", getSpecialLore(player, data)));
        } else if (tab == Tab.RESET_CONFIRM) {
            displayResetConfirm(inv, player, data);
        }

        player.openInventory(inv);
    }

    // --- Header & Layout Helpers ---

    private void displayHeader(Inventory inv, Player player, Tab activeTab, PlayerData data) {
        // R0: Header (Info, Job, Tabs, Presets, Exit)
        inv.setItem(0, createItem(Material.PLAYER_HEAD, "§e§lCharacter Details", "§7ชื่อ: §f" + player.getName()));
        inv.setItem(1, createItem(Material.GOLDEN_HELMET, "§e§lJob/Class Info", "§7อาชีพ: NOVICE (Placeholder)"));

        // Tabs (R0 C2, C3, C4, C5) - Consolidated Info into Lore
        inv.setItem(2, createTabItem(Tab.BASIC_INFO, activeTab, Material.DIAMOND, "§aBasic Info.", getBasicStatusLore(player, data)));
        inv.setItem(3, createTabItem(Tab.GENERAL, activeTab, Material.BOOK, "§9General Attribute", getGeneralLore(player, data)));
        inv.setItem(4, createTabItem(Tab.ADVANCED, activeTab, Material.ENCHANTED_BOOK, "§6Advanced Attribute", getAdvancedLore(player, data)));
        inv.setItem(5, createTabItem(Tab.SPECIAL, activeTab, Material.NETHER_STAR, "§dSpecial Attribute", getSpecialLore(player, data)));

        // R0 Presets (C6, C7)
        inv.setItem(6, createItem(Material.LIME_DYE, "§aPreset 1", "§7(ยังไม่เปิดใช้งาน) คลิกเพื่อโหลด Stat Preset 1"));
        inv.setItem(7, createItem(Material.BLUE_DYE, "§bPreset 2", "§7(ยังไม่เปิดใช้งาน) คลิกเพื่อโหลด Stat Preset 2"));

        // R0 C8: Exit
        inv.setItem(8, createItem(Material.BARRIER, "§c§lX", "§7คลิกเพื่อปิดหน้าจอสถานะ"));

        // R3 C6: Points Left (Slot 33)
        StatManager stats = plugin.getStatManager();
        int totalPendingCost = stats.getTotalPendingCost(data);
        int remainingPoints = data.getStatPoints() - totalPendingCost;

        inv.setItem(33, createItem(Material.GOLD_NUGGET, "§6§lแต้มคงเหลือ",
                "§7แต้มที่มีอยู่ (รวมแต้มรอดำเนินการ): §e" + data.getStatPoints(),
                "§7ค่าใช้จ่ายที่รอดำเนินการ: §c" + totalPendingCost,
                "§7แต้มคงเหลือ (หลังหักค่าใช้จ่าย): §a" + remainingPoints
        ));

        // R3 C7: Reset Point (Slot 34)
        inv.setItem(34, createItem(Material.REDSTONE_BLOCK, "§c§lReset Point", "§7คลิกเพื่อเข้าสู่หน้ายืนยันการรีเซ็ตแต้มทั้งหมด"));

        // R4 C6: Reset Select (Temp) (Slot 42)
        inv.setItem(42, createItem(Material.ANVIL, "§c§lReset Select", "§7(ยังไม่เปิดใช้งาน) คลิกเพื่อยกเลิกการอัพเกรดที่รอดำเนินการ"));

        // R4 C8: Reset All (Slot 44)
        inv.setItem(44, createItem(Material.REDSTONE, "§c§lReset All", "§7คลิกเพื่อเข้าสู่หน้ายืนยันการรีเซ็ตแต้มทั้งหมด"));

        // R5 C7: Allocate (Slot 52)
        inv.setItem(52, createItem(Material.LIME_CONCRETE, "§a§lAllocate", "§7(ยังไม่เปิดใช้งาน) คลิกเพื่อยืนยันการอัพเกรด Stat ทั้งหมด"));
    }

    // 1. Basic Status Lore (HP/SP/Level/Bars)
    private String[] getBasicStatusLore(Player player, PlayerData data) {
        StatManager stats = plugin.getStatManager();
        double maxHP = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHP = player.getHealth();
        int baseLevel = data.getBaseLevel();
        int jobLevel = data.getJobLevel();
        double power = stats.calculatePower(player);
        double maxPower = 5000;

        // --- Max Level Logic ---
        int maxBaseLevel = data.getMaxBaseLevel();
        int maxJobLevel = data.getMaxJobLevel();

        boolean baseIsMax = baseLevel >= maxBaseLevel;
        long baseExpReq = data.getBaseExpReq();
        String baseExpReqStr = baseIsMax ? "MAX" : String.valueOf(baseExpReq);

        boolean jobIsMax = jobLevel >= maxJobLevel;
        long jobExpReq = data.getJobExpReq();
        String jobExpReqStr = jobIsMax ? "MAX" : String.valueOf(jobExpReq);

        // For bar visualization, cap current EXP at required EXP for visual progress
        double baseExpForBar = data.getBaseExp();
        double jobExpForBar = data.getJobExp();

        // If maxed, ensure the bar shows full progress visually (current >= required)
        if (baseIsMax) baseExpForBar = Math.max(baseExpForBar, baseExpReq);
        if (jobIsMax) jobExpForBar = Math.max(jobExpForBar, jobExpReq);
        // -----------------------------


        List<String> lore = new ArrayList<>();
        lore.add("§7--- สถานะปัจจุบัน ---");

        lore.add("§aHP: §f" + String.format("%.0f/%.0f", currentHP, maxHP));
        lore.add(createBar(currentHP, maxHP, "HP"));
        lore.add("§bSP: §f" + String.format("%.0f/%.0f", data.getCurrentSP(), data.getMaxSP()));
        lore.add(createBar(data.getCurrentSP(), data.getMaxSP(), "SP"));

        // Base Level Display: Lv/LevelCap Exp/Req or Exp/MAX
        lore.add("§9Base Lv§a" + baseLevel + " §8(" + data.getBaseExp() + "/" + baseExpReqStr + ")");
        lore.add(createBar(baseExpForBar, baseExpReq, "BASE_LV"));

        // Job Level Display
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

    // 2. General Attribute Lore (Section 1) - Corrected for format
    private String[] getGeneralLore(Player player, PlayerData data) {
        StatManager stats = plugin.getStatManager();
        double maxHP = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        double currentHP = player.getHealth();
        // SP Recovery calculation (from PlayerData.regenSP logic)
        double regenIntDivisor = plugin.getConfig().getDouble("sp-regen.regen-int-divisor", 6.0);
        double currentSPRegen = 1 + (data.getStat("INT") / regenIntDivisor);
        currentSPRegen *= (1 + data.getHealingReceivedPercent() / 100.0);
        // HP Recovery calculation (from PlayerData.getHPRegen logic)
        double currentHPRegen = data.getHPRegen();

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("   §6§lGENERAL ATTRIBUTE");
        lore.add(" ");

        // HP | SP
        lore.add(formatTwoColumns("§7HP: §e" + String.format("%.0f/%.0f", currentHP, maxHP), "§7SP: §e" + String.format("%.0f/%.0f", data.getCurrentSP(), data.getMaxSP())));
        // P.ATK | M.ATK
        lore.add(formatTwoColumns("§7P.ATK: §a" + String.format("%.0f", stats.getPhysicalAttack(player)), "§7M.ATK: §b" + String.format("%.0f", stats.getMagicAttack(player))));
        // P.DEF | M.DEF
        lore.add(formatTwoColumns("§7P.DEF: §a" + String.format("%.0f", stats.getSoftDef(player)), "§7M.DEF: §b" + String.format("%.0f", stats.getSoftMDef(player))));
        // HP Recovery | SP Recovery
        lore.add(formatTwoColumns("§7HP Recovery: §e" + String.format("%.1f", currentHPRegen), "§7SP Recovery: §e" + String.format("%.1f", currentSPRegen)));
        // HIT | FLEE
        lore.add(formatTwoColumns("§7HIT: §f" + stats.getHit(player), "§7FLEE: §f" + stats.getFlee(player)));

        lore.add(" ");
        lore.add("§7--------------------");
        lore.add("§7คลิกเพื่อเปิดหน้าจอข้อมูลทั่วไป");

        return lore.toArray(new String[0]);
    }

    // 3. Advanced Attribute Lore (Section 2) - Corrected for format and suffixes
    private String[] getAdvancedLore(Player player, PlayerData data) {
        StatManager stats = plugin.getStatManager();
        double totalCritResRaw = (data.getStat("LUK") * 0.2) + data.getCritRes();

        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("   §9§lADVANCED ATTRIBUTE");
        lore.add(" ");

        // Speed / Cast
        lore.add("§f§l-- Speed / Cast --");
        // ASPD / MSPD
        lore.add(formatTwoColumns("§7ASPD: §a" + String.format("%.0f%%", (stats.getAspdBonus(player) * 100.0)), "§7MSPD: §a" + String.format("%.1f%%", data.getMSpdPercent())));
        // Variable CT/Casting (Added 's' suffix to CT fields)
        lore.add(formatTwoColumns("§7Variable CT: §d" + String.format("%.1f", data.getVarCTFlat()) + "s", "§7Variable Casting: §d" + String.format("%.1f%%", data.getVarCTPercent())));
        // Fixed CT/Casting (Added 's' suffix to CT fields)
        lore.add(formatTwoColumns("§7Fixed CT: §d" + String.format("%.1f", data.getFixedCTFlat()) + "s", "§7Fixed Casting: §d" + String.format("%.1f%%", data.getFixedCTPercent())));
        lore.add(" ");

        // Healing
        lore.add("§f§l-- Healing --");
        // Healing Effect/Receive
        lore.add(formatTwoColumns("§7Healing Effect: §a" + String.format("%.1f%%", data.getHealingEffectPercent()), "§7Healing Receive: §a" + String.format("%.1f%%", data.getHealingReceivedPercent())));
        lore.add(" ");

        // Critical
        lore.add("§f§l-- Critical --");
        // CRIT / CRIT RES
        lore.add(formatTwoColumns("§7CRIT: §e" + String.format("%.1f", stats.getCritChance(player)), "§7CRIT RES: §e" + String.format("%.1f", totalCritResRaw)));
        // CRIT DMG / CRIT DMG RES
        lore.add(formatTwoColumns("§7CRIT DMG: §c" + String.format("%.1f%%", data.getCritDmgPercent()), "§7CRIT DMG RES: §c" + String.format("%.1f%%", data.getCritDmgResPercent())));
        lore.add(" ");

        // Damage Bonus
        lore.add("§f§l-- Damage Bonus --");
        // P.DMG Bonus/M.DMG Bonus
        lore.add(formatTwoColumns("§7P.DMG Bonus: §a" + String.format("%.1f%%", data.getPDmgBonusPercent()), "§7M.DMG Bonus: §b" + String.format("%.1f%%", data.getMDmgBonusPercent())));
        lore.add(" ");

        // Damage Reduction
        lore.add("§f§l-- Damage Reduction --");
        // P.DMG Reduction/M.DMG Reduction
        lore.add(formatTwoColumns("§7P.DMG Reduction: §c" + String.format("%.1f%%", data.getPDmgReductionPercent()), "§7M.DMG Reduction: §c" + String.format("%.1f%%", data.getMDmgReductionPercent())));
        lore.add(" ");

        // Melee / Range
        lore.add("§f§l-- Melee / Range --");
        // Melee P.DMG/Range P.DMG
        lore.add(formatTwoColumns("§7Melee P.DMG: §a" + String.format("%.1f%%", data.getMeleePDmgPercent()), "§7Range P.DMG: §a" + String.format("%.1f%%", data.getRangePDmgPercent())));
        // Melee Reduction/Range Reduction
        lore.add(formatTwoColumns("§7Melee Reduction: §c" + String.format("%.1f%%", data.getMeleePDReductionPercent()), "§7Range Reduction: §c" + String.format("%.1f%%", data.getRangePDReductionPercent())));
        lore.add(" ");

        // Ignore Defense
        lore.add("§f§l-- Ignore Defense --");
        // Ignore P.DEF/M.DEF (Flat)
        lore.add(formatTwoColumns("§7Ignore P.DEF: §6" + String.format("%.0f", data.getIgnorePDefFlat()), "§7Ignore M.DEF: §6" + String.format("%.0f", data.getIgnoreMDefFlat())));
        // Ignore P.DEF%/M.DEF%
        lore.add(formatTwoColumns("§7Ignore P.DEF%: §6" + String.format("%.1f%%", data.getIgnorePDefPercent()), "§7Ignore M.DEF%: §6" + String.format("%.1f%%", data.getIgnoreMDefPercent())));
        lore.add(" ");

        // Flat DMG Boost
        lore.add("§f§l-- Flat DMG Boost --");
        // P.DMG Bonus+/M.DMG Bonus+
        lore.add(formatTwoColumns("§7P.DMG Bonus+: §e" + String.format("%.0f", data.getPDmgBonusFlat()), "§7M.DMG Bonus+: §e" + String.format("%.0f", data.getMDmgBonusFlat())));
        lore.add(" ");

        // PVE / PVP (RAW Difference Model) (Updated Heading and fields to RAW model)
        lore.add("§f§l-- PVE / PVP (RAW Difference Model) --");
        // PVE RAW Bonus/PVP RAW Bonus (Using percentage fields as flat RAW values for display)
        lore.add(formatTwoColumns("§7PVE RAW Bonus: §a" + String.format("%.0f", data.getPveDmgBonusPercent()), "§7PVP RAW Bonus: §c" + String.format("%.0f", data.getPvpDmgBonusPercent())));
        // PVE RAW Reduce/PVP RAW Reduce (Using percentage fields as flat RAW values for display)
        lore.add(formatTwoColumns("§7PVE RAW Reduce: §a" + String.format("%.0f", data.getPveDmgReductionPercent()), "§7PVP RAW Reduce: §c" + String.format("%.0f", data.getPvpDmgReductionPercent())));

        lore.add("§7--------------------");
        lore.add("§7คลิกเพื่อเปิดหน้าจอข้อมูลการต่อสู้ขั้นสูง");

        return lore.toArray(new String[0]);
    }

    // 4. Special Attribute Lore (Section 3) - Corrected for format
    private String[] getSpecialLore(Player player, PlayerData data) {
        List<String> lore = new ArrayList<>();
        lore.add("§7");
        lore.add("   §d§lSPECIAL ATTRIBUTE");
        lore.add(" ");

        // Max HP% | Max SP%
        lore.add(formatTwoColumns("§7Max HP%: §e" + String.format("%.1f%%", data.getMaxHPPercent()), "§7Max SP%: §e" + String.format("%.1f%%", data.getMaxSPPercent())));
        lore.add(" "); // Separator

        // Lifesteal P | Lifesteal M
        lore.add(formatTwoColumns("§7Lifesteal P: §f" + String.format("%.1f%%", data.getLifestealPPercent()), "§7Lifesteal M: §f" + String.format("%.1f%%", data.getLifestealMPercent())));
        // True DMG | Shield
        lore.add(formatTwoColumns("§7True DMG: §f" + String.format("%.0f", data.getTrueDamageFlat()), "§7Shield: §f" + String.format("%.0f", data.getShieldValueFlat())));
        // Shield Rate | Reserved
        lore.add(formatTwoColumns("§7Shield Rate: §f" + String.format("%.1f%%", data.getShieldRatePercent()), "§7Reserved"));

        lore.add("§7--------------------");
        lore.add("§7คลิกเพื่อเปิดหน้าจอข้อมูลพิเศษ");

        return lore.toArray(new String[0]);
    }

    // --- Stat Row Helper (R1, R2, R3, R4, R5) ---
    private void displayAllocationMatrix(Inventory inv, Player player, PlayerData data) {
        // R1: Stats (Slots 9-14)
        // R2: Bonus (Slots 18-23)
        // R3: Req (Slots 27-32)
        // R4: + Button (Slots 36-41)
        // R5: - Button (Slots 45-50)

        // Column 0-5
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

        // Confirmation Button (Slot 29)
        inv.setItem(29, createItem(Material.LIME_CONCRETE, "§a§l[ยืนยัน] รีเซ็ตสเตตัส",
                "§cคำเตือน: การกระทำนี้ไม่สามารถย้อนกลับได้!",
                "§7---",
                "§7การรีเซ็ตจะคืนแต้ม Stat ทั้งหมดตาม Base Level",
                "§7แต้มที่ใช้รีเซ็ตฟรี: §f" + Math.max(0, freeResets - usedResets) + " / " + freeResets,
                (usedResets >= freeResets) ? "§cต้องใช้: 1x " + (resetItem != null ? resetItem.name() : itemReq) : "§eเป็นการรีเซ็ตฟรี!",
                "§7---",
                "§eคลิกเพื่อดำเนินการต่อ"
        ));

        // Cancel Button (Slot 31)
        inv.setItem(31, createItem(Material.RED_CONCRETE, "§c§l[ยกเลิก]",
                "§7คลิกเพื่อกลับไปหน้าจอ Basic Info",
                "§eคลิกเพื่อยกเลิก"));

        // Clear R1 C3-C5 area
        inv.setItem(12, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(13, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
        inv.setItem(14, createItem(Material.GRAY_STAINED_GLASS_PANE, " "));
    }

    private void createStatRow(Inventory inv, Player player, PlayerData data, String statKey, Material mat, int statSlot, int bonusSlot, int reqSlot, int addSlot, int minusSlot, String statFullName) {
        StatManager stats = plugin.getStatManager();
        int currentVal = data.getStat(statKey);
        int pendingCount = data.getPendingStat(statKey);

        int bonusVal = 0;
        int totalVal = currentVal + bonusVal + pendingCount;

        int costNextPoint = stats.getStatCost(currentVal + pendingCount);
        int costPreviousPoint = stats.getStatCost(currentVal + pendingCount - 1);

        // Get the specific stat descriptions
        List<String> statLines = new ArrayList<>();
        statLines.add("§7Stats (แต้มที่อัพ): §e" + currentVal + (pendingCount > 0 ? " §a(+" + pendingCount + ")" : ""));
        statLines.add("§7Total: §a" + totalVal); // Base/Total line
        statLines.add("§7"); // Separator
        statLines.addAll(getStatDescriptionLore(statKey)); // New detailed description lines

        // 1. Stat Icon & Value (Slot: R1)
        inv.setItem(statSlot, createItem(mat, statFullName, statLines.toArray(new String[0])));

        // 2. Bonus Slot (Slot: R2)
        inv.setItem(bonusSlot, createItem(Material.DIAMOND, "§b§lBonus (" + statKey + ")",
                "§7Bonus Stats (จากอุปกรณ์/บัฟ): §b" + bonusVal,
                "§8(ยังไม่เปิดใช้งาน)"));

        // 3. Req Slot (Slot: R3) - Displays cumulative cost
        inv.setItem(reqSlot, createItem(Material.IRON_NUGGET, "§6§lRequired Points",
                "§7แต้มรอดำเนินการ: §e" + pendingCount,
                "§7ค่าใช้จ่ายรวม: §c" + stats.getPendingCost(data, statKey)
        ));

        // 4. + Button (Slot: R4)
        inv.setItem(addSlot, createItem(Material.GREEN_STAINED_GLASS_PANE, "§a§l+" + statKey,
                "§7แต้มที่ต้องการ: §c" + costNextPoint + " แต้ม",
                "§7---",
                "§eคลิกซ้าย: §7เพิ่ม 1 แต้ม",
                "§eคลิกขวา: §7เพิ่ม 10 แต้ม"
        ));

        // 5. - Button (Slot: R5)
        inv.setItem(minusSlot, createItem(Material.RED_STAINED_GLASS_PANE, "§c§l-" + statKey,
                "§7แต้มที่คืน: §a" + (pendingCount > 0 ? costPreviousPoint : 0),
                "§7---",
                "§eคลิกซ้าย: §7ลด 1 แต้ม",
                "§eคลิกขวา: §7ลด 10 แต้ม"
        ));
    }

    private List<String> getStatDescriptionLore(String statKey) {
        List<String> lines = new ArrayList<>();

        switch (statKey) {
            case "STR":
                lines.add("§cSTR §7- เพิ่มพลังโจมตีทางกายภาพ");
                // +1 P.ATK per STR
                lines.add("§7• P.ATK: §f+1 §7ต่อ STR");
                // +0.2 P.ATK (ranged component) per STR
                lines.add("§7• Ranged P.ATK: §f+0.2 §7ต่อ STR");
                // (No Weapon ATK % because plugin does not implement it)
                break;
            case "AGI":
                lines.add("§bAGI §7- เพิ่มความเร็วและความคล่องตัว");
                // +1 FLEE per AGI
                lines.add("§7• FLEE: §f+1 §7ต่อ AGI");
                // +0.2 P.DEF per AGI
                lines.add("§7• Soft P.DEF: §f+0.2 §7ต่อ AGI");
                // (Increases ASPD slightly – plugin-based)
                lines.add("§7• ASPD: §fเพิ่มขึ้นเล็กน้อย");
                break;
            case "VIT":
                lines.add("§aVIT §7- เพิ่มความทนทานและการป้องกัน");
                // +1% MaxHP per VIT
                lines.add("§7• Max HP: §f+1% §7ต่อ VIT");
                // +0.5 P.DEF per VIT
                lines.add("§7• Soft P.DEF: §f+0.5 §7ต่อ VIT");
                // +0.2 M.DEF per VIT
                lines.add("§7• Soft M.DEF: §f+0.2 §7ต่อ VIT");
                // (Increases HP Recovery)
                lines.add("§7• HP Recovery: §fเพิ่มขึ้น");
                break;
            case "INT":
                lines.add("§dINT §7- เพิ่มพลังเวทและประสิทธิภาพการใช้สกิล");
                // +1.5 M.ATK per INT
                lines.add("§7• M.ATK: §f+1.5 §7ต่อ INT");
                // +1 M.DEF per INT
                lines.add("§7• Soft M.DEF: §f+1 §7ต่อ INT");
                // +1% MaxSP per INT
                lines.add("§7• Max SP: §f+1% §7ต่อ INT");
                // (Increases SP Recovery)
                lines.add("§7• SP Recovery: §fเพิ่มขึ้น");
                // (reduces cast time slightly)
                lines.add("§7• Variable Cast Time: §fลดลงเล็กน้อย");
                break;
            case "DEX":
                lines.add("§6DEX §7- เพิ่มความแม่นยำและพลังโจมตีระยะไกล");
                // +1 Ranged P.ATK per DEX
                lines.add("§7• Ranged P.ATK: §f+1 §7ต่อ DEX");
                // +0.2 Melee P.ATK per DEX
                lines.add("§7• Melee P.ATK: §f+0.2 §7ต่อ DEX");
                // +0.2 M.DEF per DEX
                lines.add("§7• Soft M.DEF: §f+0.2 §7ต่อ DEX");
                // +1 HIT per DEX
                lines.add("§7• HIT: §f+1 §7ต่อ DEX");
                // (Increases ASPD slightly)
                lines.add("§7• ASPD: §fเพิ่มขึ้นเล็กน้อย");
                // (reduces cast time slightly)
                lines.add("§7• Variable Cast Time: §fลดลงเล็กน้อย");
                break;
            case "LUK":
                lines.add("§eLUK §7- เพิ่มค่าคริติคอลและความหลากหลายของค่าสถานะ");
                // +0.3 CRIT per LUK
                lines.add("§7• CRIT: §f+0.3 §7ต่อ LUK");
                // +0.2 CRIT RES per LUK
                lines.add("§7• CRIT RES: §f+0.2 §7ต่อ LUK");
                // +0.2 P.ATK per LUK
                lines.add("§7• P.ATK: §f+0.2 §7ต่อ LUK");
                // +0.3 M.ATK per LUK
                lines.add("§7• M.ATK: §f+0.3 §7ต่อ LUK");
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

        // For max level visual bug fix: if current > max, the bar should be full.
        double effectiveMax = (max < 1 && max > 0) ? 1.0 : max; // Ensure division by zero doesn't occur for small values
        double effectiveCurrent = Math.min(current, effectiveMax);
        if (current > max && max > 0) effectiveCurrent = max; // If capped, treat current as max for progress bar, even if it overflowed internally.


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
        final int MAX_LENGTH = 45; // Adjusted max length for better fit
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
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES); // HIDE VANILLA LORE
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) loreList.add(Component.text(line));
        meta.lore(loreList);
        item.setItemMeta(meta);
        return item;
    }
}