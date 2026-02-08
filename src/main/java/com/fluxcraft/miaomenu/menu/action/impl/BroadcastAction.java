package com.fluxcraft.MiaoMenu.menu.action.impl;

import com.fluxcraft.MiaoMenu.menu.action.MenuAction;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BroadcastAction implements MenuAction {
    @Override
    public void execute(Player player, String content, Plugin plugin) {
        Bukkit.broadcastMessage(content);
    }
}
