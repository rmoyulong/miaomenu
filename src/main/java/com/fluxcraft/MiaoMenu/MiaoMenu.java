package com.fluxcraft.MiaoMenu;

import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.fluxcraft.MiaoMenu.bedrockmenu.BedrockMenuManager;
import com.fluxcraft.MiaoMenu.commands.CommandManager;
import com.fluxcraft.MiaoMenu.config.ConfigManager;
import com.fluxcraft.MiaoMenu.foliacall.FoliaFactory;
import com.fluxcraft.MiaoMenu.integration.CraftEngineIntegration;
import com.fluxcraft.MiaoMenu.javamenu.JavaMenuListener;
import com.fluxcraft.MiaoMenu.javamenu.JavaMenuManager;
import com.fluxcraft.MiaoMenu.listeners.ClockInteractionListener;
import com.fluxcraft.MiaoMenu.listeners.PlayerLifecycleListener;
import com.fluxcraft.MiaoMenu.listeners.PlayerLifecycleListener_Folia;
import com.fluxcraft.MiaoMenu.managers.HotReloadManager;
import com.fluxcraft.MiaoMenu.managers.MenuClockManager;
import com.fluxcraft.MiaoMenu.managers.SoundsClock;
import com.fluxcraft.MiaoMenu.menu.action.ActionRegistry;
import com.fluxcraft.MiaoMenu.proxy.ProxyManager;
import com.fluxcraft.MiaoMenu.utils.Lang;

import cn.handyplus.lib.adapter.HandySchedulerUtil;

public final class MiaoMenu extends JavaPlugin {
    private static final int BSTATS_ID = 28979;
    public static final int JOIN_DELAY_TICKS = 20;
    private static final String MAIN_COMMAND = "dgeysermenu";
    private ConfigManager configManager;
    private JavaMenuManager javaMenuManager;
    private BedrockMenuManager bedrockMenuManager;
    private CommandManager commandManager;
    private HotReloadManager hotReloadManager;
    private ProxyManager proxyManager;
    private Class<?> floodgateApiClass;
    private Object floodgateApiInstance;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            Lang.init(this);
            HandySchedulerUtil.init(this);
            NamespacedKey clockKey = new NamespacedKey(this, "menu_clock");
            this.configManager = new ConfigManager(this);
            configManager.loadConfig();
            configManager.checkAndRefreshMenus();
            CraftEngineIntegration craftEngineIntegration = new CraftEngineIntegration(
                    this,
                    configManager.isCraftEngineEnabled(),
                    configManager.getCraftEngineFallbackMaterial()
            );
            SoundsClock soundsClock = new SoundsClock(this);
            ActionRegistry actionRegistry = new ActionRegistry(this);
            this.javaMenuManager = new JavaMenuManager(this, craftEngineIntegration, soundsClock);
            this.bedrockMenuManager = new BedrockMenuManager(this, actionRegistry, soundsClock);
            this.commandManager = new CommandManager(this);
            MenuClockManager clockManager = new MenuClockManager(this, clockKey);
            this.hotReloadManager = new HotReloadManager(this);
            this.proxyManager = new ProxyManager(this);
            proxyManager.initialize();
            getServer().getPluginManager().registerEvents(new JavaMenuListener(this, actionRegistry), this);
            getServer().getPluginManager().registerEvents(new ClockInteractionListener(clockManager), this);
            if (FoliaFactory.isFolia()) {
                getServer().getPluginManager().registerEvents(new PlayerLifecycleListener_Folia(this, clockManager), this);
            } else {
                getServer().getPluginManager().registerEvents(new PlayerLifecycleListener(this, clockManager), this);
            }
            registerCommands();
            javaMenuManager.loadAllMenus();
            bedrockMenuManager.loadAllMenus();
            if (configManager.getConfig().getBoolean("settings.hot-reload.enabled", true)) {
                try {
                    hotReloadManager.initialize();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to initialize HotReloadManager. Menu hot-reload will be disabled.", e);
                }
            }
            initializeBStats();
            getLogger().info("MiaoMenu v" + getPluginMeta().getVersion() + " Enabled.");
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
        if (proxyManager != null) {
            if (getServer().getMessenger().isIncomingChannelRegistered(this, "BungeeCord")) {
                getServer().getMessenger().unregisterIncomingPluginChannel(this, "BungeeCord");
            }
            if (getServer().getMessenger().isOutgoingChannelRegistered(this, "BungeeCord")) {
                getServer().getMessenger().unregisterOutgoingPluginChannel(this, "BungeeCord");
            }
            if (getServer().getMessenger().isOutgoingChannelRegistered(this, "velocity:player")) {
                getServer().getMessenger().unregisterOutgoingPluginChannel(this, "velocity:player");
            }
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
    public void openSmartMenu(Player player, String menuName) {
        if (isBedrockPlayer(player)) {
            bedrockMenuManager.openMenu(player, menuName);
        } else {
            javaMenuManager.openMenu(player, menuName);
        }
    }
    private boolean isBedrockPlayer(Player player) {
        if (Bukkit.getPluginManager().getPlugin("floodgate") == null) {
            return false;
        }
        try {
            if (floodgateApiClass == null) {
                floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            }
            if (floodgateApiInstance == null) {
                floodgateApiInstance = floodgateApiClass.getMethod("getInstance").invoke(null);
            }
            return (Boolean) floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class)
                    .invoke(floodgateApiInstance, player.getUniqueId());
        } catch (Exception e) {
            return false;
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

    public ProxyManager getProxyManager() {
        return proxyManager;
    }
}