package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigUpdaterTest {

  @TempDir Path tempDir;

  private RankedSMPXPlugin plugin;
  private ConfigUpdater configUpdater;
  private File configFile;
  private File dataFolder;

  @BeforeEach
  void setUp() throws IOException {
    plugin = mock(RankedSMPXPlugin.class);
    Logger logger = Logger.getLogger("TestLogger");
    when(plugin.getLogger()).thenReturn(logger);

    dataFolder = tempDir.resolve("plugin").toFile();
    dataFolder.mkdirs();
    when(plugin.getDataFolder()).thenReturn(dataFolder);

    configFile = new File(dataFolder, "config.yml");

    when(plugin.getResource("config.yml"))
        .thenAnswer(invocation -> getClass().getClassLoader().getResourceAsStream("config.yml"));

    when(plugin.getResource("items.yml"))
        .thenAnswer(invocation -> getClass().getClassLoader().getResourceAsStream("items.yml"));

    configUpdater = new ConfigUpdater(plugin);
  }

  @Test
  @DisplayName("items.yml gets new options added while user values and comments stay")
  void testItemsFileGetsNewKeys() throws IOException {
    File items = new File(dataFolder, "items.yml");
    Files.writeString(
        items.toPath(),
        """
        hierarchy-hammer:
          enabled: false
          # my note
          dash-speed: 1.9
        """,
        StandardCharsets.UTF_8);
    Files.writeString(configFile.toPath(), "config-version: 20\n", StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    FileConfiguration cfg = YamlConfiguration.loadConfiguration(items);
    assertFalse(cfg.getBoolean("hierarchy-hammer.enabled"), "user value kept");
    assertEquals(1.9, cfg.getDouble("hierarchy-hammer.dash-speed"), "user value kept");
    assertTrue(cfg.contains("hierarchy-hammer.death-effect.duration"), "death-effect restored");
    assertTrue(cfg.contains("spear.enabled"), "spear section added");
    assertTrue(Files.readString(items.toPath()).contains("# my note"), "comment kept");
  }

  @AfterEach
  void tearDown() {
    if (configFile.exists()) {
      configFile.delete();
    }
  }

  @Test
  @DisplayName("Config version is bumped to TARGET_VERSION (14) and new sections are merged")
  void testFullConfigUpdateToVersion14() throws IOException {
    String oldConfig =
        """
        # Top banner comment
        config-version: 1
        season:
          total-ranks: 20
        health:
          max-hearts: 20.0
          min-hearts: 10.5
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    assertEquals(
        ConfigUpdater.getTargetVersion(),
        config.getInt("config-version"),
        "Config version must be updated to target version 14");

    assertTrue(config.contains("rank-display.show-on-join"), "rank-display must be added");
    assertTrue(config.contains("auto-assign-ranks.enabled"), "auto-assign-ranks must be added");
    assertTrue(config.contains("tab-integration.enabled"), "tab-integration must be added");
    assertTrue(config.contains("season.rank-stealing.cooldown"), "rank-stealing must be added");
    assertFalse(config.contains("hierarchy-hammer"), "hammer no longer lives in config.yml");
    assertFalse(config.contains("orbital-strike"), "orbital strike is removed");
  }

  @Test
  @DisplayName("User custom values are strictly preserved with zero data loss")
  void testZeroDataLossUserValuesStrictlyPreserved() throws IOException {
    String oldConfig =
        """
        config-version: 1
        season:
          total-ranks: 42
        health:
          max-hearts: 30.0
          min-hearts: 8.5
          custom-hearts:
            1: 35.0
            2: 32.5
            99: 15.0
        potions:
          rank-1-multiplier: 3.5
          custom-multipliers:
            1: 4.0
            2: 3.8
        xp:
          rank-1-multiplier: 5.5
          custom-multipliers:
            1: 6.0
            2: 5.8
        spear:
          enabled: false
          unbreakable: false
        debug: true
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    assertEquals(42, config.getInt("season.total-ranks"), "Custom season.total-ranks preserved");
    assertEquals(30.0, config.getDouble("health.max-hearts"), "Custom health.max-hearts preserved");
    assertEquals(8.5, config.getDouble("health.min-hearts"), "Custom health.min-hearts preserved");
    assertEquals(
        35.0, config.getDouble("health.custom-hearts.1"), "Custom health.custom-hearts.1 preserved");
    assertEquals(
        32.5, config.getDouble("health.custom-hearts.2"), "Custom health.custom-hearts.2 preserved");
    assertEquals(
        15.0, config.getDouble("health.custom-hearts.99"), "Custom health.custom-hearts.99 preserved");

    assertEquals(
        3.5, config.getDouble("potions.rank-1-multiplier"), "Custom potions multiplier preserved");
    assertEquals(
        4.0, config.getDouble("potions.custom-multipliers.1"), "Custom potion multiplier for rank 1 preserved");
    assertEquals(
        3.8, config.getDouble("potions.custom-multipliers.2"), "Custom potion multiplier for rank 2 preserved");

    assertEquals(5.5, config.getDouble("xp.rank-1-multiplier"), "Custom xp multiplier preserved");
    assertEquals(
        6.0, config.getDouble("xp.custom-multipliers.1"), "Custom xp multiplier for rank 1 preserved");
    assertEquals(
        5.8, config.getDouble("xp.custom-multipliers.2"), "Custom xp multiplier for rank 2 preserved");

    assertFalse(config.getBoolean("spear.enabled"), "Custom spear.enabled (false) preserved");
    assertFalse(config.getBoolean("spear.unbreakable"), "Custom spear.unbreakable (false) preserved");
    assertTrue(config.getBoolean("debug"), "Custom debug (true) preserved");
  }

  @Test
  @DisplayName("All comments (file header, section comments, user notes, and footer) are preserved")
  void testAllCommentsPreserved() throws IOException {
    String oldConfig =
        """
        # ==========================================
        # MY CUSTOM SERVER BANNER
        # DO NOT REMOVE THIS HEADER
        # ==========================================

        # Version of configuration
        config-version: 2

        # Season Settings Section
        # This controls rank progression
        season:
          # Note from Admin: total ranks changed for season 4
          total-ranks: 25

        # End of custom user configuration
        # Footer comments must be preserved!
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    String rawAfter = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);

    assertTrue(
        rawAfter.contains("# MY CUSTOM SERVER BANNER"), "Top banner comment must be preserved");
    assertTrue(
        rawAfter.contains("# DO NOT REMOVE THIS HEADER"), "Top banner lines must be preserved");
    assertTrue(
        rawAfter.contains("# Season Settings Section"), "Section comments must be preserved");
    assertTrue(
        rawAfter.contains("# Note from Admin: total ranks changed for season 4"),
        "User custom comments must be preserved");
    assertTrue(
        rawAfter.contains("# End of custom user configuration"),
        "Footer comments must be preserved");
    assertTrue(
        rawAfter.contains("# Footer comments must be preserved!"),
        "Trailing footer comments must be preserved");
  }

  @Test
  @DisplayName("Inline comments on scalar values are preserved through updates")
  void testInlineCommentsPreserved() throws IOException {
    String oldConfig =
        """
        config-version: 2 # Current config format version
        season:
          total-ranks: 20 # Number of ranks in this season
        health:
          max-hearts: 20.0 # Maximum allowed hearts
          min-hearts: 10.5 # Minimum allowed hearts
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    String rawAfter = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);

    assertTrue(
        rawAfter.contains("# Number of ranks in this season"),
        "Inline comment on total-ranks must be preserved");
    assertTrue(
        rawAfter.contains("# Maximum allowed hearts"),
        "Inline comment on max-hearts must be preserved");
    assertTrue(
        rawAfter.contains("# Minimum allowed hearts"),
        "Inline comment on min-hearts must be preserved");
  }

  @Test
  @DisplayName("v15/v16 migration neutralises never-used hammer defaults but keeps custom values")
  void testHammerComboDefaultsMigration() throws IOException {
    String oldDefaults =
        """
        config-version: 14
        hierarchy-hammer:
          combo-damage-multiplier: 2.0
          combo-explosion-power: 2.0 # terrain
          mace-smash:
            damage-multiplier: 2.0
        """;
    Files.writeString(configFile.toPath(), oldDefaults, StandardCharsets.UTF_8);
    configUpdater.updateConfig();

    FileConfiguration migrated = YamlConfiguration.loadConfiguration(configFile);
    assertEquals(ConfigUpdater.getTargetVersion(), migrated.getInt("config-version"));
    assertEquals(1.0, migrated.getDouble("hierarchy-hammer.mace-smash.damage-multiplier"));
    assertEquals(1.0, migrated.getDouble("hierarchy-hammer.combo-damage-multiplier"));
    assertEquals(0.0, migrated.getDouble("hierarchy-hammer.combo-explosion-power"));
    assertTrue(
        Files.readString(configFile.toPath(), StandardCharsets.UTF_8).contains("# terrain"),
        "Inline comment must survive the migration");

    String custom =
        """
        config-version: 14
        hierarchy-hammer:
          combo-damage-multiplier: 1.5
          combo-explosion-power: 3.0
        """;
    Files.writeString(configFile.toPath(), custom, StandardCharsets.UTF_8);
    configUpdater.updateConfig();

    FileConfiguration kept = YamlConfiguration.loadConfiguration(configFile);
    assertEquals(1.5, kept.getDouble("hierarchy-hammer.combo-damage-multiplier"));
    assertEquals(3.0, kept.getDouble("hierarchy-hammer.combo-explosion-power"));

    String current =
        """
        config-version: 16
        hierarchy-hammer:
          combo-damage-multiplier: 2.0
          combo-explosion-power: 2.0
          mace-smash:
            damage-multiplier: 2.0
        """;
    Files.writeString(configFile.toPath(), current, StandardCharsets.UTF_8);
    configUpdater.updateConfig();

    FileConfiguration untouched = YamlConfiguration.loadConfiguration(configFile);
    assertEquals(2.0, untouched.getDouble("hierarchy-hammer.combo-damage-multiplier"));
    assertEquals(2.0, untouched.getDouble("hierarchy-hammer.combo-explosion-power"));
    assertEquals(2.0, untouched.getDouble("hierarchy-hammer.mace-smash.damage-multiplier"));
  }

  @Test
  @DisplayName("updateValues preserves existing inline comments when values change")
  void testInlineCommentPreservedOnUpdateValues() throws IOException {
    String original =
        """
        config-version: 14
        season:
          total-ranks: 20 # Season rank count
        health:
          max-hearts: 20.0 # Max hearts limit
        """;

    Files.writeString(configFile.toPath(), original, StandardCharsets.UTF_8);

    boolean updated = configUpdater.updateValues(Map.of("season.total-ranks", "30"));
    assertTrue(updated, "updateValues should report success");

    String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
    assertTrue(
        content.contains("total-ranks: 30 # Season rank count"),
        "Value should be updated to 30 while retaining inline comment");

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    assertEquals(30, config.getInt("season.total-ranks"));
  }

  @Test
  @DisplayName("Hex color strings with # inside quotes are not treated as comments")
  void testHexColorStringsNotTreatedAsComments() throws IOException {
    String original =
        """
        config-version: 14
        tab-integration:
          enabled: true
          unranked-prefix: "&#ff0000[UNRANKED] "
          blacklisted-prefix: '&0[&8BLACKLISTED&0] '
        """;

    Files.writeString(configFile.toPath(), original, StandardCharsets.UTF_8);

    boolean updated = configUpdater.updateValues(Map.of("tab-integration.enabled", "false"));
    assertTrue(updated);

    String content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
    assertTrue(
        content.contains("\"&#ff0000[UNRANKED] \""),
        "Quoted hex color string must not be truncated or corrupted");

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    assertFalse(config.getBoolean("tab-integration.enabled"));
    assertEquals("&#ff0000[UNRANKED] ", config.getString("tab-integration.unranked-prefix"));
  }

  @Test
  @DisplayName("Quoted map keys are matched and updated without producing duplicate keys")
  void testQuotedKeysHandledWithoutDuplicates() throws IOException {
    String oldConfig =
        """
        config-version: 1
        health:
          max-hearts: 20.0
          min-hearts: 10.0
          custom-hearts:
            "1": 25.0
            '2': 24.0
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    assertEquals(25.0, config.getDouble("health.custom-hearts.1"), "Rank 1 custom hearts preserved");
    assertEquals(24.0, config.getDouble("health.custom-hearts.2"), "Rank 2 custom hearts preserved");

    String raw = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
    int countKey1 = 0;
    for (String line : raw.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.equals("\"1\": 25.0") || trimmed.equals("1: 25.0") || trimmed.equals("'1': 25.0")) {
        countKey1++;
      }
    }
    assertEquals(1, countKey1, "Key 1 must only appear once, no duplicates");
  }

  @Test
  @DisplayName("Adding new subkeys to an existing section preserves all existing sibling keys")
  void testNestedKeyAdditionWithoutOverwritingSiblings() throws IOException {
    String oldConfig =
        """
        config-version: 1
        rank-display:
          show-on-join: false
          # Custom format configured by admin
          format: "&6{player}"
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    assertFalse(
        config.getBoolean("rank-display.show-on-join"),
        "Existing rank-display.show-on-join must stay false");
    assertEquals(
        "&6{player}", config.getString("rank-display.format"), "Existing format must be kept");

    assertTrue(
        config.contains("rank-display.show-on-death"),
        "Missing rank-display.show-on-death must be added");
    assertTrue(
        config.contains("rank-display.show-on-leave"),
        "Missing rank-display.show-on-leave must be added");

    String raw = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
    assertTrue(
        raw.contains("# Custom format configured by admin"), "Sibling comment must be preserved");
  }

  @Test
  @DisplayName("Backup is created in backups/ directory before modifying config")
  void testBackupCreatedInBackupsFolder() throws IOException {
    String oldConfig =
        """
        config-version: 1
        season:
          total-ranks: 20
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    File backupsDir = new File(dataFolder, "backups");
    assertTrue(backupsDir.exists(), "backups/ directory must exist");
    assertTrue(backupsDir.isDirectory(), "backups/ must be a directory");

    File[] backupFiles = backupsDir.listFiles((dir, name) -> name.startsWith("config_") && name.endsWith(".yml"));
    assertNotNull(backupFiles, "Backup files list must not be null");
    assertTrue(backupFiles.length > 0, "At least one backup file must be created");

    String backupContent = Files.readString(backupFiles[0].toPath(), StandardCharsets.UTF_8);
    assertTrue(
        backupContent.contains("config-version: 1"),
        "Backup file must contain original pre-update content");
  }

  @Test
  @DisplayName("No update is performed if config is already at target version with all keys")
  void testNoUpdateWhenAlreadyAtTargetVersionWithAllKeys() throws IOException {
    when(plugin.getResource("config.yml"))
        .thenAnswer(invocation -> getClass().getClassLoader().getResourceAsStream("config.yml"));

    try (var is = getClass().getClassLoader().getResourceAsStream("config.yml")) {
      assertNotNull(is);
      Files.copy(is, configFile.toPath());
    }

    long modifiedBefore = configFile.lastModified();

    try {
      Thread.sleep(50);
    } catch (InterruptedException ignored) {
    }

    configUpdater.updateConfig();

    File backupsDir = new File(dataFolder, "backups");
    if (backupsDir.exists()) {
      File[] backupFiles = backupsDir.listFiles();
      assertEquals(
          0,
          backupFiles != null ? backupFiles.length : 0,
          "No backup should be created when no update is needed");
    }

    assertEquals(modifiedBefore, configFile.lastModified(), "File should not be touched if up to date");
  }

  @Test
  @DisplayName("Section header with inline comment parses properly as a section with children")
  void testSectionWithInlineCommentPreserved() throws IOException {
    String oldConfig =
        """
        config-version: 1
        season: # Season settings header
          total-ranks: 18 # Custom rank count
        health:
          max-hearts: 22.0
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    assertEquals(18, config.getInt("season.total-ranks"), "total-ranks under section with comment preserved");
    assertEquals(22.0, config.getDouble("health.max-hearts"), "health.max-hearts preserved");

    String raw = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
    assertTrue(
        raw.contains("# Season settings header"),
        "Section inline comment must be preserved in output");
    assertTrue(
        raw.contains("# Custom rank count"),
        "Value inline comment must be preserved in output");
  }

  @Test
  @DisplayName("Legacy rank display formats are migrated to rank-prefixes without data loss")
  void testLegacyRankDisplayMigration() throws IOException {
    String oldConfig =
        """
        config-version: 1
        tab-integration:
          enabled: true
          prefix-format: "&b[&3#%rank%&b] "
          suffix-format: " &7[Lvl %rank%]"
        """;

    Files.writeString(configFile.toPath(), oldConfig, StandardCharsets.UTF_8);

    configUpdater.updateConfig();

    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

    // Verify migrated rank prefixes
    assertEquals("&b[&3#1&b] ", config.getString("tab-integration.rank-prefixes.1"));
    assertEquals("&b[&3#5&b] ", config.getString("tab-integration.rank-prefixes.5"));
    assertEquals("&b[&3#20&b] ", config.getString("tab-integration.rank-prefixes.20"));

    // Verify migrated rank suffixes
    assertEquals(" &7[Lvl 1]", config.getString("tab-integration.rank-suffixes.1"));
    assertEquals(" &7[Lvl 20]", config.getString("tab-integration.rank-suffixes.20"));
  }
}
