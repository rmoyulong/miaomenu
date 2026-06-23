package com.fluxcraft.MiaoMenu.commands.impl;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.commands.PluginCommand;
import com.fluxcraft.MiaoMenu.utils.Lang;

/**
 * /dgm whoami — 列印 Floodgate / Geyser 對當前玩家的辨識結果。
 *
 * 主要用於排查「基岩端玩家在 Velocity 後端被當成 Java 玩家」的情境：
 * 任何一行回 false / null，就指出該段環節壞掉，不需要再翻 server log。
 */
public class WhoamiCommand implements PluginCommand {
    private static final String FLOODGATE_PLUGIN = "floodgate";
    private static final String GEYSER_PLUGIN = "Geyser-Spigot";
    private static final String FLOODGATE_API_CLASS = "org.geysermc.floodgate.api.FloodgateApi";

    private final MiaoMenu plugin;

    public WhoamiCommand(MiaoMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Lang.get("message.players-only"));
            return;
        }

        sender.sendMessage(Lang.get("whoami.header"));
        sendPluginStatus(sender, FLOODGATE_PLUGIN, "whoami.floodgate-plugin");
        sendPluginStatus(sender, GEYSER_PLUGIN, "whoami.geyser-plugin");

        UUID uuid = player.getUniqueId();
        sender.sendMessage(Lang.get("whoami.uuid").replace("{0}", uuid.toString()));

        boolean smartDispatch = plugin.isBedrockPlayer(player);
        sender.sendMessage(Lang.get("whoami.smart-dispatch").replace("{0}", boolText(smartDispatch)));

        FloodgateProbe probe = probeFloodgate(uuid);
        sender.sendMessage(Lang.get("whoami.floodgate-api").replace("{0}", boolText(probe.apiReachable)));
        sender.sendMessage(Lang.get("whoami.is-floodgate-player").replace("{0}", boolText(probe.isFloodgatePlayer)));
        sender.sendMessage(Lang.get("whoami.floodgate-username").replace("{0}", nullText(probe.bedrockUsername)));
        sender.sendMessage(Lang.get("whoami.floodgate-xuid").replace("{0}", nullText(probe.xuid)));
        sender.sendMessage(Lang.get("whoami.floodgate-linked").replace("{0}", boolText(probe.linked)));
        if (probe.error != null) {
            sender.sendMessage(Lang.get("whoami.floodgate-error").replace("{0}", probe.error));
        }

        sender.sendMessage(Lang.get(smartDispatch ? "whoami.verdict-bedrock" : "whoami.verdict-java"));
    }

    private void sendPluginStatus(CommandSender sender, String pluginName, String key) {
        Plugin found = Bukkit.getPluginManager().getPlugin(pluginName);
        if (found == null) {
            sender.sendMessage(Lang.get(key).replace("{0}", Lang.get("whoami.missing")));
            return;
        }
        String label = found.getPluginMeta().getVersion() + " / " + (found.isEnabled()
                ? Lang.get("whoami.enabled")
                : Lang.get("whoami.disabled"));
        sender.sendMessage(Lang.get(key).replace("{0}", label));
    }

    private FloodgateProbe probeFloodgate(UUID uuid) {
        FloodgateProbe probe = new FloodgateProbe();
        if (Bukkit.getPluginManager().getPlugin(FLOODGATE_PLUGIN) == null) {
            return probe;
        }
        try {
            Class<?> apiClass = Class.forName(FLOODGATE_API_CLASS);
            Object api = apiClass.getMethod("getInstance").invoke(null);
            probe.apiReachable = true;
            probe.isFloodgatePlayer = (Boolean) apiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(api, uuid);
            Object floodgatePlayer = apiClass.getMethod("getPlayer", UUID.class).invoke(api, uuid);
            if (floodgatePlayer != null) {
                Class<?> playerClass = floodgatePlayer.getClass();
                probe.bedrockUsername = invokeStringSafe(floodgatePlayer, playerClass, "getUsername");
                probe.xuid = invokeStringSafe(floodgatePlayer, playerClass, "getXuid");
                Object linkedPlayer = invokeSafe(floodgatePlayer, playerClass, "getLinkedPlayer");
                probe.linked = linkedPlayer != null;
            }
        } catch (ReflectiveOperationException e) {
            // 抓 cause 的訊息避免被 InvocationTargetException 包成空字串
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            probe.error = cause.getClass().getSimpleName() + ": " + cause.getMessage();
        }
        return probe;
    }

    private static String invokeStringSafe(Object target, Class<?> klass, String method) {
        Object result = invokeSafe(target, klass, method);
        return result == null ? null : result.toString();
    }

    private static Object invokeSafe(Object target, Class<?> klass, String method) {
        try {
            return klass.getMethod(method).invoke(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String boolText(boolean value) {
        return Lang.get(value ? "whoami.true" : "whoami.false");
    }

    private static String nullText(String value) {
        return value == null || value.isEmpty() ? Lang.get("whoami.null") : value;
    }

    private static final class FloodgateProbe {
        boolean apiReachable;
        boolean isFloodgatePlayer;
        String bedrockUsername;
        String xuid;
        boolean linked;
        String error;
    }
}
