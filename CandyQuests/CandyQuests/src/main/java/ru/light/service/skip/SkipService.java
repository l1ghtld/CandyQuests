package ru.light.service.skip;

import org.bukkit.entity.Player;

public class SkipService {
    
    public static int getMaxSkips(Player player) {
        for (int i = 999; i >= 1; i--) {
            if (player.hasPermission("candyquests.skipquest." + i)) {
                return i;
            }
        }
        return 0;
    }
    
    public static boolean canSkip(Player player, int currentSkips) {
        int maxSkips = getMaxSkips(player);
        return maxSkips > 0 && currentSkips < maxSkips;
    }
    
    public static int getRemainingSkips(Player player, int currentSkips) {
        int maxSkips = getMaxSkips(player);
        return Math.max(0, maxSkips - currentSkips);
    }
}
