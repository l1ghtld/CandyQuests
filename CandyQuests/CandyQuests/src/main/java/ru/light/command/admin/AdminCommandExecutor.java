package ru.light.command.admin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.light.CandyQuests;
import ru.light.command.admin.sub.*;

import java.util.*;

public class AdminCommandExecutor implements CommandExecutor, TabCompleter {

    private final CandyQuests plugin;
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public AdminCommandExecutor(CandyQuests plugin) {
        this.plugin = plugin;
        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put("help", new HelpSubCommand(plugin));
        subCommands.put("reset", new ResetSubCommand(plugin));
        subCommands.put("reward", new RewardSubCommand(plugin));
        subCommands.put("skip", new SkipAllSubCommand(plugin));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        
        Player player = (Player) sender;

        if (!player.hasPermission("quests.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("admins.no_perms"));
            return true;
        }

        if (args.length == 0) {
            executeSubCommand(player, "help", new String[0]);
            return true;
        }

        String subName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        executeSubCommand(player, subName, subArgs);
        return true;
    }

    private void executeSubCommand(Player player, String subName, String[] args) {
        SubCommand subCommand = subCommands.get(subName);
        if (subCommand != null) {
            subCommand.execute(player, args);
        } else {
            subCommands.get("help").execute(player, args);
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (!sender.hasPermission("quests.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return new ArrayList<>(Arrays.asList("help", "reward", "reset", "skip"));
        }

        String subName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subName);
        
        if (subCommand != null) {
            return subCommand.tabComplete(sender, args);
        }

        return Collections.emptyList();
    }
}
