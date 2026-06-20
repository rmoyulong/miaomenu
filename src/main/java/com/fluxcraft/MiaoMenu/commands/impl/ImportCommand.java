package com.fluxcraft.MiaoMenu.commands.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.commands.PluginCommand;
import com.fluxcraft.MiaoMenu.config.LegacyDataMigrator;
import com.fluxcraft.MiaoMenu.config.LegacyImportService;
import com.fluxcraft.MiaoMenu.config.LegacyImportService.ApplyResult;
import com.fluxcraft.MiaoMenu.config.LegacyImportService.Conflict;
import com.fluxcraft.MiaoMenu.config.LegacyImportService.EntryStatus;
import com.fluxcraft.MiaoMenu.config.LegacyImportService.PreviewEntry;
import com.fluxcraft.MiaoMenu.config.LegacyImportService.PreviewResult;
import com.fluxcraft.MiaoMenu.config.LegacyImportService.ScanResult;
import com.fluxcraft.MiaoMenu.utils.Lang;

/**
 * /dgm import scan|preview|apply|rollback —— 透過指令引導從舊版插件資料夾匯入 menu 資料。
 *
 * 與 {@link LegacyDataMigrator}（首次啟動的零互動兜底）並存：本指令為「已運行伺服器上的管理員手動匯入」管道，
 * 兩條路徑共用 {@link LegacyImportService} 的偵測 / 複製 / 備份邏輯。
 *
 * 安全策略：
 *  - preview 階段產生 8 碼 SHA-1 token，apply 必須帶相同 token，防誤觸 / stale 套用
 *  - apply 前自動把衝突檔備份到 backups/import-<時戳>/，保留最近 5 份
 *  - 預設衝突策略 SKIP；可用 `--overwrite` 或 `--rename` flag 改寫
 *  - 完成後自動 reload menus 與 config，無需重啟伺服器
 */
