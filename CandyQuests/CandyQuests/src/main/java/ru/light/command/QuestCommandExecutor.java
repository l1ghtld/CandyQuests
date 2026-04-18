package ru.light.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.light.CandyQuests;
import ru.light.command.admin.AdminCommandExecutor;
import ru.light.command.handler.QuestMenuHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuestCommandExecutor implements CommandExecutor, TabCompleter {

    private final CandyQuests plugin;
    private AdminCommandExecutor adminHandler;
    private QuestMenuHandler menuHandler;

    public QuestCommandExecutor(CandyQuests plugin) {
        this.plugin = plugin;
        this.adminHandler = new AdminCommandExecutor(plugin);
        this.menuHandler = new QuestMenuHandler(plugin);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("quest")) {
            return handleQuestCommand(sender, command, label, args);
        }

        if (cmd.matches("quest[123]")) {
            return handleLevelCommand(sender, cmd);
        }

        return true;
    }

    private boolean handleQuestCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getConfigManager().getMessage("commands.invalid_command"));
                return true;
            }
            return menuHandler.openCurrentLevelMenu((Player) sender);
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("admin")) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return adminHandler.onCommand(sender, command, label, subArgs);
        }

        sender.sendMessage(plugin.getConfigManager().getMessage("commands.invalid_command"));
        return true;
    }

    private boolean handleLevelCommand(CommandSender sender, String cmd) {
        if (!(sender instanceof Player)) return false;
        
        int level = Integer.parseInt(cmd.substring(5));
        return menuHandler.openLevelMenu((Player) sender, level);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        String cmd = command.getName().toLowerCase();
        
        if (cmd.equals("quest")) {
            if (args.length == 1 && sender.hasPermission("quests.admin")) {
                List<String> completions = new ArrayList<>();
                completions.add("admin");
                return completions;
            }
            
            if (args.length >= 2 && args[0].equalsIgnoreCase("admin")) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return adminHandler.onTabComplete(sender, command, alias, subArgs);
            }
        }

        return new ArrayList<>();
    }
}
