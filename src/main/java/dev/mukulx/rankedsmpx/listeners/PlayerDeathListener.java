package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.GameManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerDeathListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final GameManager gameManager;
  private final MessageManager messageManager;
  private final ConfigManager configManager;

  public PlayerDeathListener(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull GameManager gameManager,
      @NotNull MessageManager messageManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.gameManager = gameManager;
    this.messageManager = messageManager;
    this.configManager = configManager;
  }

  private boolean isStealOnCooldown(@NotNull Player killer) {
    if (!rankManager.isOnCooldown(killer.getUniqueId())) {
      return false;
    }
    long remaining = rankManager.getCooldownRemaining(killer.getUniqueId());
    messageManager.sendMessage(
        killer, "rank.steal.cooldown", Map.of("time", String.valueOf(Math.max(1, remaining))));
    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info(killer.getName() + " is on steal cooldown (" + remaining + "s)");
    }
    return true;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player victim = event.getEntity();
    Player killer = victim.getKiller();

    // Prevent Hierarchy Hammer item duplication on death: remove from drops & wipe from victim
    if (plugin.getHierarchyHammerManager() != null) {
      event.getDrops().removeIf(item -> plugin.getHierarchyHammerManager().isHierarchyHammer(item));
      plugin.getHierarchyHammerManager().removeHammer(victim);
    }

    if (killer != null && plugin.getHammerManager() != null) {
      var item = killer.getInventory().getItemInMainHand();
      if (plugin.getHammerManager().isHammer(item)) {
        spawnHammerDeathEffect(victim.getLocation());
      }
    }

    if (plugin.getInventoryManager() != null) {
      plugin.getInventoryManager().dropExtraInventory(victim);
      plugin.getInventoryManager().removeCombatTag(victim.getUniqueId());
      if (killer != null) {
        plugin.getInventoryManager().removeCombatTag(killer.getUniqueId());
      }
    }

    if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
      var updatesManager = plugin.getDiscordManager().getUpdatesManager();
      if (updatesManager != null) {
        net.kyori.adventure.text.Component deathMessage = event.deathMessage();
        if (deathMessage != null) {
          // Discord can't render Minecraft colour codes
          updatesManager.postPlayerDeath(
              victim.getName(), dev.mukulx.rankedsmpx.managers.MessageManager.plain(deathMessage));
        }
      }
    }

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(
              "Death event: "
                  + victim.getName()
                  + " killed by "
                  + (killer != null ? killer.getName() : "environment"));
    }

    if (plugin.isPluginEnabled()
        && configManager.getConfig().getBoolean("rank-display.show-on-death", true)) {
      net.kyori.adventure.text.Component deathMessage = event.deathMessage();
      if (deathMessage != null) {
        net.kyori.adventure.text.Component formattedMessage =
            addRankToDeathMessage(deathMessage, victim, killer);
        event.deathMessage(formattedMessage);
      }
    }

    if (killer == null) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("No killer - death by environment");
      }
      return;
    }

    if (configManager.getConfig().getBoolean("season.death-effects.lightning-on-kill", true)) {
      victim.getWorld().strikeLightningEffect(victim.getLocation());
    }

    boolean hammerKill = false;
    if (killer != null && plugin.getHierarchyHammerManager() != null) {
      var item = killer.getInventory().getItemInMainHand();
      if (plugin.getHierarchyHammerManager().isHierarchyHammer(item)) {
        customizeHammerDeathMessage(event, victim, killer);
        hammerKill = true;
      }
    }

    if (!plugin.isPluginEnabled()) {
      return;
    }

    if (!configManager.isRankStealingEnabled()) {
      return;
    }

    if (killer.hasPermission("rankedsmp.bypass.steal")) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info(killer.getName() + " has bypass permission - no rank stealing");
      }
      return;
    }

    Integer victimRank = rankManager.getRank(victim.getUniqueId());
    Integer killerRank = rankManager.getRank(killer.getUniqueId());

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(
              "Ranks - Killer: "
                  + (killerRank != null ? "#" + killerRank : "unranked")
                  + ", Victim: "
                  + (victimRank != null ? "#" + victimRank : "unranked"));
    }

    if (victimRank == null) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Victim has no rank - nothing to steal");
      }
      return;
    }

    if (plugin.getDatabaseManager().isBlacklisted(killer.getUniqueId())) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info(killer.getName() + " is blacklisted - cannot steal ranks");
      }
      return;
    }

    if (killerRank == null) {
      if (isStealOnCooldown(killer)) {
        return;
      }

      if (victimRank <= 0) {
        plugin
            .getLogger()
            .warning(
                "Prevented unranked player "
                    + killer.getName()
                    + " from stealing invalid rank "
                    + victimRank
                    + " from "
                    + victim.getName());
        return;
      }

      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                "Unranked player "
                    + killer.getName()
                    + " stealing rank #"
                    + victimRank
                    + " from "
                    + victim.getName());
      }
      rankManager.setRankWithHistory(
          killer.getUniqueId(), victimRank, "Killed " + victim.getName());
      rankManager.removeRankWithHistory(
          victim.getUniqueId(), victimRank, "Killed by " + killer.getName());

      SchedulerUtil.runForEntity(
          plugin,
          killer,
          () -> {
            if (!killer.isOnline()) return;
            plugin.getHealthManager().applyHealth(killer);
            if (plugin.getTabIntegration() != null) {
              plugin.getTabIntegration().updatePlayer(killer);
            }
            if (plugin.getVanillaTabManager() != null) {
              plugin.getVanillaTabManager().updatePlayer(killer);
            }
          });

      plugin.getInventoryManager().removeExtraInventory(killer.getUniqueId());
      plugin.getInventoryManager().removeExtraInventory(victim.getUniqueId());

      plugin.getHierarchyHammerManager().updateHammerHolder();

      if (plugin.getTabIntegration() != null) {
        plugin.getTabIntegration().updatePlayer(victim);
      }

      if (plugin.getVanillaTabManager() != null) {
        plugin.getVanillaTabManager().updatePlayer(victim);
      }

      if (plugin.getDiscordConfig().getBoolean("notifications.notify-on-death", true)) {
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
          var notificationManager = plugin.getDiscordManager().getNotificationManager();
          if (notificationManager != null) {
            notificationManager.sendRankGainedDM(
                killer.getUniqueId(),
                victimRank,
                "Killed " + victim.getName() + " and stole their rank");
            notificationManager.sendRankLostToUnrankedDM(
                victim.getUniqueId(), victimRank, "Killed by " + killer.getName());
          }

          var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
          if (roleSyncManager != null) {
            roleSyncManager.syncPlayerRole(killer.getUniqueId(), victimRank);
            roleSyncManager.syncPlayerRole(victim.getUniqueId(), null);
          }

          var updatesManager = plugin.getDiscordManager().getUpdatesManager();
          if (updatesManager != null) {
            updatesManager.postRankGained(
                killer.getName(), victimRank, "Killed " + victim.getName() + " (was unranked)");
            updatesManager.postRankLostToUnranked(
                victim.getName(), victimRank, "Killed by " + killer.getName());
          }

          plugin.getDiscordManager().forceUpdate();
        }
      }

      int cooldownSeconds = configManager.getConfig().getInt("season.rank-stealing.cooldown", 0);
      if (cooldownSeconds > 0) {
        rankManager.setCooldown(killer.getUniqueId(), cooldownSeconds);
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin.getLogger().info("Set " + cooldownSeconds + "s cooldown for " + killer.getName());
        }
      }

      Map<String, String> killerPlaceholders =
          Map.of("rank", String.valueOf(victimRank), "old_rank", "Unranked");
      messageManager.sendMessage(killer, "rank.steal.success", killerPlaceholders);

      Map<String, String> victimPlaceholders =
          Map.of("rank", "Unranked", "old_rank", String.valueOf(victimRank));
      messageManager.sendMessage(victim, "rank.steal.victim", victimPlaceholders);

      Map<String, String> broadcastPlaceholders =
          Map.of(
              "killer", killer.getName(),
              "victim", victim.getName(),
              "rank", String.valueOf(victimRank));
      String broadcast = messageManager.getMessage("rank.steal.broadcast", broadcastPlaceholders);
      Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

      Map<String, String> killerTitlePlaceholders = Map.of("rank", String.valueOf(victimRank));
      String killerTitle =
          messageManager.getMessage("rank.change.title-gained", killerTitlePlaceholders);
      String killerSubtitle = messageManager.getMessage("rank.change.subtitle-gained");
      messageManager.sendTitle(killer, killerTitle, killerSubtitle, 10, 70, 20);

      String victimTitle =
          messageManager.getMessage("rank.change.title-lost", Map.of("rank", "Unranked"));
      String victimSubtitle = messageManager.getMessage("rank.change.subtitle-lost");
      messageManager.sendTitle(victim, victimTitle, victimSubtitle, 10, 70, 20);

      return;
    }

    if (victimRank >= killerRank) {
      if (victimRank > killerRank && plugin.getCustomConfigManager() != null && plugin.getCustomConfigManager().isRankDropEnabled()) {
        handleLowerRankDrop(victim, killer, victimRank, killerRank);
      }
      return;
    }

    if (victimRank <= 0 || killerRank <= 0) {
      plugin
          .getLogger()
          .warning(
              "Prevented rank swap with invalid ranks: killer="
                  + killerRank
                  + ", victim="
                  + victimRank);
      return;
    }

    int maxDifference = configManager.getMaxRankDifference();
    if (maxDifference > 0 && (killerRank - victimRank) > maxDifference) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                "Rank difference too large: " + (killerRank - victimRank) + " > " + maxDifference);
      }
      Map<String, String> placeholders = Map.of("max", String.valueOf(maxDifference));
      messageManager.sendMessage(killer, "rank.steal.max-difference", placeholders);
      return;
    }

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(
              "Swapping ranks: "
                  + killer.getName()
                  + " (#"
                  + killerRank
                  + ") <-> "
                  + victim.getName()
                  + " (#"
                  + victimRank
                  + ")");
    }

    if (isStealOnCooldown(killer)) {
      return;
    }

    int oldKillerRank = killerRank;
    rankManager.swapRanksWithHistory(
        killer.getUniqueId(),
        victim.getUniqueId(),
        "Killed " + victim.getName(),
        "Killed by " + killer.getName());

    SchedulerUtil.runForEntity(
        plugin,
        killer,
        () -> {
          if (!killer.isOnline()) return;
          plugin.getHealthManager().applyHealth(killer);
          if (plugin.getTabIntegration() != null) {
            plugin.getTabIntegration().updatePlayer(killer);
          }
          if (plugin.getVanillaTabManager() != null) {
            plugin.getVanillaTabManager().updatePlayer(killer);
          }
        });

    plugin.getInventoryManager().removeExtraInventory(killer.getUniqueId());
    plugin.getInventoryManager().removeExtraInventory(victim.getUniqueId());

    plugin.getHierarchyHammerManager().updateHammerHolder();

    if (plugin.getTabIntegration() != null) {
      plugin.getTabIntegration().updatePlayer(victim);
    }

    if (plugin.getVanillaTabManager() != null) {
      plugin.getVanillaTabManager().updatePlayer(victim);
    }

    if (plugin.getDiscordConfig().getBoolean("notifications.notify-on-death", true)) {
      if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isEnabled()) {
        var notificationManager = plugin.getDiscordManager().getNotificationManager();
        if (notificationManager != null) {
          notificationManager.sendRankGainedDM(
              killer.getUniqueId(),
              victimRank,
              "Killed " + victim.getName() + " and stole their rank");

          notificationManager.sendRankLostDM(
              victim.getUniqueId(), victimRank, oldKillerRank, "Killed by " + killer.getName());
        }

        var roleSyncManager = plugin.getDiscordManager().getRoleSyncManager();
        if (roleSyncManager != null) {
          roleSyncManager.syncPlayerRole(killer.getUniqueId(), victimRank);
          roleSyncManager.syncPlayerRole(victim.getUniqueId(), oldKillerRank);
        }

        var updatesManager = plugin.getDiscordManager().getUpdatesManager();
        if (updatesManager != null) {
          updatesManager.postRankGained(killer.getName(), victimRank, "Killed " + victim.getName());
          updatesManager.postRankLost(
              victim.getName(), victimRank, oldKillerRank, "Killed by " + killer.getName());
        }

        plugin.getDiscordManager().forceUpdate();
      }
    }

    int cooldownSeconds = configManager.getConfig().getInt("season.rank-stealing.cooldown", 0);
    if (cooldownSeconds > 0) {
      rankManager.setCooldown(killer.getUniqueId(), cooldownSeconds);
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Set " + cooldownSeconds + "s cooldown for " + killer.getName());
      }
    }

    Map<String, String> killerPlaceholders =
        Map.of(
            "rank", String.valueOf(victimRank),
            "old_rank", String.valueOf(oldKillerRank));
    messageManager.sendMessage(killer, "rank.steal.success", killerPlaceholders);

    Map<String, String> victimPlaceholders =
        Map.of(
            "rank", String.valueOf(oldKillerRank),
            "old_rank", String.valueOf(victimRank));
    messageManager.sendMessage(victim, "rank.steal.victim", victimPlaceholders);

    Map<String, String> broadcastPlaceholders =
        Map.of(
            "killer", killer.getName(),
            "victim", victim.getName(),
            "rank", String.valueOf(victimRank));
    String broadcast = messageManager.getMessage("rank.steal.broadcast", broadcastPlaceholders);
    Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcast));

    Map<String, String> killerTitlePlaceholders = Map.of("rank", String.valueOf(victimRank));
    String killerTitle =
        messageManager.getMessage("rank.change.title-gained", killerTitlePlaceholders);
    String killerSubtitle = messageManager.getMessage("rank.change.subtitle-gained");
    messageManager.sendTitle(killer, killerTitle, killerSubtitle, 10, 70, 20);

    Map<String, String> victimTitlePlaceholders = Map.of("rank", String.valueOf(oldKillerRank));
    String victimTitle =
        messageManager.getMessage("rank.change.title-lost", victimTitlePlaceholders);
    String victimSubtitle = messageManager.getMessage("rank.change.subtitle-lost");
    messageManager.sendTitle(victim, victimTitle, victimSubtitle, 10, 70, 20);

  }

  private void spawnHammerDeathEffect(org.bukkit.Location location) {
    if (!configManager.getConfig().getBoolean("hierarchy-hammer.death-effect.enabled", true)) {
      return;
    }

    if (!configManager
        .getConfig()
        .getBoolean("hierarchy-hammer.death-effect.skull.enabled", true)) {
      return;
    }

    int duration =
        configManager.getConfig().getInt("hierarchy-hammer.death-effect.duration", 5)
            * 20; // Convert to ticks
    int density =
        configManager.getConfig().getInt("hierarchy-hammer.death-effect.skull.density", 20);
    double size =
        configManager.getConfig().getDouble("hierarchy-hammer.death-effect.skull.size", 1.0);

    var particleNames =
        configManager.getConfig().getStringList("hierarchy-hammer.death-effect.skull.particles");
    if (particleNames.isEmpty()) {
      particleNames = java.util.List.of("ASH", "WHITE_ASH", "SMOKE");
    }

    java.util.List<org.bukkit.Particle> particles = new java.util.ArrayList<>();
    for (String name : particleNames) {
      try {
        particles.add(org.bukkit.Particle.valueOf(name));
      } catch (IllegalArgumentException e) {
        plugin.getLogger().warning("Invalid particle type in death-effect config: " + name);
      }
    }

    if (particles.isEmpty()) {
      plugin.getLogger().warning("No valid particles configured for hammer death effect");
      return;
    }

    org.bukkit.Location skullCenter = location.clone().add(0, 1.5, 0);

    int[] ticks = {0};
    SchedulerUtil.runTimerAtLocation(
        plugin,
        skullCenter,
        task -> {
          if (ticks[0] >= duration) {
            task.cancel();
            return;
          }

          spawnSkullParticles(skullCenter, size, particles, Math.max(1, density / 20));
          ticks[0]++;
        },
        0L,
        1L);
  }

  private void spawnSkullParticles(
      org.bukkit.Location center,
      double size,
      java.util.List<org.bukkit.Particle> particles,
      int count) {
    org.bukkit.World world = center.getWorld();
    if (world == null) return;

    double[][] skullPoints = {
      {0, 0.5, 0},
      {-0.3, 0.4, 0},
      {0.3, 0.4, 0},
      {-0.4, 0.2, 0},
      {0.4, 0.2, 0},
      {-0.5, 0, 0},
      {0.5, 0, 0},
      {-0.5, -0.2, 0},
      {0.5, -0.2, 0},
      {-0.25, 0.1, 0.1},
      {0.25, 0.1, 0.1},
      {-0.25, 0, 0.1},
      {0.25, 0, 0.1},
      {0, -0.1, 0.1},
      {0, -0.2, 0.1},
      {-0.3, -0.4, 0},
      {0.3, -0.4, 0},
      {-0.2, -0.5, 0},
      {0.2, -0.5, 0},
      {0, -0.5, 0}
    };

    for (int i = 0; i < count; i++) {
      double[] point = skullPoints[(int) (Math.random() * skullPoints.length)];

      double x = point[0] * size + (Math.random() - 0.5) * 0.1;
      double y = point[1] * size + (Math.random() - 0.5) * 0.1;
      double z = point[2] * size + (Math.random() - 0.5) * 0.1;

      org.bukkit.Location particleLoc = center.clone().add(x, y, z);

      org.bukkit.Particle particle = particles.get((int) (Math.random() * particles.size()));

      world.spawnParticle(particle, particleLoc, 1, 0, 0, 0, 0);
    }
  }

  private net.kyori.adventure.text.Component addRankToDeathMessage(
      net.kyori.adventure.text.Component message, Player victim, Player killer) {
    String victimLegacyPrefix =
        messageManager.getRankPrefixLegacy(
            rankManager.getRank(victim.getUniqueId()),
            plugin.getDatabaseManager().isBlacklisted(victim.getUniqueId()));
    String killerLegacyPrefix =
        killer != null
            ? messageManager.getRankPrefixLegacy(
                rankManager.getRank(killer.getUniqueId()),
                plugin.getDatabaseManager().isBlacklisted(killer.getUniqueId()))
            : "";

    String plainMessage =
        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(message);

    String baseSentence = plainMessage;
    String weaponSuffix = "";

    int usingIdx = plainMessage.lastIndexOf(" using ");
    int withIdx = plainMessage.lastIndexOf(" with ");

    if (usingIdx != -1) {
      baseSentence = plainMessage.substring(0, usingIdx);
      weaponSuffix = plainMessage.substring(usingIdx);
    } else if (withIdx != -1) {
      baseSentence = plainMessage.substring(0, withIdx);
      weaponSuffix = plainMessage.substring(withIdx);
    }

    if (!weaponSuffix.isEmpty()) {
      weaponSuffix = weaponSuffix.replace("[", "").replace("]", "");
    }

    String victimName = victim.getName();
    String killerName = killer != null ? killer.getName() : "";

    baseSentence = replacePlayerNameWithLegacyPrefix(baseSentence, victimName, victimLegacyPrefix);
    if (killer != null && !killerName.isBlank()) {
      baseSentence = replacePlayerNameWithLegacyPrefix(baseSentence, killerName, killerLegacyPrefix);
    }

    String formattedLegacy = baseSentence + weaponSuffix;
    return messageManager.legacyToComponent(formattedLegacy);
  }

  private String replacePlayerNameWithLegacyPrefix(String text, String playerName, String prefix) {
    if (text == null || playerName == null || playerName.isBlank() || prefix == null || prefix.isBlank()) {
      return text;
    }

    String regex = "(?i)(?<!\\[#\\d{1,3}\\]\\s)(?<!#\\d{1,3}\\s)(?<!\\[UNRANKED\\]\\s)(?<!\\[Blacklisted\\]\\s)\\b"
        + java.util.regex.Pattern.quote(playerName) + "\\b";

    return text.replaceAll(regex, java.util.regex.Matcher.quoteReplacement(prefix + playerName + "&r"));
  }

  private net.kyori.adventure.text.Component getRankPrefixComponent(Player player) {
    if (player == null) return net.kyori.adventure.text.Component.empty();
    Integer rank = rankManager.getRank(player.getUniqueId());
    boolean isBlacklisted = plugin.getDatabaseManager().isBlacklisted(player.getUniqueId());
    return messageManager.legacyToComponent(messageManager.getRankPrefixLegacy(rank, isBlacklisted));
  }

  private void customizeHammerDeathMessage(PlayerDeathEvent event, Player victim, Player killer) {
    String hammerName =
        plugin
            .getConfig()
            .getString(
                "hierarchy-hammer.name",
                "§x§f§2§b§c§4§5§lʜ§x§f§2§b§f§4§b§lɪ§x§f§2§c§1§5§1§lᴇ§x§f§2§c§4§5§6§lʀ§x§f§2§c§6§5§c§lᴀ§x§f§2§c§9§6§2§lʀ§x§f§2§c§c§6§8§lᴄ§x§f§2§c§b§6§b§lʜ§x§f§2§d§1§7§3§lʏ §x§f§2§d§6§7§f§lʜ§x§f§2§d§9§8§5§lᴀ§x§f§2§d§b§8§b§lᴍ§x§f§2§d§b§9§0§lᴍ§x§f§2§b§0§9§6§lᴇ§x§f§2§b§3§9§c§lʀ");

    net.kyori.adventure.text.Component victimPrefix = getRankPrefixComponent(victim);
    net.kyori.adventure.text.Component killerPrefix = getRankPrefixComponent(killer);
    net.kyori.adventure.text.Component hammerComp = messageManager.legacyToComponent(hammerName);

    net.kyori.adventure.text.Component deathMsg =
        victimPrefix
            .append(net.kyori.adventure.text.Component.text(victim.getName(), net.kyori.adventure.text.format.NamedTextColor.AQUA))
            .append(net.kyori.adventure.text.Component.text(" was smashed by ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
            .append(killerPrefix)
            .append(net.kyori.adventure.text.Component.text(killer.getName(), net.kyori.adventure.text.format.NamedTextColor.AQUA))
            .append(net.kyori.adventure.text.Component.text(" with ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
            .append(hammerComp);

    event.deathMessage(deathMsg);
  }

  private void handleLowerRankDrop(Player victim, Player killer, int victimRank, int killerRank) {
    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Higher rank " + killer.getName() + " (#" + killerRank
          + ") killed lower rank " + victim.getName() + " (#" + victimRank + ") -> dropping physical rank item");
    }

    rankManager.removeRankWithHistory(
        victim.getUniqueId(), victimRank, "Killed by " + killer.getName() + " (rank dropped as item)");

    plugin.getInventoryManager().removeExtraInventory(victim.getUniqueId());

    org.bukkit.inventory.ItemStack rankItem = plugin.getCustomConfigManager().createRankItem(victimRank);
    victim.getWorld().dropItemNaturally(victim.getLocation(), rankItem);

    if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
      plugin.getTabIntegration().updatePlayer(victim);
    }
    if (plugin.getVanillaTabManager() != null) {
      plugin.getVanillaTabManager().updatePlayer(victim);
    }

    String victimMsg = plugin.getCustomConfigManager().getMessage("drop-victim", Map.of(
        "killer", killer.getName(),
        "victim", victim.getName(),
        "rank", String.valueOf(victimRank)
    ));
    if (!victimMsg.isBlank()) {
      victim.sendMessage(victimMsg);
    }

    String killerMsg = plugin.getCustomConfigManager().getMessage("drop-killer", Map.of(
        "killer", killer.getName(),
        "victim", victim.getName(),
        "rank", String.valueOf(victimRank)
    ));
    if (!killerMsg.isBlank()) {
      killer.sendMessage(killerMsg);
    }

    if (plugin.getCustomConfigManager().isBroadcastOnDrop()) {
      String broadcastMsg = plugin.getCustomConfigManager().getMessage("drop-broadcast", Map.of(
          "killer", killer.getName(),
          "victim", victim.getName(),
          "rank", String.valueOf(victimRank)
      ));
      if (!broadcastMsg.isBlank()) {
        Bukkit.broadcast(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(broadcastMsg));
      }
    }

    String victimTitle = messageManager.getMessage("rank.change.title-lost", Map.of("rank", "Unranked"));
    String victimSubtitle = messageManager.getMessage("rank.change.subtitle-lost");
    messageManager.sendTitle(victim, victimTitle, victimSubtitle, 10, 70, 20);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onHammerPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
    if (plugin.getHierarchyHammerManager() == null) {
      return;
    }
    ItemStack item = event.getItem().getItemStack();
    if (!plugin.getHierarchyHammerManager().isHierarchyHammer(item)) {
      return;
    }

    if (!(event.getEntity() instanceof Player player)) {
      event.getItem().remove();
      event.setCancelled(true);
      return;
    }

    // Only authorized Rank 1 without an existing hammer can pick it up
    if (!plugin.getHierarchyHammerManager().shouldHaveHammer(player.getUniqueId())
        || plugin.getHierarchyHammerManager().hasHammer(player)) {
      event.getItem().remove();
      event.setCancelled(true);
    }
  }
}
