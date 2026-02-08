package com.fluxcraft.MiaoMenu.commands.impl;

import com.fluxcraft.MiaoMenu.commands.PluginCommand;
import com.fluxcraft.MiaoMenu.utils.Lang;
import org.bukkit.command.CommandSender;

import java.util.List;

public class HelpCommand implements PluginCommand {
    private final List<String> subCommands;

    public HelpCommand(List<String> subCommands) {
        this.subCommands = subCommands;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(Lang.get("message.help.header"));
        sender.sendMessage("§f可用子命令: " + String.join("§7, §f", subCommands));
        sender.sendMessage(Lang.get("message.help.usage"));
    }
}
