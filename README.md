# MiaoMenu / 喵喵菜单插件

[English](../README_en_us.md) | 中文

> 面向 Paper 1.21.11 的轻量级菜单插件，同时为 Java 版与基岩版玩家提供原生交互体验。

## 项目概览

MiaoMenu 是一个双端菜单插件：

- Java 玩家使用箱子 GUI 菜单
- Bedrock 玩家使用 Floodgate 表单菜单
- 自动识别玩家类型并打开对应菜单
- 支持 PlaceholderAPI、跨服跳转、热重载与条件系统
- 提供菜单时钟、示例菜单与权限控制

当前版本：`2.7.7.9`

## 界面预览

### Java 菜单预览

![Java 菜单预览 1](../pic/1.png)
![Java 菜单预览 2](../pic/2.png)

### Bedrock 菜单预览

![Bedrock 菜单预览 3](../pic/3.png)
![Bedrock 菜单预览 4](../pic/4.png)
![Bedrock 菜单预览 5](../pic/5.png)

## 核心优点

### 1. 双版本原生菜单体验
- Java 玩家看到熟悉的箱子菜单
- Bedrock 玩家看到适配移动端的原生表单
- 插件内部自动分流，无需手动区分命令入口

### 2. 面向实际服务器场景
- 支持 PlaceholderAPI 动态变量
- 支持 Floodgate / Geyser 场景下的基岩玩家菜单
- 支持 Velocity / BungeeCord 风格的跨服连接命令
- 支持 CraftEngine 自定义物品回退材质

### 3. 条件系统完整
- 支持菜单级 `view_requirement`
- 支持物品级 `conditions`
- 支持可复用的 `requirement_blocks`
- 支持 `deny_message` 与 `fallback_menu`
- 支持权限、进度、计分板、进度成就、占位符比较等条件

### 4. 运维体验友好
- 支持配置热重载
- 菜单示例文件可自动生成
- 菜单时钟支持自动发放、死亡保护、右键打开菜单
- 配置文本统一在 `config.yml` 的 `messages` 节点管理

### 5. 兼容现有配置思路
- Java 菜单结构与 DeluxeMenus 风格接近
- 对传统 YAML 菜单服管理方式更加友好

## 功能总览

### Java 菜单
Java 菜单位于 `src/main/resources/java_menus/`，支持：

- `menu_title`
- `rows`
- `items.<id>.slot`
- `material`
- `custom_model_data`
- `display_name`
- `lore`
- `left_click_commands`
- `right_click_commands`
- `conditions`
- `lock_message`
- `view_requirement`

示例文件：
- `test.yml`
- `server-selector.yml`

### Bedrock 菜单
基岩菜单位于 `src/main/resources/bedrock_menus/`，支持：

- `menu.title`
- `menu.subtitle`
- `menu.footer`
- `menu.items[*].text`
- `icon`
- `icon_type`
- `command`
- `execute_as`
- `conditions`
- `lock_message`
- `view_requirement`

### 智能菜单打开逻辑
插件会自动判断：

- 若玩家是 Floodgate 基岩玩家，则打开 Bedrock 菜单
- 否则打开 Java 菜单

这意味着同一个命令入口可以同时服务两类玩家。

### 命令系统
插件注册了以下命令：

```text
/dgeysermenu open <menu-name>
/dgeysermenu reload
/dgeysermenu help
/dgm open <menu-name>
/dgm reload
/dgm help
/fluxmenu open <menu-name>
/getmenuclock
```

说明：
- `dgm` 与 `fluxmenu` 是主命令别名
- `open` 用于打开指定菜单
- `reload` 用于重载配置与菜单文件
- `help` 用于显示帮助信息
- `getmenuclock` 用于获取菜单时钟

### 菜单时钟
菜单时钟是插件的一项特色功能：

- 玩家加入时可自动获得时钟
- 若时钟丢失，可自动补发
- 玩家死亡时，菜单时钟不会掉落
- 玩家右键时钟即可打开默认菜单
- 时钟名称由 `messages.menu.clock.name` 控制

### 热重载
在配置中启用后：

- 保存配置文件后可自动刷新菜单
- 无需频繁重启服务器
- 更适合高频调试菜单布局与按钮逻辑

### 跨服支持
插件支持代理环境中的跨服连接命令：

- 可检测 Velocity 模式
- 可检测 BungeeCord 风格信道
- 菜单按钮可以执行类似 `server lobby` 的跳转逻辑

示例见 `server-selector.yml`。

### PlaceholderAPI 支持
如果服务器安装了 PlaceholderAPI，可在：

- `display_name`
- `lore`
- 条件判断中的占位符
- 菜单提示文本

