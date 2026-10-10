# Rank perks

Each player receives the values configured for their row in `ranks.yml`. These are the plugin's shipped defaults; server owners can change every value or add ranks.

| Rank | Hearts | XP | Potion duration | Extra slots |
|---:|---:|---:|---:|---:|
| Unranked (0) | 10.0 | 1.0× | 1.00× | 0 |
| 1 | 20.0 | 3.0× | 2.00× | 54 |
| 2 | 19.5 | 2.9× | 1.96× | 54 |
| 3 | 19.0 | 2.8× | 1.91× | 50 |
| 4 | 18.5 | 2.7× | 1.86× | 45 |
| 5 | 18.0 | 2.6× | 1.81× | 40 |
| 6 | 17.5 | 2.5× | 1.76× | 35 |
| 7 | 17.0 | 2.4× | 1.71× | 30 |
| 8 | 16.5 | 2.3× | 1.66× | 25 |
| 9 | 16.0 | 2.2× | 1.61× | 20 |
| 10 | 15.5 | 2.1× | 1.56× | 15 |
| 11 | 15.0 | 2.0× | 1.51× | 0 |
| 12 | 14.5 | 1.9× | 1.46× | 0 |
| 13 | 14.0 | 1.8× | 1.41× | 0 |
| 14 | 13.5 | 1.7× | 1.36× | 0 |
| 15 | 13.0 | 1.6× | 1.31× | 0 |
| 16 | 12.5 | 1.5× | 1.26× | 0 |
| 17 | 12.0 | 1.4× | 1.21× | 0 |
| 18 | 11.5 | 1.3× | 1.16× | 0 |
| 19 | 11.0 | 1.2× | 1.11× | 0 |
| 20 | 10.5 | 1.1× | 1.05× | 0 |

One heart is two health points. Extra inventory is available to any rank whose `slots` value is above zero. The blacklisted row (`-1`) uses its prefix and suffix labels; it does not grant rank perks. Add rows above 20 in [`ranks.yml`](ranks-yml.md) to extend the rank pool. Missing values on a row inherit from the previous rank; new rows can therefore keep the preceding perks or override only the values you want to change.

## How perks apply

- **Hearts:** sets maximum health in hearts. The health manager reapplies the configured maximum when a player joins, changes rank, or respawns. `config.yml` contains the compatibility mode for servers with other health plugins.
- **XP:** multiplies experience gained while the player has that rank.
- **Potion duration:** multiplies the duration of eligible potion effects. `config.yml` controls custom effects and effect types to ignore.
- **Extra inventory:** ranks with a positive slot count can use `/inv`; `slots` tops out at 54. `config.yml` controls combat access, death drops, and anti-duplication safeguards.
- **Prefixes and suffixes:** rank rows supply the labels used in tab integration. See [TAB integration](tab-integration.md) for display setup and supported placeholders.

Use `/rankinfo [player]` or `/rank [player]` to view a player's current perks. Use `/rsmp test` to print the rank table values.
