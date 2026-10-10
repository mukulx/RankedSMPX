package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

public class ConfigManager {

  private final RankedSMPXPlugin plugin;
  private final File ranksFile;
  private FileConfiguration config;
  private volatile RankTable ranks;

  public ConfigManager(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    this.config = plugin.getConfig();
    this.ranksFile = new File(plugin.getDataFolder(), "ranks.yml");
    this.ranks = RankTableFile.loadOrCreate(ranksFile, config, plugin.getLogger());
  }

  public void reload() {
    plugin.reloadConfig();
    this.config = plugin.getConfig();
    this.ranks = RankTableFile.loadOrCreate(ranksFile, config, plugin.getLogger());
  }

  @NotNull
  public FileConfiguration getConfig() {
    return config;
  }

  @NotNull
  public RankTable getRanks() {
    return ranks;
  }

  public void setRankPrefixes(@NotNull Map<Integer, String> prefixes) throws IOException {
    RankTable updated = ranks.withPrefixes(prefixes);
    RankTableFile.write(ranksFile, updated);
    ranks = updated;
  }

  public int getTotalRanks() {
    return ranks.totalRanks();
  }

  public boolean isRankStealingEnabled() {
    return config.getBoolean("season.rank-stealing.enabled", true);
  }

  public int getMaxRankDifference() {
    return config.getInt("season.rank-stealing.max-rank-difference", 0);
  }

  public boolean isInventoryEnabled() {
    return config.getBoolean("inventory.enabled", true);
  }

  public boolean isAntiDupeEnabled() {
    return config.getBoolean("inventory.anti-dupe", true);
  }

  public int getExtraSlots(int rank) {
    return ranks.row(rank).slots();
  }
}