中使用动态变量，例如：

```yaml
display_name: "&b%player_name%"
lore:
  - "&f等级: &e%player_level%"
  - "&f金币: &6%vault_eco_balance%"
```

## 安装方法

### 环境要求
- Java 21
- Paper 1.21.11 或兼容实现
- 若需基岩菜单：安装 Floodgate
- 若需占位符解析：安装 PlaceholderAPI
- 若需跨服跳转：建议在代理环境下使用

### 安装步骤
1. 将插件 jar 放入服务器 `plugins` 目录
2. 启动服务器
3. 首次启动后会生成配置与示例菜单
4. 根据需求修改 `config.yml`、`java_menus/`、`bedrock_menus/`
5. 使用 `/dgm reload` 或重启服务器使配置生效

## 权限节点

来自 `plugin.yml` 的权限定义如下：

```yaml
dgeysermenu.*:
  children:
    dgeysermenu.use: true
    dgeysermenu.admin: true
    dgeysermenu.reload: true

dgeysermenu.use:
  default: true

dgeysermenu.admin:
  default: op

dgeysermenu.reload:
  default: op
```

### 权限说明
- `dgeysermenu.use`：允许使用菜单基础命令
- `dgeysermenu.admin`：允许使用管理功能与获取菜单时钟
- `dgeysermenu.reload`：允许重载配置
- `dgeysermenu.*`：授予全部权限

### 额外建议
在菜单条件中，你还可以自行引用其他权限节点，例如：

```yaml
requirements:
  - type: permission
    permission: vip.shop
```

这类权限并非插件固定注册项，但可以作为业务条件判断使用。

## 配置文件详解

主配置文件：`src/main/resources/config.yml`

### 顶层版本字段
```yaml
config-version: 15
menu-version: 6
```

- `config-version`：配置文件版本校验
- `menu-version`：示例菜单版本校验

### 打开菜单音效
```yaml
settings:
  open-menu-sound:
    enabled: true
    sound: "entity.experience_orb.pickup"
    volume: 1.0
    pitch: 1.0
```

说明：
- `enabled`：是否启用打开菜单音效
- `sound`：播放的原版声音键名
- `volume`：音量
- `pitch`：音调

### 默认菜单
```yaml
settings:
  default-menu: "test"
```

玩家右键菜单时钟时，会打开这里指定的默认菜单。

### 热重载
```yaml
settings:
  hot-reload:
    enabled: true
```

启用后，保存菜单文件时会尝试自动刷新。

### 自动生成示例
```yaml
settings:
  auto-generate-examples: true
```

启用后，缺失示例菜单时会自动补全。

### 代理网络支持
```yaml
settings:
  velocity-network: true
```

说明：
- 为 `true` 时优先按 Velocity 网络模式处理
- 适用于需要跨服连接命令的场景

### 自定义物品回退材质
```yaml
settings:
  item-resolver:
    fallback-material: STONE
```

当外部物品提供方不可用时，插件会回退到这里指定的原版材质。

### 菜单时钟
```yaml
settings:
  menu-clock:
    enabled: true
    give-on-join: true
```

说明：
- `enabled`：是否启用菜单时钟功能
- `give-on-join`：玩家加入时若没有时钟则自动给予

### 消息系统
```yaml
messages:
  message:
    no-permission: "&c✦ You do not have permission to use this command."
    players-only: "&c✦ Only players can use this command."
    menu-not-found: "&c✦ No menu named &e{0}&c was found. Please check the spelling."
```

说明：
- 所有可见提示文本尽量统一由 `messages` 节点管理
- 便于你自行改成中文、英文或服务器风格文案

## Java 菜单配置示例解释

示例文件：`src/main/resources/java_menus/test.yml`

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
    custom_model_data: 0
    display_name: "&e&lServer Info"
    lore:
      - "&7Click to view server information"
      - "&fOnline Players: &a%server_online%&f/&a%server_max_players%"
    left_click_commands:
      - "[message] &6=== Server Info ==="
      - "[player] list"
      - "[close]"
```

### 这一段代表什么
- `menu_title`：箱子菜单标题
- `rows`：菜单行数，只能是 1 到 6
- `view_requirement`：玩家能否打开整个菜单
- `deny_message`：不满足要求时发送的提示
- `fallback_menu`：不满足要求时跳转的替代菜单
- `slot`：按钮放在哪个格子
- `material`：按钮材质
- `custom_model_data`：资源包模型编号
- `display_name`：按钮标题
- `lore`：按钮说明
- `left_click_commands`：左键点击执行的动作列表

### 支持的材质来源
`test.yml` 中已经写明，`material` 可以来自多种来源：

- 原版材质，如 `PAPER`
- 原版材质 + `custom_model_data`
- `craftengine:namespace:item_id`
- `itemsadder:namespace:item_id`
- `mmoitems:type:id`
- `headdb:head_id`
- `base64head:base64_string`

## Java 菜单中的条件系统

### 物品条件示例
```yaml
player_info:
  conditions:
    operator: AND
    requirements:
      - type: placeholder_contains
        placeholder: "%player_name%"
        value: ""
  lock_message: "&cYou do not meet the requirements to view player info yet."
