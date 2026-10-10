package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.awt.Color;
import java.time.Instant;
import java.util.concurrent.RejectedExecutionException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;

public class UpdatesManager {

  private final RankedSMPXPlugin plugin;
  private final JDA jda;
  private volatile boolean isShuttingDown = false;

  public UpdatesManager(@NotNull RankedSMPXPlugin plugin, @NotNull JDA jda) {
    this.plugin = plugin;
    this.jda = jda;
  }

  private boolean isJDAAvailable() {
    return jda != null
        && jda.getStatus() != JDA.Status.SHUTDOWN
        && jda.getStatus() != JDA.Status.SHUTTING_DOWN
        && !isShuttingDown;
  }

  public void postGameStarted(int playerCount) {
    if (!isJDAAvailable()) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-game-start", true)) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();
    String footerName = plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX");
    embed.setTitle(footerName + " Started!");
    embed.setColor(new Color(0, 255, 0));
    embed.setDescription(
        "**"
            + footerName
            + "** has started with **"
            + playerCount
            + " players**!\n"
            + "Ranks have been randomly assigned. Check `/rankinfo` to see your rank!");
    embed.setFooter(footerName, null);
    embed.setTimestamp(Instant.now());

    try {
      channel
          .sendMessageEmbeds(embed.build())
          .queue(
              success -> {},
              error -> {
                if (!error.getMessage().contains("interrupt")
                    && !error.getMessage().contains("stopped")) {
                  plugin
                      .getLogger()
                      .warning("Failed to post game start update: " + error.getMessage());
                }
              });
    } catch (RejectedExecutionException e) {
      plugin.getLogger().info("Skipping Discord game start message - JDA requester stopped");
    }
  }

  public void postGameStopped() {
    if (!isJDAAvailable()) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-game-end", true)) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();
    String footerName = plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX");
    embed.setTitle(footerName + " Ended");
    embed.setColor(new Color(255, 0, 0));
    embed.setDescription("**" + footerName + "** has ended. All ranks have been reset.");
    embed.setFooter(footerName, null);
    embed.setTimestamp(Instant.now());

    try {
      channel
          .sendMessageEmbeds(embed.build())
          .queue(
              success -> {},
              error -> {
                if (!error.getMessage().contains("interrupt")
                    && !error.getMessage().contains("stopped")) {
                  plugin
                      .getLogger()
                      .warning("Failed to post game stop update: " + error.getMessage());
                }
              });
    } catch (RejectedExecutionException e) {
      plugin.getLogger().info("Skipping Discord game stop message - JDA requester stopped");
    }
  }

  public void postPlayerJoin(@NotNull String playerName) {
    if (!isJDAAvailable()) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-player-join", false)) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Player Joined");
    embed.setColor(new Color(100, 200, 100));
    embed.setDescription("**" + playerName + "** joined the server");
    embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
    embed.setTimestamp(Instant.now());

