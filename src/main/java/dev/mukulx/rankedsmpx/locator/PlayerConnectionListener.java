package dev.mukulx.rankedsmpx.locator;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerConnectionListener implements Listener {

  @EventHandler(priority = EventPriority.MONITOR)
  public void onJoin(PlayerJoinEvent e) {
    Player player = e.getPlayer();
    LocatorBarFilter.inject(player);
    if (LocatorBarManager.get() != null) {
      LocatorBarManager.get().onPlayerJoin(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent e) {
    if (LocatorBarManager.get() != null) {
      LocatorBarManager.get().onPlayerQuit(e.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onWorldChange(PlayerChangedWorldEvent e) {
    Player player = e.getPlayer();
    LocatorBarFilter.inject(player);
    if (LocatorBarManager.get() != null) {
      LocatorBarManager.get().onPlayerWorldChange(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onRespawn(PlayerRespawnEvent e) {
    Player player = e.getPlayer();
    LocatorBarFilter.inject(player);
    if (LocatorBarManager.get() != null) {
      LocatorBarManager.get().onPlayerRespawn(player);
    }
  }
}
