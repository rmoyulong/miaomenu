[English](./README-en.md) | [繁體中文](../README.md) | [Store Listing](./store-listing.md)

# MiaoMenu_fork — Dual-platform Menu Plugin (Fork)

> Fork:           https://github.com/Avery11111101/MiaoMenu_fork
> Original / 原作: https://github.com/Yamada0001/MiaoMenu
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

### `1.1` (2026-06-20, internal health-check loop)

> Same "zero user-facing change" contract as `1.0`. A second multi-agent **scan → severity-validate → fix → cross-verify** loop was run, with two independent verifier agents both signing off clean before the release. `config.yml`, menu YAMLs, commands, permissions, `config-version` and `menu-version` are all unchanged.

**Security / correctness**
- **Permission requirement is fail-closed.** Writing `type: permission` without a `permission:` key used to allow players through; it now denies, eliminating an accidental-bypass class of bug.
- **`material` null-tolerant.** YAML `material: ~` or empty string in `java_menus/` (and `fallback-material` in `config.yml`) used to throw `IllegalArgumentException` and kill onEnable / menu open; now falls back to `STONE`.
- **Unknown requirement `type` no longer silent.** A typo like `type: permssion` used to be silently skipped, leaving conditions unenforced; now logs a one-shot warning per type (further occurrences suppressed).
- **`ScoreboardManager` null guard.** During early startup or under abnormal Folia states `getScoreboardManager()` can return null; checked before use to avoid NPE.
- **Hot-reload watcher shutdown order.** `HotReloadManager.shutdown()` now closes the `WatchService` first, then interrupts and `join(1000ms)`s the watcher thread to ensure clean termination on disable.
- **`SoundsClock` resilience.** Wrapped in try-catch + `plugin.isEnabled()` so a disable-time menu open won't crash the flow with `IllegalStateException`.
- **`saveResourceIfNotExists` no longer silent.** When a packaged sample is missing from the jar, the failure is now warned instead of swallowed.
- **`OpenCommand` tab-complete defensive.** Guards against `args.length == 0` and `args[0] == null`.

**Memory / resources**
- **`RateLimiter` lazy sweep + proactive removal.** A CAS-guarded sweep every 60s drops entries older than 5×window, and `PlayerQuitEvent` proactively removes the UUID — preventing slow memory growth on long-running servers.
- **`MenuClockManager.playerHasClock` scans full inventory.** Switched from `getStorageContents()` (main inv only) to `getContents()` so a menu clock stashed in the off-hand isn't mistaken for "missing" and duplicated on the next join.

**PlaceholderAPI consistency**
- **Lock messages now resolve PlaceholderAPI.** `MenuItem.resolveLockMessage` used to pass `null` as the plugin to `PlaceholderUtils.parse`, so `%placeholders%` in `deny_message` / `lock_message` weren't substituted; the plugin reference is now correctly threaded through.
- **`JavaMenuListener` single-evaluate.** A new `LockState` record exposes locked-flag + lock-message in one call, replacing two separate evaluations and eliminating any in-between inconsistency.

**Decoupling / maintainability**
- **`RequirementService` takes `Plugin`.** Constructor changed from `RequirementService(MiaoMenu)` to `RequirementService(Plugin)` + a test-friendly `(Plugin, Server)` overload. `evaluateAdvancement` and `getScore` use the injected `Server` directly.
- **`JavaMenu` takes `Plugin`.** Hard-coded `MiaoMenu` field replaced with `Plugin` for looser coupling.
- **`MiaoMenu` main class is no longer `final`.** No semantic reason for it to be final; removing it makes testing and future extension cleaner.
- **`PlayerLifecycleListener` / `_Folia` onQuit null guard.** Defensive null check on `RateLimiter` in case quit fires during a partial onDisable.

