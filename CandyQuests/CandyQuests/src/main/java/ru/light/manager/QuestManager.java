package ru.light.manager;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import ru.light.CandyQuests;
import ru.light.model.Quest;
import ru.light.model.enums.QuestType;
import ru.light.manager.quest.QuestGeneratorService;
import ru.light.manager.quest.QuestIndexService;
import ru.light.manager.quest.QuestStorageService;

import java.util.*;
import java.util.stream.Collectors;

public class QuestManager {
    
    private final CandyQuests plugin;
    private final QuestGeneratorService generatorService;
    private final QuestIndexService indexService;
    private final QuestStorageService storageService;

    public QuestManager(CandyQuests plugin) {
        this.plugin = plugin;
        this.generatorService = new QuestGeneratorService(plugin);
        this.indexService = new QuestIndexService();
        this.storageService = new QuestStorageService(plugin);
        
        initialize();
    }

    private void initialize() {
        if (storageService.loadQuests()) {
            indexService.buildIndexes(storageService.getQuestsByLevel());
        } else {
            generateNewQuests();
        }
    }

    private void generateNewQuests() {
        Map<Integer, List<Quest>> quests = generatorService.generateAllQuests();
        storageService.setQuests(quests);
        indexService.buildIndexes(quests);
        storageService.saveQuests();
    }

    public List<Quest> getQuestsForLevel(int level) {
        return storageService.getQuestsForLevel(level);
    }

    public Quest getQuest(int questId) {
        return indexService.getQuestById(questId);
    }

    public List<Quest> getActiveQuestsForMaterial(UUID playerId, QuestType type, Material material) {
        return indexService.getQuestsByTypeMaterial(type, material).stream()
                .filter(q -> !plugin.getPlayerDataManager().getPlayerData(playerId).isQuestCompleted(q.getId()))
                .filter(q -> isQuestUnlocked(q.getId(), playerId))
                .collect(Collectors.toList());
    }

    public List<Quest> getActiveQuestsForEntity(UUID playerId, QuestType type, EntityType entityType) {
        return indexService.getQuestsByTypeEntity(type, entityType).stream()
                .filter(q -> !plugin.getPlayerDataManager().getPlayerData(playerId).isQuestCompleted(q.getId()))
                .filter(q -> isQuestUnlocked(q.getId(), playerId))
                .collect(Collectors.toList());
    }

    public List<Quest> getActiveQuestsForPlayer(UUID playerId, QuestType type) {
        return indexService.getQuestsByType(type).stream()
                .filter(q -> !plugin.getPlayerDataManager().getPlayerData(playerId).isQuestCompleted(q.getId()))
                .filter(q -> isQuestUnlocked(q.getId(), playerId))
                .collect(Collectors.toList());
    }

    public boolean isQuestUnlocked(int questId, UUID playerId) {
        int level = questId / 100;
        int questNumber = questId % 100;
        
        if (questNumber == 1) return true;
        
        int previousQuestId = level * 100 + (questNumber - 1);
        return plugin.getPlayerDataManager().getPlayerData(playerId).isQuestCompleted(previousQuestId);
    }

    public void regenerateQuests() {
        generateNewQuests();
        plugin.getPlayerDataManager().resetAllPlayersData();
    }

    public Material getMobDropMaterial(EntityType entityType) {
        return generatorService.getMobDropMaterial(entityType);
    }
}
