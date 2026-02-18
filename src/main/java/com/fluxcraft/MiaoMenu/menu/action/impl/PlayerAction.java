package com.fluxcraft.MiaoMenu.menu.action.impl;

import com.fluxcraft.MiaoMenu.constants.Constants;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerAction implements com.fluxcraft.MiaoMenu.menu.action.MenuAction {
    @Override
    public void execute(Player player, String content, Plugin plugin) {
        if (content == null || content.isEmpty()) return;
        String cmd = Constants.stripLeadingSlash(content);
        player.performCommand(cmd);
    }
}
