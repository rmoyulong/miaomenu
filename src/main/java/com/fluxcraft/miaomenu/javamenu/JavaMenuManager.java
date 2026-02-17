package com.fluxcraft.MiaoMenu.javamenu;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.integration.CraftEngineIntegration;
import com.fluxcraft.MiaoMenu.utils.Lang;
import com.fluxcraft.MiaoMenu.managers.SoundsClock;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavaMenuManager {
    private final MiaoMenu plugin;
    private final CraftEngineIntegration craftEngineIntegration;
    private final SoundsClock soundsClock;
    private volatile Map<String, JavaMenu> menus = Collections.emptyMap();
    public JavaMenuManager(MiaoMenu plugin, CraftEngineIntegration craftEngineIntegration, SoundsClock soundsClock) {
        this.plugin = plugin;
        this.craftEngineIntegration = craftEngineIntegration;
        this.soundsClock = soundsClock;
    }
    public void loadAllMenus() {
        Map<String, JavaMenu> newMenus = new ConcurrentHashMap<>();
        File dir = new File(plugin.getDataFolder(), "java_menus");
        if (!dir.exists() && !dir.mkdirs()) {
            plugin.getLogger().warning("Failed to create java_menus directory");
            return;
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File file : files) {
            try {
                String name = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                newMenus.put(name, new JavaMenu(config, plugin, craftEngineIntegration));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load menu: " + file.getName());
            }
        }
        this.menus = newMenus;
    }
    public void openMenu(Player player, String menuName) {
        JavaMenu menu = menus.get(menuName);
        if (menu == null) {
            player.sendMessage(Lang.get("message.menu-not-found") + menuName);
            return;
        }
        soundsClock.playMenuOpenSound(player);
        try {
            menu.open(player);
        } catch (Exception e) {
            player.sendMessage(Lang.get("open.error"));
            plugin.getLogger().severe("Error opening menu " + menuName + ": " + e.getMessage());
        }
    }
    public Map<String, JavaMenu> getMenus() {
        return Collections.unmodifiableMap(menus);
    }
}
