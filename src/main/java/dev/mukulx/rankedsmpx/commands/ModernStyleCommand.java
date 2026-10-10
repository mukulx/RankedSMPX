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
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ModernStyleCommand implements CommandExecutor {

  private final RankedSMPXPlugin plugin;
  private final MessageManager messageManager;

  private final Map<UUID, Long> pendingConfirmations = new HashMap<>();
  private static final long CONFIRMATION_TIMEOUT = 30000; // 30 seconds

  public ModernStyleCommand(
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
        applyModernStyle(player);
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

    boolean hasTabPlugin = Bukkit.getPluginManager().getPlugin("TAB") != null;

    player.sendMessage(messageManager.applySmallCaps("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    player.sendMessage(messageManager.applySmallCaps("  §6§lMODERN STYLE WARNING"));
    player.sendMessage("");

    if (!hasTabPlugin) {
      player.sendMessage(messageManager.applySmallCaps("  §c§lTAB PLUGIN NOT DETECTED!"));
      player.sendMessage("");
      player.sendMessage(
          messageManager.applySmallCaps("  §7Modern style requires the TAB plugin for"));
      player.sendMessage(messageManager.applySmallCaps("  §7RGB color and gradient support."));
      player.sendMessage("");
      player.sendMessage(
          messageManager.applySmallCaps("  §7Without TAB, colors may not display correctly."));
      player.sendMessage("");

      Component linkComponent = Component.text("  Install TAB from: ", NamedTextColor.AQUA)
          .append(Component.text("https://modrinth.com/plugin/tab-was-taken", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
              .clickEvent(ClickEvent.openUrl("https://modrinth.com/plugin/tab-was-taken"))
              .hoverEvent(Component.text("Click to open Modrinth page", NamedTextColor.GRAY)));
      player.sendMessage(linkComponent);
      player.sendMessage("");
      player.sendMessage(
          messageManager.applySmallCaps("  §7Click below if you still want to proceed:"));
    } else {
      player.sendMessage(
          messageManager.applySmallCaps(
              "  §7This will overwrite ALL tab prefixes with the modern style:"));
      player.sendMessage(
          messageManager.applySmallCaps("  §8• §7Ranks 1-5: §6§oGold to Orange gradient"));
      player.sendMessage(
          messageManager.applySmallCaps("  §8• §7Ranks 6-10: §7§oSilver to Light Blue gradient"));
      player.sendMessage(
          messageManager.applySmallCaps("  §8• §7Ranks 11-15: §a§oGreen to Lime gradient"));
      player.sendMessage(
          messageManager.applySmallCaps("  §8• §7Ranks 16+: §d§oPurple to Pink gradient"));
      player.sendMessage("");
      player.sendMessage(
          messageManager.applySmallCaps(
              "  §cYour current custom prefixes will be §c§lREPLACED§c!"));
      player.sendMessage("");
      player.sendMessage(
          messageManager.applySmallCaps("  §bYou can customize these gradients in config.yml"));
    }

    player.sendMessage("");

    Component clickMessage =
        Component.text()
            .append(Component.text("  ", NamedTextColor.WHITE))
            .append(
                messageManager
                    .applySmallCapsComponent("[CLICK TO CONFIRM]")
                    .color(NamedTextColor.GREEN)
                    .decorate(TextDecoration.BOLD)
                    .clickEvent(ClickEvent.runCommand("/rsmp modern"))
                    .hoverEvent(
                        messageManager
                            .applySmallCapsComponent("Click to apply modern style prefixes")
                            .color(NamedTextColor.GREEN)))
            .append(
                messageManager
                    .applySmallCapsComponent(" (30s timeout)")
                    .color(NamedTextColor.DARK_GRAY))
            .build();

    player.sendMessage(clickMessage);
    player.sendMessage(messageManager.applySmallCaps("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
  }

  private void applyModernStyle(@NotNull Player player) {
    try {
      int totalRanks = plugin.getConfigManager().getTotalRanks();

      java.util.Map<Integer, String> prefixes = new java.util.HashMap<>();
      prefixes.put(0, "&#C0C0C0[&#A4D7E8Unranked&#C0C0C0] &r");
      for (int rank = 1; rank <= totalRanks; rank++) {
        String[] colors =
            rank <= 5
                ? new String[] {"&#FFD700", "&#FFAA00"}
                : rank <= 10
                    ? new String[] {"&#C0C0C0", "&#A4D7E8"}
                    : rank <= 15
                        ? new String[] {"&#32CD32", "&#5FE47E"}
                        : new String[] {"&#8A2BE2", "&#C21FB7"};
        prefixes.put(rank, colors[0] + "[" + colors[1] + "#" + rank + colors[0] + "] &r");
      }
      plugin.getConfigManager().setRankPrefixes(prefixes);

      if (plugin.getTabIntegration() != null) {
        plugin.getTabIntegration().updateAllPlayers();
      }
      if (plugin.getVanillaTabManager() != null) {
        plugin.getVanillaTabManager().updateAllPlayers();
      }

      boolean hasTabPlugin = Bukkit.getPluginManager().getPlugin("TAB") != null;

      Component successMessage;
      if (hasTabPlugin) {
        successMessage =
            Component.text()
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Successfully applied modern style prefixes!")
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Ranks 1-5: ")
                        .color(NamedTextColor.GRAY))
                .append(
                    messageManager
                        .applySmallCapsComponent("Gold to Orange gradient")
                        .color(NamedTextColor.GOLD))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Ranks 6-10: ")
                        .color(NamedTextColor.GRAY))
                .append(
                    messageManager
                        .applySmallCapsComponent("Silver to Light Blue gradient")
                        .color(NamedTextColor.AQUA))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Ranks 11-15: ")
                        .color(NamedTextColor.GRAY))
                .append(
                    messageManager
                        .applySmallCapsComponent("Green to Lime gradient")
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Ranks 16+: ")
                        .color(NamedTextColor.GRAY))
                .append(
                    messageManager
                        .applySmallCapsComponent("Purple to Pink gradient")
                        .color(NamedTextColor.LIGHT_PURPLE))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Customize these gradients in config.yml")
                        .color(NamedTextColor.AQUA))
                .build();
      } else {
        successMessage =
            Component.text()
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Successfully applied modern style prefixes!")
                        .color(NamedTextColor.GREEN))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Warning: TAB plugin not detected!")
                        .color(NamedTextColor.RED))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Gradients may not display correctly.")
                        .color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(messageManager.getPrefixComponent())
                .append(
                    messageManager
                        .applySmallCapsComponent("Install TAB for full RGB color support.")
                        .color(NamedTextColor.GRAY))
                .build();
      }

      player.sendMessage(successMessage);

    } catch (Exception e) {
      Component errorMessage = Component.text()
          .append(messageManager.getPrefixComponent())
          .append(
              messageManager
                  .applySmallCapsComponent(
                      "Failed to apply modern style: " + e.getMessage())
                  .color(NamedTextColor.RED))
          .build();
      player.sendMessage(errorMessage);
      plugin.getLogger().severe("Failed to apply modern style prefixes: " + e.getMessage());
      e.printStackTrace();
    }
  }

}
