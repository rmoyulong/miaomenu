package com.fluxcraft.MiaoMenu.commands;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.commands.impl.AboutCommand;
import com.fluxcraft.MiaoMenu.commands.impl.HelpCommand;
import com.fluxcraft.MiaoMenu.commands.impl.ImportCommand;
import com.fluxcraft.MiaoMenu.commands.impl.LangCommand;
import com.fluxcraft.MiaoMenu.commands.impl.OpenCommand;
import com.fluxcraft.MiaoMenu.commands.impl.ReloadCommand;
import com.fluxcraft.MiaoMenu.utils.Lang;

public class CommandManager implements CommandExecutor, TabCompleter {
    private static final String CMD_HELP = "help";

    private final Map<String, PluginCommand> commands = new LinkedHashMap<>();
    private final MiaoMenu plugin;
    private HelpCommand helpCommand;

    public CommandManager(@NotNull MiaoMenu plugin) {
        this.plugin = plugin;
        registerCommands();
    }

    private void registerCommands() {
        Map<String, String> helpDescriptions = loadHelpDescriptions();
        register("open", new OpenCommand(plugin));
        register("reload", new ReloadCommand(plugin));
        register("import", new ImportCommand(plugin));
        register("lang", new LangCommand(plugin));
        register("about", new AboutCommand(plugin));
        helpCommand = new HelpCommand(helpDescriptions);
        register(CMD_HELP, helpCommand);
    }

    /**
     * 載入指令說明：優先採用 lang/<language>.yml 的 descriptions 區塊，
     * 找不到時退回 config.yml 內舊版的 messages.descriptions（向後相容）。
     */
    @NotNull
    private Map<String, String> loadHelpDescriptions() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        for (String name : HelpCommand.defaultCommandOrder()) {
            String key = "descriptions." + name;
            String value = Lang.get(key);
            if (value != null && !value.equals(key)) {
                descriptions.put(name, value);
            }
        }
        ConfigurationSection legacy = plugin.getConfig().getConfigurationSection("messages.descriptions");
        if (legacy != null) {
            for (String key : legacy.getKeys(false)) {
                if (legacy.isString(key)) {
                    descriptions.putIfAbsent(key, legacy.getString(key));
                }
            }
        }
        if (descriptions.isEmpty()) {
            plugin.getLogger().warning(Lang.get("log.command.descriptions-missing"));
        }
        return descriptions;
    }

    /**
     * 給 {@link LangCommand} 在切換語系後熱替換 help 顯示用的描述快取，
     * 不會重建子指令實例（避免清掉 import 等指令的 session 狀態）。
     */
    public void reloadHelpDescriptions() {
        if (helpCommand != null) {
            helpCommand.setDescriptions(loadHelpDescriptions());
        }
    }

    private void register(@NotNull String name, @NotNull PluginCommand command) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Command name cannot be empty");
        }
        commands.put(name.toLowerCase(), command);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NonNull [] args) {
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
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, Lang.get("log.command.execute-failed")
                    .replace("{0}", commandName)
                    .replace("{1}", sender.getName()), e);
            sender.sendMessage(Lang.get("open.error"));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NonNull [] args) {
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
