package ru.light.listener.inventory;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.light.CandyQuests;
import ru.light.listener.inventory.handler.QuestClickHandler;
import ru.light.listener.inventory.handler.RewardClickHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QuestInventoryListener implements Listener {
    
    private final CandyQuests plugin;
    private QuestClickHandler questClickHandler;
    private RewardClickHandler rewardClickHandler;
    
    private static final Set<InventoryAction> BLOCKED_ACTIONS = new HashSet<>(Arrays.asList(
            InventoryAction.PLACE_ALL, InventoryAction.PLACE_ONE, InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_MOVE_AND_READD, InventoryAction.HOTBAR_SWAP,
            InventoryAction.CLONE_STACK, InventoryAction.COLLECT_TO_CURSOR
    ));

    public QuestInventoryListener(CandyQuests plugin) {
        this.plugin = plugin;
        this.questClickHandler = new QuestClickHandler(plugin);
        this.rewardClickHandler = new RewardClickHandler(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory topInventory = event.getView().getTopInventory();
        
        if (!(topInventory.getHolder() instanceof Player)) return;
        if (topInventory.getSize() != 54) return;
        
        event.setCancelled(true);

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !clickedInventory.equals(topInventory)) {
            return;
        }
        
        int slot = event.getSlot();
        
        if (isRewardSlot(slot)) {
            handleRewardSlotClick(player, event, slot);
        } else {
            handleQuestSlotClick(player, event, slot);
        }
    }

    private boolean isRewardSlot(int slot) {
        String[] rewardSlots = plugin.getConfigManager().getSetting("menu.quests_settings.rewards").split(",");
        for (String rewardSlot : rewardSlots) {
            if (String.valueOf(slot).equals(rewardSlot.trim())) {
                return true;
            }
        }
        return false;
    }

    private void handleRewardSlotClick(Player player, InventoryClickEvent event, int slot) {
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            return;
        }
        
        if (BLOCKED_ACTIONS.contains(event.getAction())) {
            return;
        }
        
        if (event.getAction() == InventoryAction.PICKUP_ALL || 
            event.getAction() == InventoryAction.PICKUP_ONE || 
            event.getAction() == InventoryAction.PICKUP_HALF ||
            event.getAction() == InventoryAction.PICKUP_SOME) {
            
            ItemStack item = event.getCurrentItem();
            if (item != null && item.hasItemMeta()) {
                rewardClickHandler.handleClick(player, slot);
            }
        }
    }

    private void handleQuestSlotClick(Player player, InventoryClickEvent event, int slot) {
        ItemStack item = event.getCurrentItem();
        if (item != null && item.hasItemMeta()) {
            questClickHandler.handleClick(player, slot, item);
        }
    }
}
