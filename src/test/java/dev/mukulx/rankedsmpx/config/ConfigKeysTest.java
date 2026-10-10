package dev.mukulx.rankedsmpx.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Keeps the shipped yml files and the code in step: every option the code reads must exist in the
 * bundled file, and every option in the file must be read somewhere.
 */
class ConfigKeysTest {

  private static final Pattern READ =
      Pattern.compile(
          "(getConfig\\(\\)|getDiscordConfig\\(\\))\\s*\\.(?:getString|getInt|getBoolean|getDouble|getLong"
              + "|getStringList|getConfigurationSection)\\(\\s*\"([a-z0-9._-]+)\"");

  /** Options read through a dynamic path or accepted as an old alias. */
  private static final Set<String> ALIASES =
      Set.of("hierarchy-hammer.auto-give", "hierarchy-hammer.give-on-start", "debug");

  private static final List<String> DYNAMIC_PREFIXES =
      List.of(
          "sounds.", "rank-drop.", "potions.", "spear.enchantments", "database.", "hierarchy-hammer.lore",
          "hierarchy-hammer.death-effect.", "role-sync.colors.", "role-sync.rank-role-ids", "commands.",
          "bot.", "extra-inventory.", "locator.");

  private static String sources() throws IOException {
    try (Stream<Path> files = Files.walk(Path.of("src/main/java"))) {
      return files
          .filter(p -> p.toString().endsWith(".java") && !p.toString().contains("metrics"))
          .map(
              p -> {
                try {
                  return Files.readString(p);
                } catch (IOException e) {
                  throw new IllegalStateException(e);
                }
              })
          .collect(Collectors.joining("\n"));
    }
  }

  private static Set<String> keys(String resource) {
    return YamlConfiguration.loadConfiguration(new File("src/main/resources/" + resource)).getKeys(true);
  }

  @Test
  @DisplayName("Every option the code reads exists in the bundled yml")
  void codeReadsOnlyShippedKeys() throws IOException {
    Set<String> main = keys("config.yml");
    main.addAll(keys("items.yml"));
    main.addAll(keys("custom.yml"));
    Set<String> discord = keys("discord.yml");

    Set<String> missing = new TreeSet<>();
    Matcher m = READ.matcher(sources());
    while (m.find()) {
      String key = m.group(2);
      Set<String> known = m.group(1).startsWith("getDiscord") ? discord : main;
      if (!known.contains(key) && !ALIASES.contains(key)) {
        missing.add(key);
      }
    }
    assertTrue(missing.isEmpty(), "read by code but not in any bundled yml: " + missing);
  }

  @Test
  @DisplayName("Every option in the bundled yml is read by the code")
  void shippedKeysAreUsed() throws IOException {
    String code = sources();
    Set<String> unused = new TreeSet<>();
    for (String file : List.of("config.yml", "items.yml", "discord.yml", "custom.yml")) {
      YamlConfiguration yml =
          YamlConfiguration.loadConfiguration(new File("src/main/resources/" + file));
      for (String key : yml.getKeys(true)) {
        if (yml.isConfigurationSection(key) && !yml.getConfigurationSection(key).getKeys(false).isEmpty()) {
          continue;
        }
        String leaf = key.substring(key.lastIndexOf('.') + 1);
        boolean dynamic = DYNAMIC_PREFIXES.stream().anyMatch(key::startsWith);
        if (!dynamic && !key.equals("config-version") && !code.contains("\"" + key + "\"") && !code.contains("\"" + leaf + "\"")) {
          unused.add(file + ": " + key);
        }
      }
    }
    assertTrue(unused.isEmpty(), "in a yml but never read by the code: " + unused);
  }
}
