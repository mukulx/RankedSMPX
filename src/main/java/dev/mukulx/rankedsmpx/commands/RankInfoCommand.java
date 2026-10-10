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

public class RankInfoCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;
  private final ConfigManager configManager;

  public RankInfoCommand(
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
    if (!sender.hasPermission("rankedsmp.info")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return true;
    }

    java.util.UUID targetUUID = null;
    String targetName = null;
    Integer targetRank = null;

    if (args.length == 0) {
      if (!(sender instanceof Player)) {
        sender.sendMessage("Please specify a player or Discord role!");
        return true;
      }
      Player player = (Player) sender;
      targetUUID = player.getUniqueId();
      targetName = player.getName();
      targetRank = rankManager.getRank(targetUUID);
    } else {
      String input = args[0];

      if (input.startsWith("<@&") && input.endsWith(">")) {
        String roleId = input.substring(3, input.length() - 1);

        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
          var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
          if (roleSyncManager != null) {
            targetRank = roleSyncManager.getRankFromRoleId(roleId);

            if (targetRank == null) {
              sender.sendMessage(
                  messageManager.getPrefix() + "§cCould not find rank for role ID: " + roleId);
              return true;
            }

            Map<java.util.UUID, Integer> allRanks = rankManager.getAllRanks();
            for (Map.Entry<java.util.UUID, Integer> entry : allRanks.entrySet()) {
              if (entry.getValue().equals(targetRank)) {
                targetUUID = entry.getKey();
                Player p = Bukkit.getPlayer(targetUUID);
                if (p != null) {
                  targetName = p.getName();
                } else if (plugin.getDiscordManager() != null
                    && plugin.getDiscordManager().getLinkManager() != null) {
                  targetName = plugin.getDiscordManager().getLinkManager().getUsername(targetUUID);
                }
                break;
              }
            }

            if (targetUUID == null) {
              sender.sendMessage(
                  messageManager.getPrefix() + "§cNo player currently has Rank #" + targetRank);
              return true;
            }
          } else {
            sender.sendMessage(
                messageManager.getPrefix()
                    + "§cDiscord integration is not enabled or role sync is unavailable!");
            return true;
          }
        } else {
          sender.sendMessage(messageManager.getPrefix() + "§cDiscord integration is not enabled!");
          return true;
        }
      }
      else if (input.startsWith("<@") && input.endsWith(">")) {
        String discordId = input.substring(2, input.length() - 1);
        if (discordId.startsWith("!")) {
          discordId = discordId.substring(1);
        }

        if (plugin.getDiscordManager() != null
            && plugin.getDiscordManager().isEnabled()
            && plugin.getDiscordManager().getLinkManager() != null) {
          targetUUID = plugin.getDiscordManager().getLinkManager().getMinecraftUUID(discordId);
          if (targetUUID != null) {
            targetName = plugin.getDiscordManager().getLinkManager().getUsername(targetUUID);
            targetRank = rankManager.getRank(targetUUID);
          }
        } else {
          sender.sendMessage(messageManager.getPrefix() + "§cDiscord integration is not enabled!");
          return true;
        }

        if (targetUUID == null) {
          sender.sendMessage(
              messageManager.getPrefix()
                  + "§cCould not find player linked to Discord ID: "
                  + discordId);
          return true;
        }
      }
      else {
        Player target = Bukkit.getPlayer(input);
        if (target == null) {
          Map<String, String> placeholders = Map.of("player", input);
          messageManager.sendMessage(sender, "general.player-not-found", placeholders);
          return true;
        }
        targetUUID = target.getUniqueId();
        targetName = target.getName();
        targetRank = rankManager.getRank(targetUUID);
      }
    }

    String discordId = null;
    if (plugin.getDiscordManager() != null
        && plugin.getDiscordManager().isEnabled()
        && plugin.getDiscordManager().getLinkManager() != null
        && targetUUID != null) {
      discordId = plugin.getDiscordManager().getLinkManager().getDiscordId(targetUUID);
    }

    sender.sendMessage(messageManager.getMessage("rank.info.header"));

    sender.sendMessage(messageManager.applySmallCaps("§b§lPlayer: §f" + targetName));
    if (discordId != null) {
      sender.sendMessage(messageManager.applySmallCaps("§b§lDiscord: §f<@" + discordId + ">"));
    }
    sender.sendMessage(" ");

    if (targetRank == null) {
      sender.sendMessage(messageManager.getMessage("rank.info.unranked"));
    } else {
      int totalRanks = configManager.getTotalRanks();
      double hearts = plugin.getHealthManager().calculateHearts(targetRank);
      double potionMultiplier = plugin.getPotionManager().calculateMultiplier(targetRank);
      double xpMultiplier = plugin.getXPManager().calculateMultiplier(targetRank);
      int extraSlots = configManager.getExtraSlots(targetRank);

      Map<String, String> rankPlaceholders =
          Map.of(
              "rank", String.valueOf(targetRank),
              "total", String.valueOf(totalRanks));
      sender.sendMessage(
          messageManager.applySmallCaps("§b§lRank: §f#" + targetRank + " §7/ " + totalRanks));
      sender.sendMessage(" ");
      sender.sendMessage(messageManager.applySmallCaps("§b§lBuffs & Multipliers:"));

      Map<String, String> heartPlaceholders = Map.of("hearts", String.format("%.1f", hearts));
      sender.sendMessage(messageManager.getMessage("rank.info.hearts", heartPlaceholders));

      Map<String, String> potionPlaceholders =
          Map.of("multiplier", String.format("%.2f", potionMultiplier));
      sender.sendMessage(messageManager.getMessage("rank.info.potion-boost", potionPlaceholders));

      Map<String, String> xpPlaceholders =
          Map.of("multiplier", String.format("%.2f", xpMultiplier));
      sender.sendMessage(messageManager.getMessage("rank.info.xp-boost", xpPlaceholders));

      if (extraSlots > 0) {
        Map<String, String> slotPlaceholders = Map.of("slots", String.valueOf(extraSlots));
        sender.sendMessage(messageManager.getMessage("rank.info.extra-slots", slotPlaceholders));
      }
    }

    sender.sendMessage(messageManager.getMessage("rank.info.footer"));

    return true;
  }

  @Nullable
  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (args.length == 1) {
      List<String> completions = new ArrayList<>();
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
          completions.add(player.getName());
        }
      }
      return completions;
    }
    return null;
  }
}
