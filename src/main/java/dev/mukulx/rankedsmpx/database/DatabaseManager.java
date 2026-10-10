package dev.mukulx.rankedsmpx.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DatabaseManager {

  private final RankedSMPXPlugin plugin;
  private final ConfigManager configManager;
  private HikariDataSource dataSource;
  private boolean isMySql;

  private long lastHealthCheck = 0;
  private static final long HEALTH_CHECK_INTERVAL = TimeUnit.MINUTES.toMillis(5);

  private static final int MAX_RETRIES = 3;
  private static final long RETRY_DELAY_MS = 1000;

  private static final int QUERY_TIMEOUT = 30;

  // In-memory copy of rank_blacklist. isBlacklisted() runs for placeholders, chat and tab updates,
  private final Set<UUID> blacklistCache = ConcurrentHashMap.newKeySet();

  public DatabaseManager(@NotNull RankedSMPXPlugin plugin, @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
  }

  public boolean initialize() {
    try {
      String type = configManager.getConfig().getString("database.type", "sqlite");
      this.isMySql = type.equalsIgnoreCase("mysql");

      HikariConfig config = new HikariConfig();

      if (isMySql) {
        String host = configManager.getConfig().getString("database.mysql.host", "localhost");
        int port = configManager.getConfig().getInt("database.mysql.port", 3306);
        String database =
            configManager.getConfig().getString("database.mysql.database", "rankedsmp");
        String username = configManager.getConfig().getString("database.mysql.username", "root");
        String password =
            configManager.getConfig().getString("database.mysql.password", "password");

        String jdbcUrl =
            String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&cachePrepStmts=true&useServerPrepStmts=true&rewriteBatchedStatements=true&tcpKeepAlive=true&socketTimeout=60000",
                host, port, database);

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(
            configManager.getConfig().getInt("database.mysql.pool.maximum-pool-size", 10));
        config.setMinimumIdle(
            configManager.getConfig().getInt("database.mysql.pool.minimum-idle", 2));
        config.setConnectionTimeout(
            configManager.getConfig().getLong("database.mysql.pool.connection-timeout", 30000));
        config.setIdleTimeout(
            configManager.getConfig().getLong("database.mysql.pool.idle-timeout", 600000));
        config.setMaxLifetime(
            configManager.getConfig().getLong("database.mysql.pool.max-lifetime", 1800000));
        config.setKeepaliveTime(60000); // Keep connections alive - prevents pool from going to zero

        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

      } else {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
          if (!dataFolder.mkdirs()) {
            plugin.getLogger().severe("Failed to create plugin data folder!");
            return false;
          }
        }

        String fileName =
            configManager.getConfig().getString("database.sqlite-file", "rankedsmp.db");
        File dbFile = new File(dataFolder, fileName);

        String jdbcUrl =
            "jdbc:sqlite:"
                + dbFile.getAbsolutePath()
                + "?journal_mode=WAL&synchronous=NORMAL&cache_size=10000&temp_store=MEMORY";

        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1); // SQLite only supports single writer
        config.setConnectionTimeout(30000);
      }

      config.addDataSourceProperty("cachePrepStmts", "true");
      config.addDataSourceProperty("prepStmtCacheSize", "250");
      config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
      config.addDataSourceProperty("useServerPrepStmts", "true");

      config.setLeakDetectionThreshold(60000); // 60 seconds

      config.setPoolName("RankedSMP-HikariPool");

      this.dataSource = new HikariDataSource(config);

      try (Connection conn = getConnection()) {
        if (conn == null || !conn.isValid(5)) {
          plugin.getLogger().severe("Database connection test failed!");
          return false;
        }
      }

      createTables();
      loadBlacklistCache();

      plugin
          .getLogger()
          .info(
              "Database initialized successfully! (Type: " + (isMySql ? "MySQL" : "SQLite") + ")");
      return true;

    } catch (Exception e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to initialize database!", e);
      return false;
    }
  }

  private void createTables() throws SQLException {
    String upsertSyntax = isMySql ? "ON DUPLICATE KEY UPDATE" : "ON CONFLICT(uuid) DO UPDATE SET";

    String createRanksTable =
        "CREATE TABLE IF NOT EXISTS ranks ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "player_name VARCHAR(16) NOT NULL,"
            + "rank INT NOT NULL,"
            + "last_updated BIGINT NOT NULL"
            + ")";

    String createStealCooldownTable =
        "CREATE TABLE IF NOT EXISTS steal_cooldowns ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "cooldown_until BIGINT NOT NULL"
            + ")";

    String createBlacklistTable =
        "CREATE TABLE IF NOT EXISTS rank_blacklist ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "player_name VARCHAR(16) NOT NULL,"
            + "blacklisted_at BIGINT NOT NULL,"
            + "blacklisted_by VARCHAR(16) NOT NULL"
            + ")";

    String createRankHistoryTable;
    if (isMySql) {
      createRankHistoryTable =
          "CREATE TABLE IF NOT EXISTS rank_history ("
              + "id INT PRIMARY KEY AUTO_INCREMENT,"
              + "uuid VARCHAR(36) NOT NULL,"
              + "player_name VARCHAR(16) NOT NULL,"
              + "rank INT NOT NULL,"
              + "action VARCHAR(20) NOT NULL,"
              + "reason TEXT NOT NULL,"
              + "timestamp BIGINT NOT NULL,"
              + "INDEX idx_uuid (uuid),"
              + "INDEX idx_timestamp (timestamp)"
              + ")";
    } else {
      createRankHistoryTable =
          "CREATE TABLE IF NOT EXISTS rank_history ("
              + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + "uuid VARCHAR(36) NOT NULL,"
              + "player_name VARCHAR(16) NOT NULL,"
              + "rank INT NOT NULL,"
              + "action VARCHAR(20) NOT NULL,"
              + "reason TEXT NOT NULL,"
              + "timestamp BIGINT NOT NULL"
              + ")";
    }

    String createExtraInventoriesTable =
        "CREATE TABLE IF NOT EXISTS extra_inventories ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "contents TEXT NOT NULL,"
            + "last_updated BIGINT NOT NULL,"
            + "drop_on_join INTEGER DEFAULT 0"
            + ")";

    String createPotionEffectsTable =
        "CREATE TABLE IF NOT EXISTS potion_effects ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "effects TEXT NOT NULL,"
            + "last_updated BIGINT NOT NULL"
            + ")";

    String createHistoryUuidIndex =
        "CREATE INDEX IF NOT EXISTS idx_rank_history_uuid ON rank_history(uuid)";
    String createHistoryTimestampIndex =
        "CREATE INDEX IF NOT EXISTS idx_rank_history_timestamp ON rank_history(timestamp)";

    try (Connection conn = getConnection()) {
      if (conn == null) {
        throw new SQLException("Failed to get database connection!");
      }

      try (Statement stmt = conn.createStatement()) {
        stmt.setQueryTimeout(QUERY_TIMEOUT);

        stmt.executeUpdate(createRanksTable);
        stmt.executeUpdate(createStealCooldownTable);
        stmt.executeUpdate(createBlacklistTable);
        stmt.executeUpdate(createRankHistoryTable);
        stmt.executeUpdate(createExtraInventoriesTable);
        stmt.executeUpdate(createPotionEffectsTable);

        if (!isMySql) {
          stmt.executeUpdate(createHistoryUuidIndex);
          stmt.executeUpdate(createHistoryTimestampIndex);
        }

        try {
          String addDropOnJoinColumn =
              "ALTER TABLE extra_inventories ADD COLUMN drop_on_join INTEGER DEFAULT 0";
          stmt.executeUpdate(addDropOnJoinColumn);
          plugin.getLogger().info("Added drop_on_join column to extra_inventories table");
        } catch (SQLException e) {
          if (!e.getMessage().contains("duplicate column")
              && !e.getMessage().contains("already exists")) {
            plugin.getLogger().warning("Migration warning: " + e.getMessage());
          }
        }

        try {
          String checkTable = "SELECT 1 FROM potion_effects LIMIT 1";
          stmt.executeQuery(checkTable).close();
          plugin.getLogger().fine("potion_effects table already exists");
        } catch (SQLException e) {
          plugin.getLogger().info("Created potion_effects table for effect persistence");
        }
      }

    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to create tables!", e);
      throw e;
    }
  }

  @Nullable
  public Connection getConnection() {
    try {
      long now = System.currentTimeMillis();
      if (now - lastHealthCheck > HEALTH_CHECK_INTERVAL) {
        performHealthCheck();
        lastHealthCheck = now;
      }

      if (dataSource != null && !dataSource.isClosed()) {
        Connection conn = dataSource.getConnection();
        if (conn != null && conn.isValid(5)) {
          return conn;
        } else {
          plugin.getLogger().warning("Got invalid connection from pool!");
          if (conn != null) {
            try {
              conn.close();
            } catch (SQLException ignored) {
            }
          }
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get database connection!", e);
    }
    return null;
  }

  public boolean isSQLite() {
    return !isMySql;
  }

  private void performHealthCheck() {
    if (dataSource == null || dataSource.isClosed()) {
      plugin.getLogger().warning("Database connection pool is closed!");
      return;
    }

    try (Connection conn = dataSource.getConnection()) {
      if (conn == null || !conn.isValid(5)) {
        plugin.getLogger().warning("Database health check failed!");
      } else {
        plugin.getLogger().fine("Database health check passed");
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Database health check error: " + e.getMessage());
    }
  }

  @Nullable
  private Connection getConnectionWithRetry() {
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      Connection conn = getConnection();
      if (conn != null) {
        return conn;
      }

      if (attempt < MAX_RETRIES) {
        plugin.getLogger().warning("Connection attempt " + attempt + " failed, retrying...");
        try {
          Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    plugin.getLogger().severe("Failed to get connection after " + MAX_RETRIES + " attempts!");
    return null;
  }

  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      dataSource.close();
      plugin.getLogger().info("Database connection pool closed.");
    }
  }

  public void executeUpdate(@NotNull String sql, @Nullable Object... params) {
    executeUpdateWithRetry(sql, params, 1);
  }

  private void executeUpdateWithRetry(@NotNull String sql, @Nullable Object[] params, int attempt) {
    Connection conn = null;
    PreparedStatement stmt = null;

    try {
      conn = getConnectionWithRetry();
      if (conn == null) {
        plugin.getLogger().severe("Failed to get database connection for update!");
        return;
      }

      stmt = conn.prepareStatement(sql);
      stmt.setQueryTimeout(QUERY_TIMEOUT);

      if (params != null) {
        for (int i = 0; i < params.length; i++) {
          stmt.setObject(i + 1, params[i]);
        }
      }

      stmt.executeUpdate();

    } catch (SQLTimeoutException e) {
      plugin.getLogger().log(Level.SEVERE, "Query timeout executing: " + sql, e);
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to execute update: " + sql, e);

      if (isTransientError(e) && attempt < MAX_RETRIES) {
        plugin.getLogger().warning("Retrying update (attempt " + (attempt + 1) + ")...");
        try {
          Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
        executeUpdateWithRetry(sql, params, attempt + 1);
      }
    } finally {
      closeQuietly(stmt);
      closeQuietly(conn);
    }
  }

  private boolean isTransientError(SQLException e) {
    String sqlState = e.getSQLState();
    int errorCode = e.getErrorCode();

    if (sqlState == null) {
      return false;
    }
    boolean connectionOrRollback = sqlState.startsWith("08") || sqlState.startsWith("40");
    boolean lockTimeoutOrDeadlock = errorCode == 1205 || errorCode == 1213;
    return connectionOrRollback || lockTimeoutOrDeadlock;
  }

  private void closeQuietly(AutoCloseable resource) {
    if (resource != null) {
      try {
        resource.close();
      } catch (Exception e) {
        plugin.getLogger().log(Level.FINE, "Error closing resource", e);
      }
    }
  }

  public void executeBatch(@NotNull String sql, @NotNull List<Object[]> paramsList) {
    if (paramsList.isEmpty()) {
      plugin.getLogger().fine("Empty batch, skipping execution");
      return;
    }

    Connection conn = null;
    PreparedStatement stmt = null;
    boolean originalAutoCommit = true;

    try {
      conn = getConnectionWithRetry();
      if (conn == null) {
        plugin.getLogger().severe("Failed to get database connection for batch!");
        return;
      }

      originalAutoCommit = conn.getAutoCommit();
      conn.setAutoCommit(false);

      stmt = conn.prepareStatement(sql);
      stmt.setQueryTimeout(QUERY_TIMEOUT);

      int batchSize = 0;
      for (Object[] params : paramsList) {
        for (int i = 0; i < params.length; i++) {
          stmt.setObject(i + 1, params[i]);
        }
        stmt.addBatch();
        batchSize++;

        if (batchSize % 1000 == 0) {
          stmt.executeBatch();
          stmt.clearBatch();
          plugin.getLogger().fine("Executed batch chunk of 1000 statements");
        }
      }

      if (batchSize % 1000 != 0) {
        stmt.executeBatch();
      }

      conn.commit();
      plugin
          .getLogger()
          .info("Successfully executed batch of " + paramsList.size() + " statements");

    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Database batch operation failed: " + sql, e);

      if (conn != null) {
        try {
          conn.rollback();
          plugin.getLogger().info("Batch operation rolled back");
        } catch (SQLException ex) {
          plugin.getLogger().log(Level.SEVERE, "Failed to rollback batch!", ex);
        }
      }
    } finally {
      closeQuietly(stmt);

      if (conn != null) {
        try {
          conn.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
          plugin.getLogger().log(Level.WARNING, "Failed to restore auto-commit", e);
        }
        closeQuietly(conn);
      }
    }
  }

  public boolean isBlacklisted(@NotNull UUID uuid) {
    return blacklistCache.contains(uuid);
  }

  private void loadBlacklistCache() {
    blacklistCache.clear();
    String sql = "SELECT uuid FROM rank_blacklist";
    try (Connection conn = getConnection()) {
      if (conn == null) {
        plugin.getLogger().warning("Cannot load blacklist - no database connection");
        return;
      }
      try (PreparedStatement stmt = conn.prepareStatement(sql);
          ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          try {
            blacklistCache.add(UUID.fromString(rs.getString("uuid")));
          } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID in blacklist: " + rs.getString("uuid"));
          }
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to load rank blacklist", e);
    }
  }

  public void addToBlacklist(
      @NotNull UUID uuid, @NotNull String playerName, @NotNull String blacklistedBy) {
    if (uuid == null || playerName == null || playerName.trim().isEmpty()) {
      plugin
          .getLogger()
          .warning("Invalid blacklist parameters: uuid=" + uuid + ", name=" + playerName);
      return;
    }

    String sql =
        isMySql
            ? "INSERT INTO rank_blacklist (uuid, player_name, blacklisted_at, blacklisted_by) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), blacklisted_at=VALUES(blacklisted_at), blacklisted_by=VALUES(blacklisted_by)"
            : "INSERT OR REPLACE INTO rank_blacklist (uuid, player_name, blacklisted_at, blacklisted_by) VALUES (?, ?, ?, ?)";

    executeUpdate(sql, uuid.toString(), playerName, System.currentTimeMillis(), blacklistedBy);
    if (blacklistCache.add(uuid)) {
      org.bukkit.Bukkit.getPluginManager()
          .callEvent(new dev.mukulx.rankedsmpx.api.event.BlacklistChangeEvent(uuid, true));
    }
  }

  public void removeFromBlacklist(@NotNull UUID uuid) {
    String sql = "DELETE FROM rank_blacklist WHERE uuid = ?";
    executeUpdate(sql, uuid.toString());
    if (blacklistCache.remove(uuid)) {
      org.bukkit.Bukkit.getPluginManager()
          .callEvent(new dev.mukulx.rankedsmpx.api.event.BlacklistChangeEvent(uuid, false));
    }
  }

  @NotNull
  public List<String> getBlacklistedPlayers() {
    List<String> players = new ArrayList<>();
    String sql = "SELECT player_name FROM rank_blacklist ORDER BY player_name";
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      conn = getConnection();
      if (conn == null) {
        plugin.getLogger().warning("Cannot get blacklisted players - no database connection");
        return players;
      }

      stmt = conn.prepareStatement(sql);
      stmt.setQueryTimeout(QUERY_TIMEOUT);
      rs = stmt.executeQuery();

      while (rs.next()) {
        String name = rs.getString("player_name");
        if (name != null && !name.trim().isEmpty()) {
          players.add(name);
        }
      }

    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get blacklisted players!", e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }

    return players;
  }

  public void addRankHistory(
      @NotNull UUID uuid,
      @NotNull String playerName,
      int rank,
      @NotNull String action,
      @NotNull String reason) {
    if (uuid == null || playerName == null || playerName.trim().isEmpty()) {
      plugin.getLogger().warning("Invalid rank history parameters");
      return;
    }

    if (rank <= 0) {
      plugin.getLogger().warning("Invalid rank in history: " + rank);
      return;
    }

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          String sql =
              "INSERT INTO rank_history (uuid, player_name, rank, action, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
          executeUpdate(
              sql, uuid.toString(), playerName, rank, action, reason, System.currentTimeMillis());
        });
  }

  @NotNull
  public List<RankHistoryEntry> getRankHistory(@NotNull UUID uuid, int limit) {
    List<RankHistoryEntry> history = new ArrayList<>();

    if (limit <= 0) {
      plugin.getLogger().warning("Invalid limit for rank history: " + limit);
      return history;
    }

    String sql = "SELECT * FROM rank_history WHERE uuid = ? ORDER BY timestamp DESC LIMIT ?";
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
      conn = getConnection();
      if (conn == null) {
        plugin.getLogger().warning("Cannot get rank history - no database connection");
        return history;
      }

      stmt = conn.prepareStatement(sql);
      stmt.setQueryTimeout(QUERY_TIMEOUT);
      stmt.setString(1, uuid.toString());
      stmt.setInt(2, limit);

      rs = stmt.executeQuery();

      while (rs.next()) {
        try {
          history.add(
              new RankHistoryEntry(
                  rs.getString("player_name"),
                  rs.getInt("rank"),
                  rs.getString("action"),
                  rs.getString("reason"),
                  rs.getLong("timestamp")));
        } catch (SQLException e) {
          plugin.getLogger().warning("Skipping corrupted rank history entry: " + e.getMessage());
        }
      }

    } catch (SQLException e) {
      plugin.getLogger().log(Level.SEVERE, "Failed to get rank history for " + uuid, e);
    } finally {
      closeQuietly(rs);
      closeQuietly(stmt);
      closeQuietly(conn);
    }

    return history;
  }

  public void clearRankHistory(@NotNull UUID uuid) {
    String sql = "DELETE FROM rank_history WHERE uuid = ?";
    executeUpdate(sql, uuid.toString());
  }

  public void clearAllRankHistory() {
    String sql = "DELETE FROM rank_history";
    executeUpdate(sql);
  }

  public boolean isMySql() {
    return isMySql;
  }

  public static class RankHistoryEntry {
    private final String playerName;
    private final int rank;
    private final String action;
    private final String reason;
    private final long timestamp;

    public RankHistoryEntry(
        String playerName, int rank, String action, String reason, long timestamp) {
      this.playerName = playerName;
      this.rank = rank;
      this.action = action;
      this.reason = reason;
      this.timestamp = timestamp;
    }

    public String getPlayerName() {
      return playerName;
    }

    public int getRank() {
      return rank;
    }

    public String getAction() {
      return action;
    }

    public String getReason() {
      return reason;
    }

    public long getTimestamp() {
      return timestamp;
    }
  }
}
