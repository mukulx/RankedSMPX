# Features

RankedSMPX turns an SMP into a season-based competition. Server owners can change perk values and the number of rank slots without editing the plugin.

## Ranked seasons

- Start a season with `/rsmp start`; online players are assigned available ranks.
- When enabled, new players can receive unclaimed rank slots as they join.
- PvP rank stealing, rank-difference limits, cooldowns, and death effects are configurable.
- Assign, remove, transfer, blacklist, and audit ranks with commands or the admin menu.
- Optional inactivity rule: a rank is taken, or rerolled to a random online player, after a set time offline (`season.inactivity` in `config.yml`).
- The shipped setup has 20 ranks. Add rows in `ranks.yml` to extend it.

## Rank perks

Per-rank values live in `ranks.yml`: maximum health, XP multiplier, potion duration multiplier, extra inventory slots, tab prefix, and suffix. Default values give higher ranks stronger perks. See [Rank buffs](rank-buffs.md) and [the rank table guide](ranks-yml.md).

## Items and combat

- **Hierarchy Hammer:** configurable mace with dash, combo, and falling-smash behavior.
- **Dragon Egg Locator:** waypoint-style player locator when holding the Dragon Egg (Minecraft 1.21.6+).
- **Diamond Spear:** configurable Lunge spear (Minecraft 1.21.11+).
- **Rank-drop items:** experimental option in `custom.yml`, disabled by default. A dropped rank item can be claimed or traded.

## Storage and integrations

- SQLite storage by default, with optional MySQL and connection pooling.
- Rank history, JSON import/export, and extra inventory persistence.
- Optional PlaceholderAPI expansion, TAB integration, and LuckPerms rank contexts.
- Built-in optional Discord bot for account links, leaderboards, rank roles, messages, and admin commands.
- Folia-compatible scheduling.

## Admin tools

Use `/rsmp menu` for the Paper dialog interface (with a GUI fallback on unsupported builds), `/rsmp test` to inspect rank values, `/rsmp fix` to repair player state, and `/rsmp reload` to reload configuration. See [all commands](commands.md).

## Requirements

- Paper 1.21.1+ or compatible Paper fork; Folia is supported.
- Java 21+.
- Optional integrations: PlaceholderAPI, TAB, LuckPerms.
