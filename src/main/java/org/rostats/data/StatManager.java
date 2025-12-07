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
        // Fix: Pass plugin instance to PlayerData constructor
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

    public double getPhysicalAttack(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int str = data.getStat("STR") + data.getPendingStat("STR") + data.getSTRBonusGear();
        int dex = data.getStat("DEX") + data.getPendingStat("DEX") + data.getDEXBonusGear();
        int luk = data.getStat("LUK") + data.getPendingStat("LUK") + data.getLUKBonusGear();
        return (str * 1.0) + (dex * 0.2) + (luk * 0.2) + data.getWeaponPAtk() + data.getPAtkBonusFlat();
    }

    public double getMagicAttack(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int intel = data.getStat("INT") + data.getPendingStat("INT") + data.getINTBonusGear();
        int luk = data.getStat("LUK") + data.getPendingStat("LUK") + data.getLUKBonusGear();
        return (intel * 1.5) + (luk * 0.3) + data.getWeaponMAtk() + data.getMAtkBonusFlat();
    }

    public int getHit(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int dex = data.getStat("DEX") + data.getPendingStat("DEX") + data.getDEXBonusGear();
        return (int) (dex + data.getHitBonusFlat());
    }

    public int getFlee(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int agi = data.getStat("AGI") + data.getPendingStat("AGI") + data.getAGIBonusGear();
        return (int) (agi + data.getFleeBonusFlat());
    }

    public double getAspdBonus(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int agi = data.getStat("AGI") + data.getPendingStat("AGI") + data.getAGIBonusGear();
        int dex = data.getStat("DEX") + data.getPendingStat("DEX") + data.getDEXBonusGear();
        double statBonus = (agi * 0.01) + (dex * 0.002);
        return 1.0 + statBonus + (data.getASpdPercent() / 100.0);
    }

    public double getSoftDef(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int vit = data.getStat("VIT") + data.getVITBonusGear();
        int agi = data.getStat("AGI") + data.getAGIBonusGear();
        return (vit * 0.5) + (agi * 0.2);
    }

    public double getSoftMDef(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int intel = data.getStat("INT") + data.getINTBonusGear();
        int vit = data.getStat("VIT") + data.getVITBonusGear();
        return (intel * 1.0) + (vit * 0.2);
    }

    public double getCritChance(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int luk = data.getStat("LUK") + data.getPendingStat("LUK") + data.getLUKBonusGear();
        return luk * 0.3;
    }

    public double calculatePower(Player player) {
        PlayerData data = getData(player.getUniqueId());
        double str = data.getStat("STR") + data.getSTRBonusGear();
        double intel = data.getStat("INT") + data.getINTBonusGear();
        double agi = data.getStat("AGI") + data.getAGIBonusGear();
        double vit = data.getStat("VIT") + data.getVITBonusGear();
        double dex = data.getStat("DEX") + data.getDEXBonusGear();
        double luk = data.getStat("LUK") + data.getLUKBonusGear();
        int baseLevel = data.getBaseLevel();

        double coreStatPower = (str + intel) * 5.0;
        double secondaryStatPower = (agi + vit + dex + luk) * 2.0;
        double levelPower = baseLevel * 10.0;

        return coreStatPower + secondaryStatPower + levelPower;
    }
}