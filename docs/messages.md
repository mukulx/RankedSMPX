# Messages Customization

Complete guide to customizing plugin messages via `messages.yml`.

## Message Format Support

RankedSMPX supports **both** legacy color codes and modern MiniMessage format!

### Legacy Color Codes
```yaml
message: "&aHello &b&lWorld"  # Green "Hello", bold aqua "World"
```

**Color Codes:**
- `&0` - Black
- `&1` - Dark Blue
- `&2` - Dark Green
- `&3` - Dark Aqua
- `&4` - Dark Red
- `&5` - Dark Purple
- `&6` - Gold
- `&7` - Gray
- `&8` - Dark Gray
- `&9` - Blue
- `&a` - Green
- `&b` - Aqua
- `&c` - Red
- `&d` - Light Purple
- `&e` - Yellow
- `&f` - White

**Formatting Codes:**
- `&l` - Bold
- `&m` - Strikethrough
- `&n` - Underline
- `&o` - Italic
- `&r` - Reset

**Hex Colors:**
```yaml
message: "&#FF5555Hello"  # RGB color
```

### MiniMessage Format (Modern)
```yaml
message: "<green>Hello <aqua><bold>World</bold></aqua>"
```

**Colors:**
```yaml
<red>text</red>
<green>text</green>
<blue>text</blue>
<yellow>text</yellow>
<#FF5555>text</#FF5555>  # Hex color
```

**Formatting:**
```yaml
<bold>text</bold>
<italic>text</italic>
<underlined>text</underlined>
<strikethrough>text</strikethrough>
```

**Gradients:**
```yaml
<gradient:#FF0000:#00FF00>Rainbow Text</gradient>
<gradient:#FFD700:#FFA500>Gold to Orange</gradient>
```

## Small Caps Support

When `small-caps-messages: true` in `config.yml`, all text is converted to small caps:

```yaml
message: "Hello World"  # Displays as: ʜᴇʟʟᴏ ᴡᴏʀʟᴅ
```

**Small caps work with BOTH formats:**
- Legacy: `&aHello &bWorld` → ᴀʜᴇʟʟᴏ ᴡᴏʀʟᴅ (with colors)
- MiniMessage: `<gradient:#FF0000:#00FF00>Hello</gradient>` → ʜᴇʟʟᴏ (with gradient)

## Message Sections

### Settings
```yaml
settings:
  prefix: "&bRankedSMPX &8» &r"
  use-prefix: true
```

Control global prefix for all messages.

### General Messages
```yaml
general:
  no-permission: "{prefix}&cYou don't have permission!"
  player-not-found: "{prefix}&cPlayer '{player}' not found!"
  reload-success: "{prefix}&aConfiguration reloaded!"
```

### Command Messages
```yaml
commands:
  help:
    header: "&8&m━━━━━━━━━━━&r &6&l⚔ RankedSMP X &8&m━━━━━━━━━━━"
    admin: "&b/rsmp start &8» &dAssign ranks"
    # ... more commands
```

### Rank Messages
```yaml
rank:
  change:
    broadcast-gained: "&6&l⭐ {player} &egained rank &6&l#{rank}"
    broadcast-lost: "&c&l⚠ {player} &7dropped to rank &6&l#{rank}"
    title-gained: "&6&l✦ RANK #{rank} ✦"
    subtitle-gained: "&a&lYou gained a rank!"
```

### Hammer Messages
```yaml
hammer:
  received: "{prefix}&aYou received the Hierarchy Hammer!"
  miss-cooldown: "&6&lYou missed, &e&lnow on cooldown for &c&l{time}s"
  dropped: "&c&l✦ &cThe Hierarchy Hammer has been dropped!"
```

### GUI Messages
```yaml
gui:
  rank-management:
    title: "&0Rank Management"
    player-item:
      name: "&6#{rank} &e{player}"
      lore:
        - "&7Click to view options"
        - "&eLeft-click: &7Move rank"
```

## Placeholders

### Global Placeholders
- `{prefix}` - Plugin prefix from settings
- `{player}` - Player name
- `{rank}` - Rank number
- `{old_rank}` - Previous rank number
- `{total}` - Total number of ranks
- `{max}` - Maximum value (context-specific)
- `{count}` - Count (context-specific)
- `{time}` - Time duration
- `{reason}` - Reason for rank change

### Rank Info Placeholders
- `{hearts}` - Player hearts
- `{multiplier}` - XP/potion multiplier
- `{slots}` - Extra inventory slots
- `{distance}` - Distance in blocks

### PlaceholderAPI

You can also use PlaceholderAPI placeholders in messages:
```yaml
message: "&aHello %player_name%! Your rank is %rankedsmp_rank%"
```

See [Placeholders](placeholders.md) for full list.

## Customization Examples

### Modern Gradient Style
```yaml
rank:
  info:
    header: "<gradient:#FFD700:#FFA500>━━━━━━━━━ Rank Info ━━━━━━━━━</gradient>"
    your-rank: "<gradient:#FF0000:#00FF00>Your Rank: #{rank}</gradient>"
```

### Legacy Color Style
```yaml
rank:
  info:
    header: "&6&m━━━━━━━━━&r &e&lRank Info &6&m━━━━━━━━━"
    your-rank: "&eYour Rank: &6&l#{rank}"
```

### Minimal Style
```yaml
settings:
  use-prefix: false
general:
  no-permission: "&cNo permission."
  reload-success: "&aReloaded."
```

### Emoji Style
```yaml
rank:
  change:
    broadcast-gained: "🏆 {player} gained rank #{rank}! 🎉"
    broadcast-lost: "💀 {player} dropped to rank #{rank}"
```

## Reloading Messages

To reload messages without restarting:
```
/rsmp reload
```

This reloads `config.yml`, `discord.yml`, and `messages.yml`.

## Tips

1. **Test colors**: Use `/rsmp test <message>` to preview formatted messages
2. **Backup first**: Always backup `messages.yml` before major changes
3. **Use gradients wisely**: Gradients only work if players are on Paper/Velocity
4. **Small caps**: Works with any color format, but may not support all Unicode characters
5. **Keep it readable**: Don't overuse colors or formatting - readability is key

## Default Messages Location

Default messages are in `src/main/resources/messages.yml` in the plugin JAR.

To reset messages to defaults:
1. Delete `plugins/RankedSMPX/messages.yml`
2. Restart server or use `/rsmp reload`
