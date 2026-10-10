package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.InventoryManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class CombatListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final InventoryManager inventoryManager;

  public CombatListener(
      @NotNull RankedSMPXPlugin plugin, @NotNull InventoryManager inventoryManager) {
    this.plugin = plugin;
    this.inventoryManager = inventoryManager;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    Player attacker = null;
    if (event.getDamager() instanceof Player player) {
      attacker = player;
    } else if (event.getDamager() instanceof Projectile projectile
        && projectile.getShooter() instanceof Player shooter) {
      attacker = shooter;
    }
    if (attacker == null) {
      return;
    }

    if (!(event.getEntity() instanceof Player victim) || victim.equals(attacker)) {
      return;
    }

    inventoryManager.tagCombat(attacker.getUniqueId());
    inventoryManager.tagCombat(victim.getUniqueId());
  }
}
