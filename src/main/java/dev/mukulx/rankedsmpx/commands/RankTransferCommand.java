package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankTransferCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;

  public RankTransferCommand(
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
    if (!(sender instanceof Player)) {
      messageManager.sendMessage(sender, "errors.player-only");
      return true;
    }

    Player player = (Player) sender;

    if (!plugin.getConfig().getBoolean("rank-transfer.enabled", false)) {
      messageManager.sendMessage(player, "errors.feature-disabled");
      return true;
    }

    if (args.length < 1) {
      messageManager.sendMessage(player, "commands.rank.transfer.usage");
      return true;
    }

    boolean allowOffline = plugin.getConfig().getBoolean("rank-transfer.allow-offline", false);

    Player targetPlayer = Bukkit.getPlayer(args[0]);
    UUID targetUUID = null;
    String targetName = args[0];
    boolean isOnline = targetPlayer != null;

    if (targetPlayer != null) {
      targetUUID = targetPlayer.getUniqueId();
      targetName = targetPlayer.getName();
    } else {
      if (!allowOffline) {
        Map<String, String> placeholders = Map.of("player", args[0]);
        messageManager.sendMessage(player, "errors.player-not-found", placeholders);
        return true;
      }

      org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
      if (offlinePlayer.hasPlayedBefore() || offlinePlayer.getName() != null) {
        targetUUID = offlinePlayer.getUniqueId();
        targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : args[0];
      } else {
        Map<String, String> placeholders = Map.of("player", args[0]);
        messageManager.sendMessage(player, "errors.player-not-found", placeholders);
        return true;
      }
    }

    if (targetUUID.equals(player.getUniqueId())) {
      messageManager.sendMessage(player, "commands.rank.transfer.self");
      return true;
    }

    Integer playerRank = rankManager.getRank(player.getUniqueId());
    if (playerRank == null) {
      messageManager.sendMessage(player, "commands.rank.transfer.no-rank");
      return true;
    }

    Integer targetRank = rankManager.getRank(targetUUID);
    if (targetRank != null) {
      Map<String, String> placeholders = Map.of("player", targetName);
      messageManager.sendMessage(player, "commands.rank.transfer.target-has-rank", placeholders);
      return true;
    }

    if (plugin.getDatabaseManager().isBlacklisted(targetUUID)) {
      player.sendMessage(
          messageManager.getPrefix()
              + "§cCannot transfer rank to §b"
              + targetName
              + "§c! They are blacklisted from receiving ranks.");
      return true;
    }

    // Block transfers mid-fight so a player can't dodge losing their rank by handing it to an alt
    if (plugin.getInventoryManager().isCombatTagged(player.getUniqueId())) {
      int remaining = plugin.getInventoryManager().getRemainingCombatTime(player.getUniqueId());
      plugin
          .getMessageManager()
          .sendLegacyMessage(
              player, "&c&lIN COMBAT! &7You cannot transfer your rank for &b" + remaining + "s&7.");
      return true;
    }

    rankManager.setRankWithHistory(
        targetUUID, playerRank, "Received transfer from " + player.getName());
    rankManager.removeRankWithHistory(
        player.getUniqueId(), playerRank, "Transferred to " + targetName);

    plugin.getHealthManager().resetHealth(player);

    plugin.getInventoryManager().removeExtraInventory(player.getUniqueId());
    plugin.getInventoryManager().removeExtraInventory(targetUUID);

    if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
      plugin.getTabIntegration().updatePlayer(player);
    }
    if (plugin.getVanillaTabManager() != null) {
      plugin.getVanillaTabManager().updatePlayer(player);
    }

    if (isOnline && targetPlayer != null) {
      SchedulerUtil.runForEntity(
          plugin,
          targetPlayer,
          () -> {
            if (!targetPlayer.isOnline()) return;
            plugin.getHealthManager().applyHealth(targetPlayer);
            if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
              plugin.getTabIntegration().updatePlayer(targetPlayer);
            }
            if (plugin.getVanillaTabManager() != null) {
              plugin.getVanillaTabManager().updatePlayer(targetPlayer);
            }

            Map<String, String> targetPlaceholders =
                Map.of("player", player.getName(), "rank", String.valueOf(playerRank));
            messageManager.sendMessage(
                targetPlayer, "commands.rank.transfer.received", targetPlaceholders);

            Map<String, String> receiverTitlePlaceholders = Map.of("rank", String.valueOf(playerRank));
            String receiverTitle =
                messageManager.getMessage("rank.change.title-gained", receiverTitlePlaceholders);
            String receiverSubtitle = messageManager.getMessage("rank.change.subtitle-gained");
            messageManager.sendTitle(targetPlayer, receiverTitle, receiverSubtitle, 10, 70, 20);
          });
    }

    plugin.getHierarchyHammerManager().updateHammerHolder();

    Map<String, String> senderPlaceholders =
        Map.of("player", targetName, "rank", String.valueOf(playerRank));
    messageManager.sendMessage(player, "commands.rank.transfer.success", senderPlaceholders);

    Map<String, String> broadcastPlaceholders =
        Map.of(
            "from", player.getName(),
            "to", targetName,
            "rank", String.valueOf(playerRank));
    String broadcast =
        messageManager.getMessage("commands.rank.transfer.broadcast", broadcastPlaceholders);
    Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

    Map<String, String> receiverBroadcastPlaceholders =
        Map.of(
            "player",
            targetName,
            "rank",
            String.valueOf(playerRank),
            "reason",
            "Transfer from " + player.getName());
    String receiverBroadcast =
        messageManager.getMessage("rank.change.broadcast-gained", receiverBroadcastPlaceholders);
    Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(receiverBroadcast));

    Map<String, String> senderBroadcastPlaceholders =
        Map.of(
            "player",
            player.getName(),
            "rank",
            "0",
            "old_rank",
            String.valueOf(playerRank),
            "reason",
            "Transferred to " + targetName);
    String senderBroadcast =
        messageManager.getMessage("rank.change.broadcast-lost", senderBroadcastPlaceholders);
    Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(senderBroadcast));

    if (plugin.getDiscordConfig().getBoolean("notifications.notify-on-admin-change", true)) {
      if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
        var notificationManager = plugin.getDiscordManager().getNotificationManager();
        if (notificationManager != null) {
          notificationManager.sendRankGainedDM(
              targetUUID, playerRank, "Received rank transfer from " + player.getName());

          notificationManager.sendUnrankedDM(
              player.getUniqueId(), playerRank, "Transferred rank to " + targetName);
        }

        var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
        if (roleSyncManager != null) {
          roleSyncManager.syncPlayerRole(player.getUniqueId(), null);
          roleSyncManager.syncPlayerRole(targetUUID, playerRank);
        }

        var updatesManager = plugin.getDiscordManager().getUpdatesManager();
        if (updatesManager != null) {
          updatesManager.postRankTransferred(player.getName(), targetName, playerRank);
        }

        plugin.getDiscordManager().forceUpdate();
      }
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
    if (args.length == 1) {
      List<String> players = new ArrayList<>();
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
          players.add(player.getName());
        }
      }
      return players;
    }
    return Collections.emptyList();
  }
}
