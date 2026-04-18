package ru.light.command.admin.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.light.CandyQuests;
import ru.light.command.admin.SubCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ResetSubCommand implements SubCommand {
    
    private final CandyQuests plugin;

    public ResetSubCommand(CandyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.quest_reset"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "all_quests":
                plugin.getQuestManager().regenerateQuests();
                player.sendMessage(plugin.getConfigManager().getMessage("admins.reseted_all_quests"));
                break;
            case "all_rewards":
                plugin.getRewardManager().clearAllRewards();
                player.sendMessage(plugin.getConfigManager().getMessage("admins.reseted_all_rewards"));
                break;
            default:
                player.sendMessage(plugin.getConfigManager().getMessage("commands.quest_reset"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("all_quests", "all_rewards");
        }
        return Collections.emptyList();
    }
}
