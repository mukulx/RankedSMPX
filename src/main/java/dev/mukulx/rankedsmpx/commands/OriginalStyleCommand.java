package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class OriginalStyleCommand implements CommandExecutor {

  private final RankedSMPXPlugin plugin;
  private final MessageManager messageManager;

  private final Map<UUID, Long> pendingConfirmations = new HashMap<>();
  private static final long CONFIRMATION_TIMEOUT = 30000; // 30 seconds

  public OriginalStyleCommand(
      @NotNull RankedSMPXPlugin plugin, @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {

    if (!(sender instanceof Player player)) {
      plugin.getMessageManager().sendMessage(sender, "errors.player-only");
      return true;
    }

    if (!player.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(player, "no-permission");
      return true;
    }

    UUID playerUUID = player.getUniqueId();

    if (pendingConfirmations.containsKey(playerUUID)) {
      long requestTime = pendingConfirmations.get(playerUUID);
      long currentTime = System.currentTimeMillis();
      long timeDiff = currentTime - requestTime;

      if (timeDiff <= CONFIRMATION_TIMEOUT) {
        pendingConfirmations.remove(playerUUID);
        applyOriginalStyle(player);
        return true;
      } else {
        pendingConfirmations.remove(playerUUID);
      }
    }

    showConfirmation(player);
    return true;
  }

  private void showConfirmation(@NotNull Player player) {
    UUID playerUUID = player.getUniqueId();
    pendingConfirmations.put(playerUUID, System.currentTimeMillis());

    player.sendMessage(messageManager.applySmallCaps("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    player.sendMessage(messageManager.applySmallCaps("  §b§6§lORIGINAL STYLE WARNING"));
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps(
            "  §7This will overwrite ALL tab prefixes with the original style:"));
    player.sendMessage(
        messageManager.applySmallCaps("  §8• §7Ranked players: §f&e[#X] §b(Yellow brackets)"));
    player.sendMessage(
        messageManager.applySmallCaps(
            "  §8• §7Unranked: §f&e[&8UNRANKED&e] §7(Yellow brackets, dark gray text)"));
    player.sendMessage("");
    player.sendMessage(
        messageManager.applySmallCaps("  §cYour current custom prefixes will be §c§lREPLACED§c!"));
    player.sendMessage("");

    Component clickMessage =
        Component.text()
            .append(Component.text("  ", NamedTextColor.WHITE))
            .append(
                messageManager
                    .applySmallCapsComponent("[CLICK TO CONFIRM]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/rsmp original"))
                    .hoverEvent(
                        messageManager
                            .applySmallCapsComponent("Click to apply original style prefixes")
                            .color(NamedTextColor.GREEN)))
            .append(
                messageManager
                    .applySmallCapsComponent(" (30s timeout)")
                    .color(NamedTextColor.DARK_GRAY))
            .build();

    player.sendMessage(clickMessage);
    player.sendMessage(messageManager.applySmallCaps("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
  }

  private void applyOriginalStyle(@NotNull Player player) {
    try {
      int totalRanks = plugin.getConfigManager().getTotalRanks();

      java.util.Map<Integer, String> prefixes = new java.util.HashMap<>();
      prefixes.put(0, "&e[&7UNRANKED&e] &r");
      for (int rank = 1; rank <= totalRanks; rank++) {
        prefixes.put(rank, "&e[#" + rank + "] &r");
      }
      plugin.getConfigManager().setRankPrefixes(prefixes);

      if (plugin.getTabIntegration() != null) {
        plugin.getTabIntegration().updateAllPlayers();
      }
      if (plugin.getVanillaTabManager() != null) {
        plugin.getVanillaTabManager().updateAllPlayers();
      }

      Component successMessage =
          Component.text()
              .append(messageManager.getPrefixComponent())
              .append(
                  messageManager
                      .applySmallCapsComponent("Successfully applied original style prefixes!")
                      .color(NamedTextColor.GREEN))
              .append(Component.newline())
              .append(messageManager.getPrefixComponent())
              .append(
                  messageManager.applySmallCapsComponent("All ranks: ").color(NamedTextColor.GRAY))
              .append(Component.text("&e[#X] ", NamedTextColor.YELLOW))
              .append(
                  messageManager
                      .applySmallCapsComponent("(Yellow)")
                      .color(NamedTextColor.DARK_GRAY))
              .append(Component.newline())
              .append(messageManager.getPrefixComponent())
              .append(
                  messageManager.applySmallCapsComponent("Unranked: ").color(NamedTextColor.GRAY))
              .append(Component.text("&e[&8UNRANKED&e] ", NamedTextColor.YELLOW))
              .append(
                  messageManager
                      .applySmallCapsComponent("(Yellow brackets, dark gray text)")
                      .color(NamedTextColor.DARK_GRAY))
              .build();

      player.sendMessage(successMessage);

    } catch (Exception e) {
      Component errorMessage = Component.text()
          .append(messageManager.getPrefixComponent())
          .append(
              messageManager
                  .applySmallCapsComponent(
                      "Failed to apply original style: " + e.getMessage())
                  .color(NamedTextColor.RED))
          .build();
      player.sendMessage(errorMessage);
      plugin.getLogger().severe("Failed to apply original style prefixes: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
