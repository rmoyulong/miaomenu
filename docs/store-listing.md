# MiaoMenu_fork — Store / Website Listing Copy

可直接複製到 SpigotMC / Modrinth / Hangar / BuiltByBit 等插件網站。以下提供「英文 / 繁體中文」各一份短版與長版。Markdown 格式。

---

## 🇬🇧 English — Short

> **MiaoMenu_fork — one menu plugin for Java *and* Bedrock players.**
> A lightweight Paper / Folia plugin that serves Java players a chest GUI and Bedrock players a native Floodgate form — from the same YAML config. Localised, hot-reloadable, condition-aware, and drop-in compatible with `dmenu` / `DGeyserMenu` / upstream `MiaoMenu` data folders.

---

## 🇬🇧 English — Long

# 🍱 MiaoMenu_fork — Dual-Platform Menus, Done Right

**One plugin. Two player worlds.** MiaoMenu_fork shows Java players a familiar chest GUI and Bedrock players a native Floodgate form — driven by the *same* YAML files. No duplicated logic, no double-maintenance. Pair it with [Geyser](https://geysermc.org/) + [Floodgate](https://geysermc.org/wiki/floodgate/) and you're done.

It's a hard fork of the original [MiaoMenu](https://github.com/Yamada0001/MiaoMenu) tracking Paper / Folia **26.1.2** (with 26.2 alpha compatibility), restructured around a clean `lang/*.yml` system and drop-in migration from legacy plugin folders.

## Why MiaoMenu_fork?

- 🪞 **One config, two clients.** Same `view_requirement`, `conditions`, `requirement_blocks` and material resolver power both Java chest menus and Bedrock forms.
- 🧳 **Drop-in migration.** First start auto-imports `plugins/dmenu/`, `plugins/DGeyserMenu/`, `plugins/dgeysermenu/`, or `plugins/MiaoMenu/`. Old folders are left untouched.
- 🌐 **Speaks your language.** All visible text lives in `lang/<code>.yml`. Ships with English and Traditional Chinese; add a new locale by copying one file.
- 🔥 **Hot-reload everywhere.** Edit menus or lang files — they reload live, no server restart.
- 🛰️ **Cross-server ready.** Velocity / BungeeCord-style `server <name>` jumps work straight from menu buttons.
- 🧱 **Plays nice with item plugins.** CraftEngine, ItemsAdder, MMOItems, HeadDB and Base64 heads all resolved through one material string, with a configurable fallback.

## Features

- 🎮 **Native dual-platform menus** — chest GUI for Java, Floodgate form for Bedrock, auto-dispatched by player type.
- 🧭 **One smart entry point** — `/dgm open <menu>` works for everyone; the plugin figures out the right surface.
- 🧪 **Condition system** — menu-level `view_requirement`, item-level `conditions`, reusable `requirement_blocks`. AND / OR operators, nested children, permission / advancement / progress / scoreboard / placeholder checks.
- 💬 **Friendly denials** — every condition can supply a `deny_message` and a `fallback_menu` to redirect rejected players elsewhere.
- 🕒 **Menu clock** — auto-given on join, death-protected, right-click opens the default menu. Configurable on/off per server.
- 🎵 **Open-menu sound** — optional ambient sound when a menu opens (configurable name / volume / pitch).
- 🔁 **Hot-reload watcher** — debounced file watcher for `config.yml`, `java_menus/`, `bedrock_menus/`, `lang/`.
- 🛰️ **Cross-server commands** — Velocity / BungeeCord auto-detected; `[player] server lobby` from a menu just works.
- 🧮 **PlaceholderAPI** — full variable support in titles, lore, condition values and lock messages.
- 🛡️ **Built-in safety** — `InputValidator` rejects shell-injection-like content in `[cmd]` actions; per-player click rate-limiter prevents spam.
- 🧳 **Legacy migrator** — `LegacyDataMigrator` scans `plugins/<candidate>/` on first start and copies the whole folder in.
- 🌐 **i18n** — `lang/en.yml` (default), `lang/zh_TW.yml`, plus a `setDefaults` fallback so missing keys never show as raw strings.
- 🔧 **Backward-compatible** — original `/dgeysermenu`, `/dgm`, `/fluxmenu` commands, `dgeysermenu.*` permissions and existing `config.yml` keys are preserved.

## How it works

1. Drop the jar in `plugins/`. On first start, MiaoMenu_fork creates `config.yml`, `lang/en.yml`, `lang/zh_TW.yml`, plus sample menus under `java_menus/` and `bedrock_menus/`. If a legacy folder exists, its data is auto-imported.
2. Edit a YAML menu — both Java and Bedrock players will see the same logical buttons.
3. A player runs `/dgm open <menu>`. The plugin checks `view_requirement`, picks the right surface (chest or Floodgate form), and renders the menu with all conditions evaluated.
4. Save your YAML — hot-reload picks it up. Players opening the menu next see the updated version.

## Commands & Permissions

| Command | Permission | Description |
| --- | --- | --- |
| `/dgeysermenu open <menu>` | `dgeysermenu.use` | Open a menu by name (Java or Bedrock auto-picked) |
| `/dgm open <menu>` | `dgeysermenu.use` | Short alias for `/dgeysermenu` |
| `/fluxmenu open <menu>` | `dgeysermenu.use` | Legacy alias (preserved) |
| `/mmf open <menu>` | `dgeysermenu.use` | New fork alias |
| `/dgm reload` | `dgeysermenu.reload` | Reload config, lang files and menu definitions |
| `/dgm help` | `dgeysermenu.use` | Print help, localised |
| `/getmenuclock` | `dgeysermenu.admin` | Grant the caller a menu clock |

Permission tree: `dgeysermenu.use` (default everyone), `dgeysermenu.admin` (default op), `dgeysermenu.reload` (default op), `dgeysermenu.*` (all).

## Configuration

A small `config.yml` (language, open-menu sound, default menu, hot-reload toggle, sample auto-gen, proxy mode, fallback material, menu-clock options) plus per-language `lang/<code>.yml` for every visible string. Menus go under `java_menus/` and `bedrock_menus/` with parallel YAML schemas. Edit, then `/dgm reload` — no restart.

## Requirements

- **Paper / Folia 26.1.2** (also runs on 26.2 alpha)
- **Java 21**
- *(Optional)* **Floodgate 2.2.5+** and **Geyser 2.10.x** for Bedrock support
- *(Optional)* **PlaceholderAPI** for `%placeholders%`
- *(Optional)* **Velocity / BungeeCord** proxy for `server <name>` jumps
- *(Optional)* **CraftEngine / ItemsAdder / MMOItems / HeadDatabase** for custom items

---

## 🇹🇼 繁體中文 — 短版

> **MiaoMenu_fork — 一套選單插件，Java 與基岩玩家都吃得到。**
> 一個輕量級 Paper / Folia 插件：同一份 YAML，Java 玩家看到熟悉的箱子 GUI、基岩玩家看到 Floodgate 原生表單。支援多語系、熱重載、條件系統，並能無痛從 `dmenu` / `DGeyserMenu` / 原作 `MiaoMenu` 等舊資料夾自動匯入。

---

## 🇹🇼 繁體中文 — 長版

# 🍱 MiaoMenu_fork — 雙端選單，一次到位

**一個插件，兩個玩家世界。** MiaoMenu_fork 讓 Java 玩家看到熟悉的箱子 GUI，基岩玩家看到原生 Floodgate 表單——驅動兩者的是**同一份** YAML。不需要寫兩套邏輯、不需要維護兩份檔案。搭配 [Geyser](https://geysermc.org/) + [Floodgate](https://geysermc.org/wiki/floodgate/)，丟進去就能跑。

這是原作 [MiaoMenu](https://github.com/Yamada0001/MiaoMenu) 的 hard fork，鎖定 Paper / Folia **26.1.2**（亦相容 26.2 alpha），並把訊息系統重構為乾淨的 `lang/*.yml` 架構，同時支援從舊資料夾無痛遷移。

## 為什麼用 MiaoMenu_fork？

- 🪞 **一份設定、兩種客戶端。** 同一組 `view_requirement`、`conditions`、`requirement_blocks` 與材質解析器同時驅動 Java 箱子選單與基岩表單。
- 🧳 **無痛遷移。** 首次啟動時自動匯入 `plugins/dmenu/`、`plugins/DGeyserMenu/`、`plugins/dgeysermenu/`、`plugins/MiaoMenu/`。舊資料夾原封不動。
- 🌐 **說你的語言。** 所有可見文字都在 `lang/<code>.yml`。內建英文與繁體中文，新增語系只要複製一個檔案。
- 🔥 **全面熱重載。** 編輯選單或語言檔即時生效，不必重啟伺服器。
- 🛰️ **跨服就緒。** 選單按鈕直接寫 Velocity / BungeeCord 風格的 `server <name>` 跳轉。
- 🧱 **與物品插件友善共處。** CraftEngine、ItemsAdder、MMOItems、HeadDB、Base64 頭顱全部透過同一個 material 字串解析，並可設定 fallback 材質。

## 功能特色

- 🎮 **原生雙端選單** — Java 用箱子 GUI、基岩用 Floodgate 表單，依玩家類型自動分流。
- 🧭 **單一入口** — `/dgm open <menu>` 對所有玩家都通用，插件自己決定要開哪一種介面。
- 🧪 **條件系統** — 選單級 `view_requirement`、物品級 `conditions`、可重用的 `requirement_blocks`。支援 AND / OR、巢狀 children；條件類型涵蓋權限、成就、進度、計分板、佔位符比對。
- 💬 **友善的拒絕** — 每個條件都可指定 `deny_message` 與 `fallback_menu`，把不符合條件的玩家導向別處而非冷冰冰拒絕。
- 🕒 **選單時鐘** — 加入時自動發放、死亡不掉、右鍵開啟預設選單，可在 `config.yml` 開關。
- 🎵 **開啟選單音效** — 開選單時播放指定音效（音效名稱 / 音量 / 音調可設定）。
- 🔁 **熱重載監視器** — 對 `config.yml`、`java_menus/`、`bedrock_menus/`、`lang/` 都做了 debounce 監聽。
- 🛰️ **跨服指令** — 自動偵測 Velocity / BungeeCord；選單按鈕寫 `[player] server lobby` 即可。
- 🧮 **PlaceholderAPI** — 標題、lore、條件值、鎖定訊息都支援。
- 🛡️ **內建安全防護** — `InputValidator` 過濾 `[cmd]` 動作的注入風險；點擊速率限制器防洗版。
- 🧳 **舊資料遷移器** — `LegacyDataMigrator` 首次啟動掃 `plugins/<候選>/` 並整批複製過來。
- 🌐 **多語系** — `lang/en.yml`（預設）與 `lang/zh_TW.yml`，再加上 `setDefaults` 雙保險，缺鍵不會顯示原始字串。
- 🔧 **向後相容** — `/dgeysermenu`、`/dgm`、`/fluxmenu` 指令、`dgeysermenu.*` 權限樹、既有 `config.yml` 鍵名都保留。

## 運作流程

1. 把 jar 丟到 `plugins/`。首次啟動時 MiaoMenu_fork 會生 `config.yml`、`lang/en.yml`、`lang/zh_TW.yml`、與 `java_menus/`、`bedrock_menus/` 範例。若偵測到舊版資料夾，會自動整批匯入。
2. 編輯 YAML 選單——Java 與基岩玩家會看到同樣的邏輯按鈕。
3. 玩家輸入 `/dgm open <menu>`。插件先檢查 `view_requirement`，再依玩家類型挑介面（箱子 / 表單），並依條件系統渲染按鈕。
4. 儲存 YAML——熱重載自動接手。下次有人開選單就會看到新內容。

## 指令與權限

| 指令 | 權限 | 說明 |
| --- | --- | --- |
| `/dgeysermenu open <menu>` | `dgeysermenu.use` | 依名稱開啟選單（Java / 基岩自動挑選） |
| `/dgm open <menu>` | `dgeysermenu.use` | `/dgeysermenu` 的短別名 |
| `/fluxmenu open <menu>` | `dgeysermenu.use` | 舊版別名（保留） |
| `/mmf open <menu>` | `dgeysermenu.use` | Fork 新增別名 |
| `/dgm reload` | `dgeysermenu.reload` | 重新載入設定、語言檔與選單定義 |
| `/dgm help` | `dgeysermenu.use` | 顯示說明（依當前語系） |
| `/getmenuclock` | `dgeysermenu.admin` | 為自己取得一個選單時鐘 |

權限樹：`dgeysermenu.use`（預設所有人）、`dgeysermenu.admin`（預設 OP）、`dgeysermenu.reload`（預設 OP）、`dgeysermenu.*`（全部）。

## 設定

精簡的 `config.yml`（語系、開啟選單音效、預設選單、熱重載開關、自動產生範例、代理模式、fallback 材質、選單時鐘開關）外加各語言的 `lang/<code>.yml` 訊息檔；選單放在 `java_menus/` 與 `bedrock_menus/`，兩端共用平行的 YAML schema。改完 `/dgm reload`，免重啟。

## 需求

- **Paper / Folia 26.1.2**（也能跑在 26.2 alpha）
- **Java 21**
- *（可選）* **Floodgate 2.2.5+** 與 **Geyser 2.10.x**：開啟基岩玩家支援
- *（可選）* **PlaceholderAPI**：開啟 `%placeholders%`
- *（可選）* **Velocity / BungeeCord** 代理：開啟 `server <name>` 跨服跳轉
- *（可選）* **CraftEngine / ItemsAdder / MMOItems / HeadDatabase**：開啟自訂物品
