package ru.light.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import ru.light.CandyQuests;
import ru.light.model.PlayerData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    
    private final CandyQuests plugin;
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAccessTime = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    private static final long CACHE_TTL = 10 * 60 * 1000;

    public PlayerDataManager(CandyQuests plugin) {
        this.plugin = plugin;
        loadOnlinePlayers();
        startCleanupTask();
    }

    private void startCleanupTask() {
        this.cleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::cleanupExpiredCache, 20L * 300L, 20L * 300L);
    }

    private void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();

        lastAccessTime.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            long lastAccess = entry.getValue();
            Player player = Bukkit.getPlayer(uuid);
            
            if (player != null && player.isOnline()) {
                return false;
            }
            
            if (currentTime - lastAccess > CACHE_TTL) {
                PlayerData data = playerDataCache.remove(uuid);
                if (data != null) {
                    plugin.getDatabaseManager().savePlayer(data);
                }
                return true;
            }
            return false;
        });
    }

    private void loadOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayerData(player.getUniqueId());
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        lastAccessTime.put(uuid, System.currentTimeMillis());
        return playerDataCache.computeIfAbsent(uuid, this::loadPlayerData);
    }

    private PlayerData loadPlayerData(UUID uuid) {
        return plugin.getDatabaseManager().loadPlayer(uuid);
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            plugin.getDatabaseManager().savePlayer(data);
        }
    }

    public void resetAllPlayersData() {
        for (PlayerData data : playerDataCache.values()) {
            data.getQuestProgress().clear();
            data.getCompletedQuests().clear();
            data.getClaimedRewards().clear();
            data.getSkippedQuests().clear();
            plugin.getDatabaseManager().savePlayer(data);
        }
        plugin.getDatabaseManager().resetAll();
    }

    public void saveAllData() {
        for (PlayerData data : playerDataCache.values()) {
            plugin.getDatabaseManager().savePlayer(data);
        }
    }

    public void unloadPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.remove(uuid);
        lastAccessTime.remove(uuid);
        if (data != null) {
            plugin.getDatabaseManager().savePlayer(data);
        }
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        saveAllData();
    }
}
