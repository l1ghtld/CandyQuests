package ru.light.command.admin.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.light.CandyQuests;
import ru.light.command.admin.SubCommand;
import ru.light.model.PlayerData;
import ru.light.model.Quest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SkipAllSubCommand implements SubCommand {
    
    private final CandyQuests plugin;

    public SkipAllSubCommand(CandyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length != 2 || !args[0].equalsIgnoreCase("all_quests")) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.skip_all_usage"));
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.skip_all_unknown_level"));
            return;
        }

        if (level < 1 || level > 3) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.skip_all_unknown_level"));
            return;
        }

        List<Quest> quests = plugin.getQuestManager().getQuestsForLevel(level);
        if (quests.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.skip_all_unknown_level"));
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        boolean alreadyCompletedAll = true;
        for (Quest q : quests) {
            if (!data.isQuestCompleted(q.getId())) {
                alreadyCompletedAll = false;
                break;
            }
        }

        if (alreadyCompletedAll) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.skip_all_quests_ready"));
            return;
        }

        for (Quest q : quests) {
            if (!data.isQuestCompleted(q.getId())) {
                data.setProgress(q.getId(), q.getNeeded());
                data.completeQuest(q.getId());
            }
        }
        
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());

        player.sendMessage(plugin.getConfigManager().getMessageWithPlaceholders("commands.skip_all_success",
                "%level%", String.valueOf(level)));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Collections.singletonList("all_quests");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("all_quests")) {
            return Arrays.asList("1", "2", "3");
        }
        return Collections.emptyList();
    }
}
