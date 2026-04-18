package ru.light.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.light.CandyQuests;
import ru.light.model.PlayerData;
import ru.light.model.Quest;
import ru.light.service.skip.SkipService;

import java.util.List;

public class QuestPlaceholder extends PlaceholderExpansion {
    
    private final CandyQuests plugin;
    
    public QuestPlaceholder(CandyQuests plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "candyquests";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "Light";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        if (params.equals("skipquest_amount")) {
            int remaining = SkipService.getRemainingSkips(player, playerData.getSkipCount());
            return String.valueOf(remaining);
        }
        
        if (params.equals("skipquest_max")) {
            int max = SkipService.getMaxSkips(player);
            return String.valueOf(max);
        }
        
        if (params.equals("skipquest_used")) {
            return String.valueOf(playerData.getSkipCount());
        }
        
        if (params.startsWith("quest_completed_")) {
            try {
                int questNumber = Integer.parseInt(params.substring("quest_completed_".length()));
                int level = ((questNumber - 1) / 40) + 1;
                int questIndexInLevel = ((questNumber - 1) % 40) + 1;
                
                if (level >= 1 && level <= 3) {
                    List<Quest> quests = plugin.getQuestManager().getQuestsForLevel(level);
                    if (questIndexInLevel <= quests.size()) {
                        Quest quest = quests.get(questIndexInLevel - 1);
                        return playerData.isQuestCompleted(quest.getId()) ? "true" : "false";
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            return "false";
        }
        
        if (params.startsWith("quest_status_")) {
            try {
                int questNumber = Integer.parseInt(params.substring("quest_status_".length()));
                int level = ((questNumber - 1) / 40) + 1;
                int questIndexInLevel = ((questNumber - 1) % 40) + 1;
                
                if (level >= 1 && level <= 3) {
                    List<Quest> quests = plugin.getQuestManager().getQuestsForLevel(level);
                    if (questIndexInLevel <= quests.size()) {
                        Quest quest = quests.get(questIndexInLevel - 1);
                        if (playerData.isQuestCompleted(quest.getId())) {
                            return "§6Вы уже прошли квест";
                        } else {
                            int remaining = SkipService.getRemainingSkips(player, playerData.getSkipCount());
                            return "§6/skipquest " + questNumber + " §7(Осталось " + remaining + " раз)";
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            return "";
        }
        
        return null;
    }
}
