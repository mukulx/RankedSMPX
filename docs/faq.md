# FAQ

## What server and Java versions do I need?

Use Paper 1.21.1+ or a compatible Paper fork, with Java 21+. Folia is supported. See [Installation](installation.md).

## Are there exactly 20 ranks?

No. The default table has 20 ranked slots. The highest positive row in `ranks.yml` sets the rank count; add rows to extend it. See [Rank settings](ranks-yml.md).

## How do I start or reset a season?

Run `/rsmp start` to assign available ranks. `/rsmp stop` ends the season and clears current rank assignments; it asks for confirmation.

## How do I change hearts, XP, potion duration, or inventory slots?

Edit the player's rank row in `ranks.yml`, then run `/rsmp reload`. For behavior that affects all ranks, see [Configuration](configuration.md).

## Can I disable rank stealing?

Yes. Set `season.rank-stealing.enabled: false` in `config.yml` and reload.

## How do I access extra inventory?

Use `/inv`. A rank needs a positive `slots` value in `ranks.yml`. Combat access, death drops, and anti-dupe behavior are in `config.yml`.

## Does tab formatting need TAB?

No. RankedSMPX has a vanilla scoreboard-team fallback. Install TAB for RGB and MiniMessage tab formatting. Rank prefixes and suffixes are in `ranks.yml`.

## Do I need DiscordSRV?

No. RankedSMPX includes an optional Discord bot. Configure `discord.yml`; enabling or disabling the bot requires a server restart.

## Can I turn off the title messages?

Yes. Set `titles: false` in `config.yml` and reload. Chat and action bar messages are not affected.

## Can inactive players lose their rank?

Yes, optionally. Turn on `season.inactivity` in `config.yml`. After the `after` time offline (for example `7d`), the rank is freed (`remove`) or given to a random online player without a rank (`reroll`). Time the server itself was down does not count. Add names or UUIDs to `exempt` to protect players. It is off by default.

## Where are settings stored?

In `plugins/RankedSMPX/`. The plugin stores server settings in `config.yml`, per-rank data in `ranks.yml`, item features in `items.yml`, and messages, Discord, and experimental settings in their own files. See [Configuration](configuration.md).

## How do I set rank permissions in LuckPerms?

Enable `luckperms.enabled` in `config.yml`. RankedSMPX provides `rankedsmp:rank` and `rankedsmp:ranked` contexts. Use LuckPerms context-aware assignments; see the [LuckPerms documentation](https://luckperms.net/wiki/Context).

## Does it work with Bedrock players (Geyser and Floodgate)?

Yes. Bedrock players hold and steal ranks like anyone else. In commands, type their full name with the Floodgate prefix (for example `.Steve`, or whatever `username-prefix` is set to), or use their UUID. When Floodgate is installed on the server, the console shows `GeyserMC (Floodgate) detected` at startup, Bedrock admins get the chest menu instead of the dialog menu, and a name typed without the prefix gets a "Did you mean .Steve?" hint. Set `bedrock.enabled: false` in `config.yml` to turn those extras off.

## A setting did not change after reload. What should I do?

Check the console for YAML errors, confirm you edited the correct file, and run `/rsmp reload`. Restart for database backend changes and Discord enablement. Some third-party plugins cache placeholders or tab values.

## Where can I get help?

- [Discord support](https://discord.gg/QGsJYht2vy)
- [GitHub issues](https://github.com/mukulx/RankedSMP/issues)
- [Installation and configuration guides](installation.md)
