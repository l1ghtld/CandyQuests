package ru.light;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.light.command.QuestCommandExecutor;
import ru.light.command.admin.AdminCommandExecutor;
import ru.light.command.skip.SkipCommandExecutor;
import ru.light.config.ConfigManager;
import ru.light.database.DatabaseManager;
import ru.light.listener.inventory.QuestInventoryListener;
import ru.light.listener.player.PlayerConnectionListener;
import ru.light.listener.quest.*;
import ru.light.manager.PlayerDataManager;
import ru.light.manager.QuestManager;
import ru.light.manager.RewardManager;
import ru.light.placeholder.QuestPlaceholder;
import ru.light.translation.TranslationManager;

import java.util.Objects;

@Getter
public final class CandyQuests extends JavaPlugin {

    private static CandyQuests instance;
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private TranslationManager translationManager;
    private PlayerDataManager playerDataManager;
    private QuestManager questManager;
    private RewardManager rewardManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        initializeManagers();
        registerCommands();
        registerListeners();
        registerPlaceholders();
    }

    @Override
    public void onDisable() {
        shutdownManagers();
    }

    private void initializeManagers() {
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.translationManager = new TranslationManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.questManager = new QuestManager(this);
        this.rewardManager = new RewardManager(this);
    }

    private void registerCommands() {
        QuestCommandExecutor questRouter = new QuestCommandExecutor(this);
        AdminCommandExecutor adminExecutor = new AdminCommandExecutor(this);
        SkipCommandExecutor skipExecutor = new SkipCommandExecutor(this);

        Objects.requireNonNull(getCommand("quest")).setExecutor(questRouter);
        Objects.requireNonNull(getCommand("quest")).setTabCompleter(questRouter);
        Objects.requireNonNull(getCommand("quest1")).setExecutor(questRouter);
        Objects.requireNonNull(getCommand("quest2")).setExecutor(questRouter);
        Objects.requireNonNull(getCommand("quest3")).setExecutor(questRouter);
        Objects.requireNonNull(getCommand("skipquest")).setExecutor(skipExecutor);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new QuestInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new QuestProgressListener(this), this);
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new QuestPlaceholder(this).register();
        }
    }

    private void shutdownManagers() {
        try {
            if (translationManager != null) {
                translationManager.shutdown();
            }
            if (playerDataManager != null) {
                playerDataManager.shutdown();
            }
            if (databaseManager != null) {
                databaseManager.close();
            }
        } catch (Exception ignored) {
        }
    }

    public static CandyQuests getInstance() {
        return instance;
    }
}
