package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankHistoryCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final MessageManager messageManager;
  private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");

  public RankHistoryCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull DatabaseManager databaseManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.history")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return true;
    }

    if (args.length < 1) {
      sender.sendMessage(messageManager.getPrefix() + "§cUsage: /rankhistory <player>");
      return true;
    }

    String targetName = args[0];
    UUID targetUUID = null;
    String displayName = targetName;

    if (targetName.startsWith("<@") && targetName.endsWith(">")) {
      String discordId = targetName.substring(2, targetName.length() - 1);
      if (discordId.startsWith("!")) {
        discordId = discordId.substring(1);
      }

      if (plugin.getDiscordManager() != null) {
        targetUUID = plugin.getDiscordManager().getLinkManager().getMinecraftUUID(discordId);
        if (targetUUID != null) {
          displayName = plugin.getDiscordManager().getLinkManager().getUsername(targetUUID);
        }
      }

      if (targetUUID == null) {
        sender.sendMessage(
            messageManager.getPrefix()
                + "§cCould not find player linked to Discord ID: "
                + discordId);
        return true;
      }
    } else {
      OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
      if (!target.hasPlayedBefore() && !target.isOnline()) {
        sender.sendMessage(
            messageManager.getPrefix()
                + "§cPlayer §b"
                + targetName
                + " §chas never played on this server!");
        return true;
      }
      targetUUID = target.getUniqueId();
      displayName = target.getName();
    }

    List<DatabaseManager.RankHistoryEntry> history = databaseManager.getRankHistory(targetUUID, 20);

    if (history.isEmpty()) {
      sender.sendMessage(
          messageManager.getPrefix() + "§b" + displayName + " §ahas no rank history.");
      return true;
    }

    sender.sendMessage(
        messageManager.getPrefix()
            + "§b§lRank History for "
            + displayName
            + " §7("
            + history.size()
            + " entries):");
    sender.sendMessage(" ");

    for (DatabaseManager.RankHistoryEntry entry : history) {
      String actionColor = entry.getAction().equalsIgnoreCase("GAINED") ? "§a" : "§c";
      String actionSymbol = entry.getAction().equalsIgnoreCase("GAINED") ? "+" : "-";
      String date = dateFormat.format(new Date(entry.getTimestamp()));

      sender.sendMessage(
          "  "
              + actionColor
              + actionSymbol
              + " Rank #"
              + entry.getRank()
              + " §7- "
              + entry.getReason());
      sender.sendMessage("    §8" + date);
    }

    return true;
  }

  @Override
  @Nullable
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.history")) {
      return Collections.emptyList();
    }

    if (args.length == 1) {
      return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }

    return Collections.emptyList();
  }
}
