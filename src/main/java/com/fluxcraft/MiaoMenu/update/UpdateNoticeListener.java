package com.fluxcraft.MiaoMenu.update;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.commands.impl.AboutCommand;
import com.fluxcraft.MiaoMenu.utils.Lang;

/**
 * 管理員上線時，若 {@link UpdateChecker} 確認有新版本，發出一條 actionable 訊息。
 * 只給有 {@code dgeysermenu.admin} 權限的玩家看；一般玩家不受打擾。
 */
public class UpdateNoticeListener implements Listener {
    private final MiaoMenu plugin;

    public UpdateNoticeListener(MiaoMenu plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("dgeysermenu.admin")) {
            return;
        }
        UpdateChecker checker = plugin.getUpdateChecker();
        if (checker == null || !checker.hasUpdate()) {
            return;
        }
        String latest = checker.getLatestVersion();
        String current = plugin.getPluginMeta().getVersion();
        player.sendMessage(Lang.get("about.update-available")
                .replace("{0}", latest)
                .replace("{1}", current)
                .replace("{2}", AboutCommand.MODRINTH_URL));
    }
}
