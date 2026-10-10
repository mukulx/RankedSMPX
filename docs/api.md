# Developer API

Other plugins can read and control RankedSMPX through a small, stable API. It is in the package
`dev.mukulx.rankedsmpx.api`. Everything in `api` and `api.event` keeps working across 3.x: methods
and events are not removed or changed, only added. `RankedSMPApi#apiVersion()` goes up when
something is added. Anything outside those packages is internal.

## Add it to your plugin

Use the release from [JitPack](https://jitpack.io/#mukulx/RankedSMPX) as a compile-only dependency. Do not shade it.

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.mukulx:RankedSMPX:3.0.0")
}
```

You can also put the plugin jar on your compile classpath by hand. Use a release tag for the version, or `main-SNAPSHOT` for the latest code.

In `paper-plugin.yml` (or `plugin.yml`):

```yaml
dependencies:
  server:
    RankedSMPX:
      load: BEFORE
      required: false
```

## Use it

```java
Optional<RankedSMPApi> found = RankedSMP.find();   // empty if RankedSMPX is not running
RankedSMPApi api = RankedSMP.get();                // throws if it is not running

OptionalInt rank = api.getRank(player.getUniqueId());
RankSettings perks = api.getRankSettings(player.getUniqueId());
double xp = perks.xpMultiplier();

RankResult result = api.setRank(uuid, 3, "Won the event");
```

Call methods from the main thread (on Folia, the thread that owns the player). Read-only methods
are safe from any thread. Returned collections are immutable snapshots.

## What you can do

| Area | Methods |
| --- | --- |
| Ranks | `getRank`, `hasRank`, `getPlayerWithRank`, `getTotalRanks`, `getRanks`, `getLeaderboard` |
| Change ranks | `setRank`, `removeRank`, `swapRanks` |
| Rank values | `getRankSettings(rank)`, `getRankSettings(player)` |
| Blacklist | `isBlacklisted`, `blacklist`, `unblacklist` |
| Season | `isSeasonActive`, `startSeason`, `stopSeason` |
| Discord | `isDiscordEnabled`, `getDiscordId`, `getLinkedPlayer` |
| Plugin | `apiVersion`, `pluginVersion`, `reload` |

`setRank` applies hearts, the extra inventory, the tab prefix, the Hierarchy Hammer holder and the
Discord role, and writes the rank history. It sends no chat message, title or broadcast, so your
plugin decides how to announce the change. It returns a `RankResult`: `SUCCESS`, `INVALID_RANK`,
`RANK_TAKEN` (someone else holds it, remove it from them or use `swapRanks`) or `BLACKLISTED`.

`RankSettings` is one row of `ranks.yml`: rank, hearts, XP multiplier, potion multiplier, extra
slots, prefix and suffix. Rank `0` gives the unranked row and `-1` the blacklisted row.

## Events

All events are fired after the change has happened, so they cannot be cancelled. They can fire off
the main thread: check `event.isAsynchronous()` before touching the world.

| Event | When |
| --- | --- |
| `RankChangeEvent` | A player gained, lost or changed rank. Has the old rank, the new rank and a reason. |
| `SeasonStartEvent` | A season started. Has the number of ranked players. |
| `SeasonStopEvent` | The season ended and ranks were cleared. |
| `BlacklistChangeEvent` | A player was added to or removed from the blacklist. |
| `DiscordLinkChangeEvent` | A player linked or unlinked Discord. |

```java
@EventHandler
public void onRankChange(RankChangeEvent event) {
    if (event.isGain() && Integer.valueOf(1).equals(event.getNewRank())) {
        Bukkit.broadcast(Component.text(event.getPlayer() + " is now rank 1"));
    }
}
```

Events from a season start or stop are sent once per player, followed by one `SeasonStartEvent` or
`SeasonStopEvent`.

## Compatibility

- Check `api.apiVersion()` if you rely on a method added after version 1.
- Do not cast the API object or use classes outside `api`; they can change without notice.
