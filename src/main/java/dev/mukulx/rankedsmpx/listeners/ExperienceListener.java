package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.XPManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.jetbrains.annotations.NotNull;

public class ExperienceListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final XPManager xpManager;

  public ExperienceListener(@NotNull RankedSMPXPlugin plugin, @NotNull XPManager xpManager) {
    this.plugin = plugin;
    this.xpManager = xpManager;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onExpChange(PlayerExpChangeEvent event) {
    Player player = event.getPlayer();

    if (!plugin.isPluginEnabled()) {
      return;
    }

    Integer rank = plugin.getRankManager().getRank(player.getUniqueId());
    if (rank == null) {
      return;
    }

    int baseXP = event.getAmount();
    if (baseXP <= 0) {
      return;
    }

    int modifiedXP = xpManager.applyMultiplier(baseXP, rank);

    if (modifiedXP != baseXP) {
      event.setAmount(modifiedXP);

      if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                String.format(
                    "Applied XP multiplier to %s (Rank %d): %d XP * %.2fx = %d XP",
                    player.getName(), rank, baseXP, (double) modifiedXP / baseXP, modifiedXP));
      }
    }
  }
}
