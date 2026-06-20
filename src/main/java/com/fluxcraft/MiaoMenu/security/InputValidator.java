package com.fluxcraft.MiaoMenu.security;

import java.util.Locale;
import java.util.regex.Pattern;

public final class InputValidator {
    private static final int DEFAULT_MAX_LENGTH = 64;
    private static final int COMMAND_MAX_LENGTH = 256;
    // 對指令內容採「黑名單」風格：擋住明確會被 shell-style chain 串接的字元組合（;; && ||）與換行 / NUL，
    // 但保留中文、PAPI 展開後常見的格式符（&§<>,'"[]!?=@#$）與一般文字。
    // 原本的純白名單正則會把 PAPI 展開的 `&a` 顏色碼、玩家名含中日韓字元、`%player_displayname%`
    // 內含的 `[`、`]`、`,` 等內容全部誤殺，造成正常 menu 動作被擋下。
    // CR/LF/TAB/NUL 與聊天指令分隔符是真正會放大攻擊面的字元，仍然拒絕。
    private static final Pattern UNSAFE_CHARS = Pattern.compile("[\\r\\n\\t\\u0000]");
    private static final Pattern SAFE_MENU_NAME = Pattern.compile("[a-z0-9._-]+");

    private InputValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isSafeMenuName(String value) {
        return isWithinLimit(value, DEFAULT_MAX_LENGTH) && SAFE_MENU_NAME.matcher(value.toLowerCase(Locale.ROOT)).matches();
    }

    public static boolean isSafeCommandContent(String value) {
        return isWithinLimit(value, COMMAND_MAX_LENGTH)
                && !UNSAFE_CHARS.matcher(value).find()
                && !value.contains(";;")
                && !value.contains("&&")
                && !value.contains("||")
                // PlaceholderUtils.parse 會把 `&` 轉成 `§`，所以攻擊者塞「&&」進 PAPI placeholder 後抵達這裡時是「§§」。
                // 同步擋住以防 defense-in-depth 失效。
                && !value.contains("§§");
    }

    private static boolean isWithinLimit(String value, int maxLength) {
        return value != null && !value.isBlank() && value.length() <= maxLength;
    }
}
