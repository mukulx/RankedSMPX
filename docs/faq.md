# Frequently Asked Questions

Common questions and answers about RankedSMPX.

## General Questions

### What Minecraft versions are supported?

- **Minimum:** 1.21.1 (Paper/Spigot/Bukkit)
- **Recommended:** Latest Paper 1.21.11+
- **Dragon Egg Locator:** Requires 1.21.6+
- **Diamond Spear:** Requires 1.21.11+

### Is Paper required?

No, but highly recommended. The plugin works on:
- ✅ Paper (best performance, all features)
- ✅ Spigot (compatible, most features)
- ✅ Bukkit (compatible, basic features)

Some features require Paper:
- MiniMessage color support
- Better event handling
- Native Adventure API

### Can I use it with plugins like EssentialsX?

Yes! RankedSMPX is compatible with most plugins:
- ✅ EssentialsX
- ✅ TAB
- ✅ PlaceholderAPI
- ✅ Vault
- ✅ LuckPerms
- ✅ DiscordSRV (separate from built-in Discord)

### Does it work in Bungeecord/Velocity networks?

The plugin works on individual servers but doesn't sync across Bungeecord/Velocity:
- ⚠️ Each server has separate ranks
- ⚠️ Database must be shared manually (MySQL)
- ⚠️ Discord bot can only connect to one server

## Ranks & Gameplay

### How are ranks assigned?

1. Admin runs `/rsmp start`
2. Random ranks assigned to online players
3. Players can steal ranks through PvP
4. Admins can manually set ranks with `/rsmp set`

### Can ranks be stolen from anyone?

By default yes, but you can configure:
```yaml
rank-stealing:
  enabled: true
  max-rank-difference: 0  # 0 = no limit, 5 = only steal from ±5 ranks
```

### What happens when someone dies?

If killed by another player:
- Victim loses their rank
- Killer gains victim's rank
- Both players swap ranks
- Rank change is announced

If killed by environment:
- No rank change
- Keep your rank

### How do I reset ranks?

```
/rsmp stop
```

This resets all ranks and ends the season. Requires confirmation.

## Features & Mechanics

### Do rank buffs work automatically?

Yes! All buffs apply automatically when you have a rank:
- ❤️ Extra hearts (max health)
- ⚗️ Longer potion effects
- ✨ More XP from all sources
- 🎒 Extra inventory slots (ranks 1-10)

### How do I access extra inventory?

```
/inv
```

Only works for ranks 1-10. Opens a separate inventory for storage.

### What is the Hierarchy Hammer?

A special mace weapon with:
- Right-click: Dash forward
- 4-hit combo: Empowered attack
- Mace smash: Bonus damage when falling

See [Hierarchy Hammer](hierarchy-hammer.md) guide.

### Can I get the hammer?

Admin command:
```
/rsmp give hammer <player>
```

Or enable auto-give:
```yaml
hierarchy-hammer:
  auto-give-on-start: true
```

### What's the Dragon Egg Locator?

Shows nearby players on your action bar when holding the dragon egg. Helps create objectives for PvP.

**Requirements:** Minecraft 1.21.6+

## Configuration

### How do I change rank perks?

Edit `config.yml`:
```yaml
health:
  custom-hearts:
    1: 20.0  # Rank 1 gets 20 hearts
    
potions:
  custom-multipliers:
    1: 2.0  # Rank 1 gets 2x potion duration
    
xp:
  custom-multipliers:
    1: 3.0  # Rank 1 gets 3x XP
```

Then reload: `/rsmp reload`

### How do I change colors?

**Tab prefixes:**
```yaml
tab-integration:
  rank-prefixes:
    1: '&6[#1] '  # Gold
```

**Messages:**
Edit `messages.yml` with color codes or MiniMessage format.

### Can I use MySQL instead of SQLite?

Yes!
```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: rankedsmp
    username: root
    password: your_password
```

### How do I export/import ranks?

**Export:**
```
/rankexport
```
Creates `ranks_export.json` in plugin folder.

**Import:**
```
/rankimport
```
Reads from `ranks_import.json` in plugin folder.

## Discord Integration

### Do I need a Discord bot?

