package dev.mukulx.rankedsmpx.api.event;

import org.bukkit.event.HandlerList;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/** A player linked or unlinked their Discord account. */
public final class DiscordLinkChangeEvent extends RankedSMPEvent {

  private static final HandlerList HANDLERS = new HandlerList();

  private final UUID player;
  private final String discordId;
  private final boolean linked;

  public DiscordLinkChangeEvent(@NotNull UUID player, @NotNull String discordId, boolean linked) {
    this.player = player;
    this.discordId = discordId;
    this.linked = linked;
  }

  @NotNull
  public UUID getPlayer() {
    return player;
  }

  @NotNull
  public String getDiscordId() {
    return discordId;
  }

  /** True if the accounts were linked, false if the link was removed. */
  public boolean isLinked() {
    return linked;
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
