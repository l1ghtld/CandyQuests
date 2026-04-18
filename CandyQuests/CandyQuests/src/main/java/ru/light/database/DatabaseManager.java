package ru.light.database;

import ru.light.CandyQuests;
import ru.light.model.PlayerData;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    
    private final CandyQuests plugin;
    private Connection connection;

    public DatabaseManager(CandyQuests plugin) {
        this.plugin = plugin;
        connect();
        createTables();
    }

    private void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            
            plugin.getDataFolder().mkdirs();
            String dbPath = new File(plugin.getDataFolder(), "quest.db").getAbsolutePath();
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Ошибка подключения к SQLite: " + e.getMessage());
        }
    }

    private void createTables() {
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid TEXT PRIMARY KEY," +
                "quest_progress TEXT DEFAULT '{}'," +
                "completed_quests TEXT DEFAULT '[]'," +
                "claimed_rewards TEXT DEFAULT '[]'," +
                "skipped_quests TEXT DEFAULT '[]'" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayersTable);
        } catch (SQLException e) {
            plugin.getLogger().severe("ошибка " + e.getMessage());
        }
    }

    public PlayerData loadPlayer(UUID uuid) {
        PlayerData data = new PlayerData(uuid);
        
        String query = "SELECT * FROM players WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    parseQuestProgress(data, rs.getString("quest_progress"));
                    parseCompletedQuests(data, rs.getString("completed_quests"));
                    parseClaimedRewards(data, rs.getString("claimed_rewards"));
                    parseSkippedQuests(data, rs.getString("skipped_quests"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка загрузки данных игрока " + uuid + ": " + e.getMessage());
        }
        
        return data;
    }

    public void savePlayer(PlayerData data) {
        String query = "INSERT OR REPLACE INTO players (uuid, quest_progress, completed_quests, claimed_rewards, skipped_quests) " +
                "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, data.getUuid().toString());
            stmt.setString(2, questProgressToJson(data));
            stmt.setString(3, completedQuestsToJson(data));
            stmt.setString(4, claimedRewardsToJson(data));
            stmt.setString(5, skippedQuestsToJson(data));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка сохранения данных игрока " + data.getUuid() + ": " + e.getMessage());
        }
    }

    public void resetAll() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM players");
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка сброса всех данных: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Ошибка закрытия соединения с базой данных: " + e.getMessage());
        }
    }

    private void parseQuestProgress(PlayerData data, String json) {
        if (json == null || json.equals("{}")) return;
        
        json = json.substring(1, json.length() - 1);
        if (json.trim().isEmpty()) return;
        
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                try {
                    int questId = Integer.parseInt(keyValue[0].trim());
                    int progress = Integer.parseInt(keyValue[1].trim());
                    data.setProgress(questId, progress);
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    private void parseCompletedQuests(PlayerData data, String json) {
        if (json == null || json.equals("[]")) return;
        
        json = json.substring(1, json.length() - 1);
        if (json.trim().isEmpty()) return;
        
        String[] ids = json.split(",");
        for (String id : ids) {
            try {
                data.completeQuest(Integer.parseInt(id.trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void parseClaimedRewards(PlayerData data, String json) {
        if (json == null || json.equals("[]")) return;
        
        json = json.substring(1, json.length() - 1);
        if (json.trim().isEmpty()) return;
        
        String[] ids = json.split(",");
        for (String id : ids) {
            try {
                data.claimReward(Integer.parseInt(id.trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private void parseSkippedQuests(PlayerData data, String json) {
        if (json == null || json.equals("[]")) return;
        
        json = json.substring(1, json.length() - 1);
        if (json.trim().isEmpty()) return;
        
        String[] ids = json.split(",");
        for (String id : ids) {
            try {
                data.skipQuest(Integer.parseInt(id.trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private String questProgressToJson(PlayerData data) {
        if (data.getQuestProgress().isEmpty()) return "{}";
        
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (java.util.Map.Entry<Integer, Integer> entry : data.getQuestProgress().entrySet()) {
            if (!first) sb.append(",");
            sb.append(entry.getKey()).append(":").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String completedQuestsToJson(PlayerData data) {
        if (data.getCompletedQuests().isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Integer questId : data.getCompletedQuests()) {
            if (!first) sb.append(",");
            sb.append(questId);
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private String claimedRewardsToJson(PlayerData data) {
        if (data.getClaimedRewards().isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Integer rewardId : data.getClaimedRewards()) {
            if (!first) sb.append(",");
            sb.append(rewardId);
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private String skippedQuestsToJson(PlayerData data) {
        if (data.getSkippedQuests().isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Integer questId : data.getSkippedQuests()) {
            if (!first) sb.append(",");
            sb.append(questId);
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
