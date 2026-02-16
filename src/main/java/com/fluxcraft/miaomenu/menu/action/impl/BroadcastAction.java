package com.fluxcraft.MiaoMenu.menu.action.impl;

import com.fluxcraft.MiaoMenu.menu.action.MenuAction;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BroadcastAction implements MenuAction {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();
    @Override
    public void execute(Player player, String content, Plugin plugin) {
        Bukkit.broadcast(SERIALIZER.deserialize(content));
    }
}
