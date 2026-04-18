package ru.light.command.admin.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.light.CandyQuests;
import ru.light.command.admin.SubCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RewardSubCommand implements SubCommand {
    
    private final CandyQuests plugin;

    public RewardSubCommand(CandyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.quest_reward"));
            return;
        }

        if (args[0].equalsIgnoreCase("add")) {
            addReward(player, args);
            return;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            removeReward(player, args);
            return;
        }

        player.sendMessage(plugin.getConfigManager().getMessage("commands.quest_reward"));
    }

    private void addReward(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.help_quest_add"));
            return;
        }

        try {
            int level = Integer.parseInt(args[1]);
            String type = args[2].toLowerCase();
            int line = Integer.parseInt(args[3]);

            if (level < 1 || level > 3 || line < 1 || line > 5) {
                player.sendMessage(plugin.getConfigManager().getMessage("commands.unkown_number"));
                return;
            }

            if (type.equals("command")) {
                if (args.length < 5) {
                    player.sendMessage(plugin.getConfigManager().getMessage("commands.help_quest_add_command"));
                    return;
                }

                StringBuilder commandBuilder = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    if (i > 4) commandBuilder.append(" ");
                    commandBuilder.append(args[i]);
                }
                String command = commandBuilder.toString();

                plugin.getRewardManager().addRewardCommand(level, line, command);
                player.sendMessage(plugin.getConfigManager().getMessage("admins.reward_added"));

            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("commands.help_quest_add_command"));
            }

        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.unkown_number"));
        }
    }

    private void removeReward(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.quest_remove"));
            return;
        }

        try {
            int level = Integer.parseInt(args[1]);
            int line = Integer.parseInt(args[2]);

            plugin.getRewardManager().removeReward(level, line);
            player.sendMessage(plugin.getConfigManager().getMessage("admins.reward_removed"));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.unkown_number"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("add", "remove");
        }
        if (args.length == 3) {
            return Arrays.asList("1", "2", "3");
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
            return Collections.singletonList("command");
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("remove")) {
            return Arrays.asList("1", "2", "3", "4", "5");
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("add")) {
            return Arrays.asList("1", "2", "3", "4", "5");
        }
        if (args.length >= 6 && args[1].equalsIgnoreCase("add") && args[3].equalsIgnoreCase("command")) {
            return Collections.singletonList("%player%");
        }
        return Collections.emptyList();
    }
}
