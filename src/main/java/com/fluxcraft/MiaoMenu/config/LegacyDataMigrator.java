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
    private static final List<String> LEGACY_CANDIDATES = List.of(
            "dmenu",
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
                Path dst = target.resolve(relative.toString());
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
