package ru.light.listener.inventory.handler;

import org.bukkit.entity.Player;
import ru.light.CandyQuests;
import ru.light.model.PlayerData;
import ru.light.service.reward.RewardService;

public class RewardClickHandler {
    
    private final CandyQuests plugin;
    private final RewardService rewardService;

    public RewardClickHandler(CandyQuests plugin) {
        this.plugin = plugin;
        this.rewardService = new RewardService(plugin);
    }

    public void handleClick(Player player, int slot) {
        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());

        int level = getLevelFromTitle(player.getOpenInventory().getTitle());
        int line = getLineFromSlot(slot);
        int rewardId = rewardService.generateRewardId(level, line);

        if (playerData.isRewardClaimed(rewardId)) {
            player.sendMessage(plugin.getConfigManager().getMessage("rewards.passed"));
            return;
        }

        if (!rewardService.canClaimReward(level, line, playerData)) {
            player.sendMessage(plugin.getConfigManager().getMessage("rewards.need_to_go"));
            return;
        }

        plugin.getRewardManager().giveReward(player, level, line);
        playerData.claimReward(rewardId);
        
        for (String line2 : plugin.getConfigManager().getMessageListOrSingle("rewards.completed")) {
            player.sendMessage(line2);
        }

        plugin.getPlayerDataManager().savePlayerData(player.getUniqueId());
        player.closeInventory();
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

    private int getLineFromSlot(int slot) {
        String[] rewardSlots = plugin.getConfigManager().getSetting("menu.quests_settings.rewards").split(",");
        for (int i = 0; i < rewardSlots.length; i++) {
            if (String.valueOf(slot).equals(rewardSlots[i].trim())) {
                return i + 1;
            }
        }
        return 1;
    }
}
