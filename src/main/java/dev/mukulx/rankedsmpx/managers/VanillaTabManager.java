package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

public class VanillaTabManager {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final ConfigManager configManager;
  private Scoreboard scoreboard;

  public VanillaTabManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.configManager = configManager;
  }

  public void initialize() {
    if (!configManager.getConfig().getBoolean("tab-integration.enabled", true)) {
      plugin.getLogger().info("Tab integration disabled in config");
      return;
    }

    // Folia throws UnsupportedOperationException on team registration, so there we only set
    // the player list names.
    if (SchedulerUtil.isFolia()) {
      plugin.getLogger().info("Vanilla tab initialized using player list names (Folia)");
    } else {
      scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
      plugin.getLogger().info("Vanilla tab initialized using main scoreboard");
    }

    SchedulerUtil.runLater(plugin, this::updateAllPlayers, 20L);
  }

  public void updatePlayer(@NotNull Player player) {
    if (!configManager.getConfig().getBoolean("tab-integration.enabled", true)) {
      return;
    }
    if (!SchedulerUtil.isOwnedOrReschedule(plugin, player, () -> updatePlayer(player))) {
      return;
    }

    try {
      Integer rank = rankManager.getRank(player.getUniqueId());
      boolean isBlacklisted = plugin.getDatabaseManager().isBlacklisted(player.getUniqueId());

      String prefix = plugin.getMessageManager().getRankPrefixLegacy(rank, isBlacklisted);
      String suffix = plugin.getMessageManager().getRankSuffixLegacy(rank, isBlacklisted);

      prefix = convertHexToLegacy(prefix);
      suffix = convertHexToLegacy(suffix);

      // Append reset code so prefix color never leaks into player name
      if (!prefix.endsWith("§r") && !prefix.endsWith("&r") && !prefix.endsWith("§r ") && !prefix.endsWith("&r ")) {
        if (prefix.endsWith(" ")) {
          prefix = prefix.substring(0, prefix.length() - 1) + "&r ";
        } else {
          prefix = prefix + "&r";
        }
      }

      prefix = plugin.getMessageManager().colorize(prefix);
      suffix = plugin.getMessageManager().colorize(suffix);

      if (scoreboard != null) {
        String teamName = getTeamName(rank, isBlacklisted);
        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
          team = scoreboard.registerNewTeam(teamName);
        }

        team.prefix(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(prefix));
        team.suffix(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(suffix));

        if (!team.hasEntry(player.getName())) {
          team.addEntry(player.getName());
        }
      }

      try {
        String mmPrefix = plugin.getMessageManager().getRankPrefixMiniMessage(rank, isBlacklisted);
        net.kyori.adventure.text.minimessage.MiniMessage mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        player.playerListName(mm.deserialize(mmPrefix + "<white>" + player.getName() + "</white>"));
      } catch (Throwable ignored) {
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning(
              "Failed to update vanilla tab for "
                  + player.getName()
                  + ": "
                  + e.getClass().getSimpleName()
                  + " - "
                  + e.getMessage());
      e.printStackTrace();
    }
  }

  public void updateAllPlayers() {
    if (!configManager.getConfig().getBoolean("tab-integration.enabled", true)) {
      return;
    }

    for (Player player : Bukkit.getOnlinePlayers()) {
      SchedulerUtil.runForEntity(plugin, player, () -> {
        if (player.isOnline()) {
          updatePlayer(player);
        }
      });
    }
  }

  public void removePlayer(@NotNull Player player) {
    if (!configManager.getConfig().getBoolean("tab-integration.enabled", true)) {
      return;
    }
    if (!SchedulerUtil.isOwnedOrReschedule(plugin, player, () -> removePlayer(player))) {
      return;
    }

    try {
      if (scoreboard != null) {
        for (Team team : scoreboard.getTeams()) {
          if (team.hasEntry(player.getName())) {
            team.removeEntry(player.getName());
          }
        }
      }
      try {
        player.playerListName(null);
      } catch (Throwable ignored) {
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning(
              "Failed to remove vanilla tab data for " + player.getName() + ": " + e.getMessage());
    }
  }

  public void clearPlayer(@NotNull Player player) {
    removePlayer(player);
  }

  private String getTeamName(Integer rank, boolean isBlacklisted) {
    if (isBlacklisted) {
      return "rank_9999"; // Blacklisted appear last
    }

    if (rank != null && rank > 0) {
      return String.format("rank_%04d", rank); // Ranked players: rank_0001, rank_0002, etc.
    }

    return "rank_9998"; // Unranked appear before blacklisted, after all ranked
  }

  public void shutdown() {
    if (!configManager.getConfig().getBoolean("tab-integration.enabled", true)) {
      return;
    }

    // Tasks cannot be scheduled once the plugin is disabled, so clean up directly.
    for (Player player : Bukkit.getOnlinePlayers()) {
      removePlayer(player);
    }

    plugin.getLogger().info("Vanilla tab integration disabled.");
  }

  public void reload() {
    if (configManager.getConfig().getBoolean("tab-integration.enabled", true)) {
      updateAllPlayers();
    }
  }

  private String convertHexToLegacy(String text) {
    text = text.replaceAll("&#FFD700", "&6"); // Gold
    text = text.replaceAll("&#FFAA00", "&6"); // Gold/Orange -> Gold

    text = text.replaceAll("&#C0C0C0", "&f"); // Silver -> White
    text = text.replaceAll("&#A4D7E8", "&b"); // Light Blue -> Aqua

    text = text.replaceAll("&#32CD32", "&a"); // Lime Green -> Green
    text = text.replaceAll("&#5FE47E", "&a"); // Lime -> Green

    text = text.replaceAll("&#8A2BE2", "&5"); // Blue Violet -> Dark Purple
    text = text.replaceAll("&#C21FB7", "&d"); // Pink -> Light Purple

    text = text.replaceAll("&#[0-9A-Fa-f]{6}", "&f"); // Default to white

    return text;
  }
}
