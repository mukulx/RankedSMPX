package dev.mukulx.rankedsmpx.placeholders;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankedSMPPlaceholders extends PlaceholderExpansion {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final ConfigManager configManager;

  public RankedSMPPlaceholders(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.configManager = configManager;
  }

  @Override
  @NotNull
  public String getIdentifier() {
    return "rankedsmp";
  }

  @Override
  @NotNull
  public String getAuthor() {
    return plugin.getPluginMeta().getAuthors().toString();
  }

  @Override
  @NotNull
  public String getVersion() {
    return plugin.getPluginMeta().getVersion();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  @Nullable
  public String onPlaceholderRequest(Player player, @NotNull String identifier) {
    if (player == null) {
      return "";
    }

    Integer rank = rankManager.getRank(player.getUniqueId());
    boolean isBlacklisted = plugin.getDatabaseManager().isBlacklisted(player.getUniqueId());

    switch (identifier.toLowerCase()) {
      case "rank":
        if (isBlacklisted) return "Blacklisted";
        return rank != null ? String.valueOf(rank) : "Unranked";

      case "rank_number":
        return rank != null ? String.valueOf(rank) : "0";

      case "hearts":
        if (rank == null) return "20.0";
        double hearts = plugin.getHealthManager().calculateHearts(rank);
        return String.format("%.1f", hearts);

      case "potion_multiplier":
        if (rank == null) return "1.0";
        double potionMult = plugin.getPotionManager().calculateMultiplier(rank);
        return String.format("%.2f", potionMult);

      case "xp_multiplier":
        if (rank == null) return "1.0";
        double xpMult = plugin.getXPManager().calculateMultiplier(rank);
        return String.format("%.2f", xpMult);

      case "extra_slots":
        if (rank == null) return "0";
        return String.valueOf(configManager.getExtraSlots(rank));

      case "rank_prefix":
      case "prefix":
        return plugin.getMessageManager().getRankPrefixLegacy(rank, isBlacklisted).replace('&', '§');

      case "rank_prefix_minimessage":
      case "prefix_minimessage":
        return plugin.getMessageManager().getRankPrefixMiniMessage(rank, isBlacklisted);

      case "rank_suffix":
      case "suffix":
        return plugin.getMessageManager().getRankSuffixLegacy(rank, isBlacklisted).replace('&', '§');

      case "is_ranked":
        return (rank != null && rank > 0 && !isBlacklisted) ? "true" : "false";

      case "is_blacklisted":
        return isBlacklisted ? "true" : "false";

      case "has_hammer":
        boolean hasHammer = plugin.getHierarchyHammerManager() != null
            && plugin.getHierarchyHammerManager().isHierarchyHammer(player.getInventory().getItemInMainHand());
        return hasHammer ? "true" : "false";

      default:
        if (identifier.toLowerCase().startsWith("holder_")) {
          try {
            int targetRank = Integer.parseInt(identifier.substring("holder_".length()));
            UUID holderUUID = rankManager.getPlayerWithRank(targetRank);
            if (holderUUID == null) {
              return "None";
            }
            Player holder = Bukkit.getPlayer(holderUUID);
            if (holder != null) {
              return holder.getName();
            }
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(holderUUID);
            return offline.getName() != null ? offline.getName() : "None";
          } catch (Exception ignored) {
          }
        }
        return null;
    }
  }
}
