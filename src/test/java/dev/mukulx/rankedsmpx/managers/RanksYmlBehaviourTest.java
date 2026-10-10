package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** What each ranks.yml column does once the managers read it. */
class RanksYmlBehaviourTest {

  private static final String YAML =
      """
      ranks:
        -1: { prefix: "&0[B] &r", suffix: " &8x" }
        0:  { hearts: 10.0, xp: 1.0, potion: 1.0, slots: 0, prefix: "&7[U] &r", suffix: "" }
        1:  { hearts: 20.0, xp: 3.0, potion: 2.0, slots: 54, prefix: "&e[#1] &r", suffix: " &6*" }
        2:  { hearts: 18.0, xp: 2.0, potion: 1.5, slots: 27, prefix: "&e[#2] &r", suffix: "" }
        3:  { hearts: 12.5, xp: 1.1 }
        4: {}
        5: { hearts: 9.0, slots: 9 }
      """;

  private ConfigManager configManager;
  private RankTable table;

  @BeforeEach
  void setUp() throws Exception {
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.loadFromString(YAML);
    table = RankTable.parse(yaml.getConfigurationSection("ranks"));
    configManager = mock(ConfigManager.class);
    when(configManager.getRanks()).thenReturn(table);
    when(configManager.getConfig()).thenReturn(mock(FileConfiguration.class));
    when(configManager.getExtraSlots(anyInt())).thenAnswer(i -> table.row(i.getArgument(0)).slots());
  }

  @Test
  @DisplayName("Rows with missing columns copy the rank above; the rank count is the last row")
  void inheritance() {
    assertEquals(5, table.totalRanks());
    assertTrue(table.warnings().isEmpty(), table.warnings().toString());
    assertEquals(12.5, table.row(3).hearts());
    assertEquals(1.5, table.row(3).potion(), "potion copied from rank 2");
    assertEquals(27, table.row(3).slots(), "slots copied from rank 2");
    assertEquals(12.5, table.row(4).hearts());
    assertEquals("&e[#4] &r", table.row(4).prefix(), "prefix defaults to the rank number");
    assertEquals(9.0, table.row(5).hearts());
    assertEquals(1.1, table.row(5).xp());
    assertEquals(9, table.row(5).slots());
  }

  @Test
  @DisplayName("XP multiplier follows the rank and rounds to the nearest whole point")
  void xp() {
    XPManager xp = new XPManager(mock(RankedSMPXPlugin.class), configManager);
    assertEquals(15, xp.applyMultiplier(5, 1));
    assertEquals(10, xp.applyMultiplier(5, 2));
    assertEquals(6, xp.applyMultiplier(5, 3), "5 x 1.1 = 5.5 rounds up");
    assertEquals(1, xp.applyMultiplier(1, 3), "never below the base amount for a small boost");
  }

  @Test
  @DisplayName("Potion multiplier follows the rank and always rounds up")
  void potion() {
    RankedSMPXPlugin plugin = mock(RankedSMPXPlugin.class);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
    PotionManager potion = new PotionManager(plugin, configManager);
    assertEquals(6000, potion.applyMultiplier(3000, 1));
    assertEquals(4500, potion.applyMultiplier(3000, 2));
    assertEquals(3000, potion.applyMultiplier(3000, 0), "unranked is x1.0");
  }

  @Test
  @DisplayName("Extra inventory slots come straight from the slots column")
  void slots() {
    assertEquals(54, configManager.getExtraSlots(1));
    assertEquals(27, configManager.getExtraSlots(2));
    assertEquals(0, configManager.getExtraSlots(0));
    assertEquals(0, configManager.getExtraSlots(-1));
  }

  @Test
  @DisplayName("Unranked and blacklisted rows are used for players without a rank")
  void special() {
    assertEquals(10.0, table.unranked().hearts());
    assertEquals("&7[U] &r", table.unranked().prefix());
    assertEquals("&0[B] &r", table.blacklisted().prefix());
    assertEquals(" &8x", table.blacklisted().suffix());
    assertEquals(table.unranked(), table.row(0));
    assertEquals(table.blacklisted(), table.row(-1));
  }

  @Test
  @DisplayName("Prefix and suffix are stored per rank for tab, chat and placeholders")
  void prefixSuffix() {
    assertEquals("&e[#1] &r", table.row(1).prefix());
    assertEquals(" &6*", table.row(1).suffix());
    assertEquals("", table.row(2).suffix());
  }
}
