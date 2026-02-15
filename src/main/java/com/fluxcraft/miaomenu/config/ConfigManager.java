package com.fluxcraft.MiaoMenu.config;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.util.logging.Level;

public class ConfigManager {
    private final MiaoMenu plugin;
    private static final String JAVA_MENUS_DIR = "java_menus";
    private static final String BEDROCK_MENUS_DIR = "bedrock_menus";
    private static final String MENU_VERSION_KEY = "menu-version";

    public ConfigManager(MiaoMenu plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        initDirectory(JAVA_MENUS_DIR);
        initDirectory(BEDROCK_MENUS_DIR);
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }
    public void checkAndRefreshMenus(int targetVersion) {
        FileConfiguration config = plugin.getConfig();
        int currentVersion = config.getInt(MENU_VERSION_KEY, 0);

        if (currentVersion < targetVersion) {
            plugin.getLogger().info("检测到菜单配置文件已更新 (v" + currentVersion + " -> v" + targetVersion + ")，正在同步默认菜单...");

            try {
                initDirectory(JAVA_MENUS_DIR);
                initDirectory(BEDROCK_MENUS_DIR);
                plugin.saveResource("bedrock_menus/test.yml", true);
                plugin.saveResource("java_menus/test.yml", true);
                config.set(MENU_VERSION_KEY, targetVersion);
                plugin.saveConfig();

                plugin.getLogger().info("默认菜单文件同步完成。");
                Bukkit.getConsoleSender().sendMessage("请注意：如果你修改过默认的 test.yml，你的修改已被新版本覆盖。");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "更新默认菜单文件时发生错误！", e);
            }
        } else {
            if (plugin.getConfig().getBoolean("settings.auto-generate-examples", true)) {
                saveResourceIfNotExists("bedrock_menus/test.yml");
                saveResourceIfNotExists("java_menus/test.yml");
            }
        }
    }
    private void saveResourceIfNotExists(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
            plugin.getLogger().info("已自动生成示例文件: " + path);
        }
    }
    private void initDirectory(String dirName) {
        File dir = new File(plugin.getDataFolder(), dirName);
        if (!dir.exists()) {
            boolean success = dir.mkdirs();
            if (!success) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create directory: " + dir.getAbsolutePath());
            }
        }
    }
}