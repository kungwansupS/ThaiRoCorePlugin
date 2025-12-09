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

        // Helper for consistent display
        // createPropItem(Material, Key, DisplayName, Value, Hint)

        if (type == ActionType.DAMAGE) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "สูตรคำนวณ (Formula)", (String)data.getOrDefault("formula","ATK"), "§eคลิกเพื่อแก้สูตร (เช่น ATK * 1.5)"));
            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "element", "ธาตุ (Element)", (String)data.getOrDefault("element","NEUTRAL"), "§eคลิกเพื่อแก้ธาตุ (Fire, Water...)"));
            // [NEW] Bypass Defense
            inv.setItem(slot++, createPropItem(Material.SHIELD, "bypass-def", "เจาะเกราะ (True Damage)", data.getOrDefault("bypass-def", false).toString(), "§eคลิกเพื่อเปิด/ปิด (True/False)"));
        }
        else if (type == ActionType.HEAL) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "formula", "ปริมาณการฮีล (Amount)", (String)data.getOrDefault("formula","10"), "§eแก้ตัวเลขหรือสูตร"));
            inv.setItem(slot++, createPropItem(Material.REDSTONE, "is-mana", "ฟื้นฟูมานา? (Is Mana)", data.getOrDefault("is-mana",false).toString(), "§eคลิกเพื่อสลับ (True=SP, False=HP)"));
            inv.setItem(slot++, createPropItem(Material.PLAYER_HEAD, "self-only", "เฉพาะตัวเอง? (Self Only)", data.getOrDefault("self-only", true).toString(), "§eคลิกเพื่อสลับ (True=ตนเอง, False=เป้าหมาย)"));
        }
        else if (type == ActionType.APPLY_EFFECT) {
            inv.setItem(slot++, createPropItem(Material.NAME_TAG, "effect-id", "ไอดีเอฟเฟกต์ (ID)", (String)data.getOrDefault("effect-id","unknown"), "§eตั้งชื่ออ้างอิง (ห้ามซ้ำ)"));
            inv.setItem(slot++, createPropItem(Material.POTION, "effect-type", "ประเภท (Type)", (String)data.getOrDefault("effect-type","STAT_MODIFIER"), "§eแก้ประเภท (STAT_MODIFIER, STUN, etc)"));
            inv.setItem(slot++, createPropItem(Material.EXPERIENCE_BOTTLE, "level", "เลเวล (Level)", data.getOrDefault("level",1).toString(), "§eแก้เลเวลบัฟ"));
            inv.setItem(slot++, createPropItem(Material.IRON_SWORD, "power", "ความแรง (Power)", data.getOrDefault("power",0.0).toString(), "§eแก้ค่าพลัง (เช่น เพิ่ม STR 10)"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "duration", "ระยะเวลา (Ticks)", data.getOrDefault("duration",100).toString(), "§e(20 Ticks = 1 วินาที)"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "chance", "โอกาสติด (Chance)", data.getOrDefault("chance",1.0).toString(), "§e(1.0 = 100%, 0.5 = 50%)"));
            inv.setItem(slot++, createPropItem(Material.ANVIL, "stat-key", "ค่าสถานะ (Stat Key)", (String)data.getOrDefault("stat-key","None"), "§eระบุค่าที่จะเพิ่ม (เช่น STR, ATK)"));
        }
        else if (type == ActionType.SOUND) {
            inv.setItem(slot++, createPropItem(Material.NOTE_BLOCK, "sound", "ชื่อเสียง (Sound Name)", (String)data.getOrDefault("sound","ENTITY_EXPERIENCE_ORB_PICKUP"), "§eแก้ชื่อเสียง Minecraft"));
            inv.setItem(slot++, createPropItem(Material.REPEATER, "volume", "ความดัง (Volume)", data.getOrDefault("volume",1.0).toString(), "§eแก้ความดัง"));
            inv.setItem(slot++, createPropItem(Material.COMPARATOR, "pitch", "ความสูงเสียง (Pitch)", data.getOrDefault("pitch",1.0).toString(), "§eแก้โทนเสียง (0.5 - 2.0)"));
        }
        else if (type == ActionType.PARTICLE) {
            inv.setItem(slot++, createPropItem(Material.BLAZE_POWDER, "particle", "ชื่อ Particle", (String)data.getOrDefault("particle","VILLAGER_HAPPY"), "§eแก้ชื่อ Particle"));
            inv.setItem(slot++, createPropItem(Material.GLOWSTONE_DUST, "count", "จำนวน (Count)", data.getOrDefault("count","5").toString(), "§eแก้จำนวนเม็ด"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "ความเร็ว (Speed)", data.getOrDefault("speed","0.1").toString(), "§eแก้ความเร็วการกระจาย"));
            inv.setItem(slot++, createPropItem(Material.COMPASS, "shape", "รูปทรง (Shape)", (String)data.getOrDefault("shape", "POINT"), "§e(POINT, CIRCLE, SPHERE)"));
            inv.setItem(slot++, createPropItem(Material.ENDER_PEARL, "radius", "รัศมี (Radius)", data.getOrDefault("radius","0.5").toString(), "§eแก้ขนาดวง/ระยะ"));
            inv.setItem(slot++, createPropItem(Material.ANVIL, "points", "จุด (Points)", data.getOrDefault("points","20").toString(), "§eความละเอียดของรูปทรง"));
        }
        else if (type == ActionType.POTION) {
            inv.setItem(slot++, createPropItem(Material.POTION, "potion", "ชื่อ Potion", (String)data.getOrDefault("potion","SPEED"), "§eแก้ชื่อ Potion (เช่น SLOW)"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "duration", "ระยะเวลา (Ticks)", data.getOrDefault("duration",60).toString(), "§e(20 Ticks = 1 วินาที)"));
            inv.setItem(slot++, createPropItem(Material.GLOWSTONE, "amplifier", "ระดับความแรง (Amp)", data.getOrDefault("amplifier",0).toString(), "§e(0 = Lv.1, 1 = Lv.2)"));
            inv.setItem(slot++, createPropItem(Material.PLAYER_HEAD, "self-only", "เฉพาะตัวเอง? (Self)", data.getOrDefault("self-only", true).toString(), "§eToggle"));
        }
        else if (type == ActionType.TELEPORT) {
            inv.setItem(slot++, createPropItem(Material.ENDER_PEARL, "range", "ระยะทาง (Range)", data.getOrDefault("range",5.0).toString(), "§eแก้ระยะวาร์ป (เมตร)"));
            inv.setItem(slot++, createPropItem(Material.ENDER_EYE, "to-target", "ไปหาเป้าหมาย? (To Target)", data.getOrDefault("to-target",false).toString(), "§eTrue = วาร์ปไปหาศัตรู"));
        }
        else if (type == ActionType.PROJECTILE) {
            inv.setItem(slot++, createPropItem(Material.ARROW, "projectile", "ชนิดกระสุน (Type)", (String)data.getOrDefault("projectile","ARROW"), "§e(ARROW, SNOWBALL, FIREBALL)"));
            inv.setItem(slot++, createPropItem(Material.FEATHER, "speed", "ความเร็ว (Speed)", data.getOrDefault("speed",1.0).toString(), "§eความเร็วกระสุน"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "on-hit", "สกิลเมื่อชน (On-Hit)", (String)data.getOrDefault("on-hit","none"), "§eใส่ชื่อ ID สกิลที่จะร่ายเมื่อชน"));
        }
        else if (type == ActionType.AREA_EFFECT) {
            inv.setItem(slot++, createPropItem(Material.BEACON, "radius", "รัศมี (Radius)", data.getOrDefault("radius", 5.0).toString(), "§eแก้รัศมีวง (เมตร)"));
            inv.setItem(slot++, createPropItem(Material.ZOMBIE_HEAD, "target-type", "ประเภทเป้าหมาย", (String) data.getOrDefault("target-type", "ENEMY"), "§e(ENEMY, ALLY, ALL)"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "sub-skill", "สกิลลูกโซ่ (Sub Skill)", (String) data.getOrDefault("sub-skill", "none"), "§eID สกิลที่จะร่ายใส่ทุกคนในวง"));
            inv.setItem(slot++, createPropItem(Material.SKELETON_SKULL, "max-targets", "จำนวนเป้าหมายสูงสุด", data.getOrDefault("max-targets", 10).toString(), "§eจำกัดจำนวนคนโดน"));
        }
        else if (type == ActionType.COMMAND) {
            inv.setItem(slot++, createPropItem(Material.PAPER, "command", "คำสั่ง (Command)", (String)data.getOrDefault("command", "say Hi %player%"), "§eพิมพ์คำสั่ง (ไม่ต้องใส่ /)"));
            inv.setItem(slot++, createPropItem(Material.REDSTONE, "as-console", "ใช้โดย Console?", data.getOrDefault("as-console", false).toString(), "§eTrue = ใช้โดยระบบ (OP)"));
        }
        else if (type == ActionType.VELOCITY) {
            inv.setItem(slot++, createPropItem(Material.DIAMOND, "x", "แรง X (ซ้ายขวา)", data.getOrDefault("x",0.0).toString(), "§eความแรงแกน X"));
            inv.setItem(slot++, createPropItem(Material.DIAMOND, "y", "แรง Y (ขึ้นลง)", data.getOrDefault("y",0.0).toString(), "§eความแรงแกน Y (กระโดด)"));
            inv.setItem(slot++, createPropItem(Material.DIAMOND, "z", "แรง Z (หน้าหลัง)", data.getOrDefault("z",0.0).toString(), "§eความแรงแกน Z"));
            inv.setItem(slot++, createPropItem(Material.REDSTONE, "add", "เพิ่มแรงเดิม? (Additive)", data.getOrDefault("add",true).toString(), "§eTrue = บวกเพิ่ม, False = ตั้งค่าใหม่"));
        }
        else if (type == ActionType.LOOP) {
            inv.setItem(slot++, createPropItem(Material.CLOCK, "start", "เริ่มที่ (Start)", (String)data.getOrDefault("start", "0"), "§eค่าเริ่มต้นลูป"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "end", "จบที่ (End)", (String)data.getOrDefault("end", "10"), "§eค่าจบ"));
            inv.setItem(slot++, createPropItem(Material.CLOCK, "step", "ขยับทีละ (Step)", (String)data.getOrDefault("step", "1"), "§eเพิ่มค่าทีละเท่าไหร่"));
            inv.setItem(slot++, createPropItem(Material.NAME_TAG, "var", "ชื่อตัวแปร (Var)", (String)data.getOrDefault("var", "i"), "§eชื่อตัวแปร (เช่น i)"));
            inv.setItem(slot++, createGuiItem(Material.LIME_DYE, "§a§l[จัดการ Action ย่อย]", "§7(ต้องแก้ในไฟล์ .yml เท่านั้นสำหรับ Loop)", "§7ยังไม่รองรับการแก้ GUI ซ้อน"));
        }
        else if (type == ActionType.RAYCAST) {
            inv.setItem(slot++, createPropItem(Material.ENDER_PEARL, "range", "ระยะทาง (Range)", (String)data.getOrDefault("range","10.0"), "§eระยะเส้น Raycast"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "sub-skill", "สกิลเมื่อชน (On Hit)", (String)data.getOrDefault("sub-skill","none"), "§eID สกิลที่จะร่ายเมื่อชน"));
            inv.setItem(slot++, createPropItem(Material.TARGET, "target-type", "เป้าหมาย (Target)", (String)data.getOrDefault("target-type","SINGLE"), "§e(SINGLE / AOE)"));
        }
        else if (type == ActionType.SPAWN_ENTITY) {
            inv.setItem(slot++, createPropItem(Material.EGG, "entity-type", "ชื่อ Entity", (String)data.getOrDefault("entity-type","LIGHTNING_BOLT"), "§e(เช่น ZOMBIE, IRON_GOLEM)"));
            inv.setItem(slot++, createPropItem(Material.WRITABLE_BOOK, "skill-id", "สกิลเมื่อเกิด (On Spawn)", (String)data.getOrDefault("skill-id","none"), "§eID สกิลที่ตัวที่เกิดจะใช้"));
        }

        inv.setItem(49, createGuiItem(Material.EMERALD_BLOCK, "§a§lบันทึก (SAVE)", "§7บันทึกการเปลี่ยนแปลง", "§8---------------"));
        inv.setItem(53, createGuiItem(Material.RED_CONCRETE, "§cยกเลิก (Cancel)", "§7ไม่บันทึก", "§8---------------"));

        ItemStack bg = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) { if (inv.getItem(i) == null) inv.setItem(i, bg); }
        player.openInventory(inv);
    }

    private ItemStack createPropItem(Material mat, String key, String display, String value, String hint) {
        return createGuiItem(mat, "§e" + display, "§7ค่าปัจจุบัน: §f" + value, "§8---------------", hint, "§0Key:" + key);
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