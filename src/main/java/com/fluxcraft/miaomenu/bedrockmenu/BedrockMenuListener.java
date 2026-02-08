package com.fluxcraft.MiaoMenu.bedrockmenu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.geysermc.floodgate.api.FloodgateApi;

public class BedrockMenuListener implements Listener {

    public BedrockMenuListener() {
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FloodgateApi api = FloodgateApi.getInstance();
        if (api != null) {
            api.isFloodgatePlayer(player.getUniqueId());
        }
    }
}
