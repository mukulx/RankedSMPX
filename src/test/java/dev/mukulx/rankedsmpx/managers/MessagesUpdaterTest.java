package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessagesUpdaterTest {

  @TempDir File dir;

  private MessagesUpdater updater() {
    RankedSMPXPlugin plugin = mock(RankedSMPXPlugin.class);
    when(plugin.getDataFolder()).thenReturn(dir);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    when(plugin.getResource(anyString())).thenAnswer(inv -> getClass().getResourceAsStream("/" + inv.<String>getArgument(0)));
    return new MessagesUpdater(plugin);
  }

  @Test
  @DisplayName("New messages are added, edited text and comments stay, and no version key appears")
  void addsMissingKeysOnly() throws Exception {
    File file = new File(dir, "messages.yml");
    Files.writeString(
        file.toPath(),
        "# my note\nsettings:\n  prefix: \"&8[&aMine&8]&r \"\n",
        StandardCharsets.UTF_8);

    int added = updater().update(file);

    assertTrue(added > 0, "bundled messages were merged in");
    String text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    assertTrue(text.contains("# my note"));
    YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
    assertEquals("&8[&aMine&8]&r ", yml.getString("settings.prefix"));
    assertTrue(yml.contains("hammer.dropped"));
    assertFalse(yml.contains("config-version"));
    assertEquals(1, new File(dir, "backups").list().length);
  }

  @Test
  @DisplayName("A messages.yml that already has everything is left alone")
  void secondRunChangesNothing() throws Exception {
    File file = new File(dir, "messages.yml");
    Files.writeString(file.toPath(), "settings:\n  prefix: \"x\"\n", StandardCharsets.UTF_8);
    updater().update(file);
    String first = Files.readString(file.toPath(), StandardCharsets.UTF_8);

    assertEquals(0, updater().update(file));
    assertEquals(first, Files.readString(file.toPath(), StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("One message missing inside an existing section is restored")
  void restoresSingleKey() throws Exception {
    File file = new File(dir, "messages.yml");
    try (var in = getClass().getResourceAsStream("/messages.yml")) {
      Files.write(file.toPath(), in.readAllBytes());
    }
    String full = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    String cut = full.lines().filter(l -> !l.startsWith("  dropped-subtitle:")).collect(java.util.stream.Collectors.joining("\n")) + "\n";
    assertNotEquals(full, cut);
    Files.writeString(file.toPath(), cut, StandardCharsets.UTF_8);

    assertEquals(1, updater().update(file));
    assertNotNull(YamlConfiguration.loadConfiguration(file).getString("hammer.dropped-subtitle"));
  }
}
