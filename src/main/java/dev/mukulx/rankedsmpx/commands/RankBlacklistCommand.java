package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
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

public class RankBlacklistCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final RankManager rankManager;
  private final MessageManager messageManager;

  public RankBlacklistCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull DatabaseManager databaseManager,
      @NotNull RankManager rankManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.blacklist")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return true;
    }

    if (args.length < 1) {
      sender.sendMessage(
          messageManager.getPrefix() + "§cUsage: /rankblacklist <add|remove|list> [player]");
      return true;
    }

    String action = args[0].toLowerCase();

    switch (action) {
      case "add":
        return handleAdd(sender, args);
      case "remove":
        return handleRemove(sender, args);
      case "list":
        return handleList(sender);
      default:
        sender.sendMessage(
            messageManager.getPrefix() + "§cUsage: /rankblacklist <add|remove|list> [player]");
        return true;
    }
  }

  private boolean handleAdd(@NotNull CommandSender sender, @NotNull String[] args) {
    if (args.length < 2) {
      sender.sendMessage(messageManager.getPrefix() + "§cUsage: /rankblacklist add <player>");
      return true;
    }

    String targetName = args[1];
    OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

    if (!target.hasPlayedBefore() && !target.isOnline()) {
      sender.sendMessage(
          messageManager.getPrefix()
              + "§cPlayer §b"
              + targetName
              + " §chas never played on this server!");
      return true;
    }

    UUID targetUUID = target.getUniqueId();

    if (databaseManager.isBlacklisted(targetUUID)) {
      sender.sendMessage(
          messageManager.getPrefix() + "§b" + target.getName() + " §cis already blacklisted!");
      return true;
    }

    String blacklistedBy = sender instanceof Player ? sender.getName() : "Console";
    databaseManager.addToBlacklist(targetUUID, target.getName(), blacklistedBy);

    Integer currentRank = rankManager.getRank(targetUUID);
    if (currentRank != null) {
      rankManager.removeRank(targetUUID);

      if (target.isOnline()) {
        Player onlineTarget = (Player) target;
        plugin.getHealthManager().resetHealth(onlineTarget);
        onlineTarget.sendMessage(
            messageManager.getPrefix() + "§cYou have been blacklisted from receiving ranks.");
      }

      plugin.getInventoryManager().removeExtraInventory(targetUUID);

      plugin.getHierarchyHammerManager().updateHammerHolder();

      if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
        var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
        if (roleSyncManager != null) {
          roleSyncManager.syncPlayerRole(targetUUID, null);
        }

        var updatesManager = plugin.getDiscordManager().getUpdatesManager();
        if (updatesManager != null) {
          updatesManager.postRankRemoved(
              target.getName(), currentRank, "Blacklisted by " + blacklistedBy);
        }
      }

      Map<String, String> broadcastPlaceholders =
          Map.of(
              "player",
              target.getName(),
              "rank",
              "0",
              "old_rank",
              String.valueOf(currentRank),
              "reason",
              "Blacklisted by " + blacklistedBy);
      String broadcast =
          plugin
              .getMessageManager()
              .getMessage("rank.change.broadcast-lost", broadcastPlaceholders);
      Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

      sender.sendMessage(
          messageManager.getPrefix()
              + "§b"
              + target.getName()
              + " §ahas been blacklisted and their rank (§b#"
              + currentRank
              + "§a) has been removed.");
    } else {
      Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(
          messageManager.getPrefix()
              + "§b"
              + target.getName()
              + " §chas been blacklisted from receiving ranks."));
      sender.sendMessage(
          messageManager.getPrefix()
              + "§b"
              + target.getName()
              + " §ahas been blacklisted from receiving ranks.");
    }

    plugin
        .getLogger()
        .info(sender.getName() + " blacklisted " + target.getName() + " from receiving ranks");
    return true;
  }

  private boolean handleRemove(@NotNull CommandSender sender, @NotNull String[] args) {
    if (args.length < 2) {
      sender.sendMessage(messageManager.getPrefix() + "§cUsage: /rankblacklist remove <player>");
      return true;
    }

    String targetName = args[1];
    OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

    if (!target.hasPlayedBefore() && !target.isOnline()) {
      sender.sendMessage(
          messageManager.getPrefix()
              + "§cPlayer §b"
              + targetName
              + " §chas never played on this server!");
      return true;
    }

    UUID targetUUID = target.getUniqueId();

    if (!databaseManager.isBlacklisted(targetUUID)) {
      sender.sendMessage(
          messageManager.getPrefix() + "§b" + target.getName() + " §cis not blacklisted!");
      return true;
    }

    databaseManager.removeFromBlacklist(targetUUID);

    String unblacklistBy = sender instanceof Player ? sender.getName() : "Console";
    Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(
        messageManager.getPrefix()
            + "§b"
            + target.getName()
            + " §ahas been removed from the rank blacklist by §b"
            + unblacklistBy
            + "§a."));

    sender.sendMessage(
        messageManager.getPrefix()
            + "§b"
            + target.getName()
            + " §ahas been removed from the blacklist.");

    if (target.isOnline()) {
      Player onlineTarget = (Player) target;
      onlineTarget.sendMessage(
          messageManager.getPrefix() + "§aYou have been removed from the rank blacklist.");
    }

    plugin
        .getLogger()
        .info(sender.getName() + " removed " + target.getName() + " from rank blacklist");
    return true;
  }

  private boolean handleList(@NotNull CommandSender sender) {
    List<String> blacklisted = databaseManager.getBlacklistedPlayers();

    if (blacklisted.isEmpty()) {
      sender.sendMessage(messageManager.getPrefix() + "§aNo players are currently blacklisted.");
      return true;
    }

    sender.sendMessage(
        messageManager.getPrefix() + "§b§lBlacklisted Players §7(" + blacklisted.size() + "):");
    for (String playerName : blacklisted) {
      sender.sendMessage("  §7- §b" + playerName);
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
    if (!sender.hasPermission("rankedsmp.blacklist")) {
      return Collections.emptyList();
    }

    if (args.length == 1) {
      return Arrays.asList("add", "remove", "list").stream()
          .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
          .collect(Collectors.toList());
    }

    if (args.length == 2) {
      String action = args[0].toLowerCase();

      if (action.equals("add")) {
        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !databaseManager.isBlacklisted(p.getUniqueId()))
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
            .collect(Collectors.toList());
      } else if (action.equals("remove")) {
        return databaseManager.getBlacklistedPlayers().stream()
            .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
            .collect(Collectors.toList());
      }
    }

    return Collections.emptyList();
  }
}
