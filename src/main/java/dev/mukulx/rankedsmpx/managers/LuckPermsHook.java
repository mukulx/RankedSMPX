package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LuckPermsHook {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private ContextCalculator<Player> contextCalculator;
  private boolean hooked = false;

  public LuckPermsHook(@NotNull RankedSMPXPlugin plugin, @NotNull RankManager rankManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
  }

  public void initialize() {
    if (!plugin.getConfig().getBoolean("luckperms.enabled", false)) {
      return;
    }

    if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
      return;
    }

    try {
      LuckPerms luckPerms = LuckPermsProvider.get();
      if (luckPerms == null) {
        return;
      }

      if (plugin.getConfig().getBoolean("luckperms.register-contexts", true)) {
        this.contextCalculator = new RankedSMPContextCalculator();
        luckPerms.getContextManager().registerCalculator(contextCalculator);
      }
      this.hooked = true;
      plugin.getLogger().info("Successfully hooked into LuckPerms! Registered RankedSMP contexts.");
    } catch (Throwable t) {
      plugin.getLogger().warning("Failed to hook into LuckPerms: " + t.getMessage());
    }
  }

  public void signalUpdate(@NotNull Player player) {
    if (!hooked || !plugin.getConfig().getBoolean("luckperms.sync-on-rank-change", true)) return;
    try {
      LuckPermsProvider.get().getContextManager().signalContextUpdate(player);
    } catch (Throwable ignored) {
    }
  }

  public void shutdown() {
    if (!hooked) return;
    try {
      if (contextCalculator != null) {
        LuckPermsProvider.get().getContextManager().unregisterCalculator(contextCalculator);
      }
    } catch (Throwable ignored) {
    }
    hooked = false;
  }

  public boolean isHooked() {
    return hooked;
  }

  private class RankedSMPContextCalculator implements ContextCalculator<Player> {

    @Override
    public void calculate(@NotNull Player target, @NotNull ContextConsumer consumer) {
      if (!plugin.isPluginEnabled()) {
        return;
      }

      boolean isBlacklisted = plugin.getDatabaseManager().isBlacklisted(target.getUniqueId());
      if (isBlacklisted) {
        consumer.accept("rankedsmp:rank", "blacklisted");
        consumer.accept("rankedsmp:ranked", "false");
        return;
      }

      Integer rank = rankManager.getRank(target.getUniqueId());
      if (rank == null || rank <= 0) {
        consumer.accept("rankedsmp:rank", "unranked");
        consumer.accept("rankedsmp:ranked", "false");
      } else {
        consumer.accept("rankedsmp:rank", String.valueOf(rank));
        consumer.accept("rankedsmp:ranked", "true");
      }
    }

    @Override
    public @NotNull ContextSet estimatePotentialContexts() {
      ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
      builder.add("rankedsmp:ranked", "true");
      builder.add("rankedsmp:ranked", "false");
      builder.add("rankedsmp:rank", "unranked");
      builder.add("rankedsmp:rank", "blacklisted");
      for (int i = 1; i <= plugin.getConfigManager().getTotalRanks(); i++) {
        builder.add("rankedsmp:rank", String.valueOf(i));
      }
      return builder.build();
    }
  }
}
