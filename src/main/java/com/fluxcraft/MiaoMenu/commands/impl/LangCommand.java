package com.fluxcraft.MiaoMenu.commands.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import com.fluxcraft.MiaoMenu.MiaoMenu;
import com.fluxcraft.MiaoMenu.commands.PluginCommand;
import com.fluxcraft.MiaoMenu.utils.Lang;

/**
 * /dgm lang [&lt;語言碼&gt;] — 即時切換 language 設定並重新載入訊息檔。
 *
 * 行為：
 *  - 無參數：列出目前語言 + 可用語言檔（從 dataFolder/lang/*.yml 偵測；含 jar 內預設的 en/zh_TW）
 *  - 帶語言碼：檢查 lang/&lt;code&gt;.yml 存在 → 寫回 config.yml 的 language 鍵 → Lang.load(code) → 重新註冊 help 描述 → 通知使用者
 *
 * 權限：dgeysermenu.admin（沿用現有管理權限，不另開新節點）。
 * tab completion：補可用的語言檔名（不含 .yml 副檔名）。
 *
 * 注意：本指令只動 language 與 Lang 訊息，**不會重新載入 menu YAML**——
 * 因為訊息切換不影響 menu 結構，避免不必要的 IO 與 hot-reload 抖動。
 * 若同時要 reload menu，請另跑 /dgm reload。
 */
public class LangCommand implements PluginCommand {
    private static final String PERMISSION = "dgeysermenu.admin";
    private static final String LANG_DIR = "lang";
    private static final String YAML_EXT = ".yml";

    private final MiaoMenu plugin;

    public LangCommand(MiaoMenu plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Lang.get("message.no-permission"));
            return;
        }
        // 過濾 help 子參數：交給 HelpCommand 顯示詳細（dispatcher 已處理 `/dgm lang help`，這裡 defensive 兜底）
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(Lang.get("command.usage-lang"));
            return;
        }
        if (args.length == 0) {
            showStatus(sender);
            return;
        }
        String code = sanitize(args[0]);
        if (code.isEmpty()) {
            sender.sendMessage(Lang.get("command.usage-lang"));
            return;
        }
        Set<String> available = listAvailableLanguages();
        if (!available.contains(code)) {
            sender.sendMessage(Lang.get("lang.unknown")
                    .replace("{0}", code)
                    .replace("{1}", String.join(", ", available)));
            return;
        }
        // 寫回 config.yml 的 language 鍵
        try {
            FileConfiguration cfg = plugin.getConfig();
            cfg.set("language", code);
            cfg.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, Lang.get("log.lang.save-failed").replace("{0}", code), e);
            sender.sendMessage(Lang.get("lang.save-failed").replace("{0}", code));
            return;
        }
        // 重新載入訊息檔
        Lang.load(code);
        // help 描述快取在 CommandManager 建構時抓的，這裡呼叫 reloadHelpDescriptions 讓 `/dgm help` 也跟著語系
        plugin.getCommandManager().reloadHelpDescriptions();
        sender.sendMessage(Lang.get("lang.changed").replace("{0}", code));
        plugin.getLogger().info(Lang.get("log.lang.changed")
                .replace("{0}", code)
                .replace("{1}", sender.getName()));
    }

    private void showStatus(CommandSender sender) {
        String current = Lang.getCurrentLanguage();
        Set<String> available = listAvailableLanguages();
        sender.sendMessage(Lang.get("lang.current").replace("{0}", current));
        sender.sendMessage(Lang.get("lang.available").replace("{0}", String.join(", ", available)));
        sender.sendMessage(Lang.get("command.usage-lang"));
    }

    /**
     * 列出可用的語言檔：先掃 plugins/MiaoMenu_fork/lang/*.yml，再保證 en / zh_TW（jar 內預設）一定在內。
     */
    private Set<String> listAvailableLanguages() {
        Set<String> codes = new LinkedHashSet<>();
        // 預設一定有：對應 jar 內 lang/en.yml 與 lang/zh_TW.yml
        codes.add("en");
        codes.add("zh_TW");
        File langDir = new File(plugin.getDataFolder(), LANG_DIR);
        if (langDir.isDirectory()) {
            File[] files = langDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(YAML_EXT));
            if (files != null) {
                Arrays.stream(files)
                        .map(File::getName)
                        .map(n -> n.substring(0, n.length() - YAML_EXT.length()))
                        .filter(n -> !n.isEmpty())
                        .sorted()
                        .forEach(codes::add);
            }
        }
        return codes;
    }

    /**
     * 防止使用者輸入奇怪字元 / 路徑跳脫；只保留 [A-Za-z0-9_-]，長度上限 32。
     */
    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int len = Math.min(input.length(), 32);
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 1 && args[0] != null ? args[0].toLowerCase(Locale.ROOT) : "";
            List<String> all = new ArrayList<>(listAvailableLanguages());
            all.add("help");
            return all.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted()
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
