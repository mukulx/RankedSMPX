package dev.mukulx.rankedsmpx.managers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public final class RankTableFile {

  static final int FIRST_RANKS_YML_VERSION = 17;

  private RankTableFile() {}

  /**
   * Loads ranks.yml. A missing file is created first: from the per-rank values of an old config.yml
   * (version 1-16), otherwise from the built-in defaults. An unreadable file is reported and the
   * built-in defaults are used for this run; the file itself is left untouched.
   */
  @NotNull
  public static RankTable loadOrCreate(
      @NotNull File file, @NotNull FileConfiguration config, @NotNull Logger log) {
    if (!file.exists()) {
      int version = config.getInt("config-version", 0);
      boolean legacy = version >= 1 && version < FIRST_RANKS_YML_VERSION;
      RankTable created = legacy ? fromLegacy(config) : RankTable.defaults(20);
      try {
        write(file, created);
        log.info(
            legacy
                ? "Created ranks.yml from your config.yml per-rank settings (hearts, xp, potions, slots, prefixes)."
                : "Created ranks.yml");
      } catch (IOException e) {
        log.severe("Could not write ranks.yml: " + e.getMessage());
      }
      return created;
    }

    try {
      YamlConfiguration yaml = new YamlConfiguration();
      yaml.loadFromString(Files.readString(file.toPath(), StandardCharsets.UTF_8));
      RankTable table = RankTable.parse(yaml.getConfigurationSection("ranks"));
      table.warnings().forEach(w -> log.warning("ranks.yml: " + w));
      return table;
    } catch (Exception e) {
      log.severe("ranks.yml is unreadable (" + e.getMessage() + "). Using built-in defaults until it is fixed.");
      return RankTable.defaults(20);
    }
  }

  public static void write(@NotNull File file, @NotNull RankTable table) throws IOException {
    File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
    Files.writeString(tmp.toPath(), table.render(), StandardCharsets.UTF_8);
    Files.move(
        tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
  }

  @NotNull
  static RankTable fromLegacy(@NotNull FileConfiguration c) {
    int total = Math.max(1, c.getInt("season.total-ranks", 20));
    NavigableMap<Integer, RankTable.Row> rows = new TreeMap<>();

    double unrankedHearts = c.getDouble("health.unranked-hearts", 10.0);
    double unrankedPotion = c.getDouble("potions.unranked-multiplier", 1.0);
    double unrankedXp = c.getDouble("xp.unranked-multiplier", 1.0);
    String unrankedSuffix = c.getString("tab-integration.unranked-suffix", "");

    rows.put(
        RankTable.BLACKLISTED,
        new RankTable.Row(
            10.0,
            1.0,
            1.0,
            0,
            c.getString("tab-integration.blacklisted-prefix", "&0[&8BLACKLISTED&0] &r"),
            c.getString("tab-integration.blacklisted-suffix", "")));
    rows.put(
        RankTable.UNRANKED,
        new RankTable.Row(
            unrankedHearts,
            unrankedXp,
            unrankedPotion,
            0,
            c.getString("tab-integration.unranked-prefix", "&e[&7UNRANKED&e] &r"),
            unrankedSuffix));

    double maxHearts = c.getDouble("health.max-hearts", 20.0);
    double minHearts = c.getDouble("health.min-hearts", 10.5);
    double potion1 = c.getDouble("potions.rank-1-multiplier", 2.0);
    double potion20 = c.getDouble("potions.rank-20-multiplier", 1.05);
    double xp1 = c.getDouble("xp.rank-1-multiplier", 3.0);
    double xp20 = c.getDouble("xp.rank-20-multiplier", 1.1);
    String defaultSuffix = c.getString("tab-integration.default-suffix", "");
    String prefixFormat = c.getString("tab-integration.prefix-format");
    String suffixFormat = c.getString("tab-integration.suffix-format");

    for (int rank = 1; rank <= total; rank++) {
      double hearts = custom(c, "health.custom-hearts." + rank, linear(maxHearts, minHearts, rank, total));
      double potion = custom(c, "potions.custom-multipliers." + rank, linear(potion1, potion20, rank, total));
      double xp = custom(c, "xp.custom-multipliers." + rank, linear(xp1, xp20, rank, total));
      int slots = Math.max(0, Math.min(RankTable.MAX_SLOTS, c.getInt("inventory.slots." + rank, 0)));

      String prefix = c.getString("tab-integration.rank-prefixes." + rank);
      if (prefix == null) {
        prefix = c.getString("tab-integration.custom-rank-prefixes." + rank);
      }
      if (prefix == null && prefixFormat != null) {
        prefix = prefixFormat.replace("%rank%", String.valueOf(rank));
      }
      if (prefix == null) {
        prefix = "&e[#" + rank + "] &r";
      }

      String suffix = c.getString("tab-integration.rank-suffixes." + rank);
      if (suffix == null) {
        suffix = suffixFormat != null ? suffixFormat.replace("%rank%", String.valueOf(rank)) : defaultSuffix;
      }
      rows.put(rank, new RankTable.Row(hearts, xp, potion, slots, prefix, suffix));
    }
    return RankTable.of(rows);
  }

  private static double custom(ConfigurationSection c, String path, double linear) {
    double value = c.getDouble(path, -1);
    return RankTable.round(value > 0 ? value : linear);
  }

  private static double linear(double first, double last, int rank, int total) {
    return total <= 1 ? first : first - ((first - last) / (total - 1)) * (rank - 1);
  }
}
