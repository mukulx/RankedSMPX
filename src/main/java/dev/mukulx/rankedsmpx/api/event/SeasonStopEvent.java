package dev.mukulx.rankedsmpx.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** The season ended and every rank was cleared. */
public final class SeasonStopEvent extends RankedSMPEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  private final int clearedRanks;

  public SeasonStopEvent(int clearedRanks) {
    this.clearedRanks = clearedRanks;
  }

  /** How many ranks were cleared. */
  public int getClearedRanks() {
    return clearedRanks;
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
