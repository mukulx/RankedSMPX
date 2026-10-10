# PlaceholderAPI

Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.9089/) before RankedSMPX starts. RankedSMPX registers the `rankedsmp` expansion automatically; use its placeholders as `%rankedsmp_<name>%` in any plugin that supports PlaceholderAPI.

## Player placeholders

These require a player context:

| Placeholder | Value |
|---|---|
| `%rankedsmp_rank%` | Rank number, `Unranked`, or `Blacklisted` |
| `%rankedsmp_rank_number%` | Rank number; `0` when no ranked number is assigned |
| `%rankedsmp_hearts%` | Current max hearts |
| `%rankedsmp_xp_multiplier%` | XP multiplier, formatted to two decimals |
| `%rankedsmp_potion_multiplier%` | Potion duration multiplier, formatted to two decimals |
| `%rankedsmp_extra_slots%` | Extra inventory slot count |
| `%rankedsmp_rank_prefix%` / `%rankedsmp_prefix%` | Legacy-color rank prefix |
| `%rankedsmp_rank_prefix_minimessage%` / `%rankedsmp_prefix_minimessage%` | MiniMessage rank prefix |
| `%rankedsmp_rank_suffix%` / `%rankedsmp_suffix%` | Legacy-color rank suffix |
| `%rankedsmp_is_ranked%` | `true` for a ranked player, otherwise `false` |
| `%rankedsmp_is_blacklisted%` | Whether the player is blacklisted |
| `%rankedsmp_has_hammer%` | Whether the player is holding the Hierarchy Hammer in their main hand |

## Find a rank holder

Use `%rankedsmp_holder_<rank>%`, for example `%rankedsmp_holder_1%`, to return the current holder's name. It returns `None` if no player holds that rank.

## Use in a plugin

```yaml
text: "Rank %rankedsmp_rank% · %rankedsmp_hearts% hearts · %rankedsmp_xp_multiplier%x XP"
```

If values appear cached, run `/papi reload` and the target plugin's reload command. RankedSMPX refreshes its expansion when the plugin loads.
