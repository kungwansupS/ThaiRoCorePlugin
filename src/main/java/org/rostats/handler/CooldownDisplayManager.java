package org.rostats.handler;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.rostats.ThaiRoCorePlugin;
import org.rostats.data.PlayerData;
import org.rostats.engine.skill.SkillData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CooldownDisplayManager - แสดงเวลา Cooldown ของสกิลให้ผู้เล่นเห็นผ่าน Action Bar
 *
 * Features:
 * - แสดง Global Cooldown (GCD)
 * - แสดง Skill Cooldown ของสกิลล่าสุดที่ใช้
 * - อัพเดททุก 5 ticks (0.25 วินาที) เพื่อความลื่นไหล
 */
public class CooldownDisplayManager {

    private final ThaiRoCorePlugin plugin;
    private final Map<UUID, String> lastUsedSkill = new HashMap<>();
    private BukkitTask updateTask;

    public CooldownDisplayManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
        startDisplayTask();
    }

    /**
     * เริ่มต้น Task แสดง Cooldown ทุก 5 ticks (0.25 วินาที)
     */
    private void startDisplayTask() {
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 0L, 5L);
    }

    /**
     * หยุด Task (เรียกเมื่อปิด plugin)
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    /**
     * บันทึกสกิลล่าสุดที่ผู้เล่นใช้
     */
    public void setLastUsedSkill(Player player, String skillId) {
        lastUsedSkill.put(player.getUniqueId(), skillId);
    }

    /**
     * อัพเดทการแสดงผลสำหรับผู้เล่นทุกคน
     */
    private void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    /**
     * อัพเดทการแสดงผลสำหรับผู้เล่นคนเดียว
     */
    private void updatePlayer(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        if (data == null) return;

        StringBuilder message = new StringBuilder();
        boolean hasAnyCooldown = false;

        // 1. แสดง Global Cooldown (GCD)
        if (data.isOnGlobalCooldown()) {
            double gcdRemaining = data.getRemainingGlobalCooldown();
            message.append(String.format("§e⏱ GCD: §f%.1fs", gcdRemaining));
            hasAnyCooldown = true;
        }

        // 2. แสดง Skill Cooldown ของสกิลล่าสุด
        String lastSkillId = lastUsedSkill.get(player.getUniqueId());
        if (lastSkillId != null) {
            SkillData skill = plugin.getSkillManager().getSkill(lastSkillId);
            if (skill != null) {
                // คำนวณ cooldown ที่เหลือ
                double baseCooldown = skill.getCooldown(1); // ใช้ level 1 เป็นค่า default
                double finalCooldown = data.getFinalSkillCooldown(baseCooldown);

                if (data.isSkillOnCooldown(lastSkillId, finalCooldown)) {
                    double skillRemaining = data.getRemainingSkillCooldown(lastSkillId, finalCooldown);

                    if (hasAnyCooldown) {
                        message.append(" §8| ");
                    }

                    message.append(String.format("§6⚔ %s: §f%.1fs",
                            skill.getDisplayName(), skillRemaining));
                    hasAnyCooldown = true;
                } else {
                    // ถ้าสกิลพร้อมใช้แล้ว ลบออกจาก tracking
                    if (!data.isOnGlobalCooldown()) {
                        lastUsedSkill.remove(player.getUniqueId());
                    }
                }
            }
        }

        // 3. แสดงข้อความ หรือซ่อนถ้าไม่มี cooldown
        if (hasAnyCooldown) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(message.toString()));
        } else {
            // ส่งข้อความว่างเพื่อล้าง action bar
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(""));
        }
    }

    /**
     * แสดงข้อความ Cooldown แบบละเอียดใน chat (สำหรับ debug)
     */
    public void showDetailedCooldown(Player player) {
        PlayerData data = plugin.getStatManager().getData(player.getUniqueId());
        if (data == null) return;

        player.sendMessage("§7§m-------------------§r §6Cooldown Status §7§m-------------------");

        // Global Cooldown
        if (data.isOnGlobalCooldown()) {
            double gcd = data.getRemainingGlobalCooldown();
            player.sendMessage(String.format("§e⏱ Global Cooldown: §f%.2f seconds", gcd));
        } else {
            player.sendMessage("§a✓ Global Cooldown: Ready");
        }

        // All Skill Cooldowns
        player.sendMessage("§7");
        player.sendMessage("§6Active Skill Cooldowns:");

        boolean hasAnySkillCD = false;
        for (String skillId : plugin.getSkillManager().getSkills().keySet()) {
            SkillData skill = plugin.getSkillManager().getSkill(skillId);
            double baseCooldown = skill.getCooldown(1);
            double finalCooldown = data.getFinalSkillCooldown(baseCooldown);

            if (data.isSkillOnCooldown(skillId, finalCooldown)) {
                double remaining = data.getRemainingSkillCooldown(skillId, finalCooldown);
                player.sendMessage(String.format("  §7• §f%s: §e%.2fs",
                        skill.getDisplayName(), remaining));
                hasAnySkillCD = true;
            }
        }

        if (!hasAnySkillCD) {
            player.sendMessage("  §a✓ All skills ready");
        }

        player.sendMessage("§7§m------------------------------------------------");
    }

    /**
     * แสดง cooldown ในรูปแบบ progress bar
     */
    public String getCooldownBar(double current, double max, int length) {
        if (max <= 0) return "§a" + "█".repeat(length);

        double progress = 1.0 - (current / max); // กลับด้านเพราะเราต้องการแสดง "ว่าง" เมื่อ cooldown
        int filled = (int) Math.round(progress * length);

        StringBuilder bar = new StringBuilder();
        bar.append("§a"); // สีเขียว = พร้อมใช้
        bar.append("█".repeat(Math.max(0, filled)));
        bar.append("§c"); // สีแดง = cooldown
        bar.append("█".repeat(Math.max(0, length - filled)));

        return bar.toString();
    }

    /**
     * แสดง cooldown ทั้งหมดในรูปแบบ scoreboard (อนาคต)
     */
    public void updateScoreboard(Player player) {
        // TODO: Implement scoreboard display
        // สามารถใช้ Bukkit Scoreboard API เพื่อแสดง cooldown ในรูปแบบ sidebar
    }
}