# Configuration

All files are in `plugins/RankedSMPX/`. Edit them, then run `/rsmp reload`.

| File | Controls |
|---|---|
| `config.yml` | Season rules, health, potions, inventory, sounds, database |
| [`ranks.yml`](ranks-yml.md) | Rank count and per-rank hearts, XP, potions, slots, prefix, suffix |
| `items.yml` | Hierarchy Hammer, Diamond Spear, Dragon Egg locator |
| `messages.yml` | In-game messages |
| `discord.yml` | [Discord bot](discord.md) |
| `custom.yml` | Experimental rank-drop item, off by default |

Every option has a short comment in its file.

## Notes

- Changing the database type or enabling the Discord bot needs a restart.
- Do not edit `config-version`. Updates add new options and keep your values and comments.
- On an upgrade the old files are backed up in `backups/`.
- Use spaces, not tabs. Quote text containing `:` or `#`.
