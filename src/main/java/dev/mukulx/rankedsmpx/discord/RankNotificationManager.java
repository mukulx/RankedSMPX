package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.awt.Color;
import java.util.UUID;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankNotificationManager {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;
  private final JDA jda;
  private volatile boolean isShuttingDown = false;

  public RankNotificationManager(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager, @NotNull JDA jda) {
    this.plugin = plugin;
    this.discordManager = discordManager;
    this.jda = jda;
  }

  public void sendRankGainedDM(@NotNull UUID uuid, int rank, @NotNull String reason) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("notifications.enabled", true)) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("notifications.notify-on-gain", true)) {
      return;
    }

    String discordId = getDiscordIdFromUUID(uuid);
    if (discordId == null || !isValidDiscordId(discordId)) {
      if (discordId != null) {
        plugin
            .getLogger()
            .warning("Invalid Discord ID format: " + discordId + " - skipping DM notification");
      }
      return;
    }

    plugin
        .getLogger()
        .info("Sending rank gained DM to Discord ID: " + discordId + " for rank #" + rank);

    jda.retrieveUserById(discordId)
        .queue(
            user -> {
              if (isShuttingDown) {
                return;
              }

              plugin.getLogger().info("Retrieved Discord user: " + user.getName());

              String heartEmoji = "";
              String potionEmoji = "";
              String xpEmoji = "";
              String inventoryEmoji =
                  "";
              String rankUpEmoji = "";

              var healthManager = plugin.getHealthManager();
              var potionManager = plugin.getPotionManager();
              var xpManager = plugin.getXPManager();
              var configManager = plugin.getConfigManager();

              if (healthManager == null
                  || potionManager == null
                  || xpManager == null
                  || configManager == null) {
                plugin.getLogger().warning("Cannot send DM: One or more managers are null");
                return;
              }

              double hearts = healthManager.calculateHearts(rank);
              double potionMultiplier = potionManager.calculateMultiplier(rank);
              double xpMultiplier = xpManager.calculateMultiplier(rank);
              int extraSlots = configManager.getExtraSlots(rank);

              EmbedBuilder embed = new EmbedBuilder();
              embed.setTitle(rankUpEmoji + " You received Rank #" + rank + "!");
              embed.setColor(getRankColor(rank));

              StringBuilder description = new StringBuilder();
              description.append("**Buffs & Perks:**\n");
              description
                  .append(heartEmoji)
                  .append(" Hearts: `")
                  .append(String.format("%.1f", hearts))
                  .append("`\n");
              description
                  .append(potionEmoji)
                  .append(" Potion Duration: `")
                  .append(String.format("%.2fx", potionMultiplier))
                  .append("`\n");
              description
                  .append(xpEmoji)
                  .append(" XP Multiplier: `")
                  .append(String.format("%.2fx", xpMultiplier))
                  .append("`\n");

              if (extraSlots > 0) {
                description
                    .append(inventoryEmoji)
                    .append(" Extra Inventory: `")
                    .append(extraSlots)
                    .append(" slots`\n");
              }

              description.append("\n**Reason:** ").append(reason);

              embed.setDescription(description.toString());
              embed.setFooter(discordManager.getEmbedFooterName(), null);
              embed.setTimestamp(java.time.Instant.now());

              user.openPrivateChannel()
                  .queue(
                      channel -> {
                        if (!isShuttingDown) {
                          plugin.getLogger().info("Opened DM channel, sending message...");
                          channel
                              .sendMessageEmbeds(embed.build())
                              .queue(
                                  success ->
                                      plugin
                                          .getLogger()
                                          .info("Successfully sent DM to " + user.getName()),
                                  error -> {
                                    if (!error.getMessage().contains("interrupt")) {
                                      plugin
                                          .getLogger()
                                          .warning(
                                              "Failed to send DM to "
                                                  + user.getName()
                                                  + ": "
                                                  + error.getMessage());
                                    }
                                  });
                        }
                      },
                      error -> {
                        if (!error.getMessage().contains("interrupt")) {
                          plugin
                              .getLogger()
                              .warning(
                                  "Failed to open DM channel for "
                                      + user.getName()
                                      + ": "
                                      + error.getMessage());
                        }
                      });
            },
            error -> {
              if (!error.getMessage().contains("interrupt")) {
                plugin
                    .getLogger()
                    .warning(
                        "Failed to retrieve Discord user " + discordId + ": " + error.getMessage());
              }
            });
  }

  public void sendRankLostDM(@NotNull UUID uuid, int oldRank, int newRank, @NotNull String reason) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("notifications.enabled", true)) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("notifications.notify-on-loss", true)) {
      return;
    }

    String discordId = getDiscordIdFromUUID(uuid);
    if (discordId == null || !isValidDiscordId(discordId)) {
      return;
    }

    jda.retrieveUserById(discordId)
        .queue(
            user -> {
              if (isShuttingDown) {
                return;
              }

              String heartEmoji = "";
              String potionEmoji = "";
              String xpEmoji = "";
              String inventoryEmoji =
                  "";
              String rankDownEmoji = "";

              var healthManager = plugin.getHealthManager();
              var potionManager = plugin.getPotionManager();
              var xpManager = plugin.getXPManager();
              var configManager = plugin.getConfigManager();

              if (healthManager == null
                  || potionManager == null
                  || xpManager == null
                  || configManager == null) {
                plugin.getLogger().warning("Cannot send DM: One or more managers are null");
                return;
              }

              double hearts = healthManager.calculateHearts(newRank);
              double potionMultiplier = potionManager.calculateMultiplier(newRank);
              double xpMultiplier = xpManager.calculateMultiplier(newRank);
              int extraSlots = configManager.getExtraSlots(newRank);

              EmbedBuilder embed = new EmbedBuilder();
              embed.setTitle(rankDownEmoji + " You lost your Rank #" + oldRank + "!");
              embed.setColor(Color.RED);

              StringBuilder description = new StringBuilder();
              description.append("**Your new rank:** #").append(newRank).append("\\n\\n");
              description.append("**New Buffs:**\\n");
              description
                  .append(heartEmoji)
                  .append(" Hearts: `")
                  .append(String.format("%.1f", hearts))
                  .append("`\\n");
              description
                  .append(potionEmoji)
                  .append(" Potion Duration: `")
                  .append(String.format("%.2fx", potionMultiplier))
                  .append("`\\n");
              description
                  .append(xpEmoji)
                  .append(" XP Multiplier: `")
                  .append(String.format("%.2fx", xpMultiplier))
                  .append("`\\n");

              if (extraSlots > 0) {
                description
                    .append(inventoryEmoji)
                    .append(" Extra Inventory: `")
                    .append(extraSlots)
                    .append(" slots`\n");
              } else {
                description.append(inventoryEmoji).append(" Extra Inventory: `None`\n");
              }

              description.append("\n**Reason:** ").append(reason);

              embed.setDescription(description.toString());
              embed.setFooter(discordManager.getEmbedFooterName(), null);
              embed.setTimestamp(java.time.Instant.now());

              user.openPrivateChannel()
                  .queue(
                      channel -> {
                        if (!isShuttingDown) {
                          channel
                              .sendMessageEmbeds(embed.build())
                              .queue(
                                  success -> {},
                                  error -> {
                                    if (!error.getMessage().contains("interrupt")) {
                                      plugin
                                          .getLogger()
                                          .warning(
                                              "Failed to send DM to "
                                                  + user.getName()
                                                  + ": "
                                                  + error.getMessage());
                                    }
                                  });
                        }
                      },
                      error -> {
                        if (!error.getMessage().contains("interrupt")) {
                          plugin
                              .getLogger()
                              .warning("Failed to open DM channel for " + user.getName());
                        }
                      });
            },
            error -> {
              if (!error.getMessage().contains("interrupt")) {
                plugin.getLogger().warning("Failed to retrieve user: " + error.getMessage());
              }
            });
  }

  public void sendUnrankedDM(@NotNull UUID uuid, int oldRank, @NotNull String reason) {
    if (isShuttingDown) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("notifications.enabled", true)) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("notifications.notify-on-loss", true)) {
      return;
    }

    String discordId = getDiscordIdFromUUID(uuid);
    if (discordId == null || !isValidDiscordId(discordId)) {
      return;
    }

    jda.retrieveUserById(discordId)
        .queue(
            user -> {
              if (isShuttingDown) {
                return;
              }

              String heartEmoji = "";
              String potionEmoji = "";
              String xpEmoji = "";
              String inventoryEmoji =
                  "";
              String rankDownEmoji = "";

              double unrankedHearts = plugin.getConfigManager().getRanks().unranked().hearts();

              EmbedBuilder embed = new EmbedBuilder();
              embed.setTitle(rankDownEmoji + " You lost your Rank #" + oldRank + "!");
              embed.setColor(Color.DARK_GRAY);

              StringBuilder description = new StringBuilder();
              description.append("**You are now unranked.**\\n\\n");
              description.append("**Stats:**\\n");
              description
                  .append(heartEmoji)
                  .append(" Hearts: `")
                  .append(String.format("%.1f", unrankedHearts))
                  .append("`\\n");
              description.append(potionEmoji).append(" Potion Duration: `1.0x`\\n");
              description.append(xpEmoji).append(" XP Multiplier: `1.0x`\\n");
              description.append(inventoryEmoji).append(" Extra Inventory: `None`\\n");
              description.append("\\n**Reason:** ").append(reason);

              embed.setDescription(description.toString());
              embed.setFooter(discordManager.getEmbedFooterName(), null);
              embed.setTimestamp(java.time.Instant.now());

              user.openPrivateChannel()
                  .queue(
                      channel -> {
                        if (!isShuttingDown) {
                          channel
                              .sendMessageEmbeds(embed.build())
                              .queue(
                                  success -> {},
                                  error -> {
                                    if (!error.getMessage().contains("interrupt")) {
                                      plugin
                                          .getLogger()
                                          .warning(
                                              "Failed to send DM to "
                                                  + user.getName()
                                                  + ": "
                                                  + error.getMessage());
                                    }
                                  });
                        }
                      },
                      error -> {
                        if (!error.getMessage().contains("interrupt")) {
                          plugin
                              .getLogger()
                              .warning("Failed to open DM channel for " + user.getName());
                        }
                      });
            },
            error -> {
              if (!error.getMessage().contains("interrupt")) {
                plugin.getLogger().warning("Failed to retrieve user: " + error.getMessage());
              }
            });
  }

  public void sendRankLostToUnrankedDM(@NotNull UUID uuid, int oldRank, @NotNull String reason) {
    sendUnrankedDM(uuid, oldRank, reason);
  }

  public void shutdown() {
    isShuttingDown = true;
    plugin.getLogger().info("Notification manager shutdown");
  }

  @Nullable
  private String getDiscordIdFromUUID(UUID uuid) {
    return discordManager.getDiscordId(uuid);
  }

  private boolean isValidDiscordId(String discordId) {
    if (discordId == null || discordId.isEmpty()) {
      return false;
    }

    if (!discordId.matches("\\d{17,19}")) {
      return false;
    }

    try {
      Long.parseLong(discordId);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private Color getRankColor(int rank) {
    if (rank == 1) return new Color(255, 215, 0);
    if (rank == 2) return new Color(192, 192, 192);
    if (rank == 3) return new Color(205, 127, 50);
    if (rank <= 10) return new Color(0, 255, 255);
    return new Color(128, 128, 128);
  }
}
