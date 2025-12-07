package org.rostats.data;

import org.bukkit.entity.Player;
import org.rostats.ThaiRoCorePlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class StatManager {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final ThaiRoCorePlugin plugin;

    public StatManager(ThaiRoCorePlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerData getData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> new PlayerData(plugin));
    }

    public int getStat(UUID uuid, String statName) {
        return getData(uuid).getStat(statName);
    }

    public void setStat(UUID uuid, String statName, int value) {
        getData(uuid).setStat(statName, value);
    }

    public int getPendingCost(PlayerData data, String statName) {
        int pendingCount = data.getPendingStat(statName);
        if (pendingCount == 0) return 0;

        int currentVal = data.getStat(statName);
        int totalCost = 0;

        for (int i = 0; i < pendingCount; i++) {
            totalCost += getStatCost(currentVal + i);
        }
        return totalCost;
    }

    public int getTotalPendingCost(PlayerData data) {
        int totalCost = 0;
        for (String stat : data.getStatKeys()) {
            totalCost += getPendingCost(data, stat);
        }
        return totalCost;
    }

    public boolean upgradeStat(Player player, String statName) {
        PlayerData data = getData(player.getUniqueId());
        int pendingCount = data.getPendingStat(statName);
        int currentVal = data.getStat(statName);

        int costOfNextPoint = getStatCost(currentVal + pendingCount);
        int totalPendingCost = getTotalPendingCost(data);

        if (data.getStatPoints() < (totalPendingCost + costOfNextPoint)) {
            return false;
        }

        data.setPendingStat(statName, pendingCount + 1);
        return true;
    }

    public boolean downgradeStat(Player player, String statName) {
        PlayerData data = getData(player.getUniqueId());
        int pendingCount = data.getPendingStat(statName);

        if (pendingCount <= 0) {
            return false;
        }

        data.setPendingStat(statName, pendingCount - 1);
        return true;
    }

    public void allocateStats(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int totalCost = getTotalPendingCost(data);

        if (data.getStatPoints() >= totalCost) {
            for (String stat : data.getStatKeys()) {
                int pendingCount = data.getPendingStat(stat);
                if (pendingCount > 0) {
                    data.setStat(stat, data.getStat(stat) + pendingCount);
                }
            }
            data.setStatPoints(data.getStatPoints() - totalCost);
            data.clearAllPendingStats();
            plugin.getAttributeHandler().updatePlayerStats(player);
            player.sendMessage("§a[Allocate] Stats applied! Cost: " + totalCost);
        } else {
            player.sendMessage("§c[Allocate] Not enough points! Required: " + totalCost);
        }
    }

    public int getStatCost(int currentVal) {
        int costBase = plugin.getConfig().getInt("stat-cost.base", 2);
        int costDivisor = plugin.getConfig().getInt("stat-cost.divisor", 10);
        int costStartLevel = plugin.getConfig().getInt("stat-cost.cost-start-level", 2);

        if (currentVal < costStartLevel) return costBase;
        return ((currentVal - 1) / costDivisor) + costBase;
    }

    // --- FIX: Helper Method to get Base + Pending + Gear Bonus ---
    private int getTotalStat(PlayerData data, String statKey) {
        int base = data.getStat(statKey);
        int pending = data.getPendingStat(statKey);
        int gear = switch (statKey.toUpperCase()) {
            case "STR" -> data.getSTRBonusGear();
            case "AGI" -> data.getAGIBonusGear();
            case "VIT" -> data.getVITBonusGear();
            case "INT" -> data.getINTBonusGear();
            case "DEX" -> data.getDEXBonusGear();
            case "LUK" -> data.getLUKBonusGear();
            default -> 0;
        };
        return base + pending + gear;
    }

    // --- UPDATED CALCULATIONS USING TOTAL STATS ---

    public double getPhysicalAttack(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int str = getTotalStat(data, "STR");
        int dex = getTotalStat(data, "DEX");
        int luk = getTotalStat(data, "LUK");
        return (str * 1.0) + (dex * 0.2) + (luk * 0.2) + data.getWeaponPAtk() + data.getPAtkBonusFlat();
    }

    public double getMagicAttack(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int intel = getTotalStat(data, "INT");
        int luk = getTotalStat(data, "LUK");
        return (intel * 1.5) + (luk * 0.3) + data.getWeaponMAtk() + data.getMAtkBonusFlat();
    }

    public int getHit(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int dex = getTotalStat(data, "DEX");
        int luk = getTotalStat(data, "LUK");
        return (int) (dex + luk + data.getBaseLevel() + data.getHitBonusFlat());
    }

    public int getFlee(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int agi = getTotalStat(data, "AGI");
        int luk = getTotalStat(data, "LUK");
        return (int) (agi + (luk * 0.2) + data.getBaseLevel() + data.getFleeBonusFlat());
    }

    public double getAspdBonus(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int agi = getTotalStat(data, "AGI");
        int dex = getTotalStat(data, "DEX");
        double statBonus = (agi * 0.02) + (dex * 0.005);
        return 1.0 + statBonus + (data.getASpdPercent() / 100.0);
    }

    public double getSoftDef(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int vit = getTotalStat(data, "VIT");
        int agi = getTotalStat(data, "AGI");
        return (vit * 0.5) + (agi * 0.1);
    }

    public double getSoftMDef(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int intel = getTotalStat(data, "INT");
        int vit = getTotalStat(data, "VIT");
        return (intel * 1.0) + (vit * 0.5);
    }

    public double getCritChance(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int luk = getTotalStat(data, "LUK");
        return luk * 0.3;
    }

    public double calculatePower(Player player) {
        PlayerData data = getData(player.getUniqueId());
        double str = getTotalStat(data, "STR");
        double intel = getTotalStat(data, "INT");
        double agi = getTotalStat(data, "AGI");
        double vit = getTotalStat(data, "VIT");
        double dex = getTotalStat(data, "DEX");
        double luk = getTotalStat(data, "LUK");
        int baseLevel = data.getBaseLevel();

        double coreStatPower = (str + intel) * 5.0;
        double secondaryStatPower = (agi + vit + dex + luk) * 2.0;
        double levelPower = baseLevel * 10.0;

        return coreStatPower + secondaryStatPower + levelPower;
    }
}