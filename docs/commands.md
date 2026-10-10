# Commands

Use `/rsmp` or `/rankedsmp` for the main command. The main command also supports `/rsmp help`.

## Start a season

1. Add rank rows and choose perks in [`ranks.yml`](ranks-yml.md).
2. Run `/rsmp start` to assign open ranks to online players.
3. Players who join later can receive remaining ranks when `auto-assign-ranks.enabled` is on.

## Player commands

| Command | Aliases | Description |
|---|---|---|
| `/rankinfo [player]` | `/ri`, `/myrank` | Show rank and active perks |
| `/ranktop` | `/rt`, `/topranks` | List ranked players |
| `/rank [player]` | — | Show a player's rank and perks (online target) |
| `/ranks` | — | Open the ranked-player menu |
| `/inv` | `/extrainventory`, `/extrainv`, `/ei`, `/einv` | Open your rank inventory if you have slots |
| `/ranktransfer <player>` | `/transfer`, `/giverank` | Transfer your rank if transfers are enabled |
| `/rankhistory <player>` | — | View rank change history |
| `/linkdiscord` | `/discordlink` | Get a Discord linking code |
| `/unlinkdiscord` | `/discordunlink` | Unlink Discord |

## Season and administration

| Command | Description |
|---|---|
| `/rsmp start` | Start a season and assign available ranks |
| `/rsmp stop` | End the season and clear rank assignments; confirmation required |
| `/rsmp reload` | Reload plugin configuration |
| `/rsmp enable` / `/rsmp disable` | Enable or pause plugin gameplay without removing stored ranks |
| `/rsmp menu` | Open the admin menu (also `gui`, `manage`, `admin`, `panel`) |
| `/rsmp set <player> <rank\|unranked\|blacklisted>` | Set a player's rank |
| `/rsmp rank set <player> <rank>` | Set a player's rank |
| `/rsmp rank remove <player>` | Remove a player's rank |
| `/rsmp fix [player]` | Repair rank or health state |
| `/rsmp test` | Print rank perk values |
| `/rsmp modern` / `/rsmp original` | Apply modern or classic tab prefix presets (`/rsmp og` aliases original) |
| `/rsmp setup-dismiss` | Dismiss the first-time setup prompt |

## Rank and data tools

| Command | Description |
|---|---|
| `/rankrandom <rank> [-i] [-o]` | Assign a vacant rank; `-i` includes ranked players, `-o` includes offline players (default: unranked online players) |
| `/rankblacklist add <player>` | Prevent a player from receiving a rank |
| `/rankblacklist remove <player>` | Remove a player from the blacklist |
| `/rankblacklist list` | List blacklisted players |
| `/rankhistorywipe <player\|all>` | Delete rank history |
| `/rankexport` | Export rank data to a timestamped JSON file |
| `/rankimport <filename>` | Import rank data |
| `/rankfix [player]` | Repair invalid stored rank data |
| `/einvsee <player>` | View or edit a player's extra inventory |
| `/rsmp migrate` | Import data from the original RankedSMP plugin; confirmation required |

## Item commands

| Command | Description |
|---|---|
| `/rsmp hammer` or `/rsmp mace` | Give yourself the Hierarchy Hammer; `hammer settings` opens its settings dialog |
| `/rsmp give hammer <player>` | Give a player the Hierarchy Hammer |
| `/rsmp spear` | Give yourself a Diamond Spear (Minecraft 1.21.11+) |
| `/rsmp give spear <player>` | Give a player a Diamond Spear (Minecraft 1.21.11+) |

## Discord status

Use `/rsmp discord status` to check the connection and `/rsmp discord update` to refresh the leaderboard. Configure the bot in `discord.yml` and restart the server after changing `enabled`. The Discord bot also provides `/forcelink` and `/forceunlink` for account administration.

Most administrative commands require `rankedsmp.admin`; commands with narrower permissions are listed in [Permissions](permissions.md). Rank stealing and rank transfers are controlled separately in configuration.
