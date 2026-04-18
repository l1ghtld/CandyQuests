package ru.light.command.handler;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.light.CandyQuests;
import ru.light.model.PlayerData;
import ru.light.model.Quest;
import ru.light.model.enums.QuestType;
import ru.light.service.reward.RewardService;
import ru.light.service.skip.SkipService;
import ru.light.translation.TranslationManager;
import ru.light.ui.item.ItemBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestMenuHandler {
    
    private final CandyQuests plugin;
    private final RewardService rewardService;

    public QuestMenuHandler(CandyQuests plugin) {
        this.plugin = plugin;
        this.rewardService = new RewardService(plugin);
    }

    public boolean openCurrentLevelMenu(Player player) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int currentLevel = getCurrentPlayerLevel(playerData);
        return openLevelMenu(player, currentLevel);
    }

    public boolean openLevelMenu(Player player, int level) {
        if (level > 1 && !canOpenLevel(player, level)) {
            List<String> messages = plugin.getConfigManager().getMessageListOrSingle("quests_menu.error");
            messages.forEach(player::sendMessage);
            return true;
        }
        openQuestMenu(player, level);
        return true;
    }

    private int getCurrentPlayerLevel(PlayerData playerData) {
        List<Quest> level1Quests = plugin.getQuestManager().getQuestsForLevel(1);
        for (Quest quest : level1Quests) {
            if (!playerData.isQuestCompleted(quest.getId())) {
                return 1;
            }
        }

        List<Quest> level2Quests = plugin.getQuestManager().getQuestsForLevel(2);
        for (Quest quest : level2Quests) {
            if (!playerData.isQuestCompleted(quest.getId())) {
                return 2;
            }
        }
        return 3;
    }

    private boolean canOpenLevel(Player player, int level) {
        if (level == 1) return true;
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        for (int prevLevel = 1; prevLevel < level; prevLevel++) {
            List<Quest> prevQuests = plugin.getQuestManager().getQuestsForLevel(prevLevel);
            for (Quest quest : prevQuests) {
                if (!playerData.isQuestCompleted(quest.getId())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void openQuestMenu(Player player, int level) {
        String menuName = plugin.getConfigManager().getSetting("menu.names." + level, player);
        Inventory inventory = Bukkit.createInventory(player, 54, menuName);
        
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        List<Quest> quests = plugin.getQuestManager().getQuestsForLevel(level);
        
        String[] questSlots = plugin.getConfigManager().getSetting("menu.quests_settings.quests").split("-");
        int startSlot = Integer.parseInt(questSlots[0]);
        int endSlot = Integer.parseInt(questSlots[1]);
        
        String[] rewardSlotsStr = plugin.getConfigManager().getSetting("menu.quests_settings.rewards").split(",");
        int[] rewardSlots = new int[rewardSlotsStr.length];
        for (int i = 0; i < rewardSlotsStr.length; i++) {
            rewardSlots[i] = Integer.parseInt(rewardSlotsStr[i].trim());
        }
        
        int questIndex = 0;
        for (int slot = startSlot; slot <= endSlot; slot++) {
            boolean isRewardSlot = false;
            int rewardLine = -1;
            
            for (int i = 0; i < rewardSlots.length; i++) {
                if (rewardSlots[i] == slot) {
                    isRewardSlot = true;
                    rewardLine = i + 1;
                    break;
                }
            }
            
            if (isRewardSlot) {
                addRewardItem(inventory, slot, level, rewardLine, playerData);
            } else {
                if (questIndex < quests.size()) {
                    Quest quest = quests.get(questIndex);
                    if (plugin.getQuestManager().isQuestUnlocked(quest.getId(), player.getUniqueId())) {
                        addQuestItem(inventory, slot, quest, playerData, player);
                    } else {
                        addBarrierItem(inventory, slot, player);
                    }
                    questIndex++;
                }
            }
        }
        
        player.openInventory(inventory);
    }

    private void addQuestItem(Inventory inventory, int slot, Quest quest, PlayerData playerData, Player player) {
        Material displayMaterial = getQuestDisplayMaterial(quest);
        String questName = getQuestDisplayName(quest, player);
        List<String> lore = getQuestLore(quest, playerData, player);
        
        ItemStack item = ItemBuilder.of(displayMaterial)
                .name(questName)
                .lore(lore)
                .build();
        
        inventory.setItem(slot, item);
    }

    private Material getQuestDisplayMaterial(Quest quest) {
        if (quest.getType() == QuestType.PASS_ITEM || quest.getType() == QuestType.CRAFT_ITEM ||
            quest.getType() == QuestType.PLACE_BLOCK || quest.getType() == QuestType.BREAK_BLOCK) {
            return quest.getMaterial();
        } else if (quest.getType() == QuestType.KILL_MOB) {
            if (quest.getMaterial() != null) {
                return quest.getMaterial();
            }
            if (quest.getEntityType() != null) {
                Material mobMaterial = plugin.getQuestManager().getMobDropMaterial(quest.getEntityType());
                if (mobMaterial != null) {
                    return mobMaterial;
                }
            }
            try {
                if (quest.getEntityType() != null) {
                    return Material.valueOf(quest.getEntityType().name() + "_SPAWN_EGG");
                }
            } catch (IllegalArgumentException e) {
            }
            return Material.SPAWNER;
        } else if (quest.getType() == QuestType.KILL_PLAYER) {
            if (quest.getMaterial() != null) {
                return quest.getMaterial();
            }
            return Material.PLAYER_HEAD;
        }
        return Material.STONE;
    }

    private String getQuestDisplayName(Quest quest, Player player) {
        String configPath;
        switch (quest.getType()) {
            case PASS_ITEM:
                configPath = "menu.pass_quest.name";
                break;
            case CRAFT_ITEM:
                configPath = "menu.craft_quest.name";
                break;
            case PLACE_BLOCK:
                configPath = "menu.place_block_quest.name";
                break;
            case BREAK_BLOCK:
                configPath = "menu.breaking_quest.name";
                break;
            default:
                configPath = "menu.kill_quest.name";
                break;
        }
        
        String name = plugin.getConfigManager().getSetting(configPath, player);
        name = name.replace("%material%", getMaterialDisplayName(quest));
        name = name.replace("%whom%", getTargetDisplayName(quest));
        return name;
    }

    private String getMaterialDisplayName(Quest quest) {
        if (quest.getMaterial() == null) return "";
        return plugin.getTranslationManager().translate(quest.getMaterial());
    }

    private String getTargetDisplayName(Quest quest) {
        if (quest.getType() == QuestType.KILL_PLAYER) {
            String base = plugin.getConfigManager().getSetting("menu.kill_quest.target.player");
            if (base == null || base.isEmpty()) base = "&6Игрок";
            return base;
        } else if (quest.getType() == QuestType.KILL_MOB) {
            String mobName = "моб";
            if (quest.getEntityType() != null) {
                String t = plugin.getTranslationManager().translateEntity(quest.getEntityType());
                if (t != null && !t.trim().isEmpty()) {
                    mobName = t.substring(0, 1).toUpperCase() + t.substring(1).toLowerCase();
                }
            }
            String base = plugin.getConfigManager().getSetting("menu.kill_quest.target.mobs");
            if (base == null || base.isEmpty()) base = "&6%mob%";
            return base.replace("%mob%", mobName);
        }
        return "";
    }

    private List<String> getQuestLore(Quest quest, PlayerData playerData, Player player) {
        String configPath;
        switch (quest.getType()) {
            case PASS_ITEM:
                configPath = "menu.pass_quest.lore";
                break;
            case CRAFT_ITEM:
                configPath = "menu.craft_quest.lore";
                break;
            case PLACE_BLOCK:
                configPath = "menu.place_block_quest.lore";
                break;
            case BREAK_BLOCK:
                configPath = "menu.breaking_quest.lore";
                break;
            default:
                configPath = "menu.kill_quest.lore";
                break;
        }
        
        int progress = playerData.getProgress(quest.getId());
        boolean completed = playerData.isQuestCompleted(quest.getId());
        String status = completed ? 
                plugin.getConfigManager().getSetting("status.completed", player) : 
                plugin.getConfigManager().getSetting("status.not_completed", player);
        
        int questLevel = quest.getId() / 100;
        int questNumberInLevel = quest.getId() % 100;
        int globalQuestNumber = (questLevel - 1) * 40 + questNumberInLevel;
        
        List<String> rawLore = plugin.getConfig().getStringList("settings." + configPath);
        List<String> processedLore = new ArrayList<>();
        
        for (String line : rawLore) {
            line = line.replace("%material%", getMaterialDisplayName(quest));
            line = line.replace("%whom%", getTargetDisplayName(quest));
            line = line.replace("%passed%", String.valueOf(progress));
            line = line.replace("%necessary%", String.valueOf(quest.getNeeded()));
            line = line.replace("%status%", status);
            line = line.replace("%number_quest%", String.valueOf(globalQuestNumber));
            
            if (line.contains("%candyquests_quest_status_")) {
                String placeholder = "%candyquests_quest_status_" + globalQuestNumber + "%";
                String replacement;
                if (completed) {
                    replacement = "&6Вы уже прошли квест";
                } else {
                    int remaining = SkipService.getRemainingSkips(player, playerData.getSkipCount());
                    replacement = "&6/skipquest " + globalQuestNumber + " &7(Осталось " + remaining + " раз)";
                }
                line = line.replace(placeholder, replacement);
            }
            
            processedLore.add(line);
        }
        
        return plugin.getConfigManager().applyPlaceholdersToList(processedLore, player);
    }

    private void addBarrierItem(Inventory inventory, int slot, Player player) {
        String name = plugin.getConfigManager().getSetting("menu.barrier.name", player);
        List<String> lore = plugin.getConfigManager().getSettingList("menu.barrier.lore", player);
        
        ItemStack barrier = ItemBuilder.of(Material.BARRIER)
                .name(name)
                .lore(lore)
                .build();
        
        inventory.setItem(slot, barrier);
    }

    private void addRewardItem(Inventory inventory, int slot, int level, int line, PlayerData playerData) {
        Player player = (Player) inventory.getHolder();
        boolean canClaim = rewardService.canClaimReward(level, line, playerData);
        boolean claimed = playerData.isRewardClaimed(rewardService.generateRewardId(level, line));
        
        String status;
        if (claimed) {
            status = plugin.getConfigManager().getSetting("menu.rewards_title.status.passed", player);
        } else if (canClaim) {
            status = plugin.getConfigManager().getSetting("menu.rewards_title.status.completed", player);
        } else {
            status = plugin.getConfigManager().getSetting("menu.rewards_title.status.need_to_go", player);
        }
        
        String materialKey = "menu.rewards_title.reward_item_konec." + line;
        String materialName = plugin.getConfigManager().getSetting(materialKey);
        Material material;
        try {
            material = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            material = Material.CHEST;
        }
        
        String name = plugin.getConfigManager().getSetting("menu.rewards_title.name", player);
        List<String> lore = Arrays.asList(status);
        
        ItemStack rewardItem = ItemBuilder.of(material)
                .name(name)
                .lore(lore)
                .build();
        
        inventory.setItem(slot, rewardItem);
    }
}
