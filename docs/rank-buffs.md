# Rank Buffs

Complete breakdown of perks and bonuses for each rank.

## How Buffs Work

Rank buffs are **progressive** — the higher your rank (closer to #1), the better your perks. All multipliers scale linearly from Rank #1 to Rank #20.

## Health Scaling

Extra hearts based on rank position.

| Rank | Hearts | HP Value |
|------|--------|----------|
| #1 | 20.0 ❤ | 40 HP |
| #2 | 19.5 ❤ | 39 HP |
| #3 | 19.0 ❤ | 38 HP |
| #4 | 18.5 ❤ | 37 HP |
| #5 | 18.0 ❤ | 36 HP |
| #6 | 17.5 ❤ | 35 HP |
| #7 | 17.0 ❤ | 34 HP |
| #8 | 16.5 ❤ | 33 HP |
| #9 | 16.0 ❤ | 32 HP |
| #10 | 15.5 ❤ | 31 HP |
| #11 | 15.0 ❤ | 30 HP |
| #12 | 14.5 ❤ | 29 HP |
| #13 | 14.0 ❤ | 28 HP |
| #14 | 13.5 ❤ | 27 HP |
| #15 | 13.0 ❤ | 26 HP |
| #16 | 12.5 ❤ | 25 HP |
| #17 | 12.0 ❤ | 24 HP |
| #18 | 11.5 ❤ | 23 HP |
| #19 | 11.0 ❤ | 22 HP |
| #20 | 10.5 ❤ | 21 HP |
| Unranked | 10.0 ❤ | 20 HP |

**Notes:**
- Health applies immediately on rank change
- Survives death and respawn
- Resets to 10 hearts when rank is lost

## Potion Duration Multipliers

All positive potion effects last longer based on rank.

| Rank | Multiplier | Example (3min potion) |
|------|------------|----------------------|
| #1 | 2.0x | 6 minutes |
| #2 | 1.96x | 5m 52s |
| #3 | 1.91x | 5m 44s |
| #4 | 1.86x | 5m 34s |
| #5 | 1.81x | 5m 25s |
| #6 | 1.76x | 5m 16s |
| #7 | 1.71x | 5m 7s |
| #8 | 1.66x | 4m 59s |
| #9 | 1.61x | 4m 50s |
| #10 | 1.56x | 4m 40s |
| #11 | 1.51x | 4m 32s |
| #12 | 1.46x | 4m 23s |
| #13 | 1.41x | 4m 14s |
| #14 | 1.36x | 4m 5s |
| #15 | 1.31x | 3m 56s |
| #16 | 1.26x | 3m 47s |
| #17 | 1.21x | 3m 38s |
| #18 | 1.16x | 3m 29s |
| #19 | 1.11x | 3m 20s |
| #20 | 1.05x | 3m 9s |
| Unranked | 1.0x | 3 minutes |

**Affected Positive Effects:**
- Speed, Haste, Strength, Jump Boost
- Regeneration, Resistance, Absorption
- Fire Resistance, Water Breathing
- Night Vision, Invisibility, Luck
- Slow Falling, Conduit Power, Dolphins Grace
- Hero of the Village, Oozing, Weaving, Wind Charged, Infested

**Ignored Negative Effects:**
- Darkness, Poison, Wither, Hunger
- Weakness, Slowness, Mining Fatigue
- Nausea, Blindness, Instant Damage
- Unluck, Bad Omen, Raid Omen, Levitation

## Experience Multipliers

Gain more XP from all sources based on rank.

| Rank | Multiplier | Example (10 XP) |
|------|------------|----------------|
| #1 | 3.0x | 30 XP |
| #2 | 2.9x | 29 XP |
| #3 | 2.8x | 28 XP |
| #4 | 2.7x | 27 XP |
| #5 | 2.6x | 26 XP |
| #6 | 2.5x | 25 XP |
| #7 | 2.4x | 24 XP |
| #8 | 2.3x | 23 XP |
| #9 | 2.2x | 22 XP |
| #10 | 2.1x | 21 XP |
| #11 | 2.0x | 20 XP |
| #12 | 1.9x | 19 XP |
| #13 | 1.8x | 18 XP |
| #14 | 1.7x | 17 XP |
| #15 | 1.6x | 16 XP |
| #16 | 1.5x | 15 XP |
| #17 | 1.4x | 14 XP |
| #18 | 1.3x | 13 XP |
| #19 | 1.2x | 12 XP |
| #20 | 1.1x | 11 XP |
| Unranked | 1.0x | 10 XP |

**All XP Sources:**
- Killing mobs
- Mining ores
- Smelting items
- Breeding animals
- Fishing
- Trading with villagers
- Breaking bottles o' enchanting

## Extra Inventory Slots

Bonus inventory storage for top 10 ranks only.

| Rank | Extra Slots | Total Inventory |
|------|-------------|----------------|
| #1 | 54 slots | 90 slots |
| #2 | 54 slots | 90 slots |
| #3 | 50 slots | 86 slots |
| #4 | 45 slots | 81 slots |
| #5 | 40 slots | 76 slots |
| #6 | 35 slots | 71 slots |
| #7 | 30 slots | 66 slots |
| #8 | 25 slots | 61 slots |
| #9 | 20 slots | 56 slots |
| #10 | 15 slots | 51 slots |
| #11-20 | 0 slots | 36 slots |
| Unranked | 0 slots | 36 slots |

**Features:**
- Access with `/inv` or `/extrainventory`
- Persistent storage (survives death and logout)
- Items drop on death (configurable)
- Cannot open during combat (configurable)
- Excess items drop if rank is lost

## Special Rank Bonuses

### Rank #1 Only

**Hierarchy Hammer**
- Legendary mace weapon
- Dash ability (right-click)
- 4-hit combo system (Verdict Mode)
- Auto-given on rank assignment
- Removed when rank is lost

## Buff Customization

All multipliers and values can be customized in `config.yml`:

```yaml
# Custom hearts per rank
health:
  custom-hearts:
    1: 20.0
    2: 19.5
    # ... customize each rank

# Custom potion multipliers
potions:
  custom-multipliers:
    1: 2.0
    2: 1.96
    # ... customize each rank

# Custom XP multipliers
xp:
  custom-multipliers:
    1: 3.0
    2: 2.9
    # ... customize each rank

# Custom inventory slots
inventory:
  slots:
    1: 54
    2: 54
    # ... customize each rank
```

## Viewing Your Buffs

Use `/rank` or `/rankinfo` to see your current buffs:

```
━━━━━━━━━━━━ YOUR RANK ━━━━━━━━━━━━
   Rank: #5 ⭐
   
   ❤ Hearts: 18.0
   ⚗ Potion Duration: 1.81x
   ✨ XP Multiplier: 2.6x
   📦 Extra Inventory: 40 slots
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

Use `/ranks` to open GUI showing all ranks and their buffs.
