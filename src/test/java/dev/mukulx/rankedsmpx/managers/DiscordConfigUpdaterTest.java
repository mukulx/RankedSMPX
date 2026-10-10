package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiscordConfigUpdaterTest {

  @TempDir Path tempDir;

  private RankedSMPXPlugin plugin;
  private DiscordConfigUpdater discordConfigUpdater;
  private File discordConfigFile;
  private File dataFolder;

  @BeforeEach
  void setUp() throws IOException {
    plugin = mock(RankedSMPXPlugin.class);
    Logger logger = Logger.getLogger("TestLogger");
    when(plugin.getLogger()).thenReturn(logger);

    dataFolder = tempDir.resolve("plugin").toFile();
    dataFolder.mkdirs();
    when(plugin.getDataFolder()).thenReturn(dataFolder);

    discordConfigFile = new File(dataFolder, "discord.yml");

    when(plugin.getResource("discord.yml"))
        .thenAnswer(invocation -> getClass().getClassLoader().getResourceAsStream("discord.yml"));

    discordConfigUpdater = new DiscordConfigUpdater(plugin);
  }

  @Test
  void testDiscordConfigVersionUpdate() throws IOException {
    String oldConfig =
        """
                config-version: 0
                enabled: false
                bot:
                  token: "test-token"
                  guild-id: "123456"
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertEquals(
        2, config.getInt("config-version"), "Discord config version should be updated to 2");
  }

  @Test
  void testDiscordExistingValuesPreserved() throws IOException {
    String oldConfig =
        """
                config-version: 0
                enabled: true
                bot:
                  token: "my-custom-token"
                  guild-id: "987654321"
                embed-footer-name: "My Custom Footer"
                linking:
                  enabled: false
                  required: true
                  code-expiration: 15
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertEquals(true, config.getBoolean("enabled"), "Custom enabled should be preserved");
    assertEquals(
        "my-custom-token", config.getString("bot.token"), "Custom bot token should be preserved");
    assertEquals(
        "987654321", config.getString("bot.guild-id"), "Custom guild ID should be preserved");
    assertEquals(
        "My Custom Footer",
        config.getString("embed-footer-name"),
        "Custom footer should be preserved");
    assertEquals(
        false, config.getBoolean("linking.enabled"), "Custom linking enabled should be preserved");
    assertEquals(
        true, config.getBoolean("linking.required"), "Custom linking required should be preserved");
    assertEquals(
        15, config.getInt("linking.code-expiration"), "Custom code expiration should be preserved");
  }

  @Test
  void testDiscordNewKeysAdded() throws IOException {
    String oldConfig =
        """
                config-version: 0
                enabled: false
                bot:
                  token: "test"
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertTrue(config.contains("embed-footer-name"), "embed-footer-name should be added");
    assertTrue(config.contains("linking.enabled"), "linking.enabled should be added");
    assertTrue(config.contains("linking.required"), "linking settings should be added");
    assertTrue(config.contains("leaderboard.enabled"), "leaderboard settings should be added");
    assertTrue(config.contains("notifications.enabled"), "notifications settings should be added");
    assertTrue(config.contains("role-sync.enabled"), "role-sync settings should be added");
    assertTrue(config.contains("updates.enabled"), "updates settings should be added");
    assertTrue(config.contains("commands.help"), "commands settings should be added");
    assertTrue(config.contains("notifications.notify-on-death"), "new notification settings should be added");
    assertTrue(config.contains("debug.enabled"), "debug settings should be added");
  }

  @Test
  void testDiscordLeaderboardSettingsPreserved() throws IOException {
    String oldConfig =
        """
                config-version: 0
                leaderboard:
                  enabled: true
                  channel-id: "123456789"
                  update-interval: 600
                  max-players: 15
                  title: "My Custom Leaderboard"
                  color: "#FF0000"
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertEquals(
        true, config.getBoolean("leaderboard.enabled"), "Leaderboard enabled should be preserved");
    assertEquals(
        "123456789",
        config.getString("leaderboard.channel-id"),
        "Leaderboard channel ID should be preserved");
    assertEquals(
        600,
        config.getInt("leaderboard.update-interval"),
        "Leaderboard update interval should be preserved");
    assertEquals(15, config.getInt("leaderboard.max-players"), "Max players should be preserved");
    assertEquals(
        "My Custom Leaderboard",
        config.getString("leaderboard.title"),
        "Leaderboard title should be preserved");
    assertEquals(
        "#FF0000", config.getString("leaderboard.color"), "Leaderboard color should be preserved");
  }

  @Test
  void testDiscordRoleSyncSettingsPreserved() throws IOException {
    String oldConfig =
        """
                config-version: 0
                role-sync:
                  enabled: true
                  auto-create-roles: false
                  role-format: "Rank {rank}"
                  unranked-role: "No Rank"
                  remove-old-roles: false
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertEquals(
        true, config.getBoolean("role-sync.enabled"), "Role sync enabled should be preserved");
    assertEquals(
        false,
        config.getBoolean("role-sync.auto-create-roles"),
        "Auto create roles should be preserved");
    assertEquals(
        "Rank {rank}",
        config.getString("role-sync.role-format"),
        "Role format should be preserved");
    assertEquals(
        "No Rank",
        config.getString("role-sync.unranked-role"),
        "Unranked role should be preserved");
    assertEquals(
        false,
        config.getBoolean("role-sync.remove-old-roles"),
        "Remove old roles should be preserved");
  }

  @Test
  void testDiscordCommandTogglesPreserved() throws IOException {
    String oldConfig =
        """
                config-version: 0
                commands:
                  help: false
                  rank: true
                  rankset: false
                  link: true
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertEquals(
        false, config.getBoolean("commands.help"), "Help command toggle should be preserved");
    assertEquals(
        true, config.getBoolean("commands.rank"), "Rank command toggle should be preserved");
    assertEquals(
        false, config.getBoolean("commands.rankset"), "Rankset command toggle should be preserved");
    assertEquals(
        true, config.getBoolean("commands.link"), "Link command toggle should be preserved");
  }

  @Test
  void testDiscordNotificationSettingsPreserved() throws IOException {
    String oldConfig =
        """
                config-version: 0
                notifications:
                  enabled: true
                  notify-on-gain: false
                  notify-on-loss: true
                  notify-top-3: false
                  color: "#00FF00"
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertEquals(
        true,
        config.getBoolean("notifications.enabled"),
        "Notifications enabled should be preserved");
    assertEquals(
        false,
        config.getBoolean("notifications.notify-on-gain"),
        "Notify on gain should be preserved");
    assertEquals(
        true,
        config.getBoolean("notifications.notify-on-loss"),
        "Notify on loss should be preserved");
    assertEquals(
        false, config.getBoolean("notifications.notify-top-3"), "Notify top 3 should be preserved");
    assertEquals(
        "#00FF00",
        config.getString("notifications.color"),
        "Notification color should be preserved");
  }

  @Test
  void testDiscordBackupCreated() throws IOException {
    String oldConfig =
        """
                config-version: 0
                enabled: false
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    File[] backups = dataFolder.listFiles((dir, name) -> name.startsWith("discord.yml.backup"));

    assertNotNull(backups, "Backup files should exist");
    assertTrue(backups.length > 0, "At least one backup should be created");
  }

  @Test
  void testDiscordNoUpdateNeededWhenCurrentVersion() throws IOException {
    String currentConfig =
        """
                config-version: 2
                enabled: false
                bot:
                  token: "test"
                """;

    Files.writeString(discordConfigFile.toPath(), currentConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertEquals(2, config.getInt("config-version"), "Discord config version should remain 2");
  }

  @Test
  void testDiscordLinkingEnabledDefaultValue() throws IOException {
    String oldConfig =
        """
                config-version: 1
                enabled: true
                linking:
                  required: false
                """;

    Files.writeString(discordConfigFile.toPath(), oldConfig);
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    when(plugin.getDiscordConfig()).thenReturn(config);

    discordConfigUpdater.updateDiscordConfig();

    config = YamlConfiguration.loadConfiguration(discordConfigFile);

    assertTrue(config.contains("linking.enabled"), "linking.enabled should be added during update");
    assertEquals(
        true,
        config.getBoolean("linking.enabled"),
        "linking.enabled should default to true when added");
  }
}
