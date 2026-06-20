package com.fluxcraft.MiaoMenu.commands.impl;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.commands.PluginCommand;
import com.fluxcraft.MiaoMenu.utils.Lang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OpenCommand implements PluginCommand {
    private final MiaoMenu plugin;
    public OpenCommand(MiaoMenu plugin) {
        this.plugin = plugin;
    }
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("dgeysermenu.use")) {
            sender.sendMessage(Lang.get("message.no-permission"));
            return;
        }
        if (args.length < 1) {
            sender.sendMessage(Lang.get("command.usage-open"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("message.players-only"));
            return;
        }
        plugin.openSmartMenu(player, args[0]);
    }
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> allMenus = new ArrayList<>();
        allMenus.addAll(plugin.getJavaMenuManager().getMenus().keySet());
        allMenus.addAll(plugin.getBedrockMenuManager().getMenus().keySet());
        if (args.length == 0) {
            return allMenus;
        }
        String prefix = args[0] != null ? args[0] : "";
        return allMenus.stream()
                .filter(s -> s.startsWith(prefix))
                .collect(Collectors.toList());
    }
}
