package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.GameManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RankedSMPCommand implements CommandExecutor, TabCompleter {

  private final RankedSMPXPlugin plugin;
  private final GameManager gameManager;
  private final RankManager rankManager;
  private final MessageManager messageManager;
  private final ConfigManager configManager;
  private final Map<String, Long> stopConfirmations;
  private final MigrateCommand migrateCommand;
  private final OriginalStyleCommand originalStyleCommand;
  private final ModernStyleCommand modernStyleCommand;
  private static final long CONFIRMATION_TIMEOUT = 30000; // 30 seconds

  public RankedSMPCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull GameManager gameManager,
      @NotNull RankManager rankManager,
      @NotNull MessageManager messageManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.gameManager = gameManager;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
    this.configManager = configManager;
    this.stopConfirmations = new HashMap<>();
    this.migrateCommand = new MigrateCommand(plugin);
    this.originalStyleCommand = new OriginalStyleCommand(plugin, messageManager);
    this.modernStyleCommand = new ModernStyleCommand(plugin, messageManager);

    SchedulerUtil.runTimerAsync(plugin, this::cleanupExpiredConfirmations, 6000L, 6000L);
  }

  private void cleanupExpiredConfirmations() {
    long now = System.currentTimeMillis();
    stopConfirmations.entrySet().removeIf(entry -> now - entry.getValue() > CONFIRMATION_TIMEOUT);
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    try {
      if (args.length == 0) {
        sendHelp(sender);
        return true;
      }

      String subCommand = args[0].toLowerCase();

      if (!plugin.isPluginEnabled()) {
        if (subCommand.equals("enable")) {
          handleEnable(sender);
          return true;
        }

        sender.sendMessage(messageManager.getPrefix() + "§cPlugin is currently disabled!");
        sender.sendMessage(
            messageManager.getPrefix() + "§7An admin can re-enable it using: §b/rsmp enable");
        return true;
      }

      switch (subCommand) {
        case "help" -> sendHelp(sender);
        case "start" -> handleStart(sender);
        case "stop" -> handleStop(sender);
        case "reload" -> handleReload(sender);
        case "migrate" -> migrateCommand.execute(sender);
        case "migrate-confirm" -> {
          if (args.length >= 2) {
            migrateCommand.confirmMigration(sender, args[1]);
          }
        }
        case "test" -> handleTest(sender, args);
        case "menu", "gui", "manage", "admin", "panel" -> handleGUI(sender);
        case "rank" -> handleRankSubcommand(sender, args);
        case "original", "og" -> handleOriginalStyle(sender);
        case "modern" -> handleModernStyle(sender);
        case "setup-dismiss" -> handleSetupDismiss(sender);
        case "enable" -> handleEnable(sender);
        case "disable" -> handleDisable(sender);
        case "set" -> {
          if (args.length < 3) {
            sender.sendMessage(
                messageManager.getPrefix()
                    + "§cUsage: /rsmp set <player> <rank|unranked|blacklisted>");
            return true;
          }
          String[] setArgs = new String[args.length];
          setArgs[0] = "set";
          System.arraycopy(args, 1, setArgs, 1, args.length - 1);
          handleSet(sender, setArgs);
        }
        case "give" -> handleGive(sender, args);
        case "hammer", "mace" -> handleHammer(sender, args);
        case "spear" -> handleSpear(sender);
        case "info" -> handleInfo(sender, args);
        case "discord" -> handleDiscord(sender, args);
        case "fix" -> new RankFixCommand(
            plugin, plugin.getDatabaseManager(), rankManager, messageManager)
            .onCommand(sender, command, "fix", Arrays.copyOfRange(args, 1, args.length));
        default -> sendHelp(sender);
      }

      return true;
    } catch (Throwable t) {
      plugin.getLogger().log(java.util.logging.Level.SEVERE, "Unexpected error executing /" + label + " command: " + t.getMessage(), t);
      RankedSMPXPlugin.sendErrorMessage(sender);
      return true;
    }
  }

  private void sendHelp(@NotNull CommandSender sender) {
    sender.sendMessage(messageManager.getMessage("commands.help.header"));

    if (sender.hasPermission("rankedsmp.admin")) {
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp start §8- §7Start the season"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp stop §8- §7Stop the season"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp reload §8- §7Reload config files"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp fix [player] §8- §7Fix player hearts and rank issues"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp migrate §8- §7Import data from old RankedSMP plugin"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp manage §8- §7Open rank management GUI"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp rank set <player> <rank> §8- §7Set player rank"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp rank remove <player> §8- §7Remove a rank"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp give mace <player> §8- §7Give Hierarchy Hammer"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp give spear <player> §8- §7Give Diamond Spear (1.21.11+)"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp hammer §8- §7Get Hierarchy Hammer"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp hammer settings §8- §7Open Hammer settings"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp spear §8- §7Get Diamond Spear (1.21.11+)"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp enable §8- §7Enable the plugin"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp disable §8- §7Disable the plugin"));
      sender.sendMessage(messageManager.applySmallCaps("§b/rsmp discord <enable|disable|update|status> §8- §7Manage Discord integration"));
    }

    sender.sendMessage(messageManager.applySmallCaps("§b/rsmp info [player] §8- §7View rank information"));
    sender.sendMessage(messageManager.applySmallCaps("§b/rsmp help §8- §7Show this help menu"));
    sender.sendMessage(messageManager.getMessage("commands.help.footer"));
  }

  private void handleStart(@NotNull CommandSender sender) {
    if (!sender.hasPermission("rankedsmp.start")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (!rankManager.getAllRanks().isEmpty()) {
      int existingRanks = rankManager.getAllRanks().size();
      sender.sendMessage(
          messageManager.getPrefix()
              + "§cCannot start game - §b"
              + existingRanks
              + " §cranks already exist!");
      sender.sendMessage(
          messageManager.getPrefix()
              + "§7The game is already active. Use §b/rsmp stop §7first to clear all ranks.");
      return;
    }

    if (gameManager.isGameActive()) {
      messageManager.sendMessage(sender, "commands.start.already-started");
      return;
    }

    if (gameManager.startGame()) {
      int count = rankManager.getAllRanks().size();
      Map<String, String> placeholders = Map.of("count", String.valueOf(count));
      messageManager.sendMessage(sender, "commands.start.success", placeholders);
    }
  }

  private void handleStop(@NotNull CommandSender sender) {
    if (!sender.hasPermission("rankedsmp.stop")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (rankManager.getAllRanks().isEmpty()) {
      messageManager.sendMessage(sender, "commands.stop.not-started");
      return;
    }

    String senderName = sender.getName();
    Long confirmTime = stopConfirmations.get(senderName);

    if (confirmTime != null && System.currentTimeMillis() - confirmTime < CONFIRMATION_TIMEOUT) {
      stopConfirmations.remove(senderName);

      if (gameManager.stopGame()) {
        messageManager.sendMessage(sender, "commands.stop.success");
      }
    } else {
      stopConfirmations.put(senderName, System.currentTimeMillis());

      int playerCount = rankManager.getAllRanks().size();
      Map<String, String> placeholders = Map.of("count", String.valueOf(playerCount), "time", "30");
      messageManager.sendMessage(sender, "commands.stop.confirm", placeholders);
      messageManager.sendMessage(sender, "commands.stop.confirm-line2", placeholders);

      SchedulerUtil.runLater(
          plugin,
          () -> {
            Long time = stopConfirmations.get(senderName);
            if (time != null && time.equals(confirmTime)) {
              stopConfirmations.remove(senderName);
              messageManager.sendMessage(sender, "commands.stop.confirm-expired");
            }
          },
          600L); // 30 seconds = 600 ticks
    }
  }

  private void handleReload(@NotNull CommandSender sender) {
    if (!sender.hasPermission("rankedsmp.reload")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    plugin.reload();

    if (plugin.getTabIntegration() != null) {
      plugin.getTabIntegration().reload();
      sender.sendMessage(messageManager.getPrefix() + "§aReloaded TAB integration");
    }

    messageManager.sendMessage(sender, "general.reload-success");
  }

  private void handleOriginalStyle(@NotNull CommandSender sender) {
    originalStyleCommand.onCommand(sender, null, "original", new String[0]);
  }

  private void handleModernStyle(@NotNull CommandSender sender) {
    modernStyleCommand.onCommand(sender, null, "modern", new String[0]);
  }

  private void handleSetupDismiss(@NotNull CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage("§cThis command can only be used by players.");
      return;
    }

    if (plugin.getFirstTimeSetupManager() != null) {
      plugin.getFirstTimeSetupManager().dismiss(player);
    }
  }

  private void handleEnable(@NotNull CommandSender sender) {
    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (plugin.isPluginEnabled()) {
      sender.sendMessage(messageManager.getPrefix() + "§cPlugin is already enabled!");
      return;
    }

    plugin.setPluginEnabled(true);

    int appliedCount = 0;
    for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            try {
              if (plugin.getRankManager().hasRank(player.getUniqueId())) {
                plugin.getHealthManager().applyHealth(player);
              }

              if (plugin.getTabIntegration() != null) {
                plugin.getTabIntegration().updatePlayer(player);
              }

              if (plugin.getVanillaTabManager() != null) {
                plugin.getVanillaTabManager().updatePlayer(player);
              }
            } catch (Exception e) {
              plugin
                  .getLogger()
                  .warning("Failed to reapply health to " + player.getName() + ": " + e.getMessage());
            }
          });
      appliedCount++;
    }

    sender.sendMessage(messageManager.getPrefix() + "§aPlugin enabled successfully!");
    sender.sendMessage(messageManager.getPrefix() + "§7All features are now active.");
    sender.sendMessage(
        messageManager.getPrefix()
            + "§7Reapplied rank buffs to §b"
            + appliedCount
            + " §7online players.");
  }

  private void handleDisable(@NotNull CommandSender sender) {
    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (!plugin.isPluginEnabled()) {
      sender.sendMessage(messageManager.getPrefix() + "§cPlugin is already disabled!");
      return;
    }

    plugin.resetPlayersToVanilla();

    if (plugin.getTabIntegration() != null) {
      for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
        SchedulerUtil.runForEntity(plugin, player, () -> plugin.getTabIntegration().clearPlayer(player));
      }
      sender.sendMessage(
          messageManager.getPrefix() + "§7Cleared TAB plugin prefixes for all players.");
    }

    if (plugin.getVanillaTabManager() != null) {
      for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
        SchedulerUtil.runForEntity(plugin, player, () -> plugin.getVanillaTabManager().clearPlayer(player));
      }
      sender.sendMessage(
          messageManager.getPrefix() + "§7Cleared vanilla tab prefixes for all players.");
    }

    plugin.setPluginEnabled(false);
    sender.sendMessage(messageManager.getPrefix() + "§cPlugin disabled successfully!");
    sender.sendMessage(
        messageManager.getPrefix() + "§7All online players have been reset to vanilla defaults.");
    sender.sendMessage(
        messageManager.getPrefix() + "§7Rank prefixes removed from tab and messages.");
    sender.sendMessage(messageManager.getPrefix() + "§7Use §b/rsmp enable §7to re-enable.");
  }

  private void handleTest(@NotNull CommandSender sender, @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    sender.sendMessage(messageManager.applySmallCaps("§6§l=== RankedSMP Multiplier Test ==="));
    sender.sendMessage(" ");

    for (int rank = 1; rank <= configManager.getTotalRanks(); rank++) {
      double potionMult = plugin.getPotionManager().calculateMultiplier(rank);
      double xpMult = plugin.getXPManager().calculateMultiplier(rank);
      double hearts = plugin.getHealthManager().calculateHearts(rank);

      sender.sendMessage(
          messageManager.applySmallCaps(
              String.format(
                  "§bRank %d: §a%.1f hearts §7| §b%.2fx potion §7| §d%.2fx XP",
                  rank, hearts, potionMult, xpMult)));
    }

    sender.sendMessage(" ");
    sender.sendMessage(messageManager.applySmallCaps("§7Check console for detailed logs"));
  }

  private void handleGUI(@NotNull CommandSender sender) {
    if (!sender.hasPermission("rankedsmp.gui") && !sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (!(sender instanceof Player player)) {
      sender.sendMessage("§cThis command can only be used by players!");
      return;
    }

    if (plugin.getDialogManager() != null && plugin.getDialogManager().isAvailable()) {
      boolean opened = plugin.getDialogManager().openMainMenu(player);
      if (!opened) {
        openManagementGUI(player);
      }
    } else {
      openManagementGUI(player);
    }
  }

  private void openManagementGUI(@NotNull Player player) {
    String title = "§6§lRᴀɴᴋ §b§lMᴀɴᴀɢᴇᴍᴇɴᴛ";
    Inventory gui = Bukkit.createInventory(null, 54, dev.mukulx.rankedsmpx.managers.MessageManager.legacy(title));

    List<Map.Entry<UUID, Integer>> rankedPlayers = rankManager.getTopRanks(54);

    int slot = 0;
    for (Map.Entry<UUID, Integer> entry : rankedPlayers) {
      if (slot >= 45) break; // Leave bottom row for controls

      UUID uuid = entry.getKey();
      int rank = entry.getValue();
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
      String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      if (meta != null) {
        meta.setOwningPlayer(offlinePlayer);

        String displayName =
            messageManager.applySmallCaps(getRankColor(rank) + "§l#" + rank + " §r§f" + playerName);
        meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(displayName));

        double hearts = plugin.getHealthManager().calculateHearts(rank);
        double potionMultiplier = plugin.getPotionManager().calculateMultiplier(rank);
        double xpMultiplier = plugin.getXPManager().calculateMultiplier(rank);
        int extraSlots = plugin.getConfigManager().getExtraSlots(rank);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(
            messageManager.applySmallCaps(
                "§7Rank: §6#" + rank + " §7/ §b" + plugin.getConfigManager().getTotalRanks()));
        lore.add("");
        lore.add(messageManager.applySmallCaps("§b§lBuffs & Perks:"));
        lore.add(messageManager.applySmallCaps("  §cHearts: §f" + String.format("%.1f", hearts)));
        lore.add(
            messageManager.applySmallCaps(
                "  §dPotion Duration: §f" + String.format("%.2fx", potionMultiplier)));
        lore.add(
            messageManager.applySmallCaps(
                "  §aXP Multiplier: §f" + String.format("%.2fx", xpMultiplier)));

        if (extraSlots > 0) {
          lore.add(
              messageManager.applySmallCaps("  §bExtra Inventory: §f" + extraSlots + " slots"));
        }

        lore.add("");
        if (offlinePlayer.isOnline()) {
          lore.add(messageManager.applySmallCaps("§a● Online"));
        } else {
          lore.add(messageManager.applySmallCaps("§7● Offline"));
        }

        lore.add("");
        lore.add(messageManager.applySmallCaps("§b§lClick Actions:"));
        lore.add(messageManager.applySmallCaps("§7Left Click: §aPromote (+1 rank)"));
        lore.add(messageManager.applySmallCaps("§7Right Click: §cDemote (-1 rank)"));
        lore.add(messageManager.applySmallCaps("§7Shift + Left: §6Set to Rank #1"));
        lore.add(messageManager.applySmallCaps("§7Shift + Right: §cRemove Rank"));

        meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(lore));
        head.setItemMeta(meta);
      }

      gui.setItem(slot, head);
      slot++;
    }

    ItemStack refresh = new ItemStack(Material.LIME_DYE);
    ItemMeta refreshMeta = refresh.getItemMeta();
    if (refreshMeta != null) {
      refreshMeta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(messageManager.applySmallCaps("§a§lRefresh")));
      List<String> refreshLore = new ArrayList<>();
      refreshLore.add("");
      refreshLore.add(messageManager.applySmallCaps("§7Click to refresh the GUI"));
      refreshMeta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(refreshLore));
      refresh.setItemMeta(refreshMeta);
    }
    gui.setItem(49, refresh);

    ItemStack close = new ItemStack(Material.BARRIER);
    ItemMeta closeMeta = close.getItemMeta();
    if (closeMeta != null) {
      closeMeta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(messageManager.applySmallCaps("§c§lClose")));
      List<String> closeLore = new ArrayList<>();
      closeLore.add("");
      closeLore.add(messageManager.applySmallCaps("§7Click to close this menu"));
      closeMeta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(closeLore));
      close.setItemMeta(closeMeta);
    }
    gui.setItem(45, close);

    ItemStack info = new ItemStack(Material.BOOK);
    ItemMeta infoMeta = info.getItemMeta();
    if (infoMeta != null) {
      infoMeta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(messageManager.applySmallCaps("§b§lManagement Guide")));
      List<String> infoLore = new ArrayList<>();
      infoLore.add("");
      infoLore.add(messageManager.applySmallCaps("§7This GUI allows you to manage"));
      infoLore.add(messageManager.applySmallCaps("§7player ranks quickly."));
      infoLore.add("");
      infoLore.add(messageManager.applySmallCaps("§b§lActions:"));
      infoLore.add(messageManager.applySmallCaps("§7• Left Click: Promote (+1)"));
      infoLore.add(messageManager.applySmallCaps("§7• Right Click: Demote (-1)"));
      infoLore.add(messageManager.applySmallCaps("§7• Shift + Left: Set to #1"));
      infoLore.add(messageManager.applySmallCaps("§7• Shift + Right: Remove rank"));
      infoLore.add("");
      infoLore.add(messageManager.applySmallCaps("§7Total Ranked: §b" + rankedPlayers.size()));
      infoMeta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(infoLore));
      info.setItemMeta(infoMeta);
    }
    gui.setItem(53, info);

    ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta fillerMeta = filler.getItemMeta();
    if (fillerMeta != null) {
      fillerMeta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(" "));
      filler.setItemMeta(fillerMeta);
    }

    for (int i = 45; i < 54; i++) {
      if (gui.getItem(i) == null) {
        gui.setItem(i, filler);
      }
    }

    player.openInventory(gui);
  }

  private void handleRankSubcommand(@NotNull CommandSender sender, @NotNull String[] args) {
    if (args.length < 2) {
      sender.sendMessage(
          messageManager.getPrefix() + "§cUsage: /rsmp rank <set|remove> <player> [rank]");
      return;
    }

    switch (args[1].toLowerCase()) {
      case "set" -> {
        if (args.length < 4) {
          sender.sendMessage(
              messageManager.getPrefix()
                  + "§cUsage: /rsmp rank set <player> <rank|unranked|blacklisted>");
          return;
        }
        String[] setArgs = new String[args.length - 1];
        setArgs[0] = "set";
        System.arraycopy(args, 2, setArgs, 1, args.length - 2);
        handleSet(sender, setArgs);
      }
      case "remove" -> {
        if (args.length < 3) {
          sender.sendMessage(messageManager.getPrefix() + "§cUsage: /rsmp rank remove <player>");
          return;
        }
        String[] removeArgs = new String[] {"remove", args[2]};
        handleRemove(sender, removeArgs);
      }
      default ->
          sender.sendMessage(
              messageManager.getPrefix() + "§cUsage: /rsmp rank <set|remove> <player> [rank]");
    }
  }

  private void handleGive(@NotNull CommandSender sender, @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (args.length < 2) {
      sender.sendMessage(
          messageManager.getPrefix() + "§cUsage: /rsmp give <mace|hammer|spear> <player>");
      return;
    }

    String itemType = args[1].toLowerCase();
    if (!itemType.equals("mace") && !itemType.equals("hammer") && !itemType.equals("spear")) {
      sender.sendMessage(
          messageManager.getPrefix() + "§cUsage: /rsmp give <mace|hammer|spear> <player>");
      return;
    }

    if (args.length < 3) {
      sender.sendMessage(
          messageManager.getPrefix() + "§cUsage: /rsmp give " + args[1] + " <player>");
      return;
    }

    Player target = Bukkit.getPlayer(args[2]);
    if (target == null) {
      Map<String, String> placeholders = Map.of("player", args[2]);
      messageManager.sendMessage(sender, "general.player-not-found", placeholders);
      return;
    }

    if (itemType.equals("spear")) {
      if (!plugin.getSpearManager().isSpearsSupported()) {
        sender.sendMessage(
            messageManager.getPrefix()
                + messageManager.applySmallCaps(
                    "§cSpears are not supported on this server version (requires 1.21.11+)"));
        return;
      }

      boolean success = plugin.getSpearManager().giveSpear(target);
      if (success) {
        sender.sendMessage(
            messageManager.getPrefix()
                + messageManager.applySmallCaps(
                    "§aGave §bDiamond Spear §awith §dLunge III §ato §b"
                        + target.getName()
                        + "§a!"));
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin.getLogger().info(sender.getName() + " gave Diamond Spear to " + target.getName());
        }
      } else {
        sender.sendMessage(
            messageManager.getPrefix() + messageManager.applySmallCaps("§cFailed to give spear!"));
      }
      return;
    }

    if (!plugin.getHierarchyHammerManager().isEnabled()) {
      sender.sendMessage(
          messageManager.getPrefix()
              + messageManager.applySmallCaps("§cHierarchy Hammer is disabled in config!"));
      return;
    }

    plugin.getHierarchyHammerManager().adminGiveHammer(target);
    sender.sendMessage(
        messageManager.getPrefix()
            + messageManager.applySmallCaps(
                "§aGave §6§lHierarchy Hammer §ato §b" + target.getName() + "§a!"));
    target.sendMessage(
        messageManager.getPrefix()
            + messageManager.applySmallCaps(
                "§aYou have been given the §6§lHierarchy Hammer §aby §b"
                    + sender.getName()
                    + "§a!"));
    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info(sender.getName() + " gave Hierarchy Hammer to " + target.getName());
    }
  }

  private void handleSet(@NotNull CommandSender sender, @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.set")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (args.length < 3) {
      sender.sendMessage(
          messageManager.getPrefix() + "§cUsage: /rsmp set <player> <rank|unranked|blacklisted>");
      return;
    }

    Player target = Bukkit.getPlayer(args[1]);
    java.util.UUID targetUUID = null;
    String targetName = args[1];
    boolean isOnline = false;

    if (target != null) {
      targetUUID = target.getUniqueId();
      targetName = target.getName();
      isOnline = true;
    } else {
      org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
      if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
        targetUUID = offlinePlayer.getUniqueId();
        targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : args[1];
      } else {
        Map<String, String> placeholders = Map.of("player", args[1]);
        messageManager.sendMessage(sender, "general.player-not-found", placeholders);
        return;
      }
    }

    final UUID finalTargetUUID = targetUUID;
    final String finalTargetName = targetName;

    if (args[2].equalsIgnoreCase("blacklisted")) {
      Integer oldRank = rankManager.getRank(targetUUID);
      if (oldRank != null) {
        rankManager.removeRank(targetUUID);

        if (isOnline && target != null) {
          SchedulerUtil.runForEntity(
              plugin,
              target,
              () -> {
                if (!target.isOnline()) return;
                plugin.getHealthManager().resetHealth(target);
                target.sendMessage(
                    messageManager.getPrefix()
                        + "§cYou have been blacklisted and cannot receive ranks anymore.");
                String removeTitle = messageManager.colorize("&c&lBLACKLISTED ");
                String removeSubtitle = messageManager.colorize("&7You cannot receive ranks");
                messageManager.sendTitle(target, removeTitle, removeSubtitle, 10, 70, 20);
              });
          plugin.getInventoryManager().removeExtraInventory(targetUUID);
        }

        plugin.getHierarchyHammerManager().updateHammerHolder();

        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
          var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
          if (roleSyncManager != null) {
            roleSyncManager.syncPlayerRole(targetUUID, null);
          }
        }

        Map<String, String> removeBroadcastPlaceholders =
            Map.of(
                "player",
                targetName,
                "rank",
                "0",
                "old_rank",
                String.valueOf(oldRank),
                "reason",
                "Blacklisted by admin");
        String removeBroadcast =
            messageManager.getMessage("rank.change.broadcast-lost", removeBroadcastPlaceholders);
        Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(removeBroadcast));

        if (plugin.getDiscordConfig().getBoolean("notifications.notify-on-admin-change", true)) {
          if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
            var notificationManager = plugin.getDiscordManager().getNotificationManager();
            if (notificationManager != null) {
              notificationManager.sendUnrankedDM(targetUUID, oldRank, "Blacklisted by admin");
            }

            var updatesManager = plugin.getDiscordManager().getUpdatesManager();
            if (updatesManager != null) {
              updatesManager.postRankRemoved(targetName, oldRank, "Blacklisted by admin");
            }
          }
        }
      }

      plugin.getDatabaseManager().addToBlacklist(targetUUID, targetName, sender.getName());

      sender.sendMessage(
          messageManager.getPrefix()
              + "§cBlacklisted §b"
              + targetName
              + (oldRank != null ? " §7(removed rank #" + oldRank + ")" : "")
              + (isOnline ? "" : " §7[OFFLINE]"));
      return;
    }

    if (args[2].equalsIgnoreCase("unranked")) {
      Integer oldRank = rankManager.getRank(targetUUID);
      rankManager.removeRank(targetUUID);

      if (isOnline && target != null) {
        SchedulerUtil.runForEntity(
            plugin,
            target,
            () -> {
              if (!target.isOnline()) return;
              plugin.getHealthManager().resetHealth(target);
              target.sendMessage(
                  messageManager.getPrefix() + "§cYour rank has been removed. You are now unranked.");
              String removeTitle = messageManager.colorize("&c&lUNRANKED ");
              String removeSubtitle = messageManager.getMessage("rank.change.subtitle-lost");
              messageManager.sendTitle(target, removeTitle, removeSubtitle, 10, 70, 20);
            });
        plugin.getInventoryManager().removeExtraInventory(targetUUID);
      }

      plugin.getHierarchyHammerManager().updateHammerHolder();

      if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
        var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
        if (roleSyncManager != null) {
          roleSyncManager.syncPlayerRole(targetUUID, null);
        }
      }

      sender.sendMessage(
          messageManager.getPrefix()
              + "§aRemoved rank from §b"
              + targetName
              + " §7(now unranked)"
              + (isOnline ? "" : " §7[OFFLINE]"));

      Map<String, String> removeBroadcastPlaceholders =
          Map.of(
              "player",
              targetName,
              "rank",
              "0",
              "old_rank",
              String.valueOf(oldRank),
              "reason",
              "Removed by admin");
      String removeBroadcast =
          messageManager.getMessage("rank.change.broadcast-lost", removeBroadcastPlaceholders);
      Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(removeBroadcast));

      if (oldRank != null
          && plugin.getDiscordConfig().getBoolean("notifications.notify-on-admin-change", true)) {
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
          var notificationManager = plugin.getDiscordManager().getNotificationManager();
          if (notificationManager != null) {
            notificationManager.sendUnrankedDM(targetUUID, oldRank, "Removed by admin");
          }

          var updatesManager = plugin.getDiscordManager().getUpdatesManager();
          if (updatesManager != null) {
            updatesManager.postRankRemoved(targetName, oldRank, "Removed by admin");
          }
        }
      }
      return;
    }

    int rank;
    try {
      rank = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      sender.sendMessage(
          messageManager.getPrefix()
              + "§cInvalid rank! Use a number (1-"
              + configManager.getTotalRanks()
              + "), 'unranked', or 'blacklisted'");
      return;
    }

    if (rank < 1 || rank > configManager.getTotalRanks()) {
      Map<String, String> placeholders =
          Map.of("max", String.valueOf(configManager.getTotalRanks()));
      messageManager.sendMessage(sender, "general.invalid-rank", placeholders);
      return;
    }

    if (plugin.getDatabaseManager().isBlacklisted(targetUUID)) {
      sender.sendMessage(
          messageManager.getPrefix()
              + "§cCannot set rank for §b"
              + targetName
              + "§c! They are blacklisted. Use §b/rankblacklist remove "
              + targetName
              + " §cfirst.");
      return;
    }

    UUID currentHolder = null;
    for (var entry : rankManager.getAllRanks().entrySet()) {
      if (entry.getValue() == rank) {
        currentHolder = entry.getKey();
        break;
      }
    }

    if (currentHolder != null && !currentHolder.equals(targetUUID)) {
      OfflinePlayer oldHolder = Bukkit.getOfflinePlayer(currentHolder);
      rankManager.removeRank(currentHolder);

      sender.sendMessage(
          messageManager.getPrefix()
              + "§7Removed rank #"
              + rank
              + " from §b"
              + oldHolder.getName()
              + " §7(was holding it)");

      Player oldHolderOnline = oldHolder.getPlayer();
      if (oldHolderOnline != null && oldHolderOnline.isOnline()) {
        final UUID oldHolderUUID = currentHolder;
        SchedulerUtil.runForEntity(
            plugin,
            oldHolderOnline,
            () -> {
              if (!oldHolderOnline.isOnline()) return;
              plugin.getHealthManager().resetHealth(oldHolderOnline);
              plugin.getInventoryManager().removeExtraInventory(oldHolderUUID);

              if (plugin.getTabIntegration() != null) {
                plugin.getTabIntegration().updatePlayer(oldHolderOnline);
              }
              if (plugin.getVanillaTabManager() != null) {
                plugin.getVanillaTabManager().updatePlayer(oldHolderOnline);
              }

              oldHolderOnline.sendMessage(
                  messageManager.getPrefix()
                      + "§cYour rank #"
                      + rank
                      + " was taken by an admin and given to §b"
                      + finalTargetName);
            });
      }

      plugin
          .getLogger()
          .info("Removed rank #" + rank + " from " + oldHolder.getName() + " (admin reassignment)");
    }

    rankManager.setRank(finalTargetUUID, rank);

    if (!gameManager.isGameActive()) {
      gameManager.setGameActive(true);
      plugin.getLogger().info("Game activated via manual rank assignment");
    }

    if (isOnline && target != null) {
      SchedulerUtil.runForEntity(
          plugin,
          target,
          () -> {
            if (!target.isOnline()) return;
            plugin.getHealthManager().applyHealth(target);
            plugin.getInventoryManager().removeExtraInventory(finalTargetUUID);

            if (plugin.getTabIntegration() != null) {
              plugin.getTabIntegration().updatePlayer(target);
            }

            if (plugin.getVanillaTabManager() != null) {
              plugin.getVanillaTabManager().updatePlayer(target);
            }
          });
    }

    plugin.getHierarchyHammerManager().updateHammerHolder();

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
      if (roleSyncManager != null) {
        roleSyncManager.syncPlayerRole(targetUUID, rank);
      }
    }

    Map<String, String> placeholders = Map.of("player", targetName, "rank", String.valueOf(rank));
    messageManager.sendMessage(sender, "commands.set.success", placeholders);
    sender.sendMessage(
        messageManager.getPrefix()
            + (isOnline ? "" : "§7[OFFLINE] ")
            + "§aSet §b"
            + targetName
            + "§a's rank to §6#"
            + rank);

    if (isOnline) {
      messageManager.sendMessage(
          target, "commands.set.target-notify", Map.of("rank", String.valueOf(rank)));

      Map<String, String> titlePlaceholders = Map.of("rank", String.valueOf(rank));
      String title = messageManager.getMessage("rank.change.title-gained", titlePlaceholders);
      String subtitle = messageManager.getMessage("rank.change.subtitle-gained");
      messageManager.sendTitle(target, title, subtitle, 10, 70, 20);
    }

    Map<String, String> broadcastPlaceholders =
        Map.of(
            "player", target.getName(),
            "rank", String.valueOf(rank),
            "reason", "Set by admin");
    String broadcast =
        messageManager.getMessage("rank.change.broadcast-gained", broadcastPlaceholders);
    Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

    if (plugin.getDiscordConfig().getBoolean("notifications.notify-on-admin-change", true)) {
      if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
        var notificationManager = plugin.getDiscordManager().getNotificationManager();
        if (notificationManager != null) {
          notificationManager.sendRankGainedDM(target.getUniqueId(), rank, "Set by admin");
        }

        var updatesManager = plugin.getDiscordManager().getUpdatesManager();
        if (updatesManager != null) {
          updatesManager.postRankGained(target.getName(), rank, "Set by admin");
        }
      }
    }
  }

  private void handleRemove(@NotNull CommandSender sender, @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.remove")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (args.length < 2) {
      messageManager.sendMessage(sender, "commands.remove.usage");
      return;
    }

    Player target = Bukkit.getPlayer(args[1]);
    java.util.UUID targetUUID = null;
    String targetName = args[1];
    boolean isOnline = false;

    if (target != null) {
      targetUUID = target.getUniqueId();
      targetName = target.getName();
      isOnline = true;
    } else {
      org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
      if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
        targetUUID = offlinePlayer.getUniqueId();
        targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : args[1];
      } else {
        Map<String, String> placeholders = Map.of("player", args[1]);
        messageManager.sendMessage(sender, "general.player-not-found", placeholders);
        return;
      }
    }

    final UUID finalTargetUUID = targetUUID;

    if (!rankManager.hasRank(finalTargetUUID)) {
      Map<String, String> placeholders = Map.of("player", targetName);
      messageManager.sendMessage(sender, "commands.remove.no-rank", placeholders);
      return;
    }

    Integer oldRank = rankManager.getRank(finalTargetUUID);

    rankManager.removeRank(finalTargetUUID);

    if (isOnline && target != null) {
      SchedulerUtil.runForEntity(
          plugin,
          target,
          () -> {
            if (!target.isOnline()) return;
            plugin.getHealthManager().resetHealth(target);
            plugin.getInventoryManager().removeExtraInventory(finalTargetUUID);

            if (plugin.getTabIntegration() != null) {
              plugin.getTabIntegration().updatePlayer(target);
            }
            if (plugin.getVanillaTabManager() != null) {
              plugin.getVanillaTabManager().updatePlayer(target);
            }

            messageManager.sendMessage(target, "commands.remove.target-notify");

            String removeTitle = messageManager.colorize("&c&lUNRANKED ");
            String removeSubtitle = messageManager.getMessage("rank.change.subtitle-lost");
            messageManager.sendTitle(target, removeTitle, removeSubtitle, 10, 70, 20);
          });
    }

    plugin.getHierarchyHammerManager().updateHammerHolder();

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
      if (roleSyncManager != null) {
        roleSyncManager.syncPlayerRole(targetUUID, null);
      }
    }

    Map<String, String> placeholders = Map.of("player", targetName);
    messageManager.sendMessage(sender, "commands.remove.success", placeholders);
    sender.sendMessage(
        messageManager.getPrefix()
            + (isOnline ? "" : "§7[OFFLINE] ")
            + "§aRemoved rank from §b"
            + targetName);

    if (oldRank != null) {
      Map<String, String> broadcastPlaceholders =
          Map.of(
              "player",
              targetName,
              "rank",
              "0",
              "old_rank",
              String.valueOf(oldRank),
              "reason",
              "Removed by admin");
      String broadcast =
          messageManager.getMessage("rank.change.broadcast-lost", broadcastPlaceholders);
      Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

      if (plugin.getDiscordConfig().getBoolean("notifications.notify-on-admin-change", true)) {
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
          var notificationManager = plugin.getDiscordManager().getNotificationManager();
          if (notificationManager != null) {
            notificationManager.sendUnrankedDM(target.getUniqueId(), oldRank, "Removed by admin");
          }

          var updatesManager = plugin.getDiscordManager().getUpdatesManager();
          if (updatesManager != null) {
            updatesManager.postRankRemoved(target.getName(), oldRank, "Removed by admin");
          }
        }
      }
    }
  }

  private void handleInfo(@NotNull CommandSender sender, @NotNull String[] args) {
    if (sender instanceof Player player) {
      player.performCommand("rankinfo" + (args.length > 1 ? " " + args[1] : ""));
    }
  }

  private void handleDiscord(@NotNull CommandSender sender, @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (args.length < 2) {
      sender.sendMessage(messageManager.getPrefix() + "§bDiscord Commands:");
      sender.sendMessage(
          messageManager.applySmallCaps("§7/rsmp discord enable §8- §7Enable Discord integration"));
      sender.sendMessage(
          messageManager.applySmallCaps(
              "§7/rsmp discord disable §8- §7Disable Discord integration"));
      sender.sendMessage(
          messageManager.applySmallCaps(
              "§7/rsmp discord update §8- §7Force update leaderboard now"));
      sender.sendMessage(
          messageManager.applySmallCaps(
              "§7/rsmp give spear <player> §8- §7Give Diamond Spear (1.21.11+)"));
    }

    switch (args[1].toLowerCase()) {
      case "enable" -> {
        plugin.setDiscordValue("enabled", true);

        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isEnabled()) {
          SchedulerUtil.runAsync(plugin, () -> {
            var discordManager = new dev.mukulx.rankedsmpx.discord.DiscordManager(plugin);
            if (discordManager.initialize()) {
              sender.sendMessage(
                  messageManager.getPrefix() + "§aDiscord integration enabled!");
            } else {
              sender.sendMessage(
                  messageManager.getPrefix()
                      + "§cFailed to enable Discord integration. Check console for errors.");
            }
          });
        } else {
          sender.sendMessage(
              messageManager.getPrefix() + "§aDiscord integration is already enabled!");
        }
      }
      case "disable" -> {
        plugin.setDiscordValue("enabled", false);

        if (plugin.getDiscordManager() != null) {
          plugin.getDiscordManager().shutdown();
        }

        sender.sendMessage(messageManager.getPrefix() + "§cDiscord integration disabled!");
      }
      case "update" -> {
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
          plugin.getDiscordManager().forceUpdate();
          sender.sendMessage(messageManager.getPrefix() + "§aForcing leaderboard update...");
        } else {
          sender.sendMessage(messageManager.getPrefix() + "§cDiscord integration is not enabled!");
        }
      }
      case "status" -> {
        boolean enabled = plugin.getDiscordConfig().getBoolean("enabled", false);
        boolean discordReady =
            plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled();
        String channelId = plugin.getDiscordConfig().getString("leaderboard.channel-id", "");
        int linkedAccounts =
            plugin.getDiscordManager() != null
                ? plugin.getDiscordManager().getLinkManager().getTotalLinked()
                : 0;

        sender.sendMessage(messageManager.getPrefix() + "§bDiscord Integration Status:");
        sender.sendMessage(
            messageManager.applySmallCaps("§7Enabled in config: " + (enabled ? "§a" : "§c")));
        sender.sendMessage(
            messageManager.applySmallCaps("§7Discord ready: " + (discordReady ? "§a" : "§c")));
        sender.sendMessage(messageManager.applySmallCaps("§7Linked accounts: §b" + linkedAccounts));
        sender.sendMessage(
            messageManager.applySmallCaps(
                "§7Leaderboard channel: "
                    + (channelId.isEmpty() ? "§cNot set" : "§a" + channelId)));
        sender.sendMessage(
            messageManager.applySmallCaps(
                "§7Update interval: §b"
                    + plugin.getDiscordConfig().getInt("leaderboard.update-interval", 300)
                    + "s"));
      }
      default -> sender.sendMessage(messageManager.getPrefix() + "§cUnknown discord subcommand!");
    }
  }

  private void handleHammer(@NotNull CommandSender sender, @NotNull String[] args) {
    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          messageManager.getPrefix()
              + messageManager.applySmallCaps("§cOnly players can use this command!"));
      return;
    }

    if (args.length >= 2 && args[1].equalsIgnoreCase("settings")) {
      if (!plugin.getDialogManager().openHammerSettings(player)) {
        sender.sendMessage(
            messageManager.getPrefix()
                + "§cThe Paper Dialog API is unavailable on this server. Hammer settings were not changed.");
      }
      return;
    }

    if (!plugin.getHierarchyHammerManager().isEnabled()) {
      sender.sendMessage(
          messageManager.getPrefix()
              + messageManager.applySmallCaps("§cHierarchy Hammer is disabled in config!"));
      return;
    }

    plugin.getHierarchyHammerManager().adminGiveHammer(player);
    sender.sendMessage(
        messageManager.getPrefix()
            + messageManager.applySmallCaps("§aYou have been given the §6§lHierarchy Hammer§a!"));
    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info(player.getName() + " used /rsmp hammer to get the Hierarchy Hammer");
    }
  }

  private void handleSpear(@NotNull CommandSender sender) {
    if (!sender.hasPermission("rankedsmp.admin")) {
      messageManager.sendMessage(sender, "general.no-permission");
      return;
    }

    if (!(sender instanceof Player player)) {
      sender.sendMessage(
          messageManager.getPrefix()
              + messageManager.applySmallCaps("§cOnly players can use this command!"));
      return;
    }

    if (!plugin.getSpearManager().isSpearsSupported()) {
      sender.sendMessage(
          messageManager.getPrefix()
              + messageManager.applySmallCaps(
                  "§cSpears are not supported on this server version (requires 1.21.11+)"));
      return;
    }

    boolean success = plugin.getSpearManager().giveSpear(player);
    if (success) {
      sender.sendMessage(
          messageManager.getPrefix()
              + messageManager.applySmallCaps(
                  "§aYou have been given a §bDiamond Spear §awith §dLunge III§a!"));
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info(player.getName() + " used /rsmp spear to get a Diamond Spear");
      }
    } else {
      sender.sendMessage(
          messageManager.getPrefix() + messageManager.applySmallCaps("§cFailed to give spear!"));
    }
  }

  @Nullable
  @Override
  public List<String> onTabComplete(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      String input = args[0].toLowerCase();

      if (input.isEmpty() || "help".startsWith(input)) {
        completions.add("help");
      }

      if (input.isEmpty() || "info".startsWith(input)) {
        completions.add("info");
      }

      if (sender.hasPermission("rankedsmp.admin")) {
        List<String> adminSubcommands =
            Arrays.asList(
                "menu",
                "start",
                "stop",
                "reload",
                "enable",
                "disable",
                "migrate",
                "manage",
                "rank",
                "set",
                "give",
                "hammer",
                "mace",
                "spear",
                "discord",
                "original",
                "og",
                "modern");
        for (String sub : adminSubcommands) {
          if (input.isEmpty() || sub.startsWith(input)) {
            completions.add(sub);
          }
        }
      }
    } else if (args.length == 2) {
      String input = args[1].toLowerCase();

      if (args[0].equalsIgnoreCase("rank") && sender.hasPermission("rankedsmp.admin")) {
        List<String> rankSubs = Arrays.asList("set", "remove");
        for (String sub : rankSubs) {
          if (input.isEmpty() || sub.startsWith(input)) {
            completions.add(sub);
          }
        }
      } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("rankedsmp.set")) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (input.isEmpty() || player.getName().toLowerCase().startsWith(input)) {
            completions.add(player.getName());
          }
        }
      } else if (args[0].equalsIgnoreCase("give") && sender.hasPermission("rankedsmp.admin")) {
        List<String> giveSubs = Arrays.asList("mace", "hammer", "spear");
        for (String sub : giveSubs) {
          if (input.isEmpty() || sub.startsWith(input)) {
            completions.add(sub);
          }
        }
      } else if ((args[0].equalsIgnoreCase("hammer") || args[0].equalsIgnoreCase("mace"))
          && sender.hasPermission("rankedsmp.admin")) {
        if (input.isEmpty() || "settings".startsWith(input)) {
          completions.add("settings");
        }
      } else if (args[0].equalsIgnoreCase("info")) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (input.isEmpty() || player.getName().toLowerCase().startsWith(input)) {
            completions.add(player.getName());
          }
        }
      } else if (args[0].equalsIgnoreCase("discord") && sender.hasPermission("rankedsmp.admin")) {
        List<String> discordSubs = Arrays.asList("enable", "disable", "update", "status");
        for (String sub : discordSubs) {
          if (input.isEmpty() || sub.startsWith(input)) {
            completions.add(sub);
          }
        }
      }
    } else if (args.length == 3) {
      String input = args[2].toLowerCase();

      if (args[0].equalsIgnoreCase("rank")
          && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("remove"))
          && sender.hasPermission("rankedsmp.admin")) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (input.isEmpty() || player.getName().toLowerCase().startsWith(input)) {
            completions.add(player.getName());
          }
        }
      } else if (args[0].equalsIgnoreCase("set") && sender.hasPermission("rankedsmp.set")) {
        if (input.isEmpty() || "unranked".startsWith(input)) {
          completions.add("unranked");
        }
        if (input.isEmpty() || "blacklisted".startsWith(input)) {
          completions.add("blacklisted");
        }
        for (int i = 1; i <= configManager.getTotalRanks(); i++) {
          String rank = String.valueOf(i);
          if (input.isEmpty() || rank.startsWith(input)) {
            completions.add(rank);
          }
        }
      } else if (args[0].equalsIgnoreCase("give")
          && (args[1].equalsIgnoreCase("mace")
              || args[1].equalsIgnoreCase("hammer")
              || args[1].equalsIgnoreCase("spear"))
          && sender.hasPermission("rankedsmp.admin")) {
        for (Player player : Bukkit.getOnlinePlayers()) {
          if (input.isEmpty() || player.getName().toLowerCase().startsWith(input)) {
            completions.add(player.getName());
          }
        }
      }
    } else if (args.length == 4
        && args[0].equalsIgnoreCase("rank")
        && args[1].equalsIgnoreCase("set")
        && sender.hasPermission("rankedsmp.admin")) {
      String input = args[3].toLowerCase();

      if (input.isEmpty() || "unranked".startsWith(input)) {
        completions.add("unranked");
      }
      if (input.isEmpty() || "blacklisted".startsWith(input)) {
        completions.add("blacklisted");
      }
      for (int i = 1; i <= configManager.getTotalRanks(); i++) {
        String rank = String.valueOf(i);
        if (input.isEmpty() || rank.startsWith(input)) {
          completions.add(rank);
        }
      }
    }

    return completions;
  }

  private String getRankColor(int rank) {
    if (rank == 1) return "§6"; // Gold
    if (rank == 2) return "§f"; // White (Silver)
    if (rank == 3) return "§6"; // Gold (Bronze - using gold as closest to bronze)
    if (rank <= 10) return "§b"; // Cyan
    return "§7"; // Gray
  }
}
