package com.fluxcraft.MiaoMenu.proxy;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jspecify.annotations.NonNull;

import com.fluxcraft.MiaoMenu.MiaoMenu;

public class ProxyManager implements PluginMessageListener {
    private final MiaoMenu plugin;
    private ProxyType proxyType;
    private static final String BUNGEECORD_CHANNEL = "BungeeCord";
    
    public ProxyManager(MiaoMenu plugin) {
        this.plugin = plugin;
        this.proxyType = ProxyType.NONE;
    }
    
    public void initialize() {
        detectProxyType();
        if (proxyType != ProxyType.NONE) {
            plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, BUNGEECORD_CHANNEL, this);
            plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, BUNGEECORD_CHANNEL);
            plugin.getLogger().info("Proxy detected: " + proxyType + " - Cross-server commands enabled");
            plugin.getLogger().info("Registered channel: " + BUNGEECORD_CHANNEL);
            
            if (proxyType == ProxyType.VELOCITY) {
                plugin.getLogger().info("Velocity detected: Using BungeeCord compatibility mode");
                plugin.getLogger().info("Please ensure Velocity has BungeeCord plugin message forwarding enabled");
            }
        } else {
            plugin.getLogger().info("No proxy detected - Cross-server commands disabled");
        }
    }
    
    private void detectProxyType() {
        if (plugin.getConfig().getBoolean("settings.velocity-network", false)) {
            proxyType = ProxyType.VELOCITY;
            plugin.getLogger().info("Velocity network enabled via config");
        } else {
            try {
                Class.forName("net.md_5.bungee.api.ChatColor");
                proxyType = ProxyType.BUNGEECORD;
                plugin.getLogger().info("BungeeCord detected via classpath");
            } catch (ClassNotFoundException ignored) {
                try {
                    Class.forName("com.velocitypowered.api.proxy.ProxyServer");
                    proxyType = ProxyType.VELOCITY;
                    plugin.getLogger().info("Velocity detected via classpath");
                } catch (ClassNotFoundException ignored2) {}
            }
        }
    }
    
    public boolean isProxyConnected() {
        return proxyType != ProxyType.NONE;
    }
    
    public boolean sendServerCommand(Player player, String serverName) {
        plugin.getLogger().info("Attempting to send server command: " + serverName + " for player: " + player.getName());
        plugin.getLogger().info("Current proxy type: " + proxyType);
        
        if (proxyType == ProxyType.NONE) {
            plugin.getLogger().warning("No proxy detected, cannot send server command");
            return false;
        }
        
        try {
            switch (proxyType) {
                case BUNGEECORD:
                    plugin.getLogger().info("Sending BungeeCord Connect command to: " + serverName);
                    sendBungeeCordServerCommand(player, serverName);
                    break;
                case VELOCITY:
                    plugin.getLogger().info("Velocity detected - using BungeeCord compatibility protocol");
                    plugin.getLogger().info("Sending BungeeCord Connect command to: " + serverName);
                    sendBungeeCordServerCommand(player, serverName);
                    break;
                default:
                    plugin.getLogger().warning("Unknown proxy type: " + proxyType);
                    return false;
            }
            plugin.getLogger().info("Server command sent successfully");
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to send server command to proxy: " + e.getMessage());
            plugin.getLogger().severe("Exception type: " + e.getClass().getName());
            if (e.getCause() != null) {
                plugin.getLogger().severe("Caused by: " + e.getCause().getMessage());
            }
            return false;
        }
    }
    
    private void sendBungeeCordServerCommand(Player player, String serverName) {
        try {
            if (plugin.getServer().getMessenger().isOutgoingChannelRegistered(plugin, BUNGEECORD_CHANNEL)) {
                byte[] message = createBungeeCordServerMessage(serverName);
                plugin.getLogger().info("Sending plugin message to channel: " + BUNGEECORD_CHANNEL);
                plugin.getLogger().info("Message length: " + message.length + " bytes");
                player.sendPluginMessage(plugin, BUNGEECORD_CHANNEL, message);
                plugin.getLogger().info("Plugin message sent successfully");
            } else {
                plugin.getLogger().warning("Channel " + BUNGEECORD_CHANNEL + " is not registered!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("BungeeCord send failed: " + e.getMessage());
            plugin.getLogger().severe("Exception type: " + e.getClass().getName());
            if (e.getCause() != null) {
                plugin.getLogger().severe("Caused by: " + e.getCause().getMessage());
            }
        }
    }
    
    private byte[] createBungeeCordServerMessage(String serverName) {
        try (java.io.ByteArrayOutputStream byteStream = new java.io.ByteArrayOutputStream();
             java.io.DataOutputStream dataOut = new java.io.DataOutputStream(byteStream)) {
            dataOut.writeUTF("Connect");
            dataOut.writeUTF(serverName);
            return byteStream.toByteArray();
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create BungeeCord message: " + e.getMessage());
            return new byte[0];
        }
    }
    
    @Override
    public void onPluginMessageReceived(String channel, @NonNull Player player, byte @NonNull [] message) {
        if (channel.equals(BUNGEECORD_CHANNEL)) {
            handleBungeeCordResponse(message);
        }
    }
    
    private void handleBungeeCordResponse(byte[] message) {
        try (java.io.ByteArrayInputStream byteStream = new java.io.ByteArrayInputStream(message);
             java.io.DataInputStream dataIn = new java.io.DataInputStream(byteStream)) {
            String subchannel = dataIn.readUTF();
            plugin.getLogger().fine("Received BungeeCord message: " + subchannel);
        } catch (Exception e) {
            plugin.getLogger().fine("Failed to handle BungeeCord response: " + e.getMessage());
        }
    }
    
    public enum ProxyType {
        NONE,
        BUNGEECORD,
        VELOCITY
    }
}
