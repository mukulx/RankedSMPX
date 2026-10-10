package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FirstTimeSetupManager {

  private final RankedSMPXPlugin plugin;
  private final MessageManager messageManager;
  private final DatabaseManager databaseManager;

  private final Set<UUID> shownThisSession = new HashSet<>();

  public FirstTimeSetupManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull MessageManager messageManager,
      @NotNull DatabaseManager databaseManager) {
    this.plugin = plugin;
    this.messageManager = messageManager;
    this.databaseManager = databaseManager;
    createTable();
  }

  private void createTable() {
    String createTable;

    if (databaseManager.isSQLite()) {
      createTable =
          "CREATE TABLE IF NOT EXISTS first_time_setup ("
              + "uuid VARCHAR(36) PRIMARY KEY, "
              + "seen BOOLEAN NOT NULL DEFAULT 0, "
              + "seen_at INTEGER"
              + ")";
    } else {
      createTable =
          "CREATE TABLE IF NOT EXISTS first_time_setup ("
              + "uuid VARCHAR(36) PRIMARY KEY, "
              + "seen BOOLEAN NOT NULL DEFAULT FALSE, "
              + "seen_at BIGINT"
              + ")";
    }

    try (Connection conn = databaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(createTable)) {

      boolean tableExists = checkTableExists(conn);

      stmt.executeUpdate();

    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to initialize first_time_setup table: " + e.getMessage());
      e.printStackTrace();
    }
  }

  private boolean checkTableExists(@NotNull Connection conn) {
    try {
      String query = "SELECT COUNT(*) FROM first_time_setup LIMIT 1";
      try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.executeQuery();
        return true;
      }
    } catch (SQLException e) {
      return false;
    }
  }

  public boolean hasSeen(@NotNull Player player) {
    String query = "SELECT seen FROM first_time_setup WHERE uuid = ?";

    try (Connection conn = databaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(query)) {
      stmt.setString(1, player.getUniqueId().toString());
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        return rs.getBoolean("seen");
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to check first-time setup status: " + e.getMessage());
    }

    return false;
  }

  public void markAsSeen(@NotNull Player player) {
    String upsert;
    if (databaseManager.isSQLite()) {
      upsert =
          "INSERT INTO first_time_setup (uuid, seen, seen_at) VALUES (?, 1, ?) "
              + "ON CONFLICT(uuid) DO UPDATE SET seen = 1, seen_at = ?";
    } else {
      upsert =
          "INSERT INTO first_time_setup (uuid, seen, seen_at) VALUES (?, TRUE, ?) "
              + "ON DUPLICATE KEY UPDATE seen = TRUE, seen_at = ?";
    }

    long timestamp = System.currentTimeMillis();

    try (Connection conn = databaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(upsert)) {
      stmt.setString(1, player.getUniqueId().toString());
      stmt.setLong(2, timestamp);
      stmt.setLong(3, timestamp);
      stmt.executeUpdate();
    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to mark first-time setup as seen: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void showIfNeeded(@NotNull Player player) {
    if (!player.hasPermission("rankedsmp.admin")) {
      return;
    }

    if (hasSeen(player)) {
      return;
    }

    if (shownThisSession.contains(player.getUniqueId())) {
      return;
    }

    shownThisSession.add(player.getUniqueId());

    markAsSeen(player);

    showSetupMessage(player);
  }

  private void showSetupMessage(@NotNull Player player) {
    String discordInvite =
        plugin.getDiscordConfig().getString("discord-invite", "https://discord.gg/QGsJYht2vy");

    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps(
            "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    player.sendMessage(messageManager.applySmallCaps("  §b§6§lWELCOME TO RANKEDSMPX"));
    player.sendMessage("");
    player.sendMessage(messageManager.applySmallCaps("  §7Thank you for installing RankedSMPX!"));
    player.sendMessage("");
    player.sendMessage(messageManager.applySmallCaps("  §b§lQUICK START GUIDE:"));
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps(
            "  §61. §7Start the game: §b/rankedsmp start §7or §b/rsmp start"));
    player.sendMessage(
        messageManager.applySmallCaps("     §8→ §7Gives all online players random ranks"));
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps(
            "  §62. §7Manually assign ranks: §b/rankedsmp set <player> <rank>"));
    player.sendMessage(messageManager.applySmallCaps("     §8→ §7Example: §b/rsmp set Steve 1"));
    player.sendMessage("");
    player.sendMessage(messageManager.applySmallCaps("  §b§lDEFAULT SETTINGS:"));
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps(
            "  §7• §fTab prefixes: §bLegacy colors §7(yellow for ranks, gray for unranked)"));
    player.sendMessage(
        messageManager.applySmallCaps(
            "  §7• §fChange to modern style: §b/rsmp modern §7(requires TAB plugin)"));
    player.sendMessage(
        messageManager.applySmallCaps("  §7• §fRevert to original: §b/rsmp original"));
    player.sendMessage("");
    player.sendMessage(messageManager.applySmallCaps("  §b§lNEED HELP?"));
    player.sendMessage("");
    player.sendMessage(messageManager.applySmallCaps("  §7• Discord support: §b" + discordInvite));
    player.sendMessage(
        messageManager.applySmallCaps(
            "  §7• Report bugs: §bhttps://github.com/mukulx/RankedSMPX/issues"));
    player.sendMessage("");
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps("  §b§l§6§lTHIS MESSAGE ONLY SHOWS ONCE §b§l"));
    player.sendMessage(
        messageManager.applySmallCaps("  §7It will §c§lNEVER§7 appear again after this."));
    player.sendMessage("");
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps("  §7Hope you enjoy the plugin, §agood luck§7!"));
    player.sendMessage(messageManager.applySmallCaps("  §8(Only admins see this message)"));
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps(
            "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    player.sendMessage("");
  }

  public void dismiss(@NotNull Player player) {
    if (!player.hasPermission("rankedsmp.admin")) {
      return;
    }

    markAsSeen(player);
    player.sendMessage("");
    player.sendMessage(
        messageManager.getPrefix()
            + " "
            + messageManager.applySmallCaps("§aFirst-time setup message dismissed!"));
    player.sendMessage(
        messageManager.getPrefix()
            + " "
            + messageManager.applySmallCaps("§7This message will §c§lNEVER§7 show again."));
    player.sendMessage("");
  }
}
