# PlaceholderAPI Placeholders

RankedSMPX integrates with PlaceholderAPI to provide placeholders for use in other plugins.

## Installation

1. Install PlaceholderAPI: https://www.spigotmc.org/resources/placeholderapi.6245/
2. Install RankedSMPX
3. Placeholders automatically register on server start

## Available Placeholders

### Rank Information

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%rankedsmp_rank%` | Player's rank number or "Unranked" | `1`, `5`, `Unranked` |
| `%rankedsmp_rank_number%` | Player's rank as number (0 if unranked) | `1`, `5`, `0` |

### Rank Perks

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%rankedsmp_hearts%` | Player's max hearts | `20.0`, `15.5`, `10.0` |
| `%rankedsmp_potion_multiplier%` | Potion duration multiplier | `2.00`, `1.50`, `1.00` |
| `%rankedsmp_xp_multiplier%` | XP gain multiplier | `3.00`, `2.00`, `1.00` |
| `%rankedsmp_extra_slots%` | Extra inventory slots | `54`, `30`, `0` |

### Display Formatting

| Placeholder | Description | Example Output |
|-------------|-------------|----------------|
| `%rankedsmp_rank_prefix%` | Rank prefix from TAB config | `[#1] `, `[#5] `, `[Unranked] ` |
| `%rankedsmp_prefix%` | Alias for `rank_prefix` | Same as above |
| `%rankedsmp_rank_suffix%` | Rank suffix from TAB config | ` ⭐`, ` 👑`, `` |
| `%rankedsmp_suffix%` | Alias for `rank_suffix` | Same as above |

## Special Cases

### Blacklisted Players
- `%rankedsmp_rank%` → `Blacklisted`
- `%rankedsmp_rank_prefix%` → Blacklisted prefix from config

### Unranked Players
- `%rankedsmp_rank%` → `Unranked`
- `%rankedsmp_rank_number%` → `0`
- `%rankedsmp_hearts%` → `20.0` (default unranked hearts)
- `%rankedsmp_potion_multiplier%` → `1.00`
- `%rankedsmp_xp_multiplier%` → `1.00`
- `%rankedsmp_extra_slots%` → `0`

## Usage Examples

### Chat Formatting (EssentialsX)

```yaml
# config.yml (EssentialsX)
format: '{DISPLAYNAME} &8[%rankedsmp_rank%&8]&r: {MESSAGE}'
```

Output: `PlayerName [#1]: Hello!`

### Scoreboard (FeatherBoard)

```yaml
# featherboard/board.yml
lines:
  - '&6Your Rank: &e%rankedsmp_rank%'
  - '&cHearts: &f%rankedsmp_hearts%'
  - '&dPotion Boost: &f%rankedsmp_potion_multiplier%x'
  - '&aXP Boost: &f%rankedsmp_xp_multiplier%x'
```

### Tab List (TAB Plugin)

```yaml
# TAB/config.yml
scoreboard-teams:
  prefix: '%rankedsmp_rank_prefix%'
  suffix: '%rankedsmp_rank_suffix%'
```

### Holograms (HolographicDisplays)

```
/hd create ranks
/hd setline ranks 1 &6Top Player
/hd setline ranks 2 %rankedsmp_rank% - %player_name%
```

### Nametags (NametagEdit)

```yaml
# NametagEdit config
Groups:
  rank1:
    Permission: rankedsmp.rank.1
    Prefix: '%rankedsmp_rank_prefix%'
    Suffix: ' &c%rankedsmp_hearts%❤'
```

### Join/Quit Messages (DeluxeChat)

```yaml
# config.yml (DeluxeChat)
join-message: '&e%rankedsmp_rank_prefix%%player% &ahas joined!'
quit-message: '&e%rankedsmp_rank_prefix%%player% &chas left!'
```

### Action Bar Messages

```yaml
# Your plugin config
actionbar: '&6Rank: &e%rankedsmp_rank% &8| &cHearts: &f%rankedsmp_hearts%'
```

## Complex Examples

### Full Rank Display
```
&6[Rank %rankedsmp_rank%] &c%rankedsmp_hearts%❤ &d⚗%rankedsmp_potion_multiplier%x &a✨%rankedsmp_xp_multiplier%x
```

Output: `[Rank 1] 20.0❤ ⚗2.00x ✨3.00x`

### Conditional Formatting (DeluxeChat)

```yaml
format: '{%rankedsmp_rank_prefix%}{player} &8»&r {message}'
```

Shows rank prefix before name, or nothing if unranked.

### Rank-Based Chat Color

```yaml
# With Conditional Permissions (Vault)
format: '%rankedsmp_rank_prefix%{player}&r: &f{message}'
```

## Plugin Compatibility

### Tested and Working
- ✅ EssentialsX (chat formatting)
- ✅ TAB Plugin (tab list, nametags)
- ✅ FeatherBoard (scoreboards)
- ✅ HolographicDisplays (holograms)
- ✅ DeluxeChat (chat formatting)
- ✅ NametagEdit (nametags)
- ✅ VaultAPI (permissions/prefix/suffix)

### Known Issues
- ❌ Some placeholders may not update immediately after rank change (use `/papi reload`)
- ❌ Prefix placeholders return raw config value (colors need to be parsed by target plugin)

## Troubleshooting

### Placeholders show as text
1. Verify PlaceholderAPI is installed
2. Check RankedSMPX is enabled
3. Run `/papi parse me %rankedsmp_rank%` to test
4. Run `/papi reload` to refresh

### Placeholders return empty
1. Check if player has a rank (`/rankinfo <player>`)
2. Verify game is active (ranks assigned)
3. Check placeholder spelling (case-sensitive)
4. Run `/papi ecloud download RankedSMP` if using cloud

### Prefix colors don't work
- Prefix placeholders return raw config value
- Target plugin must support color parsing
- Use MiniMessage format if plugin supports it
- Some plugins need `translateAlternateColorCodes()`

### Values don't update
- Run `/papi reload` after config changes
- Some plugins cache placeholder values
- Players may need to rejoin
- Run `/rsmp reload` to reload RankedSMPX config

## Testing Placeholders

### In-Game Testing
```
/papi parse me %rankedsmp_rank%
/papi parse me %rankedsmp_hearts%
/papi parse me %rankedsmp_rank_prefix%
```

### Console Testing
```
/papi parserel <player> %rankedsmp_rank%
```

### Reload PlaceholderAPI
```
/papi reload
```

## Additional Resources

- PlaceholderAPI Wiki: https://wiki.placeholderapi.com/
- PlaceholderAPI Discord: https://discord.gg/placeholderapi
- RankedSMPX Discord: https://discord.com/invite/QGsJYht2vy
