package com.fluxcraft.MiaoMenu.menu.action.impl;

import java.util.Arrays;
import java.util.logging.Logger;

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
        
        Logger logger = plugin.getLogger();
        logger.info("[PlayerAction] Executing command: " + cmd + " for player: " + player.getName());
        
        if (plugin instanceof MiaoMenu miaoMenu) {
            String[] parts = cmd.split("\\s+", 2);
            logger.info("[PlayerAction] Command parts: " + Arrays.toString(parts));
            
            if (parts.length > 0 && parts[0].equalsIgnoreCase("server") && parts.length > 1) {
                String serverName = parts[1].trim();
                logger.info("[PlayerAction] Detected server command with server: " + serverName);
                
                ProxyManager proxyManager = miaoMenu.getProxyManager();
                if (proxyManager != null) {
                    logger.info("[PlayerAction] ProxyManager exists, is connected: " + proxyManager.isProxyConnected());
                    
                    if (proxyManager.isProxyConnected()) {
                        logger.info("[PlayerAction] Attempting to send server command via proxy");
                        boolean success = proxyManager.sendServerCommand(player, serverName);
                        logger.info("[PlayerAction] Proxy command result: " + success);
                        
                        if (success) {
                            logger.info("[PlayerAction] Server command sent successfully via proxy, skipping local execution");
                            return;
                        }
                    } else {
                        logger.info("[PlayerAction] Proxy not connected, falling back to local command");
                    }
                } else {
                    logger.warning("[PlayerAction] ProxyManager is null");
                }
            }
        } else {
            logger.info("[PlayerAction] Plugin is not MiaoMenu instance");
        }
        
        logger.info("[PlayerAction] Falling back to local command execution: " + cmd);
        player.performCommand(cmd);
    }
}
