package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Enforces prevent-storage and prevent-drop for the hammer, the spear and the dragon egg. */
public class ItemStorageListener implements Listener {

  /** The three guarded items, each with the config section that holds its flags. */
  private enum Guarded {
    SPEAR("spear", "special items"),
    HAMMER("hierarchy-hammer", "special items"),
    DRAGON_EGG("dragon-egg-locator", "the Dragon Egg");

    final String section;
    final String label;

    Guarded(String section, String label) {
      this.section = section;
      this.label = label;
    }
  }

  private final RankedSMPXPlugin plugin;

  public ItemStorageListener(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onInventoryClick(InventoryClickEvent event) {
    Inventory top = event.getView().getTopInventory();
    if (!isStorage(top, event.getView().title())) {
      return;
    }

    Inventory clicked = event.getClickedInventory();
    if (clicked == null) {
      return;
    }
    boolean clickedTop = clicked == top;
    Player player = event.getWhoClicked() instanceof Player p ? p : null;

    ItemStack moving;
    if (event.isShiftClick()) {
      moving = clickedTop ? null : event.getCurrentItem();
    } else if (event.getClick() == ClickType.NUMBER_KEY && player != null && event.getHotbarButton() >= 0) {
      moving = clickedTop ? player.getInventory().getItem(event.getHotbarButton()) : null;
    } else if (event.getClick() == ClickType.SWAP_OFFHAND && player != null) {
      moving = clickedTop ? player.getInventory().getItemInOffHand() : null;
    } else {
      moving = clickedTop ? event.getCursor() : null;
    }

    deny(event.getWhoClicked(), moving, "prevent-storage", () -> event.setCancelled(true), top.getType());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onInventoryDrag(InventoryDragEvent event) {
    Inventory top = event.getView().getTopInventory();
    if (!isStorage(top, event.getView().title())) {
      return;
    }
    boolean intoTop = event.getRawSlots().stream().anyMatch(slot -> slot < top.getSize());
    if (intoTop) {
      deny(event.getWhoClicked(), event.getOldCursor(), "prevent-storage", () -> event.setCancelled(true), top.getType());
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerDropItem(PlayerDropItemEvent event) {
    deny(
        event.getPlayer(),
        event.getItemDrop().getItemStack(),
        "prevent-drop",
        () -> event.setCancelled(true),
        null);
  }

  /** Cancels the action and tells the player if the item is guarded by the given flag. */
  private void deny(
      @NotNull HumanEntity who,
      @Nullable ItemStack item,
      @NotNull String flag,
      @NotNull Runnable cancel,
      @Nullable InventoryType into) {
    Guarded kind = kindOf(item);
    if (kind == null || !plugin.getConfig().getBoolean(kind.section + "." + flag, false)) {
      return;
    }
    cancel.run();
    String action = into == null ? "drop" : "store";
    String where = into == null ? "" : into == InventoryType.CHEST ? " in chests" : " in containers";
    who.sendMessage(
        plugin
            .getMessageManager()
            .applySmallCaps("§cYou cannot " + action + " " + kind.label + where + "!"));
  }

  @Nullable
  private Guarded kindOf(@Nullable ItemStack item) {
    if (item == null || item.getType().isAir()) {
      return null;
    }
    if (item.getType() == Material.DRAGON_EGG) {
      return Guarded.DRAGON_EGG;
    }
    if (plugin.getSpearManager() != null && plugin.getSpearManager().isSpear(item)) {
      return Guarded.SPEAR;
    }
    if (item.getType() == Material.MACE
        && plugin.getHierarchyHammerManager() != null
        && plugin.getHierarchyHammerManager().isHierarchyHammer(item)) {
      return Guarded.HAMMER;
    }
    return null;
  }

  private boolean isStorage(@NotNull Inventory inventory, @NotNull net.kyori.adventure.text.Component title) {
    if (isBlockedContainer(inventory.getType())) {
      return true;
    }
    String plain = MessageManager.plain(title);
    return plain.contains("Extra Inventory") || plain.contains("Eхтяα Iηνєηтσяу");
  }

  private boolean isBlockedContainer(InventoryType type) {
    return switch (type) {
      case CHEST,
              ENDER_CHEST,
              BARREL,
              SHULKER_BOX,
              HOPPER,
              DROPPER,
              DISPENSER,
              FURNACE,
              BLAST_FURNACE,
              SMOKER,
              BREWING,
              ANVIL,
              GRINDSTONE,
              CARTOGRAPHY,
              LOOM,
              STONECUTTER,
              SMITHING,
              COMPOSTER ->
          true;
      default -> false;
    };
  }
}
