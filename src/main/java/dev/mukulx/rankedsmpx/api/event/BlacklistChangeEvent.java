package dev.mukulx.rankedsmpx.api.event;

import org.bukkit.event.HandlerList;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** A player was added to or removed from the rank blacklist. */
public final class BlacklistChangeEvent extends RankedSMPEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  private final UUID player;
  private final boolean blacklisted;

  public BlacklistChangeEvent(@NotNull UUID player, boolean blacklisted) {
    this.player = player;
    this.blacklisted = blacklisted;
  }

  @NotNull
  public UUID getPlayer() {
    return player;
  }

  /** True if the player is now blacklisted, false if they were removed. */
  public boolean isBlacklisted() {
    return blacklisted;
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
