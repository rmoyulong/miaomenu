package com.fluxcraft.MiaoMenu.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 舊版 menu 資料夾匯入的純檔案層服務：偵測候選、計算差異、複製、備份、還原。
 * 不直接和指令／聊天打交道；指令層由 {@code ImportCommand} 包裝。
 * 與 {@link LegacyDataMigrator}（首次啟動的零互動兜底）共用候選清單，但流程獨立。
 */
public final class LegacyImportService {
    public static final String BACKUP_ROOT = "backups";
    public static final String BACKUP_PREFIX = "import-";
    private static final int BACKUP_RETAIN = 5;
    private static final int TOKEN_LENGTH = 8;
    private static final DateTimeFormatter STAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public enum Conflict {
        SKIP, RENAME, OVERWRITE
    }

    public enum EntryStatus {
        ADD, SKIP_SAME, CONFLICT
    }

    /**
     * 候選來源類型。{@code LEGACY_MIAO} 是舊版 MiaoMenu / DGeyserMenu 系列（同一套 YAML schema，可整批檔案複製）；
     * {@code DELUXEMENUS} 是 DeluxeMenus 插件（YAML schema 不同，需要 {@link DeluxeMenusImporter} 做格式轉換）。
     */
    public enum SourceType {
        LEGACY_MIAO, DELUXEMENUS
    }

    public record ScanResult(String name, Path path, int javaMenuCount, int bedrockMenuCount,
                             int configVersion, SourceType sourceType) {
    }

    public record PreviewEntry(String relativePath, EntryStatus status, long sizeBytes) {
    }

    public record PreviewResult(String source, Path sourcePath, List<PreviewEntry> entries, String token) {
        public long countByStatus(EntryStatus status) {
            return entries.stream().filter(e -> e.status() == status).count();
        }
    }

    public record ApplyResult(String backupId, int added, int skipped, int conflictsHandled) {
    }

    private LegacyImportService() {
    }

    /**
     * 給 ImportCommand 用的延伸候選清單：包含 {@link LegacyDataMigrator#LEGACY_CANDIDATES}
     * （MiaoMenu 系列，整批複製）+ DeluxeMenus 相關資料夾（需走轉換器）。
     * <br>順序為先 MiaoMenu 系列、後 DeluxeMenus，便於 tab completion 一致。
     */
    public static final List<String> IMPORT_CANDIDATES;
    static {
        List<String> all = new ArrayList<>(LegacyDataMigrator.LEGACY_CANDIDATES);
        all.add("DeluxeMenus");
        all.add("dmenu");
        IMPORT_CANDIDATES = Collections.unmodifiableList(all);
    }

    /**
     * 掃描 plugins/ 內所有命中候選名稱（且不是本插件自己）的資料夾。
     * 排序依 {@link #IMPORT_CANDIDATES} 順序，便於 tab completion 一致。
     * 自動辨別 source type：DeluxeMenus 結構走轉換路徑，其餘走整批複製。
     */
    public static List<ScanResult> scan(Plugin plugin) {
        Path dataDir = plugin.getDataFolder().toPath();
        Path pluginsDir = dataDir.getParent();
        if (pluginsDir == null || !Files.isDirectory(pluginsDir)) {
            return Collections.emptyList();
        }
        String ourName = dataDir.getFileName().toString();
        List<ScanResult> out = new ArrayList<>();
        for (String candidate : IMPORT_CANDIDATES) {
            if (candidate.equalsIgnoreCase(ourName)) {
                continue;
            }
            Path dir = pluginsDir.resolve(candidate);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            SourceType type = DeluxeMenusImporter.detect(dir) ? SourceType.DELUXEMENUS : SourceType.LEGACY_MIAO;
            int java;
            int bedrock;
            int cfgVer;
            if (type == SourceType.DELUXEMENUS) {
                java = DeluxeMenusImporter.listMenus(dir).size();
                bedrock = 0;
                cfgVer = -1;
            } else {
                java = countYamls(dir.resolve("java_menus"));
                bedrock = countYamls(dir.resolve("bedrock_menus"));
                cfgVer = readConfigVersion(dir.resolve("config.yml"));
            }
            out.add(new ScanResult(candidate, dir, java, bedrock, cfgVer, type));
        }
        return out;
    }

