package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankRandomCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public RankRandomCommand(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("rankrandom", "Assign a rank to a random player with roulette animation")
        .addOption(OptionType.INTEGER, "rank", "Rank number to assign", true)
        .addOption(
            OptionType.BOOLEAN,
            "include_ranked",
            "Include ranked players (they can only win better ranks)",
            false)
        .addOption(OptionType.BOOLEAN, "include_offline", "Include offline players", false)
        .setDefaultPermissions(
            net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(
                net.dv8tion.jda.api.Permission.ADMINISTRATOR));
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("rankrandom")) {
      return;
    }

    int rank = event.getOption("rank").getAsInt();

    boolean includeRanked =
        event.getOption("include_ranked") != null
            ? event.getOption("include_ranked").getAsBoolean()
            : false;
    boolean includeOffline =
        event.getOption("include_offline") != null
            ? event.getOption("include_offline").getAsBoolean()
            : false;

    handleRankRandom(event, null, rank, includeRanked, includeOffline);
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    String message = event.getMessage().getContentRaw();
    if (!message.startsWith("?rankrandom")) {
      return;
    }

    String[] args = message.split("\\s+");
    if (args.length < 2) {
      event
          .getMessage()
          .reply(
              "Usage: `?rankrandom <rank> [-i] [-o]`\n`-i` = include ranked, `-o` = include offline")
          .queue();
      return;
    }

    var member = event.getMember();
    if (member == null || !member.hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
      event.getMessage().reply("You don't have permission to use this command!").queue();
      return;
    }

    try {
      int rank = Integer.parseInt(args[1]);
      boolean includeRanked = false;
      boolean includeOffline = false;

      for (int i = 2; i < args.length; i++) {
        if (args[i].equalsIgnoreCase("-i") || args[i].equalsIgnoreCase("--include-ranked")) {
          includeRanked = true;
        } else if (args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("--offline")) {
          includeOffline = true;
        }
      }

      handleRankRandom(null, event, rank, includeRanked, includeOffline);
    } catch (NumberFormatException e) {
      event.getMessage().reply("Invalid rank number: `" + args[1] + "`").queue();
    }
  }

  private void handleRankRandom(
      SlashCommandInteractionEvent slashEvent,
      MessageReceivedEvent textEvent,
      int rank,
      boolean includeRanked,
      boolean includeOffline) {
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

    int totalRanks = plugin.getConfigManager().getTotalRanks();
    if (rank < 1 || rank > totalRanks) {
      String errorMsg = "Rank must be between 1 and " + totalRanks + "!";
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

    SchedulerUtil.run(
        plugin,
        () -> {
          List<org.bukkit.OfflinePlayer> playerPool = new ArrayList<>();

          if (includeOffline) {
            for (org.bukkit.OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
              if (offlinePlayer.getName() != null && offlinePlayer.hasPlayedBefore()) {
                if (isEligibleForRank(offlinePlayer.getUniqueId(), rank, includeRanked)) {
                  playerPool.add(offlinePlayer);
                }
              }
            }
          } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
              if (isEligibleForRank(onlinePlayer.getUniqueId(), rank, includeRanked)) {
                playerPool.add(onlinePlayer);
              }
            }
          }

          if (playerPool.isEmpty()) {
            String errorMsg =
                "No eligible players found!\n"
                    + "Try using flags: `include_ranked: true` or `include_offline: true`";
            if (isSlash) {
              slashEvent.getHook().sendMessage(errorMsg).queue();
            } else {
              textEvent.getMessage().reply(errorMsg).queue();
            }
            return;
          }

          Random random = new Random();
          org.bukkit.OfflinePlayer winner = playerPool.get(random.nextInt(playerPool.size()));
          UUID winnerUUID = winner.getUniqueId();

          UUID currentHolder = null;
          for (var entry : plugin.getRankManager().getAllRanks().entrySet()) {
            if (entry.getValue() == rank) {
              currentHolder = entry.getKey();
              break;
            }
          }

          if (currentHolder != null && !currentHolder.equals(winnerUUID)) {
            final UUID oldHolderUUID = currentHolder;
            org.bukkit.OfflinePlayer oldHolder = Bukkit.getOfflinePlayer(oldHolderUUID);
            plugin.getRankManager().removeRank(oldHolderUUID);

            String oldHolderName = oldHolder.getName() != null ? oldHolder.getName() : "Unknown";
            plugin
                .getLogger()
                .info(
                    "Removed rank #"
                        + rank
                        + " from "
                        + oldHolderName
                        + " (Discord rank random reassignment)");

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
                            + rank
                            + " was randomly reassigned via Discord to §b"
                            + winner.getName());
                  });
            } else {
              plugin.getInventoryManager().removeExtraInventory(currentHolder);
            }
          }

          Integer oldRank = plugin.getRankManager().getRank(winnerUUID);
          if (oldRank != null) {
            plugin.getRankManager().removeRank(winnerUUID);
            plugin
                .getLogger()
                .info(
                    "Removed old rank #"
                        + oldRank
                        + " from "
                        + winner.getName()
                        + " before assigning new rank");
          }

          plugin.getRankManager().setRank(winnerUUID, rank);
          plugin.getRankManager().saveRank(winnerUUID, rank);

          Player onlineWinner = winner.getPlayer();
          if (onlineWinner != null && onlineWinner.isOnline()) {
            SchedulerUtil.runForEntity(
                plugin,
                onlineWinner,
                () -> {
                  if (!onlineWinner.isOnline()) return;
                  plugin.getHealthManager().applyHealth(onlineWinner);
                  plugin.getInventoryManager().removeExtraInventory(winnerUUID);

                  if (plugin.getTabIntegration() != null) {
                    plugin.getTabIntegration().updatePlayer(onlineWinner);
                  }
                  if (plugin.getVanillaTabManager() != null) {
                    plugin.getVanillaTabManager().updatePlayer(onlineWinner);
                  }

                  Map<String, String> titlePlaceholders = Map.of("rank", String.valueOf(rank));
                  String title =
                      plugin
                          .getMessageManager()
                          .getMessage("rank.change.title-gained", titlePlaceholders);
                  String subtitle = plugin.getMessageManager().getMessage("rank.change.subtitle-gained");
                  plugin.getMessageManager().sendTitle(onlineWinner, title, subtitle, 10, 70, 20);
                });
            plugin
                .getLogger()
                .info("Applied perks to " + winner.getName() + " (online, rank #" + rank + ")");
          } else {
            plugin.getInventoryManager().removeExtraInventory(winnerUUID);
            plugin
                .getLogger()
                .info(
                    "Assigned rank #"
                        + rank
                        + " to "
                        + winner.getName()
                        + " (offline, perks will apply on join)");
          }

          plugin.getHierarchyHammerManager().updateHammerHolder();

          String successMsg =
              "**Rank Roulette Complete!**\n"
                  + "**"
                  + winner.getName()
                  + "** won Rank #"
                  + rank
                  + "!\n\n"
                  + "Pool: "
                  + playerPool.size()
                  + " players (Ranked: "
                  + includeRanked
                  + ", Offline: "
                  + includeOffline
                  + ")";
          if (isSlash) {
            slashEvent.getHook().sendMessage(successMsg).queue();
          } else {
            textEvent.getMessage().reply(successMsg).queue();
          }

          String broadcast =
              "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                  + "  §b§lRANK ASSIGNED\n"
                  + "\n"
                  + "  §a"
                  + winner.getName()
                  + " §7received §6Rank #"
                  + rank
                  + "§7!\n"
                  + "§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
          Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

          if (plugin.getDiscordManager() != null) {
            var notificationManager = plugin.getDiscordManager().getNotificationManager();
            if (notificationManager != null) {
              notificationManager.sendRankGainedDM(
                  winnerUUID, rank, "Won via rank roulette (Discord)");
            }
          }

          if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
            plugin.getDiscordManager().forceUpdate();
          }
        });
  }

  private boolean isEligibleForRank(UUID uuid, int targetRank, boolean includeRanked) {
    if (plugin.getDatabaseManager().isBlacklisted(uuid)) {
      return false;
    }

    Integer currentRank = plugin.getRankManager().getRank(uuid);

    if (currentRank == null) {
      return true;
    }

    if (!includeRanked) {
      return false;
    }

    if (targetRank < currentRank) {
      return true;
    }

    return false;
  }
}
