package ru.light.listener.quest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import ru.light.CandyQuests;
import ru.light.service.quest.QuestProgressService;

public class QuestProgressListener implements Listener {
    
    private final CandyQuests plugin;
    private QuestProgressService progressService;
    
    public QuestProgressListener(CandyQuests plugin) {
        this.plugin = plugin;
        this.progressService = new QuestProgressService(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        progressService.handlePassItem(event.getPlayer(), event.getItemDrop().getItemStack());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            progressService.handleCraftItem((Player) event.getWhoClicked(), event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        progressService.handleBlockPlace(event.getPlayer(), event.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        progressService.handleBlockBreak(event.getPlayer(), event.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            progressService.handleEntityKill(event.getEntity().getKiller(), event.getEntity());
        }
    }
}
