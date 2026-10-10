package dev.mukulx.rankedsmpx.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.awt.Color;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class RankImportDiscordCommand extends ListenerAdapter {
  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public RankImportDiscordCommand(RankedSMPXPlugin plugin, DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("rankimport", "Import rank data from a JSON file")
        .addOption(OptionType.ATTACHMENT, "file", "The rank export JSON file to import", true);
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals("rankimport")) return;

    if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
      event
          .reply("You need Administrator permission to use this command!")
          .setEphemeral(true)
          .queue();
      return;
    }

    var attachment = event.getOption("file").getAsAttachment();

    if (!attachment.getFileExtension().equals("json")) {
      event.reply("Please provide a valid JSON file!").setEphemeral(true).queue();
      return;
    }

    event.deferReply().queue();

    dev.mukulx.rankedsmpx.utils.SchedulerUtil.runAsync(
            plugin,
            () -> {
              try {
                URL url = java.net.URI.create(attachment.getUrl()).toURL();
                InputStream is = url.openStream();
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                is.close();

                JsonObject importData = JsonParser.parseString(content).getAsJsonObject();
                JsonArray ranks = importData.getAsJsonArray("ranks");
                JsonArray history =
                    importData.has("history") ? importData.getAsJsonArray("history") : null;

                int importedRanks = 0;
                int importedHistory = 0;
                boolean mySql = plugin.getDatabaseManager().isMySql();
                String rankUpsertSql =
                    mySql
                        ? "INSERT INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name=VALUES(player_name), rank=VALUES(rank), last_updated=VALUES(last_updated)"
                        : "INSERT OR REPLACE INTO ranks (uuid, player_name, rank, last_updated) VALUES (?, ?, ?, ?)";
                String historyInsertSql =
                    mySql
                        ? "INSERT INTO rank_history (uuid, player_name, rank, action, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?)"
                        : "INSERT OR REPLACE INTO rank_history (uuid, player_name, rank, action, reason, timestamp) VALUES (?, ?, ?, ?, ?, ?)";

                try (Connection conn = plugin.getDatabaseManager().getConnection()) {
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

                plugin.getRankManager().loadRanks();

                EmbedBuilder embed =
                    new EmbedBuilder()
                        .setTitle("Rank Import Complete")
                        .setColor(Color.GREEN)
                        .addField("Ranks Imported", String.valueOf(importedRanks), true)
                        .addField("History Entries", String.valueOf(importedHistory), true)
                        .addField("Source File", attachment.getFileName(), false)
                        .setDescription("All existing rank data has been overwritten!")
                        .setFooter("Imported by " + event.getUser().getName())
                        .setTimestamp(new Date().toInstant());

                event.getHook().sendMessageEmbeds(embed.build()).queue();

              } catch (Exception e) {
                plugin
                    .getLogger()
                    .severe("Failed to import rank data via Discord: " + e.getMessage());
                e.printStackTrace();
                event
                    .getHook()
                    .sendMessage("Failed to import rank data: " + e.getMessage())
                    .queue();
              }
            });
  }
}
