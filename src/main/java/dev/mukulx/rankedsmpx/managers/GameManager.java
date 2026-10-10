package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GameManager {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;
  private boolean gameActive;

  public GameManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
    this.gameActive = false;
  }

  @SuppressWarnings("removal") // Sound.valueOf is deprecated but we handle the exception
  private Sound getSoundFromConfig(String soundName) {
    try {
      return Sound.valueOf(soundName);
    } catch (IllegalArgumentException e) {
      plugin.getLogger().warning("Invalid sound name in config: " + soundName + ", using default");
      return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    }
  }

  public void initializeGameState() {
    // Season state persists across server restarts - if ranks exist in database, season is active
    this.gameActive = !rankManager.getAllRanks().isEmpty();
    if (gameActive) {
      plugin
          .getLogger()
          .info(
              "Game state loaded: Season is ACTIVE ("
                  + rankManager.getRankedPlayerCount()
                  + " ranked players)");
    } else {
      plugin.getLogger().info("Game state loaded: Season is INACTIVE (no ranks found)");
    }
  }

  public boolean startGame() {
    if (gameActive) {
      return false;
    }

    Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
    if (onlinePlayers.isEmpty()) {
      return false;
    }

    int totalRanks = plugin.getConfigManager().getTotalRanks();

    // This prevents duplicate ranks if stop command didn't fully complete
    Map<UUID, Integer> existingRanks = rankManager.getAllRanks();
    if (!existingRanks.isEmpty()) {
      plugin
          .getLogger()
          .severe(
              "CRITICAL: Cannot start game - "
                  + existingRanks.size()
                  + " ranks still exist in memory!");
      plugin.getLogger().severe("Run /rankedsmp stop first to clear all ranks");
      return false;
    }

    List<Player> players = new ArrayList<>();
    for (Player player : onlinePlayers) {
      if (!plugin.getDatabaseManager().isBlacklisted(player.getUniqueId())) {
        players.add(player);
      } else {
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin.getLogger().info("Skipping blacklisted player: " + player.getName());
        }
      }
    }

    if (players.isEmpty()) {
      return false;
    }

    Collections.shuffle(players);

    String soundName = plugin.getConfig().getBoolean("sounds.season-start.enabled", true)
        ? plugin.getConfig().getString("sounds.season-start.sound", "ENTITY_ENDER_DRAGON_GROWL")
        : null;
    float volume = (float) plugin.getConfig().getDouble("sounds.season-start.volume", 1.0);
    float pitch = (float) plugin.getConfig().getDouble("sounds.season-start.pitch", 1.0);
    Sound sound = soundName != null ? getSoundFromConfig(soundName) : null;
    String initialTitle = messageManager.getMessage("commands.start.title");
    String initialSubtitle = messageManager.getMessage("commands.start.subtitle");

    for (Player player : onlinePlayers) {
      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            if (!player.isOnline()) return;
            if (sound != null) {
              player.playSound(player.getLocation(), sound, volume, pitch);
            }
            messageManager.sendTitle(
                player,
                initialTitle,
                initialSubtitle,
                0,
                10,
                5); // 0.5 second total (instant fade in, 0.5s stay, 0.25s fade out)
          });
    }

    Map<UUID, Integer> ranksToAssign = new HashMap<>();
    List<String> assignments = new ArrayList<>();

    for (int i = 0; i < Math.min(players.size(), totalRanks); i++) {
      Player player = players.get(i);
      int rank = i + 1;
      ranksToAssign.put(player.getUniqueId(), rank);

      rankManager.setRankInMemory(player.getUniqueId(), rank);

      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            if (!player.isOnline()) return;
            plugin.getHealthManager().applyHealth(player);
            plugin.getInventoryManager().removeExtraInventory(player.getUniqueId());
          });

      String rankPrefix = messageManager.getRankPrefixLegacy(rank, false).trim();
      Map<String, String> placeholders =
          Map.of(
              "player", player.getName(),
              "rank", String.valueOf(rank),
              "rank_prefix", rankPrefix);
      assignments.add(messageManager.getMessage("commands.start.assignment", placeholders));
    }

    plugin
        .getLogger()
        .info("Season started! Assigned ranks to " + ranksToAssign.size() + " players");

    plugin.getHierarchyHammerManager().resetForNewSeason();
    plugin.getHierarchyHammerManager().updateHammerHolder();

    gameActive = true;

    SchedulerUtil.runLater(
        plugin,
        () -> {
          for (Map.Entry<UUID, Integer> entry : ranksToAssign.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
              int rank = entry.getValue();
              SchedulerUtil.runForEntity(
                  plugin,
                  player,
                  () -> {
                    if (player.isOnline()) {
                      plugin.getRankRevealAnimation().startAnimation(player, rank);
                    }
                  });
            }
          }
        },
        15L); // 0.75 seconds delay (right after initial title flash)

    SchedulerUtil.runLater(
        plugin,
        () -> {
          if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
            plugin.getTabIntegration().updateAllPlayers();
            if (plugin.getConfig().getBoolean("debug", false)) {
              plugin.getLogger().info("Updated TAB for all players after animation");
            }
          }

          if (plugin.getVanillaTabManager() != null) {
            plugin.getVanillaTabManager().updateAllPlayers();
            if (plugin.getConfig().getBoolean("debug", false)) {
              plugin.getLogger().info("Updated Vanilla Tab for all players after animation");
            }
          }

          boolean broadcastSoundEnabled =
              plugin.getConfig().getBoolean("sounds.rank-broadcast.enabled", true);
          String broadcastSoundName =
              plugin.getConfig().getString("sounds.rank-broadcast.sound", "BLOCK_NOTE_BLOCK_PLING");
          float broadcastVolume =
              (float) plugin.getConfig().getDouble("sounds.rank-broadcast.volume", 0.5);
          float broadcastPitch =
              (float) plugin.getConfig().getDouble("sounds.rank-broadcast.pitch", 1.5);

          Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(messageManager.getMessage("commands.start.broadcast")));
          Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(" "));
          Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(messageManager.getMessage("commands.start.assignment-header")));
          for (String assignment : assignments) {
            Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(assignment));
          }
          Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(messageManager.getMessage("commands.start.assignment-footer")));
          Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(" "));

          if (broadcastSoundEnabled) {
            Sound broadcastSound = getSoundFromConfig(broadcastSoundName);
            for (Player player : Bukkit.getOnlinePlayers()) {
              SchedulerUtil.runForEntity(
                  plugin,
                  player,
                  () -> player.playSound(player.getLocation(), broadcastSound, broadcastVolume, broadcastPitch));
            }
          }
        },
        115L); // ~5.75 seconds (0.75s initial + 4s animation + 1s buffer)

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          rankManager.saveRanksBatch(ranksToAssign);
        });

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      if (plugin.getDiscordConfig().getBoolean("notifications.notify-on-gain", true)) {
        var notificationManager = plugin.getDiscordManager().getNotificationManager();
        if (notificationManager != null) {
          for (Map.Entry<UUID, Integer> entry : ranksToAssign.entrySet()) {
            notificationManager.sendRankGainedDM(
                entry.getKey(), entry.getValue(), "Assigned during season start");
          }
          plugin
              .getLogger()
              .info(
                  "Queued " + ranksToAssign.size() + " Discord DM notifications for season start");
        }
      }

      var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
      if (roleSyncManager != null) {
        for (Map.Entry<UUID, Integer> entry : ranksToAssign.entrySet()) {
          roleSyncManager.syncPlayerRole(entry.getKey(), entry.getValue());
        }
        plugin
            .getLogger()
            .info("Queued " + ranksToAssign.size() + " Discord role syncs for season start");
      }

      var updatesManager = plugin.getDiscordManager().getUpdatesManager();
      if (updatesManager != null) {
        updatesManager.postGameStarted(ranksToAssign.size());

        for (Map.Entry<UUID, Integer> entry : ranksToAssign.entrySet()) {
          Player p = Bukkit.getPlayer(entry.getKey());
          if (p != null) {
            updatesManager.postRankGained(p.getName(), entry.getValue(), "Season started");
          }
        }
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin.getLogger().info("Posted season start updates to Discord");
        }
      }

      plugin.getDiscordManager().forceUpdate();

      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Forced Discord leaderboard update after game start");
      }
    }

    Bukkit.getPluginManager()
        .callEvent(new dev.mukulx.rankedsmpx.api.event.SeasonStartEvent(ranksToAssign.size()));

    return true;
  }

  public boolean stopGame() {
    Map<UUID, Integer> existingRanks = rankManager.getAllRanks();

    if (existingRanks.isEmpty()) {
      return false;
    }

    int playerCount = existingRanks.size();

    plugin.getLogger().info("Stopping season... Clearing " + playerCount + " ranks");

    rankManager.clearAllRanks();
    gameActive = false;
    Bukkit.getPluginManager()
        .callEvent(new dev.mukulx.rankedsmpx.api.event.SeasonStopEvent(playerCount));

    plugin.getLogger().info("Season stopped! Cleared ranks for " + playerCount + " players");

    plugin.getHierarchyHammerManager().resetForNewSeason();

    for (Player player : Bukkit.getOnlinePlayers()) {
      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            if (!player.isOnline()) return;
            plugin.getHierarchyHammerManager().removeHammer(player);
            plugin.getHealthManager().resetHealth(player);

            plugin.getInventoryManager().removeExtraInventory(player.getUniqueId());

            if (plugin.getTabIntegration() != null) {
              plugin.getTabIntegration().updatePlayer(player);
            }

            if (plugin.getVanillaTabManager() != null) {
              plugin.getVanillaTabManager().updatePlayer(player);
            }

            String stopTitle = messageManager.getMessage("commands.stop.title");
            String stopSubtitle = messageManager.getMessage("commands.stop.subtitle");
            messageManager.sendTitle(player, stopTitle, stopSubtitle, 10, 60, 20);

            if (plugin.getConfig().getBoolean("sounds.season-stop.enabled", true)) {
              String soundName =
                  plugin.getConfig().getString("sounds.season-stop.sound", "ENTITY_WITHER_DEATH");
              float volume = (float) plugin.getConfig().getDouble("sounds.season-stop.volume", 0.8);
              float pitch = (float) plugin.getConfig().getDouble("sounds.season-stop.pitch", 1.0);

              Sound sound = getSoundFromConfig(soundName);
              player.playSound(player.getLocation(), sound, volume, pitch);
            }
          });
    }

    Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(messageManager.getMessage("commands.stop.broadcast")));

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      var updatesManager = plugin.getDiscordManager().getUpdatesManager();
      if (updatesManager != null) {
        updatesManager.postGameStopped();
      }

      plugin.getDiscordManager().forceUpdate();
    }

    if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
      SchedulerUtil.run(
          plugin,
          () -> {
            plugin.getTabIntegration().updateAllPlayers();
            if (plugin.getConfig().getBoolean("debug", false)) {
              plugin.getLogger().info("Updated TAB for all players - ranks removed");
            }
          });
    }

    return true;
  }

  public boolean isGameActive() {
    return gameActive;
  }

  public void setGameActive(boolean active) {
    this.gameActive = active;
  }
}
