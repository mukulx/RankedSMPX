package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.InventoryManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final InventoryManager inventoryManager;

  private final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
  private final Map<UUID, Integer> clickCount = new ConcurrentHashMap<>();
  private static final long CLICK_WINDOW = 100; // 100ms window
  private static final int MAX_CLICKS_PER_WINDOW = 10; // Max 10 clicks per 100ms

  private final Map<UUID, Boolean> inventoryOpen = new ConcurrentHashMap<>();

  public InventoryListener(
      @NotNull RankedSMPXPlugin plugin, @NotNull InventoryManager inventoryManager) {
    this.plugin = plugin;
    this.inventoryManager = inventoryManager;
  }

  private boolean isRapidClicking(@NotNull UUID uuid) {
    long now = System.currentTimeMillis();
    Long lastClick = lastClickTime.get(uuid);

    if (lastClick == null || now - lastClick > CLICK_WINDOW) {
      lastClickTime.put(uuid, now);
      clickCount.put(uuid, 1);
      return false;
    }

    int count = clickCount.getOrDefault(uuid, 0) + 1;
    clickCount.put(uuid, count);

    if (count > MAX_CLICKS_PER_WINDOW) {
      plugin
          .getLogger()
          .warning(
              "Rapid clicking detected for "
                  + uuid
                  + " ("
                  + count
                  + " clicks in "
                  + (now - lastClick)
                  + "ms)");
      return true;
    }

    return false;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryOpen(InventoryOpenEvent event) {
    if (!(event.getPlayer() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();
    Inventory extraInv = inventoryManager.getExtraInventory(uuid);

    if (extraInv != null && event.getInventory().equals(extraInv)) {
      if (plugin.getConfigManager().isAntiDupeEnabled() && Boolean.TRUE.equals(inventoryOpen.get(uuid))) {
        event.setCancelled(true);
        plugin
            .getLogger()
            .warning(
                "Prevented duplicate inventory open for " + player.getName() + " (already open)");
        return;
      }

      if (!inventoryManager.hasExtraInventory(player)) {
        event.setCancelled(true);
        plugin
            .getMessageManager()
            .sendPrefixedMessage(player, "&cYou no longer have access to extra inventory!");
        plugin
            .getLogger()
            .warning("Prevented inventory open for " + player.getName() + " (no access)");
        return;
      }

      inventoryOpen.put(uuid, true);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();
    Inventory clickedInv = event.getClickedInventory();
    if (clickedInv == null) {
      return;
    }

    Inventory extraInv = inventoryManager.getExtraInventory(uuid);
    if (extraInv == null) {
      return;
    }

    if (plugin.getConfigManager().isAntiDupeEnabled() && isRapidClicking(uuid)) {
      event.setCancelled(true);
      plugin
          .getMessageManager()
          .sendLegacyMessage(player, "&c&lSLOW DOWN! &7You're clicking too fast!");
      plugin.getLogger().warning("Blocked rapid clicking for " + player.getName());
      return;
    }

    if (!inventoryManager.hasExtraInventory(player)) {
      if (clickedInv.equals(extraInv) || event.getInventory().equals(extraInv)) {
        event.setCancelled(true);
        player.closeInventory();
        plugin
            .getMessageManager()
            .sendPrefixedMessage(player, "&cYou no longer have access to extra inventory!");
        plugin
            .getLogger()
            .warning("Closed inventory for " + player.getName() + " (access lost during use)");
        return;
      }
    }

    if (plugin.getConfigManager().isAntiDupeEnabled() && event.getClick() == ClickType.DOUBLE_CLICK) {
      if (clickedInv.equals(extraInv)) {
        event.setCancelled(true);
        plugin.getLogger().info("Blocked double-click in extra inventory for " + player.getName());
        return;
      }
    }

    if (plugin.getConfigManager().isAntiDupeEnabled() && player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
      if (clickedInv.equals(extraInv) || event.getInventory().equals(extraInv)) {
        event.setCancelled(true);
        plugin
            .getMessageManager()
            .sendLegacyMessage(
                player, "&c&lNOT ALLOWED! &7Cannot use extra inventory in creative mode!");
        return;
      }
    }

    if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
      if (clickedInv.equals(extraInv)) {
        event.setCancelled(true);
        plugin
            .getMessageManager()
            .sendLegacyMessage(player, "&c&lLOCKED! &7Rank up to unlock more slots!");
        return;
      }
    }

    if (event.isShiftClick()
        && event.getCurrentItem() != null
        && event.getCurrentItem().getType() == Material.BARRIER) {
      event.setCancelled(true);
      return;
    }

    if (event.getSlot() >= 0 && clickedInv.equals(extraInv)) {
      ItemStack slotItem = clickedInv.getItem(event.getSlot());
      if (slotItem != null && slotItem.getType() == Material.BARRIER) {
        event.setCancelled(true);
        plugin
            .getMessageManager()
            .sendLegacyMessage(player, "&c&lLOCKED! &7Rank up to unlock more slots!");
        return;
      }
    }

    if (event.getClick() == ClickType.NUMBER_KEY) {
      if (clickedInv.equals(extraInv)) {
        ItemStack slotItem = clickedInv.getItem(event.getSlot());
        if (slotItem != null && slotItem.getType() == Material.BARRIER) {
          event.setCancelled(true);
          return;
        }
      }
    }

    if (event.getClick() == ClickType.DROP || event.getClick() == ClickType.CONTROL_DROP) {
      if (clickedInv.equals(extraInv)) {
        ItemStack slotItem = clickedInv.getItem(event.getSlot());
        if (slotItem != null && slotItem.getType() == Material.BARRIER) {
          event.setCancelled(true);
          return;
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();
    Inventory extraInv = inventoryManager.getExtraInventory(uuid);
    if (extraInv == null) {
      return;
    }

    if (!inventoryManager.hasExtraInventory(player)) {
      if (event.getInventory().equals(extraInv)) {
        event.setCancelled(true);
        player.closeInventory();
        plugin
            .getMessageManager()
            .sendPrefixedMessage(player, "&cYou no longer have access to extra inventory!");
        return;
      }
    }

    if (event.getInventory().equals(extraInv)) {
      for (int slot : event.getRawSlots()) {
        if (slot < extraInv.getSize()) {
          ItemStack slotItem = extraInv.getItem(slot);
          if (slotItem != null && slotItem.getType() == Material.BARRIER) {
            event.setCancelled(true);
            plugin
                .getMessageManager()
                .sendLegacyMessage(player, "&c&lLOCKED! &7Cannot drag items into locked slots!");
            return;
          }
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInventoryMoveItem(InventoryMoveItemEvent event) {
    if (!plugin.getConfigManager().isAntiDupeEnabled()) {
      return;
    }
    Inventory dest = event.getDestination();
    Inventory source = event.getSource();

    for (UUID uuid : inventoryManager.getAllExtraInventoryUUIDs()) {
      Inventory extraInv = inventoryManager.getExtraInventory(uuid);
      if (extraInv != null && (dest.equals(extraInv) || source.equals(extraInv))) {
        event.setCancelled(true);
        plugin
            .getLogger()
            .warning("Blocked hopper/dropper interaction with extra inventory for " + uuid);
        return;
      }
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onInventoryClose(InventoryCloseEvent event) {
    if (!(event.getPlayer() instanceof Player player)) {
      return;
    }

    UUID uuid = player.getUniqueId();

    Inventory extraInv = inventoryManager.getExtraInventory(uuid);
    if (extraInv != null && event.getInventory().equals(extraInv)) {
      inventoryManager.saveInventory(uuid);

      inventoryOpen.remove(uuid);

      lastClickTime.remove(uuid);
      clickCount.remove(uuid);
    } else if (inventoryManager.saveRetiredOnClose(event.getInventory())) {
      inventoryOpen.remove(uuid);
      lastClickTime.remove(uuid);
      clickCount.remove(uuid);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerQuit(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();

    inventoryOpen.remove(uuid);
    lastClickTime.remove(uuid);
    clickCount.remove(uuid);
  }
}
