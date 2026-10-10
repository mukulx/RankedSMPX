package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public class DiscordConfigUpdater {

  private final RankedSMPXPlugin plugin;
  private final File discordConfigFile;

  public static final int TARGET_VERSION = 2;

  public DiscordConfigUpdater(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    this.discordConfigFile = new File(plugin.getDataFolder(), "discord.yml");
  }

  public void updateDiscordConfig() {
    if (!discordConfigFile.exists()) {
      saveDefaultDiscordConfig();
      plugin.getLogger().info("Created default discord.yml");
      return;
    }

    try {
      List<String> currentLines =
          Files.readAllLines(discordConfigFile.toPath(), StandardCharsets.UTF_8);
      List<String> defaultLines = readDefaultLines();

      if (defaultLines.isEmpty()) {
        plugin.getLogger().warning("Could not read default discord.yml from JAR, skipping update");
        return;
      }

      ConfigUpdater.SegmentTree current = ConfigUpdater.SegmentTree.parse(currentLines);
      ConfigUpdater.SegmentTree defaults = ConfigUpdater.SegmentTree.parse(defaultLines);

      int currentVersion = getDiscordConfigVersion();
      int added = current.mergeFrom(defaults);
      boolean versionUpdated =
          currentVersion < TARGET_VERSION
              && current.setValue("config-version", String.valueOf(TARGET_VERSION));
      if (added > 0 || versionUpdated) {
        List<String> result = current.toLines();
        createBackup();
        Files.write(discordConfigFile.toPath(), result, StandardCharsets.UTF_8);
        plugin.getLogger().info("Added " + added + " new discord config key(s) with comments preserved");
      }
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to update discord config: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public int getDiscordConfigVersion() {
    FileConfiguration config = YamlConfiguration.loadConfiguration(discordConfigFile);
    return config.getInt("config-version", 1);
  }

  private List<String> readDefaultLines() {
    try (java.io.InputStream is = plugin.getResource("discord.yml")) {
      if (is == null) {
        plugin.getLogger().severe("Default discord.yml not found in JAR!");
        return List.of();
      }
      byte[] bytes = is.readAllBytes();
      String text = new String(bytes, StandardCharsets.UTF_8);
      return new ArrayList<>(List.of(text.split("\n", -1)));
    } catch (IOException e) {
      plugin.getLogger().severe("Failed to read default discord.yml from JAR: " + e.getMessage());
      return List.of();
    }
  }

  private void saveDefaultDiscordConfig() {
    try {
      if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
      java.io.InputStream is = plugin.getResource("discord.yml");
      if (is == null) {
        plugin.getLogger().severe("Could not find default discord.yml in jar!");
        return;
      }
      Files.copy(is, discordConfigFile.toPath());
    } catch (IOException e) {
      plugin.getLogger().severe("Failed to save default discord.yml: " + e.getMessage());
    }
  }

  private void createBackup() {
    try {
      File backupFile = new File(plugin.getDataFolder(), "discord.yml.backup");
      if (backupFile.exists()) {
        File oldBackup =
            new File(
                plugin.getDataFolder(),
                "discord.yml.backup." + System.currentTimeMillis());
        backupFile.renameTo(oldBackup);
      }
      Files.copy(discordConfigFile.toPath(), backupFile.toPath());
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to create discord config backup: " + e.getMessage());
    }
  }
}
