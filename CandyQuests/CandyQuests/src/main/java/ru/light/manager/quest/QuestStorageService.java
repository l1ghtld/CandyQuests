package ru.light.manager.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import ru.light.CandyQuests;
import ru.light.model.Quest;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QuestStorageService {
    
    private final CandyQuests plugin;
    private final Map<Integer, List<Quest>> questsByLevel = new ConcurrentHashMap<>();
    private final File questDataFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private long questSeed = 0;

    public QuestStorageService(CandyQuests plugin) {
        this.plugin = plugin;
        this.questDataFile = new File(plugin.getDataFolder(), "quest_data.json");
    }

    public boolean loadQuests() {
        try {
            if (!questDataFile.exists()) return false;
            
            try (FileReader reader = new FileReader(questDataFile)) {
                QuestData questData = gson.fromJson(reader, QuestData.class);
                if (questData != null && questData.questsByLevel != null) {
                    questSeed = questData.seed;
                    questsByLevel.clear();
                    questsByLevel.putAll(questData.questsByLevel);
                    return true;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка загрузки квестов: " + e.getMessage());
        }
        return false;
    }

    public void saveQuests() {
        try {
            plugin.getDataFolder().mkdirs();
            
            QuestData questData = new QuestData();
            questData.seed = questSeed;
            questData.questsByLevel = questsByLevel;
            questData.generated = true;
            
            try (FileWriter writer = new FileWriter(questDataFile)) {
                gson.toJson(questData, writer);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка сохранения квестов: " + e.getMessage());
        }
    }

    public List<Quest> getQuestsForLevel(int level) {
        return questsByLevel.getOrDefault(level, new ArrayList<>());
    }

    public Map<Integer, List<Quest>> getQuestsByLevel() {
        return questsByLevel;
    }

    public void setQuests(Map<Integer, List<Quest>> quests) {
        questsByLevel.clear();
        questsByLevel.putAll(quests);
        questSeed = System.currentTimeMillis();
    }

    @Getter
    @Setter
    private static class QuestData {
        boolean generated;
        long seed;
        Map<Integer, List<Quest>> questsByLevel;
    }
}
