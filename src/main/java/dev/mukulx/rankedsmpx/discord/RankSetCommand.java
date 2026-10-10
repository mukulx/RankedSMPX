package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Map;
import java.util.UUID;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankSetCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public RankSetCommand(@NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    OptionData rankOption =
        new OptionData(OptionType.INTEGER, "rank", "Rank to set (0 = unranked)", true)
            .setMinValue(0);

    return Commands.slash("rankset", "Set a player's rank")
        .addOptions(rankOption)
        .addOption(OptionType.USER, "player", "Player to set rank for (Discord mention)", false)
        .addOption(OptionType.STRING, "username", "Player's Minecraft username", false)
        .setDefaultPermissions(
            net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(
                net.dv8tion.jda.api.Permission.ADMINISTRATOR));
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("rankset")) {
      return;
    }

    handleRankSet(event, null);
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    String message = event.getMessage().getContentRaw();
    if (!message.startsWith("?rankset")) {
      return;
    }

    handleRankSet(null, event);
  }

  private void handleRankSet(
      SlashCommandInteractionEvent slashEvent, MessageReceivedEvent textEvent) {
    boolean isSlash = slashEvent != null;

    var member = isSlash ? slashEvent.getMember() : textEvent.getMember();
    if (member == null || !member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
      String errorMsg = "You don't have permission to use this command!";
      if (isSlash) {
        slashEvent.reply(errorMsg).setEphemeral(true).queue();
      } else {
        textEvent.getMessage().reply(errorMsg).queue();
      }
      return;
    }

    User mentionedUser = null;
    String username = null;
    int rank = -1;

    if (isSlash) {
      mentionedUser =
          slashEvent.getOption("player") != null
              ? slashEvent.getOption("player").getAsUser()
              : null;
      username =
          slashEvent.getOption("username") != null
              ? slashEvent.getOption("username").getAsString()
              : null;
      rank = slashEvent.getOption("rank").getAsInt();
    } else {
      String[] args = textEvent.getMessage().getContentRaw().split("\s+");
      if (args.length < 3) {
        textEvent
            .getMessage()
            .reply(
                "Usage: `?rankset @mention <rank>` or `?rankset <username> <rank>`\nRank: a number, or 0 for unranked")
            .queue();
        return;
      }

      if (!textEvent.getMessage().getMentions().getUsers().isEmpty()) {
        mentionedUser = textEvent.getMessage().getMentions().getUsers().get(0);
      } else {
        username = args[1];
      }

      try {
        rank = Integer.parseInt(args[2]);
      } catch (NumberFormatException e) {
        textEvent
            .getMessage()
            .reply("Invalid rank! Use a number or 0 for unranked")
            .queue();
        return;
      }
    }

    if (mentionedUser == null && username == null) {
      String errorMsg = "You must specify either a Discord user or Minecraft username!";
      if (isSlash) {
        slashEvent.reply(errorMsg).setEphemeral(true).queue();
      } else {
        textEvent.getMessage().reply(errorMsg).queue();
      }
      return;
    }

    int totalRanks = plugin.getConfigManager().getTotalRanks();
    if (rank < 0 || rank > totalRanks) {
      String errorMsg = "Invalid rank! Use 0-" + totalRanks + " (0 = unranked)";
      if (isSlash) {
        slashEvent.reply(errorMsg).setEphemeral(true).queue();
      } else {
        textEvent.getMessage().reply(errorMsg).queue();
      }
      return;
    }

    if (isSlash) {
      slashEvent.deferReply().queue();
    }

    UUID uuid = null;
    String playerName = null;

    if (mentionedUser != null) {
      uuid = getUUIDFromDiscordId(mentionedUser.getId());
      if (uuid == null) {
        String errorMsg = mentionedUser.getAsMention() + " is not linked!";
        if (isSlash) {
          slashEvent.getHook().sendMessage(errorMsg).queue();
        } else {
          textEvent.getMessage().reply(errorMsg).queue();
        }
        return;
      }
      playerName = getUsername(uuid);
    } else {
      Player player = Bukkit.getPlayerExact(username);
      if (player != null) {
        uuid = player.getUniqueId();
        playerName = player.getName();
      } else {
        String errorMsg = "Player `" + username + "` not found or not online!";
        if (isSlash) {
          slashEvent.getHook().sendMessage(errorMsg).queue();
        } else {
          textEvent.getMessage().reply(errorMsg).queue();
        }
        return;
      }
    }

    if (uuid == null) {
      String errorMsg = "Could not find player!";
      if (isSlash) {
        slashEvent.getHook().sendMessage(errorMsg).queue();
      } else {
        textEvent.getMessage().reply(errorMsg).queue();
      }
      return;
    }

    final UUID finalUuid = uuid;
    final String finalPlayerName = playerName;
    final int finalRank = rank;
    final boolean finalIsSlash = isSlash;

    SchedulerUtil.run(
        plugin,
        () -> {
          if (finalRank == 0) {
            Integer oldRank = plugin.getRankManager().getRank(finalUuid);
            plugin.getRankManager().removeRank(finalUuid);

            Player onlinePlayer = Bukkit.getPlayer(finalUuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
              SchedulerUtil.runForEntity(
                  plugin,
                  onlinePlayer,
                  () -> {
                    if (!onlinePlayer.isOnline()) return;
                    plugin.getHealthManager().resetHealth(onlinePlayer);
                    plugin.getInventoryManager().removeExtraInventory(finalUuid);
                    String removeTitle = plugin.getMessageManager().colorize("&c&lUNRANKED ");
                    String removeSubtitle =
                        plugin.getMessageManager().getMessage("rank.change.subtitle-lost");
                    plugin.getMessageManager().sendTitle(onlinePlayer, removeTitle, removeSubtitle, 10, 70, 20);
                  });
            } else {
              plugin.getInventoryManager().removeExtraInventory(finalUuid);
            }

            plugin.getHierarchyHammerManager().updateHammerHolder();

            String successMsg = "Removed rank from **" + finalPlayerName + "** (now unranked)";
            if (finalIsSlash) {
              slashEvent.getHook().sendMessage(successMsg).queue();
            } else {
              textEvent.getMessage().reply(successMsg).queue();
            }

            if (oldRank != null) {
              Map<String, String> broadcastPlaceholders =
                  Map.of(
                      "player",
                      finalPlayerName,
                      "rank",
                      "0",
                      "old_rank",
                      String.valueOf(oldRank),
                      "reason",
                      "Removed by admin via Discord");
              String broadcast =
                  plugin
                      .getMessageManager()
                      .getMessage("rank.change.broadcast-lost", broadcastPlaceholders);
              Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));
            }

            if (oldRank != null
                && plugin
                    .getDiscordConfig()
                    .getBoolean("notifications.notify-on-admin-change", true)) {
              var notificationManager = plugin.getDiscordManager().getNotificationManager();
              if (notificationManager != null) {
                notificationManager.sendUnrankedDM(
                    finalUuid, oldRank, "Removed by admin via Discord");
              }

              var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
              if (roleSyncManager != null) {
                roleSyncManager.syncPlayerRole(finalUuid, null);
              }

              var updatesManager = plugin.getDiscordManager().getUpdatesManager();
              if (updatesManager != null) {
                updatesManager.postRankRemoved(finalPlayerName, oldRank, "Removed by admin");
              }
            }
          } else {
            if (plugin.getDatabaseManager().isBlacklisted(finalUuid)) {
              String errorMsg =
                  "Cannot set rank for **"
                      + finalPlayerName
                      + "**! They are blacklisted from receiving ranks.";
              if (finalIsSlash) {
                slashEvent.getHook().sendMessage(errorMsg).queue();
              } else {
                textEvent.getMessage().reply(errorMsg).queue();
              }
              return;
            }

            UUID currentHolder = null;
            for (var entry : plugin.getRankManager().getAllRanks().entrySet()) {
              if (entry.getValue() == finalRank) {
                currentHolder = entry.getKey();
                break;
              }
            }

            if (currentHolder != null && !currentHolder.equals(finalUuid)) {
              final UUID oldHolderUUID = currentHolder;
              OfflinePlayer oldHolder = Bukkit.getOfflinePlayer(oldHolderUUID);
              plugin.getRankManager().removeRank(oldHolderUUID);

              String oldHolderName = oldHolder.getName() != null ? oldHolder.getName() : "Unknown";
              plugin
                  .getLogger()
                  .info(
                      "Removed rank #"
                          + finalRank
                          + " from "
                          + oldHolderName
                          + " (Discord admin reassignment)");

              Player oldHolderOnline = oldHolder.getPlayer();
              if (oldHolderOnline != null && oldHolderOnline.isOnline()) {
                SchedulerUtil.runForEntity(
                    plugin,
                    oldHolderOnline,
                    () -> {
                      if (!oldHolderOnline.isOnline()) return;
                      plugin.getHealthManager().resetHealth(oldHolderOnline);
                      plugin.getInventoryManager().removeExtraInventory(oldHolderUUID);

                      if (plugin.getTabIntegration() != null) {
                        plugin.getTabIntegration().updatePlayer(oldHolderOnline);
                      }
                      if (plugin.getVanillaTabManager() != null) {
                        plugin.getVanillaTabManager().updatePlayer(oldHolderOnline);
                      }

                      oldHolderOnline.sendMessage(
                          plugin.getMessageManager().getPrefix()
                              + "§cYour rank #"
                              + finalRank
                              + " was taken by an admin via Discord and given to §b"
                              + finalPlayerName);
                    });
              } else {
                plugin.getInventoryManager().removeExtraInventory(currentHolder);
              }
            }

            plugin.getRankManager().setRank(finalUuid, finalRank);

            if (!plugin.getGameManager().isGameActive()) {
              plugin.getGameManager().setGameActive(true);
              plugin.getLogger().info("Game activated via Discord rank assignment");
            }

            Player onlinePlayer = Bukkit.getPlayer(finalUuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
              SchedulerUtil.runForEntity(
                  plugin,
                  onlinePlayer,
                  () -> {
                    if (!onlinePlayer.isOnline()) return;
                    plugin.getHealthManager().applyHealth(onlinePlayer);
                    plugin.getInventoryManager().removeExtraInventory(finalUuid);

                    if (plugin.getTabIntegration() != null) {
                      plugin.getTabIntegration().updatePlayer(onlinePlayer);
                    }
                    if (plugin.getVanillaTabManager() != null) {
                      plugin.getVanillaTabManager().updatePlayer(onlinePlayer);
                    }

                    Map<String, String> titlePlaceholders = Map.of("rank", String.valueOf(finalRank));
                    String gainTitle =
                        plugin
                            .getMessageManager()
                            .getMessage("rank.change.title-gained", titlePlaceholders);
                    String gainSubtitle =
                        plugin.getMessageManager().getMessage("rank.change.subtitle-gained");
                    plugin.getMessageManager().sendTitle(onlinePlayer, gainTitle, gainSubtitle, 10, 70, 20);
                  });
            } else {
              plugin.getInventoryManager().removeExtraInventory(finalUuid);
            }

            plugin.getHierarchyHammerManager().updateHammerHolder();

            String successMsg = "Set **" + finalPlayerName + "** to rank **#" + finalRank + "**";
            if (finalIsSlash) {
              slashEvent.getHook().sendMessage(successMsg).queue();
            } else {
              textEvent.getMessage().reply(successMsg).queue();
            }

            Map<String, String> broadcastPlaceholders =
                Map.of(
                    "player",
                    finalPlayerName,
                    "rank",
                    String.valueOf(finalRank),
                    "reason",
                    "Set by admin via Discord");
            String broadcast =
                plugin
                    .getMessageManager()
                    .getMessage("rank.change.broadcast-gained", broadcastPlaceholders);
            Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

            if (plugin
                .getDiscordConfig()
                    .getBoolean("notifications.notify-on-admin-change", true)) {
              var notificationManager = plugin.getDiscordManager().getNotificationManager();
              if (notificationManager != null) {
                notificationManager.sendRankGainedDM(
                    finalUuid, finalRank, "Set by admin via Discord");
              }

              var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
              if (roleSyncManager != null) {
                roleSyncManager.syncPlayerRole(finalUuid, finalRank);
              }

              var updatesManager = plugin.getDiscordManager().getUpdatesManager();
              if (updatesManager != null) {
                updatesManager.postRankGained(finalPlayerName, finalRank, "Set by admin");
              }
            }
          }

          if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
            plugin.getDiscordManager().forceUpdate();
          }
        });
  }

  @NotNull
  private String getUsername(@NotNull UUID uuid) {
    String username = discordManager.getLinkManager().getUsername(uuid);
    if (username != null) {
      return username;
    }

    username = plugin.getRankManager().getPlayerName(uuid);
    if (username != null && !username.equals("Unknown")) {
      return username;
    }

    Player player = Bukkit.getPlayer(uuid);
    if (player != null) {
      return player.getName();
    }

    return "Unknown";
  }

  private UUID getUUIDFromDiscordId(String discordId) {
    return discordManager.getMinecraftUUID(discordId);
  }
}
