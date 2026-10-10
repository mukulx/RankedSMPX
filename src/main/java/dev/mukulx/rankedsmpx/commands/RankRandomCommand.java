package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankRandomCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;
  private final Random random = new Random();

  private static final TextColor[] ROULETTE_COLORS = {
    TextColor.fromHexString("#FF0000"), // Red
    TextColor.fromHexString("#FF7F00"), // Orange
    TextColor.fromHexString("#FFFF00"), // Yellow
    TextColor.fromHexString("#00FF00"), // Green
    TextColor.fromHexString("#00FFFF"), // Cyan
    TextColor.fromHexString("#0000FF"), // Blue
    TextColor.fromHexString("#8B00FF") // Purple
  };

  public RankRandomCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {

    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage((Player) sender, "no-permission");
      return true;
    }

    if (args.length == 0) {
      sender.sendMessage("§cUsage: /" + label + " <rank> [-i] [-o]");
      sender.sendMessage("§7Flags:");
      sender.sendMessage(
          "§7  -i, --include-ranked  §fInclude ranked players (won't get worse rank)");
      sender.sendMessage("§7  -o, --offline         §fInclude offline players");
      sender.sendMessage("§7Default: Only unranked online players");
      return true;
    }

    int rank;
    try {
      rank = Integer.parseInt(args[0]);
    } catch (NumberFormatException e) {
      sender.sendMessage("§cInvalid rank number: " + args[0]);
      return true;
    }

    int totalRanks = plugin.getConfigManager().getTotalRanks();
    if (rank < 1 || rank > totalRanks) {
      sender.sendMessage("§cRank must be between 1 and " + totalRanks);
      return true;
    }

    UUID currentHolder = null;
    for (var entry : rankManager.getAllRanks().entrySet()) {
      if (entry.getValue() == rank) {
        currentHolder = entry.getKey();
        break;
      }
    }

    if (currentHolder != null) {
      OfflinePlayer holder = Bukkit.getOfflinePlayer(currentHolder);
      sender.sendMessage("§cRank #" + rank + " is already held by §b" + holder.getName() + "§c!");
      sender.sendMessage("§7Use §f/rankedsmp rank remove " + holder.getName() + " §7first.");
      return true;
    }

    boolean includeRanked = false;
    boolean includeOffline = false;

    for (int i = 1; i < args.length; i++) {
      String flag = args[i].toLowerCase();
      if (flag.equals("-i") || flag.equals("--include-ranked")) {
        includeRanked = true;
      } else if (flag.equals("-o") || flag.equals("--offline")) {
        includeOffline = true;
      } else {
        sender.sendMessage("§cUnknown flag: " + args[i]);
        return true;
      }
    }

    List<OfflinePlayer> playerPool = new ArrayList<>();

    if (includeOffline) {
      for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
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
      sender.sendMessage("§cNo eligible players found!");
      sender.sendMessage("§7Try using flags:");
      sender.sendMessage("§7  -i  §fInclude ranked players");
      sender.sendMessage("§7  -o  §fInclude offline players");
      return true;
    }

    OfflinePlayer winner = playerPool.get(random.nextInt(playerPool.size()));

    sender.sendMessage(
        "§6§lRANK ROULETTE §r§7- Selecting random player for rank §b#" + rank + "§7...");
    sender.sendMessage(
        "§7Pool: §f"
            + playerPool.size()
            + " players §7(Include ranked: §f"
            + includeRanked
            + "§7, Offline: §f"
            + includeOffline
            + "§7)");

    startRouletteAnimation(rank, playerPool, winner);

    return true;
  }

  private boolean isEligibleForRank(@NotNull UUID uuid, int targetRank, boolean includeRanked) {
    if (plugin.getDatabaseManager().isBlacklisted(uuid)) {
      return false;
    }

    Integer currentRank = rankManager.getRank(uuid);

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

  private void startRouletteAnimation(
      int rank, @NotNull List<OfflinePlayer> playerPool, @NotNull OfflinePlayer winner) {

    final int totalSpins = 20;
    final int initialDelay = 1;
    final int maxDelay = 10;

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          for (int spin = 0; spin < totalSpins; spin++) {
            final int currentSpin = spin;

            int delay =
                initialDelay
                    + (int) ((maxDelay - initialDelay) * Math.pow(spin / (double) totalSpins, 2));

            OfflinePlayer displayPlayer;
            if (spin >= totalSpins - 3) {
              displayPlayer = winner;
            } else {
              displayPlayer = playerPool.get(random.nextInt(playerPool.size()));
            }

            final OfflinePlayer finalDisplayPlayer = displayPlayer;
            final boolean isLastSpin = (spin == totalSpins - 1);

            for (Player viewer : Bukkit.getOnlinePlayers()) {
              SchedulerUtil.runForEntity(
                  plugin,
                  viewer,
                  () -> {
                    if (viewer.isOnline()) {
                      showRouletteTitle(viewer, rank, finalDisplayPlayer, currentSpin, isLastSpin);
                    }
                  });
            }

            try {
              Thread.sleep(delay * 50L); // Convert ticks to milliseconds
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }

          try {
            Thread.sleep(500L);
          } catch (InterruptedException ignored) {
          }

          SchedulerUtil.run(
              plugin,
              () -> {
                assignRankToWinner(rank, winner);
              });
        });
  }

  private void showRouletteTitle(
      @NotNull Player viewer,
      int rank,
      @NotNull OfflinePlayer displayPlayer,
      int spin,
      boolean isLastSpin) {

    TextColor color = ROULETTE_COLORS[spin % ROULETTE_COLORS.length];

    Component titleComponent = Component.text(displayPlayer.getName(), color);

    Component subtitleComponent =
        Component.text("Rank #" + rank, isLastSpin ? NamedTextColor.GREEN : NamedTextColor.GRAY);

    Title title =
        Title.title(
            titleComponent,
            subtitleComponent,
            Title.Times.times(
                java.time.Duration.ofMillis(0),
                java.time.Duration.ofMillis(isLastSpin ? 3000 : 500),
                java.time.Duration.ofMillis(isLastSpin ? 1000 : 100)));

    if (plugin.getMessageManager().titlesEnabled()) {
      viewer.showTitle(title);
    }

    if (isLastSpin) {
      viewer.playSound(viewer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
      viewer.playSound(viewer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    } else {
      float pitch = 0.8f + (spin / 30.0f) * 0.4f;
      viewer.playSound(viewer.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
    }
  }

  private void assignRankToWinner(int rank, @NotNull OfflinePlayer winner) {
    UUID winnerUUID = winner.getUniqueId();

    Integer oldRank = rankManager.getRank(winnerUUID);
    if (oldRank != null) {
      rankManager.removeRank(winnerUUID);
      plugin
          .getLogger()
          .info(
              "Removed old rank #"
                  + oldRank
                  + " from "
                  + winner.getName()
                  + " before assigning new rank");
    }

    rankManager.setRank(winnerUUID, rank);
    rankManager.saveRank(winnerUUID, rank);

    Component announcement =
        Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD)
            .append(Component.newline())
            .append(Component.text("  ", NamedTextColor.GOLD))
            .append(Component.text("RANK ASSIGNED", NamedTextColor.AQUA))
            .append(Component.newline())
            .append(Component.newline())
            .append(Component.text("  ", NamedTextColor.WHITE))
            .append(Component.text(winner.getName(), NamedTextColor.GREEN))
            .append(Component.text(" received ", NamedTextColor.GRAY))
            .append(Component.text("Rank #" + rank, NamedTextColor.GOLD))
            .append(Component.text("!", NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.GOLD));

    for (Player online : Bukkit.getOnlinePlayers()) {
      online.sendMessage(announcement);
    }

    Player onlineWinner = winner.getPlayer();
    if (onlineWinner != null && onlineWinner.isOnline()) {
      SchedulerUtil.runForEntity(
          plugin,
          onlineWinner,
          () -> {
            plugin.getHealthManager().applyHealth(onlineWinner);
            messageManager.sendMessage(onlineWinner, "rank-received");

            if (plugin.getTabIntegration() != null) {
              plugin.getTabIntegration().updatePlayer(onlineWinner);
            }

            if (plugin.getVanillaTabManager() != null) {
              plugin.getVanillaTabManager().updatePlayer(onlineWinner);
            }

            plugin
                .getLogger()
                .info("Applied perks to " + winner.getName() + " (online, rank #" + rank + ")");
          });
    } else {
      plugin
          .getLogger()
          .info(
              "Assigned rank #"
                  + rank
                  + " to "
                  + winner.getName()
                  + " (offline, perks will apply on join)");
    }

    if (plugin.getDiscordManager() != null) {
      var notificationManager = plugin.getDiscordManager().getNotificationManager();
      if (notificationManager != null) {
        notificationManager.sendRankGainedDM(winnerUUID, rank, "Won via rank roulette");
      }
    }
  }

  @Override
  @Nullable
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {

    if (!sender.hasPermission("rankedsmp.admin")) {
      return new ArrayList<>();
    }

    List<String> suggestions = new ArrayList<>();

    if (args.length == 1) {
      int totalRanks = plugin.getConfigManager().getTotalRanks();
      String input = args[0].toLowerCase();

      for (int i = 1; i <= totalRanks; i++) {
        String rankStr = String.valueOf(i);
        if (rankStr.startsWith(input)) {
          boolean isAvailable = true;
          for (var entry : rankManager.getAllRanks().entrySet()) {
            if (entry.getValue() == i) {
              isAvailable = false;
              break;
            }
          }
          if (isAvailable) {
            suggestions.add(rankStr);
          }
        }
      }
    } else if (args.length >= 2) {
      List<String> flags = Arrays.asList("-i", "--include-ranked", "-o", "--offline");
      String input = args[args.length - 1].toLowerCase();

      List<String> usedFlags = Arrays.asList(args).subList(1, args.length - 1);

      for (String flag : flags) {
        if (flag.startsWith(input) && !usedFlags.contains(flag)) {
          if (flag.equals("-i") && usedFlags.contains("--include-ranked")) continue;
          if (flag.equals("--include-ranked") && usedFlags.contains("-i")) continue;
          if (flag.equals("-o") && usedFlags.contains("--offline")) continue;
          if (flag.equals("--offline") && usedFlags.contains("-o")) continue;

          suggestions.add(flag);
        }
      }
    }

    return suggestions;
  }
}
