# MiaoMenu_fork — AI 上下文記憶庫

> Fork:           https://github.com/Avery11111101/MiaoMenu_fork
> 原作 / Original: https://github.com/Yamada0001/MiaoMenu
>
> 此檔給 Claude 閱讀，用以接續歷史脈絡；對外正式說明請改看 `README.md`。
>
> **目前版本**：`0.2`（穩定性強化 + 無痛遷移；fork 版重新起算，從原作的 2.7.7.9 歸零）

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

### 2026-06-20 — 多代理迴圈修補（README 邏輯優化 + 程式碼小修）

- **動機**：Avery 要求站在他的角度優化 README、修掉相關問題，但鐵則是「**使用者操作零變動、檔案不需重新設定**」。整個任務以「總代理發現 → 獨立子代理驗證 → 修補 → 再開不同子代理驗證」的迴圈推進，連續兩輪驗證代理都判 clean 才收尾，並透過 Discord webhook 每輪同步進度（共 4 輪 webhook）。
- **背景考量**：
  - Fork 化後語系系統重整，但有些「漏抽」的鍵與硬編碼簡中字串還在程式裡
  - 用 Explore 子代理掃描 + 第二個 Explore 子代理交叉驗證，避免單一代理幻覺
  - 處理代理與驗證代理嚴格分離，每輪換新代理避免上下文殘留
- **改動範圍**：

| 檔案 | 行為 |
|------|------|
| `README.md` | 首行壞掉的 `./README_en_us.md` 連結改為說明（英文版尚未翻譯，可參考舊 `docs/README-en.md`）；最前面新增「向後相容聲明」段，明列保留的指令／別名／權限／設定鍵；最後新增「更新日誌」段，把 `0.1` 的關鍵變更條列在 README 內供使用者快速掃讀 |
| `src/main/java/com/fluxcraft/MiaoMenu/commands/CommandManager.java` | `loadHelpDescriptions()` 原本讀 `config.yml` 的 `messages.descriptions`（已不存在），會讓 `/dgm help` 印空白；改為優先呼叫 `Lang.get("descriptions.<name>")` 取 lang 檔內容，再退回 `config.yml messages.descriptions` 保持向後相容 |
| `src/main/java/com/fluxcraft/MiaoMenu/commands/impl/HelpCommand.java` | 把 `Lang.get("message.help.header")` / `Lang.get("message.help.usage")` 改成正確的 `help.header` / `help.usage`（原寫法會直接印 raw key 給玩家） |
| `src/main/java/com/fluxcraft/MiaoMenu/commands/impl/OpenCommand.java` | 把 `Lang.get("message.usage-open")` 改成正確的 `command.usage-open`（同樣原本會印 raw key） |
| `src/main/java/com/fluxcraft/MiaoMenu/bedrockmenu/BedrockMenuManager.java` | `handleFormResponse()` 把 `(int) ... invoke(...)` 拆箱改為 `instanceof Integer` 檢查，避免玩家關閉表單或反射回 null 時 NPE；同時 `FloodgateReflectionAccess` 改為 lazy（double-checked locking），純 Java 伺服器啟動不再因 Floodgate 缺席炸掉；`isLocked` 由 `Collections.emptyMap()` 改傳 `menu.getRequirementBlocks()` 修復 requirement_blocks 引用 |
| `src/main/java/com/fluxcraft/MiaoMenu/bedrockmenu/BedrockMenu.java` | 硬編碼簡中 `"§8[未解锁] §7"` 改用 `Lang.get("menu.locked-tag") + " §7"` |
| `src/main/java/com/fluxcraft/MiaoMenu/javamenu/JavaMenu.java` | 硬編碼簡中 `" &8[未解锁]"` 改用 `" " + Lang.get("menu.locked-tag")` |
| `src/main/java/com/fluxcraft/MiaoMenu/managers/HotReloadManager.java` | 執行緒名 `"MiaoMenu-HotReload-Thread"` → `"MiaoMenu_fork-HotReload-Thread"`，跟 Fork 改名一致 |
| `src/main/java/com/fluxcraft/MiaoMenu/utils/Lang.java` | `load()` 對 `YamlConfiguration.loadConfiguration` 加 try-catch（YAML 解析失敗時保留前次成功的 `messages`，不再靜默失效）；對 jar 內預設 `lang/en.yml` 載入也加 try-catch；成功載入時 info log；找不到鍵時的 fallback 鏈不變 |
| `src/main/resources/lang/en.yml` | `menu:` 區塊新增 `locked-tag: "&8[Locked]"` |
| `src/main/resources/lang/zh_TW.yml` | `menu:` 區塊新增 `locked-tag: "&8[未解鎖]"`（簡 → 繁） |

**驗證**：4 輪 build 皆 BUILD SUCCESS。每輪修補後皆派 2 位獨立 Explore 子代理交叉驗證（共 6 位驗證代理：C/D/E/F/G/H/I/J/K/L），最後一輪 K、L 雙雙判 clean、breaks_user_ops=false、confidence=high。

