package dev.mukulx.rankedsmpx.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** A database written by the public release keeps every row after the new version starts. */
class DatabaseUpgradeTest {

  @TempDir File dir;

  private DatabaseManager open() {
    RankedSMPXPlugin plugin = mock(RankedSMPXPlugin.class);
    when(plugin.getDataFolder()).thenReturn(dir);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    ConfigManager config = mock(ConfigManager.class);
    when(config.getConfig()).thenReturn(new YamlConfiguration());
    DatabaseManager db = new DatabaseManager(plugin, config);
    assertTrue(db.initialize());
    return db;
  }

  private static int count(DatabaseManager db, String table) throws Exception {
    try (Connection c = db.getConnection();
        Statement s = c.createStatement();
        ResultSet r = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
      return r.getInt(1);
    }
  }

  @Test
  @DisplayName("Ranks above 20, history, blacklist, inventories and potions survive a restart on the new version")
  void dataSurvivesUpgrade() throws Exception {
    DatabaseManager old = open();
    long now = System.currentTimeMillis();
    for (int rank = 1; rank <= 25; rank++) {
      old.executeUpdate(
          "INSERT INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?)",
          new UUID(0, rank).toString(), "Player" + rank, rank, now);
    }
    old.executeUpdate(
        "INSERT INTO rank_history (uuid, player_name, rank, action, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
        new UUID(0, 1).toString(), "Player1", 1, "GAINED", "Season start", now);
    old.executeUpdate(
        "INSERT INTO rank_blacklist (uuid, player_name, blacklisted_at, blacklisted_by) VALUES (?, ?, ?, ?)",
        new UUID(0, 99).toString(), "Banned", now, "Admin");
    old.executeUpdate(
        "INSERT INTO extra_inventories (uuid, contents, last_updated) VALUES (?, ?, ?)",
        new UUID(0, 5).toString(), "contents", now);
    old.executeUpdate(
        "INSERT INTO potion_effects (uuid, effects, last_updated) VALUES (?, ?, ?)",
        new UUID(0, 5).toString(), "effects", now);
    old.close();

    DatabaseManager upgraded = open();

    assertEquals(25, count(upgraded, "ranks"));
    assertEquals(1, count(upgraded, "rank_history"));
    assertEquals(1, count(upgraded, "extra_inventories"));
    assertEquals(1, count(upgraded, "potion_effects"));
    assertTrue(upgraded.isBlacklisted(new UUID(0, 99)));

    RankedSMPXPlugin plugin = mock(RankedSMPXPlugin.class);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    RankManager ranks = new RankManager(plugin, upgraded);
    ranks.loadRanks();
    assertEquals(25, ranks.getAllRanks().size());
    assertEquals(25, ranks.getRank(new UUID(0, 25)));
    upgraded.close();
  }
}
