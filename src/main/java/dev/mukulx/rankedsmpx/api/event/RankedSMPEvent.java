package dev.mukulx.rankedsmpx.api.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

/**
 * Base class of all RankedSMPX events. They are announced after the change has happened, so they
 * cannot be cancelled. An event can fire off the main thread, so check {@link #isAsynchronous()}
 * before touching the world.
 */
public abstract class RankedSMPEvent extends Event {

  protected RankedSMPEvent() {
    super(Bukkit.getServer() != null && !Bukkit.isPrimaryThread());
  }
}
