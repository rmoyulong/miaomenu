package com.fluxcraft.miaomenu.bedrockmenu;

import com.fluxcraft.miaomenu.MiaoMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.floodgate.api.FloodgateApi;

public class BedrockMenuListener implements Listener {

    private final MiaoMenu plugin;

    public BedrockMenuListener(MiaoMenu plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FloodgateApi api = FloodgateApi.getInstance();
        if (api != null && api.isFloodgatePlayer(player.getUniqueId())) {
        }
    }
}
