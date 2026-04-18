package ru.light.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ru.light.CandyQuests;
import ru.light.model.Reward;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardManager {
    
    private final CandyQuests plugin;
    private final File rewardsFolder;
    private final File rewardSettingsFile;
    private FileConfiguration rewardSettingsConfig;
    private final Map<String, Reward> rewards = new HashMap<>();

    public RewardManager(CandyQuests plugin) {
        this.plugin = plugin;
        this.rewardsFolder = new File(plugin.getDataFolder(), "rewards");
        this.rewardSettingsFile = new File(rewardsFolder, "reward-settings.yml");
        loadRewards();
    }

    private void loadRewards() {
        if (!rewardsFolder.exists()) {
            rewardsFolder.mkdirs();
        }

        if (!rewardSettingsFile.exists()) {
            try {
                rewardSettingsFile.createNewFile();
                createDefaultRewardSettings();
            } catch (IOException e) {
                return;
            }
        }

        rewardSettingsConfig = YamlConfiguration.loadConfiguration(rewardSettingsFile);
        rewards.clear();

        for (int level = 1; level <= 3; level++) {
            String levelKey = "quest-" + level;
            ConfigurationSection levelSection = rewardSettingsConfig.getConfigurationSection(levelKey);
            
            if (levelSection != null) {
                for (int line = 1; line <= 5; line++) {
                    String lineKey = "line-" + line;
                    List<String> commands = levelSection.getStringList(lineKey);
                    
                    if (commands != null && !commands.isEmpty()) {
                        String key = level + "-" + line;
                        Reward reward = new Reward();
                        reward.setCommands(new ArrayList<>(commands));
                        rewards.put(key, reward);
                    }
                }
            }
        }
    }

    private void createDefaultRewardSettings() {
        rewardSettingsConfig = YamlConfiguration.loadConfiguration(rewardSettingsFile);
        
        for (int level = 1; level <= 3; level++) {
            String levelKey = "quest-" + level;
            for (int line = 1; line <= 5; line++) {
                String lineKey = "line-" + line;
                rewardSettingsConfig.set(levelKey + "." + lineKey, new ArrayList<String>());
            }
        }
        
        saveRewardSettings();
    }

    public void giveReward(Player player, int level, int line) {
        String key = level + "-" + line;
        Reward reward = rewards.get(key);

        if (reward != null) {
            for (String command : reward.getCommands()) {
                String finalCommand = command
                        .replace("%player%", player.getName())
                        .replace("%player_name%", player.getName());
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                    }
                });
            }
        }
    }

    public void addRewardCommand(int level, int line, String command) {
        String levelKey = "quest-" + level;
        String lineKey = "line-" + line;
        String path = levelKey + "." + lineKey;
        
        List<String> commands = rewardSettingsConfig.getStringList(path);
        if (commands == null) {
            commands = new ArrayList<>();
        }
        commands.add(command);
        rewardSettingsConfig.set(path, commands);
        
        saveRewardSettings();
        loadRewards();
    }

    public void removeReward(int level, int line) {
        String levelKey = "quest-" + level;
        String lineKey = "line-" + line;
        String path = levelKey + "." + lineKey;
        
        rewardSettingsConfig.set(path, null);
        
        saveRewardSettings();
        loadRewards();
    }

    public void clearAllRewards() {
        for (int level = 1; level <= 3; level++) {
            rewardSettingsConfig.set("quest-" + level, null);
        }
        
        saveRewardSettings();
        loadRewards();
    }

    private void saveRewardSettings() {
        try {
            rewardSettingsConfig.save(rewardSettingsFile);
        } catch (IOException e) {
        }
    }
}
