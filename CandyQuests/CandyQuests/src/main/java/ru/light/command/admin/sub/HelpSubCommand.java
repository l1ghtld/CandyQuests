package ru.light.command.admin.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.light.CandyQuests;
import ru.light.command.admin.SubCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HelpSubCommand implements SubCommand {
    
    private final CandyQuests plugin;

    public HelpSubCommand(CandyQuests plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Player player, String[] args) {
        for (String line : plugin.getConfigManager().getMessageList("admins.help")) {
            player.sendMessage(line);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
