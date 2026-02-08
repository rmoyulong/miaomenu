package com.fluxcraft.MiaoMenu;

import com.fluxcraft.MiaoMenu.commands.CommandManager;
import com.fluxcraft.MiaoMenu.config.ConfigManager;
import com.fluxcraft.MiaoMenu.javamenu.JavaMenuListener;
import com.fluxcraft.MiaoMenu.javamenu.JavaMenuManager;
import com.fluxcraft.MiaoMenu.menu.action.ActionRegistry;
import com.fluxcraft.MiaoMenu.managers.HotReloadManager;
import com.fluxcraft.MiaoMenu.managers.MenuClockManager;
import com.fluxcraft.MiaoMenu.bedrockmenu.BedrockMenuListener;
import com.fluxcraft.MiaoMenu.bedrockmenu.BedrockMenuManager;
import com.fluxcraft.MiaoMenu.listeners.ClockInteractionListener;
import com.fluxcraft.MiaoMenu.listeners.PlayerLifecycleListener;
import com.fluxcraft.MiaoMenu.utils.Lang;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

import com.fluxcraft.MiaoMenu.listeners.TestXiaFa;

public final class MiaoMenu extends JavaPlugin {

    private static final int CONFIG_VERSION = 4;
    private static final int BSTATS_ID = 28979;
    public static final int JOIN_DELAY_TICKS = 20;
    private static final String BSTATS_METRICS_CLASS = "com.fluxcraft.MiaoMenu.libs.bstats.bukkit.Metrics";
    private static final String BSTATS_SIMPLE_PIE_CLASS = "com.fluxcraft.MiaoMenu.libs.bstats.charts.SimplePie";
    private static final String MAIN_COMMAND = "dgeysermenu";
    private ConfigManager configManager;
    private JavaMenuManager javaMenuManager;
    private BedrockMenuManager bedrockMenuManager;
    private CommandManager commandManager;
    private HotReloadManager hotReloadManager;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            Lang.init(this);
            NamespacedKey clockKey = new NamespacedKey(this, "menu_clock");

            this.configManager = new ConfigManager(this);
            checkAndRefreshConfig();
            configManager.loadConfig();
            this.javaMenuManager = new JavaMenuManager(this);
            ActionRegistry actionRegistry = new ActionRegistry(this, javaMenuManager);

            this.bedrockMenuManager = new BedrockMenuManager(this, actionRegistry);
            this.commandManager = new CommandManager(this);
            MenuClockManager clockManager = new MenuClockManager(this, clockKey);

            this.hotReloadManager = new HotReloadManager(this);

            getServer().getPluginManager().registerEvents(new JavaMenuListener(this, actionRegistry), this);
            getServer().getPluginManager().registerEvents(new BedrockMenuListener(), this);
            getServer().getPluginManager().registerEvents(new ClockInteractionListener(clockManager), this);
            getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, clockManager), this);
            registerCommands();

            javaMenuManager.loadAllMenus();
            bedrockMenuManager.loadAllMenus();
            new TestXiaFa(this).distribute();
            if (configManager.getConfig().getBoolean("settings.hot-reload.enabled", true)) {
                try {
                    hotReloadManager.initialize();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to initialize HotReloadManager. Menu hot-reloading will be disabled.", e);
                }
            }
            initializeBStats();
            getLogger().info("MiaoMenu v" + getDescription().getVersion() + " Enabled.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Fatal error occurred while enabling MiaoMenu. Disabling plugin.", e);
            setEnabled(false);
        }
    }
    @Override
    public void onDisable() {
        if (hotReloadManager != null) {
            hotReloadManager.shutdown();
        }
    }
    private void registerCommands() {
        PluginCommand command = getCommand(MAIN_COMMAND);
        if (command != null) {
            command.setExecutor(commandManager);
            command.setTabCompleter(commandManager);
        } else {
            getLogger().severe("Failed to register command '" + MAIN_COMMAND + "'. Please check your plugin.yml.");
        }
    }
    private void checkAndRefreshConfig() {
        int currentVersion = getConfig().getInt("config-version", 0);
        if (currentVersion < CONFIG_VERSION) {
            getLogger().warning(Lang.get("message.config-update-warning"));
            saveResource("config.yml", true);
            reloadConfig();
            Lang.init(this);
            getLogger().info(Lang.get("message.config-updated"));
        }
    }
    private void initializeBStats() {
        try {
            Class<?> metricsClass = Class.forName(BSTATS_METRICS_CLASS);
            Object metrics = metricsClass.getConstructor(JavaPlugin.class, int.class).newInstance(this, BSTATS_ID);
            addCustomChart(metrics, metricsClass, "server_software", this::detectServerSoftware);
            addCustomChart(metrics, metricsClass, "minecraft_version", this::detectMinecraftVersion);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "BStats initialization failed", e);
        }
    }
    private void addCustomChart(Object metrics, Class<?> metricsClass, String chartId, java.util.concurrent.Callable<String> callable) throws Exception {
        Class<?> simplePieClass = Class.forName(BSTATS_SIMPLE_PIE_CLASS);
        Object simplePie = simplePieClass.getConstructor(String.class, java.util.concurrent.Callable.class).newInstance(chartId, callable);
        metricsClass.getMethod("addCustomChart", simplePieClass).invoke(metrics, simplePie);
    }
    private String detectServerSoftware() {
        String version = Bukkit.getVersion();
        List<String> knownTypes = List.of("Paper", "Spigot", "Purpur", "Mint","Leaves", "Leaf","Luminol","Folia");
        for (String type : knownTypes) {
            if (version.contains(type)) {
                return type;
            }
        }
        return "Other";
    }
    private String detectMinecraftVersion() {
        String version = Bukkit.getBukkitVersion().split("-")[0];
        int maxLength = Math.min(4, version.length());
        return version.substring(0, maxLength);
    }
    public ConfigManager getConfigManager() { return configManager; }
    public JavaMenuManager getJavaMenuManager() { return javaMenuManager; }
    public BedrockMenuManager getBedrockMenuManager() { return bedrockMenuManager; }
}
