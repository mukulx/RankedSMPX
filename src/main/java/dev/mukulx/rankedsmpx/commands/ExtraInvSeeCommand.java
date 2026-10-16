package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.PlayerResolver;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExtraInvSeeCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;

  public ExtraInvSeeCommand(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {

    if (!(sender instanceof Player admin)) {
      plugin.getMessageManager().sendMessage(sender, "errors.player-only");
      return true;
    }

    if (!admin.hasPermission("rankedsmp.extrainvsee")) {
      plugin.getMessageManager().sendMessage(admin, "no-permission");
      return true;
    }

    if (args.length == 0) {
      plugin.getMessageManager().sendLegacyMessage(admin, "&cUsage: /" + label + " <player>");
      return true;
    }

    String targetName = args[0];

    PlayerResolver.Target target =
        plugin
            .getPlayerResolver()
            .resolve(
                admin,
                targetName,
                () ->
                    plugin
                        .getMessageManager()
                        .sendLegacyMessage(admin, "&cPlayer not found: " + targetName));
    if (target != null) {
      openExtraInventory(
          admin, target.online(), target.uuid(), target.name(), target.online() != null);
    }
    return true;
  }

  private void openExtraInventory(
      @NotNull Player admin,
      @Nullable Player target,
      @NotNull UUID targetUUID,
      @NotNull String targetName,
      boolean isOnline) {

    Inventory extraInv = plugin.getInventoryManager().getExtraInventory(targetUUID);

    if (extraInv == null) {
      Integer rank = plugin.getRankManager().getRank(targetUUID);
      if (rank == null || rank <= 0) {
        plugin.getMessageManager().sendLegacyMessage(admin, "&c" + targetName + " has no rank and no extra inventory.");
        return;
      }

      int slots = plugin.getConfigManager().getExtraSlots(rank);
      if (slots <= 0) {
        plugin.getMessageManager().sendLegacyMessage(
            admin, "&c" + targetName + " (Rank #" + rank + ") has no extra inventory slots.");
        return;
      }

      extraInv = plugin.getInventoryManager().createExtraInventory(targetUUID, rank);
      plugin.getInventoryManager().loadInventory(targetUUID, extraInv);
    }

    if (isOnline && target != null) {
      final Inventory targetExtraInv = extraInv;
      SchedulerUtil.runForEntity(
          plugin,
          target,
          () -> {
            try {
              Inventory openInv = target.getOpenInventory().getTopInventory();
              if (openInv != null && openInv.equals(targetExtraInv)) {
                target.closeInventory();
                plugin.getMessageManager().sendLegacyMessage(target, "&bYour extra inventory was closed by an administrator.");
                plugin
                    .getLogger()
                    .info(
                        "Closed "
                            + targetName
                            + "'s extra inventory (admin "
                            + admin.getName()
                            + " is inspecting)");
              }
            } catch (Exception ignored) {
            }
          });
    }

    String title = "§bExtra Inv: §3" + targetName;
    if (title.length() > 32) {
      title = title.substring(0, 32);
    }

    Inventory viewInv = Bukkit.createInventory(null, extraInv.getSize(), dev.mukulx.rankedsmpx.managers.MessageManager.legacy(title));
    viewInv.setContents(extraInv.getContents());

    plugin.getInventoryManager().trackAdminViewing(admin.getUniqueId(), targetUUID, viewInv);

    admin.openInventory(viewInv);

    plugin
        .getLogger()
        .info(
            admin.getName()
                + " opened extra inventory of "
                + targetName
                + " (online: "
                + isOnline
                + ")");

    plugin.getMessageManager().sendLegacyMessage(admin, "&aOpened extra inventory of &b" + targetName);
    plugin.getMessageManager().sendLegacyMessage(admin, "&7You can add/remove items. Changes will be saved automatically.");
  }

  @Override
  @Nullable
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String alias,
      @NotNull String[] args) {

    if (!sender.hasPermission("rankedsmp.extrainvsee")) {
      return new ArrayList<>();
    }

    if (args.length == 1) {
      String search = args[0].toLowerCase();

      List<String> suggestions = new ArrayList<>();

      for (Player player : Bukkit.getOnlinePlayers()) {
        if (plugin.getInventoryManager().getExtraInventory(player.getUniqueId()) != null) {
          if (player.getName().toLowerCase().startsWith(search)) {
            suggestions.add(player.getName());
          }
        }
      }

      for (UUID uuid : plugin.getInventoryManager().getAllExtraInventoryUUIDs()) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null) {
          if (offlinePlayer.getName().toLowerCase().startsWith(search)) {
            if (!offlinePlayer.isOnline()) {
              suggestions.add(offlinePlayer.getName());
            }
          }
        }
      }

      return suggestions;
    }

    return new ArrayList<>();
  }
}
