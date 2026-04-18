package ru.light.service.quest;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import ru.light.CandyQuests;
import ru.light.model.PlayerData;
import ru.light.model.Quest;
import ru.light.model.enums.QuestType;

import java.util.List;

public class QuestProgressService {
    
    private final CandyQuests plugin;

    public QuestProgressService(CandyQuests plugin) {
        this.plugin = plugin;
    }

    public void handlePassItem(Player player, ItemStack item) {
        List<Quest> activeQuests = plugin.getQuestManager()
                .getActiveQuestsForMaterial(player.getUniqueId(), QuestType.PASS_ITEM, item.getType());
        
        if (activeQuests.isEmpty()) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Quest quest = activeQuests.get(0);
        
        int currentProgress = playerData.getProgress(quest.getId());
        int needed = quest.getNeeded();
        
        if (currentProgress >= needed) return;

        int amount = Math.min(item.getAmount(), needed - currentProgress);
        if (amount <= 0) return;

        playerData.addProgress(quest.getId(), amount);
        item.setAmount(item.getAmount() - amount);

        checkQuestCompletion(player, quest, playerData);
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
    }

    public void handleCraftItem(Player player, CraftItemEvent event) {
        ItemStack item = event.getRecipe().getResult();
        List<Quest> activeQuests = plugin.getQuestManager()
                .getActiveQuestsForMaterial(player.getUniqueId(), QuestType.CRAFT_ITEM, item.getType());
        
        if (activeQuests.isEmpty()) return;

        int craftedAmount = item.getAmount();
        if (event.isShiftClick()) {
            int maxCraftable = getMaxCraftableAmount(event);
            craftedAmount = maxCraftable * item.getAmount();
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        Quest quest = activeQuests.get(0);
        
        playerData.addProgress(quest.getId(), craftedAmount);
        checkQuestCompletion(player, quest, playerData);
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
    }

    public void handleBlockPlace(Player player, Material material) {
        List<Quest> activeQuests = plugin.getQuestManager()
                .getActiveQuestsForMaterial(player.getUniqueId(), QuestType.PLACE_BLOCK, material);
        
        if (activeQuests.isEmpty()) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        for (Quest quest : activeQuests) {
            processBlockQuest(player, quest, playerData, 1);
        }
    }

    public void handleBlockBreak(Player player, Material material) {
        List<Quest> activeQuests = plugin.getQuestManager()
                .getActiveQuestsForMaterial(player.getUniqueId(), QuestType.BREAK_BLOCK, material);
        
        if (activeQuests.isEmpty()) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        
        for (Quest quest : activeQuests) {
            processBlockQuest(player, quest, playerData, 1);
        }
    }

    public void handleEntityKill(Player killer, Entity entity) {
        List<Quest> activeQuests;
        
        if (entity instanceof Player) {
            activeQuests = plugin.getQuestManager()
                    .getActiveQuestsForPlayer(killer.getUniqueId(), QuestType.KILL_PLAYER);
        } else {
            activeQuests = plugin.getQuestManager()
                    .getActiveQuestsForEntity(killer.getUniqueId(), QuestType.KILL_MOB, entity.getType());
        }
        
        if (activeQuests.isEmpty()) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(killer.getUniqueId());
        
        for (Quest quest : activeQuests) {
            processKillQuest(killer, quest, playerData);
        }
    }

    private void processBlockQuest(Player player, Quest quest, PlayerData playerData, int amount) {
        if (playerData.isQuestCompleted(quest.getId())) return;
        
        playerData.addProgress(quest.getId(), amount);
        checkQuestCompletion(player, quest, playerData);
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
    }

    private void processKillQuest(Player player, Quest quest, PlayerData playerData) {
        if (playerData.isQuestCompleted(quest.getId())) return;
        
        playerData.addProgress(quest.getId(), 1);
        checkQuestCompletion(player, quest, playerData);
        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
    }

    private void checkQuestCompletion(Player player, Quest quest, PlayerData playerData) {
        if (playerData.getProgress(quest.getId()) >= quest.getNeeded()) {
            playerData.completeQuest(quest.getId());
            
            for (String line : plugin.getConfigManager().getMessageListOrSingle("quests.completed")) {
                player.sendMessage(line);
            }
        }
    }

    private int getMaxCraftableAmount(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        int minAmount = Integer.MAX_VALUE;
        
        for (ItemStack ingredient : matrix) {
            if (ingredient != null && ingredient.getAmount() > 0) {
                minAmount = Math.min(minAmount, ingredient.getAmount());
            }
        }
        
        return minAmount == Integer.MAX_VALUE ? 1 : minAmount;
    }
}
