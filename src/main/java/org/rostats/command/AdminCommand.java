package org.rostats.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.rostats.ROStatsPlugin;
import org.rostats.data.PlayerData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final ROStatsPlugin plugin;

    // Define all top-level sub-commands
    private static final List<String> MAIN_COMMANDS = Arrays.asList(
            "check", "reset", "set", "add", "save"
    );

    // Define all reset sub-sub-commands
    private static final List<String> RESET_COMMANDS = Arrays.asList(
            "blevel", "jlevel", "stat"
    );

    // Define the types that can be SET or ADDED
    private static final List<String> SET_ADD_TYPES = Arrays.asList(
            "blevel", "jlevel", "points", "bexp", "jexp", "stat"
    );

    // Define stat keys
    private static final List<String> STAT_KEYS = Arrays.asList(
            "STR", "AGI", "VIT", "INT", "DEX", "LUK"
    );


    public AdminCommand(ROStatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("rostats.admin")) {
            sender.sendMessage("§cNo Permission.");
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. Handle Global Commands (save)
        if (sub.equals("save")) {
            for (Player p : Bukkit.getOnlinePlayers()) plugin.getDataManager().savePlayerData(p);
            sender.sendMessage("§aSaved data.");
            return true;
        }

        // 2. All other commands require a target player (args[1])
        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cTarget offline.");
            return true;
        }

        PlayerData data = plugin.getStatManager().getData(target.getUniqueId());

        switch (sub) {
            case "check": // /roadmin check <player>
                sender.sendMessage("§6--- " + target.getName() + " ---");
                sender.sendMessage("§eBase Lv: " + data.getBaseLevel() + " (" + data.getBaseExp() + "/" + data.getBaseExpReq() + ")");
                sender.sendMessage("§eJob Lv: " + data.getJobLevel() + " (" + data.getJobExp() + "/" + data.getJobExpReq() + ")");
                sender.sendMessage("§eStat Points: " + data.getStatPoints() + " | Skill Points: " + data.getSkillPoints());
                break;

            case "add": // /roadmin add (bexp|jexp|points|stat) <player> <value> [stat_key]
                if (args.length < 4) return true;
                String addType = args[2].toLowerCase();

                try {
                    if (addType.equals("bexp")) {
                        long longVal = Long.parseLong(args[3]);
                        data.addBaseExp(longVal, target.getUniqueId());
                        sender.sendMessage("§aAdded " + longVal + " Base EXP.");
                    } else if (addType.equals("jexp")) {
                        long longVal = Long.parseLong(args[3]);
                        data.addJobExp(longVal, target.getUniqueId());
                        sender.sendMessage("§aAdded " + longVal + " Job EXP.");
                    } else if (addType.equals("points")) {
                        int intVal = Integer.parseInt(args[3]);
                        data.setStatPoints(data.getStatPoints() + intVal);
                        sender.sendMessage("§aAdded " + intVal + " Stat Points.");
                    } else if (addType.equals("stat")) { // /roadmin add stat <player> <STAT> <value>
                        if (args.length < 5) return true;
                        String statKey = args[3].toUpperCase();
                        int addVal = Integer.parseInt(args[4]);

                        if (STAT_KEYS.contains(statKey)) {
                            // Directly modify stat value
                            plugin.getStatManager().setStat(target.getUniqueId(), statKey, data.getStat(statKey) + addVal);
                            sender.sendMessage("§aAdded " + addVal + " to " + statKey + ".");
                        } else {
                            sender.sendMessage("§cInvalid Stat Key: " + statKey);
                            return true;
                        }
                    } else {
                        return true; // Invalid addType
                    }
                    update(target);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid value!");
                }
                break;

            case "set": // /roadmin set (blevel|jlevel|points|stat) <player> <value> [stat_key]
                if (args.length < 4) return true;
                String setType = args[2].toLowerCase();

                try {
                    int val = Integer.parseInt(args[3]);

                    if (setType.equals("blevel")) data.setBaseLevel(val);
                    else if (setType.equals("jlevel")) data.setJobLevel(val);
                    else if (setType.equals("points")) data.setStatPoints(val);
                    else if (setType.equals("stat")) { // /roadmin set stat <player> <STAT> <value>
                        if (args.length < 5) return true;
                        String statKey = args[3].toUpperCase();
                        int setVal = Integer.parseInt(args[4]);

                        if (STAT_KEYS.contains(statKey)) {
                            plugin.getStatManager().setStat(target.getUniqueId(), statKey, setVal);
                        } else {
                            sender.sendMessage("§cInvalid Stat Key: " + statKey);
                            return true;
                        }
                    } else {
                        return true; // Invalid setType
                    }

                    sender.sendMessage("§aValue set.");
                    update(target);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cInvalid value!");
                }
                break;

            case "reset": // /roadmin reset (blevel|jlevel|stat) <player>
                if (args.length < 3) return true;
                String resetType = args[2].toLowerCase();

                if (resetType.equals("blevel")) {
                    data.setBaseLevel(1);
                    data.setBaseExp(0);
                    data.setStatPoints(0);
                    sender.sendMessage("§aBase Level reset to 1. Stat points cleared.");
                    update(target);
                } else if (resetType.equals("jlevel")) {
                    data.setJobLevel(1);
                    data.setJobExp(0);
                    data.setSkillPoints(0);
                    sender.sendMessage("§aJob Level reset to 1. Skill points cleared.");
                    update(target);
                } else if (resetType.equals("stat")) { // /roadmin reset stat <player>
                    data.resetStats(); // Resets stats to 1 and recalculates points
                    sender.sendMessage("§aStats reset to 1. Points recalculated.");
                    update(target);
                } else {
                    return true; // Invalid resetType
                }
                break;
        }
        return true;
    }

    private void update(Player p) {
        plugin.getAttributeHandler().updatePlayerStats(p);
        plugin.getManaManager().updateBar(p);
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§6--- ROAdmin Help ---");
        s.sendMessage("§c/roadmin save");
        s.sendMessage("§c/roadmin check <player>");
        s.sendMessage("§c/roadmin reset (blevel|jlevel|stat) <player>");
        s.sendMessage("§c/roadmin set (blevel|jlevel|points) <player> <val>");
        s.sendMessage("§c/roadmin set stat <player> <STAT> <val>");
        s.sendMessage("§c/roadmin add (bexp|jexp|points) <player> <val>");
        s.sendMessage("§c/roadmin add stat <player> <STAT> <val>");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        // Arg 1: Main Commands
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], MAIN_COMMANDS, completions);
        }
        // Arg 2: Player Names (for all player-based commands)
        else if (args.length == 2 && MAIN_COMMANDS.contains(args[0].toLowerCase())) {
            if (!args[0].equalsIgnoreCase("save")) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList());
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
            }
        }
        // Arg 3: Context-sensitive argument (reset/set/add type)
        else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("reset")) {
                StringUtil.copyPartialMatches(args[2], RESET_COMMANDS, completions);
            } else if (sub.equals("set") || sub.equals("add")) {
                StringUtil.copyPartialMatches(args[2], SET_ADD_TYPES, completions);
            }
        }
        // Arg 4: STAT key or Value (for set/add level/exp/points)
        else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String type = args[2].toLowerCase();

            if (sub.equals("set") && type.equals("stat")) {
                StringUtil.copyPartialMatches(args[3], STAT_KEYS, completions);
            } else if (sub.equals("add") && type.equals("stat")) {
                StringUtil.copyPartialMatches(args[3], STAT_KEYS, completions);
            } else if ((sub.equals("set") || sub.equals("add")) && (type.equals("blevel") || type.equals("jlevel") || type.equals("points"))) {
                // Suggest common values for levels/points
                completions.add("1");
                completions.add("50");
                completions.add("100");
                completions.add("500");
            } else if ((sub.equals("set") || sub.equals("add")) && (type.equals("bexp") || type.equals("jexp"))) {
                // Suggest common values for EXP
                completions.add("1000");
                completions.add("10000");
                completions.add("100000");
            }
        }
        // Arg 5: Value for set/add stat
        else if (args.length == 5) {
            String sub = args[0].toLowerCase();
            String type = args[2].toLowerCase();

            if ((sub.equals("set") || sub.equals("add")) && type.equals("stat") && STAT_KEYS.contains(args[3].toUpperCase())) {
                completions.add("1");
                completions.add("10");
                completions.add("50");
                completions.add("100");
            }
        }

        return completions;
    }
}