package org.rostats.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.rostats.ROStatsPlugin;
import org.rostats.data.PlayerData;
import org.rostats.data.StatManager;

public class PAPIHook extends PlaceholderExpansion {

    private final ROStatsPlugin plugin;

    public PAPIHook(ROStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "rostats"; }
    @Override
    public @NotNull String getAuthor() { return "ServerDev"; }
    @Override
    public @NotNull String getVersion() { return "1.0"; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) return "";
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) return "";

        StatManager stats = plugin.getStatManager();
        PlayerData data = stats.getData(player.getUniqueId());

        return switch (params.toLowerCase()) {
            // Core Levels & Points
            case "baselevel" -> String.valueOf(data.getBaseLevel());
            case "baseexp" -> String.valueOf(data.getBaseExp());
            case "baseexpreq" -> String.valueOf(data.getBaseExpReq());
            case "joblevel" -> String.valueOf(data.getJobLevel());
            case "jobexp" -> String.valueOf(data.getJobExp());
            case "jobexpreq" -> String.valueOf(data.getJobExpReq());
            case "statpoints" -> String.valueOf(data.getStatPoints());
            case "skillpoints" -> String.valueOf(data.getSkillPoints());
            case "resetcount" -> String.valueOf(data.getResetCount());

            // HP/SP Values & Regen
            case "maxhp" -> String.format("%.0f", onlinePlayer.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            case "currenthp" -> String.format("%.0f", onlinePlayer.getHealth());
            case "maxsp" -> String.format("%.0f", data.getMaxSP());
            case "currentsp" -> String.format("%.0f", data.getCurrentSP());
            case "hpregen" -> String.format("%.1f", data.getHPRegen());

            // Core Combat Stats (Derived)
            case "patk" -> String.format("%.0f", stats.getPhysicalAttack(onlinePlayer));
            case "matk" -> String.format("%.0f", stats.getMagicAttack(onlinePlayer));
            case "hit" -> String.valueOf(stats.getHit(onlinePlayer));
            case "flee" -> String.valueOf(stats.getFlee(onlinePlayer));
            case "critchance" -> String.format("%.1f", stats.getCritChance(onlinePlayer));
            case "softdef" -> String.format("%.0f", stats.getSoftDef(onlinePlayer));
            case "softmdef" -> String.format("%.0f", stats.getSoftMDef(onlinePlayer));
            case "power" -> String.format("%.0f", stats.calculatePower(onlinePlayer));

            // Advanced/Percentage Stats (Formatted as %.1f or %.0f as appropriate)
            case "maxhppercent" -> String.format("%.1f", data.getMaxHPPercent());
            case "maxsppercent" -> String.format("%.1f", data.getMaxSPPercent());
            case "patkbonusflat" -> String.format("%.0f", data.getPAtkBonusFlat());
            case "matkbonusflat" -> String.format("%.0f", data.getMAtkBonusFlat());
            case "critres" -> String.format("%.1f", data.getCritRes());
            case "critdmg" -> String.format("%.1f", data.getCritDmgPercent());
            case "critdmgres" -> String.format("%.1f", data.getCritDmgResPercent());
            case "pdmgbonus" -> String.format("%.1f", data.getPDmgBonusPercent());
            case "mdmgbonus" -> String.format("%.1f", data.getMDmgBonusPercent());
            case "pdmgreduce" -> String.format("%.1f", data.getPDmgReductionPercent());
            case "mdmgreduce" -> String.format("%.1f", data.getMDmgReductionPercent());
            case "meleedmg" -> String.format("%.1f", data.getMeleePDmgPercent());
            case "rangedmg" -> String.format("%.1f", data.getRangePDmgPercent());
            case "meleereduce" -> String.format("%.1f", data.getMeleePDReductionPercent());
            case "rangereduce" -> String.format("%.1f", data.getRangePDReductionPercent());
            case "ignorepdefflat" -> String.format("%.0f", data.getIgnorePDefFlat());
            case "ignoremdefflat" -> String.format("%.0f", data.getIgnoreMDefFlat());
            case "ignorepdefperc" -> String.format("%.1f", data.getIgnorePDefPercent());
            case "ignoremdefperc" -> String.format("%.1f", data.getIgnoreMDefPercent());
            case "aspdperc" -> String.format("%.1f", data.getASpdPercent());
            case "mspdperc" -> String.format("%.1f", data.getMSpdPercent());
            case "varctperc" -> String.format("%.1f", data.getVarCTPercent());
            case "varctflat" -> String.format("%.1f", data.getVarCTFlat());
            case "fixedctperc" -> String.format("%.1f", data.getFixedCTPercent());
            case "fixedctflat" -> String.format("%.1f", data.getFixedCTFlat());
            case "healeffect" -> String.format("%.1f", data.getHealingEffectPercent());
            case "healreceive" -> String.format("%.1f", data.getHealingReceivedPercent());
            case "lifestealP" -> String.format("%.1f", data.getLifestealPPercent());
            case "lifestealM" -> String.format("%.1f", data.getLifestealMPercent());
            case "truedmg" -> String.format("%.0f", data.getTrueDamageFlat());
            case "shieldvalue" -> String.format("%.0f", data.getShieldValueFlat());
            case "shieldrate" -> String.format("%.1f", data.getShieldRatePercent());
            case "finaldmg" -> String.format("%.1f", data.getFinalDmgPercent());
            case "finaldmgres" -> String.format("%.1f", data.getFinalDmgResPercent());
            case "finalpdmg" -> String.format("%.1f", data.getFinalPDmgPercent());
            case "finalmdmg" -> String.format("%.1f", data.getFinalMDmgPercent());

            // PVE/PVP RAW Stats
            case "pvebonus" -> String.format("%.0f", data.getPveDmgBonusPercent());
            case "pvpbonus" -> String.format("%.0f", data.getPvpDmgBonusPercent());
            case "pvereduce" -> String.format("%.0f", data.getPveDmgReductionPercent());
            case "pvpreduce" -> String.format("%.0f", data.getPvpDmgReductionPercent());

            // Other utility fields
            case "weaponpatk" -> String.format("%.0f", data.getWeaponPAtk());
            case "weaponmatk" -> String.format("%.0f", data.getWeaponMAtk());
            case "hitbonus" -> String.format("%.0f", data.getHitBonusFlat());
            case "fleebonus" -> String.format("%.0f", data.getFleeBonusFlat());
            case "basemspd" -> String.format("%.2f", data.getBaseMSPD());

            default -> {
                String statName = params.toUpperCase();
                if (data.getStat(statName) > 0) {
                    // Raw stat values (STR, AGI, etc.)
                    yield String.valueOf(data.getStat(statName));
                }
                yield null;
            }
        };
    }
}