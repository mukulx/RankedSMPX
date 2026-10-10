package dev.mukulx.rankedsmpx.api;

import org.jetbrains.annotations.NotNull;

/**
 * The values of one row of ranks.yml.
 *
 * @param rank 1 and up for ranked players, 0 for unranked, -1 for blacklisted
 * @param hearts max health in hearts
 * @param xpMultiplier multiplier applied to XP the player gains
 * @param potionMultiplier multiplier applied to potion effect durations
 * @param extraSlots usable slots in the extra inventory, 0 for none
 * @param prefix tab list / chat prefix, with {@code &} color codes
 * @param suffix tab list suffix, with {@code &} color codes
 */
public record RankSettings(
    int rank,
    double hearts,
    double xpMultiplier,
    double potionMultiplier,
    int extraSlots,
    @NotNull String prefix,
    @NotNull String suffix) {}
