package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.HealthManager;
import dev.mukulx.rankedsmpx.managers.InventoryManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerJoinListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final HealthManager healthManager;
  private final InventoryManager inventoryManager;

  public PlayerJoinListener(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull HealthManager healthManager,
      @NotNull InventoryManager inventoryManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.healthManager = healthManager;
    this.inventoryManager = inventoryManager;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (!plugin.isPluginEnabled()) {
      return;
    }

    if (inventoryManager.shouldDropOnJoin(player.getUniqueId())) {
      inventoryManager.dropExtraInventoryOnJoin(player);
    }

    if (plugin.getConfig().getBoolean("rank-display.show-on-join", true)) {
      String prefix = getRankPrefixMiniMessage(player);

      net.kyori.adventure.text.minimessage.MiniMessage mm =
          net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
      event.joinMessage(mm.deserialize(prefix + "<aqua>" + player.getName() + " joined the game</yellow>"));
    }

    if (inventoryManager.isBeingViewedByAdmin(player.getUniqueId())) {
      UUID adminUUID = inventoryManager.getAdminViewingPlayer(player.getUniqueId());
      if (adminUUID != null) {
        Player admin = org.bukkit.Bukkit.getPlayer(adminUUID);
        if (admin != null && admin.isOnline()) {
          admin.closeInventory();
          admin.sendMessage(
              "§b"
                  + player.getName()
                  + " §7joined while you were viewing their inventory. Changes saved and view closed.");
          plugin
              .getLogger()
              .info(
                  "Closed admin view for "
                      + admin.getName()
                      + " because "
                      + player.getName()
                      + " joined");
        }
      }
    }

    SchedulerUtil.runForEntityLater(
        plugin,
        player,
        () -> {
          // Enforce 20 HP base health and apply exact rank modifier (heals any corrupted NBT from restarts/rejoins)
          healthManager.fixPlayerHealth(player);

          if (plugin.getHierarchyHammerManager().shouldHaveHammer(player.getUniqueId())) {
            plugin.getHierarchyHammerManager().giveHammer(player);
          }

          if (plugin.getPotionEffectStore() != null) {
            plugin.getPotionEffectStore().restoreEffects(player);
          }

          if (plugin.getTabIntegration() != null) {
            plugin.getTabIntegration().updatePlayer(player);
          }

          if (plugin.getVanillaTabManager() != null) {
            plugin.getVanillaTabManager().updatePlayer(player);
          }

          tryAutoAssignRank(player);
        },
        1L);

    SchedulerUtil.runForEntityLater(
        plugin,
        player,
        () -> {
          if (!player.isOnline()) return;
          if (plugin.getTabIntegration() != null) {
            plugin.getTabIntegration().updatePlayer(player);
          }
          if (plugin.getVanillaTabManager() != null) {
            plugin.getVanillaTabManager().updatePlayer(player);
          }
        },
        15L);

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      var updatesManager = plugin.getDiscordManager().getUpdatesManager();
      if (updatesManager != null) {
        updatesManager.postPlayerJoin(player.getName());
      }

      var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
      if (roleSyncManager != null) {
        roleSyncManager.syncModRole(
            player.getUniqueId(),
            player.hasPermission(dev.mukulx.rankedsmpx.discord.RoleSyncManager.MOD_PERMISSION));
      }
    }

    if (player.hasPermission("rankedsmp.admin") || player.isOp()) {
      SchedulerUtil.runForEntityLater(
          plugin,
          player,
          () -> {
            if (plugin.getFirstTimeSetupManager() != null) {
              plugin.getFirstTimeSetupManager().showIfNeeded(player);
            }

            if (org.bukkit.Bukkit.getPluginManager().getPlugin("RankedSMP") != null) {
              player.playSound(
                  player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);

              String prefix = plugin.getMessageManager().getPrefix();
              player.sendMessage("");
              player.sendMessage(
                  plugin
                      .getMessageManager()
                      .applySmallCaps(prefix + " &c&lWARNING: Old RankedSMP plugin detected!"));
              player.sendMessage(
                  plugin
                      .getMessageManager()
                      .applySmallCaps(prefix + " &7This will cause conflicts with RankedSMPX."));
              player.sendMessage(
                  plugin
                      .getMessageManager()
                      .applySmallCaps(prefix + " &7Remove the old plugin and run &b/rsmp migrate"));
              player.sendMessage("");
            }

            if (plugin.getUpdateChecker() != null
                && plugin.getUpdateChecker().isUpdateAvailable()) {
              player.playSound(
                  player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

              String prefix = plugin.getMessageManager().getPrefix();
              String currentVersion = plugin.getPluginMeta().getVersion();
              String latestVersion = plugin.getUpdateChecker().getLatestVersion();

              player.sendMessage(
                  plugin
                      .getMessageManager()
                      .applySmallCaps(prefix + " &bA new version is available!"));
              player.sendMessage(
                  plugin
                      .getMessageManager()
                      .applySmallCaps(
                          prefix
                              + " &7Your version: &c"
                              + currentVersion
                              + " &7| Latest: &a"
                              + latestVersion));
              player.sendMessage(
                  plugin
                      .getMessageManager()
                      .applySmallCaps(
                          prefix + " &cUsing the latest version ensures your server is safe!"));
              player.sendMessage(
                  plugin
                      .getMessageManager()
                      .applySmallCaps(
                          prefix
                              + " &cBugs, dupes, etc. are always fixed in new versions when found."));
              player.sendMessage(
                  plugin.getMessageManager().applySmallCaps(prefix + " &7Download: ")
                      + plugin.getMessageManager().colorize("&b" + plugin.getUpdateChecker().getProjectUrl()));
            }
          },
          60L); // 3 seconds delay
    }
  }

  private String getRankPrefixMiniMessage(Player player) {
    Integer rank = rankManager.getRank(player.getUniqueId());
    boolean isBlacklisted = plugin.getDatabaseManager().isBlacklisted(player.getUniqueId());
    return plugin.getMessageManager().getRankPrefixMiniMessage(rank, isBlacklisted);
  }

  private void tryAutoAssignRank(@NotNull Player player) {
    if (!plugin.getGameManager().isGameActive()) {
      return;
    }

    if (!plugin.getConfig().getBoolean("auto-assign-ranks.enabled", true)) {
      return;
    }

    if (rankManager.hasRank(player.getUniqueId())) {
      return;
    }

    if (plugin.getDatabaseManager().isBlacklisted(player.getUniqueId())) {
      return;
    }

    int totalRanks = plugin.getConfigManager().getTotalRanks();
    Set<Integer> available = rankManager.getAvailableRanks(totalRanks);
    if (available.isEmpty()) {
      return;
    }

    int rank = Collections.min(available);

    rankManager.setRank(player.getUniqueId(), rank);
    healthManager.applyHealth(player);

    SchedulerUtil.runForEntityLater(
        plugin,
        player,
        () -> {
          plugin.getRankRevealAnimation().startAnimation(player, rank);
        },
        14L);

    SchedulerUtil.runForEntityLater(
        plugin,
        player,
        () -> {
          if (!player.isOnline()) return;
          if (plugin.getTabIntegration() != null) {
            plugin.getTabIntegration().updatePlayer(player);
          }
          if (plugin.getVanillaTabManager() != null) {
            plugin.getVanillaTabManager().updatePlayer(player);
          }

          String rankPrefix = plugin.getMessageManager().getRankPrefixLegacy(rank, false).trim();
          String message =
              plugin
                  .getMessageManager()
                  .getMessage(
                      "commands.start.assignment",
                      Map.of(
                          "player", player.getName(),
                          "rank", String.valueOf(rank),
                          "rank_prefix", rankPrefix));
          Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(message));
        },
        114L); // 1 (initial) + 14 (animation delay) + 100 = ~115 ticks
  }
}
