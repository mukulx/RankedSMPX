package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.discord.DiscordLinkManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnlinkDiscordCommand implements CommandExecutor {

  private final RankedSMPXPlugin plugin;
  private final MessageManager messageManager;

  public UnlinkDiscordCommand(
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
      sender.sendMessage("This command can only be used by players!");
      return true;
    }

    if (!plugin.getDiscordConfig().getBoolean("enabled", false)) {
      player.sendMessage(messageManager.getMessage("discord.not-enabled"));
      return true;
    }

    if (!plugin.getDiscordConfig().getBoolean("linking.allow-unlink", true)) {
      player.sendMessage(
          Component.text("Unlinking is not allowed on this server!", NamedTextColor.RED));
      return true;
    }

    DiscordLinkManager linkManager = plugin.getDiscordManager().getLinkManager();

    if (!linkManager.isLinked(player.getUniqueId())) {
      player.sendMessage(
          Component.text("Your account is not linked to Discord!", NamedTextColor.RED));
      return true;
    }

    boolean success = linkManager.unlinkAccount(player.getUniqueId());

    if (success) {
      player.sendMessage(
          Component.text("Successfully unlinked your Discord account!", NamedTextColor.GREEN));
    } else {
      player.sendMessage(
          Component.text(
              "Failed to unlink your account. Please try again later.", NamedTextColor.RED));
    }

    return true;
  }
}
