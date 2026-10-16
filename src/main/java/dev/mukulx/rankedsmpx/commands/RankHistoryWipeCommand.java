package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.utils.PlayerResolver;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankHistoryWipeCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final MessageManager messageManager;

  public RankHistoryWipeCommand(
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
    if (!sender.hasPermission("rankedsmp.historywipe")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return true;
    }

    if (args.length < 1) {
      sender.sendMessage(messageManager.getPrefix() + "§cUsage: /rankhistorywipe <player|all>");
      return true;
    }

    String targetName = args[0];

    if (targetName.equalsIgnoreCase("all")) {
      databaseManager.clearAllRankHistory();
      sender.sendMessage(messageManager.getPrefix() + "§aCleared rank history for all players!");
      plugin.getLogger().info(sender.getName() + " cleared all rank history");
      return true;
    }

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
      PlayerResolver.Target target =
          plugin
              .getPlayerResolver()
              .resolve(
                  sender,
                  targetName,
                  () ->
                      sender.sendMessage(
                          messageManager.getPrefix()
                              + "§cPlayer §b"
                              + targetName
                              + " §chas never played on this server!"));
      if (target == null) {
        return true;
      }
      targetUUID = target.uuid();
      displayName = target.name();
    }

    databaseManager.clearRankHistory(targetUUID);
    sender.sendMessage(
        messageManager.getPrefix() + "§aCleared rank history for §b" + displayName + "§a!");
    plugin.getLogger().info(sender.getName() + " cleared rank history for " + displayName);

    return true;
  }

  @Override
  @Nullable
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.historywipe")) {
      return Collections.emptyList();
    }

    if (args.length == 1) {
      List<String> suggestions = new ArrayList<>();
      suggestions.add("all");
      suggestions.addAll(
          Bukkit.getOnlinePlayers().stream()
              .map(Player::getName)
              .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
              .collect(Collectors.toList()));
      return suggestions;
    }

    return Collections.emptyList();
  }
}
