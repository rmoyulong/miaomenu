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
import java.util.logging.Level;
import java.util.stream.Stream;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public final class MiaoMenu extends JavaPlugin {

    private static final int CONFIG_VERSION = 6;
    private static final int MENU_VERSION = 1;
    private static final int BSTATS_ID = 28979;
    public static final int JOIN_DELAY_TICKS = 20;
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
            configManager.checkAndRefreshMenus(MENU_VERSION);
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
            if (configManager.getConfig().getBoolean("settings.hot-reload.enabled", true)) {
                try {
                    hotReloadManager.initialize();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to initialize HotReloadManager. Menu hot-reloading will be disabled.", e);
                }
            }
            initializeBStats();
            getLogger().info("MiaoMenu v" + getName() + " Enabled.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Fatal error occurred while enabling MiaoMenu. Disabling plugin.", e);
            throw new RuntimeException("Fatal error during MiaoMenu enable", e);
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
            Metrics metrics = new Metrics(this, BSTATS_ID);
            metrics.addCustomChart(new SimplePie("server_software", this::detectServerSoftware));
            metrics.addCustomChart(new SimplePie("minecraft_version", this::detectMinecraftVersion));
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "BStats initialization failed", e);
        }
    }
    private String detectServerSoftware() {
        String version = Bukkit.getVersion();
        return Stream.of("Paper", "Spigot", "Purpur", "Mint", "Leaves", "Leaf", "Luminol", "Folia")
                .filter(version::contains)
                .findFirst()
                .orElse("Other");
    }
    private String detectMinecraftVersion() {
        String version = Bukkit.getBukkitVersion().split("-")[0];
        int maxLength = Math.min(4, version.length());
        return version.substring(0, maxLength);
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
    public JavaMenuManager getJavaMenuManager() {
        return javaMenuManager;
    }
    public BedrockMenuManager getBedrockMenuManager() {
        return bedrockMenuManager;
    }
}