    try {
      channel
          .sendMessageEmbeds(embed.build())
          .queue(
              success -> {},
              error -> {
                if (!error.getMessage().contains("interrupt")
                    && !error.getMessage().contains("stopped")) {
                  plugin
                      .getLogger()
                      .warning("Failed to post player join update: " + error.getMessage());
                }
              });
    } catch (RejectedExecutionException e) {
      plugin.getLogger().info("Skipping Discord player join message - JDA requester stopped");
    }
  }

  public void postPlayerLeave(@NotNull String playerName) {
    if (!isJDAAvailable()) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-player-leave", false)) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Player Left");
    embed.setColor(new Color(200, 100, 100));
    embed.setDescription("**" + playerName + "** left the server");
    embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
    embed.setTimestamp(Instant.now());

    try {
      channel
          .sendMessageEmbeds(embed.build())
          .queue(
              success -> {},
              error -> {
                if (!error.getMessage().contains("interrupt")
                    && !error.getMessage().contains("stopped")) {
                  plugin
                      .getLogger()
                      .warning("Failed to post player leave update: " + error.getMessage());
                }
              });
    } catch (RejectedExecutionException e) {
      plugin.getLogger().info("Skipping Discord player leave message - JDA requester stopped");
    }
  }

  public void postPlayerDeath(@NotNull String playerName, @NotNull String deathMessage) {
    if (!isJDAAvailable()) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-player-death", false)) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Player Death");
    embed.setColor(new Color(128, 128, 128));
    embed.setDescription(deathMessage);
    embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
    embed.setTimestamp(Instant.now());

    try {
      channel
          .sendMessageEmbeds(embed.build())
          .queue(
              success -> {},
              error -> {
                if (!error.getMessage().contains("interrupt")
                    && !error.getMessage().contains("stopped")) {
                  plugin.getLogger().warning("Failed to post death update: " + error.getMessage());
                }
              });
    } catch (RejectedExecutionException e) {
      plugin.getLogger().info("Skipping Discord death message - JDA requester stopped");
    }
  }

  public void postRankGained(@NotNull String playerName, int rank, @NotNull String reason) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-rank-changes", false)) {
      return;
    }

    postUpdate(playerName, rank, "gained", reason, new Color(0, 255, 0));
  }

  public void postRankLost(
      @NotNull String playerName, int oldRank, int newRank, @NotNull String reason) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-rank-changes", false)) {
      return;
    }

    postUpdate(playerName, oldRank, "lost", reason, new Color(255, 0, 0));
  }

  public void postRankLostToUnranked(
      @NotNull String playerName, int oldRank, @NotNull String reason) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-rank-changes", false)) {
      return;
    }

    postUpdate(playerName, oldRank, "lost", reason + " (now unranked)", new Color(255, 0, 0));
  }

  public void postRankTransferred(@NotNull String fromPlayer, @NotNull String toPlayer, int rank) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-rank-changes", false)) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Rank Transferred");
    embed.setColor(new Color(100, 149, 237));
    embed.setDescription(
        "**" + fromPlayer + "** transferred their **Rank #" + rank + "** to **" + toPlayer + "**");
    embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
    embed.setTimestamp(Instant.now());

    channel
        .sendMessageEmbeds(embed.build())
        .queue(
            success -> plugin.getLogger().info("Posted rank transfer update to Discord"),
            error -> {
              if (!error.getMessage().contains("interrupt")) {
                plugin
                    .getLogger()
                    .warning("Failed to post rank transfer update: " + error.getMessage());
              }
            });
  }

  public void postRankRemoved(@NotNull String playerName, int oldRank, @NotNull String reason) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("updates.announce-rank-changes", false)) {
      return;
    }

    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Rank Removed");
    embed.setColor(new Color(128, 128, 128));
    embed.setDescription(
        "**" + playerName + "** lost their **Rank #" + oldRank + "**\n**Reason:** " + reason);
    embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
    embed.setTimestamp(Instant.now());

    channel
        .sendMessageEmbeds(embed.build())
        .queue(
            success -> plugin.getLogger().info("Posted rank removed update to Discord"),
            error -> {
              if (!error.getMessage().contains("interrupt")) {
                plugin
                    .getLogger()
                    .warning("Failed to post rank removed update: " + error.getMessage());
              }
            });
  }

  private void postUpdate(
      @NotNull String playerName,
      int rank,
      @NotNull String action,
      @NotNull String reason,
      @NotNull Color color) {
    String channelId = plugin.getDiscordConfig().getString("updates.channel-id", "");
    if (channelId.isEmpty()) {
      return;
    }

    TextChannel channel = jda.getTextChannelById(channelId);
    if (channel == null) {
      plugin.getLogger().warning("Updates channel not found: " + channelId);
      return;
    }

    String title = action.equals("gained") ? "Rank Gained" : "Rank Lost";

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle(title);
    embed.setColor(color);
    embed.setDescription(
        "**" + playerName + "** " + action + " **Rank #" + rank + "**\n**Reason:** " + reason);
    embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
    embed.setTimestamp(Instant.now());

    channel
        .sendMessageEmbeds(embed.build())
        .queue(
            success -> plugin.getLogger().info("Posted rank " + action + " update to Discord"),
            error -> {
              if (!error.getMessage().contains("interrupt")) {
                plugin.getLogger().warning("Failed to post rank update: " + error.getMessage());
              }
            });
  }

  public void shutdown() {
    isShuttingDown = true;
    plugin.getLogger().info("Updates manager shutdown");
  }
}
