package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.player.PlayerLoadEvent;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.nametag.NameTagManager;
import me.neznamy.tab.api.tablist.SortingManager;
import me.neznamy.tab.api.tablist.TabListFormatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TABIntegration {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final ConfigManager configManager;
  private boolean hooked = false;

  public TABIntegration(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.configManager = configManager;
  }

  public void initialize() {
    if (!Bukkit.getPluginManager().isPluginEnabled("TAB")) {
      plugin.getLogger().warning("TAB plugin not found! TAB integration disabled.");
      return;
    }

    try {
      TabAPI api = TabAPI.getInstance();
      if (api == null) {
        plugin.getLogger().warning("TAB API not available!");
        return;
      }

      api.getEventBus()
          .register(TabLoadEvent.class, event -> SchedulerUtil.run(plugin, this::updateAllPlayers));

      api.getEventBus()
          .register(
              PlayerLoadEvent.class,
              event -> {
                TabPlayer tabPlayer = event.getPlayer();
                Player player = Bukkit.getPlayer(tabPlayer.getUniqueId());
                if (player != null) {
                  SchedulerUtil.runForEntity(plugin, player, () -> updatePlayer(player));
                }
              });

      hooked = true;
      plugin.getLogger().info("Successfully hooked into TAB!");

      SchedulerUtil.runLater(plugin, this::updateAllPlayers, 20L);

    } catch (Exception e) {
      plugin.getLogger().warning("Failed to hook into TAB: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void updatePlayer(@NotNull Player player) {
    if (!hooked) return;

    try {
      TabAPI api = TabAPI.getInstance();
      if (api == null) return;

      TabPlayer tabPlayer = api.getPlayer(player.getUniqueId());
      if (tabPlayer == null || !tabPlayer.isLoaded()) {
        // Player not yet loaded in TAB - retry in 5 ticks
        SchedulerUtil.runForEntityLater(
            plugin,
            player,
            () -> {
              if (player.isOnline()) {
                updatePlayerInternal(player);
              }
            },
            5L);
        return;
      }

      updatePlayerInternal(player);
    } catch (Exception e) {
      if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("not loaded")) {
        plugin
            .getLogger()
            .warning("Failed to update TAB for " + player.getName() + ": " + e.getMessage());
      }
    }
  }

  private void updatePlayerInternal(@NotNull Player player) {
    if (!hooked) return;

    try {
      TabAPI api = TabAPI.getInstance();
      if (api == null) return;

      TabPlayer tabPlayer = api.getPlayer(player.getUniqueId());
      if (tabPlayer == null) return;

      Integer rank = rankManager.getRank(player.getUniqueId());
      boolean isBlacklisted = plugin.getDatabaseManager().isBlacklisted(player.getUniqueId());

      String prefix = getPrefix(rank, isBlacklisted);
      String suffix = plugin.getMessageManager().getRankSuffixLegacy(rank, isBlacklisted);

      NameTagManager nameTagManager = api.getNameTagManager();
      if (nameTagManager != null) {
        nameTagManager.setPrefix(tabPlayer, prefix);
        nameTagManager.setSuffix(tabPlayer, suffix);
      }

      TabListFormatManager tabListManager = api.getTabListFormatManager();
      if (tabListManager != null) {
        tabListManager.setPrefix(tabPlayer, prefix);
        tabListManager.setSuffix(tabPlayer, suffix);
      }

      SortingManager sortingManager = api.getSortingManager();
      if (sortingManager != null) {
        String sortKey = getSortKey(rank, isBlacklisted, player.getUniqueId().toString());
        sortingManager.forceTeamName(tabPlayer, sortKey);
      }

    } catch (Exception e) {
      if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("not loaded")) {
        plugin
            .getLogger()
            .warning("Failed to update TAB for " + player.getName() + ": " + e.getMessage());
      }
    }
  }

  public void updateAllPlayers() {
    if (!hooked) return;

    for (Player player : Bukkit.getOnlinePlayers()) {
      SchedulerUtil.runForEntity(plugin, player, () -> {
        if (player.isOnline()) {
          updatePlayer(player);
        }
      });
    }
  }

  public void removePlayer(@NotNull Player player) {
    if (!hooked) return;

    try {
      TabAPI api = TabAPI.getInstance();
      if (api == null) return;

      TabPlayer tabPlayer = api.getPlayer(player.getUniqueId());
      if (tabPlayer == null || !tabPlayer.isLoaded()) return;

      NameTagManager nameTagManager = api.getNameTagManager();
      if (nameTagManager != null) {
        nameTagManager.setPrefix(tabPlayer, null);
        nameTagManager.setSuffix(tabPlayer, null);
      }

      TabListFormatManager tabListManager = api.getTabListFormatManager();
      if (tabListManager != null) {
        tabListManager.setPrefix(tabPlayer, null);
        tabListManager.setSuffix(tabPlayer, null);
      }

      SortingManager sortingManager = api.getSortingManager();
      if (sortingManager != null) {
        sortingManager.forceTeamName(tabPlayer, null);
      }

    } catch (Exception e) {
      if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("not loaded")) {
        plugin
            .getLogger()
            .warning("Failed to remove TAB data for " + player.getName() + ": " + e.getMessage());
      }
    }
  }

  public void clearPlayer(@NotNull Player player) {
    removePlayer(player);
  }

  private String getPrefix(Integer rank, boolean isBlacklisted) {
    String miniMessagePrefix = plugin.getMessageManager().getRankPrefixMiniMessage(rank, isBlacklisted);
    String legacy = plugin.getMessageManager().convertMiniMessageToLegacy(miniMessagePrefix);
    // Append reset code so prefix color never leaks into player name
    if (!legacy.endsWith("§r") && !legacy.endsWith("&r") && !legacy.endsWith("§r ") && !legacy.endsWith("&r ")) {
      if (legacy.endsWith(" ")) {
        legacy = legacy.substring(0, legacy.length() - 1) + "§r ";
      } else {
        legacy = legacy + "§r";
      }
    }
    return legacy;
  }

  private String getSortKey(Integer rank, boolean isBlacklisted, @NotNull String uuid) {
    String uniqueSuffix = uuid.replace("-", "").substring(0, 12);

    if (isBlacklisted) {
      return "9999" + uniqueSuffix; // Blacklisted appear last
    }

    if (rank != null && rank > 0) {
      return String.format("%04d%s", rank, uniqueSuffix); // Ranked: 0001<UUID fragment>
    }

    return "9998" + uniqueSuffix;
  }

  public void shutdown() {
    if (!hooked) return;

    for (Player player : Bukkit.getOnlinePlayers()) {
      SchedulerUtil.runForEntity(plugin, player, () -> removePlayer(player));
    }

    hooked = false;
    plugin.getLogger().info("TAB integration disabled.");
  }

  public void reload() {
    if (hooked) {
      updateAllPlayers();
    }
  }

  public boolean isHooked() {
    return hooked;
  }
}
