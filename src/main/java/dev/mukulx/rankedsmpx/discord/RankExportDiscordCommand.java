package dev.mukulx.rankedsmpx.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class RankExportDiscordCommand extends ListenerAdapter {
  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public RankExportDiscordCommand(RankedSMPXPlugin plugin, DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("rankexport", "Export all rank data to a JSON file");
  }

  @Override
  public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
    if (!event.getName().equals("rankexport")) return;

    if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
      event
          .reply("You need Administrator permission to use this command!")
          .setEphemeral(true)
          .queue();
      return;
    }

    event.deferReply().queue();

    dev.mukulx.rankedsmpx.utils.SchedulerUtil.runAsync(
            plugin,
            () -> {
              try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                JsonObject exportData = new JsonObject();
                exportData.addProperty(
                    "export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                exportData.addProperty("plugin_version", plugin.getPluginMeta().getVersion());

                JsonArray ranks = new JsonArray();
                try (Connection conn = plugin.getDatabaseManager().getConnection();
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
                try (Connection conn = plugin.getDatabaseManager().getConnection();
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

                EmbedBuilder embed =
                    new EmbedBuilder()
                        .setTitle("Rank Export Complete")
                        .setColor(Color.GREEN)
                        .addField("Players", String.valueOf(ranks.size()), true)
                        .addField("History Entries", String.valueOf(history.size()), true)
                        .addField("Filename", filename, false)
                        .setFooter("Exported by " + event.getUser().getName())
                        .setTimestamp(new Date().toInstant());

                event
                    .getHook()
                    .sendMessageEmbeds(embed.build())
                    .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(exportFile))
                    .queue();

              } catch (Exception e) {
                plugin
                    .getLogger()
                    .severe("Failed to export rank data via Discord: " + e.getMessage());
                e.printStackTrace();
                event
                    .getHook()
                    .sendMessage("Failed to export rank data: " + e.getMessage())
                    .queue();
              }
            });
  }
}
