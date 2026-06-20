package com.fluxcraft.MiaoMenu.menu.action.impl;

import com.fluxcraft.MiaoMenu.constants.Constants;
import com.fluxcraft.MiaoMenu.foliacall.FoliaFactory;
import com.fluxcraft.MiaoMenu.security.InputValidator;
import com.fluxcraft.MiaoMenu.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CmdAction implements com.fluxcraft.MiaoMenu.menu.action.MenuAction {
    @Override
    public void execute(Player player, String content, Plugin plugin) {
        if (content == null || content.isEmpty()) return;
        String cmd = Constants.stripLeadingSlash(content);
        if (!InputValidator.isSafeCommandContent(cmd)) {
            player.sendMessage(Lang.get("message.unsafe-input"));
            plugin.getLogger().warning("[cmd] 拒絕執行不安全指令內容（player=" + player.getName() + "）：" + cmd);
            return;
        }
        if (FoliaFactory.isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            );
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
    }
}