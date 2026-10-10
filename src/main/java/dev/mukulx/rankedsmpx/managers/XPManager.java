package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import org.jetbrains.annotations.NotNull;

public class XPManager {

  private final RankedSMPXPlugin plugin;
  private final ConfigManager configManager;

  public XPManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
  }

  public double calculateMultiplier(int rank) {
    return configManager.getRanks().row(rank).xp();
  }

  public int applyMultiplier(int baseXP, int rank) {
    double multiplier = calculateMultiplier(rank);
    return (int) Math.round(baseXP * multiplier);
  }
}
