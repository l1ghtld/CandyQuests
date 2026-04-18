package ru.light.manager.quest;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import ru.light.model.Quest;
import ru.light.model.enums.QuestType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestIndexService {
    
    private final Map<Integer, Quest> questsById = new ConcurrentHashMap<>();
    private final Map<QuestType, Map<Material, List<Quest>>> questsByTypeMaterial = new ConcurrentHashMap<>();
    private final Map<QuestType, Map<EntityType, List<Quest>>> questsByTypeEntity = new ConcurrentHashMap<>();
    private final Map<QuestType, List<Quest>> questsByType = new ConcurrentHashMap<>();

    public void buildIndexes(Map<Integer, List<Quest>> questsByLevel) {
        clearIndexes();
        
        for (List<Quest> quests : questsByLevel.values()) {
            for (Quest quest : quests) {
                indexQuest(quest);
            }
        }
    }

    private void indexQuest(Quest quest) {
        questsById.put(quest.getId(), quest);
        
        questsByType.computeIfAbsent(quest.getType(), k -> new ArrayList<>()).add(quest);
        
        if (quest.getMaterial() != null) {
            questsByTypeMaterial
                    .computeIfAbsent(quest.getType(), k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(quest.getMaterial(), k -> new ArrayList<>())
                    .add(quest);
        }
        
        if (quest.getEntityType() != null) {
            questsByTypeEntity
                    .computeIfAbsent(quest.getType(), k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(quest.getEntityType(), k -> new ArrayList<>())
                    .add(quest);
        }
    }

    private void clearIndexes() {
        questsById.clear();
        questsByTypeMaterial.clear();
        questsByTypeEntity.clear();
        questsByType.clear();
    }

    public Quest getQuestById(int id) {
        return questsById.get(id);
    }

    public List<Quest> getQuestsByType(QuestType type) {
        return questsByType.getOrDefault(type, Collections.emptyList());
    }

    public List<Quest> getQuestsByTypeMaterial(QuestType type, Material material) {
        Map<Material, List<Quest>> materialQuests = questsByTypeMaterial.get(type);
        if (materialQuests == null) return Collections.emptyList();
        
        List<Quest> quests = materialQuests.get(material);
        return quests != null ? quests : Collections.emptyList();
    }

    public List<Quest> getQuestsByTypeEntity(QuestType type, EntityType entityType) {
        Map<EntityType, List<Quest>> entityQuests = questsByTypeEntity.get(type);
        if (entityQuests == null) return Collections.emptyList();
        
        List<Quest> quests = entityQuests.get(entityType);
        return quests != null ? quests : Collections.emptyList();
    }
}
