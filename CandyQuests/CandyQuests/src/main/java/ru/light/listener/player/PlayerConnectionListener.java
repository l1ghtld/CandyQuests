package ru.light.listener.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.light.CandyQuests;

public class PlayerConnectionListener implements Listener {
    
    private final CandyQuests plugin;

    public PlayerConnectionListener(CandyQuests plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().getPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().unloadPlayerData(event.getPlayer().getUniqueId());
    }
}
