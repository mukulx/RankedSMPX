package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RanksGUIListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final MessageManager messageManager;

  private static final String MANAGEMENT_GUI_TITLE = "§6§lRᴀɴᴋ §b§lMᴀɴᴀɢᴇᴍᴇɴᴛ";

  public RanksGUIListener(
      @NotNull RankedSMPXPlugin plugin, @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.messageManager = messageManager;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInventoryClick(@NotNull InventoryClickEvent event) {
    InventoryView view = event.getView();
    String title = dev.mukulx.rankedsmpx.managers.MessageManager.legacyString(view.title());

    if (isRanksGUI(title)) {
      event.setCancelled(true);

      if (event.isShiftClick()) {
        event.setCancelled(true);
      }
      return;
    }

    if (isManagementGUI(title)) {
      event.setCancelled(true);

      if (event.isShiftClick()) {
        event.setCancelled(true);
      }

      if (!(event.getWhoClicked() instanceof Player player)) {
        return;
      }

      if (!player.hasPermission("rankedsmp.gui")) {
        player.closeInventory();
        messageManager.sendMessage(player, "general.no-permission");
        return;
      }

      if (event.getClickedInventory() == null
          || !event.getClickedInventory().equals(view.getTopInventory())) {
        return;
      }

      ItemStack clicked = event.getCurrentItem();
      if (clicked == null || clicked.getType() == Material.AIR) {
        return;
      }

      int slot = event.getSlot();

      if (slot >= 45) {
        handleControlButton(player, clicked, slot);
        return;
      }

      if (clicked.getType() == Material.PLAYER_HEAD) {
        handlePlayerHeadClick(player, clicked, event.getClick());
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onInventoryDrag(@NotNull InventoryDragEvent event) {
    InventoryView view = event.getView();
    String title = dev.mukulx.rankedsmpx.managers.MessageManager.legacyString(view.title());

    if (isRanksGUI(title) || isManagementGUI(title)) {
      event.setCancelled(true);
    }
  }

  private boolean isRanksGUI(@NotNull String title) {
    String plain = title.replaceAll("§[0-9a-fk-or]", "");
    return plain.matches("(?i)(Tᴏᴘ|ᴛᴏᴘ|Top) \\d+ (Rᴀɴᴋᴇᴅ|ʀᴀɴᴋᴇᴅ|Ranked) .*");
  }

  private boolean isManagementGUI(@NotNull String title) {
    if (title.equals(MANAGEMENT_GUI_TITLE)) {
      return true;
    }

    String plainTitle = title.replaceAll("§[0-9a-fk-or]", "");
    return plainTitle.contains("ʀᴀɴᴋ")
        || plainTitle.contains("ᴍᴀɴᴀɢᴇᴍᴇɴᴛ")
        || (plainTitle.contains("Rank") && plainTitle.contains("Management"));
  }

  private void handleControlButton(@NotNull Player player, @NotNull ItemStack item, int slot) {
    switch (slot) {
      case 45 -> {
        player.closeInventory();
        player.sendMessage(
            messageManager.getPrefix() + messageManager.applySmallCaps("§7GUI closed."));
      }
      case 49 -> {
        player.closeInventory();
        SchedulerUtil.runForEntityLater(plugin, player, () -> player.performCommand("rsmp manage"), 1L);
        player.sendMessage(
            messageManager.getPrefix() + messageManager.applySmallCaps("§aRefreshing..."));
      }
      case 53 -> {
      }
    }
  }

  private void handlePlayerHeadClick(
      @NotNull Player player, @NotNull ItemStack head, @NotNull ClickType clickType) {
    if (!(head.getItemMeta() instanceof SkullMeta meta)) {
      return;
    }

    OfflinePlayer target = meta.getOwningPlayer();
    if (target == null) {
      return;
    }

    UUID targetUUID = target.getUniqueId();
    Integer currentRank = plugin.getRankManager().getRank(targetUUID);

    if (currentRank == null) {
      player.sendMessage(
          messageManager.getPrefix()
              + messageManager.applySmallCaps("§cPlayer no longer has a rank!"));
      return;
    }

    String targetName = target.getName() != null ? target.getName() : "Unknown";
    int totalRanks = plugin.getConfigManager().getTotalRanks();

    switch (clickType) {
      case LEFT -> {
        if (currentRank == 1) {
          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps("§c" + targetName + " is already Rank #1!"));
          return;
        }

        int newRank = currentRank - 1;

        UUID occupantUUID = findPlayerWithRank(newRank);
        if (occupantUUID != null && !occupantUUID.equals(targetUUID)) {
          plugin.getRankManager().swapRanks(targetUUID, occupantUUID);
          applyRankChanges(targetUUID);
          applyRankChanges(occupantUUID);

          OfflinePlayer occupant = Bukkit.getOfflinePlayer(occupantUUID);
          String occupantName = occupant.getName() != null ? occupant.getName() : "Unknown";

          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps(
                      "§aSwapped ranks: §b"
                          + targetName
                          + " §a(#"
                          + currentRank
                          + " → #"
                          + newRank
                          + ") ↔ §b"
                          + occupantName
                          + " §a(#"
                          + newRank
                          + " → #"
                          + currentRank
                          + ")"));
        } else {
          plugin.getRankManager().setRank(targetUUID, newRank);
          applyRankChanges(targetUUID);

          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps(
                      "§aPromoted §b"
                          + targetName
                          + " §afrom §6#"
                          + currentRank
                          + " §ato §6#"
                          + newRank));
        }

        refreshGUI(player);
      }
      case RIGHT -> {
        if (currentRank >= totalRanks) {
          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps(
                      "§c" + targetName + " is already at the lowest rank!"));
          return;
        }

        int newRank = currentRank + 1;

        UUID occupantUUID = findPlayerWithRank(newRank);
        if (occupantUUID != null && !occupantUUID.equals(targetUUID)) {
          plugin.getRankManager().swapRanks(targetUUID, occupantUUID);
          applyRankChanges(targetUUID);
          applyRankChanges(occupantUUID);

          OfflinePlayer occupant = Bukkit.getOfflinePlayer(occupantUUID);
          String occupantName = occupant.getName() != null ? occupant.getName() : "Unknown";

          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps(
                      "§aSwapped ranks: §b"
                          + targetName
                          + " §a(#"
                          + currentRank
                          + " → #"
                          + newRank
                          + ") ↔ §b"
                          + occupantName
                          + " §a(#"
                          + newRank
                          + " → #"
                          + currentRank
                          + ")"));
        } else {
          plugin.getRankManager().setRank(targetUUID, newRank);
          applyRankChanges(targetUUID);

          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps(
                      "§cDemoted §b"
                          + targetName
                          + " §cfrom §6#"
                          + currentRank
                          + " §cto §6#"
                          + newRank));
        }

        refreshGUI(player);
      }
      case SHIFT_LEFT -> {
        if (currentRank == 1) {
          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps("§c" + targetName + " is already Rank #1!"));
          return;
        }

        UUID occupantUUID = findPlayerWithRank(1);
        if (occupantUUID != null && !occupantUUID.equals(targetUUID)) {
          plugin.getRankManager().swapRanks(targetUUID, occupantUUID);
          applyRankChanges(targetUUID);
          applyRankChanges(occupantUUID);

          OfflinePlayer occupant = Bukkit.getOfflinePlayer(occupantUUID);
          String occupantName = occupant.getName() != null ? occupant.getName() : "Unknown";

          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps(
                      "§aSwapped ranks: §b"
                          + targetName
                          + " §a(#"
                          + currentRank
                          + " → §6§l#1§a) ↔ §b"
                          + occupantName
                          + " §a(§6§l#1 §a→ #"
                          + currentRank
                          + ")"));
        } else {
          plugin.getRankManager().setRank(targetUUID, 1);
          applyRankChanges(targetUUID);

          player.sendMessage(
              messageManager.getPrefix()
                  + messageManager.applySmallCaps(
                      "§aSet §b" + targetName + " §ato §6§l#1 §a(was #" + currentRank + ")"));
        }

        refreshGUI(player);
      }
      case SHIFT_RIGHT -> {
        plugin.getRankManager().removeRank(targetUUID);
        applyRankChanges(targetUUID);

        player.sendMessage(
            messageManager.getPrefix()
                + messageManager.applySmallCaps(
                    "§cRemoved rank from §b" + targetName + " §c(was #" + currentRank + ")"));

        refreshGUI(player);
      }
      default -> {
      }
    }
  }

  @Nullable
  private UUID findPlayerWithRank(int rank) {
    for (var entry : plugin.getRankManager().getAllRanks().entrySet()) {
      if (entry.getValue() == rank) {
        return entry.getKey();
      }
    }
    return null;
  }

  private void applyRankChanges(@NotNull UUID uuid) {
    Player onlinePlayer = Bukkit.getPlayer(uuid);
    Integer newRank = plugin.getRankManager().getRank(uuid);

    if (onlinePlayer != null) {
      if (newRank != null) {
        plugin.getHealthManager().applyHealth(onlinePlayer);
      } else {
        plugin.getHealthManager().resetHealth(onlinePlayer);
      }

      plugin.getInventoryManager().removeExtraInventory(uuid);

      if (plugin.getTabIntegration() != null) {
        plugin.getTabIntegration().updatePlayer(onlinePlayer);
      }
    }

    plugin.getHierarchyHammerManager().updateHammerHolder();

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
      if (roleSyncManager != null) {
        roleSyncManager.syncPlayerRole(uuid, newRank);
      }
    }
  }

  private void refreshGUI(@NotNull Player player) {
    player.closeInventory();
    SchedulerUtil.runForEntityLater(plugin, player, () -> player.performCommand("rsmp manage"), 2L);
  }
}
