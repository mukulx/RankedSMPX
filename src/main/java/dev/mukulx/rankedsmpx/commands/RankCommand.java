package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;
  private final ConfigManager configManager;

  public RankCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull MessageManager messageManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
    this.configManager = configManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!plugin.isPluginEnabled()) {
      sender.sendMessage(messageManager.getPrefix() + "§cPlugin is currently disabled!");
      sender.sendMessage(
          messageManager.getPrefix() + "§7An admin can re-enable it using: §b/rsmp enable");
      return true;
    }

    if (args.length == 0) {
      if (!(sender instanceof Player player)) {
        messageManager.sendPrefixedMessage(sender, "&cOnly players can use this command!");
        return true;
      }
      showRank(sender, player);
    } else {
      Player target = Bukkit.getPlayer(args[0]);
      if (target == null) {
        Map<String, String> placeholders = Map.of("player", args[0]);
        messageManager.sendMessage(sender, "general.player-not-found", placeholders);
        return true;
      }
      showRank(sender, target);
    }

    return true;
  }

  private void showRank(@NotNull CommandSender sender, @NotNull Player target) {
    Integer rank = rankManager.getRank(target.getUniqueId());

    if (rank == null) {
      messageManager.sendPrefixedMessage(sender, "&c" + target.getName() + " is not ranked yet!");
      return;
    }

    double hearts = plugin.getHealthManager().calculateHearts(rank);
    double potionMultiplier = plugin.getPotionManager().calculateMultiplier(rank);
    double xpMultiplier = plugin.getXPManager().calculateMultiplier(rank);
    int extraSlots = getExtraSlots(rank);

    sender.sendMessage(messageManager.applySmallCapsComponent("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    sender.sendMessage(messageManager.applySmallCapsComponent("&6&lRank Information"));
    sender.sendMessage("");
    sender.sendMessage(messageManager.applySmallCapsComponent("&b  Player: &f" + target.getName()));
    sender.sendMessage(
        messageManager.applySmallCapsComponent(
        "&b  Rank: " + getRankColor(rank) + "&l#" + rank + " &7/ " + configManager.getTotalRanks()));
    sender.sendMessage("");
    sender.sendMessage(messageManager.applySmallCapsComponent("&b&lBuffs & Perks:"));
    sender.sendMessage(messageManager.applySmallCapsComponent("  &cHearts: &f" + String.format("%.1f", hearts)));
    sender.sendMessage(
        messageManager.applySmallCapsComponent("  &dPotion Duration: &f" + String.format("%.2fx", potionMultiplier)));
    sender.sendMessage(
        messageManager.applySmallCapsComponent("  &aXP Multiplier: &f" + String.format("%.2fx", xpMultiplier)));

    if (extraSlots > 0) {
      sender.sendMessage(
          messageManager.applySmallCapsComponent("  &bExtra Inventory: &f" + extraSlots + " slots"));
    }

    sender.sendMessage("");
    sender.sendMessage(messageManager.applySmallCapsComponent("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
  }

  private String getRankColor(int rank) {
    if (rank == 1) return "§6"; // Gold
    if (rank == 2) return "§f"; // White (Silver)
    if (rank == 3) return "§6"; // Gold (Bronze - using gold as closest to bronze)
    if (rank <= 10) return "§b"; // Cyan
    return "§7"; // Gray
  }

  private int getExtraSlots(int rank) {
    return configManager.getExtraSlots(rank);
  }

  @Nullable
  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
          completions.add(player.getName());
        }
      }
    }

    return completions;
  }
}
