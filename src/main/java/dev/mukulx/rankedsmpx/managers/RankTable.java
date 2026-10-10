package dev.mukulx.rankedsmpx.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

public final class RankTable {

  public static final int BLACKLISTED = -1;
  public static final int UNRANKED = 0;
  public static final int MAX_SLOTS = 54;

  public static final String HEADER =
      """
      # ============================================================================
      #  RankedSMPX - ranks.yml
      #  One line per rank. After editing, run /rsmp reload.
      # ============================================================================
      #
      #  ROWS
      #    -1   blacklisted players (only prefix and suffix are used)
      #     0   unranked players
      #    1+   ranked players, 1 is the top rank
      #
      #  COLUMNS
      #    hearts  max health in hearts (1 heart = 2 HP)
      #    xp      XP multiplier
      #    potion  potion duration multiplier
      #    slots   extra /inv slots, 0 = none, max 54
      #    prefix  tab list / chat prefix (&-codes or MiniMessage)
      #    suffix  tab list suffix
      #
      #  ADD A RANK
      #    Add one more line with the next number. That is all.
      #    Everything you leave out is filled in for you:
      #      hearts, xp, potion, slots, suffix  copied from the rank above
      #      prefix                             "&e[#<number>] &r"
      #    So the shortest possible new rank is:   21: {}
      #    or with your own values:                21: { hearts: 10.0, xp: 1.0 }
      #
      #  The number of ranks is the highest row. Use 1, 2, 3... with no gaps.
      #  Raising it makes more ranks available; the season start, auto-assign,
      #  /rsmp commands, Discord roles, leaderboard, LuckPerms and the database
      #  all follow. Players keep their rank when you add rows.
      #  Do not lower the highest row mid-season: players holding a rank above
      #  it lose that rank the next time /rsmp fix runs.
      # ============================================================================
      """;

  private static final double[] DEFAULT_POTION = {
    2.00, 1.96, 1.91, 1.86, 1.81, 1.76, 1.71, 1.66, 1.61, 1.56,
    1.51, 1.46, 1.41, 1.36, 1.31, 1.26, 1.21, 1.16, 1.11, 1.05
  };
  private static final int[] DEFAULT_SLOTS = {54, 54, 50, 45, 40, 35, 30, 25, 20, 15};

  public record Row(
      double hearts, double xp, double potion, int slots, @NotNull String prefix, @NotNull String suffix) {

    public Row withPrefix(@NotNull String newPrefix) {
      return new Row(hearts, xp, potion, slots, newPrefix, suffix);
    }
  }

  private final NavigableMap<Integer, Row> rows;
  private final List<String> warnings;

  private RankTable(NavigableMap<Integer, Row> rows, List<String> warnings) {
    this.rows = Collections.unmodifiableNavigableMap(rows);
    this.warnings = List.copyOf(warnings);
  }

  @NotNull
  public static RankTable defaults(int ranks) {
    NavigableMap<Integer, Row> rows = new TreeMap<>();
    rows.put(BLACKLISTED, new Row(10.0, 1.0, 1.0, 0, "&0[&8BLACKLISTED&0] &r", ""));
    rows.put(UNRANKED, new Row(10.0, 1.0, 1.0, 0, "&e[&7UNRANKED&e] &r", ""));
    for (int rank = 1; rank <= Math.max(1, ranks); rank++) {
      rows.put(rank, defaultRow(rank, ranks));
    }
    return new RankTable(rows, List.of());
  }

  private static Row defaultRow(int rank, int ranks) {
    double hearts = round(20.0 - 0.5 * (rank - 1));
    double xp = round(3.0 - 0.1 * (rank - 1));
    double potion =
        rank <= DEFAULT_POTION.length ? DEFAULT_POTION[rank - 1] : DEFAULT_POTION[DEFAULT_POTION.length - 1];
    int slots = rank <= DEFAULT_SLOTS.length ? DEFAULT_SLOTS[rank - 1] : 0;
    return new Row(Math.max(hearts, 1.0), Math.max(xp, 1.0), potion, slots, "&e[#" + rank + "] &r", "");
  }

  /**
   * Parses the {@code ranks} section. Missing columns copy the row below; unusable values fall back
   * the same way and are reported in {@link #warnings()}. Never throws and never returns an empty
   * ranked set: a table with no usable rank falls back to {@link #defaults(int)}.
   */
  @NotNull
  public static RankTable parse(ConfigurationSection ranksSection) {
    List<String> warnings = new ArrayList<>();
    if (ranksSection == null) {
      warnings.add("ranks.yml has no 'ranks' section, using built-in defaults");
      return withWarnings(defaults(20), warnings);
    }

    NavigableMap<Integer, ConfigurationSection> raw = new TreeMap<>();
    for (String key : ranksSection.getKeys(false)) {
      ConfigurationSection row = ranksSection.getConfigurationSection(key);
      Integer rank = parseRank(key);
      if (rank == null || rank < BLACKLISTED || row == null) {
        warnings.add("Ignoring invalid rank row '" + key + "' (use -1, 0, 1, 2, ...)");
        continue;
      }
      raw.put(rank, row);
    }

    if (raw.lastKey() < 1) {
      warnings.add("ranks.yml has no ranked rows (1, 2, ...), using built-in defaults");
      return withWarnings(defaults(20), warnings);
    }

    int expected = 1;
    for (int rank : raw.tailMap(1).keySet()) {
      if (rank > expected) {
        warnings.add(
            "Ranks " + (rank > expected + 1 ? expected + "-" + (rank - 1) : String.valueOf(expected))
                + " have no row; add them or they copy the row below. Rank rows must be numbered 1, 2, 3... with no gaps.");
      }
      expected = rank + 1;
    }

    NavigableMap<Integer, Row> rows = new TreeMap<>();
    Row previous = defaults(1).rows.get(UNRANKED);
    for (Map.Entry<Integer, ConfigurationSection> entry : raw.entrySet()) {
      int rank = entry.getKey();
      Row row = resolve(rank, entry.getValue(), previous, warnings);
      rows.put(rank, row);
      if (rank >= UNRANKED) {
        previous = row;
      }
    }
    rows.putIfAbsent(BLACKLISTED, defaults(1).rows.get(BLACKLISTED));
    rows.putIfAbsent(UNRANKED, defaults(1).rows.get(UNRANKED));
    return new RankTable(rows, warnings);
  }

