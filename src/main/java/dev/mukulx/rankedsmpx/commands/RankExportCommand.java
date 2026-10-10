package dev.mukulx.rankedsmpx.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankExportCommand implements CommandExecutor {
  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final MessageManager messageManager;

  public RankExportCommand(
      RankedSMPXPlugin plugin, DatabaseManager databaseManager, MessageManager messageManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("rankedsmp.export")) {
      sender.sendMessage(messageManager.getMessage("no-permission"));
      return true;
    }

    sender.sendMessage(Component.text("Exporting rank data...", NamedTextColor.AQUA));

    SchedulerUtil.runAsync(
            plugin,
            () -> {
              try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                JsonObject exportData = new JsonObject();
                exportData.addProperty(
                    "export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                exportData.addProperty("plugin_version", plugin.getPluginMeta().getVersion());

                JsonArray ranks = new JsonArray();
                try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt =
                        conn.prepareStatement(
                            "SELECT uuid, player_name, rank, last_updated FROM ranks ORDER BY rank DESC")) {

                  ResultSet rs = stmt.executeQuery();
                  while (rs.next()) {
                    JsonObject rankData = new JsonObject();
                    rankData.addProperty("uuid", rs.getString("uuid"));
                    rankData.addProperty("player_name", rs.getString("player_name"));
                    rankData.addProperty("rank", rs.getInt("rank"));
                    rankData.addProperty("last_updated", rs.getLong("last_updated"));
                    ranks.add(rankData);
                  }
                }
                exportData.add("ranks", ranks);

                JsonArray history = new JsonArray();
                try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt =
                        conn.prepareStatement(
                            "SELECT uuid, player_name, rank, action, reason, timestamp FROM rank_history ORDER BY timestamp DESC LIMIT 1000")) {

                  ResultSet rs = stmt.executeQuery();
                  while (rs.next()) {
                    JsonObject historyData = new JsonObject();
                    historyData.addProperty("uuid", rs.getString("uuid"));
                    historyData.addProperty("player_name", rs.getString("player_name"));
                    historyData.addProperty("rank", rs.getInt("rank"));
                    historyData.addProperty("action", rs.getString("action"));
                    historyData.addProperty("reason", rs.getString("reason"));
                    historyData.addProperty("timestamp", rs.getLong("timestamp"));
                    history.add(historyData);
                  }
                }
                exportData.add("history", history);

                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String filename = "rank_export_" + timestamp + ".json";
                File exportFile = new File(plugin.getDataFolder(), filename);

                try (FileWriter writer = new FileWriter(exportFile)) {
                  writer.write(gson.toJson(exportData));
                }

                SchedulerUtil.run(
                        plugin,
                        () -> {
                          sender.sendMessage(
                              Component.text("Rank data exported successfully!", NamedTextColor.GREEN));
                          sender.sendMessage(
                              Component.text("File: ", NamedTextColor.GRAY)
                                  .append(Component.text(filename, NamedTextColor.WHITE)));
                          sender.sendMessage(
                              Component.text("Players: ", NamedTextColor.GRAY)
                                  .append(Component.text(ranks.size(), NamedTextColor.WHITE)));
                          sender.sendMessage(
                              Component.text("History entries: ", NamedTextColor.GRAY)
                                  .append(Component.text(history.size(), NamedTextColor.WHITE)));

                          if (sender instanceof Player) {
                            sender.sendMessage(Component.text("Uploading to Discord...", NamedTextColor.AQUA));
                            uploadToDiscord((Player) sender, exportFile, filename);
                          }
                        });

              } catch (Exception e) {
                plugin.getLogger().severe("Failed to export rank data: " + e.getMessage());
                e.printStackTrace();
                SchedulerUtil.run(
                        plugin,
                        () ->
                            sender.sendMessage(
                                Component.text("Failed to export rank data: " + e.getMessage(), NamedTextColor.RED)));
              }
            });

    return true;
  }

  private void uploadToDiscord(Player player, File file, String filename) {
    if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isEnabled()) {
      player.sendMessage(Component.text("Discord integration is not enabled", NamedTextColor.RED));
      return;
    }

    try {
      var bot = plugin.getDiscordManager().getBot();
      if (bot == null) {
        player.sendMessage(Component.text("Discord bot is not available", NamedTextColor.RED));
        return;
      }

      String discordId = null;
      if (plugin.getDiscordManager() != null) {
        discordId = plugin.getDiscordManager().getLinkManager().getDiscordId(player.getUniqueId());
      }

      if (discordId == null) {
        player.sendMessage(
            Component.text("You need to link your Discord account to use this feature!", NamedTextColor.RED));
        player.sendMessage(Component.text("Use the Discord commands instead: /rankexport", NamedTextColor.GRAY));
        return;
      }

      bot.retrieveUserById(discordId)
          .queue(
              user -> {
                user.openPrivateChannel()
                    .queue(
                        privateChannel -> {
                          privateChannel
                              .sendMessage("**Rank Export** from in-game")
                              .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(file))
                              .queue(
                                  success ->
                                      player.sendMessage(
                                          Component.text("Export file sent to your Discord DMs!", NamedTextColor.GREEN)),
                                  error -> {
                                    player.sendMessage(
                                        Component.text("Failed to send DM. Make sure your DMs are open!", NamedTextColor.RED));
                                    player.sendMessage(
                                        Component.text("Use /rankexport in Discord instead.", NamedTextColor.GRAY));
                                  });
                        },
                        error -> {
                          player.sendMessage(
                              Component.text("Could not open DM channel with you!", NamedTextColor.RED));
                          player.sendMessage(
                              Component.text("Use /rankexport in Discord instead.", NamedTextColor.GRAY));
                        });
              },
              error -> {
                player.sendMessage(Component.text("Could not find your Discord account!", NamedTextColor.RED));
              });

    } catch (Exception e) {
      player.sendMessage(Component.text("Error uploading to Discord: " + e.getMessage(), NamedTextColor.RED));
      plugin.getLogger().severe("Error uploading export to Discord: " + e.getMessage());
    }
  }
}
