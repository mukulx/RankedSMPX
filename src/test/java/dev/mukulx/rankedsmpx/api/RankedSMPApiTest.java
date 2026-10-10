package dev.mukulx.rankedsmpx.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.api.event.RankChangeEvent;
import dev.mukulx.rankedsmpx.api.internal.RankedSMPApiImpl;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.GameManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.managers.RankTable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RankedSMPApiTest {

  private final UUID alice = UUID.randomUUID();
  private final UUID bob = UUID.randomUUID();

  private RankedSMPXPlugin plugin;
  private RankManager ranks;
  private DatabaseManager database;
  private GameManager game;
  private RankedSMPApi api;

  @BeforeEach
  void setUp() {
    plugin = mock(RankedSMPXPlugin.class);
    ranks = mock(RankManager.class);
    database = mock(DatabaseManager.class);
    game = mock(GameManager.class);
    ConfigManager config = mock(ConfigManager.class);
    RankTable table = RankTable.defaults(20);
    when(ranks.getRank(any())).thenReturn(null);
    when(plugin.getRankManager()).thenReturn(ranks);
    when(plugin.getDatabaseManager()).thenReturn(database);
    when(plugin.getGameManager()).thenReturn(game);
    when(plugin.getConfigManager()).thenReturn(config);
    when(config.getRanks()).thenReturn(table);
    when(config.getTotalRanks()).thenReturn(table.totalRanks());
    api = new RankedSMPApiImpl(plugin);
  }

  @Test
  @DisplayName("Rank lookups map to empty values for unranked players")
  void lookups() {
    when(ranks.getRank(alice)).thenReturn(4);
    when(ranks.getPlayerWithRank(4)).thenReturn(alice);
    assertEquals(4, api.getRank(alice).getAsInt());
    assertTrue(api.getRank(bob).isEmpty());
    assertEquals(alice, api.getPlayerWithRank(4).orElseThrow());
    assertTrue(api.getPlayerWithRank(5).isEmpty());
    assertEquals(20, api.getTotalRanks());
    assertEquals(1, RankedSMPApiImpl.API_VERSION);
  }

  @Test
  @DisplayName("Leaderboard and rank map are snapshots in the expected shape")
  void snapshots() {
    when(ranks.getAllRanks()).thenReturn(Map.of(alice, 1));
    when(ranks.getTopRanks(2)).thenReturn(List.of(Map.entry(alice, 1), Map.entry(bob, 2)));
    assertEquals(Map.of(alice, 1), api.getRanks());
    assertThrows(UnsupportedOperationException.class, () -> api.getRanks().put(bob, 3));
    assertEquals(List.of(new RankedPlayer(alice, 1), new RankedPlayer(bob, 2)), api.getLeaderboard(2));
  }

  @Test
  @DisplayName("Rank settings come from ranks.yml for ranked, unranked and blacklisted players")
  void settings() {
    when(ranks.getRank(alice)).thenReturn(1);
    when(database.isBlacklisted(bob)).thenReturn(true);
    RankSettings first = api.getRankSettings(alice);
    assertEquals(1, first.rank());
    assertEquals(20.0, first.hearts());
    assertEquals(3.0, first.xpMultiplier());
    assertEquals(54, first.extraSlots());
    assertEquals(0, api.getRankSettings(UUID.randomUUID()).rank());
    assertEquals(-1, api.getRankSettings(bob).rank());
    assertEquals(10.0, api.getRankSettings(0).hearts());
  }

  @Test
  @DisplayName("setRank refuses invalid, blacklisted and taken ranks without changing anything")
  void setRankValidation() {
    assertEquals(RankResult.INVALID_RANK, api.setRank(alice, 0, "x"));
    assertEquals(RankResult.INVALID_RANK, api.setRank(alice, 21, "x"));

    when(database.isBlacklisted(alice)).thenReturn(true);
    assertEquals(RankResult.BLACKLISTED, api.setRank(alice, 3, "x"));

    when(database.isBlacklisted(alice)).thenReturn(false);
    when(ranks.getPlayerWithRank(3)).thenReturn(bob);
    assertEquals(RankResult.RANK_TAKEN, api.setRank(alice, 3, "x"));

    verify(ranks, never()).setRankWithHistory(any(), anyInt(), any());
  }

  @Test
  @DisplayName("removeRank and swapRanks report false when a player has no rank")
  void removeAndSwapGuards() {
    assertFalse(api.removeRank(alice, "x"));
    when(ranks.getRank(alice)).thenReturn(1);
    assertFalse(api.swapRanks(alice, bob, "x"));
    assertFalse(api.swapRanks(alice, alice, "x"));
    verify(ranks, never()).swapRanksWithHistory(any(), any(), any(), any());
  }

  @Test
  @DisplayName("Season state is read from the game manager")
  void season() {
    when(game.isGameActive()).thenReturn(true);
    assertTrue(api.isSeasonActive());
    when(game.startGame()).thenReturn(false);
    assertFalse(api.startSeason());
  }

  @Test
  @DisplayName("Discord lookups are empty when the bot is off")
  void discordOff() {
    assertFalse(api.isDiscordEnabled());
    assertTrue(api.getDiscordId(alice).isEmpty());
    assertTrue(api.getLinkedPlayer("123").isEmpty());
  }

  @Test
  @DisplayName("RankChangeEvent tells gains from losses")
  void eventDirection() {
    assertTrue(new RankChangeEvent(alice, null, 5, "").isGain());
    assertTrue(new RankChangeEvent(alice, 5, 2, "").isGain());
    assertTrue(new RankChangeEvent(alice, 2, 5, "").isLoss());
    assertTrue(new RankChangeEvent(alice, 3, null, "").isLoss());
    assertFalse(new RankChangeEvent(alice, 3, null, "").isGain());
  }
}
