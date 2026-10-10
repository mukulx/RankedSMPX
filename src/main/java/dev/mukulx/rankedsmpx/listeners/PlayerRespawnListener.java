package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.HealthManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerRespawnListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final HealthManager healthManager;

  public PlayerRespawnListener(
      @NotNull RankedSMPXPlugin plugin, @NotNull HealthManager healthManager) {
    this.plugin = plugin;
    this.healthManager = healthManager;
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();

    if (plugin.isDebug()) {
      org.bukkit.Location loc = event.getRespawnLocation();
      org.bukkit.World world = loc.getWorld();
      plugin
          .getLogger()
          .info(
              "Player "
                  + player.getName()
                  + " respawning at: "
                  + (world != null ? world.getName() : "Unknown")
                  + " "
                  + loc.getBlockX()
                  + ", "
                  + loc.getBlockY()
                  + ", "
                  + loc.getBlockZ()
                  + " (isBedSpawn: "
                  + event.isBedSpawn()
                  + ", isAnchorSpawn: "
                  + event.isAnchorSpawn()
                  + ")");
    }

    SchedulerUtil.runForEntityLater(
        plugin,
        player,
        () -> {
          if (!player.isOnline() || player.isDead()) {
            return;
          }
          if (plugin.getInventoryManager() != null) {
            plugin.getInventoryManager().removeCombatTag(player.getUniqueId());
          }
          if (plugin.getConfig().getBoolean("health.apply-on-respawn", true)) {
            healthManager.applyHealth(player);
          }
          player.updateInventory();

          if (plugin.getTabIntegration() != null) {
            plugin.getTabIntegration().updatePlayer(player);
          }
          if (plugin.getVanillaTabManager() != null) {
            plugin.getVanillaTabManager().updatePlayer(player);
          }
        },
        1L);
  }
}
