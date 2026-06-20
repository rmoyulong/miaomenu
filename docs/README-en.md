[English](./README-en.md) | [繁體中文](../README.md) | [Store Listing](./store-listing.md)

# MiaoMenu_fork — Dual-platform Menu Plugin (Fork)

> 📦 Download / Modrinth: https://modrinth.com/plugin/miaomenu_fork
> Fork:                  https://github.com/Avery11111101/MiaoMenu_fork
> Original / 原作:        https://github.com/Yamada0001/MiaoMenu
>
> A lightweight menu plugin for Paper / Folia / Geyser **26.1.2** (with 26.2 alpha compatibility), serving both Java and Bedrock players natively. Ships with `en` (default) and `zh_TW` locales.

**Current version**: `1.1` (Internal-only health check loop on top of `1.0` stable; **zero user-facing changes** — `config.yml`, menu YAMLs, commands, permissions and versions are all unchanged. Fork numbering reset from upstream 2.7.7.9)

---

## Highlights

- **Dual-platform native menus** — Java players get a chest GUI, Bedrock players get a Floodgate form. Auto-dispatched.
- **i18n via `lang/<language>.yml`** — All visible strings live outside `config.yml`; drop in a new locale file to translate.
- **Drop-in migration** — On first start, the plugin auto-imports data from `plugins/dmenu/`, `plugins/DGeyserMenu/`, `plugins/dgeysermenu/`, and `plugins/MiaoMenu/` into `plugins/MiaoMenu_fork/`. Old folders are left untouched.
- **Hot-reload** — Both menu YAML files and `lang/` files reload live.
- **Cross-server commands** — Velocity / BungeeCord style `server <name>` jumps from menu buttons.
- **PlaceholderAPI** — Variables work in titles, lore, and condition checks.
- **Condition system** — Menu-level `view_requirement`, item-level `conditions`, reusable `requirement_blocks`, with `deny_message` / `fallback_menu` / `lock_message`.
- **Menu clock** — Auto-given on join, death-protected, right-click to open the default menu.
- **CraftEngine / ItemsAdder / MMOItems / HeadDB / Base64 heads** — Material resolver with fallback.

---

## Backward Compatibility

The fork is built around **zero user-facing changes**:

- Commands `/dgeysermenu`, `/dgm`, `/fluxmenu` are preserved. `/mmf` is added as a new alias.
- Sub-commands `open / reload / help` and `/getmenuclock` are preserved.
- Permission nodes `dgeysermenu.*`, `dgeysermenu.use`, `dgeysermenu.admin`, `dgeysermenu.reload` are preserved.
- `config.yml`, `java_menus/*.yml`, `bedrock_menus/*.yml` keys and structure are preserved.

The only new setting is `language: en|zh_TW` (defaults to `en`). If you keep your old `config.yml`, the plugin detects `config-version` and fills in missing keys without breaking your customisations.

---

## Quick Start

### Requirements
- Java 21
- Paper / Folia **26.1.2** (also runs on 26.2 alpha)
- (Optional) Floodgate 2.2.5+ and Geyser 2.10.x for Bedrock players
- (Optional) PlaceholderAPI for placeholders
- (Optional) Velocity / BungeeCord proxy for `server <name>` jumps

### Install
1. Drop `MiaoMenu_fork-1.1.jar` into `plugins/`.
2. Start the server. The plugin auto-generates `config.yml`, `lang/en.yml`, `lang/zh_TW.yml`, and sample menus.
3. (If migrating) Old data is auto-imported from legacy folders on the first run.
4. Edit `config.yml` to change `language: en` / `zh_TW`, or edit menus under `java_menus/` / `bedrock_menus/`.
5. Run `/dgm reload` to apply.

### Commands
```text
/dgeysermenu open <menu>
/dgeysermenu reload
/dgeysermenu help
/dgm open <menu>            # short alias
/fluxmenu open <menu>       # legacy alias
/mmf open <menu>            # new fork alias
/getmenuclock               # admin: grab a menu clock
```

### Permissions
- `dgeysermenu.use` — open menus (default: everyone)
- `dgeysermenu.admin` — admin functions, `/getmenuclock` (default: op)
- `dgeysermenu.reload` — `/dgm reload` (default: op)
- `dgeysermenu.*` — all of the above

---

## Java Menu Example (`java_menus/test.yml`)

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

Supported `material` sources: vanilla items (with `custom_model_data`), `craftengine:ns:id`, `itemsadder:ns:id`, `mmoitems:type:id`, `headdb:id`, `base64head:<b64>`.

---

## Bedrock Menu Example (`bedrock_menus/test.yml`)

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

## Localisation

```yaml
# config.yml
language: en      # or zh_TW, or any <code> matching a lang/<code>.yml file
```

- Built-in locales: `en` (default), `zh_TW` (Traditional Chinese, Taiwan).
- Add a locale by dropping `plugins/MiaoMenu_fork/lang/<code>.yml`.
- Missing keys fall back to `lang/en.yml` bundled in the jar.
- Hot-reload picks up `lang/` edits without `/dgm reload`.

---

## Build

```bash
mvn package
```

Output: `target/MiaoMenu_fork-1.1.jar`.

---

## Changelog

The full changelog lives in [更新日誌.md](../更新日誌.md) at the repo root (Traditional Chinese + English, side by side).

---

## Credits

- Upstream: [Yamada0001/MiaoMenu](https://github.com/Yamada0001/MiaoMenu) — no fork without the original.
- i18n inspiration: [Avery11111101/AFly](https://github.com/Avery11111101/AFly).

## License

See `LICENSE` in the repository root.
