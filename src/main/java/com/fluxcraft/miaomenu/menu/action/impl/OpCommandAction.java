package com.fluxcraft.miaomenu.menu.action.impl;

import com.fluxcraft.miaomenu.constants.Constants;
import com.fluxcraft.miaomenu.menu.action.MenuAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class OpCommandAction implements MenuAction {
    @Override
    public void execute(Player player, String content, Plugin plugin) {
        String cmd = Constants.stripLeadingSlash(content);
        Bukkit.getScheduler().runTask(plugin, () -> {
            boolean wasOp = player.isOp();
            try {
                player.setOp(true);
                player.performCommand(cmd);
            } finally {
                player.setOp(wasOp);
            }
        });
    }
}
