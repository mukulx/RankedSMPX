# RankedSMPX

**A highly configurable Feature Rich Ranked SMP plugin. Inspired by RankedSMP.** Players hold numbered rank slots and gain configurable perks as they climb.

[Download the plugin](https://modrinth.com/plugin/rankedsmpx) · [Discord support](https://discord.gg/QGsJYht2vy) · [Report an issue](https://github.com/mukulx/RankedSMP/issues)

## Start here

- [Install the plugin](installation.md)
- [Run your first season](commands.md#start-a-season)
- [Set rank perks](ranks-yml.md)
- [Find a setting](configuration.md)

## How it plays

An admin starts a season. Online players are assigned ranks, and remaining rank slots can be assigned to players as they join. With rank stealing enabled, PvP kills can change who holds a slot. Per-rank hearts, XP, potion duration, inventory slots, and tab labels are editable in `ranks.yml`.

The shipped rank table has 20 ranked slots. Add rows to create more; the highest positive row sets the total.

## Platform and integrations

| | |
|---|---|
| Server | Paper 1.21.1+ and Folia; Java 21+ |
| Current source version | 3.0.0 |
| Optional plugins | PlaceholderAPI, TAB, LuckPerms, Geyser + Floodgate |
| Storage | SQLite by default; MySQL optional |
| Built-in integration | Optional Discord bot |

See [all features](features.md), [commands](commands.md), or [configuration files](configuration.md).

## Pages

- Start here: [Installation](installation.md), [Features](features.md)
- Gameplay: [Commands](commands.md), [Rank perks](rank-buffs.md), [Hierarchy Hammer](hierarchy-hammer.md)
- Configure: [Configuration map](configuration.md), [Rank settings](ranks-yml.md), [Messages](messages.md), [Permissions](permissions.md)
- Integrations: [TAB](tab-integration.md), [PlaceholderAPI](placeholders.md), [Discord bot](discord.md)
- Developers: [API](api.md)
- Help: [FAQ](faq.md), [Support](support.md)
