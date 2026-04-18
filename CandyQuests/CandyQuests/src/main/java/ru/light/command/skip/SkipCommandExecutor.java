package ru.light.command.skip;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.light.CandyQuests;
import ru.light.model.PlayerData;
import ru.light.model.Quest;
import ru.light.service.skip.SkipService;

import java.util.ArrayList;
import java.util.List;

public class SkipCommandExecutor implements CommandExecutor, TabCompleter {

    private final CandyQuests plugin;

    public SkipCommandExecutor(CandyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        
        Player player = (Player) sender;

        if (args.length == 1) {
            try {
                int questNumber = Integer.parseInt(args[0]);
                skipSingleQuest(player, questNumber);
                return true;
            } catch (NumberFormatException ignored) {
            }
        }

        player.sendMessage(plugin.getConfigManager().getMessage("commands.invalid_command"));
        return true;
    }

    private void skipSingleQuest(Player player, int questNumber) {
        if (!player.hasPermission("quests.skipquest")) {
            for (String line : plugin.getConfigManager().getMessageListOrSingle("skipquest.no_subscription")) {
                player.sendMessage(line);
            }
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        int level = ((questNumber - 1) / 40) + 1;
        int questIndexInLevel = ((questNumber - 1) % 40) + 1;

        if (level < 1 || level > 3) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.quest_not_found"));
            return;
        }

        List<Quest> quests = plugin.getQuestManager().getQuestsForLevel(level);
        if (questIndexInLevel > quests.size()) {
            player.sendMessage(plugin.getConfigManager().getMessage("commands.quest_not_found"));
            return;
        }

        Quest quest = quests.get(questIndexInLevel - 1);

        if (playerData.isQuestCompleted(quest.getId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("quests.passed"));
            return;
        }

        if (!SkipService.canSkip(player, playerData.getSkipCount())) {
            player.sendMessage(plugin.getConfigManager().getMessage("skipquest.skip_error"));
            return;
        }

        if (!plugin.getQuestManager().isQuestUnlocked(quest.getId(), player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("skipquest.next_quest_skip_error"));
            return;
        }

        playerData.setProgress(quest.getId(), quest.getNeeded());
        playerData.completeQuest(quest.getId());
        playerData.skipQuest(quest.getId());

        for (String line : plugin.getConfigManager().getMessageListOrSingleWithPlaceholders("skipquest.skipped",
                "%quest_skipped%", String.valueOf(questNumber))) {
            player.sendMessage(line);
        }
        
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        return new ArrayList<>();
    }
}
