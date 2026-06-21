# TAB Integration

RankedSMPX integrates with player lists to display ranks as prefixes/suffixes. Works with **TAB plugin** or **vanilla Minecraft** tab.

## How It Works

- **TAB Plugin Installed**: Uses TAB's advanced features (RGB colors, gradients, animations)
- **No TAB Plugin**: Uses vanilla Minecraft scoreboard teams (legacy colors only)
- **No setup needed**: Automatically detects and configures!

## Basic Configuration

```yaml
# config.yml
tab-integration:
  enabled: true
  
  # Unranked player prefix
  unranked-prefix: '&e[&8UNRANKED&e] '
  unranked-suffix: ''
  
  # Blacklisted player prefix
  blacklisted-prefix: '&0[&8BLACKLISTED&0] '
  blacklisted-suffix: ''
  
  # Rank prefixes (1-20)
  rank-prefixes:
    1: '&e[#1] '
    2: '&e[#2] '
    # ... up to 20
  
  # Rank suffixes (optional)
  rank-suffixes:
    1: ''
    2: ''
    # ... up to 20
```

## Color Format Support

### With TAB Plugin (RGB + Gradients)

TAB plugin supports:
- Legacy color codes (`&a`, `&b`, `&c`)
- RGB hex colors (`&#FF5555`)
- MiniMessage format (`<gradient:#FF0000:#00FF00>`)

**Example gradients:**
```yaml
rank-prefixes:
  1: '<gradient:#FFD700:#FFA500>[#1]</gradient> '     # Gold to orange
  2: '<gradient:#C0C0C0:#A4D7E8>[#2]</gradient> '     # Silver
  3: '<#FF5555>[#3]</#FF5555> '                       # Solid red
```

### Without TAB Plugin (Legacy Only)

Vanilla tab only supports:
- Legacy color codes (`&a`, `&b`, `&c`)
- Hex colors with & format (`&#FF5555`)

**Example legacy:**
```yaml
rank-prefixes:
  1: '&6[#1] '   # Gold
  2: '&e[#2] '   # Yellow
  3: '&c[#3] '   # Red
```

## Style Presets

### Original Style (Legacy Colors)

```
/rsmp original
```

Sets all prefixes to simple yellow format:
```yaml
rank-prefixes:
  1: '&e[#1] '
  2: '&e[#2] '
  # ... all ranks yellow
```

### Modern Style (Gradients)

```
/rsmp modern
```

**Requires TAB plugin!**

Sets gradient prefixes by tier:
- **Ranks 1-5**: Gold gradient
- **Ranks 6-10**: Silver gradient  
- **Ranks 11-15**: Green gradient
- **Ranks 16-20**: Purple gradient

```yaml
rank-prefixes:
  1: '&#FFD700[&#FFAA00#1&#FFD700] '      # Gold
  6: '&#C0C0C0[&#A4D7E8#6&#C0C0C0] '      # Silver
  11: '&#32CD32[&#5FE47E#11&#32CD32] '    # Green
  16: '&#8A2BE2[&#C21FB7#16&#8A2BE2] '    # Purple
```

## PlaceholderAPI Support

You can use PlaceholderAPI placeholders in prefixes/suffixes!

**Example:**
```yaml
rank-prefixes:
  1: '&7[&bVIP&7] &6[#1] &c%rankedsmp_hearts%❤ '
```

This shows: `[VIP] [#1] 20.0❤ PlayerName`

See [Placeholders](placeholders.md) for full list.

## Custom Prefixes Per Rank

### Simple Numbers
```yaml
rank-prefixes:
  1: '&6[#1] '
  2: '&e[#2] '
  3: '&c[#3] '
```

### Tier Names
```yaml
rank-prefixes:
  1: '&6[&lGOLD&6] '
  2: '&6[&lGOLD&6] '
  3: '&6[&lGOLD&6] '
  4: '&7[&lSILVER&7] '
  5: '&7[&lSILVER&7] '
```

### Emojis
```yaml
rank-prefixes:
  1: '&6👑 [#1] '
  2: '&e⭐ [#2] '
  3: '&c🔥 [#3] '
```

## Vanilla Tab Behavior

When TAB plugin is not installed:
- Uses Minecraft scoreboard teams
- Only legacy color codes work
- Team names are auto-generated: `rsmp_rank_1`, `rsmp_rank_2`, etc.
- Teams are created/updated automatically

**Limitations:**
- No RGB colors
- No gradients
- No animations
- Sorting by team name (ranks appear in order)

## TAB Plugin Behavior

When TAB plugin is installed:
- Uses TAB's prefix/suffix system
- Full RGB and gradient support
- Animation support (if configured in TAB)
- Better sorting options

**TAB plugin download:**
- SpigotMC: https://www.spigotmc.org/resources/tab.57806/
- GitHub: https://github.com/NEZNAMY/TAB

## Troubleshooting

### Prefixes don't show
1. Check `tab-integration.enabled: true` in config
2. Verify game is active (`/rsmp start` or manually assign ranks)
3. Check player has a rank (`/rankinfo <player>`)
4. Reload config (`/rsmp reload`)

### RGB colors don't work
- RGB requires TAB plugin
- Verify TAB plugin is installed and enabled
- Use `/rsmp original` to switch to legacy colors
- Check TAB config allows RGB

### Prefixes show for unranked players
- This is intended behavior during active seasons
- Set `unranked-prefix: ''` to hide prefix
- Unranked players get "Unranked" prefix when game is active

### Prefixes duplicate with other plugins
- TAB plugin takes priority over other plugins
- Check TAB config for conflicting prefix sources
- Disable other prefix plugins or configure priority

### Changes don't apply
- Run `/rsmp reload` after config changes
- If using TAB plugin, also run `/tab reload`
- Players may need to rejoin for changes to appear

## Integration with TAB Plugin

### Disabling TAB's Built-in Prefixes

If TAB is managing its own prefixes, disable them:

```yaml
# TAB/config.yml
scoreboard-teams:
  enabled: false  # Disable TAB's team prefixes
```

### Using Both Systems

You can use TAB for other features while RankedSMPX manages rank prefixes:

```yaml
# TAB/config.yml
scoreboard-teams:
  enabled: true
  anti-override: false  # Allow RankedSMPX to override
```

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/rsmp original` | `rankedsmp.admin` | Switch to legacy color prefixes |
| `/rsmp modern` | `rankedsmp.admin` | Switch to gradient prefixes (TAB only) |
| `/rsmp reload` | `rankedsmp.reload` | Reload prefix configuration |

## Examples

### Minimal Setup
```yaml
tab-integration:
  enabled: true
  unranked-prefix: ''
  rank-prefixes:
    1: '[1] '
    2: '[2] '
    # ... simple numbers
```

### Full RGB (TAB Plugin)
```yaml
rank-prefixes:
  1: '<gradient:#FFD700:#FF8C00>#1</gradient> '
  2: '<gradient:#FFD700:#FFA500>#2</gradient> '
```

### Legacy with Hex (No TAB)
```yaml
rank-prefixes:
  1: '&#FFD700[#1] '
  2: '&#FFA500[#2] '
```

### With Hearts
```yaml
rank-prefixes:
  1: '&c%rankedsmp_hearts%❤ &6[#1] '
```