public class ImportCommand implements PluginCommand {
    private static final String PERMISSION = "dgeysermenu.import";
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);
    private static final int MAX_PREVIEW_ENTRIES = 12;

    private final MiaoMenu plugin;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public ImportCommand(MiaoMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION) && !sender.hasPermission("dgeysermenu.admin")) {
            sender.sendMessage(Lang.get("message.no-permission"));
            return;
        }
        if (args.length == 0) {
            sender.sendMessage(Lang.get("command.usage-import"));
            return;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        switch (sub) {
            case "scan" -> handleScan(sender);
            case "preview" -> handlePreview(sender, rest);
            case "apply" -> handleApply(sender, rest);
            case "rollback" -> handleRollback(sender, rest);
            default -> sender.sendMessage(Lang.get("command.usage-import"));
        }
    }

    /* ------------------- scan ------------------- */

    private void handleScan(CommandSender sender) {
        List<ScanResult> results = LegacyImportService.scan(plugin);
        if (results.isEmpty()) {
            sender.sendMessage(Lang.get("import.scan.empty"));
            return;
        }
        sender.sendMessage(Lang.get("import.scan.header").replace("{0}", String.valueOf(results.size())));
        for (ScanResult r : results) {
            String typeKey = r.sourceType() == LegacyImportService.SourceType.DELUXEMENUS
                    ? "import.scan.entry-deluxemenus"
                    : "import.scan.entry";
            sender.sendMessage(Lang.get(typeKey)
                    .replace("{0}", r.name())
                    .replace("{1}", String.valueOf(r.javaMenuCount()))
                    .replace("{2}", String.valueOf(r.bedrockMenuCount()))
                    .replace("{3}", r.configVersion() < 0 ? "?" : String.valueOf(r.configVersion())));
        }
        sender.sendMessage(Lang.get("import.scan.hint"));
    }

    /* ------------------- preview ------------------- */

    private void handlePreview(CommandSender sender, String[] rest) {
        if (rest.length < 1) {
            sender.sendMessage(Lang.get("command.usage-import"));
            return;
        }
        String source = rest[0];
        ScanResult target = findCandidate(source);
        if (target == null) {
            sender.sendMessage(Lang.get("import.preview.unknown-source").replace("{0}", source));
            return;
        }
        try {
            PreviewResult preview = target.sourceType() == LegacyImportService.SourceType.DELUXEMENUS
                    ? LegacyImportService.previewDeluxeMenus(target.name(), target.path(),
                            plugin.getDataFolder().toPath())
                    : LegacyImportService.preview(target.name(), target.path(),
                            plugin.getDataFolder().toPath());
            sessions.put(sessionKey(sender), new Session(preview, Instant.now(), target.sourceType()));
            if (target.sourceType() == LegacyImportService.SourceType.DELUXEMENUS) {
                sender.sendMessage(Lang.get("import.preview.deluxemenus-note"));
            }
            renderPreview(sender, preview);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, Lang.get("log.import.preview-failed").replace("{0}", source), e);
            sender.sendMessage(Lang.get("import.preview.io-error").replace("{0}", source));
        }
    }

    private void renderPreview(CommandSender sender, PreviewResult preview) {
        long add = preview.countByStatus(EntryStatus.ADD);
        long skip = preview.countByStatus(EntryStatus.SKIP_SAME);
        long conflict = preview.countByStatus(EntryStatus.CONFLICT);
        sender.sendMessage(Lang.get("import.preview.header")
                .replace("{0}", preview.source())
                .replace("{1}", String.valueOf(add))
                .replace("{2}", String.valueOf(skip))
                .replace("{3}", String.valueOf(conflict)));
        int shown = 0;
        for (PreviewEntry entry : preview.entries()) {
            if (shown++ >= MAX_PREVIEW_ENTRIES) {
                sender.sendMessage(Lang.get("import.preview.truncated")
                        .replace("{0}", String.valueOf(preview.entries().size() - MAX_PREVIEW_ENTRIES)));
                break;
            }
            String key = switch (entry.status()) {
                case ADD -> "import.preview.add";
                case SKIP_SAME -> "import.preview.skip";
                case CONFLICT -> "import.preview.conflict";
            };
            sender.sendMessage(Lang.get(key).replace("{0}", entry.relativePath()));
        }
        sender.sendMessage(Lang.get("import.preview.token")
                .replace("{0}", preview.token())
                .replace("{1}", preview.source()));
    }

    /* ------------------- apply ------------------- */

    private void handleApply(CommandSender sender, String[] rest) {
        if (rest.length < 2) {
            sender.sendMessage(Lang.get("command.usage-import"));
            return;
        }
        String source = rest[0];
        String token = rest[1];
        Conflict conflict = parseConflictFlag(rest);

        // 用 compute() 原子取 session：若不存在 / 已過期 / 來源 token 不符，回 null 並順手清掉舊紀錄
        Session session = sessions.compute(sessionKey(sender), (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return null;
            }
            return existing;
        });
        if (session == null) {
            sender.sendMessage(Lang.get("import.apply.no-session"));
            return;
        }
        PreviewResult preview = session.preview();
        if (!preview.source().equalsIgnoreCase(source) || !preview.token().equalsIgnoreCase(token)) {
            sender.sendMessage(Lang.get("import.apply.token-mismatch"));
            return;
        }
        try {
            ApplyResult result = session.sourceType() == LegacyImportService.SourceType.DELUXEMENUS
                    ? LegacyImportService.applyDeluxeMenus(plugin, preview, conflict)
                    : LegacyImportService.apply(plugin, preview, conflict);
            sessions.remove(sessionKey(sender));
            plugin.getConfigManager().loadConfig();
            plugin.getJavaMenuManager().loadAllMenus();
            plugin.getBedrockMenuManager().loadAllMenus();
            sender.sendMessage(Lang.get("import.apply.success")
                    .replace("{0}", preview.source())
                    .replace("{1}", String.valueOf(result.added()))
                    .replace("{2}", String.valueOf(result.skipped()))
                    .replace("{3}", String.valueOf(result.conflictsHandled()))
                    .replace("{4}", result.backupId()));
            plugin.getLogger().info(Lang.get("log.import.completed")
                    .replace("{0}", preview.source())
                    .replace("{1}", sender.getName())
                    .replace("{2}", result.backupId()));
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, Lang.get("log.import.copy-failed")
                    .replace("{0}", preview.source()), e);
            sender.sendMessage(Lang.get("import.apply.io-error"));
        }
    }

    private Conflict parseConflictFlag(String[] rest) {
        for (String token : rest) {
            String t = token.toLowerCase(Locale.ROOT);
            if (t.equals("--overwrite")) {
                return Conflict.OVERWRITE;
            }
            if (t.equals("--rename")) {
                return Conflict.RENAME;
            }
        }
        return Conflict.SKIP;
    }

    /* ------------------- rollback ------------------- */

    private void handleRollback(CommandSender sender, String[] rest) {
        List<String> backups = LegacyImportService.listBackups(plugin);
        if (backups.isEmpty()) {
            sender.sendMessage(Lang.get("import.rollback.not-found"));
            return;
        }
        String target = rest.length >= 1 ? rest[0] : backups.get(0);
        try {
            int count = LegacyImportService.rollback(plugin, target);
            plugin.getConfigManager().loadConfig();
            plugin.getJavaMenuManager().loadAllMenus();
            plugin.getBedrockMenuManager().loadAllMenus();
            sender.sendMessage(Lang.get("import.rollback.success")
                    .replace("{0}", target)
                    .replace("{1}", String.valueOf(count)));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, Lang.get("log.import.rollback-failed")
                    .replace("{0}", target), e);
            sender.sendMessage(Lang.get("import.rollback.io-error").replace("{0}", target));
        }
    }

    /* ------------------- tab completion ------------------- */

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return List.of("scan", "preview", "apply", "rollback");
        }
        if (args.length == 1) {
            return filterPrefix(List.of("scan", "preview", "apply", "rollback"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ((sub.equals("preview") || sub.equals("apply")) && args.length == 2) {
            List<String> candidates = LegacyImportService.scan(plugin).stream()
                    .map(ScanResult::name)
                    .collect(Collectors.toCollection(ArrayList::new));
            // 包含尚未存在的預設候選，以利使用者輸入後再決定
            for (String c : LegacyImportService.IMPORT_CANDIDATES) {
                if (!candidates.contains(c)) {
                    candidates.add(c);
                }
            }
            return filterPrefix(candidates, args[1]);
        }
        if (sub.equals("apply") && args.length == 3) {
            Session session = sessions.get(sessionKey(sender));
            if (session != null && !session.isExpired()
                    && session.preview().source().equalsIgnoreCase(args[1])) {
                return filterPrefix(List.of(session.preview().token()), args[2]);
            }
            return Collections.emptyList();
        }
        if (sub.equals("apply") && args.length >= 4) {
            return filterPrefix(List.of("--overwrite", "--rename"), args[args.length - 1]);
        }
        if (sub.equals("rollback") && args.length == 2) {
            return filterPrefix(LegacyImportService.listBackups(plugin), args[1]);
        }
        return Collections.emptyList();
    }

    /* ------------------- 工具 ------------------- */

    private ScanResult findCandidate(String source) {
        for (ScanResult r : LegacyImportService.scan(plugin)) {
            if (r.name().equalsIgnoreCase(source)) {
                return r;
            }
        }
        // 來源不在 scan 結果（資料夾可能不存在）→ 仍允許找已知候選名稱再給個錯誤訊息
        Path pluginsDir = plugin.getDataFolder().toPath().getParent();
        if (pluginsDir != null) {
            for (String c : LegacyImportService.IMPORT_CANDIDATES) {
                if (c.equalsIgnoreCase(source)) {
                    Path dir = pluginsDir.resolve(c);
                    if (java.nio.file.Files.isDirectory(dir)) {
                        LegacyImportService.SourceType type =
                                com.fluxcraft.MiaoMenu.config.DeluxeMenusImporter.detect(dir)
                                        ? LegacyImportService.SourceType.DELUXEMENUS
                                        : LegacyImportService.SourceType.LEGACY_MIAO;
                        return new ScanResult(c, dir, 0, 0, -1, type);
                    }
                }
            }
        }
        return null;
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted()
                .collect(Collectors.toList());
    }

    private String sessionKey(CommandSender sender) {
        if (sender instanceof Player p) {
            return "p:" + p.getUniqueId();
        }
        return "c:" + sender.getName();
    }

    private record Session(PreviewResult preview, Instant createdAt, LegacyImportService.SourceType sourceType) {
        boolean isExpired() {
            return Duration.between(createdAt, Instant.now()).compareTo(SESSION_TTL) > 0;
        }
    }
}
