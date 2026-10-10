package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.InventoryManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class ExtraInvSeeListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final InventoryManager inventoryManager;

  public ExtraInvSeeListener(
      @NotNull RankedSMPXPlugin plugin, @NotNull InventoryManager inventoryManager) {
    this.plugin = plugin;
    this.inventoryManager = inventoryManager;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryClose(@NotNull InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) {
      return;
    }

    UUID adminUUID = player.getUniqueId();

    if (!inventoryManager.isAdminViewing(adminUUID)) {
      return;
    }

    Inventory closedInv = event.getInventory();
    Inventory trackedInv = inventoryManager.getAdminViewingInventory(adminUUID);

    if (trackedInv == null || !closedInv.equals(trackedInv)) {
      return;
    }

    UUID targetUUID = inventoryManager.getAdminViewingTarget(adminUUID);
    if (targetUUID == null) {
      return;
    }

    inventoryManager.saveAdminViewingChanges(adminUUID);

    inventoryManager.stopTrackingAdminViewing(adminUUID);

    plugin
        .getLogger()
        .info(
            player.getName()
                + " closed extra inventory view for "
                + targetUUID
                + " (changes saved)");

    player.sendMessage("§aChanges saved to extra inventory.");
  }
}