**Verification flow**
- 1 scanner found 20 candidates → main agent validated → 8 real / 12 false-positives → fixes applied → verifier A caught 1 missing thread (PlaceholderAPI plugin) → patched → adversarial verifier B caught 5 mediums (4 fixed; 1 `BungeeCord` detection M-4 deferred to v1.1.1) → verifier A' caught `OpenCommand` defensive → patched → verifier C and Plan-style verifier D both signed off clean for two consecutive rounds → released.
- Progress was streamed to Discord webhook each round.
- `README.AI.md` updated with the full motivation / step trail.

**Deferred to v1.1.1**
- **M-4**: `ProxyManager.detectProxyType` uses `net.md_5.bungee.api.ChatColor` class presence to detect BungeeCord — unreliable because Paper / Spigot ship that class. The default `velocity-network: true` path masks this; detection logic needs rewriting.
- `pom.xml` should switch to `<release>21</release>` instead of `<source>` / `<target>` (low severity).

### `1.0` (2026-06-20, stable release)

- After two rounds of multi-agent scan / fix / verify loops (`0.1` → `0.2`), all P0 / P1 findings are clean — promoted to stable.
- Version jumps from the `0.x` preview range to `1.0` to signal that the "zero user-facing change" compatibility contract has been validated.
- Trilingual READMEs (Traditional Chinese / English / Simplified Chinese) are aligned to this version with the full changelog.
- Build artefact renamed to `MiaoMenu_fork-1.1.jar`. No code logic changed in this release — docs, version strings and packaging numbers only; runtime behaviour is equivalent to `0.2`.

### `0.2` (2026-06-20, stability & migration)

- **Drop-in migration**: new `LegacyDataMigrator` imports from `plugins/dmenu`, `plugins/DGeyserMenu`, `plugins/dgeysermenu`, `plugins/MiaoMenu` on first start.
- **Fix**: plugin no longer fails to enable when Floodgate is absent — `BedrockMenuManager` reflection is lazy.
- **Fix**: `/getmenuclock` executor was missing; now wired with `dgeysermenu.admin` and Player check.
- **Fix**: `MENU_VERSION` constant corrected (3 → 6) — sample `test.yml` is no longer overwritten on every start.
- **Fix (Folia)**: `PlayerLifecycleListener_Folia` adds `onDeath` so the menu clock no longer drops on death.
- **Fix**: Bedrock `requirement_blocks` shared conditions now resolved (real blocks map passed instead of empty).
- **Security**: `CmdAction` now passes through `InputValidator.isSafeCommandContent`; injected content is rejected with an `unsafe-input` message.
- **UX**: clock right-click no longer double-fires (off-hand event is dropped).
- **plugin.yml**: `softdepend` casing fixed (`Floodgate` → `floodgate`).
- **i18n**: `menu.locked-tag` extracted so the Bedrock locked-button tag is translatable.

### `0.1` (2026-06-20, fork start)

- Bumped to MC 26.x: `paper-api` → 26.1.2.build.72-stable, `folia-api` → 26.1.2.build.8-stable.
- `floodgate` 2.2.0 → 2.2.5-SNAPSHOT; added `geyser` 2.10.1-SNAPSHOT soft-dep.
- `plugin.yml` `api-version` → `'26.1'`.
- i18n split: `messages:` block in `config.yml` moved to `lang/<language>.yml`; `en` and `zh_TW` shipped.
- `Lang.load()` lookup order: `lang/<language>.yml` → `lang/en.yml` fallback → `config.yml` (backward compatible).
- Hot-reload now watches `lang/` too.
- New `/mmf` alias; `/dgm`, `/fluxmenu`, `/dgeysermenu` untouched.
- Renamed to `MiaoMenu_fork`: jar + plugin folder renamed; commands / permissions / config keys unchanged.

---

## Credits

- Upstream: [Yamada0001/MiaoMenu](https://github.com/Yamada0001/MiaoMenu) — no fork without the original.
- i18n inspiration: [Avery11111101/AFly](https://github.com/Avery11111101/AFly).

## License

See `LICENSE` in the repository root.
