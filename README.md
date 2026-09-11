[logo]: https://github.com/HelpChat/DeluxeMenus/assets/52609756/f24ac57d-98db-4d57-a723-791a2654e73f

[upstream]: https://github.com/HelpChat/DeluxeMenus
[upstreamSpigot]: https://www.spigotmc.org/resources/11734/
[upstreamWiki]: https://wiki.helpch.at/helpchat-plugins/deluxemenus
[upstreamDiscord]: https://helpch.at/discord

[fork]: https://TODO-fork-repository-url
[forkIssues]: https://TODO-fork-issue-tracker-url
[forkDiscord]: https://TODO-discord-invite
[forkReleases]: https://TODO-releases-download-url
[forkDocs]: https://TODO-fork-docs-url

[license]: LICENSE
[licenseImg]: https://img.shields.io/github/license/helpchat/deluxemenus?&logo=github
[contributing]: CONTRIBUTING.md

[![logo]][upstream]

[![licenseImg]][license]

# NeoDeluxeMenus (Fork)

> **This is an unofficial fork of [DeluxeMenus][upstream] by HelpChat.**
> It is not affiliated with, endorsed by, or supported by HelpChat or ExtendedClip.
> Do **not** report issues with this fork to the upstream project.

This fork tracks upstream and adds extra features on top, while narrowing platform support
so the code can use modern Paper APIs directly.

## What is different in this fork

- **Paper only.** Paper and Paper forks (Purpur, Folia-family forks, Pufferfish, etc.).
  Spigot and CraftBukkit are **not supported** and will not be supported.
- **Minecraft 1.21.11 and newer only.** Older versions are out of scope; use upstream
  DeluxeMenus if you run an older server.
- **Fully config compatible.** Every existing DeluxeMenus configuration works unchanged.
  All original features behave exactly as they do in the upstream plugin, drop the jar in,
  keep your menus. New features are strictly additive and opt-in.
- **`hide_player_inventory`.** Per-menu option that hides the player inventory shown
  underneath a menu. Purely visual — it only blanks the outgoing container packets, the
  server side inventory is never touched, so items cannot be lost. Requires
  [PacketEvents](https://github.com/retrooper/packetevents); ignored without it.
- **`location: bottom`.** Per-item option that places a button in the player inventory
  area underneath the menu instead of in the menu itself. Defaults to `top`. Slots are
  numbered like a player inventory: `0-8` is the hotbar, `9-35` is the main storage.
  Everything else about the item (`slots`, `priority`, `view_requirement`, all click
  handlers, `update`) works exactly as it does for normal items.

  Buttons drawn there are never given to the player — they are only substituted into the
  same packets `hide_player_inventory` already blanks — so they cannot be taken, dropped
  or duped, and the real inventory comes back untouched when the menu closes. Because of
  that, the option **requires `hide_player_inventory: true` on the same menu** (plus
  PacketEvents); bottom items on any other menu are skipped with a warning.

  ```yaml
  items:
    close_button:
      material: BARRIER
      location: bottom
      slot: 4            # fifth hotbar slot
      display_name: "&cClose"
      left_click_commands:
        - "[close]"
  ```

## Requirements

| Requirement | Version |
| --- | --- |
| Server software | Paper (or a Paper fork) |
| Minecraft | 1.21.11 → latest |
| Java | 25+ |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Required |

## Optional integrations

Vault, HeadDatabase, HeadDB, CraftEngine, ItemsAdder, Nexo, Oraxen, ExecutableItems,
ExecutableBlocks, SCore, SimpleItemGenerator, MMOItems.

[PacketEvents](https://github.com/retrooper/packetevents) — required only by the
`hide_player_inventory` menu option.

## Installation

1. Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).
2. Download the jar from [releases][forkReleases] (or build it yourself, below).
3. Drop it in `plugins/`, remove any existing DeluxeMenus jar, and restart the server.

Migrating from upstream DeluxeMenus: swap the jar. Keep `config.yml` and `gui_menus/` as-is.

## Building

```bash
./gradlew shadowJar
```

Output lands in `build/libs/`. Requires JDK 25.

## Documentation

Menu configuration is identical to upstream, so the official docs apply:

- [DeluxeMenus Wiki][upstreamWiki]

Fork-specific additions: [fork documentation][forkDocs]

## Support

Support for **this fork**:

- [Issue Tracker][forkIssues]
- [Discord][forkDiscord]

For questions about original DeluxeMenus behaviour, upstream resources are still the best
reference — but please do not open fork bug reports there:

- [Upstream repository][upstream] · [Upstream Discord][upstreamDiscord] · [Spigot page][upstreamSpigot]

## Contributing

See the [Contributing file][contributing]. Pull requests that fix upstream bugs are welcome,
but consider sending those to [upstream][upstream] as well so everyone benefits.

## Credits

- Original plugin by [HelpChat][upstream] / ExtendedClip.
- Fork maintained by **JazzyPham / MadvionLabs**.
