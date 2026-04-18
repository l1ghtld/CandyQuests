package ru.light.listener.inventory.handler;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.light.CandyQuests;
import ru.light.model.PlayerData;
import ru.light.model.Quest;
import ru.light.model.enums.QuestType;

import java.util.List;

public class QuestClickHandler {
    
    private final CandyQuests plugin;

    public QuestClickHandler(CandyQuests plugin) {
        this.plugin = plugin;
    }

    public void handleClick(Player player, int slot, ItemStack clickedItem) {
        if (clickedItem.getType() == Material.BARRIER) return;

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int level = getLevelFromTitle(player.getOpenInventory().getTitle());

        Quest quest = getQuestFromSlot(level, slot);
        if (quest == null) return;

        if (!plugin.getQuestManager().isQuestUnlocked(quest.getId(), player.getUniqueId())) {
            return;
        }

        if (playerData.isQuestCompleted(quest.getId())) {
            player.sendMessage(plugin.getConfigManager().getMessage("quests.passed"));
            return;
        }
        
        if (quest.getType() != QuestType.PASS_ITEM) {
            return;
        }

        processPassItemQuest(player, quest, playerData);
    }

    private void processPassItemQuest(Player player, Quest quest, PlayerData playerData) {
        ItemStack[] contents = player.getInventory().getContents();
        int totalAmount = 0;
        
        for (ItemStack invItem : contents) {
            if (invItem != null && invItem.getType() == quest.getMaterial()) {
                totalAmount += invItem.getAmount();
            }
        }

        if (totalAmount == 0) return;

        int currentProgress = playerData.getProgress(quest.getId());
        int questNeeded = quest.getNeeded();
        int stillNeeded = questNeeded - currentProgress;

        if (stillNeeded <= 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("quests.passed"));
            return;
        }

        int toSubmit = Math.min(totalAmount, stillNeeded);
        int removed = 0;
        
        for (ItemStack invItem : contents) {
            if (invItem != null && invItem.getType() == quest.getMaterial() && removed < toSubmit) {
                int removeFromStack = Math.min(invItem.getAmount(), toSubmit - removed);
                invItem.setAmount(invItem.getAmount() - removeFromStack);
                removed += removeFromStack;

                if (invItem.getAmount() <= 0) {
                    invItem.setType(Material.AIR);
                }

                if (removed >= toSubmit) break;
            }
        }

        playerData.addProgress(quest.getId(), toSubmit);
        int newProgress = playerData.getProgress(quest.getId());
        
        if (newProgress >= questNeeded) {
            playerData.completeQuest(quest.getId());
            for (String line : plugin.getConfigManager().getMessageListOrSingle("quests.completed")) {
                player.sendMessage(line);
            }
        }

        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        
        int currentLevel = getLevelFromTitle(player.getOpenInventory().getTitle());
        player.closeInventory();
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.performCommand("quest" + currentLevel);
        });
    }

    private Quest getQuestFromSlot(int level, int slot) {
        String questSlotsStr = plugin.getConfigManager().getSetting("menu.quests_settings.quests");
        String[] questSlots = questSlotsStr.split("-");

        if (questSlots.length != 2) return null;

        int startSlot = Integer.parseInt(questSlots[0]);
        int endSlot = Integer.parseInt(questSlots[1]);

        if (slot < startSlot || slot > endSlot) return null;
        
        List<Quest> quests = plugin.getQuestManager().getQuestsForLevel(level);
        String[] rewardSlotsStr = plugin.getConfigManager().getSetting("menu.quests_settings.rewards").split(",");
        
        int questIndex = 0;
        for (int currentSlot = startSlot; currentSlot < slot; currentSlot++) {
            boolean isRewardSlot = false;
            for (String rewardSlot : rewardSlotsStr) {
                if (String.valueOf(currentSlot).equals(rewardSlot.trim())) {
                    isRewardSlot = true;
                    break;
                }
            }
            if (!isRewardSlot) {
                questIndex++;
            }
        }

        for (String rewardSlot : rewardSlotsStr) {
            if (String.valueOf(slot).equals(rewardSlot.trim())) {
                return null;
            }
        }

        if (questIndex >= 0 && questIndex < quests.size()) {
            Quest quest = quests.get(questIndex);
            int questLevel = quest.getId() / 100;
            if (questLevel != level) {
                return null;
            }
            return quest;
        }

        return null;
    }

    private int getLevelFromTitle(String title) {
        for (int i = 1; i <= 3; i++) {
            String menuName = plugin.getConfigManager().getSetting("menu.names." + i);
            if (title.equals(menuName)) {
                return i;
            }
        }
        return 1;
    }
}
