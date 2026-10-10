package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.locator.LocatorBarAPI;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

public class DragonEggLocatorListener implements Listener {

  private static final double MAX_RANGE = 6.0E7D; // 60 million blocks

  private final RankedSMPXPlugin plugin;
  private final Set<UUID> playersWithEgg = java.util.concurrent.ConcurrentHashMap.newKeySet();

  public DragonEggLocatorListener(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    try {
      if (LocatorBarAPI.isAvailable()) {
        LocatorBarAPI.hideAll();
      }
    } catch (Throwable ignored) {
    }
  }

  public void initializeOnlinePlayers() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            if (!player.isOnline()) return;
            boolean hasEgg = hasEggInInventory(player);
            setAttributes(player, hasEgg);
            if (hasEgg) {
              playersWithEgg.add(player.getUniqueId());
              enableLocatorBar(player);
            } else {
              disableLocatorBar(player);
            }
          });
    }
  }

  public void initializePlayer(@NotNull Player player) {
    boolean hasEgg = hasEggInInventory(player);
    setAttributes(player, hasEgg);
    if (hasEgg) {
      playersWithEgg.add(player.getUniqueId());
      enableLocatorBar(player);
    } else {
      disableLocatorBar(player);
    }
  }

  private void enableLocatorBar(@NotNull Player player) {
    try {
      if (LocatorBarAPI.isAvailable()) {
        LocatorBarAPI.show(player);
        LocatorBarAPI.clearTracking(player);
      }
    } catch (Throwable ignored) {
    }
    syncWaypointsForPlayer(player);
  }

  private void disableLocatorBar(@NotNull Player player) {
    try {
      if (LocatorBarAPI.isAvailable()) {
        LocatorBarAPI.hide(player);
      }
    } catch (Throwable ignored) {
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    SchedulerUtil.runForEntityLater(
        plugin,
        player,
        () -> {
          if (!player.isOnline()) return;

          initializePlayer(player);

          for (UUID uuid : playersWithEgg) {
            Player eggHolder = Bukkit.getPlayer(uuid);
            if (eggHolder != null && eggHolder.isOnline()) {
              SchedulerUtil.runForEntity(plugin, eggHolder, () -> {
                if (eggHolder.isOnline()) {
                  enableLocatorBar(eggHolder);
                }
              });
            }
          }
        },
        10L);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    playersWithEgg.remove(player.getUniqueId());
    try {
      if (LocatorBarAPI.isAvailable()) {
        LocatorBarAPI.reset(player);
      }
    } catch (Throwable ignored) {
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerDeath(PlayerDeathEvent event) {
    scheduleCheck(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    scheduleCheck(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    HumanEntity humanEntity = event.getWhoClicked();
    if (humanEntity instanceof Player) {
      scheduleCheck((Player) humanEntity);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    HumanEntity humanEntity = event.getPlayer();
    if (humanEntity instanceof Player) {
      scheduleCheck((Player) humanEntity);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onItemDrop(PlayerDropItemEvent event) {
    if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
      if (plugin.getConfig().getBoolean("dragon-egg-locator.prevent-drop", false)) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(
            plugin.getMessageManager().getPrefix()
                + plugin.getMessageManager().applySmallCaps("§cYou cannot drop the Dragon Egg!"));
        return;
      }
      scheduleCheck(event.getPlayer());
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (event.getBlock().getType() == Material.DRAGON_EGG) {
      if (plugin.getConfig().getBoolean("dragon-egg-locator.prevent-drop", false)) {
        event.setCancelled(true);
        event.getPlayer().sendMessage("§cYou cannot place the Dragon Egg!");
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onItemPickup(EntityPickupItemEvent event) {
    LivingEntity livingEntity = event.getEntity();
    if (!(livingEntity instanceof Player)) return;

    if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
      scheduleCheck((Player) livingEntity);
    }
  }

  private void scheduleCheck(@NotNull Player player) {
    SchedulerUtil.runForEntityLater(
        plugin, player, () -> checkAndUpdate(player), 2L);
  }

  private void checkAndUpdate(@NotNull Player player) {
    if (!player.isOnline()) return;

    boolean hasEgg = hasEggInInventory(player);
    boolean hadEggBefore = playersWithEgg.contains(player.getUniqueId());

    if (hasEgg == hadEggBefore) return;

    if (hasEgg) {
      playersWithEgg.add(player.getUniqueId());
      enableLocatorBar(player);
    } else {
      playersWithEgg.remove(player.getUniqueId());
      disableLocatorBar(player);
    }

    setAttributes(player, hasEgg);
  }

  public boolean hasEggInInventory(@NotNull Player player) {
    return player.getInventory().contains(Material.DRAGON_EGG);
  }

  private void setAttributes(@NotNull Player player, boolean hasEgg) {
    try {
      AttributeInstance transmit = player.getAttribute(Attribute.WAYPOINT_TRANSMIT_RANGE);
      AttributeInstance receive = player.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);

      if (transmit != null) {
        transmit.setBaseValue(MAX_RANGE);
      }

      if (receive != null) {
        receive.setBaseValue(hasEgg ? MAX_RANGE : 0.0D);
      }
    } catch (Throwable ignored) {
    }
  }

  private void syncWaypointsForPlayer(@NotNull Player viewer) {
    try {
      for (Player other : Bukkit.getOnlinePlayers()) {
        if (other.equals(viewer)) continue;
        try {
          viewer.hidePlayer(plugin, other);
        } catch (Throwable ignored) {
        }
      }

      SchedulerUtil.runForEntityLater(
          plugin,
          viewer,
          () -> {
            if (!viewer.isOnline()) return;

            for (Player other : Bukkit.getOnlinePlayers()) {
              if (other.equals(viewer)) continue;
              try {
                viewer.showPlayer(plugin, other);
              } catch (Throwable ignored) {
              }
            }
          },
          1L);
    } catch (Throwable ignored) {
    }
  }

  public void cleanup() {
    try {
      if (LocatorBarAPI.isAvailable()) {
        LocatorBarAPI.revertAll();
      }
    } catch (Throwable ignored) {
    }
    playersWithEgg.clear();
    plugin.getLogger().info("Dragon Egg Locator cleaned up");
  }
}
