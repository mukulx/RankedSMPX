package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Upgrades the exact files shipped in the public Modrinth release (2.2.12, config-version 16). */
class ModrinthMigrationTest {

  @TempDir File dir;
  private RankedSMPXPlugin plugin;

  @BeforeEach
  void setUp() {
    plugin = mock(RankedSMPXPlugin.class);
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
    doAnswer(inv -> null).when(plugin).reloadConfig();
  }

  private void install(String name, String edit) throws Exception {
    String text;
    try (InputStream in = getClass().getResourceAsStream("/modrinth-2.2.12/" + name)) {
      text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    Files.writeString(new File(dir, name).toPath(), text, StandardCharsets.UTF_8);
  }

  private String read(String name) throws Exception {
    return Files.readString(new File(dir, name).toPath(), StandardCharsets.UTF_8);
  }

  private void replaceInConfig(String from, String to) throws Exception {
    String text = read("config.yml");
    assertTrue(text.contains(from), "fixture should contain: " + from);
    Files.writeString(new File(dir, "config.yml").toPath(), text.replace(from, to), StandardCharsets.UTF_8);
  }

  private RankTable upgrade() {
    ConfigLayout.migrate(plugin);
    plugin.reloadConfig();
    SplitConfiguration cfg = ConfigLayout.load(plugin);
    return RankTableFile.loadOrCreate(new File(dir, "ranks.yml"), cfg, Logger.getLogger("test"));
  }

  @Test
  @DisplayName("Untouched 2.2.12 install upgrades to the same rank values and keeps every other file")
  void untouchedInstall() throws Exception {
    for (String f : new String[] {"config.yml", "discord.yml", "messages.yml", "custom.yml"}) {
      install(f, null);
    }
    String messagesBefore = read("messages.yml");
    String discordBefore = read("discord.yml");
    String customBefore = read("custom.yml");

    RankTable migrated = upgrade();

    RankTable defaults = RankTable.defaults(20);
    assertEquals(20, migrated.totalRanks());
    for (int rank = 1; rank <= 20; rank++) {
      assertEquals(defaults.row(rank), migrated.row(rank), "rank " + rank);
    }
    assertEquals(defaults.unranked(), migrated.unranked());
    assertEquals(defaults.blacklisted(), migrated.blacklisted());
    assertTrue(migrated.warnings().isEmpty(), migrated.warnings().toString());

    assertEquals(messagesBefore, read("messages.yml"), "messages.yml untouched");
    assertEquals(discordBefore, read("discord.yml"), "discord.yml untouched");
    assertEquals(customBefore, read("custom.yml"), "custom.yml untouched");
    assertTrue(new File(dir, "items.yml").exists());
    File[] backups = new File(dir, "backups").listFiles((d, n) -> n.startsWith("config_v16_"));
    assertNotNull(backups);
    assertEquals(1, backups.length, "the old config.yml is backed up");
    assertTrue(Files.readString(backups[0].toPath()).contains("config-version: 16"));
  }

  @Test
  @DisplayName("A customised 2.2.12 install keeps hearts, xp, potions, slots, prefixes and settings")
  void customisedInstall() throws Exception {
    for (String f : new String[] {"config.yml", "discord.yml", "messages.yml", "custom.yml"}) {
      install(f, null);
    }
    replaceInConfig("  total-ranks: 20", "  total-ranks: 25");
    replaceInConfig("    3: 19.0\n", "    3: 17.25\n");
    replaceInConfig("    4: 2.7      # Rank 4", "    4: 9.9      # Rank 4");
    replaceInConfig("    5: 40   # 40 slots", "    5: 27   # 40 slots");
    replaceInConfig("    2: '&e[#2] &r'", "    2: '&#FFD700[&#FFAA00#2&#FFD700] &r'");
    replaceInConfig("  type: sqlite", "  type: mysql");
    replaceInConfig("    host: localhost", "    host: db.example.net");
    replaceInConfig("    cooldown: 0\n", "    cooldown: 45\n");
    replaceInConfig("dash-speed: 1.0", "dash-speed: 1.8");
    replaceInConfig("name: \"§x", "name: \"§cMy Hammer§x");
    replaceInConfig("small-caps-messages: true", "small-caps-messages: false");
    replaceInConfig("ENTITY_ENDER_DRAGON_GROWL\n    volume: 1.0", "ENTITY_ENDER_DRAGON_GROWL\n    volume: 0.3");

    RankTable t = upgrade();
    SplitConfiguration c = ConfigLayout.load(plugin);

    assertEquals(25, t.totalRanks());
    assertEquals(17.25, t.row(3).hearts());
    assertEquals(9.9, t.row(4).xp());
    assertEquals(27, t.row(5).slots());
    assertEquals("&#FFD700[&#FFAA00#2&#FFD700] &r", t.row(2).prefix());
    assertEquals(20.0, t.row(1).hearts());

    assertEquals("mysql", c.getString("database.type"));
    assertEquals("db.example.net", c.getString("database.mysql.host"));
    assertEquals(45, c.getInt("season.rank-stealing.cooldown"));
    assertEquals(1.8, c.getDouble("hierarchy-hammer.dash-speed"));
    assertTrue(c.getString("hierarchy-hammer.name").startsWith("§cMy Hammer"));
    assertFalse(c.getBoolean("small-caps-messages"));
    assertEquals(0.3, c.getDouble("sounds.season-start.volume"));
    assertEquals(ConfigUpdater.getTargetVersion(), c.getInt("config-version"));
    assertFalse(read("config.yml").contains("orbital-strike"), "removed feature is not carried over");
  }

  @Test
  @DisplayName("After the upgrade, a second start changes no file")
  void secondStartIsStable() throws Exception {
    install("config.yml", null);
    upgrade();
    String config = read("config.yml");
    String items = read("items.yml");
    String ranks = read("ranks.yml");

    upgrade();

    assertEquals(config, read("config.yml"));
    assertEquals(items, read("items.yml"));
    assertEquals(ranks, read("ranks.yml"));
    assertEquals(1, new File(dir, "backups").list().length, "no extra backups");
  }

  @Test
  @DisplayName("Upgraded files are valid YAML")
  void filesParse() throws Exception {
    install("config.yml", null);
    upgrade();
    for (String f : new String[] {"config.yml", "items.yml", "ranks.yml"}) {
      new YamlConfiguration().loadFromString(read(f));
    }
  }
}
