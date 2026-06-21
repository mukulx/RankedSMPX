# Discord Integration

RankedSMPX includes a full-featured Discord bot for managing ranks, viewing leaderboards, and syncing player roles.

## Features

- 🤖 Discord bot with slash commands
- 📊 Auto-updating leaderboard embeds
- 🔗 Account linking system (Minecraft ↔ Discord)
- 👥 Role synchronization (rank-based Discord roles)
- 📢 Server update announcements
- 💬 Direct message notifications

## Setup

### Step 1: Create Discord Bot

1. Go to https://discord.com/developers/applications
2. Click "New Application"
3. Give it a name (e.g., "RankedSMP Bot")
4. Go to "Bot" tab
5. Click "Reset Token" and copy your bot token
6. Enable these intents:
   - ✅ Server Members Intent
   - ✅ Message Content Intent
7. Go to "OAuth2" → "URL Generator"
8. Select scopes:
   - ✅ `bot`
   - ✅ `applications.commands`
9. Select bot permissions:
   - ✅ Send Messages
   - ✅ Embed Links
   - ✅ Manage Roles
   - ✅ Read Message History
10. Copy the generated URL and open it in browser
11. Select your Discord server and authorize

### Step 2: Configure Plugin

Edit `discord.yml`:

```yaml
enabled: true

bot:
  token: "YOUR_BOT_TOKEN_HERE"  # From step 1
  guild-id: "YOUR_GUILD_ID_HERE"  # Right-click server → Copy Server ID
```

**To get Guild ID:**
1. Enable Developer Mode: Discord → Settings → Advanced → Developer Mode
2. Right-click your server icon
3. Click "Copy Server ID"

### Step 3: Start Plugin

```
/rsmp reload
```

Bot should come online in your Discord server!

## Discord Commands

### Player Commands

| Command | Description |
|---------|-------------|
| `/rank [player]` | View rank and stats |
| `/link <code>` | Link Minecraft account |
| `/unlink` | Unlink Minecraft account |
| `/help` | Show all commands |

### Admin Commands

| Command | Description |
|---------|-------------|
| `/rankset <player> <rank>` | Set player's rank |
| `/rankrandom` | Assign random ranks |
| `/setchannel <type>` | Set leaderboard/updates channel |
| `/rankexport` | Export ranks to JSON |
| `/rankimport <json>` | Import ranks from JSON |

## Features

### Auto-Updating Leaderboard

**Setup:**
```
/setchannel leaderboard
```

**Configuration:**
```yaml
leaderboard:
  enabled: true
  update-interval: 300  # seconds
  show-unlinked: true
  max-players: 20
  title: "🏆 RankedSMP X Leaderboard"
  color: "#00FFFF"
```

**Features:**
- Updates automatically every 5 minutes (configurable)
- Shows rank, player name, Discord mention
- Color-coded by rank tier
- Pagination for 20+ players

### Account Linking

**How it works:**
1. Player runs `/linkdiscord` in Minecraft
2. Receives 6-digit code
3. Runs `/link <code>` in Discord
4. Accounts are linked!

**Configuration:**
```yaml
linking:
  enabled: true
  required: false  # Kick unlinked players
  remind-on-join: true
  code-expiration: 10  # minutes
  allow-unlink: true
```

**Benefits:**
- Discord mentions in leaderboard
- Direct message notifications
- Role synchronization
- Admin commands work with Discord names

### Role Synchronization

**Automatic Discord roles based on rank!**

**Setup:**
```yaml
role-sync:
  enabled: true
  auto-create-roles: true  # Create roles automatically
  role-format: "Rank #{rank}"
  unranked-role: "Unranked"
  remove-old-roles: true  # Remove old rank roles
```

**Features:**
- Auto-creates roles if they don't exist
- Updates roles when ranks change
- Configurable role colors by tier
- Removes old rank roles on change

**Manual Role IDs:**
```yaml
role-sync:
  auto-create-roles: false
  rank-role-ids:
    1: "123456789012345678"
    2: "234567890123456789"
    # ... map ranks to existing role IDs
```

