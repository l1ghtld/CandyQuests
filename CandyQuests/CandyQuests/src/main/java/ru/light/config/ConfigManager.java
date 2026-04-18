package ru.light.config;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ru.light.CandyQuests;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    
    private final CandyQuests plugin;
    private final boolean placeholderAPIEnabled;

    public ConfigManager(CandyQuests plugin) {
        this.plugin = plugin;
        this.placeholderAPIEnabled = plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public String getMessage(String path) {
        return colorize(getConfig().getString("messages." + path, ""));
    }

    public String getMessage(String path, Player player) {
        return applyPlaceholders(getMessage(path), player);
    }

    public String getMessageWithPlaceholders(String path, String... placeholders) {
        return replacePlaceholders(getMessage(path), placeholders);
    }

    public List<String> getMessageList(String path) {
        return getConfig().getStringList("messages." + path).stream()
                .map(this::colorize)
                .collect(Collectors.toList());
    }

    public List<String> getMessageList(String path, Player player) {
        return applyPlaceholders(getMessageList(path), player);
    }

    public List<String> getMessageListOrSingle(String path) {
        if (getConfig().isList("messages." + path)) {
            return getMessageList(path);
        }
        
        String message = getMessage(path);
        List<String> result = new ArrayList<>();
        if (!message.isEmpty()) {
            result.add(message);
        }
        return result;
    }

    public List<String> getMessageListOrSingleWithPlaceholders(String path, String... placeholders) {
        return getMessageListOrSingle(path).stream()
                .map(msg -> replacePlaceholders(msg, placeholders))
                .collect(Collectors.toList());
    }

    public String getSetting(String path) {
        return colorize(getConfig().getString("settings." + path, ""));
    }

    public String getSetting(String path, Player player) {
        return applyPlaceholders(getSetting(path), player);
    }

    public List<String> getSettingList(String path) {
        return getConfig().getStringList("settings." + path).stream()
                .map(this::colorize)
                .collect(Collectors.toList());
    }

    public List<String> getSettingList(String path, Player player) {
        return applyPlaceholders(getSettingList(path), player);
    }

    public int getSettingInt(String path) {
        return getConfig().getInt("settings." + path, 0);
    }

    public List<String> applyPlaceholdersToList(List<String> texts, Player player) {
        return applyPlaceholders(texts, player);
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private String applyPlaceholders(String text, Player player) {
        if (placeholderAPIEnabled && player != null) {
            text = PlaceholderAPI.setPlaceholders(player, text);
        }
        return colorize(text);
    }

    private List<String> applyPlaceholders(List<String> texts, Player player) {
        if (placeholderAPIEnabled && player != null) {
            return texts.stream()
                    .map(text -> PlaceholderAPI.setPlaceholders(player, text))
                    .map(this::colorize)
                    .collect(Collectors.toList());
        }
        return texts.stream()
                .map(this::colorize)
                .collect(Collectors.toList());
    }

    private String replacePlaceholders(String message, String... placeholders) {
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return message;
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
