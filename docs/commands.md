# Commands

All commands support both `/rankedsmp` and `/rsmp` as the main command.

## Player Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/rankinfo [player]` | `/ri`, `/myrank` | View your rank and active buffs |
| `/ranktop` | `/rt`, `/topranks` | View the top ranked players |
| `/rank [player]` | — | View rank and buffs for yourself or another player |
| `/ranks` | — | Open GUI showing top 20 ranked players with buffs |
| `/extrainventory` | `/inv`, `/ei`, `/einv` | Open your extra inventory (Ranks 1-10 only) |
| `/ranktransfer <player>` | `/transfer`, `/giverank` | Give your rank to another player (must be enabled) |
| `/linkdiscord` | `/discordlink` | Link your Minecraft account to Discord |
| `/unlinkdiscord` | `/discordunlink` | Unlink your Discord account |

## Admin Commands — Core

| Command | Permission | Description |
|---------|------------|-------------|
| `/rsmp start` | `rankedsmp.admin` | Start the game and assign random ranks |
| `/rsmp stop` | `rankedsmp.admin` | Stop the game and clear all ranks (requires confirmation) |
| `/rsmp reload` | `rankedsmp.admin` | Reload all config files, messages, and TAB integration |
| `/rsmp enable` | `rankedsmp.admin` | Enable the plugin at runtime (reapply all buffs) |
| `/rsmp disable` | `rankedsmp.admin` | Disable the plugin (reset all players to vanilla, clear tab prefixes) |
| `/rsmp manage` | `rankedsmp.gui` | Open rank management GUI |
| `/rsmp help` | — | Show help menu |

## Admin Commands — Rank Management

| Command | Permission | Description |
|---------|------------|-------------|
| `/rsmp set <player> <rank>` | `rankedsmp.set` | Set a player's rank (number, `unranked`, or `blacklisted`) |
| `/rsmp rank set <player> <rank>` | `rankedsmp.set` | Same as above (nested format) |
| `/rsmp rank remove <player>` | `rankedsmp.remove` | Remove a player's rank |
| `/rankrandom <rank> [-i] [-o]` | `rankedsmp.admin` | Assign rank to random player with roulette animation |
| `/rankblacklist <add\|remove\|list> [player]` | `rankedsmp.blacklist` | Manage the rank blacklist |
| `/rankhistory <player>` | `rankedsmp.history` | View a player's rank change history |
| `/rankhistorywipe <player\|all>` | `rankedsmp.historywipe` | Wipe rank history |

## Admin Commands — Items

| Command | Permission | Description |
|---------|------------|-------------|
| `/rsmp hammer` | `rankedsmp.admin` | Give yourself the Hierarchy Hammer |
| `/rsmp mace` | `rankedsmp.admin` | Alias for `/rsmp hammer` |
| `/rsmp give mace <player>` | `rankedsmp.admin` | Give Hierarchy Hammer to a player |
| `/rsmp give hammer <player>` | `rankedsmp.admin` | Same as above |
| `/rsmp spear` | `rankedsmp.admin` | Give yourself a Diamond Spear (1.21.11+) |
| `/rsmp give spear <player>` | `rankedsmp.admin` | Give Diamond Spear to a player (1.21.11+) |

## Admin Commands — Data & Discord

| Command | Permission | Description |
|---------|------------|-------------|
| `/rsmp migrate` | `rankedsmp.admin` | Import data from old RankedSMP plugin |
| `/rsmp migrate-confirm <token>` | `rankedsmp.admin` | Confirm migration with token |
| `/rsmp discord enable` | `rankedsmp.admin` | Enable Discord bot integration |
| `/rsmp discord disable` | `rankedsmp.admin` | Disable Discord bot integration |
| `/rsmp discord update` | `rankedsmp.admin` | Force update Discord leaderboard |
| `/rsmp discord status` | `rankedsmp.admin` | Check Discord integration status |
| `/rsmp info [player]` | `rankedsmp.admin` | View rank info (redirects to `/rankinfo`) |
| `/rsmp test` | `rankedsmp.admin` | Display all rank multiplier values in chat |
| `/rsmp setup-dismiss` | — | Dismiss first-time setup message |
| `/einvsee <player>` | `rankedsmp.extrainvsee` | View/edit player's extra inventory (supports offline) |
| `/rankexport` | `rankedsmp.export` | Export all ranks to JSON |
| `/rankimport <filename>` | `rankedsmp.import` | Import ranks from JSON |
| `/backup` | `rankedsmp.admin` | Create full database backup |
| `/restore <filename>` | `rankedsmp.admin` | Restore from backup file |
| `/rankfix` | `rankedsmp.admin` | Fix invalid ranks in the database |

