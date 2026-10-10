package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InactivityManagerTest {

  private static final long HOUR = 3_600_000L;
  private static final UUID LONG_GONE = new UUID(0, 1);
  private static final UUID RECENT = new UUID(0, 2);
  private static final UUID ONLINE = new UUID(0, 3);
  private static final UUID EXEMPT = new UUID(0, 4);
  private static final UUID NEVER = new UUID(0, 5);

  @Test
  @DisplayName("Only offline, non-exempt players past the limit are picked, best rank first")
  void picksInactive() {
    long now = 1_000 * HOUR;
    Map<UUID, Integer> ranks = Map.of(LONG_GONE, 7, RECENT, 2, ONLINE, 1, EXEMPT, 3, NEVER, 4);
    Map<UUID, Long> seen =
        Map.of(
            LONG_GONE, now - 200 * HOUR,
            RECENT, now - 5 * HOUR,
            ONLINE, now - 900 * HOUR,
            EXEMPT, now - 900 * HOUR,
            NEVER, 0L);

    List<UUID> result =
        InactivityManager.findInactive(
            ranks, seen::get, ONLINE::equals, EXEMPT::equals, now, 168 * HOUR);

    assertEquals(List.of(LONG_GONE), result);
  }

  @Test
  @DisplayName("Several inactive players are returned in rank order")
  void orderedByRank() {
    long now = 1_000 * HOUR;
    UUID a = new UUID(1, 1);
    UUID b = new UUID(1, 2);
    List<UUID> result =
        InactivityManager.findInactive(
            Map.of(a, 9, b, 3), uuid -> now - 500 * HOUR, u -> false, u -> false, now, 24 * HOUR);
    assertEquals(List.of(b, a), result);
  }

  @Test
  @DisplayName("A player exactly at the limit counts as inactive")
  void boundary() {
    long now = 100 * HOUR;
    List<UUID> result =
        InactivityManager.findInactive(
            Map.of(RECENT, 1), uuid -> now - 24 * HOUR, u -> false, u -> false, now, 24 * HOUR);
    assertEquals(List.of(RECENT), result);
    assertTrue(Set.copyOf(result).contains(RECENT));
  }

  @Test
  @DisplayName("Server downtime does not count: nobody loses a rank right after a long outage")
  void downtimeIsIgnored() {
    long now = 1_000 * HOUR;
    long serverStarted = now - 1 * HOUR;
    long lastPlayed = now - 400 * HOUR;

    assertEquals(serverStarted, InactivityManager.effectiveLastSeen(lastPlayed, serverStarted));
    assertEquals(0, InactivityManager.effectiveLastSeen(0, serverStarted));

    List<UUID> result =
        InactivityManager.findInactive(
            Map.of(RECENT, 1),
            uuid -> InactivityManager.effectiveLastSeen(lastPlayed, serverStarted),
            u -> false,
            u -> false,
            now,
            168 * HOUR);
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("The shipped config has the inactivity rule switched off")
  void offByDefault() throws Exception {
    try (var in = getClass().getResourceAsStream("/config.yml")) {
      var yml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
          new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
      assertFalse(yml.getBoolean("season.inactivity.enabled", true));
      assertTrue(yml.contains("season.inactivity.enabled"));
    }
  }
}