### Update Announcements

**Post game events to Discord channel!**

**Setup:**
```
/setchannel updates
```

**Configuration:**
```yaml
updates:
  enabled: true
  announce-game-start: true
  announce-game-end: true
  announce-rank-changes: false
  announce-player-join: false
  announce-player-leave: false
  announce-player-death: false
  mention-role-id: ""  # Optional @role mention
```

**Announces:**
- ✅ Season start/end
- ✅ Rank changes (gain, loss, transfer)
- ✅ Player join/leave (optional)
- ✅ Player deaths (optional)

### Direct Message Notifications

**Send DMs to players on rank changes!**

**Configuration:**
```yaml
notifications:
  enabled: true
  notify-on-gain: true
  notify-on-loss: true
  notify-top-3: true  # Special notification for top 3
  color: "#FFD700"
```

**Example DM:**
```
🎉 Congratulations!
You gained Rank #5!

Your new perks:
❤ Hearts: 18.0
⚗ Potion Boost: 1.81x
✨ XP Boost: 2.6x
```

## Minecraft Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/linkdiscord` | Get Discord linking code | `rankedsmp.linkdiscord` |
| `/unlinkdiscord` | Unlink Discord account | `rankedsmp.unlinkdiscord` |
| `/rsmp discord enable` | Enable Discord bot | `rankedsmp.admin` |
| `/rsmp discord disable` | Disable Discord bot | `rankedsmp.admin` |
| `/rsmp discord status` | View bot status | `rankedsmp.admin` |
| `/rsmp discord update` | Force leaderboard update | `rankedsmp.admin` |

## Admin Settings

```yaml
admin:
  admin-role-id: ""  # Discord role for admin commands
  admin-user-ids: []  # Discord user IDs with admin access
```

**Admin access methods:**
1. Discord server owner (always has access)
2. Users with `admin-role-id` role
3. Users in `admin-user-ids` list

## Troubleshooting

### Bot doesn't come online
1. Check bot token is correct
2. Verify token hasn't expired/been reset
3. Check guild-id is correct
4. Ensure bot was invited with correct permissions
5. Check console for errors

### Slash commands don't appear
1. Wait up to 1 hour for Discord to register commands
2. Try kicking and re-inviting the bot
3. Check bot has `applications.commands` scope
4. Run `/rsmp discord status` to verify registration

### Leaderboard doesn't update
1. Run `/setchannel leaderboard` again
2. Check `leaderboard.enabled: true`
3. Verify bot has permissions to post in channel
4. Check `update-interval` is at least 60 seconds
5. Force update: `/rsmp discord update`

### Role sync doesn't work
1. Check `role-sync.enabled: true`
2. Ensure bot has "Manage Roles" permission
3. Move bot's role above rank roles in server settings
4. Check role names match `role-format` if not auto-creating

### Players can't link accounts
1. Check `linking.enabled: true`
2. Verify code hasn't expired (default 10 minutes)
3. Check player has permission `rankedsmp.linkdiscord`
4. Try unlinking and relinking

## Advanced Configuration

### Custom Bot Activity

```yaml
bot:
  activity:
    type: "WATCHING"  # PLAYING, WATCHING, LISTENING, COMPETING
    message: "RankedSMP X | /rank"
```

### Clear Old Commands

```yaml
bot:
  clear-previous-commands: true  # Delete old slash commands
```

### Embed Customization

```yaml
embed-footer-name: "RankedSMPX"  # Footer text in all embeds
discord-invite: "https://discord.com/invite/QGsJYht2vy"  # Support server link
```

## Security Notes

- ⚠️ Never share your bot token publicly
- ⚠️ Regenerate token if accidentally exposed
- ⚠️ Use admin role/user IDs to restrict admin commands

## Support

- Discord: https://discord.com/invite/QGsJYht2vy
- Issues: https://github.com/mukulx/RankedSMPX/issues