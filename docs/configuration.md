# Configuration Guide

Complete guide to configuring RankedSMPX via `config.yml`. All examples are taken from the actual default configuration.

## Table of Contents

- [Messages](#messages)
- [Database](#database)
- [Season Settings](#season-settings)
- [Rank Display](#rank-display)
- [Health System](#health-system)
- [Potion Multipliers](#potion-multipliers)
- [XP Multipliers](#xp-multipliers)
- [Extra Inventory](#extra-inventory)
- [Hierarchy Hammer](#hierarchy-hammer)
- [Dragon Egg Locator](#dragon-egg-locator)
- [Diamond Spear](#diamond-spear)
- [TAB Integration](#tab-integration)
- [Sound Effects](#sound-effects)
- [Rank Transfer](#rank-transfer)
- [Debug Mode](#debug-mode)

---

## Messages

### Small Caps Messages

Convert all messages to small caps font while preserving colors and formatting.

```yaml
# Small Caps Messages - Convert all messages to small caps font (ʜᴇʟʟᴏ)
small-caps-messages: true
```

**Example:** "Hello World" becomes "ʜᴇʟʟᴏ ᴡᴏʀʟᴅ"

---

## Database

Configure database backend for persistent storage.

### SQLite (Default - Zero Configuration)

```yaml
database:
  type: sqlite
  sqlite-file: rankedsmp.db
```

### MySQL (Requires External Setup)

```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: rankedsmp
    username: root
    password: your_secure_password
    pool:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**HikariCP Pool Settings:**
- `maximum-pool-size`: Max number of connections (10 recommended)
- `minimum-idle`: Minimum idle connections (2 recommended)
- `connection-timeout`: Time to wait for connection (30000ms = 30s)
- `idle-timeout`: How long idle connections stay alive (600000ms = 10min)
- `max-lifetime`: Maximum connection lifetime (1800000ms = 30min)

---

## Season Settings

Configure total ranks and rank stealing mechanics.

```yaml
season:
  total-ranks: 20
  
  rank-stealing:
    enabled: true
    max-rank-difference: 0  # 0 = unlimited, 5 = can only steal ±5 ranks
  
  death-effects:
    lightning-on-kill: false  # Spawn lightning when player is killed
```

**Examples:**
- `max-rank-difference: 0` → Rank #20 can steal from Rank #1
- `max-rank-difference: 5` → Rank #20 can only steal from Rank #15-#20
- `lightning-on-kill: true` → Lightning strikes victim's location on PvP death

---

## Rank Display

Control how ranks appear in join/quit/death messages.

```yaml
rank-display:
  show-on-join: true
  show-on-leave: true
  show-on-death: true
  format: "&b#{rank} &r{player}"
  unranked-format: "&7Unranked &r{player}"
```

**Placeholders:**
- `{rank}` → Rank number (1-20)
- `{player}` → Player name

**Examples:**
- `"&#rank} {player}"` → `#5 PlayerName`
- `"{player} [Rank {rank}]"` → `PlayerName [Rank 5]`
- `"&6R{rank} &r{player}"` → Gold "R5" then player name

---

## Health System

Configure max health (hearts) by rank.

### Basic Configuration

```yaml
health:
  max-hearts: 20.0      # Rank 1
  min-hearts: 10.5      # Rank 20
  unranked-hearts: 10.0 # Unranked players
  apply-on-respawn: true
```

**Linear Interpolation:** Plugin automatically calculates heart values between ranks 1-20.

### Custom Hearts Per Rank

Override linear interpolation with exact values:

```yaml
health:
  custom-hearts:
    0: 10.0   # Unranked
    1: 20.0   # Rank 1
    2: 19.5
    3: 19.0
    4: 18.5
    5: 18.0
    6: 17.5
    7: 17.0
    8: 16.5
    9: 16.0
    10: 15.5
    11: 15.0
    12: 14.5
    13: 14.0
    14: 13.5
    15: 13.0
    16: 12.5
    17: 12.0
    18: 11.5
    19: 11.0
    20: 10.5  # Rank 20
```

**Note:** 1 heart = 2 health points. 20 hearts = 40 health.

---

## Potion Multipliers

Extend potion duration based on rank.

### Basic Configuration

```yaml
potions:
  rank-1-multiplier: 2.0    # 200% duration for Rank 1
  rank-20-multiplier: 1.05  # 105% duration for Rank 20
  unranked-multiplier: 1.0  # 100% duration for unranked
  extend-custom-effects: true
```

**Example:** Rank 1 drinks 3-minute potion → 6 minutes (3 × 2.0)

### Ignored Effects

Prevent negative effects from being extended:

```yaml
potions:
  ignored-effects:
    - DARKNESS
    - POISON
    - WITHER
    - HUNGER
    - WEAKNESS
    - SLOWNESS
    - MINING_FATIGUE
    - NAUSEA
    - BLINDNESS
    - INSTANT_DAMAGE
    - UNLUCK
    - BAD_OMEN
    - RAID_OMEN
    - LEVITATION
```

### Custom Multipliers Per Rank

Override linear interpolation:

```yaml
potions:
  custom-multipliers:
    0: 1.0    # Unranked
    1: 2.0    # Rank 1
    2: 1.96
    3: 1.91
    4: 1.86
    5: 1.81
    6: 1.76
    7: 1.71
    8: 1.66
    9: 1.61
    10: 1.56
    11: 1.51
    12: 1.46
    13: 1.41
    14: 1.36
    15: 1.31
    16: 1.26
    17: 1.21
    18: 1.16
    19: 1.11
    20: 1.05  # Rank 20
```

---

## XP Multipliers

Multiply XP gained from all sources.

### Basic Configuration

```yaml
xp:
  rank-1-multiplier: 3.0   # 300% XP for Rank 1
  rank-20-multiplier: 1.1  # 110% XP for Rank 20
  unranked-multiplier: 1.0
```

**Example:** Rank 1 kills mob that drops 5 XP → 15 XP (5 × 3.0)

### Custom Multipliers Per Rank

```yaml
xp:
  custom-multipliers:
    0: 1.0    # Unranked
    1: 3.0    # Rank 1
    2: 2.9
    3: 2.8
    4: 2.7
    5: 2.6
    6: 2.5
    7: 2.4
    8: 2.3
    9: 2.2
    10: 2.1
    11: 2.0
    12: 1.9
    13: 1.8
    14: 1.7
    15: 1.6
    16: 1.5
    17: 1.4
    18: 1.3
    19: 1.2
    20: 1.1   # Rank 20
```

**Applies to:** Mining, killing mobs, smelting, breeding, fishing, etc.

---

## Extra Inventory

Give extra inventory slots to top 10 ranks.

```yaml
inventory:
  enabled: true
  drop-on-death: true
  combat:
    disabled-during-combat: false
    combat-duration: 10  # seconds
  slots:
    1: 54   # Rank 1: 54 slots (6 rows - maximum)
    2: 54   # Rank 2: 54 slots
    3: 50   # Rank 3: 50 slots
    4: 45   # Rank 4: 45 slots
    5: 40   # Rank 5: 40 slots
    6: 35   # Rank 6: 35 slots
    7: 30   # Rank 7: 30 slots
    8: 25   # Rank 8: 25 slots
    9: 20   # Rank 9: 20 slots
    10: 15  # Rank 10: 15 slots
```

**Notes:**
- Maximum 54 slots (6 rows) due to Minecraft limitations
- Access with `/inv` command
- Unused slots filled with barrier blocks
- `drop-on-death: true` → Items drop on death
- `drop-on-death: false` → Items kept on death

**Combat System:**
- `disabled-during-combat: true` → Can't open during combat
- `combat-duration: 10` → 10 seconds after damage = combat

---

## Hierarchy Hammer

Configure the custom mace weapon with dash and combo abilities.

### Basic Settings

```yaml
hierarchy-hammer:
  enabled: true
  auto-give-on-start: false
  players-only: true
```

- `auto-give-on-start: false` → Admin must give with `/rsmp give hammer`
- `players-only: true` → Only player hits count toward combo

### Dash Mechanics

```yaml
hierarchy-hammer:
  dash-distance: 5.0
  dash-cooldown: 0
  dash-item-cooldown: 10
  dash-air-time: 15
  dash-slow-fall-strength: 3
  dash-speed: 1.0
  require-landing: true
```

**Settings Explained:**
- `dash-distance: 5.0` → Dash 5 blocks forward
- `dash-cooldown: 0` → No cooldown (only requires landing)
- `dash-item-cooldown: 10` → 10s item cooldown after dash
- `dash-air-time: 15` → Stay airborne for 15 ticks (0.75s)
- `dash-slow-fall-strength: 3` → Slow fall level 3 for aiming
- `require-landing: true` → Must land before next dash

### Combo System (Verdict Mode)

```yaml
hierarchy-hammer:
  combo-required: 3
  combo-damage-multiplier: 2.0
  combo-explosion-power: 2.0
  combo-reset-time: 5
  miss-cooldown: 10
```

**How it works:**
1. Land 3 hits consecutively
2. 4th hit deals 2x damage with explosion
3. Miss or wait >5s → combo resets
4. Miss → 10s cooldown penalty

### Mace Smash (Falling Damage Bonus)

```yaml
hierarchy-hammer:
  mace-smash:
    enabled: true
    damage-multiplier: 2.0
    min-fall-distance: 1.5
```

**Example:** Fall 2 blocks and hit → 2x damage

### Storage Protection

```yaml
hierarchy-hammer:
  prevent-storage: true
  prevent-drop: false
```

- `prevent-storage: true` → Can't store in chests/barrels/etc.
- `prevent-drop: false` → Can drop with Q key

### Custom Name and Lore

```yaml
hierarchy-hammer:
  name: "§x§f§2§b§c§4§5§lHierarchy Hammer"
  lore:
    - "&7A powerful hammer that manifested"
    - "&7as a result of improper judgement"
    - ""
    - "&6SENTENCING &fRMB"
    - "&7Launch forward, allowing you to initiate"
    - "&7hits with the weapon."
```

---

## Dragon Egg Locator

Show nearby players when holding dragon egg (1.21.6+ only).

```yaml
dragon-egg-locator:
  enabled: true
  update-interval: 20      # Update every 20 ticks (1 second)
  show-distance: true      # Show distance in blocks
  prevent-storage: true
  prevent-drop: false
```

**Action Bar Display:** `Players Nearby: PlayerName - 15m`

---

## Diamond Spear

Configure diamond spear with dash enchantment (1.21.11+ only).

```yaml
spear:
  enabled: true
  unbreakable: true
  prevent-storage: true
  prevent-drop: true
  enchantments:
    LUNGE: 3         # Dash ability
    # SHARPNESS: 5   # Extra damage
    # KNOCKBACK: 2   # Knockback
```

**Available Enchantments:**
- `LUNGE` → Spear-specific dash ability
- `SHARPNESS` → Extra damage
- `KNOCKBACK` → Knock enemies back
- `UNBREAKING` → Durability (if unbreakable is false)
- `MENDING` → Repair with XP

---

## TAB Integration

Configure rank prefixes in tab list (works with TAB plugin or vanilla).

### Basic Settings

```yaml
tab-integration:
  enabled: true
  unranked-prefix: '&e[&8UNRANKED&e] '
  unranked-suffix: ''
  blacklisted-prefix: '&0[&8BLACKLISTED&0] '
  blacklisted-suffix: ''
```

### Rank Prefixes

**Legacy Colors (Works Everywhere):**

```yaml
tab-integration:
  rank-prefixes:
    1: '&e[#1] '
    2: '&e[#2] '
    3: '&e[#3] '
    # ... up to 20
```

**RGB/Gradients (TAB Plugin Only):**

```yaml
tab-integration:
  rank-prefixes:
    1: '&#FFD700[#1] '                                    # Hex color
    2: '<gradient:#FFD700:#FFA500>[#2]</gradient> '      # Gradient
```

**PlaceholderAPI Support:**

```yaml
tab-integration:
  rank-prefixes:
    1: '&c%rankedsmp_hearts%❤ &6[#1] '  # Shows hearts
```

### Style Commands

- `/rsmp original` → Legacy yellow colors
- `/rsmp modern` → RGB gradients (requires TAB plugin)

---

## Sound Effects

Configure sound effects for game events.

```yaml
sounds:
  season-start:
    enabled: true
    sound: ENTITY_ENDER_DRAGON_GROWL
    volume: 1.0
    pitch: 1.0
  
  rank-reveal:
    enabled: true
    sound: ENTITY_PLAYER_LEVELUP
    volume: 1.0
    pitch: 1.2
  
  rank-broadcast:
    enabled: true
    sound: BLOCK_NOTE_BLOCK_PLING
    volume: 0.5
    pitch: 1.5
  
  season-stop:
    enabled: true
    sound: ENTITY_WITHER_DEATH
    volume: 0.8
    pitch: 1.0
```

**Sound List:** https://www.digminecraft.com/lists/sound_list_pc.php

---

## Rank Transfer

Allow players to transfer their rank to another player.

```yaml
rank-transfer:
  enabled: false           # Disabled by default
  allow-offline: false     # Can transfer to offline players
```

**Command:** `/ranktransfer <player>`

---

## Debug Mode

Enable detailed logging for troubleshooting.

```yaml
debug:
  enabled: false
  log-death-events: false
  log-chat-events: false
  log-xp-events: false
  log-potion-events: false
```

**Example Output:**
```
[RankedSMPX] [DEBUG] PlayerName killed by PlayerName2 - stealing rank #5
[RankedSMPX] [DEBUG] PlayerName XP: 5 → 15 (multiplier: 3.0x)
```

---

## Reloading Configuration

Apply changes without restarting:

```
/rsmp reload
```

This reloads:
- `config.yml`
- `discord.yml`
- `messages.yml`
- TAB integration
- All cached values

**Note:** Some changes may require players to rejoin for full effect.

---

## Configuration Version

```yaml
config-version: 7
```

**Do not edit this value!** It's used for automatic config updates.

---

## Tips

1. **Backup first** before making major changes
2. **Use `/rsmp reload`** after editing config
3. **Check console** for config errors on reload
4. **Test with `/rsmp test`** to see all multiplier values
5. **Linear interpolation** is automatic - only use custom values if needed

---

## Common Issues

### Config not reloading
- Check console for YAML syntax errors
- Ensure proper indentation (spaces, not tabs)
- Restart server if `/rsmp reload` doesn't work

### Custom values not applying
- Make sure `custom-hearts` / `custom-multipliers` section exists
- Check rank number matches (1-20, plus 0 for unranked)
- Reload after changes

### TAB prefixes not showing
- Verify `tab-integration.enabled: true`
- Check game is active (`/rsmp start`)
- Use `/rsmp reload` and `/tab reload` if using TAB plugin

---

For Discord bot configuration, see [discord.yml guide](discord.md).
