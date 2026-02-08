package com.fluxcraft.MiaoMenu.bedrockmenu;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.menu.action.ActionRegistry;
import com.fluxcraft.MiaoMenu.utils.Lang;
import com.fluxcraft.MiaoMenu.utils.PlaceholderUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BedrockMenuManager {
    private final MiaoMenu plugin;
    private final ActionRegistry actionRegistry;
    private final Map<String, BedrockMenu> menus = new ConcurrentHashMap<>();
    public BedrockMenuManager(MiaoMenu plugin, ActionRegistry actionRegistry) {
        this.plugin = plugin;
        this.actionRegistry = actionRegistry;
    }
    public void loadAllMenus() {
        menus.clear();
        File dir = new File(plugin.getDataFolder(), "bedrock_menus");
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                String name = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                menus.put(name, new BedrockMenu(name, config, plugin));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load Bedrock menu: " + file.getName());
            }
        }
    }
    public void openMenu(Player player, String menuName) {
        if (!Bukkit.getPluginManager().isPluginEnabled("Geyser-Floodgate")) {
            player.sendMessage(Lang.get("message.players-only") + " (Geyser not detected)");
            return;
        }
        try {
            if (!org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                player.sendMessage(Lang.get("message.players-only"));
                return;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking Floodgate player status: " + e.getMessage());
            return;
        }
        BedrockMenu menu = menus.get(menuName);
        if (menu == null) {
            player.sendMessage(Lang.get("message.menu-not-found") + menuName);
            return;
        }
        org.geysermc.floodgate.api.FloodgateApi.getInstance().sendForm(player.getUniqueId(), menu.buildForm(player).validResultHandler(response -> {
            int clickedIndex = response.clickedButtonId();
            if (clickedIndex >= 0 && clickedIndex < menu.getMenuItems().size()) {
                BedrockMenu.BedrockMenuItem item = menu.getMenuItems().get(clickedIndex);
                handleItemClick(player, item);
            }
        }));
    }
    private void handleItemClick(Player player, BedrockMenu.BedrockMenuItem item) {
        String cmd = item.getCommand();
        if (cmd == null || cmd.isEmpty()) {
            return;
        }
        String parsed = PlaceholderUtils.parse(player, cmd, plugin);
        actionRegistry.dispatch(player, parsed);
    }
    public Map<String, BedrockMenu> getMenus() { return menus; }
}
