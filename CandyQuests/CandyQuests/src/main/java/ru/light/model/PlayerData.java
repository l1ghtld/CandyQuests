package ru.light.model;

import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
public class PlayerData {
    
    private final UUID uuid;
    private final Map<Integer, Integer> questProgress = new HashMap<>();
    private final Set<Integer> completedQuests = new HashSet<>();
    private final Set<Integer> claimedRewards = new HashSet<>();
    private final Set<Integer> skippedQuests = new HashSet<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public void addProgress(int questId, int amount) {
        questProgress.put(questId, questProgress.getOrDefault(questId, 0) + amount);
    }

    public int getProgress(int questId) {
        return questProgress.getOrDefault(questId, 0);
    }

    public void setProgress(int questId, int amount) {
        questProgress.put(questId, amount);
    }

    public void completeQuest(int questId) {
        completedQuests.add(questId);
    }

    public boolean isQuestCompleted(int questId) {
        return completedQuests.contains(questId);
    }

    public void claimReward(int rewardId) {
        claimedRewards.add(rewardId);
    }

    public boolean isRewardClaimed(int rewardId) {
        return claimedRewards.contains(rewardId);
    }
    
    public void skipQuest(int questId) {
        skippedQuests.add(questId);
    }
    
    public boolean isQuestSkipped(int questId) {
        return skippedQuests.contains(questId);
    }
    
    public int getSkipCount() {
        return skippedQuests.size();
    }
}
