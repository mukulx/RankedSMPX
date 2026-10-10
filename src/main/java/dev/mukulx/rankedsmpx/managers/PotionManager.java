package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import org.jetbrains.annotations.NotNull;

public class PotionManager {

  private final RankedSMPXPlugin plugin;
  private final ConfigManager configManager;

  public PotionManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
  }

  public double calculateMultiplier(int rank) {
    return configManager.getRanks().row(rank).potion();
  }

  public int applyMultiplier(int baseDuration, int rank) {
    double multiplier = calculateMultiplier(rank);
    // Round up so a buff never feels weaker than the multiplier says.
    int result = (int) Math.ceil(baseDuration * multiplier);

    if (configManager.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(
              String.format(
                  "Potion duration calculation: %d ticks * %.2fx = %d ticks (%.1f seconds -> %.1f seconds)",
                  baseDuration, multiplier, result, baseDuration / 20.0, result / 20.0));
    }

    return result;
  }
}
