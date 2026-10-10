# Installation

## Requirements

| Requirement | Supported |
|---|---|
| Minecraft server | Paper 1.21.1+ or a compatible Paper fork; Folia is supported |
| Java | 21+ |

RankedSMPX uses Paper's plugin loader and downloads its runtime libraries from Maven Central when the server starts. Allow the server to reach Maven Central on first startup.

## Install

1. Stop the server.
2. Download the latest jar from [Modrinth](https://modrinth.com/plugin/rankedsmpx).
3. Put it in the server's `plugins/` folder.
4. Start the server and check the console for `RankedSMPX` startup errors.
5. Run `/rsmp start` when players are ready to begin a season.

## Files created

The plugin stores its settings in `plugins/RankedSMPX/`:

| File | Purpose |
|---|---|
| `config.yml` | Season behavior, rank display, health, inventory, database, integrations, and technical settings |
| `ranks.yml` | One row per rank: perks, slots, prefixes, suffixes |
| `items.yml` | Hierarchy Hammer, spear, and Dragon Egg Locator |
| `messages.yml` | In-game message text |
| `discord.yml` | Optional Discord bot and server integration |
| `custom.yml` | Experimental rank-drop item feature |

Back up the plugin folder before upgrading. The plugin backs up and migrates older configuration layouts automatically; keep `ranks.yml` if it already contains your custom rank values.

## Optional integrations

| Plugin | What it adds |
|---|---|
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.9089/) | RankedSMPX placeholders in supported plugins |
| [TAB](https://www.spigotmc.org/resources/tab.57806/) | RGB and MiniMessage tab prefixes; vanilla tab formatting works without TAB |
| [LuckPerms](https://luckperms.net/) | Optional live rank contexts |

The Discord bot is built in and does not require a separate Discord plugin.

## Moving from the original RankedSMP plugin

1. Back up the server and stop it.
2. Disable or remove the old plugin.
3. Install RankedSMPX and start the server.
4. Run `/rsmp migrate` and follow the confirmation prompt.

The migration command imports supported data into RankedSMPX. Keep your backup until you have checked the imported ranks and inventories.
