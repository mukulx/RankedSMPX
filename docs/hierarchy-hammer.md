# Hierarchy Hammer

The Hierarchy Hammer is a custom mace weapon with special abilities: dash mechanics and a 4-hit combo system called "Verdict Mode".

## Overview

The Hierarchy Hammer is a powerful weapon that combines:
- **Sentencing (Dash)**: Right-click to dash forward
- **Verdict Mode**: Land 3 hits, then unleash a devastating 4th hit with 2x damage and explosion
- **Mace Smash**: Bonus damage when falling (like vanilla mace)

## Getting the Hammer

### Admin Command
```
/rsmp give hammer <player>
```

### Auto-Give on Game Start
```yaml
# config.yml
hierarchy-hammer:
  auto-give-on-start: false  # Set to true to auto-give
```

When enabled, the hammer is given to rank 1 player when the season starts.

## Sentencing (Dash Ability)

**How to use:** Right-click (RMB) with the hammer

**What it does:**
- Launches you forward in the direction you're looking
- Applies slow fall to help you aim your attacks
- Stops when you hit something or after a set time

**Configuration:**
```yaml
hierarchy-hammer:
  dash-distance: 5.0              # Distance in blocks
  dash-speed: 1.0                 # Speed multiplier
  dash-air-time: 15               # Ticks airborne (20 = 1 sec)
  dash-slow-fall-strength: 3      # Slow fall power (0-5)
  dash-cooldown: 0                # Cooldown in seconds (0 = only landing)
  dash-item-cooldown: 10          # Item cooldown after dash
  require-landing: true           # Must land before next dash
```

**Cooldown System:**
- If `dash-cooldown: 0` → Can dash again after landing
- If `dash-cooldown: 10` → 10 second cooldown between dashes
- `dash-item-cooldown` disables the mace like an ender pearl (prevents instant use)

## Verdict Mode (Combo System)

**How it works:**
1. Hit an enemy (1st hit) - Combo starts
2. Hit an enemy (2nd hit) - Combo continues
3. Hit an enemy (3rd hit) - Combo continues
4. Hit an enemy (4th hit) - **VERDICT!** 2x damage + explosion

**Combo Rules:**
- Miss or wait too long → Combo resets
- Only hits on valid targets count
- `players-only: true` → Only player hits count
- `players-only: false` → Both player and mob hits count

**Configuration:**
```yaml
hierarchy-hammer:
  combo-required: 3                 # Hits needed before verdict
  combo-damage-multiplier: 2.0      # Verdict damage multiplier
  combo-explosion-power: 2.0        # Explosion power (visual)
  combo-reset-time: 5               # Seconds before reset
  miss-cooldown: 10                 # Cooldown for missing
  players-only: true                # Only player hits count
```

**Visual Effects on Verdict:**
- Massive explosion particles
- 20 fake blocks fly out in a circle
- Spiral golden particles
- Ground-shaking sounds (explosion, anvil, wither)

## Mace Smash Bonus

**How it works:**
- Fall from a height and hit with the hammer
- Bonus damage based on fall distance
- Works like vanilla mace smash attacks

**Configuration:**
```yaml
hierarchy-hammer:
  mace-smash:
    enabled: true
    damage-multiplier: 2.0        # Damage multiplier when falling
    min-fall-distance: 1.5        # Minimum blocks fallen
```

**Stacking:**
- Mace Smash + Verdict Mode = Both multipliers apply!
- Example: 2x (smash) × 2x (verdict) = 4x total damage

## Miss Penalty

**What happens when you miss:**
- Swing and don't hit anything → Miss registered
- Hammer goes on cooldown for configured time
- Cannot attack or dash during cooldown

**Configuration:**
```yaml
hierarchy-hammer:
  miss-cooldown: 10  # Cooldown in seconds
```

**Action Bar Message:**
```
"You missed, now on cooldown for 10s"
```

## Storage & Drop Protection

**Prevent Storage:**
```yaml
hierarchy-hammer:
  prevent-storage: true  # Cannot store in chests, etc.
```

**Prevent Drop:**
```yaml
hierarchy-hammer:
  prevent-drop: false  # Can drop with Q key
```

When `prevent-drop: true`:
- Hammer cannot be dropped with Q key
- Useful for preventing accidental loss
- Admins must give new hammer if lost

## Strategy Tips

### Dash + Combo
1. Dash toward enemy
2. Land 3 quick hits
3. Dash away or prepare verdict
4. Land verdict hit for massive damage

### Aerial Combat
1. Dash into the air
2. Fall toward enemy
3. Land mace smash (2x damage)
4. Continue combo on ground

### Verdict Timing
- Watch your combo counter (action bar shows progress)
- Don't wait too long between hits (5 second timeout)
- Save verdict for high-value targets

### Miss Avoidance
- Only swing when close to target
- Use dash to close distance first
- Practice timing to avoid cooldown penalty

## Troubleshooting

### Hammer doesn't work
- Check `hierarchy-hammer.enabled: true` in config
- Check plugin is enabled (`/rsmp enable`)
- Verify you have the actual Hierarchy Hammer item

### Dash goes on cooldown immediately
- You might have missed a hit (miss penalty)
- Check `miss-cooldown` setting
- Ensure you're landing between dashes if `require-landing: true`

### Combo resets too fast
- Increase `combo-reset-time` in config
- Default is 5 seconds between hits
- Check if `players-only: true` is preventing mob hits from counting

### Verdict doesn't activate
- Need exactly 3 hits before the 4th
- Check if combo is resetting (missing or timeout)
- Verify `combo-required: 3` in config

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/rsmp give hammer <player>` | `rankedsmp.admin` | Give Hierarchy Hammer |
| `/rsmp hammer` | `rankedsmp.admin` | Give yourself hammer |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `rankedsmp.admin` | Give hammer to players | op |

## Known Issues

- Hammer is experimental and may have bugs
- Block displays may cause lag with many verdict hits
- Dash physics can be inconsistent on laggy servers
