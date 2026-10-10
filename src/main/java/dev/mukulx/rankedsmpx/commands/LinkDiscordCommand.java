package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.discord.DiscordLinkManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LinkDiscordCommand implements CommandExecutor {

  private final RankedSMPXPlugin plugin;
  private final MessageManager messageManager;

  public LinkDiscordCommand(
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

    DiscordLinkManager linkManager = plugin.getDiscordManager().getLinkManager();

    if (linkManager.isLinked(player.getUniqueId())) {
      player.sendMessage(
          Component.text()
              .append(Component.text("", NamedTextColor.GREEN))
              .append(
                  Component.text(
                      "Your account is already linked to Discord!", NamedTextColor.AQUA))
              .build());

      player.sendMessage(
          Component.text()
              .append(Component.text("Use ", NamedTextColor.GRAY))
              .append(Component.text("/unlinkdiscord", NamedTextColor.AQUA))
              .append(Component.text(" to unlink.", NamedTextColor.GRAY))
              .build());

      return true;
    }

    String code = linkManager.generateCode(player);

    if (code.equals("ALREADY_LINKED")) {
      player.sendMessage(Component.text("You are already linked!", NamedTextColor.RED));
      return true;
    }

    String botMention = "the bot";

    player.sendMessage(
        Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
    player.sendMessage(Component.text());
    player.sendMessage(
        Component.text("  Discord Account Linking", NamedTextColor.GOLD, TextDecoration.BOLD));
    player.sendMessage(Component.text());
    player.sendMessage(Component.text("  Your linking code:", NamedTextColor.GRAY));
    player.sendMessage(Component.text());

    Component codeComponent =
        Component.text("    " + code, NamedTextColor.GREEN, TextDecoration.BOLD)
            .hoverEvent(HoverEvent.showText(Component.text("Click to copy", NamedTextColor.AQUA)))
            .clickEvent(ClickEvent.copyToClipboard(code));
    player.sendMessage(codeComponent);

    player.sendMessage(Component.text());
    player.sendMessage(Component.text("  Instructions:", NamedTextColor.AQUA));
    player.sendMessage(Component.text("  1. Open Discord", NamedTextColor.GRAY));
    player.sendMessage(Component.text("  2. Send a DM to " + botMention, NamedTextColor.GRAY));
    player.sendMessage(
        Component.text("  3. Use the command: ", NamedTextColor.GRAY)
            .append(Component.text("/link " + code, NamedTextColor.AQUA)));
    player.sendMessage(Component.text());
    player.sendMessage(
        Component.text(
            "  Code expires in "
                + plugin.getDiscordConfig().getInt("linking.code-expiration", 10)
                + " minutes",
            NamedTextColor.AQUA));
    player.sendMessage(Component.text());
    player.sendMessage(
        Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));

    return true;
  }
}
