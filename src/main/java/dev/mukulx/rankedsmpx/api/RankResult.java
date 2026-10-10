package dev.mukulx.rankedsmpx.api;

/** Outcome of {@link RankedSMPApi#setRank}. */
public enum RankResult {
  /** The rank was applied. */
  SUCCESS,
  /** The rank is below 1 or above {@link RankedSMPApi#getTotalRanks()}. */
  INVALID_RANK,
  /** Another player already holds the rank. Remove it from them or use {@code swapRanks}. */
  RANK_TAKEN,
  /** The player is blacklisted and cannot hold a rank. */
  BLACKLISTED
}
