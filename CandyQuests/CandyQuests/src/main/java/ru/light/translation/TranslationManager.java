package ru.light.translation;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;
import ru.light.CandyQuests;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationManager {

    private final CandyQuests plugin;
    private final ConcurrentMap<Material, String> translations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> dictionary = new ConcurrentHashMap<>();
    private final File translationsFile;
    @Getter
    private volatile boolean loaded = false;
    private volatile boolean loading = false;
    private BukkitTask updateTask;

    private final String[] TRANSLATION_APIS = new String[]{
            "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.20.1/assets/minecraft/lang/ru_ru.json",
            "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.19.4/assets/minecraft/lang/ru_ru.json",
            "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.18.2/assets/minecraft/lang/ru_ru.json"
    };

    public TranslationManager(CandyQuests plugin) {
        this.plugin = plugin;
        this.translationsFile = new File(plugin.getDataFolder(), "translations.json");
        
        if (!translationsFile.exists()) {
            plugin.getDataFolder().mkdirs();
            updateTranslationsAsync();
        } else {
            loadTranslationsFromFile();
        }
        
        this.updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                this::updateTranslationsAsync, 20L * 3600L * 24L, 20L * 3600L * 24L);
    }

    private void loadTranslationsFromFile() {
        try {
            if (!translationsFile.exists()) return;
            
            Map<String, String> jsonData = parseJsonFromFile();
            dictionary.clear();
            dictionary.putAll(jsonData);
            
            for (Material material : Material.values()) {
                String[] keys = getTranslationKeys(material);
                for (String key : keys) {
                    if (jsonData.containsKey(key)) {
                        translations.put(material, jsonData.get(key));
                        break;
                    }
                }
            }
            loaded = true;
        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка загрузки переводов: " + e.getMessage());
        }
    }

    private Map<String, String> parseJsonFromFile() throws IOException {
        Map<String, String> result = new HashMap<>();
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(translationsFile), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        
        String jsonContent = content.toString().trim();
        if (jsonContent.startsWith("{") && jsonContent.endsWith("}")) {
            jsonContent = jsonContent.substring(1, jsonContent.length() - 1);
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(jsonContent);
            
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                result.put(key, unescapeJson(value));
            }
        }
        return result;
    }

    private void writeJsonToFile(Map<String, String> data) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(translationsFile), "UTF-8")) {
            writer.write("{\n");
            boolean first = true;
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (!first) {
                    writer.write(",\n");
                }
                writer.write("  \"" + escapeJson(entry.getKey()) + "\": \"" +
                        escapeJson(entry.getValue()) + "\"");
                first = false;
            }
            writer.write("\n}");
        }
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String input) {
        return input.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private void updateTranslationsAsync() {
        if (loading) return;
        loading = true;
        
        CompletableFuture.supplyAsync(() -> {
            for (String apiUrl : TRANSLATION_APIS) {
                if (loadTranslationsFromUrl(apiUrl)) {
                    return true;
                }
            }
            return false;
        }).whenComplete((success, throwable) -> {
            if (success) {
                loaded = true;
            }
            loading = false;
        });
    }

    private boolean loadTranslationsFromUrl(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "CandyQuests-Plugin/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) return false;
            
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            Map<String, String> newTranslations = parseJsonString(response.toString());
            dictionary.clear();
            dictionary.putAll(newTranslations);
            
            for (Material material : Material.values()) {
                String[] keys = getTranslationKeys(material);
                for (String key : keys) {
                    if (newTranslations.containsKey(key)) {
                        translations.put(material, newTranslations.get(key));
                        break;
                    }
                }
            }
            
            writeJsonToFile(newTranslations);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, String> parseJsonString(String jsonString) {
        Map<String, String> result = new HashMap<>();
        String content = jsonString.trim();
        
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
            Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(content);
            
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                result.put(key, unescapeJson(value));
            }
        }
        return result;
    }

    private String[] getTranslationKeys(Material material) {
        String name = material.name().toLowerCase();
        if (material.isBlock()) {
            return new String[]{
                    "block.minecraft." + name,
                    "tile." + name + ".name",
                    "item.minecraft." + name
            };
        } else {
            return new String[]{
                    "item.minecraft." + name,
                    "item." + name + ".name",
                    "block.minecraft." + name
            };
        }
    }

    public String translate(Material material) {
        if (material == null) return "Неизвестный предмет";
        
        String translation = translations.get(material);
        if (translation != null && !translation.isEmpty()) {
            return translation;
        }
        return material.name().toLowerCase().replace("_", " ");
    }

    public String translateEntity(EntityType type) {
        if (type == null) return "неизвестная сущность";
        
        String key = "entity.minecraft." + type.name().toLowerCase();
        String value = dictionary.get(key);
        
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return type.name().toLowerCase().replace("_", " ");
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
    }
}
