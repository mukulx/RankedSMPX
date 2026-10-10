# Discord integration

RankedSMPX includes an optional Discord bot for account linking, leaderboards, role sync, announcements, and rank notifications. Configure it in `plugins/RankedSMPX/discord.yml`.

## Set up the bot

1. Create an application at [Discord Developer Portal](https://discord.com/developers/applications), then add a bot.
2. Copy its token. Keep the token private; regenerate it if it is exposed.
3. Invite the bot to your server with the `bot` and `applications.commands` OAuth2 scopes. Grant the permissions it needs for the features you enable, such as viewing channels, sending messages, embedding links, and managing roles.
4. Enable the **Server Members Intent** in the bot settings for member and role features.
5. Copy your server ID with Discord Developer Mode enabled.
6. Set `bot.token`, `bot.guild-id`, and `enabled: true` in `discord.yml`.
7. Restart the Minecraft server. Discord libraries load before the plugin starts, so `/rsmp reload` alone cannot enable the bot.

```yaml
enabled: true
bot:
  token: "YOUR_BOT_TOKEN_HERE"
  guild-id: "YOUR_GUILD_ID_HERE"
```

## Features and settings

| Section | Purpose |
|---|---|
| `linking` | Link Minecraft and Discord accounts; optionally require links to play |
| `leaderboard` | Leaderboard channel, update interval and player limit |
| `notifications` | DM players about rank gains and losses; toggles for admin changes and deaths |
| `role-sync` | Assign roles by rank; optional role creation and custom role IDs |
| `updates` | Announce game events and rank changes in a channel |
| `commands` | Enable or disable Discord slash commands |
| `debug` | Extra Discord logging |

Discord admin commands need the Discord Administrator permission. Set channels with `/setchannel leaderboard` or `/setchannel updates`. Role sync needs the bot's **Manage Roles** permission and its role must be above the roles it manages.

## Commands

| Discord command | Description |
|---|---|
| `/help` | Show available bot commands |
| `/rank [player]` | Look up a rank |
| `/link <code>` | Complete account linking |
| `/unlink` | Unlink an account, if allowed |
| `/rankset <player> <rank>` | Set a rank (admin access required) |
| `/rankrandom` | Assign an available rank (admin access required) |
| `/forcelink <discord_user> <minecraft_username>` | Link an account as an admin |
| `/forceunlink [discord_user] [minecraft_username]` | Remove an account link as an admin |
| `/setchannel <type>` | Set a bot channel (admin access required) |
| `/rankexport` / `/rankimport` | Export or import ranks (admin access required) |

In Minecraft, players use `/linkdiscord` to receive a code and `/unlinkdiscord` to disconnect their account. Admins can check the connection with `/rsmp discord status` and refresh the leaderboard with `/rsmp discord update`.

## Troubleshooting

- **Bot stays offline:** check `enabled`, token, guild ID, console errors, and restart the server.
- **Commands are missing:** ensure the bot was invited with `applications.commands`; check `commands` settings and `/rsmp discord status`.
- **Leaderboard does not post:** set its channel with `/setchannel leaderboard`, enable it, and check channel permissions.
- **Roles do not update:** enable `role-sync`, grant Manage Roles, and move the bot role above the target roles.
- **A player cannot link:** check `linking.enabled`, code expiry, and whether the account is already linked.

Never post `discord.yml` with a real bot token in an issue or public repository.
