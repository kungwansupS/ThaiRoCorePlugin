package org.rostats.data;

import org.bukkit.entity.Player;
import org.rostats.ROStatsPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class StatManager {
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final ROStatsPlugin plugin;

    public StatManager(ROStatsPlugin plugin) {
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

    // Helper method to get the total cost of pending points for one stat
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

    // Helper method to get the total cost of ALL pending points
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

    // === FORMULAS (SECTION A: DERIVED STATS) ===

    // Formula A.3 (P.ATK) - Corrected
    public double getPhysicalAttack(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int str = data.getStat("STR") + data.getPendingStat("STR");
        int dex = data.getStat("DEX") + data.getPendingStat("DEX");
        int luk = data.getStat("LUK") + data.getPendingStat("LUK");

        // Physical Attack = STR × 1 + DEX × 0.2 + LUK × 0.2 + WeaponATK + PhysicalBonus
        return (str * 1.0) + (dex * 0.2) + (luk * 0.2) + data.getWeaponPAtk() + data.getPAtkBonusFlat();
    }

    // Formula A.4 (M.ATK) - Corrected
    public double getMagicAttack(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int intel = data.getStat("INT") + data.getPendingStat("INT");
        int luk = data.getStat("LUK") + data.getPendingStat("LUK");

        // Magic Attack = INT × 1.5 + LUK × 0.3 + MagicBonus
        return (intel * 1.5) + (luk * 0.3) + data.getWeaponMAtk() + data.getMAtkBonusFlat();
    }

    // Setting to 0.0 as it's no longer derived from STR based on new formulas (Used in CombatHandler)
    public double getPhysicalDamageBonus(Player player) {
        return 0.0;
    }

    // Setting to 0.0 as it's no longer derived from INT based on new formulas (Used in CombatHandler)
    public double getMagicDamageBonus(Player player) {
        return 0.0;
    }

    // Formula A.5 (HIT) - Corrected
    public int getHit(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int dex = getStat(player.getUniqueId(), "DEX") + data.getPendingStat("DEX");

        // Hit = DEX × 1 + BaseHit (BaseHit assumed to be data.getHitBonusFlat())
        return (int) (dex + data.getHitBonusFlat());
    }

    // Formula A.6 (FLEE) - Corrected
    public int getFlee(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int agi = getStat(player.getUniqueId(), "AGI") + data.getPendingStat("AGI");

        // Flee = AGI × 1 + BaseFlee (BaseFlee assumed to be data.getFleeBonusFlat())
        return (int) (agi + data.getFleeBonusFlat());
    }

    // ASPD - New logic for consistency with AttributeHandler and GearBonus%
    // Returns the total multiplier to be applied (e.g. 1.20 for 120%)
    public double getAspdBonus(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int agi = getStat(player.getUniqueId(), "AGI");
        int dex = getStat(player.getUniqueId(), "DEX");

        // ASPD Stat Bonus: AGI * 1% (0.01) + DEX * 0.2% (0.002) - based on original AttributeHandler coefficients
        double statBonus = (agi * 0.01) + (dex * 0.002);

        // Value is 1.0 (base) + statBonus + GearBonus
        return 1.0 + statBonus + (data.getASpdPercent() / 100.0);
    }

    // Helper Methods (for display/combat compatibility)

    // SoftPDEF - Corrected
    public double getSoftDef(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int vit = getStat(player.getUniqueId(), "VIT");
        int agi = getStat(player.getUniqueId(), "AGI");
        // SoftPDEF = VIT × 0.5 + AGI × 0.2
        return (vit * 0.5) + (agi * 0.2); // Proxy for BasePDef
    }

    // SoftMDEF - Corrected
    public double getSoftMDef(Player player) {
        PlayerData data = getData(player.getUniqueId());
        int intel = getStat(player.getUniqueId(), "INT");
        int vit = getStat(player.getUniqueId(), "VIT");
        // SoftMDEF = INT × 1 + VIT × 0.2
        return (intel * 1.0) + (vit * 0.2); // Proxy for BaseMDef
    }

    // CritChance - Adjusted to return raw value (LUK * 0.3)
    public double getCritChance(Player player) {
        int luk = getStat(player.getUniqueId(), "LUK");
        // CriticalChance = max(0, LUK × 0.3 - TargetCritRes)
        return luk * 0.3; // Raw CRIT value
    }

    public double getPhysicalPenetration(Player player) {
        int luk = getStat(player.getUniqueId(), "LUK");
        return (luk * 0.1) / 100.0; // Proxy P.PEN% (Keeping existing LUK derived stat)
    }

    // Keeping this method as it's not explicitly contradicted by a new formula, and may be used elsewhere.
    public double getCriticalDamage(Player player) {
        int str = getStat(player.getUniqueId(), "STR");
        return 1.4 + ((str * 0.2) / 100.0); // Old formula kept as proxy
    }

    public double calculatePower(Player player) {
        PlayerData data = getData(player.getUniqueId());
        double str = data.getStat("STR");
        double intel = data.getStat("INT");
        double agi = data.getStat("AGI");
        double vit = data.getStat("VIT");
        double dex = data.getStat("DEX");
        double luk = data.getStat("LUK");
        int baseLevel = data.getBaseLevel();

        double coreStatPower = (str + intel) * 5.0;
        double secondaryStatPower = (agi + vit + dex + luk) * 2.0;
        double levelPower = baseLevel * 10.0;

        return coreStatPower + secondaryStatPower + levelPower;
    }
}