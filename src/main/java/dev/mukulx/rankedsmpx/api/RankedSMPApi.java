package dev.mukulx.rankedsmpx.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Query and control RankedSMPX from another plugin. Get an instance with {@link RankedSMP#get()}.
 *
 * <p>Call methods from the main server thread (on Folia, from the thread that owns the player).
 * Methods that only read are safe from any thread. All returned collections are immutable
 * snapshots.
 */
public interface RankedSMPApi {

  /** Version of this API. It goes up whenever methods or events are added. */
  int apiVersion();

  /** Version of the RankedSMPX plugin, for example {@code 3.0.0}. */
  @NotNull
  String pluginVersion();

  // Ranks

  /** The rank the player holds, or empty if they are unranked. */
  @NotNull
  OptionalInt getRank(@NotNull UUID player);

  boolean hasRank(@NotNull UUID player);

  /** The player holding the given rank, if anyone does. */
  @NotNull
  Optional<UUID> getPlayerWithRank(int rank);

  /** How many ranks exist (the highest row of ranks.yml). */
  int getTotalRanks();

  /** Every ranked player and their rank. */
  @NotNull
  Map<UUID, Integer> getRanks();

  /** The best ranked players first, at most {@code limit}. */
  @NotNull
  List<RankedPlayer> getLeaderboard(int limit);

  /**
   * Gives the player a rank and applies hearts, extra inventory, tab prefix, hammer holder and
   * Discord role the same way {@code /rsmp set} does. No chat message or broadcast is sent.
   *
   * @param reason shown in the rank history, for example {@code "Won the event"}
   */
  @NotNull
  RankResult setRank(@NotNull UUID player, int rank, @NotNull String reason);

  /** Takes the player's rank away. Returns false if they had none. */
  boolean removeRank(@NotNull UUID player, @NotNull String reason);

  /** Exchanges the ranks of two ranked players. Returns false if either has no rank. */
  boolean swapRanks(@NotNull UUID first, @NotNull UUID second, @NotNull String reason);

  // Rank values

  /** The ranks.yml row for a rank. 0 gives the unranked row, -1 the blacklisted row. */
  @NotNull
  RankSettings getRankSettings(int rank);

  /** The ranks.yml row that applies to this player right now (rank, unranked or blacklisted). */
  @NotNull
  RankSettings getRankSettings(@NotNull UUID player);

  // Blacklist

  boolean isBlacklisted(@NotNull UUID player);

  /** Blacklists the player and removes their rank. {@code by} is stored for the record. */
  void blacklist(@NotNull UUID player, @NotNull String playerName, @NotNull String by);

  void unblacklist(@NotNull UUID player);

  // Season

  boolean isSeasonActive();

  /** Starts a season like {@code /rsmp start}. Returns false if one is running or nobody is online. */
  boolean startSeason();

  /** Ends the season like {@code /rsmp stop} and clears all ranks. Returns false if none is running. */
  boolean stopSeason();

  // Discord

  /** True if the Discord bot is enabled and connected. */
  boolean isDiscordEnabled();

  /** The Discord user id linked to this player. */
  @NotNull
  Optional<String> getDiscordId(@NotNull UUID player);

  /** The player linked to this Discord user id. */
  @NotNull
  Optional<UUID> getLinkedPlayer(@NotNull String discordId);

  // Plugin

  /** Reloads every config file, like {@code /rsmp reload}. */
  void reload();
}
