package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankManager {

  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final Map<UUID, Integer> playerRanks;
  private final Map<UUID, Long> stealCooldowns;

  public RankManager(@NotNull RankedSMPXPlugin plugin, @NotNull DatabaseManager databaseManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.playerRanks = new ConcurrentHashMap<>();
    this.stealCooldowns = new ConcurrentHashMap<>();
  }

  public void loadRanks() {
    playerRanks.clear();
    stealCooldowns.clear();

    try (Connection conn = databaseManager.getConnection()) {
      if (conn == null) {
        plugin.getLogger().severe("Failed to get database connection for loading ranks!");
        return;
      }

      try (PreparedStatement stmt = conn.prepareStatement("SELECT uuid, rank FROM ranks");
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          try {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            int rank = rs.getInt("rank");
            playerRanks.put(uuid, rank);
          } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID in database: " + rs.getString("uuid"));
          }
        }
      } catch (SQLException e) {
        plugin.getLogger().severe("Failed to load ranks: " + e.getMessage());
      }

      try (PreparedStatement stmt =
              conn.prepareStatement("SELECT uuid, cooldown_until FROM steal_cooldowns");
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          try {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            long cooldownUntil = rs.getLong("cooldown_until");
            if (cooldownUntil > System.currentTimeMillis()) {
              stealCooldowns.put(uuid, cooldownUntil);
            }
          } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID in cooldowns: " + rs.getString("uuid"));
          }
        }
      } catch (SQLException e) {
        plugin.getLogger().severe("Failed to load cooldowns: " + e.getMessage());
      }
    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to load ranks: " + e.getMessage());
    }

    plugin
        .getLogger()
        .info(
            "Loaded "
                + playerRanks.size()
                + " ranks and "
                + stealCooldowns.size()
                + " active cooldowns");
  }

  public void saveAllRanks() {
    for (Map.Entry<UUID, Integer> entry : playerRanks.entrySet()) {
      saveRank(entry.getKey(), entry.getValue());
    }
  }

  public void saveRank(@NotNull UUID uuid, int rank) {
    if (rank <= 0) {
      plugin.getLogger().severe("Attempted to save invalid rank " + rank + " for " + uuid);
      return;
    }

    Player player = Bukkit.getPlayer(uuid);
    String playerName;
    if (player != null) {
      playerName = player.getName();
    } else {
      org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
      playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
    }

    String sql =
        databaseManager.isMySql()
            ? "INSERT INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), rank=VALUES(rank), last_updated=VALUES(last_updated)"
            : "INSERT OR REPLACE INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?)";

    databaseManager.executeUpdate(
        sql, uuid.toString(), playerName, rank, System.currentTimeMillis());
  }

  public void saveRankAsync(@NotNull UUID uuid, int rank) {
    if (rank <= 0) {
      plugin.getLogger().severe("Attempted to save invalid rank " + rank + " for " + uuid);
      return;
    }

    SchedulerUtil.runAsync(plugin, () -> saveRank(uuid, rank));
  }

  public void saveRanksBatch(@NotNull Map<UUID, Integer> ranks) {
    if (ranks.isEmpty()) {
      plugin.getLogger().fine("No ranks to save in batch");
      return;
    }

    String sql =
        databaseManager.isMySql()
            ? "INSERT INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), rank=VALUES(rank), last_updated=VALUES(last_updated)"
            : "INSERT OR REPLACE INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?)";

    List<Object[]> paramsList = new ArrayList<>();
    long timestamp = System.currentTimeMillis();

    for (Map.Entry<UUID, Integer> entry : ranks.entrySet()) {
      if (entry.getValue() <= 0) {
        plugin
            .getLogger()
            .warning("Skipping invalid rank " + entry.getValue() + " for " + entry.getKey());
        continue;
      }

      Player player = Bukkit.getPlayer(entry.getKey());
      String playerName;
      if (player != null) {
        playerName = player.getName();
      } else {
        org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
        playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
      }
      paramsList.add(
          new Object[] {entry.getKey().toString(), playerName, entry.getValue(), timestamp});
    }

    if (!paramsList.isEmpty()) {
      databaseManager.executeBatch(sql, paramsList);
    }
  }

  public void removeRank(@NotNull UUID uuid) {
    removeRank(uuid, "");
  }

  public void removeRank(@NotNull UUID uuid, @NotNull String reason) {
    Integer oldRank = playerRanks.remove(uuid);
    String sql = "DELETE FROM ranks WHERE uuid = ?";
    databaseManager.executeUpdate(sql, uuid.toString());

    Player player = Bukkit.getPlayer(uuid);
    if (player != null && player.isOnline()) {
      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            if (player.isOnline()) {
              plugin.getInventoryManager().dropExtraInventoryOnRankRemoval(player);
            }
          });
    } else {
      plugin.getInventoryManager().markForDropOnJoin(uuid);
    }

    updateTABForPlayer(uuid);
    fireRankChange(uuid, oldRank, null, reason);
  }

  public synchronized void setRank(@NotNull UUID uuid, int rank) {
    setRank(uuid, rank, "");
  }

  public synchronized void setRank(@NotNull UUID uuid, int rank, @NotNull String reason) {
    if (rank <= 0) {
      plugin.getLogger().severe("CRITICAL: Attempted to set invalid rank " + rank + " for " + uuid);
      plugin.getLogger().severe("Stack trace:");
      Thread.dumpStack();
      return;
    }

    Integer oldRank = playerRanks.put(uuid, rank);

    saveRank(uuid, rank);

    updateTABForPlayer(uuid);
    fireRankChange(uuid, oldRank, rank, reason);
  }

  public void setRankInMemory(@NotNull UUID uuid, int rank) {
    if (rank <= 0) {
      plugin
          .getLogger()
          .severe("CRITICAL: Attempted to set invalid rank " + rank + " in memory for " + uuid);
      return;
    }
    Integer oldRank = playerRanks.put(uuid, rank);
    fireRankChange(uuid, oldRank, rank, "");
  }

  @Nullable
  public Integer getRank(@NotNull UUID uuid) {
    return playerRanks.get(uuid);
  }

  @Nullable
  public String getPlayerName(@NotNull UUID uuid) {
    String sql = "SELECT player_name FROM ranks WHERE uuid = ?";
    try (Connection conn = databaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, uuid.toString());
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("player_name");
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to get player name for " + uuid + ": " + e.getMessage());
    }
    return null;
  }

  @Nullable
  public UUID getPlayerWithRank(int rank) {
    for (Map.Entry<UUID, Integer> entry : playerRanks.entrySet()) {
      if (entry.getValue() != null && entry.getValue() == rank) {
        return entry.getKey();
      }
    }
    return null;
  }

  public boolean hasRank(@NotNull UUID uuid) {
    return playerRanks.containsKey(uuid);
  }

  @NotNull
  public Map<UUID, Integer> getAllRanks() {
    return Collections.unmodifiableMap(playerRanks);
  }

  @NotNull
  public List<Map.Entry<UUID, Integer>> getTopRanks(int limit) {
    return playerRanks.entrySet().stream()
        .sorted(Map.Entry.comparingByValue()) // Lower rank number = better rank
        .limit(limit)
        .toList();
  }

  public synchronized void swapRanks(@NotNull UUID uuid1, @NotNull UUID uuid2) {
    swapRanks(uuid1, uuid2, "", "");
  }

  public synchronized void swapRanks(
      @NotNull UUID uuid1, @NotNull UUID uuid2, @NotNull String reason1, @NotNull String reason2) {
    Integer rank1 = playerRanks.get(uuid1);
    Integer rank2 = playerRanks.get(uuid2);

    if (rank1 != null && rank2 != null) {
      playerRanks.put(uuid1, rank2);
      playerRanks.put(uuid2, rank1);
      saveRank(uuid1, rank2);
      saveRank(uuid2, rank1);

      updateTABForPlayer(uuid1);
      updateTABForPlayer(uuid2);
      fireRankChange(uuid1, rank1, rank2, reason1);
      fireRankChange(uuid2, rank2, rank1, reason2);
    }
  }

  public void clearAllRanks() {
    Map<UUID, Integer> previous = new HashMap<>(playerRanks);
    playerRanks.clear();
    stealCooldowns.clear();

    // Run the database operations synchronously to avoid race conditions
    databaseManager.executeUpdate("DELETE FROM ranks");
    databaseManager.executeUpdate("DELETE FROM steal_cooldowns");

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Cleared all ranks from memory and database");
    }
    previous.forEach((uuid, rank) -> fireRankChange(uuid, rank, null, "Season ended"));
  }

  public boolean isOnCooldown(@NotNull UUID uuid) {
    Long cooldownUntil = stealCooldowns.get(uuid);
    if (cooldownUntil == null) {
      return false;
    }

    if (System.currentTimeMillis() >= cooldownUntil) {
      stealCooldowns.remove(uuid, cooldownUntil);
      return false;
    }

    return true;
  }

  public long getCooldownRemaining(@NotNull UUID uuid) {
    Long cooldownUntil = stealCooldowns.get(uuid);
    if (cooldownUntil == null) {
      return 0;
    }

    long remaining = cooldownUntil - System.currentTimeMillis();
    return Math.max(0, remaining / 1000);
  }

  public void setCooldown(@NotNull UUID uuid, int seconds) {
    if (seconds < 0) {
      plugin.getLogger().warning("Invalid cooldown seconds: " + seconds);
      return;
    }

    long cooldownUntil = System.currentTimeMillis() + (seconds * 1000L);
    stealCooldowns.put(uuid, cooldownUntil);

    String sql =
        databaseManager.isMySql()
            ? "INSERT INTO steal_cooldowns (uuid, cooldown_until) VALUES (?, ?) ON DUPLICATE KEY UPDATE cooldown_until=VALUES(cooldown_until)"
            : "INSERT OR REPLACE INTO steal_cooldowns (uuid, cooldown_until) VALUES (?, ?)";

    databaseManager.executeUpdate(sql, uuid.toString(), cooldownUntil);
  }

  @NotNull
  public Set<Integer> getAvailableRanks(int totalRanks) {
    Set<Integer> available = new HashSet<>();
    for (int i = 1; i <= totalRanks; i++) {
      available.add(i);
    }
    available.removeAll(playerRanks.values());
    return available;
  }

  public int getRankedPlayerCount() {
    return playerRanks.size();
  }

  public synchronized void setRankWithHistory(
      @NotNull UUID uuid, int rank, @NotNull String reason) {
    setRank(uuid, rank, reason);

    Player player = Bukkit.getPlayer(uuid);
    String playerName = player != null ? player.getName() : "Unknown";
    databaseManager.addRankHistory(uuid, playerName, rank, "GAINED", reason);
  }

  public void removeRankWithHistory(@NotNull UUID uuid, int oldRank, @NotNull String reason) {
    removeRank(uuid, reason);

    Player player = Bukkit.getPlayer(uuid);
    String playerName = player != null ? player.getName() : "Unknown";
    databaseManager.addRankHistory(uuid, playerName, oldRank, "LOST", reason);
  }

  public synchronized void swapRanksWithHistory(
      @NotNull UUID uuid1, @NotNull UUID uuid2, @NotNull String reason1, @NotNull String reason2) {
    Integer rank1 = playerRanks.get(uuid1);
    Integer rank2 = playerRanks.get(uuid2);

    if (rank1 != null && rank2 != null) {
      swapRanks(uuid1, uuid2, reason1, reason2);

      Player player1 = Bukkit.getPlayer(uuid1);
      String playerName1 = player1 != null ? player1.getName() : "Unknown";

      Player player2 = Bukkit.getPlayer(uuid2);
      String playerName2 = player2 != null ? player2.getName() : "Unknown";

      String action1 = rank2 < rank1 ? "GAINED" : "LOST";
      databaseManager.addRankHistory(uuid1, playerName1, rank2, action1, reason1);

      String action2 = rank1 < rank2 ? "GAINED" : "LOST";
      databaseManager.addRankHistory(uuid2, playerName2, rank1, action2, reason2);
    }
  }

  private void fireRankChange(
      @NotNull UUID uuid, @Nullable Integer oldRank, @Nullable Integer newRank, @NotNull String reason) {
    if (java.util.Objects.equals(oldRank, newRank)) {
      return;
    }
    Bukkit.getPluginManager()
        .callEvent(new dev.mukulx.rankedsmpx.api.event.RankChangeEvent(uuid, oldRank, newRank, reason));
  }

  private void updateTABForPlayer(@NotNull UUID uuid) {
    Player player = Bukkit.getPlayer(uuid);
    if (player == null || !player.isOnline()) {
      return;
    }

    TABIntegration tabIntegration = plugin.getTabIntegration();
    if (tabIntegration != null && tabIntegration.isHooked()) {
      SchedulerUtil.runForEntity(plugin, player, () -> tabIntegration.updatePlayer(player));
    }

    VanillaTabManager vanillaTabManager = plugin.getVanillaTabManager();
    if (vanillaTabManager != null) {
      SchedulerUtil.runForEntity(plugin, player, () -> vanillaTabManager.updatePlayer(player));
    }

    if (plugin.getLuckPermsHook() != null && plugin.getLuckPermsHook().isHooked()) {
      plugin.getLuckPermsHook().signalUpdate(player);
    }
  }

}
