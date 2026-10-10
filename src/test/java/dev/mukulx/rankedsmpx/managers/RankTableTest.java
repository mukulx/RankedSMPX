package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RankTableTest {

  private static final Logger LOG = Logger.getLogger("test");

  private static RankTable parse(String yaml) {
    YamlConfiguration c = new YamlConfiguration();
    try {
      c.loadFromString(yaml);
    } catch (InvalidConfigurationException e) {
      throw new AssertionError(e);
    }
    return RankTable.parse(c.getConfigurationSection("ranks"));
  }

  @Test
  @DisplayName("Defaults keep the shipped per-rank values")
  void defaultsMatchShippedValues() {
    RankTable t = RankTable.defaults(20);
    assertEquals(20, t.totalRanks());
    assertEquals(20.0, t.row(1).hearts());
    assertEquals(10.5, t.row(20).hearts());
    assertEquals(3.0, t.row(1).xp());
    assertEquals(1.1, t.row(20).xp(), 1e-9);
    assertEquals(1.96, t.row(2).potion());
    assertEquals(54, t.row(2).slots());
    assertEquals(15, t.row(10).slots());
    assertEquals(0, t.row(11).slots());
    assertEquals("&e[#7] &r", t.row(7).prefix());
    assertEquals(10.0, t.unranked().hearts());
    assertEquals("&0[&8BLACKLISTED&0] &r", t.blacklisted().prefix());
  }

  @Test
  @DisplayName("Rendered file parses back to the same table")
  void renderRoundTrips() {
    RankTable original = RankTable.defaults(20).withPrefixes(Map.of(3, "&#FFD700[\"x\"\\] &r"));
    RankTable back = parse(original.render());
    assertEquals(original.totalRanks(), back.totalRanks());
    for (int rank = -1; rank <= 20; rank++) {
      assertEquals(original.row(rank), back.row(rank), "row " + rank);
    }
    assertTrue(back.warnings().isEmpty(), back.warnings().toString());
  }

  @Test
  @DisplayName("Adding row 21 and 22 grows the rank pool")
  void extraRowsExtendPool() {
    String yaml = RankTable.defaults(20).render() + "  21: { hearts: 10.0, xp: 1.0, potion: 1.0, slots: 0, prefix: \"&c[#21] &r\", suffix: \"\" }\n"
        + "  22: { hearts: 9.5 }\n";
    RankTable t = parse(yaml);
    assertEquals(22, t.totalRanks());
    assertEquals("&c[#21] &r", t.row(21).prefix());
    assertEquals(9.5, t.row(22).hearts());
    assertEquals(1.0, t.row(22).xp(), "missing column copies the row above");
    assertEquals("&e[#22] &r", t.row(22).prefix(), "missing prefix defaults to [#N]");
  }

  @Test
  @DisplayName("A gap in the rows uses the nearest lower row")
  void gapFallsBackToLowerRow() {
    RankTable t = parse("ranks:\n  1: { hearts: 20.0 }\n  3: { hearts: 18.0 }\n");
    assertEquals(3, t.totalRanks());
    assertEquals(20.0, t.row(2).hearts());
    assertEquals(18.0, t.row(3).hearts());
  }

  @Test
  @DisplayName("Bad values warn and fall back instead of breaking")
  void invalidValuesAreContained() {
    RankTable t =
        parse("ranks:\n  1: { hearts: 20.0, xp: 3.0, potion: 2.0, slots: 99 }\n  2: { hearts: -4, xp: abc, slots: -1 }\n  nope: { hearts: 5 }\n");
    assertEquals(54, t.row(1).slots(), "slots clamp to 54");
    assertEquals(20.0, t.row(2).hearts(), "non-positive hearts copy the row above");
    assertEquals(3.0, t.row(2).xp());
    assertEquals(0, t.row(2).slots());
    assertEquals(2, t.totalRanks());
    assertFalse(t.warnings().isEmpty());
  }

  @Test
  @DisplayName("No usable ranked rows falls back to built-in defaults")
  void emptyFallsBackToDefaults() {
    assertEquals(20, parse("ranks:\n  0: { hearts: 10.0 }\n").totalRanks());
    assertEquals(20, RankTable.parse(null).totalRanks());
  }

  @Test
  @DisplayName("Blacklisted row only needs prefix and suffix and keeps its own values")
  void blacklistedRow() {
    RankTable t = parse("ranks:\n  -1: { prefix: \"&4BANNED \", suffix: \"!\" }\n  1: { hearts: 20.0 }\n");
    assertEquals("&4BANNED ", t.blacklisted().prefix());
    assertEquals("!", t.blacklisted().suffix());
    assertEquals(1, t.totalRanks());
    assertTrue(RankTable.defaults(20).render().contains("-1: { prefix:"));
  }

  @Test
  @DisplayName("Prefix updates only touch rows that exist")
  void withPrefixes() {
    RankTable t = RankTable.defaults(3).withPrefixes(Map.of(0, "U", 2, "two", 99, "ghost"));
    assertEquals("U", t.unranked().prefix());
    assertEquals("two", t.row(2).prefix());
    assertEquals(3, t.totalRanks());
  }

  @Test
  @DisplayName("Migrating an old config keeps custom values, linear defaults and prefixes")
  void migratesLegacyConfig(@TempDir File dir) throws Exception {
    YamlConfiguration old = new YamlConfiguration();
    old.loadFromString(
        """
        config-version: 16
        season:
          total-ranks: 22
        health:
          max-hearts: 20.0
          min-hearts: 10.0
          unranked-hearts: 9.0
          custom-hearts:
            1: 25.0
        potions:
          rank-1-multiplier: 2.0
          rank-20-multiplier: 1.0
          custom-multipliers:
            2: 1.5
        xp:
          rank-1-multiplier: 3.0
          rank-20-multiplier: 1.0
          unranked-multiplier: 1.2
        inventory:
          slots:
            1: 54
            2: 36
        tab-integration:
          unranked-prefix: '&7NONE '
          blacklisted-prefix: '&4BAN '
          default-suffix: ' &6*'
          rank-prefixes:
            1: '&6[1] '
          rank-suffixes:
            2: ' &b!'
        """);
    File file = new File(dir, "ranks.yml");

    RankTable t = RankTableFile.loadOrCreate(file, old, LOG);

    assertTrue(file.exists());
    assertEquals(22, t.totalRanks());
    assertEquals(25.0, t.row(1).hearts(), "custom hearts win");
    assertEquals(10.0, t.row(22).hearts(), 0.01, "last rank reaches min-hearts");
    assertEquals(1.5, t.row(2).potion(), "custom potion wins");
    assertEquals(2.0, t.row(1).potion(), "linear first value");
    assertEquals(1.2, t.unranked().xp());
    assertEquals(9.0, t.unranked().hearts());
    assertEquals(36, t.row(2).slots());
    assertEquals(0, t.row(3).slots());
    assertEquals("&6[1] ", t.row(1).prefix());
    assertEquals("&e[#2] &r", t.row(2).prefix());
    assertEquals(" &b!", t.row(2).suffix());
    assertEquals(" &6*", t.row(3).suffix(), "default-suffix applies to ranks without an override");
    assertEquals("&7NONE ", t.unranked().prefix());
    assertEquals("&4BAN ", t.blacklisted().prefix());

    RankTable reloaded = RankTableFile.loadOrCreate(file, new YamlConfiguration(), LOG);
    for (int rank = -1; rank <= 22; rank++) {
      assertEquals(t.row(rank), reloaded.row(rank), "round trip row " + rank);
    }
  }

  @Test
  @DisplayName("Migration never overwrites an existing ranks.yml")
  void existingFileWins(@TempDir File dir) throws Exception {
    File file = new File(dir, "ranks.yml");
    Files.writeString(file.toPath(), "ranks:\n  1: { hearts: 7.0 }\n");
    YamlConfiguration old = new YamlConfiguration();
    old.set("config-version", 16);
    old.set("health.custom-hearts.1", 25.0);

    RankTable t = RankTableFile.loadOrCreate(file, old, LOG);

    assertEquals(7.0, t.row(1).hearts());
    assertEquals("ranks:\n  1: { hearts: 7.0 }\n", Files.readString(file.toPath()));
  }

  @Test
  @DisplayName("Fresh install and unreadable file use defaults without touching the broken file")
  void freshAndBrokenFiles(@TempDir File dir) throws Exception {
    YamlConfiguration fresh = new YamlConfiguration();
    fresh.set("config-version", 17);
    File file = new File(dir, "ranks.yml");
    assertEquals(20, RankTableFile.loadOrCreate(file, fresh, LOG).totalRanks());
    assertTrue(file.exists());

    File broken = new File(dir, "broken.yml");
    Files.writeString(broken.toPath(), "ranks: [unclosed");
    assertEquals(20, RankTableFile.loadOrCreate(broken, fresh, LOG).totalRanks());
    assertEquals("ranks: [unclosed", Files.readString(broken.toPath()));
  }

  @Test
  @DisplayName("A new rank can be just its number")
  void emptyRowAddsRank() {
    String yaml = RankTable.defaults(20).render().replace("\n  20:", "\n  21: {}\n  22: { hearts: 5.0 }\n  20:");
    RankTable t = parse(yaml);
    assertEquals(22, t.totalRanks());
    assertEquals(t.row(20).hearts(), t.row(21).hearts());
    assertEquals(t.row(20).xp(), t.row(21).xp());
    assertEquals("&e[#21] &r", t.row(21).prefix());
    assertEquals(5.0, t.row(22).hearts());
    assertEquals("&e[#22] &r", t.row(22).prefix());
    assertTrue(t.warnings().isEmpty(), t.warnings().toString());
  }

  @Test
  @DisplayName("Gaps between rank numbers produce a warning")
  void gapWarns() {
    RankTable t = parse("ranks:\n  1: { hearts: 20 }\n  2: { hearts: 19 }\n  5: { hearts: 10 }\n");
    assertEquals(5, t.totalRanks());
    assertTrue(t.warnings().stream().anyMatch(w -> w.contains("3-4")), t.warnings().toString());
  }

  @Test
  @DisplayName("Bundled ranks.yml matches the generated default file")
  void bundledFileMatchesGenerated() throws Exception {
    try (var in = getClass().getClassLoader().getResourceAsStream("ranks.yml")) {
      assertNotNull(in);
      assertEquals(RankTable.defaults(20).render(), new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
  }
}
