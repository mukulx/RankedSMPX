package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.awt.Color;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class HelpCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public HelpCommand(@NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("help", "Show all available RankedSMPX commands");
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("help")) {
      return;
    }

    event.deferReply(true).queue();

    var config = plugin.getDiscordConfig();

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("RankedSMPX Commands");
    embed.setDescription("Here are all available commands for RankedSMPX:");
    embed.setColor(Color.decode("#00FFFF"));

    StringBuilder generalCommands = new StringBuilder();

    if (config.getBoolean("commands.help", true)) {
      generalCommands.append("`/help` - Show this help message\n");
    }

    if (config.getBoolean("commands.rank", true)) {
      generalCommands.append("`/rank [player]` - Check your rank or another player's rank\n");
    }

    if (config.getBoolean("commands.link", true)) {
      generalCommands.append("`/link <code>` - Link your Minecraft account to Discord\n");
    }

    if (config.getBoolean("commands.unlink", true)) {
      generalCommands.append("`/unlink` - Unlink your Minecraft account from Discord\n");
    }

    if (generalCommands.length() > 0) {
      embed.addField("General Commands", generalCommands.toString(), false);
    }

    StringBuilder adminCommands = new StringBuilder();

    if (config.getBoolean("commands.rankset", true)) {
      adminCommands.append("`/rankset <player> <rank>` - Set a player's rank (Admin)\n");
    }

    if (config.getBoolean("commands.rankrandom", true)) {
      adminCommands.append("`/rankrandom` - Randomly assign ranks to all players (Admin)\n");
    }

    if (config.getBoolean("commands.setchannel", true)) {
      adminCommands.append(
          "`/setchannel <type> <channel>` - Set leaderboard/updates channel (Admin)\n");
    }

    if (config.getBoolean("commands.rankexport", true)) {
      adminCommands.append("`/rankexport` - Export all rank data to JSON (Admin)\n");
    }

    if (config.getBoolean("commands.rankimport", true)) {
      adminCommands.append("`/rankimport` - Import rank data from JSON (Admin)\n");
    }

    if (adminCommands.length() > 0) {
      embed.addField("Admin Commands", adminCommands.toString(), false);
    }

    StringBuilder prefixCommands = new StringBuilder();
    prefixCommands.append("`?help` - Show this help message\n");
    prefixCommands.append("`?rank [player]` - Check rank\n");
    prefixCommands.append("`?link <code>` - Link account\n");
    prefixCommands.append("`?unlink` - Unlink account\n");

    embed.addField("Prefix Commands", prefixCommands.toString(), false);

    embed.addField(
        "ℹInformation",
        "• Use `/` for slash commands\n"
            + "• Use `?` for prefix commands\n"
            + "• Admin commands require proper permissions\n"
            + "• Link your account to access more features!",
        false);

    embed.setFooter(
        plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX")
            + " v"
            + plugin.getPluginMeta().getVersion(),
        null);
    embed.setTimestamp(java.time.Instant.now());

    event.getHook().sendMessageEmbeds(embed.build()).queue();
  }
}
