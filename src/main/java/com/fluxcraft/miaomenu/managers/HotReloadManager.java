package com.fluxcraft.miaomenu.managers;

import com.fluxcraft.miaomenu.MiaoMenu;
import com.fluxcraft.miaomenu.utils.Lang;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class HotReloadManager {
    private final MiaoMenu plugin;
    private WatchService watchService;
    private Map<WatchKey, Path> keys;
    private volatile boolean running = false;
    private static final String JAVA_MENU_DIR = "java_menus";
    private static final String BEDROCK_MENU_DIR = "bedrock_menus";
    private static final String FILE_EXTENSION = ".yml";
    public HotReloadManager(MiaoMenu plugin) {
        this.plugin = plugin;
    }
    public void initialize() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        keys = new HashMap<>();
        this.running = true;
        registerDirectory(new File(plugin.getDataFolder(), JAVA_MENU_DIR).toPath());
        registerDirectory(new File(plugin.getDataFolder(), BEDROCK_MENU_DIR).toPath());

        plugin.getLogger().info(Lang.get("hot-reload.initialized"));
        startWatcher();
    }
    private void registerDirectory(Path dir) {
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Could not create directory " + dir + ": " + e.getMessage(), e);
                return;
            }
        }
        try {
            WatchKey key = dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            keys.put(key, dir);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not watch directory " + dir, e);
        }
    }
    private void startWatcher() {
        Thread t = new Thread(() -> {
            while (running) {
                try {
                    WatchKey key = watchService.take();
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        if (kind == StandardWatchEventKinds.OVERFLOW) continue;
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path filename = ev.context();
                        if (filename.toString().endsWith(FILE_EXTENSION)) {
                            String logMsg = Lang.get("hot-reload.detected").replace("{0}", filename.toString());
                            plugin.getLogger().info(logMsg);
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                plugin.getJavaMenuManager().loadAllMenus();
                                plugin.getBedrockMenuManager().loadAllMenus();
                            }, 10L);
                        }
                    }
                    if (!key.reset()) break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in HotReload watcher loop", e);
                }
            }
        }, "DGeyserMenu-HotReload-Thread");
        t.setDaemon(true);
        t.start();
    }
    public void shutdown() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing WatchService", e);
            }
        }
    }
}