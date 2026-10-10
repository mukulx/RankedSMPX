# Rank settings (`ranks.yml`)

Edit `plugins/RankedSMPX/ranks.yml` to configure the rank pool and each rank's perks. The shipped file contains rows `-1`, `0`, and `1` through `20`.

```yaml
ranks:
  -1:
    prefix: "&0[&8BLACKLISTED&0] &r"
    suffix: ""
  0:
    hearts: 10.0
    xp: 1.0
    potion: 1.0
    slots: 0
    prefix: "&e[&7UNRANKED&e] &r"
    suffix: ""
  1:
    hearts: 20.0
    xp: 3.0
    potion: 2.0
    slots: 54
    prefix: "&e[#1] &r"
    suffix: ""
  2:
    hearts: 19.5
    xp: 2.9
    potion: 1.96
    slots: 54
    prefix: "&e[#2] &r"
    suffix: ""
```

| Row | Meaning |
|---|---|
| `-1` | Blacklisted label; only `prefix` and `suffix` are used |
| `0` | Unranked players |
| `1` and above | Ranked players |

| Field | Meaning |
|---|---|
| `hearts` | Maximum health in hearts (1 heart = 2 health points) |
| `xp` | XP gain multiplier |
| `potion` | Potion duration multiplier |
| `slots` | Extra `/inv` slots; `0` disables it, maximum `54` |
| `prefix`, `suffix` | Tab list labels; use legacy `&` colors or MiniMessage where supported |

## Add a rank

Add a numbered row above the current highest rank, then run `/rsmp reload`. The highest positive row becomes the rank count. For example, adding row `21` creates rank 21.

Rows can omit values; missing fields inherit the prior row. Invalid rows or values are reported in the console and fall back to usable defaults.

## Upgrade behavior

If `ranks.yml` does not exist, older config versions that stored rank values in `config.yml` are imported automatically. Existing `ranks.yml` files are kept. Before a config layout migration, the old config is backed up in `plugins/RankedSMPX/backups/`.

`/rsmp modern` and `/rsmp original` update the rank prefix values. Back up custom comments in `ranks.yml` before using those presets because the file is rewritten.
