package dev.mukulx.rankedsmpx.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DurationParserTest {

  @Test
  @DisplayName("Days, hours, minutes and combinations are read")
  void parses() {
    assertEquals(86_400_000L, DurationParser.parseMillis("1d").getAsLong());
    assertEquals(12 * 3_600_000L, DurationParser.parseMillis("12h").getAsLong());
    assertEquals(36 * 3_600_000L, DurationParser.parseMillis("1d12h").getAsLong());
    assertEquals(36 * 3_600_000L, DurationParser.parseMillis(" 1D 12H ").getAsLong());
    assertEquals(30 * 60_000L, DurationParser.parseMillis("30m").getAsLong());
  }

  @Test
  @DisplayName("A bare number counts as hours")
  void bareNumber() {
    assertEquals(48 * 3_600_000L, DurationParser.parseMillis("48").getAsLong());
  }

  @Test
  @DisplayName("Nonsense is rejected")
  void invalid() {
    assertTrue(DurationParser.parseMillis(null).isEmpty());
    assertTrue(DurationParser.parseMillis("").isEmpty());
    assertTrue(DurationParser.parseMillis("soon").isEmpty());
    assertTrue(DurationParser.parseMillis("5 days").isEmpty());
    assertTrue(DurationParser.parseMillis("1d xyz").isEmpty());
  }
}
