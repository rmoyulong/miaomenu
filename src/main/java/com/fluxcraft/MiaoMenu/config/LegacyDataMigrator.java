package com.fluxcraft.MiaoMenu.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.bukkit.plugin.Plugin;

/**
 * 從舊版資料夾（dmenu / DGeyserMenu / MiaoMenu 等）自動匯入設定與選單，達成「無痛轉移」。
 *
 * 觸發條件：本插件 dataFolder 內尚未有 config.yml（首次啟動或新環境），且 plugins/ 內存在
 * 名稱命中候選列表的舊資料夾。命中時整個資料夾複製過來；舊 config.yml 的版本若與本版不符，
 * 後續 {@link ConfigManager#loadConfig()} 會自動備份成 config.yml.old 並寫入新版本，
 * java_menus/、bedrock_menus/、lang/ 等子資料夾則原封不動保留。
 */
public final class LegacyDataMigrator {
    /**
     * 與 {@link LegacyImportService} 共用的候選來源資料夾名稱清單。
     * 順序即為偵測／tab completion 順序，新候選請追加在尾端以維持向後相容。
     *
     * 註：原先曾列入 "dmenu" — 但 `plugins/dmenu/` 對應的是 DeluxeMenus 插件（完全不同的選單系統，
     * YAML 結構與本插件不相容），盲目匯入只會把使用者環境弄壞，故 v1.2 起移除。
     * DeluxeMenus → MiaoMenu_fork 的選單轉換需要人工 port，不在無痛轉移範圍。
     */
    public static final List<String> LEGACY_CANDIDATES = List.of(
            "DGeyserMenu",
            "dgeysermenu",
            "MiaoMenu"
    );

    private LegacyDataMigrator() {
    }

    public static void migrateIfNeeded(Plugin plugin) {
        Path dataDir = plugin.getDataFolder().toPath();
        Path configPath = dataDir.resolve("config.yml");
        if (Files.exists(configPath)) {
            return;
        }
        Path pluginsDir = dataDir.getParent();
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return;
        }
        String ourName = dataDir.getFileName().toString();
        for (String candidateName : LEGACY_CANDIDATES) {
            if (candidateName.equalsIgnoreCase(ourName)) {
                continue;
            }
            Path legacyDir = pluginsDir.resolve(candidateName);
            if (!Files.isDirectory(legacyDir)) {
                continue;
            }
            Path legacyConfig = legacyDir.resolve("config.yml");
            if (!Files.isRegularFile(legacyConfig)) {
                continue;
            }
            try {
                Files.createDirectories(dataDir);
                copyDirectory(legacyDir, dataDir);
                plugin.getLogger().info("[legacy-migrate] 已從 " + candidateName
                        + "/ 匯入舊版資料 → " + ourName + "/（如版本不相容，舊 config.yml 會被自動備份為 config.yml.old）");
            } catch (IOException | UncheckedIOException e) {
                plugin.getLogger().log(Level.WARNING,
                        "[legacy-migrate] 從 " + candidateName + " 匯入失敗，請手動複製。", e);
            }
            return;
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(src -> {
                Path relative = source.relativize(src);
                String rel = relative.toString().replace('\\', '/');
                // 與 LegacyImportService.shouldIgnore() 策略對齊：拒絕含上層跳躍 / 絕對路徑的條目，
                // 並把潛在 symlink target 阻擋於 dataFolder 之外。
                if (rel.contains("..") || rel.startsWith("/")) {
                    return;
                }
                Path dst = target.resolve(rel);
                try {
                    if (Files.isDirectory(src)) {
                        if (!Files.exists(dst)) {
                            Files.createDirectories(dst);
                        }
                        return;
                    }
                    Path parent = dst.getParent();
                    if (parent != null && !Files.exists(parent)) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
