package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiscordLinkManager {

  private final RankedSMPXPlugin plugin;

  private final Map<UUID, LinkingCode> pendingCodes = new ConcurrentHashMap<>();

  private final Map<String, UUID> discordToUuidCache = new ConcurrentHashMap<>();

  private final Map<UUID, String> uuidToDiscordCache = new ConcurrentHashMap<>();

  private final Map<UUID, String> usernameCache = new ConcurrentHashMap<>();

  private static final String DELETE_LINK_SQL = "DELETE FROM discord_links WHERE uuid = ?";

  private static final String SELECT_BY_UUID_SQL =
      "SELECT discord_id, minecraft_username FROM discord_links WHERE uuid = ?";

  private static final String SELECT_BY_DISCORD_SQL =
      "SELECT uuid, minecraft_username FROM discord_links WHERE discord_id = ?";

  private static final String SELECT_ALL_SQL =
      "SELECT uuid, discord_id, minecraft_username FROM discord_links";

  public DiscordLinkManager(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void initialize() {
    createTable();
    loadLinksFromDatabase();
    startCodeCleanupTask();
    plugin
        .getLogger()
        .info(
            "Discord link manager initialized with " + uuidToDiscordCache.size() + " cached links");
  }

  private void createTable() {
    String createTableSql =
        """
            CREATE TABLE IF NOT EXISTS discord_links (
                uuid VARCHAR(36) PRIMARY KEY,
                discord_id VARCHAR(20) NOT NULL UNIQUE,
                minecraft_username VARCHAR(16) NOT NULL,
                linked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

    String createDiscordIdIndex =
        "CREATE INDEX IF NOT EXISTS idx_discord_id ON discord_links(discord_id)";
    String createUsernameIndex =
        "CREATE INDEX IF NOT EXISTS idx_username ON discord_links(minecraft_username)";

    try (Connection conn = plugin.getDatabaseManager().getConnection()) {
      try (PreparedStatement stmt = conn.prepareStatement(createTableSql)) {
        stmt.executeUpdate();
      }

      try (PreparedStatement stmt = conn.prepareStatement(createDiscordIdIndex)) {
        stmt.executeUpdate();
      }
      try (PreparedStatement stmt = conn.prepareStatement(createUsernameIndex)) {
        stmt.executeUpdate();
      }

      plugin.getLogger().info("Discord links table initialized");
    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to create discord_links table: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private void loadLinksFromDatabase() {
    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
        ResultSet rs = stmt.executeQuery()) {

      int count = 0;
      while (rs.next()) {
        try {
          String uuidStr = rs.getString("uuid");
          String discordId = rs.getString("discord_id");
          String username = rs.getString("minecraft_username");

          if (uuidStr == null || discordId == null) {
            plugin
                .getLogger()
                .warning(
                    "Skipping invalid link entry: uuid=" + uuidStr + ", discordId=" + discordId);
            continue;
          }

          UUID uuid = UUID.fromString(uuidStr);

          uuidToDiscordCache.put(uuid, discordId);
          discordToUuidCache.put(discordId, uuid);
          if (username != null && !username.isEmpty()) {
            usernameCache.put(uuid, username);
          }
          count++;
        } catch (IllegalArgumentException e) {
          plugin.getLogger().warning("Invalid UUID in discord_links table: " + e.getMessage());
        }
      }

      plugin.getLogger().info("Loaded " + count + " Discord links from database");

    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to load Discord links: " + e.getMessage());
      e.printStackTrace();
    }
  }

  @NotNull
  public String generateCode(@NotNull Player player) {
    if (player == null) {
      return "ERROR";
    }

    if (isLinked(player.getUniqueId())) {
      return "ALREADY_LINKED";
    }

    LinkingCode existing = pendingCodes.get(player.getUniqueId());
    if (existing != null && !existing.isExpired()) {
      return existing.code;
    }

    String code = String.format("%06d", new Random().nextInt(999999));

    int expirationMinutes = plugin.getDiscordConfig().getInt("linking.code-expiration", 10);
    long expirationTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(expirationMinutes);

    pendingCodes.put(player.getUniqueId(), new LinkingCode(code, player.getName(), expirationTime));

    plugin
        .getLogger()
        .info("Generated linking code for " + player.getName() + " (" + player.getUniqueId() + ")");

    return code;
  }

  public boolean verifyCode(@NotNull String discordId, @NotNull String code) {
    if (discordId == null || code == null || discordId.isEmpty() || code.isEmpty()) {
      return false;
    }

    if (discordToUuidCache.containsKey(discordId)) {
      plugin.getLogger().warning("Discord ID " + discordId + " is already linked");
      return false;
    }

    for (Map.Entry<UUID, LinkingCode> entry : pendingCodes.entrySet()) {
      LinkingCode linkingCode = entry.getValue();

      if (linkingCode.code.equals(code)) {
        if (linkingCode.isExpired()) {
          pendingCodes.remove(entry.getKey());
          plugin.getLogger().info("Linking code expired for " + linkingCode.username);
          return false;
        }

        UUID uuid = entry.getKey();
        String username = linkingCode.username;

        boolean success = linkAccounts(uuid, discordId, username);

        if (success) {
          pendingCodes.remove(uuid);
          plugin
              .getLogger()
              .info("Successfully linked " + username + " to Discord ID " + discordId);
        }

        return success;
      }
    }

    plugin.getLogger().info("Invalid linking code attempted: " + code);
    return false;
  }

  private void syncModRoleAfterLink(@NotNull UUID uuid) {
    RoleSyncManager roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
    if (roleSyncManager != null) {
      roleSyncManager.syncModRoleIfOnline(uuid);
    }
  }

  public boolean linkAccounts(
      @NotNull UUID uuid, @NotNull String discordId, @NotNull String username) {
    if (uuid == null || discordId == null || username == null) {
      plugin.getLogger().warning("Cannot link accounts: null parameter");
      return false;
    }

    UUID existingUuid = discordToUuidCache.get(discordId);
    if (existingUuid != null && !existingUuid.equals(uuid)) {
      plugin
          .getLogger()
          .warning("Discord ID " + discordId + " is already linked to " + existingUuid);
      return false;
    }

    String existingDiscordId = uuidToDiscordCache.get(uuid);
    if (existingDiscordId != null && !existingDiscordId.equals(discordId)) {
      plugin.getLogger().info("Unlinking old Discord ID " + existingDiscordId + " from " + uuid);
      discordToUuidCache.remove(existingDiscordId);
    }

    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(getInsertLinkSql())) {

      stmt.setString(1, uuid.toString());
      stmt.setString(2, discordId);
      stmt.setString(3, username);

      int rows = stmt.executeUpdate();

      if (rows > 0) {
        uuidToDiscordCache.put(uuid, discordId);
        discordToUuidCache.put(discordId, uuid);
        usernameCache.put(uuid, username);

        plugin
            .getLogger()
            .info("Linked " + username + " (" + uuid + ") to Discord ID " + discordId);
        syncModRoleAfterLink(uuid);
        fireLinkChange(uuid, discordId, true);
        return true;
      }

      return false;

    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to link accounts: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public boolean forceLink(
      @NotNull UUID uuid, @NotNull String discordId, @NotNull String username) {
    if (uuid == null || discordId == null || username == null) {
      plugin.getLogger().warning("Cannot force link accounts: null parameter");
      return false;
    }

    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(getInsertLinkSql())) {

      stmt.setString(1, uuid.toString());
      stmt.setString(2, discordId);
      stmt.setString(3, username);

      int rows = stmt.executeUpdate();

      if (rows > 0) {
        uuidToDiscordCache.put(uuid, discordId);
        discordToUuidCache.put(discordId, uuid);
        usernameCache.put(uuid, username);

        plugin
            .getLogger()
            .info("Force linked " + username + " (" + uuid + ") to Discord ID " + discordId);
        syncModRoleAfterLink(uuid);
        fireLinkChange(uuid, discordId, true);
        return true;
      }

      return false;

    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to force link accounts: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private void fireLinkChange(@NotNull UUID uuid, @NotNull String discordId, boolean linked) {
    org.bukkit.Bukkit.getPluginManager()
        .callEvent(new dev.mukulx.rankedsmpx.api.event.DiscordLinkChangeEvent(uuid, discordId, linked));
  }

  private String getInsertLinkSql() {
    if (plugin.getDatabaseManager().isMySql()) {
      return """
          INSERT INTO discord_links (uuid, discord_id, minecraft_username, linked_at)
          VALUES (?, ?, ?, CURRENT_TIMESTAMP)
          ON DUPLICATE KEY UPDATE
            discord_id=VALUES(discord_id),
            minecraft_username=VALUES(minecraft_username),
            linked_at=VALUES(linked_at)
          """;
    }
    return """
        INSERT OR REPLACE INTO discord_links (uuid, discord_id, minecraft_username, linked_at)
        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
        """;
  }

  public boolean unlinkAccount(@NotNull UUID uuid) {
    if (uuid == null) {
      return false;
    }

    String discordId = uuidToDiscordCache.get(uuid);
    if (discordId == null) {
      plugin.getLogger().info("Cannot unlink " + uuid + ": not linked");
      return false;
    }

    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(DELETE_LINK_SQL)) {

      stmt.setString(1, uuid.toString());
      int rows = stmt.executeUpdate();

      if (rows > 0) {
        uuidToDiscordCache.remove(uuid);
        discordToUuidCache.remove(discordId);
        usernameCache.remove(uuid);

        plugin.getLogger().info("Unlinked account: " + uuid);
        fireLinkChange(uuid, discordId, false);
        return true;
      }

      return false;

    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to unlink account: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public boolean unlinkDiscord(@NotNull String discordId) {
    if (discordId == null || discordId.isEmpty()) {
      return false;
    }

    UUID uuid = discordToUuidCache.get(discordId);
    if (uuid == null) {
      plugin.getLogger().info("Cannot unlink Discord ID " + discordId + ": not linked");
      return false;
    }

    return unlinkAccount(uuid);
  }

  public boolean unlinkMinecraft(@NotNull UUID uuid) {
    return unlinkAccount(uuid);
  }

  @Nullable
  public String getDiscordId(@Nullable UUID uuid) {
    if (uuid == null) {
      return null;
    }

    String discordId = uuidToDiscordCache.get(uuid);
    if (discordId != null) {
      return discordId;
    }

    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_BY_UUID_SQL)) {

      stmt.setString(1, uuid.toString());

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          discordId = rs.getString("discord_id");
          String username = rs.getString("minecraft_username");

          if (discordId != null) {
            uuidToDiscordCache.put(uuid, discordId);
            discordToUuidCache.put(discordId, uuid);
            if (username != null) {
              usernameCache.put(uuid, username);
            }
          }

          return discordId;
        }
      }

    } catch (SQLException e) {
      plugin
          .getLogger()
          .warning("Failed to get Discord ID for UUID " + uuid + ": " + e.getMessage());
    }

    return null;
  }

  @Nullable
  public UUID getMinecraftUUID(@Nullable String discordId) {
    if (discordId == null || discordId.isEmpty()) {
      return null;
    }

    UUID uuid = discordToUuidCache.get(discordId);
    if (uuid != null) {
      return uuid;
    }

    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_BY_DISCORD_SQL)) {

      stmt.setString(1, discordId);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          String uuidStr = rs.getString("uuid");
          String username = rs.getString("minecraft_username");

          if (uuidStr != null) {
            uuid = UUID.fromString(uuidStr);

            discordToUuidCache.put(discordId, uuid);
            uuidToDiscordCache.put(uuid, discordId);
            if (username != null) {
              usernameCache.put(uuid, username);
            }

            return uuid;
          }
        }
      }

    } catch (SQLException | IllegalArgumentException e) {
      plugin
          .getLogger()
          .warning("Failed to get UUID for Discord ID " + discordId + ": " + e.getMessage());
    }

    return null;
  }

  public boolean isLinked(@Nullable UUID uuid) {
    if (uuid == null) {
      return false;
    }
    return uuidToDiscordCache.containsKey(uuid);
  }

  @Nullable
  public String getUsername(@Nullable UUID uuid) {
    if (uuid == null) {
      return null;
    }

    String username = usernameCache.get(uuid);
    if (username != null) {
      return username;
    }

    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt = conn.prepareStatement(SELECT_BY_UUID_SQL)) {

      stmt.setString(1, uuid.toString());

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          username = rs.getString("minecraft_username");
          if (username != null) {
            usernameCache.put(uuid, username);
          }
          return username;
        }
      }

    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to get username for UUID " + uuid + ": " + e.getMessage());
    }

    return null;
  }

  public void updateUsername(@NotNull UUID uuid, @NotNull String username) {
    if (uuid == null || username == null || username.isEmpty()) {
      return;
    }

    if (!isLinked(uuid)) {
      return;
    }

    String cachedUsername = usernameCache.get(uuid);
    if (username.equals(cachedUsername)) {
      return;
    }

    String discordId = uuidToDiscordCache.get(uuid);
    if (discordId == null) {
      return;
    }

    try (Connection conn = plugin.getDatabaseManager().getConnection();
        PreparedStatement stmt =
            conn.prepareStatement(
                "UPDATE discord_links SET minecraft_username = ? WHERE uuid = ?")) {

      stmt.setString(1, username);
      stmt.setString(2, uuid.toString());

      int rows = stmt.executeUpdate();
      if (rows > 0) {
        usernameCache.put(uuid, username);
        plugin.getLogger().info("Updated username for " + uuid + " to " + username);
      }

    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to update username: " + e.getMessage());
    }
  }

  public int getTotalLinked() {
    return uuidToDiscordCache.size();
  }

  private void startCodeCleanupTask() {
    dev.mukulx.rankedsmpx.utils.SchedulerUtil.runTimerAsync(
            plugin,
            () -> {
              int removed = 0;
              Iterator<Map.Entry<UUID, LinkingCode>> iterator = pendingCodes.entrySet().iterator();
              while (iterator.hasNext()) {
                Map.Entry<UUID, LinkingCode> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                  iterator.remove();
                  removed++;
                }
              }
              if (removed > 0) {
                plugin.getLogger().info("Cleaned up " + removed + " expired linking codes");
              }
            },
            6000L,
            6000L); // Run every 5 minutes
  }

  public void shutdown() {
    pendingCodes.clear();
    uuidToDiscordCache.clear();
    discordToUuidCache.clear();
    usernameCache.clear();
    plugin.getLogger().info("Discord link manager shutdown complete");
  }

  private static class LinkingCode {
    final String code;
    final String username;
    final long expirationTime;

    LinkingCode(String code, String username, long expirationTime) {
      this.code = code;
      this.username = username;
      this.expirationTime = expirationTime;
    }

    boolean isExpired() {
      return System.currentTimeMillis() > expirationTime;
    }
  }
}
