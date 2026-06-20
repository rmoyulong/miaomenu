[English](./README-en.md) | [繁體中文](../README.md) | [简体中文](./README-zh.md)

# MiaoMenu_fork — 喵喵菜单插件分支版

> Fork:           https://github.com/Avery11111101/MiaoMenu_fork
> 原作 / Original: https://github.com/Yamada0001/MiaoMenu
>
> 面向 Paper / Folia / Geyser **26.1.2**（亦兼容 26.2 alpha）的轻量级菜单插件，同时为 Java 版与基岩版玩家提供原生交互体验，内置 `en` 英文（默认）与 `zh_TW` 繁体中文双语切换。

**当前版本**：`0.2`（Fork 版重新起算；原作为 2.7.7.9）

> 想看繁体中文完整说明，请到仓库根目录的 `README.md`。本文件只列重点。

---

## 亮点

- **双端原生菜单**：Java 玩家看到熟悉的箱子选单，基岩玩家看到 Floodgate 原生表单；插件内部自动分流。
- **i18n 多语系**：所有可见文字搬到 `lang/<language>.yml`，加新语系直接丢一个文件就行。
- **无痛迁移**：首次启动会自动从 `plugins/dmenu/`、`plugins/DGeyserMenu/`、`plugins/dgeysermenu/`、`plugins/MiaoMenu/` 整批匯入到 `plugins/MiaoMenu_fork/`，旧文件夹原封不动。
- **热重载**：菜单 YAML 与 `lang/` 文件即时生效。
- **跨服指令**：菜单按钮可直接发出 Velocity / BungeeCord 风格的 `server <name>` 跳转。
- **PlaceholderAPI**：标题、lore、条件判断都支持占位符。
- **条件系统**：选单级 `view_requirement`、物品级 `conditions`、可重用 `requirement_blocks`、`deny_message` / `fallback_menu` / `lock_message`。
- **选单时钟**：加入服务器自动给予、死亡保护、右键开启默认选单。
- **多来源物品**：CraftEngine / ItemsAdder / MMOItems / HeadDB / Base64 头颅，并附备援材质。

---

## 向后兼容声明

Fork 版重点放在 **不改使用者操作习惯**：

- 主指令 `/dgeysermenu`、`/dgm`、`/fluxmenu` 全数保留，**额外新增** `/mmf` 别名。
- 子指令 `open / reload / help`、`/getmenuclock` 全数保留。
- 权限节点 `dgeysermenu.*`、`dgeysermenu.use`、`dgeysermenu.admin`、`dgeysermenu.reload` 全数保留。
- `config.yml`、`java_menus/*.yml`、`bedrock_menus/*.yml` 键名与结构保留，原本的配置文件可以直接带过来。

唯一新增的设定是 `language: en|zh_TW`（默认 `en`），并把 `config.yml` 内原本的 `messages:` 区块搬到 `lang/<language>.yml`。

---

## 快速上手

### 环境需求
- Java 21
- Paper / Folia **26.1.2**（也兼容 26.2 alpha）
- （可选）Floodgate 2.2.5+ 与 Geyser 2.10.x（基岩玩家用）
- （可选）PlaceholderAPI（占位符）
- （可选）Velocity / BungeeCord 代理（跨服跳转）

### 安装步骤
1. 把 `MiaoMenu_fork-0.2.jar` 丢进 `plugins/`。
2. 启动服务器，自动生成 `config.yml`、`lang/en.yml`、`lang/zh_TW.yml` 与示例选单。
3. （迁移情境）首次启动会从旧版插件文件夹自动匯入数据。
4. 修改 `config.yml` 中的 `language: en` / `zh_TW`，或编辑 `java_menus/`、`bedrock_menus/` 内的 YAML。
5. 执行 `/dgm reload` 即可生效。

### 指令一览
```text
/dgeysermenu open <menu>
/dgeysermenu reload
/dgeysermenu help
/dgm open <menu>            # 短别名
/fluxmenu open <menu>       # 旧别名
/mmf open <menu>            # Fork 新别名
/getmenuclock               # 管理员：取得选单时钟
```

### 权限
- `dgeysermenu.use` — 开启选单（默认所有人）
- `dgeysermenu.admin` — 管理功能与 `/getmenuclock`（默认 op）
- `dgeysermenu.reload` — `/dgm reload`（默认 op）
- `dgeysermenu.*` — 以上全部

---

## Java 选单示例（`java_menus/test.yml`）

