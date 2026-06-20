package com.fluxcraft.MiaoMenu.managers;

import java.util.logging.Level;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import org.bukkit.entity.Player;

public class SoundsClock {
    private final MiaoMenu plugin;
    public SoundsClock(MiaoMenu plugin) {
        this.plugin = plugin;
    }
    public void playMenuOpenSound(Player player) {
        if (!plugin.isEnabled() || !plugin.getConfig().getBoolean("settings.open-menu-sound.enabled", false)) {
            return;
        }
        String soundName = plugin.getConfig().getString("settings.open-menu-sound.sound", "entity.experience_orb.pickup");
        float volume = (float) plugin.getConfig().getDouble("settings.open-menu-sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("settings.open-menu-sound.pitch", 1.0);
        try {
            player.getScheduler().run(plugin, task -> player.playSound(player.getLocation(), soundName, org.bukkit.SoundCategory.MASTER, volume, pitch), null);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // 插件停用瞬間或聲音名稱錯誤時不該讓整個開啟流程中斷。
            plugin.getLogger().log(Level.FINE, "playMenuOpenSound skipped: " + e.getMessage());
        }
    }
}