package com.fluxcraft.MiaoMenu.utils;

import java.util.logging.Level;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PlaceholderUtils {

    private PlaceholderUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String parse(Player player, String text, Plugin plugin) {
        if (text == null) {
            return "";
        }
        if (player != null) {
            text = text.replace("%player_name%", player.getName());
            text = text.replace("%player%", player.getName());
        }
        text = text.replace('&', '§');
        if (player != null && plugin != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                text = PlaceholderAPI.setPlaceholders(player, text);
            } catch (RuntimeException e) {
                // 抬升到 WARNING，否則 expansion 內部炸掉時 user 完全看不到失敗訊號，
                // 表面只會看到「佔位符顯示原文」進而誤判是 MiaoMenu 沒呼叫 PAPI。
                plugin.getLogger().log(Level.WARNING, "PlaceholderAPI 解析失敗（玩家 "
                        + player.getName() + "，文字 \"" + text + "\"）", e);
            }
        }
        return text;
    }
}
