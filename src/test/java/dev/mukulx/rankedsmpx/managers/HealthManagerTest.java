package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class HealthManagerTest {

  @Mock private RankedSMPXPlugin plugin;

  @Mock private RankManager rankManager;

  @Mock private ConfigManager configManager;

  @Mock private FileConfiguration config;

  @Mock private Logger logger;

  @Mock private Player player;

  @Mock private AttributeInstance attributeInstance;

  private HealthManager healthManager;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    when(plugin.getLogger()).thenReturn(logger);

    when(configManager.getConfig()).thenReturn(config);
    when(configManager.getRanks()).thenReturn(RankTable.defaults(20));
    when(configManager.getTotalRanks()).thenReturn(20);

    when(player.getName()).thenReturn("TestPlayer");
    when(player.getUniqueId()).thenReturn(UUID.randomUUID());
    when(player.getAttribute(Attribute.MAX_HEALTH)).thenReturn(attributeInstance);

    healthManager = new HealthManager(plugin, rankManager, configManager);
  }

  @Test
  @DisplayName("Calculate hearts for Rank 1 should return max hearts")
  void testCalculateHeartsRank1() {

    double hearts = healthManager.calculateHearts(1);

    assertEquals(20.0, hearts, 0.001, "Rank 1 should have 20 hearts");
  }

  @Test
  @DisplayName("Calculate hearts for Rank 20 should return min hearts")
  void testCalculateHeartsRank20() {

    double hearts = healthManager.calculateHearts(20);

    assertEquals(10.0, hearts, 0.001, "Rank 20 should have 10 hearts");
  }

  @Test
  @DisplayName("Apply health should set max health and heal player")
  void testApplyHealth() {
    UUID uuid = player.getUniqueId();
    when(rankManager.getRank(uuid)).thenReturn(1);

    healthManager.applyHealth(player);

    verify(attributeInstance).setBaseValue(40.0); // 20 hearts * 2 = 40 HP
    verify(player).setHealth(40.0);
  }

  @Test
  @DisplayName("Apply health should reset health for unranked player")
  void testApplyHealthUnranked() {
    UUID uuid = player.getUniqueId();
    when(rankManager.getRank(uuid)).thenReturn(null);

    healthManager.applyHealth(player);

    verify(attributeInstance).setBaseValue(20.0); // 10 hearts * 2 = 20 HP
    verify(player).setHealth(20.0);
  }

  @Test
  @DisplayName("Reset health should set to unranked hearts")
  void testResetHealth() {

    healthManager.resetHealth(player);

    verify(attributeInstance).setBaseValue(24.0); // 12 hearts * 2 = 24 HP
    verify(player).setHealth(24.0);
  }

  @Test
  @DisplayName("Apply health should handle null attribute gracefully")
  void testApplyHealthNullAttribute() {
    UUID uuid = player.getUniqueId();
    when(rankManager.getRank(uuid)).thenReturn(1);
    when(player.getAttribute(Attribute.MAX_HEALTH)).thenReturn(null);

    assertDoesNotThrow(() -> healthManager.applyHealth(player));
  }

  @Test
  @DisplayName("Apply health should not revive dead players")
  void testApplyHealthDoesNotReviveDeadPlayer() {
    UUID uuid = player.getUniqueId();
    when(rankManager.getRank(uuid)).thenReturn(1);
    when(player.isDead()).thenReturn(true);

    healthManager.applyHealth(player);

    verify(attributeInstance).setBaseValue(40.0);
    verify(player, never()).setHealth(anyDouble());
  }
}
