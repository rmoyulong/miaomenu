package com.fluxcraft.MiaoMenu.update;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.plugin.Plugin;

import com.fluxcraft.MiaoMenu.utils.Lang;

/**
 * 從 Modrinth API 非同步抓最新版本號，與目前 plugin 版本比對。
 *
 * 使用方式：
 * <pre>
 *     UpdateChecker checker = new UpdateChecker(plugin);
 *     checker.refreshAsync();   // onEnable 呼叫一次，30 秒 timeout 內回來
 *     ...
 *     if (checker.hasUpdate()) {  // PlayerJoin 時用
 *         sender.sendMessage(...);
 *     }
 * </pre>
 *
 * 設計取捨：
 *  - 用 JDK 21 內建 {@link HttpClient}，不額外引入依賴
 *  - {@code CompletableFuture.supplyAsync} 跑 HTTP，避免阻塞主執行緒；不依賴 Bukkit/Folia scheduler
 *  - 失敗或網路不通時靜默退場（warn log），不影響插件啟動或玩家進服
 *  - 只在啟動時抓一次（不做定期輪詢）— 若使用者長期不重啟、或需即時更新檢查，請手動重啟
 */
public final class UpdateChecker {
    private static final String API_URL = "https://api.modrinth.com/v2/project/miaomenu_fork/version";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern VERSION_NUMBER_PATTERN =
            Pattern.compile("\"version_number\"\\s*:\\s*\"([^\"]+)\"");

    private final Plugin plugin;
    private volatile String latestVersion;
    private volatile boolean checked;

    public UpdateChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * 觸發非同步抓取；完成後可透過 {@link #hasUpdate()} 查詢結果。
     */
    public CompletableFuture<Void> refreshAsync() {
        return CompletableFuture.supplyAsync(this::fetchLatest)
                .orTimeout(TIMEOUT.toSeconds() + 2, java.util.concurrent.TimeUnit.SECONDS)
                .handle((v, ex) -> {
                    checked = true;
                    if (ex != null) {
                        plugin.getLogger().log(Level.FINE,
                                Lang.get("log.update.fetch-failed").replace("{0}", ex.getClass().getSimpleName()));
                        return null;
                    }
                    latestVersion = v;
                    if (v != null && !v.equalsIgnoreCase(plugin.getPluginMeta().getVersion())) {
                        plugin.getLogger().info(Lang.get("log.update.available")
                                .replace("{0}", v)
                                .replace("{1}", plugin.getPluginMeta().getVersion()));
                    } else {
                        plugin.getLogger().fine(Lang.get("log.update.up-to-date"));
                    }
                    return null;
                });
    }

    /**
     * 是否有新版本（最近一次成功抓取後）。
     * 條件：已完成檢查、latestVersion 非空、與目前版本不同（簡單字串比對）。
     */
    public boolean hasUpdate() {
        if (!checked || latestVersion == null) {
            return false;
        }
        String current = plugin.getPluginMeta().getVersion();
        return !latestVersion.equalsIgnoreCase(current);
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isChecked() {
        return checked;
    }

    private String fetchLatest() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "MiaoMenu_fork/" + plugin.getPluginMeta().getVersion()
                            + " (+https://github.com/Avery11111101/MiaoMenu_fork)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                plugin.getLogger().fine("Modrinth API HTTP " + response.statusCode());
                return null;
            }
            // 不引入 JSON parser；列表 API 的版本按發布時間排序，第一個就是最新
            // 比對正則：第一個 "version_number": "x.y" 即為 latest
            Matcher m = VERSION_NUMBER_PATTERN.matcher(response.body());
            if (m.find()) {
                return m.group(1);
            }
            return null;
        } catch (Exception e) {
            // 任何例外（網路、SSL、parser、interrupt）都吞掉並回 null —— update check 失敗不能影響插件
            return null;
        }
    }
}
