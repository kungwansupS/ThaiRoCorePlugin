package org.rostats.itemeditor;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;
import org.rostats.ThaiRoCorePlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AttributeEditorGUI {

    private final ThaiRoCorePlugin plugin;
    private final ItemAttributeManager attributeManager;
    private static final int INVENTORY_SIZE = 54;
    public static final String GUI_TITLE = "§0§lItem Attribute Editor";

    // --- Static Map of Pages and Attributes ---
    // Maps: Page Index -> Tab Name -> List of Field Names
    private static final Map<Integer, Map<String, List<String>>> PAGES = new LinkedHashMap<>();

    // Static initializer for GUI structure
    static {
        // PAGE 1: GENERAL
        Map<String, List<String>> page1 = new LinkedHashMap<>();
        page1.put("Utility & Info", List.of("removeVanillaAttribute"));
        PAGES.put(1, page1);

        // PAGE 2: CORE STATS
        Map<String, List<String>> page2 = new LinkedHashMap<>();
        page2.put("Core Stats", List.of(
                "strGear", "agiGear", "vitGear", "intGear", "dexGear", "lukGear"
        ));
        PAGES.put(2, page2);

        // PAGE 3: OFFENSIVE
        Map<String, List<String>> page3 = new LinkedHashMap<>();
        page3.put("Weapon & Flat ATK", List.of(
                "weaponPAtk", "weaponMAtk", "pAtkFlat", "mAtkFlat"
        ));
        page3.put("Damage %/Flat", List.of(
                "pDmgPercent", "mDmgPercent", "pDmgFlat", "mDmgFlat"
        ));
        page3.put("Critical & Pen", List.of(
                "critDmgPercent", "critRes", "critDmgResPercent",
                "pPenFlat", "mPenFlat", "pPenPercent", "mPenPercent"
        ));
        page3.put("Final Damage", List.of(
                "finalDmgPercent", "finalPDmgPercent", "finalMDmgPercent", "finalDmgResPercent"
        ));
        PAGES.put(3, page3);

        // PAGE 4: DEFENSIVE
        Map<String, List<String>> page4 = new LinkedHashMap<>();
        page4.put("Reduction %", List.of(
                "pDmgReductionPercent", "mDmgReductionPercent",
                "meleePDReductionPercent", "rangePDReductionPercent"
        ));
        page4.put("Ignore Def", List.of(
                "ignorePDefFlat", "ignoreMDefFlat",
                "ignorePDefPercent", "ignoreMDefPercent"
        ));
        page4.put("Max HP/SP & Shield", List.of(
                "maxHPPercent", "maxSPPercent",
                "shieldValueFlat", "shieldRatePercent"
        ));
        PAGES.put(4, page4);

        // PAGE 5: UTILITY
        Map<String, List<String>> page5 = new LinkedHashMap<>();
        page5.put("Speed & Hit", List.of(
                "aSpdPercent", "mSpdPercent", "baseMSPD",
                "hitFlat", "fleeFlat"
        ));
        page5.put("Cast Time", List.of(
                "varCTPercent", "varCTFlat",
                "fixedCTPercent", "fixedCTFlat"
        ));
        page5.put("Healing & LifeSteal", List.of(
                "healingEffectPercent", "healingReceivedPercent",
                "lifestealPPercent", "lifestealMPercent",
                "trueDamageFlat"
        ));
        PAGES.put(5, page5);

        // PAGE 6: PVE/PVP & Save
        Map<String, List<String>> page6 = new LinkedHashMap<>();
        page6.put("PvE/PvP Damage", List.of(
                "pveDmgPercent", "pvpDmgPercent",
                "pveDmgReductionPercent", "pvpDmgReductionPercent"
        ));
        page6.put("Melee/Range DMG", List.of(
                "meleePDmgPercent", "rangePDmgPercent"
        ));
        PAGES.put(6, page6);
    }

    // --- Attribute Display Metadata (Simulating the old enum) ---
    private static final Map<String, AttributeMeta> ATTRIBUTE_METADATA = new LinkedHashMap<>();

    // Static initializer for metadata (Simplified for the massive list)
    static {
        // Core Stat Bonuses (int)
        ATTRIBUTE_METADATA.put("strGear", new AttributeMeta("§cSTR Bonus", Material.IRON_BLOCK, "%.0f", 1, 10, "int"));
        ATTRIBUTE_METADATA.put("agiGear", new AttributeMeta("§bAGI Bonus", Material.FEATHER, "%.0f", 1, 10, "int"));
        ATTRIBUTE_METADATA.put("vitGear", new AttributeMeta("§aVIT Bonus", Material.IRON_CHESTPLATE, "%.0f", 1, 10, "int"));
        ATTRIBUTE_METADATA.put("intGear", new AttributeMeta("§dINT Bonus", Material.ENCHANTED_BOOK, "%.0f", 1, 10, "int"));
        ATTRIBUTE_METADATA.put("dexGear", new AttributeMeta("§6DEX Bonus", Material.BOW, "%.0f", 1, 10, "int"));
        ATTRIBUTE_METADATA.put("lukGear", new AttributeMeta("§eLUK Bonus", Material.RABBIT_FOOT, "%.0f", 1, 10, "int"));

        // Flat ATK
        ATTRIBUTE_METADATA.put("weaponPAtk", new AttributeMeta("§cWeapon P.ATK", Material.IRON_SWORD, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("weaponMAtk", new AttributeMeta("§dWeapon M.ATK", Material.ENCHANTED_BOOK, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("pAtkFlat", new AttributeMeta("§cBonus P.ATK Flat", Material.REDSTONE, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("mAtkFlat", new AttributeMeta("§dBonus M.ATK Flat", Material.LAPIS_LAZULI, "%.0f", 1, 10));

        // Damage %/Flat
        ATTRIBUTE_METADATA.put("pDmgPercent", new AttributeMeta("§aP.DMG Bonus %", Material.DIAMOND_SWORD, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("mDmgPercent", new AttributeMeta("§bM.DMG Bonus %", Material.DIAMOND_HOE, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("pDmgFlat", new AttributeMeta("§cP.DMG Flat", Material.REDSTONE_BLOCK, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("mDmgFlat", new AttributeMeta("§dM.DMG Flat", Material.LAPIS_BLOCK, "%.0f", 1, 10));

        // Critical
        ATTRIBUTE_METADATA.put("critDmgPercent", new AttributeMeta("§eCrit DMG %", Material.YELLOW_DYE, "%.1f%%", 5.0, 10.0));
        ATTRIBUTE_METADATA.put("critDmgResPercent", new AttributeMeta("§eCrit DMG RES %", Material.SHULKER_SHELL, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("critRes", new AttributeMeta("§eCrit Resistance", Material.SHIELD, "%.1f", 1.0, 10.0));

        // Penetration / Ignore Def
        ATTRIBUTE_METADATA.put("pPenFlat", new AttributeMeta("§6P.Pen Flat", Material.IRON_NUGGET, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("mPenFlat", new AttributeMeta("§6M.Pen Flat", Material.LAPIS_LAZULI, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("pPenPercent", new AttributeMeta("§6P.Pen %", Material.IRON_INGOT, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("mPenPercent", new AttributeMeta("§6M.Pen %", Material.DIAMOND, "%.1f%%", 1.0, 5.0));

        // Final Damage
        ATTRIBUTE_METADATA.put("finalDmgPercent", new AttributeMeta("§6Final DMG %", Material.GOLD_INGOT, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("finalPDmgPercent", new AttributeMeta("§6Final P.DMG %", Material.DIAMOND_CHESTPLATE, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("finalMDmgPercent", new AttributeMeta("§6Final M.DMG %", Material.GOLDEN_CHESTPLATE, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("finalDmgResPercent", new AttributeMeta("§6Final DMG RES %", Material.GOLD_BLOCK, "%.1f%%", 1.0, 5.0));

        // PVE/PVP
        ATTRIBUTE_METADATA.put("pveDmgPercent", new AttributeMeta("§aPVE DMG Bonus %", Material.OAK_SAPLING, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("pvpDmgPercent", new AttributeMeta("§cPVP DMG Bonus %", Material.IRON_SWORD, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("pveDmgReductionPercent", new AttributeMeta("§aPVE DMG Reduce %", Material.SPRUCE_SAPLING, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("pvpDmgReductionPercent", new AttributeMeta("§cPVP DMG Reduce %", Material.IRON_AXE, "%.0f", 1, 10));

        // Reduction %
        ATTRIBUTE_METADATA.put("pDmgReductionPercent", new AttributeMeta("§cP.DMG Reduce %", Material.IRON_CHESTPLATE, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("mDmgReductionPercent", new AttributeMeta("§dM.DMG Reduce %", Material.LEATHER_CHESTPLATE, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("meleePDReductionPercent", new AttributeMeta("§cMelee PDR %", Material.LEATHER, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("rangePDReductionPercent", new AttributeMeta("§cRange PDR %", Material.ARROW, "%.1f%%", 1.0, 5.0));

        // Ignore Def
        ATTRIBUTE_METADATA.put("ignorePDefFlat", new AttributeMeta("§6Ignore P.DEF Flat", Material.ANVIL, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("ignoreMDefFlat", new AttributeMeta("§6Ignore M.DEF Flat", Material.BREWING_STAND, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("ignorePDefPercent", new AttributeMeta("§6Ignore P.DEF %", Material.IRON_BLOCK, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("ignoreMDefPercent", new AttributeMeta("§6Ignore M.DEF %", Material.LAPIS_BLOCK, "%.1f%%", 1.0, 5.0));

        // Max HP/SP & Shield
        ATTRIBUTE_METADATA.put("maxHPPercent", new AttributeMeta("§aMax HP %", Material.RED_WOOL, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("maxSPPercent", new AttributeMeta("§bMax SP %", Material.BLUE_WOOL, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("shieldValueFlat", new AttributeMeta("§bShield Value Flat", Material.PRISMARINE_SHARD, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("shieldRatePercent", new AttributeMeta("§bShield Rate %", Material.PRISMARINE_CRYSTALS, "%.1f%%", 1.0, 5.0));

        // Speed & Hit
        ATTRIBUTE_METADATA.put("aSpdPercent", new AttributeMeta("§aASPD %", Material.FEATHER, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("mSpdPercent", new AttributeMeta("§aMovement SPD %", Material.LEATHER_BOOTS, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("baseMSPD", new AttributeMeta("§aBase Move Speed", Material.SUGAR, "%.2f", 0.01, 0.1));
        ATTRIBUTE_METADATA.put("hitFlat", new AttributeMeta("§6HIT Flat", Material.TARGET, "%.0f", 1, 10));
        ATTRIBUTE_METADATA.put("fleeFlat", new AttributeMeta("§bFLEE Flat", Material.FEATHER, "%.0f", 1, 10));

        // Cast Time
        ATTRIBUTE_METADATA.put("varCTPercent", new AttributeMeta("§dVariable CT %", Material.CLOCK, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("varCTFlat", new AttributeMeta("§dVariable CT Flat (s)", Material.CLOCK, "%.1f", 0.1, 1.0));
        ATTRIBUTE_METADATA.put("fixedCTPercent", new AttributeMeta("§dFixed CT %", Material.COMPASS, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("fixedCTFlat", new AttributeMeta("§dFixed CT Flat (s)", Material.COMPASS, "%.1f", 0.1, 1.0));

        // Healing & LifeSteal
        ATTRIBUTE_METADATA.put("healingEffectPercent", new AttributeMeta("§aHealing Effect %", Material.GLOW_BERRIES, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("healingReceivedPercent", new AttributeMeta("§aHealing Receive %", Material.GLOWSTONE_DUST, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("lifestealPPercent", new AttributeMeta("§cP. Lifesteal %", Material.POISONOUS_POTATO, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("lifestealMPercent", new AttributeMeta("§dM. Lifesteal %", Material.ROTTEN_FLESH, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("trueDamageFlat", new AttributeMeta("§6True Damage Flat", Material.NETHER_STAR, "%.0f", 1, 10));

        // Utility
        ATTRIBUTE_METADATA.put("removeVanillaAttribute", new AttributeMeta("§cRemove Vanilla Attributes", Material.BARRIER, "Toggle", 1, 1, "boolean"));

        // Melee/Range DMG
        ATTRIBUTE_METADATA.put("meleePDmgPercent", new AttributeMeta("§aMelee P.DMG %", Material.BLAZE_ROD, "%.1f%%", 1.0, 5.0));
        ATTRIBUTE_METADATA.put("rangePDmgPercent", new AttributeMeta("§aRange P.DMG %", Material.SPECTRAL_ARROW, "%.1f%%", 1.0, 5.0));
    }

    // Helper class to encapsulate attribute metadata (replaces the enum)
    public static class AttributeMeta {
        public final String displayName;
        public final Material material;
        public final String format;
        public final double clickStep;
        public final double rightClickStep;
        public final String dataType; // "double", "int", or "boolean"

        public AttributeMeta(String displayName, Material material, String format, double clickStep, double rightClickStep) {
            this(displayName, material, format, clickStep, rightClickStep, "double");
        }

        public AttributeMeta(String displayName, Material material, String format, double clickStep, double rightClickStep, String dataType) {
            this.displayName = displayName;
            this.material = material;
            this.format = format;
            this.clickStep = clickStep;
            this.rightClickStep = rightClickStep;
            this.dataType = dataType;
        }
    }


    public AttributeEditorGUI(ThaiRoCorePlugin plugin, ItemAttributeManager attributeManager) {
        this.plugin = plugin;
        this.attributeManager = attributeManager;
    }

    public void open(Player player, ItemStack item, int page) {
        Inventory inv = Bukkit.createInventory(player, INVENTORY_SIZE, Component.text(GUI_TITLE + " (Page " + page + ")"));

        // Store the item being edited in the GUI (Slot 0)
        inv.setItem(0, item.clone());

        // Filler/Background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (i > 8 && i != 0) inv.setItem(i, filler); // Fill slots 9-53
        }

        // --- Header / Navigation Row (R0) ---
        displayHeader(inv, item, page);

        // --- Content Area (Slots 9-53, excluding borders) ---
        Map<String, List<String>> currentPage = PAGES.getOrDefault(page, new LinkedHashMap<>());
        int slot = 9;

        for (Map.Entry<String, List<String>> section : currentPage.entrySet()) {
            // Add Section Header (Slot 9, 18, 27, 36, 45, etc.)
            if (slot % 9 == 0) {
                inv.setItem(slot++, createItem(Material.BLACK_STAINED_GLASS_PANE, "§8§l--- " + section.getKey() + " ---"));
            }

            for (String attributeKey : section.getValue()) {
                if (slot >= INVENTORY_SIZE) break;
                // Place the attribute icon
                inv.setItem(slot++, createAttributeIcon(item, attributeKey));
            }

            // Skip to next row start if not already there
            while (slot % 9 != 0) {
                slot++;
            }
        }

        player.openInventory(inv);
    }

    // --- Header and Navigation ---
    private void displayHeader(Inventory inv, ItemStack item, int page) {
        // Find current attributes for display in header
        ItemAttribute currentAttr = attributeManager.getAttributesFromItem(item);

        // [R0, C0] Item Being Edited (Slot 0) - Already set in open()

        // [R0, C1] Item Info (Placeholder)
        inv.setItem(1, createItem(Material.PAPER, "§bCustom Lore", "§7Custom Lore lines are preserved when editing."));

        // Navigation Buttons
        int numPages = PAGES.size();

        // Previous Page (Slot 2)
        if (page > 1) {
            inv.setItem(2, createItem(Material.ARROW, "§aPrevious Page", "§7Click to go to Page " + (page - 1)));
        } else {
            inv.setItem(2, createItem(Material.GRAY_STAINED_GLASS_PANE, "§8First Page"));
        }

        // Page Info (Slot 3)
        inv.setItem(3, createItem(Material.COMPASS, "§bPage " + page + "/" + numPages, "§7Current Page View"));

        // Next Page (Slot 4)
        if (page < numPages) {
            inv.setItem(4, createItem(Material.ARROW, "§aNext Page", "§7Click to go to Page " + (page + 1)));
        } else {
            inv.setItem(4, createItem(Material.GRAY_STAINED_GLASS_PANE, "§8Last Page"));
        }

        // Utility Row
        inv.setItem(5, createItem(Material.BOOK, "§b§lLoad Template", "§7Click to load attributes from a template.")); // Load Template
        inv.setItem(6, createItem(Material.REDSTONE_BLOCK, "§c§lRemove Vanilla Attributes",
                currentAttr.isRemoveVanillaAttribute() ? "§aStatus: Enabled" : "§cStatus: Disabled",
                "§cClick to remove vanilla attributes.")); // Remove Vanilla
        inv.setItem(7, createItem(Material.ENDER_CHEST, "§d§lSave & Copy", "§7Click to save and clone the item.")); // Save & Copy
        inv.setItem(8, createItem(Material.BARRIER, "§cClose", "§7Close the editor and apply changes.")); // Close

        // R5, C8: Save / Export Buttons (Placeholder in Page 6)
        if (page == 6) {
            inv.setItem(52, createItem(Material.CHEST, "§aSave Attributes to PDC", "§7Attributes are saved automatically on click."));
            inv.setItem(53, createItem(Material.WRITABLE_BOOK, "§eExport to YAML", "§7Click to export item to a YAML template file."));
        }
    }

    /**
     * Creates an icon for an attribute, showing its current value and modification steps.
     */
    public ItemStack createAttributeIcon(ItemStack item, String attributeKey) {
        AttributeMeta meta = ATTRIBUTE_METADATA.get(attributeKey);
        if (meta == null) {
            return createItem(Material.RED_TERRACOTTA, "§cUnknown Attribute", "§7Key: " + attributeKey);
        }

        // Use reflection to get the current value from the POJO
        ItemAttribute currentAttr = attributeManager.getAttributesFromItem(item);
        double currentValue;
        try {
            Field field = ItemAttribute.class.getDeclaredField(attributeKey);
            field.setAccessible(true);
            Object value = field.get(currentAttr);

            if (value instanceof Double) {
                currentValue = (Double) value;
            } else if (value instanceof Integer) {
                currentValue = (Integer) value;
            } else if (value instanceof Boolean) {
                currentValue = (Boolean) value ? 1.0 : 0.0;
            } else {
                currentValue = 0.0;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            currentValue = 0.0;
        }

        List<String> lore = new ArrayList<>();

        if (meta.dataType.equals("boolean")) {
            boolean isEnabled = currentValue == 1.0;
            lore.add("§7Key: §f" + attributeKey);
            lore.add("§7Current Value: " + (isEnabled ? "§aEnabled" : "§cDisabled"));
            lore.add(" ");
            lore.add("§eLeft-Click: §7Toggle");
            lore.add("§eMiddle-Click: §7Toggle");
        } else {
            String formattedValue = String.format(meta.format, currentValue);
            lore.add("§7Key: §f" + attributeKey);
            lore.add("§7Current Value: " + (currentValue != 0 ? "§a" : "§7") + formattedValue);
            lore.add(" ");

            String format = meta.format;
            lore.add("§eLeft-Click: §7+" + String.format(format, meta.clickStep));
            lore.add("§eRight-Click: §7-" + String.format(format, meta.clickStep));
            lore.add("§eShift+Left-Click: §7+" + String.format(format, meta.rightClickStep));
            lore.add("§eShift+Right-Click: §7-" + String.format(format, meta.rightClickStep));
            lore.add("§eMiddle-Click: §7Reset to 0");
        }


        return createItem(meta.material, meta.displayName, lore.toArray(new String[0]));
    }

    /**
     * Maps an inventory slot to its corresponding ItemAttribute field name and page.
     * This method iterates through the PAGES structure to determine the attribute key.
     */
    public String getAttributeKeyBySlot(int slot, int page) {
        if (slot < 9 || slot >= INVENTORY_SIZE) return null;

        Map<String, List<String>> currentPage = PAGES.getOrDefault(page, new LinkedHashMap<>());

        int currentSlot = 9;

        for (Map.Entry<String, List<String>> section : currentPage.entrySet()) {
            currentSlot++; // Skip section header

            for (String attributeKey : section.getValue()) {
                if (currentSlot == slot) {
                    return attributeKey;
                }
                currentSlot++;
            }
            // Skip filler to next row start
            while (currentSlot % 9 != 0) {
                currentSlot++;
            }
        }
        return null;
    }

    // --- Helper to create a basic item stack ---
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

    // Helper to get the total number of pages
    public static int getMaxPages() {
        return PAGES.size();
    }
}