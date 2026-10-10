package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class PlayerQuitListener implements Listener {

  private final RankedSMPXPlugin plugin;

  public PlayerQuitListener(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();

    if (!plugin.isPluginEnabled()) {
      return;
    }

    if (plugin.getConfig().getBoolean("rank-display.show-on-leave", true)) {
      String prefix = getRankPrefixMiniMessage(player);

      net.kyori.adventure.text.minimessage.MiniMessage mm =
          net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
      event.quitMessage(mm.deserialize(prefix + "<aqua>" + player.getName() + " left the game</yellow>"));
    }

    if (player.getOpenInventory() != null) {
      var extraInv = plugin.getInventoryManager().getExtraInventory(player.getUniqueId());
      if (extraInv != null && player.getOpenInventory().getTopInventory().equals(extraInv)) {
        player.closeInventory();
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin
              .getLogger()
              .info("Closed extra inventory for quitting player: " + player.getName());
        }
      }
    }

    Integer rank = plugin.getRankManager().getRank(player.getUniqueId());
    if (rank != null) {
      plugin.getRankManager().saveRankAsync(player.getUniqueId(), rank);
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Queued rank save for " + player.getName() + ": #" + rank);
      }
    }

    Inventory extraInvToSave = plugin.getInventoryManager().getExtraInventory(player.getUniqueId());
    if (extraInvToSave != null) {
      plugin.getInventoryManager().saveInventoryAsync(player.getUniqueId());
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Queued inventory save for " + player.getName());
      }
    }

    if (plugin.getPotionEffectStore() != null) {
      plugin.getPotionEffectStore().saveEffects(player);
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Saved potion effects for " + player.getName());
      }
    }

    plugin.getInventoryManager().clearInventoryOnQuit(player.getUniqueId());

    if (plugin.getVanillaTabManager() != null) {
      plugin.getVanillaTabManager().removePlayer(player);
    }

    if (plugin.getPotionEffectListener() != null) {
      plugin.getPotionEffectListener().cleanupPlayer(player.getUniqueId());
    }

    if (rank != null && rank == 1) {
      plugin.getHierarchyHammerManager().removeHammerOffline(player.getUniqueId());
    }

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      var updatesManager = plugin.getDiscordManager().getUpdatesManager();
      if (updatesManager != null) {
        updatesManager.postPlayerLeave(player.getName());
      }
    }

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Player quit: " + player.getName() + " - Data saved successfully");
    }
  }

  private String getRankPrefixMiniMessage(Player player) {
    Integer rank = plugin.getRankManager().getRank(player.getUniqueId());
    boolean isBlacklisted = plugin.getDatabaseManager().isBlacklisted(player.getUniqueId());
    return plugin.getMessageManager().getRankPrefixMiniMessage(rank, isBlacklisted);
  }
}
