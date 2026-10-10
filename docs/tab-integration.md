# Tab list integration

RankedSMPX adds each player's rank prefix and suffix to the tab list. Set the values per rank in [`ranks.yml`](ranks-yml.md), then run `/rsmp reload`.

```yaml
ranks:
  1: { hearts: 20.0, xp: 3.0, potion: 2.0, slots: 54, prefix: "&6[#1] &r", suffix: "" }
```

## With TAB installed

The optional [TAB plugin](https://www.spigotmc.org/resources/tab.57806/) enables its advanced formatting, including RGB and MiniMessage. The `tab-integration.enabled` setting in `config.yml` controls RankedSMPX tab formatting.

## Without TAB

RankedSMPX uses vanilla scoreboard teams. This fallback supports legacy Minecraft color and formatting codes such as `&6` and `&l`; it does not render TAB's RGB gradients.

## Prefix presets

- `/rsmp modern` applies the modern gradient prefix set.
- `/rsmp original` (alias `/rsmp og`) applies the classic color prefix set.

These commands rewrite the prefix values in `ranks.yml`, including custom prefixes. Back up the file first if you want to keep your own labels. Set a custom `prefix` or `suffix` per row to customize individual ranks.

If a prefix does not appear, check `tab-integration.enabled`, reload RankedSMPX, and rejoin. With TAB installed, also check TAB's group and sorting settings.
