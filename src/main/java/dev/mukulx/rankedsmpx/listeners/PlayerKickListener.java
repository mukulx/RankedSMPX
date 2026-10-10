package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerKickListener implements Listener {

  private final RankedSMPXPlugin plugin;

  public PlayerKickListener(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerKick(PlayerKickEvent event) {
    Player player = event.getPlayer();

    if (player.getOpenInventory() != null) {
      var extraInv = plugin.getInventoryManager().getExtraInventory(player.getUniqueId());
      if (extraInv != null && player.getOpenInventory().getTopInventory().equals(extraInv)) {
        player.closeInventory();
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin.getLogger().info("Closed extra inventory for kicked player: " + player.getName());
        }
      }
    }

    Integer rank = plugin.getRankManager().getRank(player.getUniqueId());
    if (rank != null) {
      dev.mukulx.rankedsmpx.utils.SchedulerUtil.runAsync(plugin, () -> {
        plugin.getRankManager().saveRank(player.getUniqueId(), rank);
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin.getLogger().info("Saved rank for kicked player " + player.getName() + ": #" + rank);
        }
      });
    }

    if (plugin.getInventoryManager().hasExtraInventory(player)) {
      plugin.getInventoryManager().saveInventory(player.getUniqueId());
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Saved extra inventory for kicked player " + player.getName());
      }
    }

    if (plugin.getPotionEffectStore() != null) {
      plugin.getPotionEffectStore().saveEffects(player);
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Saved potion effects for kicked player " + player.getName());
      }
    }

    if (plugin.getPotionEffectListener() != null) {
      plugin.getPotionEffectListener().cleanupPlayer(player.getUniqueId());
    }

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Player kicked: " + player.getName() + " - Data saved successfully");
    }
  }
}
