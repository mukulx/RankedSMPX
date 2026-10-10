# Hierarchy Hammer

The Hierarchy Hammer is a custom mace configured in `plugins/RankedSMPX/items.yml`.

## Give the hammer

- `/rsmp hammer` or `/rsmp mace` gives it to the admin using the command.
- `/rsmp give hammer <player>` gives it to a player.
- Set `hierarchy-hammer.auto-give-on-start: true` to grant it when a season starts.

These commands require admin access.

## Abilities

- **Dash:** right-click to launch forward. Strength (`dash-speed`) and cooldown (`dash-cooldown`) are configurable.
- **Verdict:** land the configured number of consecutive falling smash hits; the next smash triggers the empowered hit. Missing or waiting past the combo reset time resets progress. Damage, explosion power and cooldowns are configurable.
- **Mace smash:** set the minimum fall distance and damage multiplier. The default multiplier is neutral (`1.0`).
- **Particles:** `particles.enabled` turns the dash trail and effects on or off.
- **Death effect:** a particle skull appears where a player dies holding the hammer.

`prevent-storage` and `prevent-drop` keep the hammer out of chests, `/inv` and the ground. `enabled: false` turns the whole hammer off. The item name, lore and model are also in `items.yml`.

```yaml
hierarchy-hammer:
  enabled: true
  auto-give-on-start: false
  dash-cooldown: 0
  dash-speed: 1.0
  combo-required: 3
  combo-damage-multiplier: 1.0
  combo-explosion-power: 0.0
  combo-reset-time: 5
  miss-cooldown: 10
  verdict-cooldown: 30
  verdict-damage: 40.0
  mace-smash:
    enabled: true
    damage-multiplier: 1.0
    min-fall-distance: 1.5
  particles:
    enabled: true
  prevent-storage: true
  prevent-drop: false
```

Run `/rsmp reload` after editing settings. If the item loses its custom identity or abilities, give a fresh one with `/rsmp hammer`.
