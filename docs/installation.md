# Installation

## Requirements

| Requirement | Version |
|-------------|---------|
| Minecraft | 1.21.1+ |
| Java | 21+ |
| Server Software | Paper (recommended), Purpur, Pufferfish, Folia, Leaf, Spigot, or Bukkit |

> **First startup:** The plugin downloads required dependencies (JDA, Gson, MySQL connector, SQLite, HikariCP) from Maven Central automatically. This only happens once and might take some time to download.

## Install from Modrinth

1. Download the latest `.jar` from [Modrinth](https://modrinth.com/plugin/rankedsmpx)
2. Drop the `.jar` into your server's `plugins/` folder
3. Start/restart your server
4. The plugin will create default config files on first launch

## Post-Installation

On first launch, the plugin creates these files in `plugins/RankedSMPX/`:

| File | Description |
|------|-------------|
| `config.yml` | Main configuration |
| `messages.yml` | Customizable messages |
| `discord.yml` | Discord bot settings |

## Optional Dependencies

| Plugin | Purpose |
|--------|---------|
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.9089/) | Placeholders in chat, scoreboards, holograms |
| [TAB](https://www.spigotmc.org/resources/tab.57806/) | Advanced tab list formatting (RGB colors, gradients) |

> If TAB is not installed, the plugin automatically uses vanilla Minecraft scoreboard teams for tab list integration.

## Migrating from Old RankedSMP

If you used the old RankedSMP plugin (by Lusik21556):

1. Stop your server
2. Remove/disable the old RankedSMP plugin
3. Install RankedSMPX
4. Start your server
5. Run `/rsmp migrate`
6. Click the confirmation message

Changes apply instantly.
