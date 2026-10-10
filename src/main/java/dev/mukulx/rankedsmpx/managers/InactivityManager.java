package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.api.RankResult;
import dev.mukulx.rankedsmpx.api.RankedSMPApi;
import dev.mukulx.rankedsmpx.api.internal.RankedSMPApiImpl;
import dev.mukulx.rankedsmpx.utils.DurationParser;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Takes the rank from players who have been offline for too long (season.inactivity in
 * config.yml). Works while the player is offline, using the server's last-played time.
 */
public class InactivityManager {

  private static final long MIN_INTERVAL_MS = 60_000L;

  private final RankedSMPXPlugin plugin;
  private final RankedSMPApi api;
  private final long startedAt = System.currentTimeMillis();
  private Object task;

  public InactivityManager(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    this.api = new RankedSMPApiImpl(plugin);
  }

  public void start() {
    stop();
    var config = plugin.getConfig();
    if (!config.getBoolean("season.inactivity.enabled", false)) {
      return;
    }
    OptionalLong after = DurationParser.parseMillis(config.getString("season.inactivity.after", "7d"));
    OptionalLong interval =
        DurationParser.parseMillis(config.getString("season.inactivity.check-interval", "30m"));
    if (after.isEmpty() || after.getAsLong() <= 0) {
      plugin.getLogger().warning("season.inactivity.after is not a valid time (use 12h, 3d, 1d12h). Inactivity check is off.");
      return;
    }
    long intervalTicks = Math.max(MIN_INTERVAL_MS, interval.orElse(1_800_000L)) / 50L;
    task = SchedulerUtil.runTimer(plugin, this::check, intervalTicks, intervalTicks);
    plugin.getLogger().info("Inactivity check on: ranks are taken after " + config.getString("season.inactivity.after", "7d") + " offline");
  }

  public void stop() {
    if (task != null) {
      SchedulerUtil.cancelTask(task);
      task = null;
    }
  }

  /**
   * Time the player was last seen. Time the server itself was down does not count against anyone,
   * so a restart after a long downtime never takes ranks.
   */
  static long effectiveLastSeen(long lastPlayed, long serverStartedAt) {
    return lastPlayed <= 0 ? 0 : Math.max(lastPlayed, serverStartedAt);
  }

  /** Ranked players whose last visit is older than the limit. Online and exempt players never match. */
  static List<UUID> findInactive(
      @NotNull Map<UUID, Integer> ranks,
      @NotNull ToLongFunction<UUID> lastSeen,
      @NotNull Predicate<UUID> online,
      @NotNull Predicate<UUID> exempt,
      long now,
      long limitMillis) {
    return ranks.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .map(Map.Entry::getKey)
        .filter(uuid -> !online.test(uuid) && !exempt.test(uuid))
        .filter(
            uuid -> {
              long seen = lastSeen.applyAsLong(uuid);
              return seen > 0 && now - seen >= limitMillis;
            })
        .toList();
  }

  void check() {
    var config = plugin.getConfig();
    if (!config.getBoolean("season.inactivity.enabled", false) || !api.isSeasonActive()) {
      return;
    }
    String after = config.getString("season.inactivity.after", "7d");
    OptionalLong limit = DurationParser.parseMillis(after);
    if (limit.isEmpty() || limit.getAsLong() <= 0) {
      return;
    }

    Predicate<UUID> exempt = exemptFilter(config.getStringList("season.inactivity.exempt"));
    List<UUID> inactive =
        findInactive(
            api.getRanks(),
            uuid ->
                effectiveLastSeen(Bukkit.getOfflinePlayer(uuid).getLastPlayed(), startedAt),
            uuid -> Bukkit.getPlayer(uuid) != null,
            exempt,
            System.currentTimeMillis(),
            limit.getAsLong());

    boolean reroll = "reroll".equalsIgnoreCase(config.getString("season.inactivity.action", "reroll"));
    for (UUID uuid : inactive) {
      takeRank(uuid, after, reroll, exempt);
    }
  }

  /** Frees the rank and, when rerolling, hands the same rank to a random eligible online player. */
  private void takeRank(UUID uuid, String after, boolean reroll, Predicate<UUID> exempt) {
    OptionalInt current = api.getRank(uuid);
    if (current.isEmpty() || !api.removeRank(uuid, "Inactive for " + after)) {
      return;
    }
    int rank = current.getAsInt();
    String name = nameOf(uuid);

    Player winner = reroll ? pickWinner(exempt) : null;
    if (winner != null
        && api.setRank(winner.getUniqueId(), rank, "Took over inactive " + name)
            == RankResult.SUCCESS) {
      announce("inactivity.rerolled", name, rank, after, winner.getName());
      plugin.getLogger().info("Rank #" + rank + " of inactive " + name + " given to " + winner.getName());
      return;
    }
    announce("inactivity.removed", name, rank, after, null);
    plugin.getLogger().info("Rank #" + rank + " of inactive " + name + " removed");
  }

  /** A random online player with no rank who is not blacklisted or exempt. */
  private Player pickWinner(Predicate<UUID> exempt) {
    List<Player> pool =
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> !api.hasRank(p.getUniqueId()))
            .filter(p -> !api.isBlacklisted(p.getUniqueId()))
            .filter(p -> !exempt.test(p.getUniqueId()))
            .map(p -> (Player) p)
            .toList();
    return pool.isEmpty() ? null : pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
  }

  private Predicate<UUID> exemptFilter(List<String> entries) {
    Set<String> exempt =
        entries.stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    if (exempt.isEmpty()) {
      return uuid -> false;
    }
    return uuid -> {
      if (exempt.contains(uuid.toString().toLowerCase(Locale.ROOT))) {
        return true;
      }
      String name = Bukkit.getOfflinePlayer(uuid).getName();
      return name != null && exempt.contains(name.toLowerCase(Locale.ROOT));
    };
  }

  private static String nameOf(UUID uuid) {
    String name = Bukkit.getOfflinePlayer(uuid).getName();
    return name != null ? name : uuid.toString();
  }

  private void announce(String path, String player, int rank, String time, String winner) {
    Map<String, String> placeholders = new HashMap<>();
    placeholders.put("player", player);
    placeholders.put("rank", String.valueOf(rank));
    placeholders.put("time", time);
    if (winner != null) {
      placeholders.put("winner", winner);
    }
    Bukkit.broadcast(plugin.getMessageManager().getMessageComponent(path, placeholders));
  }
}