**保留的使用者操作**：`/dgeysermenu`、`/dgm`、`/fluxmenu`、`/mmf`、`/getmenuclock` 指令、`dgeysermenu.*` 權限樹、`config.yml` 所有鍵、`java_menus/*.yml` 與 `bedrock_menus/*.yml` 的 YAML 結構、`config-version: 16` 與 `menu-version: 6` — **全部不變**。

**未動**：核心選單渲染／時鐘／代理／安全模組／範例 yaml／pom.xml。`pom.xml` 沒改用 `<release>21</release>`（屬「低嚴重度建議」，未動以縮小 PR 範圍）；`RequirementServiceTest` 仍因 Byte Buddy 與 Java 25 環境不相容掛測（與本次無關，升版前就存在）。

### 2026-06-20（v0.2）— 多輪掃描/修復循環 + 無痛遷移 + 收斂到 main

**動機**：Avery 要求把整個插件支援最新 mc 版（26.1.2）與 Geyser/Floodgate，並要能無痛從 dmenu/DGeyserMenu/MiaoMenu 等舊資料夾轉移過來。鐵則：執行緒「找問題 → 驗證嚴重性 → 不同子代理修 → 不同子代理驗證 → 連兩輪 clean 才能停」。最終把所有變更收斂到 `main` 分支（本地 + GitHub 都只剩 main）。

**處理流程**：
1. 子代理 A 全面掃描 → 24 條 findings（4 P0 / 8 P1 / 12 P2）
2. 主代理親自驗證 P0/P1 真實性（讀檔交叉比對，發現 A 對 HelpCommand 的判斷已被既有程式碼吸收掉）
3. 主代理修復 → mvn 編譯通過
4. 子代理 B 獨立驗證 → 8/8 通過，順手指出 softdepend 大小寫
5. 主代理修 softdepend → 第二輪掃描子代理 C → 0 個 P0/P1（只剩美化），C 順手把寫死的 `[未解鎖]` 抽到 `menu.locked-tag`
6. 子代理 D 最終驗證 → A-K 全 ✅，循環收斂
7. 主代理實作 `LegacyDataMigrator`，子代理 E 驗證可上線

**改動範圍（v0.2 新增）**：

| 檔案 | 行為 |
|------|------|
| `bedrockmenu/BedrockMenuManager.java` | Floodgate 反射改 lazy（volatile + 雙重檢查 + synchronized），純 Java 環境也能 onEnable；handleClick 改傳真實 `menu.getRequirementBlocks()` |
| `bedrockmenu/BedrockMenu.java` | 新增 `getRequirementBlocks()` getter；鎖定按鈕標籤改用 `Lang.get("menu.locked-tag")` |
| `MiaoMenu.java` | 補上 `getmenuclock` 指令 executor（admin 權限 + Player 檢查 + `MenuClockManager#giveClockToPlayer`）；onEnable 最前面呼叫 `LegacyDataMigrator.migrateIfNeeded(this)`；`MenuClockManager` 改為 field 以供指令 executor 使用 |
| `config/ConfigManager.java` | `MENU_VERSION` 3 → 6（與 `config.yml` 對齊，不再每次啟動覆寫使用者 `test.yml`） |
| `config/LegacyDataMigrator.java`（新） | 首次啟動偵測 `plugins/dmenu`、`plugins/DGeyserMenu`、`plugins/dgeysermenu`、`plugins/MiaoMenu`，整批 `Files.walk` + `Files.copy` 到本插件 dataFolder；觸發條件嚴格（本資料夾 config.yml 不存在才跑） |
| `listeners/PlayerLifecycleListener_Folia.java` | 補上 `onDeath`，呼叫 `clockManager.removeClockFromDrops(event.getDrops())`，Folia 上死亡不再噴鐘 |
| `listeners/ClockInteractionListener.java` | 加入 `if (event.getHand() != EquipmentSlot.HAND) return;` 避免雙手雙觸發 |
| `menu/action/impl/CmdAction.java` | 在 dispatch 前用 `InputValidator.isSafeCommandContent` 過濾，注入內容拒絕並回送 `unsafe-input` log warning |
| `resources/plugin.yml` | `softdepend` 內 `Floodgate` → `floodgate` 與其 plugin name 對齊 |
| `resources/lang/{zh_TW,en}.yml` | `descriptions-missing` 訊息文字更新（已遷移到 lang，不再講 config.yml）|
| `pom.xml` | `version` 0.1 → 0.2 |
| `README.md` | 新增「無痛轉移」段、`0.2` changelog；改用「插件」用詞 |

**驗證**：`mvn -DskipTests clean package` BUILD SUCCESS；子代理 B、D、E 三輪獨立驗證全部 ✅。

**未動**：javamenu/、menu/action/（除了 CmdAction）、security/、proxy/ — 核心業務邏輯不動。

**收斂分支**：完成後把 `feat/i18n-26.1.2-fork` 合進 `main`，本地與 GitHub 只剩 `main` 分支，避免 fork 內亂枝。
