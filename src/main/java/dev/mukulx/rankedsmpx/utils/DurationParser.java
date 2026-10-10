package dev.mukulx.rankedsmpx.utils;

import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/** Reads durations such as {@code 30m}, {@code 12h}, {@code 7d} or {@code 1d12h}. */
public final class DurationParser {

  private static final Pattern PART = Pattern.compile("(\\d+)\\s*([dhms])");

  private DurationParser() {}

  /**
   * @return the duration in milliseconds, or empty if the text is not a duration. A bare number
   *     counts as hours.
   */
  public static OptionalLong parseMillis(@Nullable String text) {
    if (text == null) {
      return OptionalLong.empty();
    }
    String value = text.trim().toLowerCase();
    if (value.matches("\\d+")) {
      return OptionalLong.of(Long.parseLong(value) * 3_600_000L);
    }
    Matcher matcher = PART.matcher(value);
    long total = 0;
    int end = 0;
    while (matcher.find()) {
      if (matcher.start() != end && !value.substring(end, matcher.start()).isBlank()) {
        return OptionalLong.empty();
      }
      end = matcher.end();
      long amount = Long.parseLong(matcher.group(1));
      total +=
          amount
              * switch (matcher.group(2)) {
                case "d" -> 86_400_000L;
                case "h" -> 3_600_000L;
                case "m" -> 60_000L;
                default -> 1_000L;
              };
    }
    if (end == 0 || !value.substring(end).isBlank()) {
      return OptionalLong.empty();
    }
    return OptionalLong.of(total);
  }
}
