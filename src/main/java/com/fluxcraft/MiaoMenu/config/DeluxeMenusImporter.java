package com.fluxcraft.MiaoMenu.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 把 DeluxeMenus 的 YAML 結構轉換為 MiaoMenu_fork 的 java_menus 格式。
 *
 * <h3>結構對應</h3>
 * <pre>
 * DeluxeMenus                              MiaoMenu_fork
 * --------------------------------         -----------------------------
 * config.yml.gui_menus.&lt;id&gt;.file → 來源檔   java_menus/&lt;id&gt;.yml
 * menu_title                       →        menu_title                   （原樣）
 * size: 54                         →        rows: 6                      （除 9，1–6 區間）
 * open_command / register_command  →        略過                          （MiaoMenu 走 /dgm open &lt;id&gt;）
 * items.&lt;id&gt;.slot                  →        items.&lt;id&gt;.slot              （原樣）
 * items.&lt;id&gt;.material              →        items.&lt;id&gt;.material          （原樣）
 * items.&lt;id&gt;.display_name          →        items.&lt;id&gt;.display_name      （原樣）
 * items.&lt;id&gt;.lore                  →        items.&lt;id&gt;.lore              （原樣）
 * items.&lt;id&gt;.custom_model_data     →        items.&lt;id&gt;.custom_model_data （原樣）
 * items.&lt;id&gt;.left_click_commands   →        items.&lt;id&gt;.left_click_commands （動作 prefix 轉換）
 * items.&lt;id&gt;.right_click_commands  →        items.&lt;id&gt;.right_click_commands（動作 prefix 轉換）
 * </pre>
 *
 * <h3>動作 prefix 對應</h3>
 * <pre>
 * [player] ...     → [player] ...      （MiaoMenu 也有）
 * [message] ...    → [message] ...     （兩邊相同）
 * [close]          → [close]           （兩邊相同）
 * [console] ...    → [cmd] ...         （MiaoMenu 用 cmd 走 console）
 * [openguimenu] X  → [player] dgm open X （走 MiaoMenu 主指令，最穩定的等價路徑）
 * 其他             → 原樣保留           （由 MiaoMenu 的 DefaultAction 處理，未知 prefix 預設視同 player）
 * </pre>
 *
 * <h3>不轉換 / 略過的欄位</h3>
 * <ul>
 *   <li>{@code update: true} — MiaoMenu 不需此標記（PlaceholderAPI 解析時機由 MiaoMenu 自己決定）</li>
 *   <li>{@code skin: <player_name>} — MiaoMenu 目前不支援 PLAYER_HEAD 的 skin 動態欄位，保留 material 但丟棄 skin</li>
 *   <li>{@code view_requirement}、{@code conditions} — DeluxeMenus 與 MiaoMenu 的 requirement 系統格式接近但細節差異，保留欄位內容讓使用者手動審閱</li>
 * </ul>
 *
 * 所有公開方法皆為純函式，無副作用（除 {@link #applyConversion} 會寫檔案）。
 */
public final class DeluxeMenusImporter {
    public static final String CONFIG_FILE = "config.yml";
    public static final String MENUS_DIR = "gui_menus";

    public record MenuEntry(String id, Path sourceFile, String outputRelative) {
    }

    private DeluxeMenusImporter() {
    }

    /**
     * 判斷某資料夾是否為 DeluxeMenus 結構：config.yml 內含 {@code gui_menus:} 鍵 + 存在 gui_menus/ 子資料夾。
     */
    public static boolean detect(Path sourceDir) {
        if (sourceDir == null || !Files.isDirectory(sourceDir)) {
            return false;
        }
        Path config = sourceDir.resolve(CONFIG_FILE);
        if (!Files.isRegularFile(config)) {
            return false;
        }
        try {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(config.toFile());
            return y.isConfigurationSection("gui_menus");
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 列出 DeluxeMenus config.yml 內定義的所有選單；
     * 每個 entry 標記原始檔案路徑與轉換後在 java_menus/ 下的目標檔名（用 menu id）。
     */
    public static List<MenuEntry> listMenus(Path sourceDir) {
        List<MenuEntry> entries = new ArrayList<>();
        Path config = sourceDir.resolve(CONFIG_FILE);
        if (!Files.isRegularFile(config)) {
            return entries;
        }
        YamlConfiguration y;
        try {
            y = YamlConfiguration.loadConfiguration(config.toFile());
        } catch (RuntimeException e) {
            return entries;
        }
        ConfigurationSection guiMenus = y.getConfigurationSection("gui_menus");
        if (guiMenus == null) {
            return entries;
        }
        for (String id : guiMenus.getKeys(false)) {
            String relativeFile = guiMenus.getString(id + ".file");
            if (relativeFile == null || relativeFile.isBlank()) {
                continue;
            }
            Path menuFile = sourceDir.resolve(MENUS_DIR).resolve(relativeFile);
            if (!Files.isRegularFile(menuFile)) {
                // 容錯：DeluxeMenus 設定指向不存在的檔案 → 略過
                continue;
            }
            String safeId = sanitizeFileSegment(id);
            String outputRel = "java_menus/" + safeId + ".yml";
            entries.add(new MenuEntry(id, menuFile, outputRel));
        }
        return entries;
    }

    /**
     * 把單一 DeluxeMenus menu yaml 轉成 MiaoMenu_fork 的 YAML 字串。
     */
    public static String convertMenu(Path sourceFile) throws IOException {
        if (!Files.isRegularFile(sourceFile)) {
            throw new IOException("menu file not found: " + sourceFile);
        }
        YamlConfiguration src;
        try {
            src = YamlConfiguration.loadConfiguration(sourceFile.toFile());
        } catch (RuntimeException e) {
            throw new IOException("failed to parse DeluxeMenus YAML: " + sourceFile.getFileName(), e);
        }
        YamlConfiguration dst = new YamlConfiguration();

        // menu_title 原樣搬過去；缺值給一個合理 fallback
        dst.set("menu_title", src.getString("menu_title", sourceFile.getFileName().toString()));

        // size 是格數（9, 18, ...）→ MiaoMenu 用 rows（1–6）
        int size = src.getInt("size", src.getInt("rows", 54) * 9);
        int rows = Math.max(1, Math.min(6, size / 9));
        dst.set("rows", rows);

        // open_command / register_command 不轉（MiaoMenu 走主指令 /dgm open <menuId>）
        // 為了讓使用者能溯源，把原始 open_command 寫到一個保留欄位 _imported_open_command
        if (src.contains("open_command")) {
            dst.set("_imported_open_command", src.getString("open_command"));
        }

        ConfigurationSection items = src.getConfigurationSection("items");
        if (items != null) {
            for (String itemId : items.getKeys(false)) {
                ConfigurationSection srcItem = items.getConfigurationSection(itemId);
                if (srcItem == null) {
                    continue;
                }
                String basePath = "items." + itemId;
                copyIfPresent(srcItem, dst, basePath, "slot");
                copyIfPresent(srcItem, dst, basePath, "material");
                copyIfPresent(srcItem, dst, basePath, "custom_model_data");
                copyIfPresent(srcItem, dst, basePath, "display_name");
                if (srcItem.contains("lore")) {
                    dst.set(basePath + ".lore", srcItem.getStringList("lore"));
                }
                if (srcItem.contains("left_click_commands")) {
                    dst.set(basePath + ".left_click_commands", convertActions(srcItem.getStringList("left_click_commands")));
                }
                if (srcItem.contains("right_click_commands")) {
                    dst.set(basePath + ".right_click_commands", convertActions(srcItem.getStringList("right_click_commands")));
                }
                // 保留 conditions/view_requirement/lock_message 等欄位讓使用者手動審閱
                copyIfPresent(srcItem, dst, basePath, "lock_message");
                copyIfPresent(srcItem, dst, basePath, "view_requirement");
                copyIfPresent(srcItem, dst, basePath, "conditions");
                copyIfPresent(srcItem, dst, basePath, "item_conditions");
            }
        }

        String header = "# 由 MiaoMenu_fork DeluxeMenusImporter 從 " + sourceFile.getFileName()
                + " 自動轉換產生。\n"
                + "# 動作前綴對應：[console]→[cmd]、[openguimenu] X→[player] dgm open X；\n"
                + "# 其餘 prefix（[player]/[message]/[close]）原樣保留。\n"
                + "# 如需手動微調 PlaceholderAPI、條件或物品 skin，請直接編輯本檔。\n\n";
        return header + dst.saveToString();
    }

    /**
     * 把轉換後內容寫入目標檔，回傳實際寫入的位元組數。
     */
    public static long applyConversion(Path targetFile, String yamlContent) throws IOException {
        Path parent = targetFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        byte[] bytes = yamlContent.getBytes(StandardCharsets.UTF_8);
        Files.write(targetFile, bytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return bytes.length;
    }

    /* ---------------- 內部工具 ---------------- */

    private static void copyIfPresent(ConfigurationSection src, YamlConfiguration dst, String basePath, String key) {
        if (src.contains(key)) {
            dst.set(basePath + "." + key, src.get(key));
        }
    }

    /**
     * DeluxeMenus → MiaoMenu_fork 動作 prefix 轉換。
     * 不認識的 prefix 原樣保留，由 MiaoMenu 的 DefaultAction 處理。
     */
    static List<String> convertActions(List<String> src) {
        List<String> dst = new ArrayList<>(src.size());
        for (String raw : src) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                dst.add(raw);
                continue;
            }
            // 找出 [prefix]<空白>... 結構
            int bracketStart = trimmed.indexOf('[');
            int bracketEnd = trimmed.indexOf(']');
            if (bracketStart != 0 || bracketEnd <= 0) {
                dst.add(raw);
                continue;
            }
            String prefix = trimmed.substring(1, bracketEnd).trim().toLowerCase();
            String body = trimmed.substring(bracketEnd + 1).trim();
            String converted = switch (prefix) {
                case "console" -> "[cmd] " + body;
                case "openguimenu" -> "[player] dgm open " + body;
                // 其他通通保留：[player] [message] [close] 以及未知 prefix
                default -> raw;
            };
            dst.add(converted);
        }
        return dst;
    }

    /**
     * 把 DeluxeMenus 的 menu id 變成 java_menus 下安全的檔名：
     * 移除路徑相關字元、空白；只保留 [A-Za-z0-9._-]；避免 path traversal。
     */
    static String sanitizeFileSegment(String input) {
        if (input == null || input.isEmpty()) {
            return "menu";
        }
        StringBuilder sb = new StringBuilder();
        int len = Math.min(input.length(), 64);
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                sb.append(c);
            } else if (Character.isWhitespace(c)) {
                sb.append('_');
            }
        }
        String result = sb.toString();
        if (result.isEmpty() || result.equals(".") || result.equals("..")) {
            return "menu";
        }
        // 防止以 . 開頭被當隱藏檔；MiaoMenu 不希望有這種行為
        while (result.startsWith(".")) {
            result = result.substring(1);
        }
        return result.isEmpty() ? "menu" : result;
    }

    /**
     * 給 {@code ImportCommand} preview 用的合計：在現有 dataFolder 下，逐個 menu 比對「會新增 vs 衝突」。
     */
    public static Map<String, EntryResolution> resolveDiff(List<MenuEntry> entries, Path targetDataDir) {
        Map<String, EntryResolution> resolved = new LinkedHashMap<>();
        for (MenuEntry e : entries) {
            Path dst = targetDataDir.resolve(e.outputRelative());
            if (Files.exists(dst)) {
                resolved.put(e.id(), new EntryResolution(e, true));
            } else {
                resolved.put(e.id(), new EntryResolution(e, false));
            }
        }
        return resolved;
    }

    public record EntryResolution(MenuEntry entry, boolean targetExists) {
    }
}
