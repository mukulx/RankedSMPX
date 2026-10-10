package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankFixCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final RankManager rankManager;
  private final MessageManager messageManager;

  public RankFixCommand(
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

    if (!sender.hasPermission("rankedsmp.admin")) {
      if (sender instanceof Player player) {
        fixSinglePlayer(player, sender, true);
        return true;
      }
      messageManager.sendMessage(sender, "general.no-permission");
      return true;
    }

    if (args.length == 1 && !args[0].equalsIgnoreCase("all") && !args[0].equalsIgnoreCase("sync")) {
      Player target = Bukkit.getPlayer(args[0]);
      if (target != null) {
        fixSinglePlayer(target, sender, false);
        return true;
      }
    }

    sender.sendMessage(messageManager.applySmallCaps("§7Checking and repairing server ranks..."));

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          int invalidRanksFixed = 0;
          int namesFixed = 0;
          int totalRanks = plugin.getConfigManager().getTotalRanks();

          try {
            try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt =
                    conn.prepareStatement("DELETE FROM ranks WHERE rank <= 0 OR rank IS NULL")) {
              if (conn != null) {
                invalidRanksFixed += stmt.executeUpdate();
              }
            }

            try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt =
                    conn.prepareStatement("SELECT COUNT(*) FROM ranks WHERE rank > ?")) {
              if (conn != null) {
                stmt.setInt(1, totalRanks);
                ResultSet above = stmt.executeQuery();
                if (above.next() && above.getInt(1) > 0) {
                  plugin
                      .getLogger()
                      .warning(
                          above.getInt(1)
                              + " player(s) hold a rank above "
                              + totalRanks
                              + ". They were kept; add rows to ranks.yml to cover them.");
                }
              }
            }

            try (Connection conn = databaseManager.getConnection();
                PreparedStatement stmt =
                    conn.prepareStatement(
                        "SELECT uuid FROM ranks WHERE player_name IS NULL OR player_name = ''")) {
              if (conn != null) {
                ResultSet rs = stmt.executeQuery();
                List<String> toUpdate = new ArrayList<>();
                while (rs.next()) {
                  toUpdate.add(rs.getString("uuid"));
                }
                rs.close();

                for (String uuidStr : toUpdate) {
                  try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String resolvedName = Bukkit.getOfflinePlayer(uuid).getName();
                    if (resolvedName != null && !resolvedName.isBlank()) {
                      try (PreparedStatement updateStmt =
                          conn.prepareStatement("UPDATE ranks SET player_name = ? WHERE uuid = ?")) {
                        updateStmt.setString(1, resolvedName);
                        updateStmt.setString(2, uuidStr);
                        namesFixed += updateStmt.executeUpdate();
                      }
                    }
                  } catch (Exception ignored) {
                  }
                }
              }
            }

            rankManager.loadRanks();

            final int invalidFixed = invalidRanksFixed;
            final int fixedNames = namesFixed;

            SchedulerUtil.run(
                plugin,
                () -> {
                  int playersFixed = 0;
                  for (Player player : Bukkit.getOnlinePlayers()) {
                    SchedulerUtil.runForEntity(plugin, player, () -> fixPlayerInternal(player));
                    playersFixed++;
                  }

                  sender.sendMessage(
                      messageManager.applySmallCaps("§aAll systems checked and updated:"));
                  sender.sendMessage(
                      messageManager.applySmallCaps(
                          "  §8• §7Online Players: §aRestored hearts & perks for §b"
                              + playersFixed
                              + " §aplayer(s)"));

                  if (invalidFixed > 0 || fixedNames > 0) {
                    sender.sendMessage(
                        messageManager.applySmallCaps(
                            "  §8• §7Database Cleanup: §b"
                                + invalidFixed
                                + " §7invalid rank(s), §b"
                                + fixedNames
                                + " §7name(s) fixed"));
                  } else {
                    sender.sendMessage(
                        messageManager.applySmallCaps("  §8• §7Database: §aAll ranks look good (0 issues)"));
                  }
                });

          } catch (Exception e) {
            plugin.getLogger().warning("RankFix encountered an error: " + e.getMessage());
            SchedulerUtil.run(
                plugin,
                () ->
                    sender.sendMessage(
                        messageManager.applySmallCaps("§cFailed to complete: " + e.getMessage())));
          }
        });

    return true;
  }

  private void fixSinglePlayer(@NotNull Player target, @NotNull CommandSender sender, boolean isSelf) {
    SchedulerUtil.runForEntity(
        plugin,
        target,
        () -> {
          double hearts = fixPlayerInternal(target);
          Integer rank = rankManager.getRank(target.getUniqueId());

          String rankText = rank != null ? "§6#" + rank : "§7Unranked";
          String heartsText = String.format("%.1f", hearts);

          if (isSelf) {
            target.sendMessage(
                messageManager.applySmallCaps(
                    "§7Your hearts & perks have been reset to normal §8("
                        + rankText
                        + " §7• §c"
                        + heartsText
                        + "§8)"));
          } else {
            sender.sendMessage(
                messageManager.applySmallCaps(
                    "§7Restored hearts & perks for §b"
                        + target.getName()
                        + " §8("
                        + rankText
                        + " §7• §c"
                        + heartsText
                        + "§8)"));
            target.sendMessage(
                messageManager.applySmallCaps(
                    "§7Your hearts & perks have been refreshed by an admin §8("
                        + rankText
                        + " §7• §c"
                        + heartsText
                        + "§8)"));
          }
        });
  }

  private double fixPlayerInternal(@NotNull Player player) {
    double hearts = plugin.getHealthManager().fixPlayerHealth(player);

    if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
      plugin.getTabIntegration().updatePlayer(player);
    }
    if (plugin.getVanillaTabManager() != null) {
      plugin.getVanillaTabManager().updatePlayer(player);
    }

    if (plugin.getHierarchyHammerManager().shouldHaveHammer(player.getUniqueId())) {
      plugin.getHierarchyHammerManager().giveHammer(player);
    }

    return hearts;
  }

  @Override
  public @Nullable List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {
    if (args.length == 1 && sender.hasPermission("rankedsmp.admin")) {
      List<String> completions = new ArrayList<>();
      completions.add("all");
      for (Player player : Bukkit.getOnlinePlayers()) {
        if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
          completions.add(player.getName());
        }
      }
      return completions;
    }
    return Collections.emptyList();
  }
}