    /**
     * 把來源資料夾與目標 dataFolder 比對，逐檔分類 ADD / SKIP_SAME / CONFLICT，並產 8 碼 token。
     * SKIP_SAME 為「同路徑且內容位元相同」，CONFLICT 為「同路徑但內容不同」。
     */
    public static PreviewResult preview(String source, Path sourcePath, Path targetDir) throws IOException {
        if (!Files.isDirectory(sourcePath)) {
            throw new IOException("source folder not found: " + sourcePath);
        }
        List<PreviewEntry> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(sourcePath)) {
            for (Path src : (Iterable<Path>) stream::iterator) {
                if (Files.isDirectory(src)) {
                    continue;
                }
                Path relative = sourcePath.relativize(src);
                String rel = relative.toString().replace('\\', '/');
                if (shouldIgnore(rel)) {
                    continue;
                }
                Path dst = targetDir.resolve(rel);
                long size = Files.size(src);
                EntryStatus status;
                if (!Files.exists(dst)) {
                    status = EntryStatus.ADD;
                } else if (sameContent(src, dst)) {
                    status = EntryStatus.SKIP_SAME;
                } else {
                    status = EntryStatus.CONFLICT;
                }
                entries.add(new PreviewEntry(rel, status, size));
            }
        }
        entries.sort(Comparator.comparing(PreviewEntry::relativePath));
        String token = computeToken(source, entries);
        return new PreviewResult(source, sourcePath, entries, token);
    }

    /**
     * DeluxeMenus 專用預覽：列出 gui_menus 內每個選單 → 計算 java_menus/&lt;id&gt;.yml 的 ADD / CONFLICT。
     * 沒有 SKIP_SAME 概念（轉換結果幾乎不可能位元相等）。
     */
    public static PreviewResult previewDeluxeMenus(String source, Path sourcePath, Path targetDir) throws IOException {
        if (!Files.isDirectory(sourcePath)) {
            throw new IOException("source folder not found: " + sourcePath);
        }
        List<PreviewEntry> entries = new ArrayList<>();
        for (DeluxeMenusImporter.MenuEntry m : DeluxeMenusImporter.listMenus(sourcePath)) {
            Path dst = targetDir.resolve(m.outputRelative());
            EntryStatus status = Files.exists(dst) ? EntryStatus.CONFLICT : EntryStatus.ADD;
            long size = Files.size(m.sourceFile());
            entries.add(new PreviewEntry(m.outputRelative(), status, size));
        }
        entries.sort(Comparator.comparing(PreviewEntry::relativePath));
        String token = computeToken(source, entries);
        return new PreviewResult(source, sourcePath, entries, token);
    }

    /**
     * DeluxeMenus 專用套用：對 gui_menus 內每個選單跑 {@link DeluxeMenusImporter#convertMenu} 後寫到 java_menus/。
     * 衝突檔同樣先備份到 backups/import-&lt;timestamp&gt;/，再依 {@link Conflict} 策略落盤。
     */
    public static ApplyResult applyDeluxeMenus(Plugin plugin, PreviewResult preview, Conflict conflict) throws IOException {
        Path dataDir = plugin.getDataFolder().toPath();
        Files.createDirectories(dataDir);
        String backupId = createBackup(plugin, preview, dataDir);
        int added = 0;
        int skipped = 0;
        int handled = 0;
        for (DeluxeMenusImporter.MenuEntry m : DeluxeMenusImporter.listMenus(preview.sourcePath())) {
            Path dst = dataDir.resolve(m.outputRelative());
            ensureParent(dst);
            boolean existed = Files.exists(dst);
            String converted = DeluxeMenusImporter.convertMenu(m.sourceFile());
            if (!existed) {
                DeluxeMenusImporter.applyConversion(dst, converted);
                added++;
            } else {
                switch (conflict) {
                    case SKIP -> skipped++;
                    case OVERWRITE -> {
                        DeluxeMenusImporter.applyConversion(dst, converted);
                        handled++;
                    }
                    case RENAME -> {
                        Path renamed = dst.resolveSibling(dst.getFileName().toString() + ".imported");
                        DeluxeMenusImporter.applyConversion(renamed, converted);
                        handled++;
                    }
                }
            }
        }
        rotateBackups(plugin);
        return new ApplyResult(backupId, added, skipped, handled);
    }

    /**
     * 套用 preview 結果：先備份所有「會被影響到的」既有檔，再依 conflict 策略複製。
     * 回傳 ApplyResult 含 backupId、新增／略過／衝突處理數。
     */
    public static ApplyResult apply(Plugin plugin, PreviewResult preview, Conflict conflict) throws IOException {
        Path dataDir = plugin.getDataFolder().toPath();
        Files.createDirectories(dataDir);
        String backupId = createBackup(plugin, preview, dataDir);
        int added = 0;
        int skipped = 0;
        int handled = 0;
        for (PreviewEntry entry : preview.entries()) {
            Path src = preview.sourcePath().resolve(entry.relativePath());
            Path dst = dataDir.resolve(entry.relativePath());
            ensureParent(dst);
            switch (entry.status()) {
                case ADD -> {
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    added++;
                }
                case SKIP_SAME -> skipped++;
                case CONFLICT -> {
                    switch (conflict) {
                        case SKIP -> skipped++;
                        case OVERWRITE -> {
                            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                            handled++;
                        }
                        case RENAME -> {
                            Path renamed = dst.resolveSibling(dst.getFileName().toString() + ".imported");
                            Files.copy(src, renamed, StandardCopyOption.REPLACE_EXISTING);
                            handled++;
                        }
                    }
                }
            }
        }
        rotateBackups(plugin);
        return new ApplyResult(backupId, added, skipped, handled);
    }

    public static List<String> listBackups(Plugin plugin) {
        Path root = plugin.getDataFolder().toPath().resolve(BACKUP_ROOT);
        if (!Files.isDirectory(root)) {
            return Collections.emptyList();
        }
        try (Stream<Path> stream = Files.list(root)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith(BACKUP_PREFIX))
                    .sorted(Comparator.reverseOrder())
                    .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 把指定 backupId 的內容複製回 dataFolder（覆寫衝突檔）。回傳實際還原的檔案數。
     */
    public static int rollback(Plugin plugin, String backupId) throws IOException {
        if (!isSafeBackupId(backupId)) {
            throw new IOException("invalid backup id: " + backupId);
        }
        Path dataDir = plugin.getDataFolder().toPath();
        Path backupDir = dataDir.resolve(BACKUP_ROOT).resolve(backupId);
        if (!Files.isDirectory(backupDir)) {
            throw new IOException("backup not found: " + backupId);
        }
        int[] count = {0};
        try (Stream<Path> stream = Files.walk(backupDir)) {
            stream.forEach(src -> {
                if (Files.isDirectory(src)) {
                    return;
                }
                Path rel = backupDir.relativize(src);
                Path dst = dataDir.resolve(rel.toString());
                try {
                    ensureParent(dst);
                    Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    count[0]++;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return count[0];
    }

    /* ---------------- 內部工具 ---------------- */

    private static String createBackup(Plugin plugin, PreviewResult preview, Path dataDir) throws IOException {
        // 秒級時戳可能在同秒被連續觸發兩次匯入而衝突，後綴若已存在則遞增 -2, -3 ...
        String base = BACKUP_PREFIX + LocalDateTime.now().format(STAMP_FMT);
        String backupId = base;
        Path backupRoot = dataDir.resolve(BACKUP_ROOT);
        int suffix = 2;
        while (Files.exists(backupRoot.resolve(backupId))) {
            backupId = base + "-" + suffix++;
        }
        Path backupDir = backupRoot.resolve(backupId);
        boolean needed = false;
        for (PreviewEntry entry : preview.entries()) {
            if (entry.status() == EntryStatus.CONFLICT) {
                Path existing = dataDir.resolve(entry.relativePath());
                if (Files.isRegularFile(existing)) {
                    Path dst = backupDir.resolve(entry.relativePath());
                    ensureParent(dst);
                    Files.copy(existing, dst, StandardCopyOption.REPLACE_EXISTING);
                    needed = true;
                }
            }
        }
        if (!needed) {
            // 沒衝突也保留一個空 marker 目錄，方便 rollback 列表展示對應的「乾淨匯入」
            Files.createDirectories(backupDir);
        }
        return backupId;
    }

    private static void rotateBackups(Plugin plugin) {
        Path root = plugin.getDataFolder().toPath().resolve(BACKUP_ROOT);
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> stream = Files.list(root)) {
            List<Path> backups = stream
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith(BACKUP_PREFIX))
                    .sorted(Comparator.reverseOrder())
                    .toList();
            if (backups.size() <= BACKUP_RETAIN) {
                return;
            }
            for (Path old : backups.subList(BACKUP_RETAIN, backups.size())) {
                deleteRecursively(old);
            }
        } catch (IOException ignored) {
            // 旋轉失敗不阻斷主流程
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> all = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path p : all) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static void ensureParent(Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
    }

    private static boolean sameContent(Path a, Path b) {
        try {
            if (Files.size(a) != Files.size(b)) {
                return false;
            }
            return Arrays.equals(Files.readAllBytes(a), Files.readAllBytes(b));
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean shouldIgnore(String rel) {
        // 不把備份、log、暫存檔帶進來；亦過濾任何含上層跳躍的相對路徑（path traversal 防守）
        if (rel.contains("..") || rel.startsWith("/")) {
            return true;
        }
        String lower = rel.toLowerCase();
        if (lower.startsWith(BACKUP_ROOT + "/")) {
            return true;
        }
        if (lower.endsWith(".log") || lower.endsWith(".tmp") || lower.endsWith(".bak")) {
            return true;
        }
        return lower.equals("config.yml.old");
    }

    private static int countYamls(Path dir) {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            long count = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yml"))
                    .count();
            return (int) Math.min(count, Integer.MAX_VALUE);
        } catch (IOException e) {
            return 0;
        }
    }

    private static int readConfigVersion(Path configFile) {
        if (!Files.isRegularFile(configFile)) {
            return -1;
        }
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(configFile.toFile());
            return y.getInt("config-version", -1);
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private static String computeToken(String source, List<PreviewEntry> entries) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(source.getBytes());
            Set<String> seen = new HashSet<>();
            for (PreviewEntry e : entries) {
                if (!seen.add(e.relativePath())) {
                    continue;
                }
                md.update(e.relativePath().getBytes());
                md.update((byte) e.status().ordinal());
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
                if (sb.length() >= TOKEN_LENGTH) {
                    break;
                }
            }
            return sb.substring(0, TOKEN_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            // 退化：用內容長度 + source hashCode（不該觸發；SHA-1 為 JVM 必備）
            return String.format("%08x", (source.hashCode() ^ entries.size()) & 0xFFFFFFFFL).substring(0, TOKEN_LENGTH);
        }
    }

    private static boolean isSafeBackupId(String backupId) {
        if (backupId == null || backupId.isEmpty()) {
            return false;
        }
        if (!backupId.startsWith(BACKUP_PREFIX)) {
            return false;
        }
        // 防止 path traversal：必須只含 [a-zA-Z0-9-]
        for (int i = 0; i < backupId.length(); i++) {
            char c = backupId.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-')) {
                return false;
            }
        }
        return true;
    }
}
