package dev.mukulx.rankedsmpx.api.event;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** A season started and ranks were handed out. */
public final class SeasonStartEvent extends RankedSMPEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  private final int rankedPlayers;

  public SeasonStartEvent(int rankedPlayers) {
    this.rankedPlayers = rankedPlayers;
  }

  /** How many players received a rank. */
  public int getRankedPlayers() {
    return rankedPlayers;
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
