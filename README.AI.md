# MiaoMenu_fork — AI 上下文記憶庫

> Fork:           https://github.com/Avery11111101/MiaoMenu_fork
> 原作 / Original: https://github.com/Yamada0001/MiaoMenu
>
> 此檔給 Claude 閱讀，用以接續歷史脈絡；對外正式說明請改看 `README.md`。
>
> **目前版本**：`0.1`（fork 版重新起算，從原作的 2.7.7.9 歸零）

## 1. 專案核心用意

MiaoMenu_fork 是 MiaoMenu 的分支版，鎖定 Minecraft Java 26.1.2（含 26.2 alpha 軟相容）。
痛點：原版 MiaoMenu 停在 Paper 1.21.11，且訊息檔死綁在 `config.yml` 的 `messages:` 區塊，跨語系維運成本高、又難分包翻譯。Fork 版要解決：

- 跟上 MC 26.x 的 Paper API、Floodgate、Geyser 最新版
- 拆出獨立 `lang/<language>.yml`，讓非開發者也能加語系
- 重新命名插件為 `MiaoMenu_fork` 並掛 GitHub 連結到每個設定檔頂端

## 2. 使用者決策動機（重大變更原因）

### 2026-06-20 — 多語系架構 + 升至 26.1.2

- **動機**：Avery 要把專案打造成跟 AFly（同作者另一個插件）一樣的多語系結構，且 MC 已經邁入 26.x 系列，原本還停在 1.21.11 的 Paper API 已過時。
- **背景考量**：
  - AFly 的 `Lang.java` 是「instance 載入 + jar 內 en.yml setDefaults fallback」模式，Avery 喜歡這種乾淨的結構
  - Floodgate/Geyser 對 26.x 的支援已上線（Floodgate 2.2.5-SNAPSHOT、Geyser 2.10.1-SNAPSHOT）
  - Avery 要求所有設定檔頂端都附 GitHub 連結，方便使用者回追來源

## 3. 歷史變更軌跡

### 2026-06-20 — Fork 化 + 多語系 + 升 26.1.2（this commit）

**改動範圍**：

| 檔案 | 行為 |
|------|------|
| `pom.xml` | `artifactId`/`name` 改 `MiaoMenu_fork`；`paper.api.version` 1.21.11-R0.1-SNAPSHOT → `26.1.2.build.72-stable`；`folia.api.version` → `26.1.2.build.8-stable`；`floodgate.version` 2.2.0-SNAPSHOT → `2.2.5-SNAPSHOT`；新增 `geyser.version` = `2.10.1-SNAPSHOT` 與對應 dependency；頂端加 GitHub header |
| `src/main/resources/plugin.yml` | 頂端加 GitHub header；`api-version` 1.21 → `'26.1'`；`softdepend` 多加 `Geyser-Spigot`；alias 多加 `mmf` |
| `src/main/resources/config.yml` | 刪除整個 `messages:` 子樹（移到 `lang/<language>.yml`）；新增 `language: zh_TW`；`config-version` 15 → 16；頂端加雙語 GitHub header |
| `src/main/resources/lang/zh_TW.yml`（新） | 將原本簡體訊息搬過來轉成繁體，保留顏色碼與 `{0}` 佔位符 |
| `src/main/resources/lang/en.yml`（新） | 英文翻譯，含原本只在中文出現的 `message/menu/help/descriptions/hot-reload` |
| `src/main/java/com/fluxcraft/MiaoMenu/utils/Lang.java` | 仿 AFly：保留靜態 `Lang.get(key)` API；新增 `Lang.init(plugin)`（同步釋出 jar 內 lang 檔）、`Lang.load(language)`（fallback en、`setDefaults`）；查詢順序：lang 檔 key → lang 檔 `messages.<key>` → config.yml（向後相容） |
| `src/main/java/com/fluxcraft/MiaoMenu/config/ConfigManager.java` | `CONFIG_VERSION` 12 → 16；`loadConfig()` 末段呼叫 `Lang.load(語言)` |
| `src/main/java/com/fluxcraft/MiaoMenu/managers/HotReloadManager.java` | 監聽 `lang/` 目錄，變更時即時 `Lang.load` |

**驗證**：`mvn -DskipTests clean package` BUILD SUCCESS，jar 內含 `lang/zh_TW.yml`、`lang/en.yml`、改寫後 `config.yml` 與 `plugin.yml`。

**已知測試問題**：`RequirementServiceTest` 用 Mockito `mock(MiaoMenu.class)` 但 `MiaoMenu` 是 `final class`，Paper 26 把 `JavaPlugin` 鎖成 sealed → Mockito 無法 inline mock。此為**升版前就存在**的測試端問題，與本次無關。後續可考慮把 MiaoMenu 改成 non-final 或在測試端改用真實 JavaPlugin instance + InstantiatorService。

**未動**：`bedrockmenu/`、`javamenu/`、`menu/`、`commands/impl/` 內的業務邏輯、範例 `java_menus/test.yml`、`bedrock_menus/test.yml`、`security/`、`proxy/`。
