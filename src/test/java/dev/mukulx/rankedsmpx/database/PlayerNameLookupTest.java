package dev.mukulx.rankedsmpx.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerNameLookupTest {

  private static final UUID BEDROCK = UUID.fromString("00000000-0000-0000-0009-01fd79799f17");
  private static final UUID JAVA = new UUID(1, 1);
  private static final UUID BANNED = new UUID(1, 2);

  @TempDir File dir;
  private DatabaseManager db;

  @BeforeEach
  void setUp() {
    RankedSMPXPlugin plugin = mock(RankedSMPXPlugin.class);
    when(plugin.getDataFolder()).thenReturn(dir);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    ConfigManager config = mock(ConfigManager.class);
    when(config.getConfig()).thenReturn(new YamlConfiguration());
    db = new DatabaseManager(plugin, config);
    assertTrue(db.initialize());

    long now = System.currentTimeMillis();
    db.executeUpdate(
        "INSERT INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?)",
        BEDROCK.toString(), ".SubPepper", 20, now);
    db.executeUpdate(
        "INSERT INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?)",
        JAVA.toString(), "Sub_Pepper", 3, now);
    db.executeUpdate(
        "INSERT INTO rank_blacklist (uuid, player_name, blacklisted_at, blacklisted_by) VALUES (?, ?, ?, ?)",
        BANNED.toString(), "*Griefer", now, "Console");
  }

  @AfterEach
  void tearDown() {
    db.close();
  }

  @Test
  @DisplayName("Names are found in any table, ignoring case")
  void findsByName() {
    assertEquals(Map.of(BEDROCK, ".SubPepper"), db.findPlayersByName(".subpepper"));
    assertEquals(Map.of(BANNED, "*Griefer"), db.findPlayersByName("*GRIEFER"));
    assertTrue(db.findPlayersByName("SubPepper").isEmpty());
  }

  @Test
  @DisplayName("A UUID maps back to its stored name")
  void findsNameByUuid() {
    assertEquals(".SubPepper", db.findPlayerName(BEDROCK));
    assertNull(db.findPlayerName(new UUID(9, 9)));
  }

  @Test
  @DisplayName("Suffix search treats _ and % in names as plain characters")
  void suffixIsLiteral() {
    assertEquals(Map.of(BEDROCK, ".SubPepper"), db.findPlayersByNameSuffix("SubPepper"));
    assertEquals(Map.of(JAVA, "Sub_Pepper"), db.findPlayersByNameSuffix("b_Pepper"));
    assertTrue(db.findPlayersByNameSuffix("%").isEmpty());
  }
}
