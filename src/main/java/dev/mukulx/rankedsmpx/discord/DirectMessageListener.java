package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.awt.Color;
import java.time.Instant;
import java.util.regex.Pattern;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DirectMessageListener extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9]{6}$");

  public DirectMessageListener(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    if (!event.isFromType(ChannelType.PRIVATE)) {
      return;
    }

    String message = event.getMessage().getContentRaw().trim().toUpperCase();
    String discordId = event.getAuthor().getId();

    DiscordLinkManager linkManager = discordManager.getLinkManager();
    var linkedUuid = linkManager.getMinecraftUUID(discordId);
    if (linkedUuid != null) {
      String username = linkManager.getUsername(linkedUuid);

      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("ℹAlready Linked");
      embed.setDescription("Your Discord account is already linked to **" + username + "**");
      embed.setColor(Color.BLUE);
      embed.addField("Want to unlink?", "Use `/unlink` in the Discord server", false);
      embed.setTimestamp(Instant.now());
      embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);

      event.getChannel().sendMessageEmbeds(embed.build()).queue();
      return;
    }

    if (CODE_PATTERN.matcher(message).matches()) {
      boolean success = linkManager.verifyCode(discordId, message);

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
        embed.setFooter(
            plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);

        event.getChannel().sendMessageEmbeds(embed.build()).queue();

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
        embed.setTitle("Invalid Code");
        embed.setDescription("The code you provided is invalid or expired.");
        embed.setColor(new Color(255, 0, 0));
        embed.addField(
            "How to get a code",
            "1. Join the Minecraft server\n"
                + "2. Use the command `/linkdiscord`\n"
                + "3. Copy the 6-digit code\n"
                + "4. Send it here or use `/link <code>`",
            false);
        embed.addField("Note", "Codes expire after 10 minutes", false);
        embed.setTimestamp(Instant.now());
        embed.setFooter(
            plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
      }
    } else {
      EmbedBuilder embed = new EmbedBuilder();
      embed.setTitle("Link Your Account");
      embed.setDescription("To link your Minecraft account to Discord:");
      embed.setColor(Color.CYAN);
      embed.addField("Step 1", "Join the Minecraft server", false);
      embed.addField("Step 2", "Use the command `/linkdiscord` in-game", false);
      embed.addField("Step 3", "Copy the 6-digit code you receive", false);
      embed.addField("Step 4", "Send the code here or use `/link <code>`", false);
      embed.addField("Example", "If your code is `ABC123`, just send:\n`ABC123`", false);
      embed.setTimestamp(Instant.now());
      embed.setFooter(plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX"), null);

      event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
  }
}
