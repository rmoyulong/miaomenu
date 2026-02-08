package com.fluxcraft.MiaoMenu.config;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.util.logging.Level;

public class ConfigManager {
    private final MiaoMenu plugin;
    private static final String JAVA_MENUS_DIR = "java_menus";
    private static final String BEDROCK_MENUS_DIR = "bedrock_menus";
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
