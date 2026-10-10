package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.awt.Color;
import java.time.Instant;
import java.util.UUID;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnlinkDiscordCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public UnlinkDiscordCommand(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("unlink", "Unlink your Minecraft account from Discord");
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("unlink")) {
      return;
    }

    event.deferReply(true).queue();

    if (!plugin.getDiscordConfig().getBoolean("linking.allow-unlink", true)) {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Unlinking Disabled");
      embed.setDescription("Unlinking is not allowed on this server.");
      embed.setColor(new Color(255, 0, 0));
      embed.setTimestamp(Instant.now());

      event.getHook().sendMessageEmbeds(embed.build()).queue();
      return;
    }

    String discordId = event.getUser().getId();
    DiscordLinkManager linkManager = discordManager.getLinkManager();

    UUID uuid = linkManager.getMinecraftUUID(discordId);
    if (uuid == null) {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Not Linked");
      embed.setDescription("Your Discord account is not linked to any Minecraft account.");
      embed.setColor(new Color(255, 0, 0));
      embed.setTimestamp(Instant.now());

      event.getHook().sendMessageEmbeds(embed.build()).queue();
      return;
    }

    String username = linkManager.getUsername(uuid);

    boolean success = linkManager.unlinkAccount(uuid);

    if (success) {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Account Unlinked");
      embed.setDescription("Your Discord account has been unlinked from **" + username + "**");
      embed.setColor(new Color(0, 255, 0));
      embed.setTimestamp(Instant.now());
      embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);

      event.getHook().sendMessageEmbeds(embed.build()).queue();

      Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline()) {
        SchedulerUtil.runForEntity(
            plugin,
            player,
            () -> {
              if (player.isOnline()) {
                player.sendMessage(
                    net.kyori.adventure.text.Component.text()
                        .append(
                            net.kyori.adventure.text.Component.text(
                                "", net.kyori.adventure.text.format.NamedTextColor.AQUA))
                        .append(
                            net.kyori.adventure.text.Component.text(
                                "Your Discord account has been unlinked.",
                                net.kyori.adventure.text.format.NamedTextColor.GRAY))
                        .build());
              }
            });
      }

    } else {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Unlinking Failed");
      embed.setDescription("Failed to unlink your account. Please try again later.");
      embed.setColor(new Color(255, 0, 0));
      embed.setTimestamp(Instant.now());

      event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
  }
}
