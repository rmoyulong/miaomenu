package com.fluxcraft.MiaoMenu.bedrockmenu;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.menu.action.ActionRegistry;
import com.fluxcraft.MiaoMenu.utils.Lang;
import com.fluxcraft.MiaoMenu.utils.PlaceholderUtils;
import com.fluxcraft.MiaoMenu.managers.SoundsClock;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BedrockMenuManager {
    private final MiaoMenu plugin;
    private final ActionRegistry actionRegistry;
    private final SoundsClock soundsClock;
    private final Map<String, BedrockMenu> menus = new ConcurrentHashMap<>();
    public BedrockMenuManager(MiaoMenu plugin, ActionRegistry actionRegistry, SoundsClock soundsClock) {
        this.plugin = plugin;
        this.actionRegistry = actionRegistry;
        this.soundsClock = soundsClock;
    }
    public void loadAllMenus() {
        menus.clear();
        File dir = new File(plugin.getDataFolder(), "bedrock_menus");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().severe("Failed to create bedrock_menus directory! Please check file permissions.");
            return;
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                String name = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                menus.put(name, new BedrockMenu(name, config, plugin));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load Bedrock menu: " + file.getName() + " | Reason: " + e.getMessage());
            }
        }
    }
    public void openMenu(Player player, String menuName) {
        BedrockMenu menu = menus.get(menuName);
        if (menu == null) {
            player.sendMessage(Lang.get("message.menu-not-found") + menuName);
            return;
        }
        soundsClock.playMenuOpenSound(player);
        try {
            FloodgateSender.send(plugin, player, menu, actionRegistry);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to send form '" + menuName + "' to Bedrock player " + player.getName(), e);
            player.sendMessage(Lang.get("open.error"));
        }
    }
    private static class FloodgateSender {
        static void send(MiaoMenu plugin, Player player, BedrockMenu menu, ActionRegistry actionRegistry) {
            org.geysermc.floodgate.api.FloodgateApi api = org.geysermc.floodgate.api.FloodgateApi.getInstance();
            api.sendForm(player.getUniqueId(), menu.buildForm(player).validResultHandler(response -> {
                int clickedIndex = response.clickedButtonId();
                if (clickedIndex < menu.getMenuItems().size()) {
                    BedrockMenu.BedrockMenuItem item = menu.getMenuItems().get(clickedIndex);
                    String cmd = item.getCommand();
                    if (cmd != null && !cmd.isEmpty()) {
                        String parsed = PlaceholderUtils.parse(player, cmd, plugin);
                        actionRegistry.dispatch(player, parsed);
                    }
                }
            }));
        }
    }
    public Map<String, BedrockMenu> getMenus() {
        return Collections.unmodifiableMap(menus);
    }
}