package dev.mukulx.rankedsmpx.discord;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiscordManager {

  private final RankedSMPXPlugin plugin;
  private DiscordLinkManager linkManager;
  private LeaderboardManager leaderboardManager;
  private RankNotificationManager notificationManager;
  private RoleSyncManager roleSyncManager;
  private UpdatesManager updatesManager;
  private JDA jda;
  private volatile boolean enabled;
  private volatile boolean initializing;

  public DiscordManager(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    this.enabled = false;
  }

  public synchronized boolean initialize() {
    if (initializing || enabled) {
      plugin.getLogger().info("Discord integration already initialized or initializing");
      return enabled;
    }

    initializing = true;

    try {
      if (!plugin.getDiscordConfig().getBoolean("enabled", false)) {
        plugin.getLogger().info("Discord integration is disabled in config");
        return false;
      }

      String botToken = plugin.getDiscordConfig().getString("bot.token", "");
      if (botToken.isEmpty() || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
        plugin
            .getLogger()
            .warning("Discord bot token not configured! Please set bot.token in discord.yml");
        return false;
      }

      plugin.getLogger().info("Connecting to Discord...");

      JDABuilder builder =
          JDABuilder.createDefault(botToken)
              .enableIntents(
                  GatewayIntent.GUILD_MEMBERS,
                  GatewayIntent.GUILD_MESSAGES,
                  GatewayIntent.MESSAGE_CONTENT,
                  GatewayIntent.DIRECT_MESSAGES)
              .setChunkingFilter(ChunkingFilter.ALL)
              .setMemberCachePolicy(MemberCachePolicy.ALL)
              .setStatus(OnlineStatus.ONLINE);

      String activityType = plugin.getDiscordConfig().getString("bot.activity.type", "PLAYING");
      String activityMessage =
          plugin.getDiscordConfig().getString("bot.activity.message", "RankedSMP");

      Activity activity =
          switch (activityType.toUpperCase()) {
            case "WATCHING" -> Activity.watching(activityMessage);
            case "LISTENING" -> Activity.listening(activityMessage);
            case "COMPETING" -> Activity.competing(activityMessage);
            default -> Activity.playing(activityMessage);
          };

      builder.setActivity(activity);

      jda = builder.build();

      jda.awaitReady();
      plugin.getLogger().info("Discord bot connected successfully!");

      linkManager = new DiscordLinkManager(plugin);
      linkManager.initialize();

      if (plugin.getDiscordConfig().getBoolean("leaderboard.enabled", true)) {
        leaderboardManager = new LeaderboardManager(plugin, this, jda);
      }

      if (plugin.getDiscordConfig().getBoolean("notifications.enabled", true)) {
        notificationManager = new RankNotificationManager(plugin, this, jda);
      }

      if (plugin.getDiscordConfig().getBoolean("updates.enabled", false)) {
        updatesManager = new UpdatesManager(plugin, jda);
      }

      String guildId = plugin.getDiscordConfig().getString("bot.guild-id", "");

      if (!guildId.isEmpty() && !guildId.equals("YOUR_GUILD_ID_HERE")) {
        var guild = jda.getGuildById(guildId);
        if (guild != null) {
          if (plugin.getDiscordConfig().getBoolean("role-sync.enabled", false)) {
            roleSyncManager = new RoleSyncManager(plugin, this, jda, guild);
            roleSyncManager.initialize();
          }

          registerSlashCommands(guild);
        } else {
          plugin.getLogger().warning("Guild not found with ID: " + guildId);
        }
      }

      registerEventListeners();

      this.enabled = true;
      plugin.getLogger().info("Discord integration enabled successfully!");
      plugin.getLogger().info("Linked accounts: " + linkManager.getTotalLinked());

      if (leaderboardManager != null) {
        String channelId = plugin.getDiscordConfig().getString("leaderboard.channel-id", "");
        if (!channelId.isEmpty()) {
          leaderboardManager.startUpdating();
        }
      }

      return true;

    } catch (Exception e) {
      plugin.getLogger().warning("Failed to initialize Discord integration: " + e.getMessage());
      e.printStackTrace();
      return false;
    } finally {
      initializing = false;
    }
  }

  private void registerSlashCommands(net.dv8tion.jda.api.entities.Guild guild) {
    var config = plugin.getDiscordConfig();

    if (config.getBoolean("bot.clear-previous-commands", false)) {
      plugin.getLogger().info("Clearing all previous slash commands...");
      guild
          .updateCommands()
          .queue(
              success -> {
                plugin.getLogger().info("Successfully cleared all previous slash commands!");
                plugin.getLogger().info("Registering new slash commands...");
                registerCommands(guild, config);
              },
              error -> {
                plugin
                    .getLogger()
                    .warning("Failed to clear previous commands: " + error.getMessage());
                plugin.getLogger().info("Attempting to register commands anyway...");
                registerCommands(guild, config);
              });
    } else {
      registerCommands(guild, config);
    }
  }

  private void registerCommands(
      net.dv8tion.jda.api.entities.Guild guild,
      org.bukkit.configuration.file.FileConfiguration config) {

    if (config.getBoolean("commands.help", true)) {
      HelpCommand helpCommand = new HelpCommand(plugin, this);
      jda.addEventListener(helpCommand);
      guild
          .upsertCommand(HelpCommand.getCommandData())
          .queue(
              success -> plugin.getLogger().info("Registered /help command"),
              error ->
                  plugin
                      .getLogger()
                      .warning("Failed to register /help command: " + error.getMessage()));
    }

    if (config.getBoolean("commands.link", true)) {
      LinkDiscordCommand linkCommand = new LinkDiscordCommand(plugin, this);
      jda.addEventListener(linkCommand);
      guild
          .upsertCommand(LinkDiscordCommand.getCommandData())
          .queue(
              success -> plugin.getLogger().info("Registered /link command"),
              error ->
                  plugin
                      .getLogger()
                      .warning("Failed to register /link command: " + error.getMessage()));
    }

    if (config.getBoolean("commands.unlink", true)) {
      UnlinkDiscordCommand unlinkCommand = new UnlinkDiscordCommand(plugin, this);
      jda.addEventListener(unlinkCommand);
      guild
          .upsertCommand(UnlinkDiscordCommand.getCommandData())
          .queue(
              success -> plugin.getLogger().info("Registered /unlink command"),
              error ->
                  plugin
                      .getLogger()
                      .warning("Failed to register /unlink command: " + error.getMessage()));
    }

    if (config.getBoolean("commands.forcelink", true)) {
      ForceLinkCommand forceLinkCommand = new ForceLinkCommand(plugin, this);
      jda.addEventListener(forceLinkCommand);
      guild
          .upsertCommand(ForceLinkCommand.getCommandData())
          .queue(
              success -> plugin.getLogger().info("Registered /forcelink command"),
              error ->
                  plugin
                      .getLogger()
                      .warning("Failed to register /forcelink command: " + error.getMessage()));
    }

    if (config.getBoolean("commands.forceunlink", true)) {
      ForceUnlinkCommand forceUnlinkCommand = new ForceUnlinkCommand(plugin, this);
      jda.addEventListener(forceUnlinkCommand);
      guild
          .upsertCommand(ForceUnlinkCommand.getCommandData())
          .queue(
              success -> plugin.getLogger().info("Registered /forceunlink command"),
              error ->
                  plugin
                      .getLogger()
                      .warning("Failed to register /forceunlink command: " + error.getMessage()));
    }

    if (config.getBoolean("commands.setchannel", true)) {
      SetChannelCommand setChannelCommand = new SetChannelCommand(plugin, this);
      jda.addEventListener(setChannelCommand);
      guild.upsertCommand(SetChannelCommand.getCommandData()).queue();
    }

    if (config.getBoolean("commands.rank", true)) {
      RankCommand rankCommand = new RankCommand(plugin, this);
      jda.addEventListener(rankCommand);
      guild.upsertCommand(RankCommand.getCommandData()).queue();
    }

    if (config.getBoolean("commands.rankset", true)) {
      RankSetCommand rankSetCommand = new RankSetCommand(plugin, this);
      jda.addEventListener(rankSetCommand);
      guild.upsertCommand(RankSetCommand.getCommandData()).queue();
    }

    if (config.getBoolean("commands.rankrandom", true)) {
      RankRandomCommand rankRandomCommand = new RankRandomCommand(plugin, this);
      jda.addEventListener(rankRandomCommand);
      guild.upsertCommand(RankRandomCommand.getCommandData()).queue();
    }

    if (config.getBoolean("commands.rankexport", true)) {
      RankExportDiscordCommand rankExportCommand = new RankExportDiscordCommand(plugin, this);
      jda.addEventListener(rankExportCommand);
      guild.upsertCommand(RankExportDiscordCommand.getCommandData()).queue();
    }

    if (config.getBoolean("commands.rankimport", true)) {
      RankImportDiscordCommand rankImportCommand = new RankImportDiscordCommand(plugin, this);
      jda.addEventListener(rankImportCommand);
      guild.upsertCommand(RankImportDiscordCommand.getCommandData()).queue();
    }
  }

  private void registerEventListeners() {
    DirectMessageListener dmListener = new DirectMessageListener(plugin, this);
    jda.addEventListener(dmListener);
    plugin.getLogger().info("Registered DM listener for account linking");
  }

  public void shutdown() {
    if (linkManager != null) {
      linkManager.shutdown();
    }
    if (leaderboardManager != null) {
      leaderboardManager.stopUpdating();
    }
    if (roleSyncManager != null) {
      roleSyncManager.shutdown();
    }
    if (notificationManager != null) {
      notificationManager.shutdown();
    }
    if (updatesManager != null) {
      updatesManager.shutdown();
    }
    if (jda != null) {
      jda.shutdown();
    }
    enabled = false;
    plugin.getLogger().info("Discord integration shutdown complete");
  }

  @Nullable
  public String getDiscordId(@NotNull java.util.UUID uuid) {
    if (linkManager == null) {
      return null;
    }
    return linkManager.getDiscordId(uuid);
  }

  @Nullable
  public java.util.UUID getMinecraftUUID(@NotNull String discordId) {
    if (linkManager == null) {
      return null;
    }
    return linkManager.getMinecraftUUID(discordId);
  }

  public boolean isLinked(@NotNull java.util.UUID uuid) {
    if (linkManager == null) {
      return false;
    }
    return linkManager.isLinked(uuid);
  }

  @NotNull
  public DiscordLinkManager getLinkManager() {
    return linkManager;
  }

  @Nullable
  public LeaderboardManager getLeaderboardManager() {
    return leaderboardManager;
  }

  @Nullable
  public RankNotificationManager getNotificationManager() {
    return notificationManager;
  }

  @Nullable
  public RoleSyncManager getRoleSyncManager() {
    return roleSyncManager;
  }

  @Nullable
  public UpdatesManager getUpdatesManager() {
    return updatesManager;
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Nullable
  public JDA getBot() {
    return jda;
  }

  public void forceUpdate() {
    if (leaderboardManager != null) {
      leaderboardManager.updateLeaderboard();
    }
  }

  @NotNull
  public String getEmbedFooterName() {
    return plugin.getDiscordConfig().getString("embed-footer-name", "RankedSMPX");
  }
}
