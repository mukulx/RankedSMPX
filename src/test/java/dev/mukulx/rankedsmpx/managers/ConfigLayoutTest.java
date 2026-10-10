package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLayoutTest {

  @TempDir File dir;
  private JavaPlugin plugin;

  @BeforeEach
  void setUp() {
    plugin = mock(JavaPlugin.class);
    when(plugin.getDataFolder()).thenReturn(dir);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    when(plugin.getResource(anyString()))
        .thenAnswer(inv -> getClass().getResourceAsStream("/" + inv.<String>getArgument(0)));
    doAnswer(
            inv -> {
              try (InputStream in = getClass().getResourceAsStream("/" + inv.<String>getArgument(0))) {
                Files.copy(in, new File(dir, inv.<String>getArgument(0)).toPath(), StandardCopyOption.REPLACE_EXISTING);
              }
              return null;
            })
        .when(plugin)
        .saveResource(anyString(), anyBoolean());
  }

  private File writeOldConfig() throws Exception {
    String text;
    try (InputStream in = getClass().getResourceAsStream("/config-v16.yml")) {
      text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    text = text.replace("dash-speed: 1.0", "dash-speed: 1.7");
    text = text.replace("    cooldown: 0\n", "    cooldown: 30\n");
    text = text.replace("debug: false", "debug: true");
    text = text.replace("    LUNGE: 3\n", "    LUNGE: 3\n    SHARPNESS: 5\n");
    text = text.replace("    - LEVITATION\n", "    - LEVITATION\n    - SLOW_FALLING\n");
    text = text.replace("  mode: MODIFIER", "  mode: DIRECT");
    File config = new File(dir, "config.yml");
    Files.writeString(config.toPath(), text);
    return config;
  }

  @Test
  @DisplayName("Old single-file config is split without losing any edited value")
  void migratesSingleFileConfig() throws Exception {
    File config = writeOldConfig();

    ConfigLayout.migrate(plugin);

    assertTrue(new File(dir, "items.yml").exists());
    assertTrue(new File(dir, "ranks.yml").exists());
    assertTrue(new File(dir, "backups").list((d, n) -> n.startsWith("config_v16_")).length == 1);

    SplitConfiguration c = ConfigLayout.load(plugin);
    assertEquals(1.7, c.getDouble("hierarchy-hammer.dash-speed"));
    assertEquals(30, c.getInt("season.rank-stealing.cooldown"));
    assertTrue(c.getBoolean("debug"));
    assertEquals(5, c.getInt("spear.enchantments.SHARPNESS"), "owner-added enchantment survives");
    assertEquals(3, c.getInt("spear.enchantments.LUNGE"));
    assertEquals("DIRECT", c.getString("health.mode"));
    assertTrue(c.getStringList("potions.ignored-effects").contains("SLOW_FALLING"));
    assertEquals(ConfigUpdater.getTargetVersion(), c.getInt("config-version"));

    String main = Files.readString(config.toPath());
    assertFalse(main.contains("hierarchy-hammer:"), "hammer moved out of config.yml");
    assertTrue(main.contains("health:"));
    assertFalse(main.contains("custom-hearts"), "per-rank tables are gone");
    assertFalse(main.contains("█"), "banners are gone");
    assertTrue(main.lines().count() > 150, "config.yml contains the full settings reference");
    assertTrue(Files.readString(new File(dir, "items.yml").toPath()).contains("# Dash speed multiplier."),
        "template comments are kept");
  }

  @Test
  @DisplayName("The shipped v17 single-file config is also migrated")
  void migratesPreviousLatestVersion() throws Exception {
    File config = writeOldConfig();
    Files.writeString(config.toPath(), Files.readString(config.toPath()).replace("config-version: 16", "config-version: 17"));

    ConfigLayout.migrate(plugin);

    assertTrue(new File(dir, "items.yml").exists());
    assertEquals(ConfigUpdater.getTargetVersion(), YamlConfiguration.loadConfiguration(config).getInt("config-version"));
    assertEquals(1.7, ConfigLayout.load(plugin).getDouble("hierarchy-hammer.dash-speed"));
  }

  @Test
  @DisplayName("The v18 advanced file is merged into config.yml and backed up")
  void mergesAdvancedFileIntoConfig() throws Exception {
    File config = new File(dir, "config.yml");
    Files.writeString(config.toPath(), "config-version: 18\ndebug: true\n");
    File advanced = new File(dir, "advanced.yml");
    Files.writeString(
        advanced.toPath(),
        "health:\n  mode: DIRECT\ndatabase:\n  type: mysql\n");

    ConfigLayout.migrate(plugin);
    SplitConfiguration c = ConfigLayout.load(plugin);

    assertEquals("DIRECT", c.getString("health.mode"));
    assertEquals("mysql", c.getString("database.type"));
    assertTrue(c.getBoolean("debug"));
    assertEquals(ConfigUpdater.getTargetVersion(), c.getInt("config-version"));
    assertFalse(advanced.exists());
    assertTrue(new File(dir, "backups").list((d, n) -> n.startsWith("advanced_v18_")).length == 1);
  }

  @Test
  @DisplayName("Migration runs once: a second start changes nothing")
  void migrationIsIdempotent() throws Exception {
    writeOldConfig();
    ConfigLayout.migrate(plugin);
    String items = Files.readString(new File(dir, "items.yml").toPath());
    String main = Files.readString(new File(dir, "config.yml").toPath());
    File[] backups = new File(dir, "backups").listFiles();

    ConfigLayout.migrate(plugin);

    assertEquals(items, Files.readString(new File(dir, "items.yml").toPath()));
    assertEquals(main, Files.readString(new File(dir, "config.yml").toPath()));
    assertEquals(backups.length, new File(dir, "backups").listFiles().length);
  }

  @Test
  @DisplayName("Fresh install creates all files from the templates")
  void freshInstall() throws Exception {
    doAnswer(inv -> { plugin.saveResource("config.yml", false); return null; }).when(plugin).saveDefaultConfig();
    plugin.saveDefaultConfig();
    ConfigLayout.migrate(plugin);

    SplitConfiguration c = ConfigLayout.load(plugin);
    assertTrue(new File(dir, "ranks.yml").exists());
    assertFalse(new File(dir, "advanced.yml").exists());
    assertEquals(1.0, c.getDouble("hierarchy-hammer.dash-speed"));
    assertEquals("MODIFIER", c.getString("health.mode"));
    assertTrue(c.getBoolean("season.rank-stealing.enabled"));
    assertEquals(0, new File(dir, "backups").listFiles() == null ? 0 : new File(dir, "backups").listFiles().length);
  }

  @Test
  @DisplayName("Reads, writes and defaults are routed to the owning file")
  void routing() throws Exception {
    ConfigLayout.migrate(plugin); // no config.yml yet: nothing to do
    SplitConfiguration c = ConfigLayout.load(plugin);

    new File(dir, "items.yml").delete();
    Files.writeString(new File(dir, "items.yml").toPath(), "spear:\n  enabled: false\n");
    c = ConfigLayout.load(plugin);
    assertFalse(c.getBoolean("spear.enabled"));
    assertEquals(1.0, c.getDouble("hierarchy-hammer.dash-speed"), "missing key falls back to bundled default");

    c.set("spear.enabled", true);
    c.set("debug", true);
    c.set("health.mode", "AGGRESSIVE");
    c.saveAll();

    assertTrue(YamlConfiguration.loadConfiguration(new File(dir, "items.yml")).getBoolean("spear.enabled"));
    assertTrue(YamlConfiguration.loadConfiguration(new File(dir, "config.yml")).getBoolean("debug"));
    assertEquals("AGGRESSIVE", YamlConfiguration.loadConfiguration(new File(dir, "config.yml")).getString("health.mode"));
    assertNull(YamlConfiguration.loadConfiguration(new File(dir, "config.yml")).get("spear"));

    ConfigurationSection hammer = c.getConfigurationSection("hierarchy-hammer");
    assertNotNull(hammer);
    assertEquals(List.of(), c.getStringList("not.there"));
  }

  @Test
  @DisplayName("Every key the plugin reads from the templates sits in the file ConfigLayout names")
  void templatesMatchOwnership() throws Exception {
    for (String file : new String[] {"config.yml", "items.yml"}) {
      YamlConfiguration y;
      try (var in = getClass().getResourceAsStream("/" + file)) {
        y = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
      }
      for (String key : y.getKeys(false)) {
        if (key.equals("config-version")) continue;
        assertEquals(file, ConfigLayout.fileFor(key), key + " in " + file);
      }
    }
  }
}
