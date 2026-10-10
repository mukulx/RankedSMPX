package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.managers.HammerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HammerProtectionListener implements Listener {

  private final HammerManager hammerManager;

  public HammerProtectionListener(@NotNull HammerManager hammerManager) {
    this.hammerManager = hammerManager;
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerFallDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
      return;
    }

    if (hammerManager.hasFallProtection(player.getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    ItemStack item = event.getCurrentItem();
    if (item == null || !hammerManager.isHammer(item)) {
      return;
    }

    if (hammerManager.isOnCooldown(player)) {
      int slot = event.getSlot();

      if (slot >= 0 && slot <= 8) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onHandSwap(PlayerSwapHandItemsEvent event) {
    Player player = event.getPlayer();

    ItemStack offhand = event.getOffHandItem();
    if (offhand != null && hammerManager.isHammer(offhand)) {
      if (hammerManager.isOnCooldown(player)) {
        event.setCancelled(true);
      }
    }

    ItemStack mainhand = event.getMainHandItem();
    if (mainhand != null && hammerManager.isHammer(mainhand)) {
      if (hammerManager.isOnCooldown(player)) {
        event.setCancelled(true);
      }
    }
  }
}
