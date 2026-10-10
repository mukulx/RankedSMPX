package dev.mukulx.rankedsmpx.api.internal;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.api.RankResult;
import dev.mukulx.rankedsmpx.api.RankSettings;
import dev.mukulx.rankedsmpx.api.RankedPlayer;
import dev.mukulx.rankedsmpx.api.RankedSMPApi;
import dev.mukulx.rankedsmpx.managers.RankTable;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** The plugin's implementation of {@link RankedSMPApi}. Not part of the public API. */
public final class RankedSMPApiImpl implements RankedSMPApi {

  public static final int API_VERSION = 1;

  private final RankedSMPXPlugin plugin;

  public RankedSMPApiImpl(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public int apiVersion() {
    return API_VERSION;
  }

  @NotNull
  @Override
  public String pluginVersion() {
    return plugin.getPluginMeta().getVersion();
  }

  @NotNull
  @Override
  public OptionalInt getRank(@NotNull UUID player) {
    Integer rank = plugin.getRankManager().getRank(player);
    return rank == null ? OptionalInt.empty() : OptionalInt.of(rank);
  }

  @Override
  public boolean hasRank(@NotNull UUID player) {
    return plugin.getRankManager().hasRank(player);
  }

  @NotNull
  @Override
  public Optional<UUID> getPlayerWithRank(int rank) {
    return Optional.ofNullable(plugin.getRankManager().getPlayerWithRank(rank));
  }

  @Override
  public int getTotalRanks() {
    return plugin.getConfigManager().getTotalRanks();
  }

  @NotNull
  @Override
  public Map<UUID, Integer> getRanks() {
    return Collections.unmodifiableMap(new LinkedHashMap<>(plugin.getRankManager().getAllRanks()));
  }

  @NotNull
  @Override
  public List<RankedPlayer> getLeaderboard(int limit) {
    return plugin.getRankManager().getTopRanks(Math.max(0, limit)).stream()
        .map(entry -> new RankedPlayer(entry.getKey(), entry.getValue()))
        .toList();
  }

  @NotNull
  @Override
  public RankResult setRank(@NotNull UUID player, int rank, @NotNull String reason) {
    if (rank < 1 || rank > getTotalRanks()) {
      return RankResult.INVALID_RANK;
    }
    if (plugin.getDatabaseManager().isBlacklisted(player)) {
      return RankResult.BLACKLISTED;
    }
    UUID holder = plugin.getRankManager().getPlayerWithRank(rank);
    if (holder != null && !holder.equals(player)) {
      return RankResult.RANK_TAKEN;
    }

    plugin.getRankManager().setRankWithHistory(player, rank, reason);
    if (!plugin.getGameManager().isGameActive()) {
      plugin.getGameManager().setGameActive(true);
    }
    refreshPlayer(player, rank, true);
    return RankResult.SUCCESS;
  }

  @Override
  public boolean removeRank(@NotNull UUID player, @NotNull String reason) {
    Integer oldRank = plugin.getRankManager().getRank(player);
    if (oldRank == null) {
      return false;
    }
    plugin.getRankManager().removeRankWithHistory(player, oldRank, reason);
    refreshPlayer(player, null, false);
    return true;
  }

  @Override
  public boolean swapRanks(@NotNull UUID first, @NotNull UUID second, @NotNull String reason) {
    Integer rank1 = plugin.getRankManager().getRank(first);
    Integer rank2 = plugin.getRankManager().getRank(second);
    if (rank1 == null || rank2 == null || first.equals(second)) {
      return false;
    }
    plugin.getRankManager().swapRanksWithHistory(first, second, reason, reason);
    refreshPlayer(first, rank2, true);
    refreshPlayer(second, rank1, true);
    return true;
  }

  @NotNull
  @Override
  public RankSettings getRankSettings(int rank) {
    RankTable.Row row = plugin.getConfigManager().getRanks().row(rank);
    int shown = Math.max(rank, RankTable.BLACKLISTED);
    return new RankSettings(
        shown, row.hearts(), row.xp(), row.potion(), row.slots(), row.prefix(), row.suffix());
  }

  @NotNull
  @Override
  public RankSettings getRankSettings(@NotNull UUID player) {
    if (plugin.getDatabaseManager().isBlacklisted(player)) {
      return getRankSettings(RankTable.BLACKLISTED);
    }
    return getRankSettings(getRank(player).orElse(RankTable.UNRANKED));
  }

  @Override
  public boolean isBlacklisted(@NotNull UUID player) {
    return plugin.getDatabaseManager().isBlacklisted(player);
  }

  @Override
  public void blacklist(@NotNull UUID player, @NotNull String playerName, @NotNull String by) {
    if (plugin.getRankManager().hasRank(player)) {
      removeRank(player, "Blacklisted");
    }
    plugin.getDatabaseManager().addToBlacklist(player, playerName, by);
  }

  @Override
  public void unblacklist(@NotNull UUID player) {
    plugin.getDatabaseManager().removeFromBlacklist(player);
  }

  @Override
  public boolean isSeasonActive() {
    return plugin.getGameManager().isGameActive();
  }

  @Override
  public boolean startSeason() {
    return plugin.getGameManager().startGame();
  }

  @Override
  public boolean stopSeason() {
    return plugin.getGameManager().stopGame();
  }

  @Override
  public boolean isDiscordEnabled() {
    return plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled();
  }

  @NotNull
  @Override
  public Optional<String> getDiscordId(@NotNull UUID player) {
    if (!isDiscordEnabled() || plugin.getDiscordManager().getLinkManager() == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(plugin.getDiscordManager().getLinkManager().getDiscordId(player));
  }

  @NotNull
  @Override
  public Optional<UUID> getLinkedPlayer(@NotNull String discordId) {
    if (!isDiscordEnabled() || plugin.getDiscordManager().getLinkManager() == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(plugin.getDiscordManager().getLinkManager().getMinecraftUUID(discordId));
  }

  @Override
  public void reload() {
    plugin.reload();
  }

  /** Applies hearts, extra inventory, hammer holder and Discord role after a rank change. */
  private void refreshPlayer(@NotNull UUID uuid, Integer rank, boolean ranked) {
    Player online = Bukkit.getPlayer(uuid);
    if (online != null) {
      SchedulerUtil.runForEntity(
          plugin,
          online,
          () -> {
            if (!online.isOnline()) {
              return;
            }
            if (ranked) {
              plugin.getHealthManager().applyHealth(online);
            } else {
              plugin.getHealthManager().resetHealth(online);
            }
            plugin.getInventoryManager().removeExtraInventory(uuid);
          });
    }
    plugin.getHierarchyHammerManager().updateHammerHolder();
    if (isDiscordEnabled() && plugin.getDiscordManager().getRoleSyncManager() != null) {
      plugin.getDiscordManager().getRoleSyncManager().syncPlayerRole(uuid, rank);
    }
  }
}