No, Discord integration is optional. The plugin works fine without it.

### How do I set up Discord bot?

See [Discord Integration](discord.md) guide.

### Why isn't my bot coming online?

Common issues:
1. Wrong bot token
2. Wrong guild ID
3. Missing bot permissions
4. Bot not invited to server

### Can players use Discord to check ranks?

Yes! After linking accounts:
```
/rank [player]
```

Shows rank and stats in Discord.

## Permissions & Commands

### What permissions do players need?

Default players get:
- `rankedsmp.info` - View ranks
- `rankedsmp.top` - View leaderboard
- `rankedsmp.inv` - Access extra inventory

See [Permissions](permissions.md) for full list.

### How do I give admin access?

```
/lp user <player> permission set rankedsmp.admin true
```

Or with any permission plugin.

### Can I disable specific commands?

Use permission plugin to deny:
```
/lp group default permission set rankedsmp.transfer false
```

This prevents players from using `/ranktransfer`.

## TAB Integration

### Do I need TAB plugin?

No, works with vanilla Minecraft tab too!

**With TAB plugin:**
- RGB colors and gradients
- Advanced formatting
- Better performance

**Without TAB plugin:**
- Legacy colors only
- Basic prefixes
- Scoreboard teams

### Why don't my gradients work?

Gradients require TAB plugin. Use `/rsmp modern` to enable gradients.

If still not working:
1. Check TAB plugin is installed
2. Verify TAB version is 4.0+
3. Use `/rsmp original` for legacy colors

### How do I change prefix format?

```yaml
tab-integration:
  rank-prefixes:
    1: '<gradient:#FFD700:#FFA500>[#1]</gradient> '
```

Or use `/rsmp modern` / `/rsmp original` for presets.

## PlaceholderAPI

### How do I use placeholders?

1. Install PlaceholderAPI
2. Use `%rankedsmp_rank%` in any plugin that supports PAPI

See [Placeholders](placeholders.md) for full list.

### Why aren't placeholders updating?

```
/papi reload
```

Or wait a few seconds for automatic refresh.

## Performance & Compatibility

### Does it cause lag?

No significant lag. The plugin is optimized:
- Async database queries
- Efficient event handling
- Minimal packet sending
- Connection pooling

### Can I use it on a shared host?

Yes, but check:
- Java 21 support
- Plugin installation allowed
- MySQL access (if needed)
- Discord bot allowed (optional)

### What about 1.20.x or older?

Not supported. Plugin requires:
- Java 21
- Paper API 1.21.1+
- Modern Adventure API

## Troubleshooting

### Ranks don't show in tab

1. Check `tab-integration.enabled: true`
2. Run `/rsmp reload`
3. Players rejoin server
4. If using TAB plugin, run `/tab reload`

### Extra inventory items disappeared

Check:
```yaml
inventory:
  drop-on-death: true
```

If `true`, items drop on death. Set to `false` to keep items.

### Potion effects not extending

Check:
```yaml
potions:
  extend-custom-effects: true
  ignored-effects:
    - POISON  # Negative effects
```

Make sure beneficial effects aren't in `ignored-effects`.

### Commands don't work

1. Check plugin is enabled: `/plugins`
2. Verify permissions
3. Check console for errors
4. Try `/rsmp reload`

### "Plugin disabled" message

The plugin can be disabled while keeping server running:
```
/rsmp disable
```

Enable it again:
```
/rsmp enable
```

## Getting Help

### Where can I get support?

- 💬 Discord: https://discord.com/invite/QGsJYht2vy
- 📝 GitHub Issues: https://github.com/mukulx/RankedSMPX/issues
- 📦 Modrinth: https://modrinth.com/project/rankedsmpx

### How do I report a bug?

1. Go to GitHub Issues
2. Click "New Issue"
3. Use bug report template
4. Include:
   - Server version
   - Plugin version
   - Steps to reproduce
   - Error logs (if any)

### Can I request features?

Yes! Create a GitHub issue with:
- Feature description
- Use case
- Why it's useful
- How it should work

### Is there a Discord server?

Yes! Join for:
- Support
- Feature discussions
- Update announcements
- Community sharing

https://discord.com/invite/QGsJYht2vy