```yaml
menu_title: "&6&lMain Menu &7| &fServer Name"
rows: 6
view_requirement:
  deny_message: "&cYou cannot open this menu yet."
  fallback_menu: "test"
  requirements:
    - type: permission
      permission: dgeysermenu.use
items:
  server_info:
    slot: 10
    material: KNOWLEDGE_BOOK
    display_name: "&e&lServer Info"
    lore:
      - "&7Click to view server information"
      - "&fOnline Players: &a%server_online%&f/&a%server_max_players%"
    left_click_commands:
      - "[message] &6=== Server Info ==="
      - "[player] list"
      - "[close]"
```

`material` 支持：原版材质（搭配 `custom_model_data`）、`craftengine:ns:id`、`itemsadder:ns:id`、`mmoitems:type:id`、`headdb:id`、`base64head:<b64>`。

---

## 基岩选单示例（`bedrock_menus/test.yml`）

```yaml
menu:
  title: "§6§lMain Menu"
  subtitle: "§7Welcome to the server!"
  footer: "§8Server version 26.1.x"
  items:
    - text: "§a§lTeleport\n§7Quickly travel to different locations"
      icon: "textures/items/compass_item"
      icon_type: "path"
      command: "warp"
      execute_as: "player"
view_requirement:
  deny_message: "&cYou cannot open the Bedrock main menu right now."
  fallback_menu: "test"
  requirements:
    - type: permission
      permission: dgeysermenu.use
```

---

## 多语系

```yaml
# config.yml
language: en      # 或 zh_TW，或任何符合 lang/<code>.yml 的代号
```

- 内建语系：`en`（默认）、`zh_TW`（繁体中文）。
- 自订语系：在 `plugins/MiaoMenu_fork/lang/` 内新增 `<code>.yml`。
- 缺少的键会自动 fallback 到 jar 内 `lang/en.yml`，不会出现空讯息。
- 热重载会自动监听 `lang/`，不用每次 `/dgm reload`。

---

## 建置

```bash
mvn package
```

产物：`target/MiaoMenu_fork-0.2.jar`。

---

## 更新日志

### `0.2`（2026-06-20，稳定性与迁移）

- **无痛转移**：新增 `LegacyDataMigrator`，首次启动自动从旧版插件文件夹匯入数据。
- **修复**：没装 Floodgate 时插件启动失败 — `BedrockMenuManager` 改为 lazy 反射初始化。
- **修复**：`/getmenuclock` 指令未挂载 — 补上 executor、权限与 Player 检查。
- **修复**：`MENU_VERSION` 常数 3 → 6，不再每次启动覆写使用者 `test.yml`。
- **修复 (Folia)**：玩家死亡会喷钟 — `PlayerLifecycleListener_Folia` 补上 `onDeath`。
- **修复**：基岩选单 `requirement_blocks` 共享条件失效 — 改传真实 blocks map。
- **安全**：`CmdAction` 接入 `InputValidator.isSafeCommandContent`，注入内容直接拒绝。
- **体验**：时钟右键不再双手双触发。
- **plugin.yml**：`softdepend` 大小写修正（`Floodgate` → `floodgate`）。
- **多语系**：`menu.locked-tag` 抽出，基岩鎖定按钮的「[未解锁]」标签改由 lang 控制。

### `0.1`（2026-06-20，Fork 起点）

- 跟上 MC 26.x：`paper-api` → 26.1.2.build.72-stable、`folia-api` → 26.1.2.build.8-stable。
- 升级 `floodgate` 2.2.0 → 2.2.5-SNAPSHOT；新增 `geyser` 2.10.1-SNAPSHOT 软相依。
- `plugin.yml` 的 `api-version` 改为 `'26.1'`。
- 多语系拆分：`config.yml` 内 `messages:` 区块抽出为 `lang/<language>.yml`，内建 `en`（默认）与 `zh_TW`。
- `Lang.load()` 查询顺序：`lang/<language>.yml` → `lang/en.yml` fallback → `config.yml`（向后兼容）。
- 热重载延伸：监听 `lang/`，编辑语系档即时生效。
- 指令别名新增 `/mmf`，不影响原本 `/dgm`、`/fluxmenu`、`/dgeysermenu`。
- 改名 `MiaoMenu_fork`：jar 与插件文件夹改名；指令、权限节点、配置键名「完全没动」。

---

## 鸣谢

- 原作：[Yamada0001/MiaoMenu](https://github.com/Yamada0001/MiaoMenu) — 没有原版就没有这个 Fork。
- 多语系架构灵感：[Avery11111101/AFly](https://github.com/Avery11111101/AFly)。

## License

详见仓库根目录的 `LICENSE`。
