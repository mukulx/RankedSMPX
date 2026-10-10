package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

public class SetChannelCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public SetChannelCommand(
      @NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("setchannel", "Set a channel for RankedSMP features")
        .addOptions(
            new OptionData(OptionType.STRING, "type", "Channel type to set", true)
                .addChoice("Leaderboard", "leaderboard")
                .addChoice("Updates", "updates"))
        .setDefaultPermissions(
            net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions.enabledFor(
                Permission.ADMINISTRATOR));
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("setchannel")) {
      return;
    }

    if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
      event
          .reply("You need Administrator permission to use this command!")
          .setEphemeral(true)
          .queue();
      return;
    }

    String type = event.getOption("type").getAsString();

    if (type.equals("leaderboard")) {
      handleLeaderboardChannel(event);
    } else if (type.equals("updates")) {
      handleUpdatesChannel(event);
    }
  }

  private void handleUpdatesChannel(@NotNull SlashCommandInteractionEvent event) {
    if (!plugin.getDiscordConfig().getBoolean("updates.enabled", false)) {
      event
          .reply(
              "Updates channel feature is disabled!\n"
                  + "Enable it in `discord.yml`:\n"
                  + "```yaml\n"
                  + "updates:\n"
                  + "  enabled: true\n"
                  + "```")
          .setEphemeral(true)
          .queue();
      return;
    }

    String channelId = event.getChannel().getId();

    plugin.getDiscordConfig().set("updates.channel-id", channelId);
    plugin.saveDiscordConfig();

    event
        .reply(
            "Updates channel set to <#"
                + channelId
                + ">!\n"
                + "Game events and rank changes will be posted here.\n"
                + "Configure what to announce in `discord.yml` under `updates`.")
        .queue();

    plugin.getLogger().info("Updates channel set to: " + channelId);
  }

  private void handleLeaderboardChannel(@NotNull SlashCommandInteractionEvent event) {
    if (!plugin.getDiscordConfig().getBoolean("leaderboard.enabled", false)) {
      event
          .reply(
              "Leaderboard feature is disabled!\n"
                  + "Enable it in `discord.yml`:\n"
                  + "```yaml\n"
                  + "leaderboard:\n"
                  + "  enabled: true\n"
                  + "```")
          .setEphemeral(true)
          .queue();
      return;
    }

    String channelId = event.getChannel().getId();

    plugin.getDiscordConfig().set("leaderboard.channel-id", channelId);
    plugin.getDiscordConfig().set("leaderboard.message-id", ""); // Reset message ID
    plugin.saveDiscordConfig();

    int updateInterval = plugin.getDiscordConfig().getInt("leaderboard.update-interval", 300);

    event
        .reply(
            "Leaderboard channel set to <#"
                + channelId
                + ">!\n"
                + "The leaderboard will be posted here and updated automatically every "
                + updateInterval
                + " seconds.")
        .queue();

    if (discordManager.getLeaderboardManager() != null) {
      discordManager.getLeaderboardManager().startUpdating();
    }

    plugin.getLogger().info("Leaderboard channel set to: " + channelId);
  }
}
