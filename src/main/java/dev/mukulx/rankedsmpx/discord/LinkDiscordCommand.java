package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.awt.Color;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LinkDiscordCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public LinkDiscordCommand(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("link", "Link your Minecraft account to Discord")
        .addOption(OptionType.STRING, "code", "The 6-digit code from /linkdiscord in-game", true);
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("link")) {
      return;
    }

    event.deferReply(true).queue();

    String code = event.getOption("code").getAsString().trim();
    String discordId = event.getUser().getId();

    DiscordLinkManager linkManager = discordManager.getLinkManager();
    boolean success = linkManager.verifyCode(discordId, code);

    if (success) {
      var uuid = linkManager.getMinecraftUUID(discordId);
      String username = linkManager.getUsername(uuid);

      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Account Linked Successfully!");
      embed.setDescription("Your Discord account has been linked to **" + username + "**");
      embed.setColor(new Color(0, 255, 0));
      embed.addField(
          "Benefits",
          "• Receive rank change notifications\n"
              + "• Appear on the leaderboard with your Discord tag\n"
              + "• Access Discord-exclusive features",
          false);
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
                                "", net.kyori.adventure.text.format.NamedTextColor.GREEN))
                        .append(
                            net.kyori.adventure.text.Component.text(
                                "Successfully linked your Discord account!",
                                net.kyori.adventure.text.format.NamedTextColor.AQUA))
                        .build());
              }
            });
      }

    } else {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Linking Failed");
      embed.setDescription("Invalid or expired code. Please try again.");
      embed.setColor(new Color(255, 0, 0));
      embed.addField(
          "How to get a code",
          "1. Join the Minecraft server\n"
              + "2. Use the command `/linkdiscord`\n"
              + "3. Copy the 6-digit code\n"
              + "4. Use `/link <code>` here in Discord",
          false);
      embed.setTimestamp(Instant.now());
      embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);

      event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
  }
}