```

说明：
- `conditions`：物品级条件判断
- `operator`：多个条件之间的关系，可为 `AND` 或 `OR`
- `placeholder_contains`：判断占位符结果是否包含指定值
- `lock_message`：不满足条件时点击按钮显示的文本

### 复杂条件示例
```yaml
shop:
  conditions:
    operator: AND
    requirements:
      - type: advancement
        advancement: "minecraft:story/root"
    children:
      - operator: OR
        requirements:
          - type: permission
            permission: "vip.shop"
          - type: progress
            objective: "trade_count"
            value: 5
```

这一示例表示：
- 玩家必须先完成一个指定进度成就
- 然后再满足以下任意一个条件：
  - 拥有 `vip.shop` 权限
  - 计分板 `trade_count` 至少达到 5

## Bedrock 菜单配置示例解释

示例文件：`src/main/resources/bedrock_menus/test.yml`

```yaml
menu:
  title: "§6§lMain Menu"
  subtitle: "§7Welcome to the server!"
  footer: "§8Server version 1.21.x"
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

### 字段说明
- `title`：表单标题
- `subtitle`：副标题
- `footer`：页脚
- `items`：按钮列表
- `text`：按钮显示文本
- `icon`：图标路径或 URL
- `icon_type`：图标类型
- `command`：点击后执行的命令
- `execute_as`：以玩家或控制台身份执行
- `view_requirement`：菜单整体访问条件

## 跨服菜单示例

示例文件：`src/main/resources/java_menus/server-selector.yml`

```yaml
items:
  lobby:
    left_click_commands:
      - "[player] server lobby"
      - "[message] &aConnecting to lobby..."
      - "[close]"
```

这类写法适合：
- Velocity 网络
- BungeeCord 风格代理网络
- 大厅服 / 生存服 / 创造服 / 小游戏服切换入口

## 高级玩法示例

### 1. 打开另一个菜单
```yaml
left_click_commands:
  - "[menu] shop"
```

### 2. 给玩家发送提示
```yaml
left_click_commands:
  - "[message] &a欢迎使用菜单!"
```

### 3. 让玩家执行命令
```yaml
left_click_commands:
  - "[player] spawn"
```

### 4. 让控制台执行命令
```yaml
left_click_commands:
  - "[console] give %player_name% diamond 1"
```

### 5. 关闭菜单
```yaml
left_click_commands:
  - "[close]"
```

## 目录结构

```text
MiaoMenu/
├─ pic/
├─ docs/
├─ src/main/resources/
│  ├─ config.yml
│  ├─ plugin.yml
│  ├─ java_menus/
│  │  ├─ test.yml
│  │  └─ server-selector.yml
│  └─ bedrock_menus/
│     └─ test.yml
```

## 构建方法

```bash
mvn test
mvn package
```

默认产物会生成在 `target/` 目录下。

## 常见问题

### 1. 菜单打不开
请依次检查：
- 玩家是否拥有 `dgeysermenu.use`
- 菜单文件名与命令中的菜单名是否一致
- YAML 缩进是否正确
- `view_requirement` 是否拒绝了当前玩家

### 2. 基岩菜单没有显示
请检查：
- Floodgate 是否正确安装
- 玩家是否确实通过 Floodgate 接入
- `bedrock_menus/` 是否存在对应菜单

### 3. 按钮点击后没有效果
请检查：
- `command` 或点击动作是否写错
- 玩家本身是否有执行目标命令的权限
- 控制台是否报错

### 4. 占位符没有替换
请检查：
- PlaceholderAPI 是否已安装
- 使用的占位符是否来自已安装扩展
- 写法是否正确

### 5. 跨服命令无效
请检查：
- 代理环境是否正确工作
- `velocity-network` 配置是否符合你的网络架构
- 代理转发与信道是否可用

## 适用场景

MiaoMenu 适合以下服务器：

- 同时服务 Java 与 Bedrock 玩家
- 需要主菜单、功能导航、服务器选择器
- 希望使用 YAML 快速配置菜单
- 希望结合 PlaceholderAPI 展示动态数据
- 希望以低维护成本实现条件菜单系统

## License

本项目使用 `LICENSE` 文件中声明的许可证。
