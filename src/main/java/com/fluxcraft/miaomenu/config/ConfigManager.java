package com.fluxcraft.MiaoMenu.config;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.utils.Lang;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;

public class ConfigManager {
    private final MiaoMenu plugin;
    private static final int CONFIG_VERSION = 10;
    private static final int MENU_VERSION = 2;
    private static final String JAVA_MENUS_DIR = "java_menus";
    private static final String BEDROCK_MENUS_DIR = "bedrock_menus";
    private static final String MENU_VERSION_KEY = "menu-version";
    private static final String CRAFTENGINE_ENABLED_KEY = "settings.craftengine.enabled";
    private static final String CRAFTENGINE_FALLBACK_KEY = "settings.craftengine.fallback-material";
    public ConfigManager(MiaoMenu plugin) {
        this.plugin = plugin;
    }
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            FileConfiguration currentConfig = plugin.getConfig();
            int currentVersion = currentConfig.getInt("config-version", 0);
            if (currentVersion != CONFIG_VERSION) {
                File backupFile = new File(plugin.getDataFolder(), "config.yml.old");
                if (backupFile.exists()) {
                    if (!backupFile.delete()) {
                        plugin.getLogger().warning("Failed to delete old backup config.yml.old");
                    }
                }
                if (!configFile.renameTo(backupFile)) {
                    plugin.getLogger().warning("Failed to backup old config.yml, proceeding with overwrite.");
                }
                plugin.saveResource("config.yml", true);
                plugin.getLogger().info(Lang.get("message.config-updated"));
            }
        } else {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        initDirectory(JAVA_MENUS_DIR);
        initDirectory(BEDROCK_MENUS_DIR);
    }
    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
    public boolean isCraftEngineEnabled() {
        return getConfig().getBoolean(CRAFTENGINE_ENABLED_KEY, true);
    }
    public Material getCraftEngineFallbackMaterial() {
        String materialName = getConfig().getString(CRAFTENGINE_FALLBACK_KEY, "STONE");
        Material material = Material.matchMaterial(materialName);
        return material != null ? material : Material.STONE;
    }
    public void checkAndRefreshMenus() {
        FileConfiguration config = plugin.getConfig();
        int currentVersion = config.getInt(MENU_VERSION_KEY, 0);
        if (currentVersion != MENU_VERSION) {
            try {
                initDirectory(JAVA_MENUS_DIR);
                initDirectory(BEDROCK_MENUS_DIR);
                plugin.saveResource("bedrock_menus/test.yml", true);
                plugin.saveResource("java_menus/test.yml", true);
                config.set(MENU_VERSION_KEY, MENU_VERSION);
                config.save(new File(plugin.getDataFolder(), "config.yml"));
                plugin.reloadConfig();
                plugin.getLogger().info(Lang.get("message.reloaded"));
            } catch (Exception e) {
                plugin.getLogger().severe(Lang.get("message.io-error"));
            }
        } else {
            if (config.getBoolean("settings.auto-generate-examples", true)) {
                saveResourceIfNotExists("bedrock_menus/test.yml");
                saveResourceIfNotExists("java_menus/test.yml");
            }
        }
    }
    private void saveResourceIfNotExists(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
    }
    private void initDirectory(String dirName) {
        File dir = new File(plugin.getDataFolder(), dirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                plugin.getLogger().warning(Lang.get("message.io-error"));
            }
        }
    }
}