## Admin Commands — TAB Style

| Command | Permission | Description |
|---------|------------|-------------|
| `/rsmp modern` | `rankedsmp.admin` | Apply modern gradient tab prefixes (requires TAB + Paper) |
| `/rsmp original` | `rankedsmp.admin` | Revert to original legacy color tab prefixes |
| `/rsmp og` | `rankedsmp.admin` | Alias for `/rsmp original` |

> **Note:** Both `/rsmp modern` and `/rsmp original` require confirmation (click prompt, 30s timeout). They overwrite all tab prefix config values.

## `/rsmp set` — Special Values

The `/rsmp set` command accepts three types of values:

| Value | Description |
|-------|-------------|
| `1-20` (number) | Assign that rank to the player |
| `unranked` | Remove the player's rank entirely |
| `blacklisted` | Blacklist the player (remove rank + block future rank assignment) |

Blacklisted players:
- Cannot receive ranks from any source (start, set, random, steal)
- Must be unblacklisted with `/rankblacklist remove` before they can receive ranks again
- Display with `[BLACKLISTED]` prefix in tab

## `/rsmp enable` / `/rsmp disable`

Temporarily enable or disable the plugin at runtime without restarting:

- **`/rsmp disable`**: Resets all online players to vanilla defaults (10 hearts, no effects), clears tab prefixes, but preserves all rank data in the database
- **`/rsmp enable`**: Reapplies all rank buffs to online players, restores tab prefixes

Useful for testing or temporarily disabling the plugin without losing data.

## `/rsmp test`

Displays a table of all rank multiplier values in chat:

```
=== RankedSMP Multiplier Test ===
Rank 1: 20.0 hearts | 2.00x potion | 3.00x XP
Rank 2: 19.5 hearts | 1.96x potion | 2.90x XP
...
Rank 20: 10.5 hearts | 1.05x potion | 1.10x XP
```

## `/rankrandom` Flags

| Flag | Description |
|------|-------------|
| `-i` or `--include-ranked` | Include ranked players (can only win better ranks) |
| `-o` or `--offline` | Include offline players in selection pool |

**Default behavior:** Only unranked online players are eligible.

**Examples:**
- `/rankrandom 5` — Give rank #5 to a random unranked online player
- `/rankrandom 1 -i` — Give rank #1 to any online player
- `/rankrandom 10 -i -o` — Give rank #10 to any player (online/offline, ranked/unranked)

**Animation:** 20 spins over 5-6 seconds with sound effects, color cycling, and visual feedback via Adventure API titles.

## Discord Bot Commands

| Command | Who | Description |
|---------|-----|-------------|
| `/help` | Everyone | Show all available commands |
| `/rank [player]` | Everyone | Check rank for yourself or another player |
| `/link <code>` | Everyone | Link Minecraft account with 6-digit code |
| `/unlink` | Everyone | Unlink your Minecraft account |
| `/rankset <rank> [player] [username]` | Admin | Set a player's rank |
| `/rankrandom <rank> [include_ranked] [include_offline]` | Admin | Randomly assign rank with roulette |
| `/setchannel <type>` | Admin | Set leaderboard or updates channel |
| `/rankexport` | Admin | Export ranks as JSON |
| `/rankimport` | Admin | Import ranks from JSON |
