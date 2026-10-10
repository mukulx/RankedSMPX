package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.awt.Color;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LeaderboardManager {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;
  private final JDA jda;
  private Object updateTask;
  private String lastLeaderboardHash;
  private volatile boolean isShuttingDown = false;

  public LeaderboardManager(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager, @NotNull JDA jda) {
    this.plugin = plugin;
    this.discordManager = discordManager;
    this.jda = jda;
    this.lastLeaderboardHash = "";
  }

  public void startUpdating() {
    if (updateTask != null) {
      SchedulerUtil.cancelTask(updateTask);
      updateTask = null;
    }

    int interval = plugin.getDiscordConfig().getInt("leaderboard.update-interval", 300);
    if (interval < 60) {
      interval = 60; // Minimum 60 seconds to avoid rate limits
    }

    long intervalTicks = interval * 20L;

    SchedulerUtil.runLaterAsync(plugin, this::updateLeaderboard, 100L);

    updateTask =
        SchedulerUtil.runTimerAsync(
            plugin, this::updateLeaderboard, intervalTicks, intervalTicks);

    plugin.getLogger().info("Leaderboard auto-update started (every " + interval + " seconds)");
  }

  public void stopUpdating() {
    isShuttingDown = true;
    if (updateTask != null) {
      SchedulerUtil.cancelTask(updateTask);
      updateTask = null;
    }
    plugin.getLogger().info("Leaderboard updates stopped");
  }

  public void updateLeaderboard() {
    if (isShuttingDown) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("leaderboard.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Leaderboard channel not found: " + channelId);
      return;
    }

    MessageEmbed embed = buildLeaderboardEmbed();

    String currentHash = generateLeaderboardHash();
    if (currentHash.equals(lastLeaderboardHash)) {
      return;
    }
    lastLeaderboardHash = currentHash;

    String messageId = plugin.getDiscordConfig().getString("leaderboard.message-id", "");

    if (messageId.isEmpty()) {
      channel
          .sendMessageEmbeds(embed)
          .queue(
              message -> {
                plugin.getDiscordConfig().set("leaderboard.message-id", message.getId());
                plugin.saveDiscordConfig();
                plugin.getLogger().info("Leaderboard message created: " + message.getId());
              },
              error -> {
                if (!error.getMessage().contains("interrupt")) {
                  plugin.getLogger().warning("Failed to send leaderboard: " + error.getMessage());
                }
              });
    } else {
      channel
          .retrieveMessageById(messageId)
          .queue(
              message ->
                  message
                      .editMessageEmbeds(embed)
                      .queue(
                          success -> {},
                          error -> {
                            if (!error.getMessage().contains("interrupt")) {
                              channel
                                  .sendMessageEmbeds(embed)
                                  .queue(
                                      newMessage -> {
                                        plugin
                                            .getDiscordConfig()
                                            .set("leaderboard.message-id", newMessage.getId());
                                        plugin.saveDiscordConfig();
                                      },
                                      sendError -> {
                                        if (!sendError.getMessage().contains("interrupt")) {
                                          plugin
                                              .getLogger()
                                              .warning(
                                                  "Failed to send leaderboard: "
                                                      + sendError.getMessage());
                                        }
                                      });
                            }
                          }),
              error -> {
                if (!error.getMessage().contains("interrupt")) {
                  channel
                      .sendMessageEmbeds(embed)
                      .queue(
                          newMessage -> {
                            plugin
                                .getDiscordConfig()
                                .set("leaderboard.message-id", newMessage.getId());
                            plugin.saveDiscordConfig();
                          },
                          sendError -> {
                            if (!sendError.getMessage().contains("interrupt")) {
                              plugin
                                  .getLogger()
                                  .warning("Failed to send leaderboard: " + sendError.getMessage());
                            }
                          });
                }
              });
    }
  }

  private MessageEmbed buildLeaderboardEmbed() {
    EmbedBuilder builder = new EmbedBuilder();
    String footerName = plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX");
    builder.setTitle(footerName + " Leaderboard");
    builder.setColor(new Color(0, 255, 255));
    builder.setTimestamp(Instant.now());
    builder.setFooter(
        "Auto-updates every "
            + plugin.getDiscordConfig().getInt("leaderboard.update-interval", 300)
            + " seconds");

    Map<UUID, Integer> ranks = plugin.getRankManager().getAllRanks();

    if (ranks.isEmpty()) {
      builder.setDescription(
          "*No players are currently ranked.*\n\nUse `/rsmp start` in-game to begin the season!");
      return builder.build();
    }

    List<Map.Entry<UUID, Integer>> sortedRanks =
        ranks.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());

    int maxPlayers = plugin.getDiscordConfig().getInt("leaderboard.max-players", 20);
    if (maxPlayers > 0 && sortedRanks.size() > maxPlayers) {
      sortedRanks = sortedRanks.subList(0, maxPlayers);
    }

    boolean showUnlinked = plugin.getDiscordConfig().getBoolean("leaderboard.show-unlinked", true);

    StringBuilder description = new StringBuilder();
    description.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

    for (Map.Entry<UUID, Integer> entry : sortedRanks) {
      UUID uuid = entry.getKey();
      int rank = entry.getValue();

      String playerName = plugin.getRankManager().getPlayerName(uuid);
      if (playerName == null || playerName.equals("Unknown")) {
        Player player = Bukkit.getPlayerExact(uuid.toString());
        playerName = player != null ? player.getName() : "Unknown";
      }

      String discordId = discordManager.getDiscordId(uuid);

      if (!showUnlinked && discordId == null) {
        continue;
      }

      String rankEmoji = getRankEmoji(rank);

      description.append(rankEmoji).append(" **").append(playerName).append("**");

      if (discordId != null) {
        description.append(" (<@").append(discordId).append(">)");
      } else {
        description.append(" *(Not Linked)*");
      }

      description
          .append("\n   └ Rank: **#")
          .append(rank)
          .append("**/")
          .append(plugin.getConfigManager().getTotalRanks());
      description.append("\n\n");
    }

    builder.setDescription(description.toString());

    return builder.build();
  }

  private String getRankEmoji(int rank) {
    return switch (rank) {
      case 1 -> "";
      case 2 -> "";
      case 3 -> "";
      default -> "#" + rank;
    };
  }

  private String generateLeaderboardHash() {
    Map<UUID, Integer> ranks = plugin.getRankManager().getAllRanks();

    StringBuilder sb = new StringBuilder();
    ranks.entrySet().stream()
        .sorted(Map.Entry.comparingByValue())
        .forEach(
            entry -> {
              sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
            });

    return sb.toString();
  }
}
