package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class RankTopCommand implements CommandExecutor {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;

  public RankTopCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    sender.sendMessage(messageManager.getMessage("rank.top.header"));

    List<Map.Entry<UUID, Integer>> topRanks = rankManager.getTopRanks(10);

    if (topRanks.isEmpty()) {
      sender.sendMessage(messageManager.getPrefix() + "§7No ranked players yet!");
    } else {
      for (Map.Entry<UUID, Integer> entry : topRanks) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
        String playerName = player.getName() != null ? player.getName() : "Unknown";

        Map<String, String> placeholders =
            Map.of("rank", String.valueOf(entry.getValue()), "player", playerName);
        sender.sendMessage(messageManager.getMessage("rank.top.entry", placeholders));
      }
    }

    sender.sendMessage(messageManager.getMessage("rank.top.footer"));

    return true;
  }
}
