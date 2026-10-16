package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.PlayerResolver;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.awt.Color;
import java.time.Instant;
import java.util.UUID;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ForceLinkCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public ForceLinkCommand(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("forcelink", "Force link a Discord user to a Minecraft account (Admin)")
        .addOption(OptionType.USER, "discord_user", "Discord user to link", true)
        .addOption(
            OptionType.STRING, "minecraft_username", "Minecraft username to link to", true);
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("forcelink")) {
      return;
    }

    event.deferReply(true).queue();

    if (event.getMember() == null || !event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("No Permission");
      embed.setDescription("You need Administrator permission to use this command.");
      embed.setColor(new Color(255, 0, 0));
      embed.setTimestamp(Instant.now());
      embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
      event.getHook().sendMessageEmbeds(embed.build()).queue();
      return;
    }

    User discordUser = event.getOption("discord_user").getAsUser();
    String minecraftUsername = event.getOption("minecraft_username").getAsString().trim();

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          PlayerResolver.Result found = plugin.getPlayerResolver().resolve(minecraftUsername);

          if (!found.found()) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Player Not Found");
            embed.setDescription(PlayerResolver.describeMissing(minecraftUsername, found));
            embed.setColor(new Color(255, 0, 0));
            embed.setTimestamp(Instant.now());
            embed.setFooter(
                plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
            return;
          }

          UUID minecraftUUID = found.target().uuid();
          String discordId = discordUser.getId();

          DiscordLinkManager linkManager = discordManager.getLinkManager();
          UUID existingUUID = linkManager.getMinecraftUUID(discordId);
          String existingDiscordId = linkManager.getDiscordId(minecraftUUID);

          if (existingUUID != null && existingUUID.equals(minecraftUUID)) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("ℹAlready Linked");
            embed.setDescription(
                discordUser.getAsMention()
                    + " is already linked to **"
                    + found.target().name()
                    + "**");
            embed.setColor(new Color(255, 165, 0));
            embed.setTimestamp(Instant.now());
            embed.setFooter(
                plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
            return;
          }

          if (existingUUID != null) {
            linkManager.unlinkDiscord(discordId);
          }
          if (existingDiscordId != null) {
            linkManager.unlinkMinecraft(minecraftUUID);
          }

          boolean success = linkManager.forceLink(minecraftUUID, discordId, found.target().name());

          if (success) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Accounts Force Linked");
            embed.setDescription(
                "Successfully linked:\n"
                    + "**Discord:** "
                    + discordUser.getAsMention()
                    + " (`"
                    + discordId
                    + "`)\n"
                    + "**Minecraft:** "
                    + found.target().name()
                    + " (`"
                    + minecraftUUID
                    + "`)");
            embed.setColor(new Color(0, 255, 0));
            embed.setFooter("Linked by " + event.getUser().getAsTag(), event.getUser().getAvatarUrl());
            embed.setTimestamp(Instant.now());
            event.getHook().sendMessageEmbeds(embed.build()).queue();

            Player player = Bukkit.getPlayer(minecraftUUID);
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
                                      "",
                                      net.kyori.adventure.text.format.NamedTextColor.GREEN))
                              .append(
                                  net.kyori.adventure.text.Component.text(
                                      "Your account has been linked to Discord by an admin.",
                                      net.kyori.adventure.text.format.NamedTextColor.AQUA))
                              .build());
                    }
                  });
            }

            discordUser
                .openPrivateChannel()
                .queue(
                    channel -> {
                      EmbedBuilder dmEmbed = new EmbedBuilder();
                      dmEmbed.setTitle("Account Linked");
                      dmEmbed.setDescription(
                          "Your Discord account has been linked to **"
                              + found.target().name()
                              + "** by a server administrator.");
                      dmEmbed.setColor(new Color(0, 255, 0));
                      dmEmbed.setTimestamp(Instant.now());
                      dmEmbed.setFooter(
                          plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"),
                          null);
                      channel.sendMessageEmbeds(dmEmbed.build()).queue(
                          success1 -> {},
                          error -> {
                          });
                    });

          } else {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Link Failed");
            embed.setDescription("Failed to link accounts. Please check the server logs.");
            embed.setColor(new Color(255, 0, 0));
            embed.setTimestamp(Instant.now());
            embed.setFooter(
                plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
            event.getHook().sendMessageEmbeds(embed.build()).queue();
          }
        });
  }
}
