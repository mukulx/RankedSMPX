package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.awt.Color;
import java.util.UUID;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankCommand extends ListenerAdapter {

  private final RankedSMPXPlugin plugin;
  private final DiscordManager discordManager;

  public RankCommand(@NotNull RankedSMPXPlugin plugin, @NotNull DiscordManager discordManager) {
    this.plugin = plugin;
    this.discordManager = discordManager;
  }

  public static CommandData getCommandData() {
    return Commands.slash("rank", "Check a player's rank with buffs and stats")
        .addOption(OptionType.USER, "player", "Player to check rank for (Discord mention)", false)
        .addOption(OptionType.STRING, "username", "Player's Minecraft username", false);
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
    if (!event.getName().equals("rank")) {
      return;
    }

    handleRankSlash(event);
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    if (event.getAuthor().isBot()) {
      return;
    }

    String message = event.getMessage().getContentRaw();
    if (!message.startsWith("?rank")) {
      return;
    }

    String[] args = message.split("\s+");

    if (args.length == 1) {
      handleOwnRank(event);
    } else {
      handleOtherRank(event, args);
    }
  }

  private void handleRankSlash(@NotNull SlashCommandInteractionEvent event) {
    User mentionedUser =
        event.getOption("player") != null ? event.getOption("player").getAsUser() : null;
    String username =
        event.getOption("username") != null ? event.getOption("username").getAsString() : null;

    if (mentionedUser == null && username == null) {
      String discordId = event.getUser().getId();
      UUID uuid = discordManager.getMinecraftUUID(discordId);

      if (uuid == null) {
        event
            .reply("You are not linked! Use `/link` to link your account.")
            .setEphemeral(true)
            .queue();
        return;
      }

      String mcUsername = getUsername(uuid);
      sendRankEmbedSlash(event, uuid, mcUsername, event.getUser());
    } else if (mentionedUser != null) {
      String discordId = mentionedUser.getId();
      UUID uuid = discordManager.getMinecraftUUID(discordId);

      if (uuid == null) {
        event
            .reply(mentionedUser.getAsMention() + " is not linked!")
            .setEphemeral(true)
            .queue();
        return;
      }

      String mcUsername = getUsername(uuid);
      sendRankEmbedSlash(event, uuid, mcUsername, mentionedUser);
    } else {
      Player player = Bukkit.getPlayer(username);
      if (player == null) {
        event
            .reply("Player `" + username + "` not found or not online!")
            .setEphemeral(true)
            .queue();
        return;
      }

      sendRankEmbedSlash(event, player.getUniqueId(), player.getName(), null);
    }
  }

  private void handleOwnRank(@NotNull MessageReceivedEvent event) {
    String discordId = event.getAuthor().getId();

    UUID uuid = discordManager.getMinecraftUUID(discordId);
    if (uuid == null) {
      event.getMessage().reply("You are not linked! Use `/link` to link your account.").queue();
      return;
    }

    String username = getUsername(uuid);
    sendRankEmbed(event, uuid, username, event.getAuthor());
  }

  private void handleOtherRank(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
    String target = args[1];

    if (!event.getMessage().getMentions().getUsers().isEmpty()) {
      User mentionedUser = event.getMessage().getMentions().getUsers().get(0);
      String discordId = mentionedUser.getId();

      UUID uuid = discordManager.getMinecraftUUID(discordId);
      if (uuid == null) {
        event.getMessage().reply(mentionedUser.getAsMention() + " is not linked!").queue();
        return;
      }

      String username = getUsername(uuid);
      sendRankEmbed(event, uuid, username, mentionedUser);
    } else {
      Player player = Bukkit.getPlayer(target);
      if (player == null) {
        event.getMessage().reply("Player `" + target + "` not found or not online!").queue();
        return;
      }

      sendRankEmbed(event, player.getUniqueId(), player.getName(), null);
    }
  }

  private void sendRankEmbedSlash(
      @NotNull SlashCommandInteractionEvent event,
      @NotNull UUID uuid,
      String username,
      @Nullable User discordUser) {
    Integer rank = plugin.getRankManager().getRank(uuid);

    if (rank == null) {
      event.reply("`" + username + "` is not ranked yet!").setEphemeral(true).queue();
      return;
    }

    EmbedBuilder embed = buildRankEmbed(uuid, username, rank, discordUser);
    event.replyEmbeds(embed.build()).queue();
  }

  private void sendRankEmbed(
      @NotNull MessageReceivedEvent event,
      @NotNull UUID uuid,
      String username,
      @Nullable User discordUser) {
    Integer rank = plugin.getRankManager().getRank(uuid);

    if (rank == null) {
      event.getMessage().reply("`" + username + "` is not ranked yet!").queue();
      return;
    }

    EmbedBuilder embed = buildRankEmbed(uuid, username, rank, discordUser);
    event.getMessage().replyEmbeds(embed.build()).queue();
  }

  private EmbedBuilder buildRankEmbed(
      @NotNull UUID uuid, String username, int rank, @Nullable User discordUser) {
    double hearts = plugin.getHealthManager().calculateHearts(rank);
    double potionMultiplier = plugin.getPotionManager().calculateMultiplier(rank);
    double xpMultiplier = plugin.getXPManager().calculateMultiplier(rank);

    int extraSlots = plugin.getConfigManager().getExtraSlots(rank);

    EmbedBuilder embed = new EmbedBuilder();
    embed.setTitle("Rank Information");
    embed.setColor(getRankColor(rank));

    if (discordUser != null) {
      embed.setDescription("**Player:** " + username + " (" + discordUser.getAsMention() + ")");
    } else {
      embed.setDescription("**Player:** " + username);
    }

    int totalRanks = plugin.getConfigManager().getTotalRanks();
    embed.addField("Rank", "#" + rank + " / " + totalRanks, true);
    embed.addField("Hearts", String.format("%.1f", hearts), true);
    embed.addField("Potion Duration", String.format("%.2fx", potionMultiplier), true);
    embed.addField("XP Multiplier", String.format("%.2fx", xpMultiplier), true);

    if (extraSlots > 0) {
      embed.addField("Extra Inventory", extraSlots + " slots", true);
    } else {
      embed.addField("Extra Inventory", "None", true);
    }

    String footerName = plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX");
    embed.setFooter(footerName + " • Use /rank in-game for more details");
    embed.setTimestamp(java.time.Instant.now());

    return embed;
  }

  @NotNull
  private String getUsername(@NotNull UUID uuid) {
    String username = discordManager.getLinkManager().getUsername(uuid);
    if (username != null) {
      return username;
    }

    username = plugin.getRankManager().getPlayerName(uuid);
    if (username != null && !username.equals("Unknown")) {
      return username;
    }

    Player player = Bukkit.getPlayer(uuid);
    if (player != null) {
      return player.getName();
    }

    return "Unknown";
  }

  private Color getRankColor(int rank) {
    if (rank == 1) return new Color(255, 215, 0);
    if (rank == 2) return new Color(192, 192, 192);
    if (rank == 3) return new Color(205, 127, 50);
    if (rank <= 10) return new Color(0, 255, 255);
    return new Color(128, 128, 128);
  }
}