  private static RankTable withWarnings(RankTable table, List<String> warnings) {
    return new RankTable(new TreeMap<>(table.rows), warnings);
  }

  private static Integer parseRank(String key) {
    try {
      return Integer.parseInt(key.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Row resolve(int rank, ConfigurationSection s, Row fallback, List<String> warnings) {
    if (rank == BLACKLISTED) {
      Row base = defaults(1).rows.get(BLACKLISTED);
      return new Row(
          base.hearts(),
          base.xp(),
          base.potion(),
          0,
          s.getString("prefix", base.prefix()),
          s.getString("suffix", ""));
    }
    double hearts = positive(s, "hearts", rank, fallback.hearts(), warnings);
    double xp = positive(s, "xp", rank, fallback.xp(), warnings);
    double potion = positive(s, "potion", rank, fallback.potion(), warnings);
    int slots = fallback.slots();
    if (s.contains("slots")) {
      slots = s.getInt("slots", fallback.slots());
      if (slots < 0 || slots > MAX_SLOTS) {
        warnings.add("Rank " + rank + ": slots must be 0-" + MAX_SLOTS + ", got " + slots);
        slots = Math.max(0, Math.min(MAX_SLOTS, slots));
      }
    }
    String defaultPrefix = rank >= 1 ? "&e[#" + rank + "] &r" : fallback.prefix();
    String prefix = s.getString("prefix", defaultPrefix);
    String suffix = s.getString("suffix", rank >= 1 ? fallback.suffix() : "");
    return new Row(hearts, xp, potion, slots, prefix, suffix);
  }

  private static double positive(
      ConfigurationSection s, String key, int rank, double fallback, List<String> warnings) {
    if (!s.contains(key)) {
      return fallback;
    }
    double value = s.getDouble(key, Double.NaN);
    if (Double.isNaN(value) || value <= 0) {
      warnings.add("Rank " + rank + ": '" + key + "' must be above 0, using " + fallback);
      return fallback;
    }
    return value;
  }

  public int totalRanks() {
    return rows.lastKey();
  }

  @NotNull
  public Row row(int rank) {
    if (rank < 1) {
      return rows.get(rank <= BLACKLISTED ? BLACKLISTED : UNRANKED);
    }
    Map.Entry<Integer, Row> entry = rows.floorEntry(rank);
    return entry != null && entry.getKey() >= 1 ? entry.getValue() : rows.get(UNRANKED);
  }

  @NotNull
  public Row unranked() {
    return rows.get(UNRANKED);
  }

  @NotNull
  public Row blacklisted() {
    return rows.get(BLACKLISTED);
  }

  @NotNull
  public List<String> warnings() {
    return warnings;
  }

  @NotNull
  public RankTable withPrefixes(@NotNull Map<Integer, String> prefixes) {
    NavigableMap<Integer, Row> copy = new TreeMap<>(rows);
    prefixes.forEach((rank, prefix) -> copy.computeIfPresent(rank, (r, row) -> row.withPrefix(prefix)));
    return new RankTable(copy, List.of());
  }

  /** Builds a table from already-resolved rows. Used by the legacy config migration. */
  @NotNull
  static RankTable of(@NotNull NavigableMap<Integer, Row> rows) {
    return new RankTable(new TreeMap<>(rows), List.of());
  }

  @NotNull
  public String render() {
    StringBuilder out = new StringBuilder();
    out.append(HEADER);
    out.append("\nranks:\n");
    for (Map.Entry<Integer, Row> entry : rows.entrySet()) {
      Row r = entry.getValue();
      if (entry.getKey() == BLACKLISTED) {
        out.append(
            String.format(
                Locale.ROOT,
                "  %-3s { prefix: %s, suffix: %s }%n",
                "-1:",
                quote(r.prefix()),
                quote(r.suffix())));
        continue;
      }
      out.append(
          String.format(
              Locale.ROOT,
              "  %-3s { hearts: %-5s xp: %-4s potion: %-5s slots: %-4s prefix: %s, suffix: %s }%n",
              entry.getKey() + ":",
              num(r.hearts()) + ",",
              num(r.xp()) + ",",
              num(r.potion()) + ",",
              r.slots() + ",",
              quote(r.prefix()),
              quote(r.suffix())));
    }
    int next = totalRanks() + 1;
    out.append("  # ")
        .append(next)
        .append(": { hearts: 15.0, xp: 2.0, potion: 1.51, slots: 0,   prefix: \"&e[#")
        .append(next)
        .append("] &r\", suffix: \"\" }   <- example: remove the # to add rank ")
        .append(next)
        .append('\n');
    return out.toString().replace("\r\n", "\n");
  }

  private static String num(double value) {
    return Double.toString(round(value));
  }

  private static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  static double round(double value) {
    return Math.round(value * 1000.0) / 1000.0;
  }
}
