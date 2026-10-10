package dev.mukulx.rankedsmpx.dialogs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Every setting a dialog reads or writes must exist in config.yml or items.yml. */
class DialogConfigKeysTest {

  private static YamlConfiguration load(String name) throws Exception {
    try (InputStream in = DialogConfigKeysTest.class.getResourceAsStream("/" + name)) {
      return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("Dialog settings point at keys that exist")
  void keysExist() throws Exception {
    YamlConfiguration config = load("config.yml");
    YamlConfiguration items = load("items.yml");
    Pattern direct = Pattern.compile("(?:DialogSupport\\.put\\(values,|config\\.get(?:Boolean|Double)\\()\\s*\"([a-z0-9.-]+)\"");
    Pattern relative = Pattern.compile("(?:PATH \\+|values\\.put\\(PATH \\+)\\s*\"([a-z0-9.-]+)\"");
    Set<String> paths = new TreeSet<>();
    try (Stream<Path> files = Files.walk(Path.of("src/main/java/dev/mukulx/rankedsmpx/dialogs"))) {
      for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
        String text = Files.readString(file);
        boolean hammer = file.getFileName().toString().startsWith("Hammer");
        for (Matcher m = direct.matcher(text); m.find(); ) {
          paths.add(m.group(1));
        }
        if (hammer) {
          for (Matcher m = relative.matcher(text); m.find(); ) {
            paths.add("hierarchy-hammer." + m.group(1));
          }
        }
      }
    }
    assertFalse(paths.isEmpty());
    for (String path : paths) {
      assertTrue(
          config.isSet(path) || items.isSet(path), "dialog uses a key that is in no yml file: " + path);
      assertFalse(
          config.isConfigurationSection(path) && !path.contains("."),
          "dialog treats a section as a single value: " + path);
    }
  }
}
