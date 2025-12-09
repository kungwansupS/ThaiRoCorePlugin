package org.rostats.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.engine.action.ActionType;
import org.rostats.engine.action.SkillAction;

import java.util.Arrays;
import java.util.Map;

public class SkillActionPropertyGUI {

    private final ThaiRoCorePlugin plugin;
    private final String skillId;
    private final int actionIndex;
    private final ActionType type;
    private final Map<String, Object> data;

    public SkillActionPropertyGUI(ThaiRoCorePlugin plugin, String skillId, int actionIndex, SkillAction action) {
        this.plugin = plugin;
        this.skillId = skillId;
        this.actionIndex = actionIndex;
        this.type = action.getType();
        this.data = action.serialize();
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("ActionEdit: " + skillId + " #" + actionIndex));
        int slot = 0;

        if (type == ActionType.DAMAGE) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "Formula", (String)data.getOrDefault("formula","ATK"), "§eEdit Formula"));
            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "element", "Element", (String)data.getOrDefault("element","NEUTRAL"), "§eEdit Element"));
        }
        else if (type == ActionType.HEAL) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "Heal Amount", (String)data.getOrDefault("formula","10"), "§eEdit Amount"));
            inv.setItem(slot++, createPropItem(Material.REDSTONE, "is-mana", "Is Mana?", data.getOrDefault("is-mana",false).toString(), "§eToggle (HP/SP)"));
            // [UPDATED] Add Self Only
            inv.setItem(slot++, createPropItem(Material.PLAYER_HEAD, "self-only", "Self Only?", data.getOrDefault("self-only", true).toString(), "§eToggle Target Mode"));
        }
        else if (type == ActionType.APPLY_EFFECT) {
            inv.setItem(slot++, createPropItem(Material.NAME_TAG, "effect-id", "ID", (String)data.getOrDefault("effect-id","unknown"), "§eEdit ID"));
            inv.setItem(slot++, createPropItem(Material.POTION, "effect-type", "Type", (String)data.getOrDefault("effect-type","STAT_MODIFIER"), "§eEdit Type"));
            inv.setItem(slot++, createPropItem(Material.EXPERIENCE_BOTTLE, "level", "Level", data.getOrDefault("level",1).toString(), "§eEdit Level"));
            inv.setItem(slot++, createPropItem(Material.IRON_SWORD, "power", "Power", data.getOrDefault("power",0.0).toString(), "§eEdit Power"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "duration", "Duration", data.getOrDefault("duration",100).toString(), "§eEdit Duration (Ticks)"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "chance", "Chance", data.getOrDefault("chance",1.0).toString(), "§eEdit Chance (0.0-1.0)"));
            inv.setItem(slot++, createPropItem(Material.ANVIL, "stat-key", "Stat Key", (String)data.getOrDefault("stat-key","None"), "§eEdit Key (e.g. STR)"));
        }
        else if (type == ActionType.SOUND) {
            inv.setItem(slot++, createPropItem(Material.NOTE_BLOCK, "sound", "Sound Name", (String)data.getOrDefault("sound","ENTITY_EXPERIENCE_ORB_PICKUP"), "§eEdit Sound"));
            inv.setItem(slot++, createPropItem(Material.REPEATER, "volume", "Volume", data.getOrDefault("volume",1.0).toString(), "§eEdit Vol"));
            inv.setItem(slot++, createPropItem(Material.COMPARATOR, "pitch", "Pitch", data.getOrDefault("pitch",1.0).toString(), "§eEdit Pitch"));
        }
        else if (type == ActionType.PARTICLE) {
            // [UPDATED] Particle Properties
            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "particle", "Particle Name", (String)data.getOrDefault("particle","VILLAGER_HAPPY"), "§eEdit Particle"));
            inv.setItem(slot++, createPropItem(Material.GLOWSTONE_DUST, "count", "Count", data.getOrDefault("count","5").toString(), "§eEdit Count (Expr)"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "Speed", data.getOrDefault("speed","0.1").toString(), "§eEdit Speed (Expr)"));
            inv.setItem(slot++, createPropItem(Material.COMPASS, "shape", "Shape", (String)data.getOrDefault("shape", "POINT"), "§eEdit (POINT/CIRCLE/SPHERE/CYLINDER/SQUARE)"));
            inv.setItem(slot++, createPropItem(Material.ENDER_PEARL, "radius", "Radius/Length", data.getOrDefault("radius","0.5").toString(), "§eEdit Radius (Expr)"));
            inv.setItem(slot++, createPropItem(Material.ANVIL, "points", "Points/Slices", data.getOrDefault("points","20").toString(), "§eEdit Points (for Shapes)"));
        }
        else if (type == ActionType.POTION) {
            inv.setItem(slot++, createPropItem(Material.POTION, "potion", "Potion Type", (String)data.getOrDefault("potion","SPEED"), "§eEdit Potion"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "duration", "Duration", data.getOrDefault("duration",60).toString(), "§eEdit Duration (Ticks)"));
            inv.setItem(slot++, createPropItem(Material.GLOWSTONE, "amplifier", "Amplifier", data.getOrDefault("amplifier",0).toString(), "§eEdit Amp"));
            // [UPDATED] Add Self Only
            inv.setItem(slot++, createPropItem(Material.PLAYER_HEAD, "self-only", "Self Only?", data.getOrDefault("self-only", true).toString(), "§eToggle Target Mode"));
        }
        else if (type == ActionType.TELEPORT) {
            inv.setItem(slot++, createPropItem(Material.ENDER_PEARL, "range", "Range", data.getOrDefault("range",5.0).toString(), "§eEdit Range"));
            inv.setItem(slot++, createPropItem(Material.ENDER_EYE, "to-target", "To Target", data.getOrDefault("to-target",false).toString(), "§eToggle"));
        }
        else if (type == ActionType.PROJECTILE) {
            inv.setItem(slot++, createPropItem(Material.ARROW, "projectile", "Projectile Type", (String)data.getOrDefault("projectile","ARROW"), "§eEdit Proj. Type"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "Speed", data.getOrDefault("speed",1.0).toString(), "§eEdit Speed"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "on-hit", "On-Hit Skill ID", (String)data.getOrDefault("on-hit","none"), "§eEdit On-Hit ID"));
        }
        else if (type == ActionType.AREA_EFFECT) {
            // [UPDATED] Area Effect Properties
            inv.setItem(slot++, createPropItem(Material.BEACON, "radius", "Radius", data.getOrDefault("radius", 5.0).toString(), "§eEdit Radius (Blocks)"));
            inv.setItem(slot++, createPropItem(Material.ZOMBIE_HEAD, "target-type", "Target Type", (String) data.getOrDefault("target-type", "ENEMY"), "§eEdit (ENEMY/ALLY/ALL)"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "sub-skill", "Sub Skill ID", (String) data.getOrDefault("sub-skill", "none"), "§eEdit Sub Skill"));
            inv.setItem(slot++, createPropItem(Material.SKELETON_SKULL, "max-targets", "Max Targets", data.getOrDefault("max-targets", 10).toString(), "§eEdit Max Count"));
        }
        else if (type == ActionType.COMMAND) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "command", "Command", (String)data.getOrDefault("command", "say Hi %player%"), "§eEdit Command String"));
            inv.setItem(slot++, createPropItem(Material.REDSTONE, "as-console", "As Console?", data.getOrDefault("as-console", false).toString(), "§eToggle (Console/Player)"));
        }
        else if (type == ActionType.VELOCITY) {
            inv.setItem(slot++, createPropItem(Material.DIAMOND, "x", "Velocity X", data.getOrDefault("x",0.0).toString(), "§eEdit Velocity X (Expr)"));
            inv.setItem(slot++, createPropItem(Material.DIAMOND, "y", "Velocity Y", data.getOrDefault("y",0.0).toString(), "§eEdit Velocity Y (Expr)"));
            inv.setItem(slot++, createPropItem(Material.DIAMOND, "z", "Velocity Z", data.getOrDefault("z",0.0).toString(), "§eEdit Velocity Z (Expr)"));
            inv.setItem(slot++, createPropItem(Material.REDSTONE, "add", "Additive?", data.getOrDefault("add",true).toString(), "§eToggle (Add/Set)"));
        }
        else if (type == ActionType.LOOP) {
            inv.setItem(slot++, createPropItem(Material.CLOCK, "start", "Start (Expr)", (String)data.getOrDefault("start", "0"), "§eEdit Start Formula"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "end", "End (Expr)", (String)data.getOrDefault("end", "10"), "§eEdit End Formula"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "step", "Step (Expr)", (String)data.getOrDefault("step", "1"), "§eEdit Step Formula"));
            inv.setItem(slot++, createPropItem(Material.NAME_TAG, "var", "Variable Name", (String)data.getOrDefault("var", "i"), "§eEdit Variable Name (e.g. i, angle)"));
            inv.setItem(slot++, createGuiItem(Material.LIME_DYE, "§a§l[MANAGE SUB-ACTIONS]", "§7Sub-actions are defined in the .yml.", "§7You need to edit them manually for now."));
        }
        else if (type == ActionType.RAYCAST) { // [NEW]
            inv.setItem(slot++, createPropItem(Material.ENDER_PEARL, "range", "Range", (String)data.getOrDefault("range","10.0"), "§eEdit Range (Expr)"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "sub-skill", "Sub Skill ID (On Hit)", (String)data.getOrDefault("sub-skill","none"), "§eEdit On-Hit Skill ID"));
            inv.setItem(slot++, createPropItem(Material.TARGET, "target-type", "Target Type", (String)data.getOrDefault("target-type","SINGLE"), "§eEdit (SINGLE/AOE)"));
        }
        else if (type == ActionType.SPAWN_ENTITY) { // [NEW]
            inv.setItem(slot++, createPropItem(Material.EGG, "entity-type", "Entity Type", (String)data.getOrDefault("entity-type","LIGHTNING_BOLT"), "§eEdit Entity Type (e.g. LIGHTNING_BOLT, IRON_GOLEM)"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "skill-id", "On Spawn Skill ID", (String)data.getOrDefault("skill-id","none"), "§eEdit Skill ID (Optional)"));
        }

        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lSAVE CHANGES", "§7Apply changes.", "§8---------------", "§7บันทึกการเปลี่ยนแปลง"));
        inv.setItem(53, createGuiItem(Material.RED_CONCRETE, "§cCancel", "§7Discard.", "§7ยกเลิก"));

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) { if (inv.getItem(i) == null) inv.setItem(i, bg); }
        player.openInventory(inv);
    }

    private ItemStack createPropItem(Material mat, String key, String display, String value, String hint) {
        return createGuiItem(mat, "§e" + display, "§7Value: §f" + value, "§8---------------", hint, "§0Key:" + key);
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
}