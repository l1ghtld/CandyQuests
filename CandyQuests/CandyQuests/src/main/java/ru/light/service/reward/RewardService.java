package ru.light.service.reward;

import ru.light.CandyQuests;
import ru.light.model.PlayerData;
import ru.light.model.Quest;

import java.util.List;

public class RewardService {
    
    private final CandyQuests plugin;

    public RewardService(CandyQuests plugin) {
        this.plugin = plugin;
    }

    public boolean canClaimReward(int level, int line, PlayerData playerData) {
        List<Quest> quests = plugin.getQuestManager().getQuestsForLevel(level);
        if (quests.isEmpty()) return false;
        
        int questsPerReward = 8;
        int startIndex = (line - 1) * questsPerReward;
        int endIndex = Math.min(startIndex + questsPerReward, quests.size());
        
        if (startIndex >= quests.size()) return false;
        
        for (int i = startIndex; i < endIndex; i++) {
            Quest quest = quests.get(i);
            if (!playerData.isQuestCompleted(quest.getId())) {
                return false;
            }
        }
        
        return true;
    }

    public int generateRewardId(int level, int line) {
        return level * 1000 + line;
    }
}
