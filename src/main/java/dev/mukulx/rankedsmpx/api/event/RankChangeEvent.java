package dev.mukulx.rankedsmpx.api.event;

import org.bukkit.event.HandlerList;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A player gained, lost or changed rank. */
public final class RankChangeEvent extends RankedSMPEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  private final UUID player;
  private final Integer oldRank;
  private final Integer newRank;
  private final String reason;

  public RankChangeEvent(
      @NotNull UUID player, @Nullable Integer oldRank, @Nullable Integer newRank, @NotNull String reason) {
    this.player = player;
    this.oldRank = oldRank;
    this.newRank = newRank;
    this.reason = reason;
  }

  @NotNull
  public UUID getPlayer() {
    return player;
  }

  /** The previous rank, or null if the player had none. */
  @Nullable
  public Integer getOldRank() {
    return oldRank;
  }

  /** The new rank, or null if the player lost their rank. */
  @Nullable
  public Integer getNewRank() {
    return newRank;
  }

  /** Why the rank changed, such as "Killed by Steve". Empty when no reason was given. */
  @NotNull
  public String getReason() {
    return reason;
  }

  public boolean isGain() {
    return newRank != null && (oldRank == null || newRank < oldRank);
  }

  public boolean isLoss() {
    return oldRank != null && (newRank == null || newRank > oldRank);
  }

  @NotNull
  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  @NotNull
  public static HandlerList getHandlerList() {
    return HANDLERS;
  }
}
