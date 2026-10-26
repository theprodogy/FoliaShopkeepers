<p align="center">
  <img src="https://github.com/Shopkeepers/Shopkeepers-Wiki/wiki/images/logos/shopkeepers_logo_small_with_text.png?raw=true" alt="Shopkeepers logo"/>
</p>

# FoliaShopkeepers

A fork of [Shopkeepers](https://github.com/Shopkeepers/Shopkeepers) for **Paper and Folia**: villager shopkeepers that sell exactly what you want, built on Paper's plugin lifecycle API and Folia's region schedulers, with configurable shop view distance.

[![Latest release](https://img.shields.io/github/v/release/theprodogy/FoliaShopkeepers)](https://github.com/theprodogy/FoliaShopkeepers/releases/latest)
[![Build](https://github.com/theprodogy/FoliaShopkeepers/actions/workflows/release.yml/badge.svg?branch=master)](https://github.com/theprodogy/FoliaShopkeepers/actions/workflows/release.yml)
[![License: GPL v3](https://img.shields.io/github/license/theprodogy/FoliaShopkeepers)](LICENSE)
![Paper](https://img.shields.io/badge/Paper-1.21.11%20%7C%2026.1.2%20%7C%2026.2%20%7C%2026.3-blue)
![Folia](https://img.shields.io/badge/Folia-1.21.11-green)

## Contents

- [Features](#features)
- [Supported versions](#supported-versions)
- [Installation](#installation)
- [Usage](#usage)
- [Differences from upstream Shopkeepers](#differences-from-upstream-shopkeepers)
- [Building from source](#building-from-source)
- [Developer API](#developer-api)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Admin shops** with infinite stock, and **player shops** that sell, buy, trade or sell books from a linked container.
- **Any mob, sign or Citizens NPC** as the shopkeeper, with editable names, equipment and appearance.
- **Folia support** on 1.21.11: shop ticking, spawning, AI and teleports run on the region that owns them.
- **Shop view distance**: `/shopkeepersadmin viewdistance` sets how far players can see shop entities, from a few blocks up to the server's view distance.
- **Paper-native**: `paper-plugin.yml`, Brigadier commands with full tab completion, region-aware schedulers.
- **Trade logs, trade notifications, shop expiration and inactivity cleanup**, as in upstream Shopkeepers.

## Supported versions

One jar runs on every version below.

| Server | Versions |
| --- | --- |
| Paper | 1.21.11, 26.1.2, 26.2, 26.3 |
| Folia | 1.21.11 |
| Spigot | Not supported |

## Installation

1. Download `Shopkeepers-<version>.jar` from the [latest release](https://github.com/theprodogy/FoliaShopkeepers/releases/latest).
2. Stop the server and **back up `plugins/Shopkeepers/`** if you are upgrading.
3. Remove any other Shopkeepers jar from `plugins/`.
4. Put the new jar into `plugins/` and start the server.

Existing upstream Shopkeepers data (`config.yml`, `data/save.yml`, language files) loads as-is. Do not move a world back to an older Minecraft version: Data saved on a newer version cannot be loaded on an older one.

## Usage

### Creating a shop

1. Get a shop creation item with `/shopkeeper give`.
2. Hold it without aiming at a block: Left/right-click to pick the shop type, sneak + left/right-click to pick the shopkeeper's look (mob, sign, ...).
3. For player shops, right-click a container (e.g. a chest) to link it.
4. Right-click a block to place the shopkeeper, then right-click the shopkeeper to set up its trades.

The full setup guide is in the [Shopkeepers wiki](https://github.com/Shopkeepers/Shopkeepers-Wiki/wiki).

### Common commands

| Command | Description | Permission |
| --- | --- | --- |
| `/shopkeeper help` | Lists all commands | `shopkeeper.help` |
| `/shopkeeper give` | Gives a shop creation item | `shopkeeper.give` |
| `/shopkeeper list [player\|admin]` | Lists shops | `shopkeeper.list.*` |
| `/shopkeeper remote [shop]` | Opens a shop remotely | `shopkeeper.remote` |
| `/shopkeeper teleport <shop>` | Teleports to a shop | `shopkeeper.teleport` |
| `/shopkeeper history` | Shows the trading history | `shopkeeper.history.*` |
| `/shopkeeper reload` | Reloads the config and language file | `shopkeeper.reload` |
| `/shopkeepersadmin viewdistance [<blocks>\|ALWAYS_VISIBLE\|DEFAULT]` | Shows or sets the shop view distance | `shopkeeper.admin.commands` |

`/shopkeepers` is an alias for `/shopkeeper`, and `/skadmin` for `/shopkeepersadmin`. All commands tab complete.

### Shop view distance

```text
/skadmin viewdistance 128             # Shop entities visible up to 128 blocks away
/skadmin viewdistance ALWAYS_VISIBLE  # As far as the server's view distance allows
/skadmin viewdistance DEFAULT         # Use the entity-tracking-range from spigot.yml
/skadmin viewdistance                 # Show the current value
```

The value is saved as `shopkeeper-view-distance` in `config.yml`:

| Value | Meaning |
| --- | --- |
| `0` | Server's entity tracking range (default) |
| `-1` | `ALWAYS_VISIBLE` |
| `1` to `512` | Distance in blocks |

The server's `view-distance` is always the upper limit, because players cannot see entities in chunks they have not loaded. This applies to mob and NPC shops; sign shops are blocks and always visible.

## Differences from upstream Shopkeepers

- Paper and Folia only. Spigot, and Minecraft versions before 1.21.11, are not supported.
- Loaded via `paper-plugin.yml`; commands are registered through Paper's lifecycle API.
- Help and usage messages always show `/shopkeeper`, also when `/shopkeepers` was typed.
- New: `/shopkeepersadmin viewdistance` and the `shopkeeper-view-distance` setting.
- On Folia: Other plugins cannot cancel the teleports of shop entities, and the Citizens, Towny and WorldGuard integrations are only active if those plugins support Folia.

Everything else, including permissions and config keys, matches upstream.

## Building from source

Requires Git and JDK 25. Gradle and the Paper dev bundles are downloaded automatically.

```bash
git clone https://github.com/theprodogy/FoliaShopkeepers.git
cd FoliaShopkeepers
./gradlew build
```

The plugin jar is written to `modules/dist/build/libs/`. The build runs all tests, javac lint and Eclipse null analysis, and must finish without warnings.

| Module | Contents |
| --- | --- |
| `modules/api` | Public API for add-on plugins |
| `modules/main` | Core plugin code |
| `modules/paper` | Paper-specific features |
| `modules/versions/<version>` | Minecraft version-specific code |
| `modules/dist` | Bundles the final plugin jar |
| `modules/test` | Tests |

Releases are built by GitHub Actions: Pushing a tag such as `v2.28.3` publishes a release with the jar attached.

## Developer API

The public API is unchanged from upstream, so add-ons built against `ShopkeepersAPI` keep working. Add it as a `provided` dependency from the upstream repository:

```xml
<repository>
  <id>shopkeepers-repo</id>
  <url>https://raw.githubusercontent.com/Shopkeepers/Repository/main/releases/</url>
</repository>

<dependency>
  <groupId>com.nisovin.shopkeepers</groupId>
  <artifactId>ShopkeepersAPI</artifactId>
  <version>2.28.1</version>
  <scope>provided</scope>
</dependency>
```

Start with [`ShopkeepersAPI`](modules/api/src/main/java/com/nisovin/shopkeepers/api/ShopkeepersAPI.java), e.g. `ShopkeepersAPI.getShopkeeperRegistry()`. To only check whether an entity is a shopkeeper, use `entity.hasMetadata("shopkeeper")`.

## Contributing

1. Fork the repository and create a branch: `git checkout -b my-change`.
2. Follow the code style in [AGENTS.md](AGENTS.md): Tabs, CRLF line endings, 100-column soft limit.
3. Run `./gradlew build` and make sure it passes without warnings.
4. Commit with a short, lowercase message prefixed with the area, e.g. `commands: fix viewdistance tab completion`.
5. Open a pull request.

Bugs and ideas go to the [issue tracker](https://github.com/theprodogy/FoliaShopkeepers/issues). Version history is in [CHANGELOG.md](CHANGELOG.md).

## License

[GPL-3.0](LICENSE)

## Acknowledgments

Shopkeepers was created by nisovin and is maintained by blablubbabc and its [contributors](https://github.com/Shopkeepers/Shopkeepers/graphs/contributors). This fork builds on their work: [upstream repository](https://github.com/Shopkeepers/Shopkeepers), [wiki](https://github.com/Shopkeepers/Shopkeepers-Wiki/wiki).
