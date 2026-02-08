package com.fluxcraft.MiaoMenu.menu.action.impl;

import com.fluxcraft.MiaoMenu.constants.Constants;
import com.fluxcraft.MiaoMenu.menu.action.MenuAction;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerCommandAction implements MenuAction {
    @Override
    public void execute(Player player, String content, Plugin plugin) {
        String cmd = Constants.stripLeadingSlash(content);
        player.performCommand(cmd);
    }
}