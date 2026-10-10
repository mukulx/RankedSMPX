package dev.mukulx.rankedsmpx.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RankImportCommand implements CommandExecutor {
  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final RankManager rankManager;
  private final MessageManager messageManager;

  public RankImportCommand(
      RankedSMPXPlugin plugin,
      DatabaseManager databaseManager,
      RankManager rankManager,
      MessageManager messageManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("rankedsmp.import")) {
      sender.sendMessage(messageManager.getMessage("no-permission"));
      return true;
    }

    if (args.length < 1) {
      sender.sendMessage(Component.text("Usage: /rankimport <filename>", NamedTextColor.RED));
      sender.sendMessage(Component.text("Example: /rankimport rank_export_20240315_120000.json", NamedTextColor.GRAY));
      return true;
    }

    String filename = args[0];
    if (!filename.endsWith(".json")) {
      filename += ".json";
    }

    File importFile = new File(plugin.getDataFolder(), filename);

    if (!importFile.exists()) {
      sender.sendMessage(Component.text("File not found: " + filename, NamedTextColor.RED));
      sender.sendMessage(
          Component.text("Place the export file in: " + plugin.getDataFolder().getAbsolutePath(), NamedTextColor.GRAY));
      return true;
    }

    sender.sendMessage(Component.text("Importing rank data from " + filename + "...", NamedTextColor.AQUA));
    sender.sendMessage(
        Component.text("WARNING: This will overwrite existing rank data!", NamedTextColor.RED, TextDecoration.BOLD));

    final File finalFile = importFile;
    SchedulerUtil.runAsync(
            plugin,
            () -> {
              try {
                String content = new String(Files.readAllBytes(finalFile.toPath()));
                JsonObject importData = JsonParser.parseString(content).getAsJsonObject();

                JsonArray ranks = importData.getAsJsonArray("ranks");
                JsonArray history =
                    importData.has("history") ? importData.getAsJsonArray("history") : null;

                int importedRanks = 0;
                int importedHistory = 0;
                String rankUpsertSql =
                    databaseManager.isMySql()
                        ? "INSERT INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), rank=VALUES(rank), last_updated=VALUES(last_updated)"
                        : "INSERT OR REPLACE INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?)";
                String historyInsertSql =
                    databaseManager.isMySql()
                        ? "INSERT INTO rank_history (uuid, player_name, rank, action, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
                        : "INSERT OR REPLACE INTO rank_history (uuid, player_name, rank, action, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?)";

                try (Connection conn = databaseManager.getConnection()) {
                  conn.setAutoCommit(false);

                  try (PreparedStatement stmt =
                      conn.prepareStatement(rankUpsertSql)) {

                    for (int i = 0; i < ranks.size(); i++) {
                      JsonObject rankData = ranks.get(i).getAsJsonObject();
                      stmt.setString(1, rankData.get("uuid").getAsString());
                      stmt.setString(2, rankData.get("player_name").getAsString());
                      stmt.setInt(3, rankData.get("rank").getAsInt());
                      stmt.setLong(4, rankData.get("last_updated").getAsLong());
                      stmt.addBatch();
                      importedRanks++;
                    }
                    stmt.executeBatch();
                  }

                  if (history != null && history.size() > 0) {
                    try (PreparedStatement stmt =
                        conn.prepareStatement(historyInsertSql)) {

                      for (int i = 0; i < history.size(); i++) {
                        JsonObject historyData = history.get(i).getAsJsonObject();
                        stmt.setString(1, historyData.get("uuid").getAsString());
                        stmt.setString(2, historyData.get("player_name").getAsString());
                        stmt.setInt(3, historyData.get("rank").getAsInt());
                        stmt.setString(4, historyData.get("action").getAsString());
                        stmt.setString(5, historyData.get("reason").getAsString());
                        stmt.setLong(6, historyData.get("timestamp").getAsLong());
                        stmt.addBatch();
                        importedHistory++;
                      }
                      stmt.executeBatch();
                    }
                  }

                  conn.commit();
                }

                rankManager.loadRanks();

                final int finalImportedRanks = importedRanks;
                final int finalImportedHistory = importedHistory;

                SchedulerUtil.run(
                        plugin,
                        () -> {
                          sender.sendMessage(
                              Component.text("Rank data imported successfully!", NamedTextColor.GREEN));
                          sender.sendMessage(
                              Component.text("Ranks imported: ", NamedTextColor.GRAY)
                                  .append(Component.text(finalImportedRanks, NamedTextColor.WHITE)));
                          sender.sendMessage(
                              Component.text("History entries: ", NamedTextColor.GRAY)
                                  .append(Component.text(finalImportedHistory, NamedTextColor.WHITE)));
                          sender.sendMessage(
                              Component.text("Ranks have been reloaded from database", NamedTextColor.AQUA));

                          if (finalImportedRanks > 0
                              && !plugin.getGameManager().isGameActive()) {
                            plugin.getGameManager().setGameActive(true);
                            sender.sendMessage(
                                Component.text("Game activated after importing "
                                    + finalImportedRanks
                                    + " ranks", NamedTextColor.GREEN));
                          }
                        });

              } catch (Exception e) {
                plugin.getLogger().severe("Failed to import rank data: " + e.getMessage());
                e.printStackTrace();
                SchedulerUtil.run(
                        plugin,
                        () ->
                            sender.sendMessage(
                                Component.text("Failed to import rank data: " + e.getMessage(), NamedTextColor.RED)));
              }
            });

    return true;
  }
}
