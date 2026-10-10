package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ForceUnlinkCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public ForceUnlinkCommand(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("forceunlink", "Force unlink a Discord or Minecraft account (Admin)")
        .addOption(OptionType.USER, "discord_user", "Discord user to unlink", false)
        .addOption(OptionType.STRING, "minecraft_username", "Minecraft username to unlink", false);
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("forceunlink")) {
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

    User discordUser = event.getOption("discord_user") != null
        ? event.getOption("discord_user").getAsUser()
        : null;
    String minecraftUsername = event.getOption("minecraft_username") != null
        ? event.getOption("minecraft_username").getAsString().trim()
        : null;

    if (discordUser == null && minecraftUsername == null) {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Invalid Usage");
      embed.setDescription(
          "You must specify either a Discord user or a Minecraft username to unlink.\n\n"
              + "**Usage:**\n"
              + "`/forceunlink discord_user:@user` - Unlink by Discord user\n"
              + "`/forceunlink minecraft_username:player` - Unlink by Minecraft username");
      embed.setColor(new Color(255, 0, 0));
      embed.setTimestamp(Instant.now());
      embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
      event.getHook().sendMessageEmbeds(embed.build()).queue();
      return;
    }

    DiscordLinkManager linkManager = discordManager.getLinkManager();

    if (discordUser != null) {
      String discordId = discordUser.getId();
      UUID linkedUUID = linkManager.getMinecraftUUID(discordId);

      if (linkedUUID == null) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("ℹNot Linked");
        embed.setDescription(
            discordUser.getAsMention() + " is not linked to any Minecraft account.");
        embed.setColor(new Color(255, 165, 0));
        embed.setTimestamp(Instant.now());
        embed.setFooter(
            plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
        return;
      }

      String linkedUsername = linkManager.getUsername(linkedUUID);
      boolean success = linkManager.unlinkDiscord(discordId);

      if (success) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Account Unlinked");
        embed.setDescription(
            "Successfully unlinked:\n"
                + "**Discord:** "
                + discordUser.getAsMention()
                + "\n**Minecraft:** "
                + linkedUsername);
        embed.setColor(new Color(0, 255, 0));
        embed.setFooter("Unlinked by " + event.getUser().getAsTag(), event.getUser().getAvatarUrl());
        embed.setTimestamp(Instant.now());
        event.getHook().sendMessageEmbeds(embed.build()).queue();

        Player player = Bukkit.getPlayer(linkedUUID);
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
                                  "Your Discord link has been removed by an admin.",
                                  net.kyori.adventure.text.format.NamedTextColor.RED))
                          .build());
                }
              });
        }

        discordUser
            .openPrivateChannel()
            .queue(
                channel -> {
                  EmbedBuilder dmEmbed = new EmbedBuilder();
                  dmEmbed.setTitle("Account Unlinked");
                  dmEmbed.setDescription(
                      "Your Discord account has been unlinked from **"
                          + linkedUsername
                          + "** by a server administrator.");
                  dmEmbed.setColor(new Color(255, 165, 0));
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
        embed.setTitle("Unlink Failed");
        embed.setDescription("Failed to unlink account. Please check the server logs.");
        embed.setColor(new Color(255, 0, 0));
        embed.setTimestamp(Instant.now());
        embed.setFooter(
            plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
        event.getHook().sendMessageEmbeds(embed.build()).queue();
      }
      return;
    }

    if (minecraftUsername != null) {
      SchedulerUtil.runAsync(
          plugin,
          () -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(minecraftUsername);

            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
              EmbedBuilder embed = new EmbedBuilder();
              embed.setTitle("Player Not Found");
              embed.setDescription(
                  "Minecraft player **" + minecraftUsername + "** has never joined the server.");
              embed.setColor(new Color(255, 0, 0));
              embed.setTimestamp(Instant.now());
              embed.setFooter(
                  plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
              event.getHook().sendMessageEmbeds(embed.build()).queue();
              return;
            }

            UUID minecraftUUID = offlinePlayer.getUniqueId();
            String linkedDiscordId = linkManager.getDiscordId(minecraftUUID);

            if (linkedDiscordId == null) {
              EmbedBuilder embed = new EmbedBuilder();
              embed.setTitle("ℹNot Linked");
              embed.setDescription(
                  "**" + offlinePlayer.getName() + "** is not linked to any Discord account.");
              embed.setColor(new Color(255, 165, 0));
              embed.setTimestamp(Instant.now());
              embed.setFooter(
                  plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
              event.getHook().sendMessageEmbeds(embed.build()).queue();
              return;
            }

            boolean success = linkManager.unlinkMinecraft(minecraftUUID);

            if (success) {
              EmbedBuilder embed = new EmbedBuilder();
              embed.setTitle("Account Unlinked");
              embed.setDescription(
                  "Successfully unlinked:\n"
                      + "**Minecraft:** "
                      + offlinePlayer.getName()
                      + "\n**Discord ID:** "
                      + linkedDiscordId);
              embed.setColor(new Color(0, 255, 0));
              embed.setFooter(
                  "Unlinked by " + event.getUser().getAsTag(), event.getUser().getAvatarUrl());
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
                                        net.kyori.adventure.text.format.NamedTextColor.AQUA))
                                .append(
                                    net.kyori.adventure.text.Component.text(
                                        "Your Discord link has been removed by an admin.",
                                        net.kyori.adventure.text.format.NamedTextColor.RED))
                                .build());
                      }
                    });
              }

            } else {
              EmbedBuilder embed = new EmbedBuilder();
              embed.setTitle("Unlink Failed");
              embed.setDescription("Failed to unlink account. Please check the server logs.");
              embed.setColor(new Color(255, 0, 0));
              embed.setTimestamp(Instant.now());
              embed.setFooter(
                  plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);
              event.getHook().sendMessageEmbeds(embed.build()).queue();
            }
          });
    }
  }
}
