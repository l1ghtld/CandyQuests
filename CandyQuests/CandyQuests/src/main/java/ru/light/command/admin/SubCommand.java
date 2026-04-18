package ru.light.command.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public interface SubCommand {
    
    void execute(Player player, String[] args);
    
    default List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
