package dev.mukulx.rankedsmpx;

import dev.mukulx.rankedsmpx.commands.ExtraInvSeeCommand;
import dev.mukulx.rankedsmpx.commands.InvCommand;
import dev.mukulx.rankedsmpx.commands.PaperCommandAdapter;
import dev.mukulx.rankedsmpx.commands.RankCommand;
import dev.mukulx.rankedsmpx.commands.RankExportCommand;
import dev.mukulx.rankedsmpx.commands.RankFixCommand;
import dev.mukulx.rankedsmpx.commands.RankImportCommand;
import dev.mukulx.rankedsmpx.commands.RankInfoCommand;
import dev.mukulx.rankedsmpx.commands.RankRandomCommand;
import dev.mukulx.rankedsmpx.commands.RankTopCommand;
import dev.mukulx.rankedsmpx.commands.RankTransferCommand;
import dev.mukulx.rankedsmpx.commands.RankedSMPCommand;
import dev.mukulx.rankedsmpx.commands.RanksCommand;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.dialogs.DialogManager;
import dev.mukulx.rankedsmpx.discord.DiscordManager;
import dev.mukulx.rankedsmpx.listeners.ChatListener;
import dev.mukulx.rankedsmpx.listeners.CombatListener;
import dev.mukulx.rankedsmpx.listeners.DiscordLinkListener;
import dev.mukulx.rankedsmpx.listeners.DragonEggLocatorListener;
import dev.mukulx.rankedsmpx.listeners.ExperienceListener;
import dev.mukulx.rankedsmpx.listeners.ExtraInvSeeListener;
import dev.mukulx.rankedsmpx.listeners.HammerListener;
import dev.mukulx.rankedsmpx.listeners.HammerProtectionListener;
import dev.mukulx.rankedsmpx.listeners.InventoryListener;
import dev.mukulx.rankedsmpx.listeners.ItemStorageListener;
import dev.mukulx.rankedsmpx.listeners.PlayerDeathListener;
import dev.mukulx.rankedsmpx.listeners.PlayerJoinListener;
import dev.mukulx.rankedsmpx.listeners.PlayerQuitListener;
import dev.mukulx.rankedsmpx.listeners.PlayerRespawnListener;
import dev.mukulx.rankedsmpx.listeners.PotionEffectListener;
import dev.mukulx.rankedsmpx.listeners.RanksGUIListener;
import dev.mukulx.rankedsmpx.managers.BedrockSupport;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.ConfigUpdater;
import dev.mukulx.rankedsmpx.managers.ConfigLayout;
import dev.mukulx.rankedsmpx.managers.SplitConfiguration;
import dev.mukulx.rankedsmpx.managers.CustomConfigManager;
import dev.mukulx.rankedsmpx.managers.DiscordConfigUpdater;
import dev.mukulx.rankedsmpx.managers.FirstTimeSetupManager;
import dev.mukulx.rankedsmpx.managers.GameManager;
import dev.mukulx.rankedsmpx.managers.HammerManager;
import dev.mukulx.rankedsmpx.managers.HealthManager;
import dev.mukulx.rankedsmpx.managers.HierarchyHammerManager;
import dev.mukulx.rankedsmpx.managers.InventoryManager;
import dev.mukulx.rankedsmpx.managers.LuckPermsHook;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.PotionEffectStore;
import dev.mukulx.rankedsmpx.managers.PotionManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.managers.RankRevealAnimation;
import dev.mukulx.rankedsmpx.managers.SpearManager;
import dev.mukulx.rankedsmpx.managers.TABIntegration;
import dev.mukulx.rankedsmpx.managers.VanillaTabManager;
import dev.mukulx.rankedsmpx.managers.XPManager;
import dev.mukulx.rankedsmpx.metrics.Metrics;
import dev.mukulx.rankedsmpx.metrics.Metrics.SimplePie;
import dev.mukulx.rankedsmpx.placeholders.RankedSMPPlaceholders;
import dev.mukulx.rankedsmpx.utils.ErrorReports;
import dev.mukulx.rankedsmpx.utils.PlayerResolver;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import dev.mukulx.rankedsmpx.utils.UpdateChecker;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankedSMPXPlugin extends JavaPlugin {

  private ConfigManager configManager;
  private ConfigUpdater configUpdater;
  private MessageManager messageManager;
  private DatabaseManager databaseManager;
  private RankManager rankManager;
  private PlayerResolver playerResolver;
  private GameManager gameManager;
  private HealthManager healthManager;
  private PotionManager potionManager;
  private XPManager xpManager;
  private InventoryManager inventoryManager;
  private HammerManager hammerManager;
  private HierarchyHammerManager hierarchyHammerManager;
  private DragonEggLocatorListener dragonEggLocatorListener;
  private SpearManager spearManager;
  private TABIntegration tabIntegration;
  private VanillaTabManager vanillaTabManager;
  private LuckPermsHook luckPermsHook;
  private BedrockSupport bedrockSupport;
  private DiscordManager discordManager;
  private UpdateChecker updateChecker;
  private PotionEffectListener potionEffectListener;
  private PotionEffectStore potionEffectStore;
  private RankRevealAnimation rankRevealAnimation;
  private FirstTimeSetupManager firstTimeSetupManager;
  private DialogManager dialogManager;
  private CustomConfigManager customConfigManager;

  private boolean pluginEnabled = true;

  private dev.mukulx.rankedsmpx.managers.InactivityManager inactivityManager;
  private org.bukkit.configuration.file.FileConfiguration discordConfig;
  private java.io.File discordConfigFile;

  @Override
  public void onEnable() {
    setupErrorLoggingHook();
    try {
      saveDefaultConfig();
      ConfigLayout.migrate(this);
      reloadConfig();
      loadDiscordConfig(true);

      DiscordConfigUpdater discordConfigUpdater = new DiscordConfigUpdater(this);
      discordConfigUpdater.updateDiscordConfig();

      this.configUpdater = new ConfigUpdater(this);
      configUpdater.updateConfig();

      this.configManager = new ConfigManager(this);
      this.customConfigManager = new CustomConfigManager(this);
      this.messageManager = new MessageManager(this, configManager);
      this.dialogManager = new DialogManager(this);

      printBanner();

      checkForOldPlugin();
      this.databaseManager = new DatabaseManager(this, configManager);
      this.rankManager = new RankManager(this, databaseManager);
      this.bedrockSupport = new BedrockSupport(this);
      this.playerResolver = new PlayerResolver(databaseManager, messageManager, bedrockSupport);
      this.gameManager = new GameManager(this, rankManager, messageManager);
      this.healthManager = new HealthManager(this, rankManager, configManager);
      this.potionManager = new PotionManager(this, configManager);
      this.xpManager = new XPManager(this, configManager);
      this.inventoryManager = new InventoryManager(this, rankManager, configManager);
      this.hammerManager = new HammerManager(this, configManager, messageManager);
      this.hierarchyHammerManager = new HierarchyHammerManager(this, rankManager);
      this.potionEffectStore =
          new PotionEffectStore(this, databaseManager);
      this.rankRevealAnimation = new RankRevealAnimation(this);

      try {
        dev.mukulx.rankedsmpx.locator.LocatorBarAPI.setLoggingEnabled(isDebug());
        dev.mukulx.rankedsmpx.locator.LocatorBarAPI.init(this);
        if (configManager.getConfig().getBoolean("locator-bar.hide-players", true)) {
          dev.mukulx.rankedsmpx.locator.LocatorBarAPI.hideAll();
        }
      } catch (Throwable t) {
        getLogger().warning("Failed to initialize LocatorBarAPI: " + t.getMessage());
      }

      if (configManager.getConfig().getBoolean("dragon-egg-locator.enabled", true)) {
        this.dragonEggLocatorListener = new DragonEggLocatorListener(this);
        getLogger().info("Dragon Egg Locator enabled!");
      } else {
        getLogger().info("Dragon Egg Locator is disabled in config");
      }

      this.spearManager = new SpearManager(this);

      int pluginId = 31075;
      Metrics metrics = new Metrics(this, pluginId);

      metrics.addCustomChart(
          new SimplePie(
              "database_type",
              () -> configManager.getConfig().getString("database.type", "sqlite")));

      metrics.addCustomChart(
          new SimplePie(
              "discord_enabled",
              () -> discordConfig.getBoolean("enabled", false) ? "Enabled" : "Disabled"));

      getLogger().info("bStats metrics initialized successfully.");

      if (!databaseManager.initialize()) {
        getLogger().severe("Failed to initialize database! Disabling plugin...");
        Bukkit.getPluginManager().disablePlugin(this);
        return;
      }

      rankManager.loadRanks();

      gameManager.initializeGameState();

      this.firstTimeSetupManager = new FirstTimeSetupManager(this, messageManager, databaseManager);

      registerCommands();
      getServer().getPluginManager().registerEvents(new dev.mukulx.rankedsmpx.listeners.CommandListHider(), this);

      registerListeners();

      if (configManager.getConfig().getBoolean("tab-integration.enabled", false)) {
        if (Bukkit.getPluginManager().getPlugin("TAB") != null) {
          this.tabIntegration = new TABIntegration(this, rankManager, configManager);
          tabIntegration.initialize();
          getLogger().info("TAB integration enabled!");
        } else {
          getLogger().warning("TAB integration enabled but TAB plugin not found!");
        }
      }

      if (configManager.getConfig().getBoolean("tab-integration.enabled", true)) {
        if (Bukkit.getPluginManager().getPlugin("TAB") == null) {
          this.vanillaTabManager = new VanillaTabManager(this, rankManager, configManager);
          vanillaTabManager.initialize();
          getLogger().info("Vanilla tab integration enabled (TAB plugin not detected)!");
        } else {
          getLogger()
              .info("Vanilla tab integration skipped (TAB plugin detected - using TAB instead)");
        }
      }

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        new RankedSMPPlaceholders(this, rankManager, configManager).register();
        getLogger().info("PlaceholderAPI integration enabled!");
      }

      setupLuckPerms();
      bedrockSupport.initialize();

      getServer()
          .getServicesManager()
          .register(
              dev.mukulx.rankedsmpx.api.RankedSMPApi.class,
              new dev.mukulx.rankedsmpx.api.internal.RankedSMPApiImpl(this),
              this,
              org.bukkit.plugin.ServicePriority.Normal);

      this.inactivityManager = new dev.mukulx.rankedsmpx.managers.InactivityManager(this);
      inactivityManager.start();

      if (discordConfig.getBoolean("enabled", false)) {
        this.discordManager = new DiscordManager(this);
        discordManager.initialize();
      }

      SchedulerUtil.runLater(this, this::reapplyHealthToOnlinePlayers, 20L);

      this.updateChecker = new UpdateChecker(this);
      updateChecker.checkForUpdates();

    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed to enable plugin!", e);

      try {
        if (discordManager != null) {
          discordManager.shutdown();
        }
        if (databaseManager != null) {
          databaseManager.close();
        }
        if (tabIntegration != null) {
          tabIntegration.shutdown();
        }
      } catch (Exception cleanupError) {
        getLogger()
            .log(Level.SEVERE, "Error during cleanup after failed initialization!", cleanupError);
      }

      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  @Override
  public void onDisable() {
    try {
      if (rankRevealAnimation != null) {
        rankRevealAnimation.cancelAllAnimations();
      }

      if (healthManager != null) {
        healthManager.cancelAggressiveTask();
      }

      if (inactivityManager != null) {
        inactivityManager.stop();
      }

      if (tabIntegration != null) {
        tabIntegration.shutdown();
      }

      if (vanillaTabManager != null) {
        vanillaTabManager.shutdown();
      }

      if (discordManager != null) {
        discordManager.shutdown();
      }

      if (luckPermsHook != null) {
        luckPermsHook.shutdown();
      }

      if (dragonEggLocatorListener != null) {
        dragonEggLocatorListener.cleanup();
      }

      try {
        dev.mukulx.rankedsmpx.locator.LocatorBarAPI.shutdown();
      } catch (Throwable ignored) {
      }

      if (rankManager != null) {
        rankManager.saveAllRanks();
      }

      if (inventoryManager != null) {
        inventoryManager.saveAllInventories();
      }

      resetPlayersToVanilla();

      if (databaseManager != null) {
        databaseManager.close();
      }

      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        new RankedSMPPlaceholders(this, rankManager, configManager).unregister();
      }

      getLogger().info("RankedSMP has been disabled!");

    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Error during plugin disable!", e);
    }
  }

  private void registerListeners() {
    try {
      PluginManager pm = getServer().getPluginManager();

      pm.registerEvents(
          new PlayerJoinListener(this, rankManager, healthManager, inventoryManager), this);

      pm.registerEvents(
          new PlayerDeathListener(this, rankManager, gameManager, messageManager, configManager),
          this);

      pm.registerEvents(new PlayerRespawnListener(this, healthManager), this);

      pm.registerEvents(new PlayerQuitListener(this), this);

      pm.registerEvents(new dev.mukulx.rankedsmpx.listeners.PlayerKickListener(this), this);

      pm.registerEvents(new ExperienceListener(this, xpManager), this);

      potionEffectListener = new PotionEffectListener(this, potionManager);
      pm.registerEvents(potionEffectListener, this);

      pm.registerEvents(new InventoryListener(this, inventoryManager), this);

      pm.registerEvents(new HammerListener(this, hammerManager), this);

      pm.registerEvents(new HammerProtectionListener(hammerManager), this);

      pm.registerEvents(new RanksGUIListener(this, messageManager), this);

      pm.registerEvents(new ChatListener(this, rankManager, configManager), this);

      pm.registerEvents(new CombatListener(this, inventoryManager), this);

      pm.registerEvents(new ExtraInvSeeListener(this, inventoryManager), this);

      pm.registerEvents(new ItemStorageListener(this), this);

      if (dragonEggLocatorListener != null) {
        pm.registerEvents(dragonEggLocatorListener, this);

        dragonEggLocatorListener.initializeOnlinePlayers();
      }

      pm.registerEvents(new dev.mukulx.rankedsmpx.listeners.DiscordLinkListener(this), this);

      pm.registerEvents(
          new dev.mukulx.rankedsmpx.listeners.RankItemListener(
              this, customConfigManager, rankManager, healthManager),
          this);

    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed during listener registration!", e);
      throw e;
    }
  }

  private void registerCommands() {
    try {

      registerCommand(
          "rankedsmp",
          "Main RankedSMPX command",
          java.util.List.of("rsmp"),
          new RankedSMPCommand(this, gameManager, rankManager, messageManager, configManager));

      registerCommand(
          "rankinfo",
          "View your rank information",
          java.util.List.of("ri", "myrank"),
          new RankInfoCommand(this, rankManager, messageManager, configManager));

      registerCommand(
          "ranktop",
          "View top ranked players",
          java.util.List.of("rt", "topranks"),
          new RankTopCommand(this, rankManager, messageManager));

      registerCommand("rank", new RankCommand(this, rankManager, messageManager, configManager));

      registerCommand("ranks", new RanksCommand(this, rankManager, messageManager));

      registerCommand(
          "extrainventory",
          "Open your extra inventory",
          java.util.List.of("inv", "extrainv", "ei", "einv"),
          new InvCommand(this, inventoryManager, messageManager));

      registerCommand(
          "einvsee",
          "View and edit a player's extra inventory",
          java.util.List.of("extrainvsee"),
          new ExtraInvSeeCommand(this));

      registerCommand(
          "ranktransfer",
          "Transfer your rank to another player",
          java.util.List.of("transfer", "giverank"),
          new RankTransferCommand(this, rankManager, messageManager));

      registerCommand(
          "rankblacklist",
          new dev.mukulx.rankedsmpx.commands.RankBlacklistCommand(
              this, databaseManager, rankManager, messageManager));

      registerCommand(
          "rankhistory",
          new dev.mukulx.rankedsmpx.commands.RankHistoryCommand(
              this, databaseManager, messageManager));

      registerCommand(
          "rankhistorywipe",
          new dev.mukulx.rankedsmpx.commands.RankHistoryWipeCommand(
              this, databaseManager, messageManager));

      registerCommand("rankexport", new RankExportCommand(this, databaseManager, messageManager));

      registerCommand(
          "rankimport", new RankImportCommand(this, databaseManager, rankManager, messageManager));

      registerCommand(
          "rankfix", new RankFixCommand(this, databaseManager, rankManager, messageManager));

      registerCommand(
          "linkdiscord",
          new dev.mukulx.rankedsmpx.commands.LinkDiscordCommand(this, messageManager));

      registerCommand(
          "unlinkdiscord",
          new dev.mukulx.rankedsmpx.commands.UnlinkDiscordCommand(this, messageManager));

      registerCommand(
          "rankrandom",
          "Assign a rank to a random player",
          java.util.List.of("rrandom", "randomrank"),
          new RankRandomCommand(this, rankManager, messageManager));

    } catch (Exception e) {
      getLogger().log(Level.SEVERE, "Failed during command registration!", e);
      throw e;
    }
  }

  private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
    registerCommand(name, "RankedSMPX command", java.util.List.of(), executor);
  }

  private void registerCommand(
      String name,
      String description,
      java.util.List<String> aliases,
      org.bukkit.command.CommandExecutor executor) {
    registerCommand(name, description, aliases, new PaperCommandAdapter(name, executor));
  }

  private SplitConfiguration splitConfig;

  @Override
  public void reloadConfig() {
    splitConfig = ConfigLayout.load(this);
  }

  @Override
  public org.bukkit.configuration.file.FileConfiguration getConfig() {
    if (splitConfig == null) {
      reloadConfig();
    }
    return splitConfig;
  }

  @Override
  public void saveConfig() {
    try {
      getConfig();
      splitConfig.saveAll();
    } catch (java.io.IOException e) {
      getLogger().log(Level.SEVERE, "Could not save the configuration files", e);
    }
  }

  public void reload() {
    reloadConfig();

    configUpdater.updateConfig();

    loadDiscordConfig(false);
    DiscordConfigUpdater discordConfigUpdater = new DiscordConfigUpdater(this);
    discordConfigUpdater.updateDiscordConfig();

    configManager.reload();
    int totalRanks = configManager.getTotalRanks();
    getLogger().info("ranks.yml: " + totalRanks + " ranks loaded");
    if (rankManager != null) {
      long above = rankManager.getAllRanks().values().stream().filter(r -> r > totalRanks).count();
      if (above > 0) {
        getLogger().warning(above + " player(s) hold a rank above " + totalRanks + " (the highest row in ranks.yml).");
      }
    }

    if (customConfigManager != null) {
      customConfigManager.reload();
    }

    messageManager.reload();
    if (inactivityManager != null) {
      inactivityManager.start();
    }
    dev.mukulx.rankedsmpx.locator.LocatorBarAPI.setLoggingEnabled(isDebug());

    healthManager.cancelAggressiveTask();

    setupLuckPerms();
    bedrockSupport.initialize();

    for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
      if (inventoryManager != null) {
        inventoryManager.removeExtraInventory(player.getUniqueId());
      }
      SchedulerUtil.runForEntity(this, player, () -> healthManager.fixPlayerHealth(player));
      if (luckPermsHook != null) {
        luckPermsHook.signalUpdate(player);
      }
    }

    if (spearManager != null && spearManager.isSpearsSupported()) {
      spearManager.updateAllSpears();
    }

    if (tabIntegration != null) {
      tabIntegration.reload();
    }
    if (vanillaTabManager != null) {
      vanillaTabManager.reload();
    }

    if (discordManager != null && discordConfig.getBoolean("enabled", false)) {
      discordManager.shutdown();
      discordManager = new DiscordManager(this);
      discordManager.initialize();
    } else if (discordManager != null && !discordConfig.getBoolean("enabled", false)) {
      discordManager.shutdown();
      discordManager = null;
    } else if (discordManager == null && discordConfig.getBoolean("enabled", false)) {
      discordManager = new DiscordManager(this);
      discordManager.initialize();
    }

    getLogger().info("Configuration reloaded!");
  }

  private void setupLuckPerms() {
    if (luckPermsHook != null) {
      luckPermsHook.shutdown();
      luckPermsHook = null;
    }
    if (getConfig().getBoolean("luckperms.enabled", false)
        && Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
      luckPermsHook = new LuckPermsHook(this, rankManager);
      luckPermsHook.initialize();
    }
  }

  @NotNull
  public ConfigUpdater getConfigUpdater() {
    return configUpdater;
  }

  @NotNull
  public ConfigManager getConfigManager() {
    return configManager;
  }

  @NotNull
  public MessageManager getMessageManager() {
    return messageManager;
  }

  @NotNull
  public DialogManager getDialogManager() {
    return dialogManager;
  }

  @NotNull
  public DatabaseManager getDatabaseManager() {
    return databaseManager;
  }

  @NotNull
  public RankManager getRankManager() {
    return rankManager;
  }

  @NotNull
  public PlayerResolver getPlayerResolver() {
    return playerResolver;
  }

  @NotNull
  public GameManager getGameManager() {
    return gameManager;
  }

  @NotNull
  public HealthManager getHealthManager() {
    return healthManager;
  }

  @NotNull
  public PotionManager getPotionManager() {
    return potionManager;
  }

  @NotNull
  public XPManager getXPManager() {
    return xpManager;
  }

  @NotNull
  public InventoryManager getInventoryManager() {
    return inventoryManager;
  }

  @NotNull
  public HammerManager getHammerManager() {
    return hammerManager;
  }

  @NotNull
  public HierarchyHammerManager getHierarchyHammerManager() {
    return hierarchyHammerManager;
  }

  @NotNull
  public dev.mukulx.rankedsmpx.managers.CustomConfigManager getCustomConfigManager() {
    return customConfigManager;
  }

  @Nullable
  public PotionEffectStore getPotionEffectStore() {
    return potionEffectStore;
  }

  @NotNull

  @Nullable
  public PotionEffectListener getPotionEffectListener() {
    return potionEffectListener;
  }

  @NotNull
  public RankRevealAnimation getRankRevealAnimation() {
    return rankRevealAnimation;
  }

  @Nullable
  public FirstTimeSetupManager getFirstTimeSetupManager() {
    return firstTimeSetupManager;
  }

  @NotNull
  public SpearManager getSpearManager() {
    return spearManager;
  }

  @Nullable
  public TABIntegration getTabIntegration() {
    return tabIntegration;
  }

  @Nullable
  public VanillaTabManager getVanillaTabManager() {
    return vanillaTabManager;
  }

  @Nullable
  public LuckPermsHook getLuckPermsHook() {
    return luckPermsHook;
  }

  @NotNull
  public BedrockSupport getBedrockSupport() {
    return bedrockSupport;
  }

  @Nullable
  public DiscordManager getDiscordManager() {
    return discordManager;
  }

  @NotNull
  public UpdateChecker getUpdateChecker() {
    return updateChecker;
  }

  @NotNull
  public org.bukkit.configuration.file.FileConfiguration getDiscordConfig() {
    return discordConfig;
  }

  private void loadDiscordConfig(boolean logLoad) {
    discordConfigFile = new java.io.File(getDataFolder(), "discord.yml");

    if (!discordConfigFile.exists()) {
      saveResource("discord.yml", false);
    }

    discordConfig =
        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(discordConfigFile);
    if (logLoad) {
      getLogger().info("Loaded discord.yml configuration");
    }
  }

  /** Changes one value in discord.yml, keeping its comments, and reloads the file. */
  public boolean setDiscordValue(@NotNull String path, @NotNull Object value) {
    boolean ok =
        configUpdater.updateValuesIn(discordConfigFile, java.util.Map.of(path, String.valueOf(value)));
    if (ok) {
      loadDiscordConfig(false);
    }
    return ok;
  }

  public void saveDiscordConfig() {
    try {
      discordConfig.save(discordConfigFile);
    } catch (java.io.IOException e) {
      getLogger().severe("Could not save discord.yml: " + e.getMessage());
    }
  }

  private void printBanner() {
    printBannerAdventure();
  }

  private void printBannerAdventure() {
    TextColor startColor = TextColor.fromHexString("#00FFFF"); // Cyan
    TextColor endColor = TextColor.fromHexString("#008B8B"); // Dark Cyan

    String[] bannerLines = {
      " ██████╗  █████╗ ███╗   ██╗██╗  ██╗███████╗██████╗ ███████╗███╗   ███╗██████╗ ██╗  ██╗",
      " ██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝██╔════╝██╔══██╗██╔════╝████╗ ████║██╔══██╗╚██╗██╔╝",
      " ██████╔╝███████║██╔██╗ ██║█████╔╝ █████╗  ██║  ██║███████╗██╔████╔██║██████╔╝ ╚███╔╝ ",
      " ██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ ██╔══╝  ██║  ██║╚════██║██║╚██╔╝██║██╔═══╝  ██╔██╗ ",
      " ██║  ██║██║  ██║██║ ╚████║██║  ██╗███████╗██████╔╝███████║██║ ╚═╝ ██║██║     ██╔╝ ██╗",
      " ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝╚══════╝╚═════╝ ╚══════╝╚═╝     ╚═╝╚═╝     ╚═╝  ╚═╝"
    };

    Bukkit.getConsoleSender().sendMessage(Component.empty());

    for (String line : bannerLines) {
      Component gradientLine = createGradient(line, startColor, endColor);
      Bukkit.getConsoleSender().sendMessage(gradientLine);
    }

    Bukkit.getConsoleSender().sendMessage(Component.empty());

    TextColor cyanColor = TextColor.fromHexString("#00FFFF");
    TextColor grayColor = TextColor.fromHexString("#AAAAAA");

    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text("   By ", grayColor).append(Component.text("mukulx", cyanColor)));

    Bukkit.getConsoleSender()
        .sendMessage(Component.text("   Thanks for using the plugin!", grayColor));

    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text("   Discord: ", grayColor)
                .append(Component.text("https://discord.gg/QGsJYht2vy", cyanColor)));

    Bukkit.getConsoleSender().sendMessage(Component.empty());
  }

  private void setupErrorLoggingHook() {
    try {
      Logger pluginLogger = getLogger();
      pluginLogger.addHandler(
          new Handler() {
            private long lastReportTime = 0;

            @Override
            public void publish(LogRecord record) {
              if (record == null || record.getLevel() == null) return;
              if (Bukkit.isStopping() || !isEnabled()) return;
              if (ErrorReports.isPluginBug(record)) {
                long now = System.currentTimeMillis();
                if (now - lastReportTime > 3000) {
                  lastReportTime = now;
                  java.util.concurrent.CompletableFuture.runAsync(() -> {
                    try {
                      Thread.sleep(40);
                    } catch (InterruptedException ignored) {}

                    TextColor red = TextColor.fromHexString("#FF5555");
                    TextColor yellow = TextColor.fromHexString("#55FFFF");
                    TextColor gray = TextColor.fromHexString("#AAAAAA");
                    TextColor cyan = TextColor.fromHexString("#00FFFF");

                    Bukkit.getConsoleSender().sendMessage(Component.empty());
                    Bukkit.getConsoleSender().sendMessage(Component.text("   [RankedSMPX] ════════════════════════════════════════════════════════", red));
                    Bukkit.getConsoleSender().sendMessage(Component.text("   [RankedSMPX]  Oops! Looks like something went wrong :(", yellow));
                    Bukkit.getConsoleSender().sendMessage(Component.text("   [RankedSMPX]  Please join our Discord server and report this error:", gray));
                    Bukkit.getConsoleSender().sendMessage(Component.text("   [RankedSMPX]  → ", gray).append(Component.text("https://discord.gg/QGsJYht2vy", cyan)));
                    Bukkit.getConsoleSender().sendMessage(Component.text("   [RankedSMPX] ════════════════════════════════════════════════════════", red));
                    Bukkit.getConsoleSender().sendMessage(Component.empty());
                  });
                }
              }
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
          });
    } catch (Throwable ignored) {}
  }

  public static void sendErrorMessage(@NotNull org.bukkit.command.CommandSender sender) {
    if (Bukkit.isStopping()) {
      return;
    }
    if (sender instanceof org.bukkit.entity.Player) {
      sender.sendMessage("§c§l[RankedSMPX] §cOops! Looks like something went wrong :(");
      sender.sendMessage("§7Please join our Discord server and report this: §bhttps://discord.gg/QGsJYht2vy");
    } else {
      Bukkit.getConsoleSender()
          .sendMessage(
              Component.text(
                  "§c[RankedSMPX] Oops! Looks like something went wrong :( Please report at §bhttps://discord.gg/QGsJYht2vy"));
    }
  }

  private void checkForOldPlugin() {
    try {
      if (Bukkit.getPluginManager().getPlugin("RankedSMP") != null) {
        checkForOldPluginAdventure();
      }
    } catch (Exception e) {
      getLogger().warning("=".repeat(60));
      getLogger().warning("WARNING: Old RankedSMP plugin detected!");
      getLogger().warning("This may cause conflicts with RankedSMPX.");
      getLogger().warning("=".repeat(60));
    }
  }

  private void checkForOldPluginAdventure() {
    TextColor redColor = TextColor.fromHexString("#FF5555");
    TextColor yellowColor = TextColor.fromHexString("#55FFFF");
    TextColor grayColor = TextColor.fromHexString("#AAAAAA");

    String warningText = safeToSmallCaps("WARNING: OLD RANKEDSMP PLUGIN DETECTED!");
    String conflictText = safeToSmallCaps("This will cause conflicts with RankedSMPX.");
    String migrateText = safeToSmallCaps("To migrate your data:");
    String continueText =
        safeToSmallCaps("RankedSMPX will continue loading, but conflicts may occur!");

    Bukkit.getConsoleSender().sendMessage(Component.empty());
    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text(
                "╔════════════════════════════════════════════════════════════╗", redColor));
    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text("║ ", redColor)
                .append(Component.text(warningText + "!", yellowColor))
                .append(Component.text("              ║", redColor)));
    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text(
                "╚════════════════════════════════════════════════════════════╝", redColor));
    Bukkit.getConsoleSender().sendMessage(Component.empty());
    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text(
                safeToSmallCaps("The old RankedSMP plugin is still loaded!"), grayColor));
    Bukkit.getConsoleSender().sendMessage(Component.text(conflictText, grayColor));
    Bukkit.getConsoleSender().sendMessage(Component.empty());
    Bukkit.getConsoleSender().sendMessage(Component.text(migrateText, yellowColor));
    Bukkit.getConsoleSender()
        .sendMessage(Component.text("  1. " + safeToSmallCaps("Stop your server"), grayColor));
    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text(
                "  2. " + safeToSmallCaps("Remove/disable the old RankedSMP plugin"), grayColor));
    Bukkit.getConsoleSender()
        .sendMessage(Component.text("  3. " + safeToSmallCaps("Start your server"), grayColor));
    Bukkit.getConsoleSender()
        .sendMessage(
            Component.text("  4. " + safeToSmallCaps("Run: "), grayColor)
                .append(Component.text("/rsmp migrate", yellowColor)));
    Bukkit.getConsoleSender().sendMessage(Component.empty());
    Bukkit.getConsoleSender().sendMessage(Component.text(continueText, redColor));
    Bukkit.getConsoleSender().sendMessage(Component.empty());
  }

  private String safeToSmallCaps(String text) {
    try {
      if (messageManager != null) {
        return messageManager.toSmallCaps(text);
      }
    } catch (Exception ignored) {
    }
    return text;
  }

  private Component createGradient(String text, TextColor start, TextColor end) {
    if (text.isEmpty()) {
      return Component.text(text);
    }

    Component result = Component.empty();
    int length = text.length();

    for (int i = 0; i < length; i++) {
      float factor = (float) i / (length - 1);

      int r = (int) (start.red() + factor * (end.red() - start.red()));
      int g = (int) (start.green() + factor * (end.green() - start.green()));
      int b = (int) (start.blue() + factor * (end.blue() - start.blue()));

      TextColor color = TextColor.color(r, g, b);
      result = result.append(Component.text(text.charAt(i), color));
    }

    return result;
  }

  public boolean isPluginEnabled() {
    return pluginEnabled;
  }

  public void setPluginEnabled(boolean enabled) {
    this.pluginEnabled = enabled;
  }

  private void reapplyHealthToOnlinePlayers() {
    try {
      int appliedCount = 0;
      for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
        SchedulerUtil.runForEntity(
            this,
            player,
            () -> {
              try {
                healthManager.fixPlayerHealth(player);
              } catch (Exception e) {
                getLogger()
                    .warning("Failed to reapply health to " + player.getName() + ": " + e.getMessage());
              }
            });
        appliedCount++;
      }

      if (appliedCount > 0) {
        getLogger().info("Reapplied rank-based health to " + appliedCount + " online players");
      }
    } catch (Exception e) {
      getLogger().log(Level.WARNING, "Error during health reapplication to online players!", e);
    }
  }

  /** Tasks cannot be scheduled while the plugin is disabling, so run them directly then. */
  private void runNowOrScheduled(org.bukkit.entity.Player player, Runnable task) {
    if (isEnabled()) {
      SchedulerUtil.runForEntity(this, player, task);
    } else {
      task.run();
    }
  }

  public void resetPlayersToVanilla() {
    try {
      getLogger().info("Resetting online players to vanilla defaults...");

      int resetCount = 0;
      for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
        runNowOrScheduled(
            player,
            () -> {
              try {
                player.closeInventory();

                org.bukkit.attribute.AttributeInstance healthAttribute =
                    player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (healthAttribute != null) {
                  if (healthManager != null) {
                    healthManager.removeRankedSMPModifiers(healthAttribute);
                  }
                  healthAttribute.setBaseValue(20.0);
                  if (player.getHealth() > 20.0) {
                    player.setHealth(20.0);
                  }
                }

                for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
                  org.bukkit.potion.PotionEffectType type = effect.getType();
                  if (type != org.bukkit.potion.PotionEffectType.BAD_OMEN
                      && type != org.bukkit.potion.PotionEffectType.HERO_OF_THE_VILLAGE) {
                    player.removePotionEffect(type);
                  }
                }
              } catch (Exception e) {
                getLogger()
                    .warning(
                        "Failed to reset player " + player.getName() + " to vanilla: " + e.getMessage());
              }
            });
        resetCount++;
      }

      getLogger().info("Successfully scheduled reset for " + resetCount + " players to vanilla defaults");

    } catch (Exception e) {
      getLogger().log(Level.WARNING, "Error during player reset to vanilla!", e);
    }
  }

  public boolean isDebug() {
    return getConfig().getBoolean("debug", false);
  }
}
