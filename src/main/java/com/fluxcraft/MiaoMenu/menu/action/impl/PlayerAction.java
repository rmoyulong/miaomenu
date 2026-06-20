package com.fluxcraft.MiaoMenu.menu.action.impl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.constants.Constants;
import com.fluxcraft.MiaoMenu.proxy.ProxyManager;

public class PlayerAction implements com.fluxcraft.MiaoMenu.menu.action.MenuAction {
    @Override
    public void execute(Player player, String content, Plugin plugin) {
        if (content == null || content.isEmpty()) return;
        String cmd = Constants.stripLeadingSlash(content);

        // 與 Paper 處理 client chat packet 時印的 `issued server command:` 對齊；
        // 因 player.performCommand() / ProxyManager.sendServerCommand() 都不會印這行，
        // 走選單觸發的指令在主控台會默默消失，難以稽核。
        Bukkit.getLogger().info(player.getName() + " issued server command: /" + cmd);

        if (plugin instanceof MiaoMenu miaoMenu) {
            String[] parts = cmd.split("\\s+", 2);

            if (parts.length > 0 && parts[0].equalsIgnoreCase("server") && parts.length > 1) {
                String serverName = parts[1].trim();
                ProxyManager proxyManager = miaoMenu.getProxyManager();
                if (proxyManager != null && proxyManager.isProxyConnected()) {
                    boolean success = proxyManager.sendServerCommand(player, serverName);
                    if (success) return;
                }
            }
        }

        player.performCommand(cmd);
    }
}
