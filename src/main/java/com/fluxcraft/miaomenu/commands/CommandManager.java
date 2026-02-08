package com.fluxcraft.MiaoMenu.commands;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.commands.impl.*;
import com.fluxcraft.MiaoMenu.utils.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private static final String CMD_HELP = "help";
    private final Map<String, PluginCommand> commands = new LinkedHashMap<>();
    private final MiaoMenu plugin;

    public CommandManager(@NotNull MiaoMenu plugin) {
        this.plugin = plugin;
        register("open", new OpenCommand(plugin));
        register("reload", new ReloadCommand(plugin));
        register(CMD_HELP, new HelpCommand(List.copyOf(commands.keySet())));
    }
    private void register(@NotNull String name, @NotNull PluginCommand command) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be empty");
        }
        commands.put(name.toLowerCase(), command);
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args == null) {
            args = new String[0];
        }
        if (args.length == 0) {
            dispatchCommand(sender, CMD_HELP, new String[0]);
            return true;
        }
        String subCommandName = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        dispatchCommand(sender, subCommandName, subArgs);
        return true;
    }
    private void dispatchCommand(@NotNull CommandSender sender, @NotNull String commandName, @NotNull String[] args) {
        PluginCommand target = commands.get(commandName);
        if (target == null) {
            sender.sendMessage(Lang.get("command.unknown"));
            dispatchCommand(sender, CMD_HELP, new String[0]);
            return;
        }
        try {
            target.execute(sender, args);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "An unexpected error occurred while executing command: /" + commandName, e);
            sender.sendMessage("§cAn internal error occurred while executing this command. Please check the console logs.");
        }
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args == null) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return commands.keySet().stream()
                    .filter(key -> key.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }
        if (args.length > 1) {
            PluginCommand target = commands.get(args[0].toLowerCase());
            if (target != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                List<String> completion = target.tabComplete(sender, subArgs);
                return completion != null ? completion : Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }
}