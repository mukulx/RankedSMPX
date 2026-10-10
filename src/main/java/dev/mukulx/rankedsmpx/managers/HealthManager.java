package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HealthManager {

  private static final NamespacedKey HEALTH_MODIFIER_KEY =
      new NamespacedKey("rankedsmpx", "health_bonus");

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final ConfigManager configManager;

  private Object aggressiveTask = null;

  public HealthManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.configManager = configManager;

    startAggressiveModeTask();
  }

  private void startAggressiveModeTask() {
    String mode = configManager.getConfig().getString("health.mode", "MODIFIER").toUpperCase();
    if (!"AGGRESSIVE".equals(mode)) {
      return;
    }

    int recheckInterval = configManager.getConfig().getInt("health.aggressive.recheck-interval", 5);
    long recheckTicks = recheckInterval * 20L; // Convert seconds to ticks

    plugin.getLogger().warning("AGGRESSIVE health mode enabled! This will fight other plugins for health control.");
    plugin.getLogger().warning("Rechecking player health every " + recheckInterval + " seconds.");

    aggressiveTask = dev.mukulx.rankedsmpx.utils.SchedulerUtil.runTimer(
        plugin,
        () -> {
          for (org.bukkit.entity.Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            dev.mukulx.rankedsmpx.utils.SchedulerUtil.runForEntity(
                plugin, player, () -> reapplyHealthIfNeeded(player));
          }
        },
        recheckTicks,
        recheckTicks
    );
  }

  private void reapplyHealthIfNeeded(@NotNull Player player) {
    Integer rank = rankManager.getRank(player.getUniqueId());
    double expectedHearts =
        (rank != null)
            ? calculateHearts(rank)
            : configManager.getRanks().unranked().hearts();
    double expectedHP = expectedHearts * 2.0;

    AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
    if (attribute == null || Math.abs(attribute.getValue() - expectedHP) <= 0.5) {
      return;
    }

    if (configManager.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .warning(
              "AGGRESSIVE: Detected wrong health for "
                  + player.getName()
                  + " (expected: "
                  + expectedHP
                  + " HP, current: "
                  + attribute.getValue()
                  + " HP). Forcing fix...");
    }
    fixPlayerHealth(player);
  }

  public void cancelAggressiveTask() {
    if (aggressiveTask != null) {
      dev.mukulx.rankedsmpx.utils.SchedulerUtil.cancelTask(aggressiveTask);
      aggressiveTask = null;
    }
  }

  public void applyHealth(@NotNull Player player) {
    Integer rank = rankManager.getRank(player.getUniqueId());
    double hearts = (rank != null)
        ? calculateHearts(rank)
        : configManager.getRanks().unranked().hearts();

    if (configManager.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(
              "Applying health to "
                  + player.getName()
                  + " ("
                  + (rank != null ? "Rank " + rank : "Unranked")
                  + "): "
                  + hearts
                  + " hearts");
    }
    setMaxHealth(player, hearts, true);
  }

  public double calculateHearts(int rank) {
    return configManager.getRanks().row(rank).hearts();
  }

  /**
   * @param healToFull true for a real rank change (heal to new max); false for a resync (join,
   *     reload, aggressive recheck) where current health must be kept and only clamped
   */
  private void setMaxHealth(@NotNull Player player, double hearts, boolean healToFull) {
    final double requestedHearts = hearts;
    if (!dev.mukulx.rankedsmpx.utils.SchedulerUtil.isOwnedOrReschedule(
        plugin, player, () -> setMaxHealth(player, requestedHearts, healToFull))) {
      return;
    }
    AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
    if (attribute == null) {
      plugin
          .getLogger()
          .severe(
              "Failed to get MAX_HEALTH attribute for "
                  + player.getName()
                  + "! This should never happen.");
      return;
    }

    // Config values are in hearts, Minecraft API uses health points (2 HP = 1 heart)
    double healthPoints = hearts * 2.0;

    if (healthPoints < 1.0) {
      plugin
          .getLogger()
          .warning(
              "Health points too low for "
                  + player.getName()
                  + ": "
                  + healthPoints
                  + ". Setting to minimum (1.0)");
      healthPoints = 1.0;
      hearts = 0.5;
    } else if (healthPoints > 2048.0) {
      plugin
          .getLogger()
          .warning(
              "Health points too high for "
                  + player.getName()
                  + ": "
                  + healthPoints
                  + ". Setting to maximum (2048.0)");
      healthPoints = 2048.0;
      hearts = 1024.0;
    }

    String mode = configManager.getConfig().getString("health.mode", "MODIFIER").toUpperCase();

    if ("MODIFIER".equals(mode)) {
      applyHealthModifier(player, attribute, hearts);
    } else if ("AGGRESSIVE".equals(mode)) {
      applyHealthAggressive(player, attribute, hearts);
    } else {
      removeRankedSMPModifiers(attribute);
      attribute.setBaseValue(healthPoints);
    }

    // Never heal a player who is mid-death: setHealth would revive them.
    if (player.isDead()) {
      if (configManager.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Skipped healing dead player " + player.getName());
      }
      return;
    }

    double maxHealth = attribute.getValue();
    try {
      if (player.getHealth() > maxHealth) {
        player.setHealth(maxHealth);
      } else if (player.getHealth() <= 0.0) {
        player.setHealth(Math.min(2.0, maxHealth));
      } else if (healToFull) {
        player.setHealth(maxHealth);
      }
    } catch (Throwable e) {
      plugin.getLogger().warning("Failed to adjust health for " + player.getName() + ": " + e.getMessage());
    }

    if (configManager.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(
              "Set "
                  + player.getName()
                  + " max health to "
                  + hearts
                  + " hearts ("
                  + healthPoints
                  + " HP) using mode: "
                  + mode);
    }
  }

  public void removeRankedSMPModifiers(@NotNull AttributeInstance attribute) {
    java.util.List<AttributeModifier> toRemove = new java.util.ArrayList<>();
    for (AttributeModifier mod : new java.util.ArrayList<>(attribute.getModifiers())) {
      boolean match = false;
      try {
        NamespacedKey key = mod.getKey();
        if (key != null) {
          String ns = key.getNamespace().toLowerCase();
          String k = key.getKey().toLowerCase();
          if (ns.equals("rankedsmpx") || ns.equals("rankedsmp") || k.contains("health_bonus")) {
            match = true;
          }
        }
      } catch (Throwable ignored) {
      }

      if (!match) {
        String name = mod.getName();
        if (name != null) {
          String lowerName = name.toLowerCase();
          if (lowerName.contains("health_bonus") || lowerName.contains("rankedsmpx") || lowerName.contains("rankedsmp")) {
            match = true;
          }
        }
      }

      if (match) {
        toRemove.add(mod);
      }
    }
    for (AttributeModifier mod : toRemove) {
      try {
        attribute.removeModifier(mod);
      } catch (Throwable ignored) {
      }
    }
  }

  /**
   * Thoroughly fix and resynchronize a player's max health to the exact expected value,
   * cleaning up any stacked modifiers, corrupted base values, or health glitches. Current health
   * is kept (only clamped to the new max), so rejoining or reloading never heals.
   *
   * @param player The player to fix
   * @return The resulting hearts
   */
  public double fixPlayerHealth(@NotNull Player player) {
    Integer rank = rankManager.getRank(player.getUniqueId());
    double targetHearts = (rank != null)
        ? calculateHearts(rank)
        : configManager.getRanks().unranked().hearts();

    setMaxHealth(player, targetHearts, false);
    return targetHearts;
  }

  private void applyHealthModifier(
      @NotNull Player player, @NotNull AttributeInstance attribute, double hearts) {
    // Inspect and ensure base value is vanilla 20 HP (10 hearts) to heal any corrupted NBT
    if (attribute.getBaseValue() != 20.0) {
      attribute.setBaseValue(20.0);
    }

    removeRankedSMPModifiers(attribute);

    double bonusHearts = hearts - 10.0;

    if (Math.abs(bonusHearts) > 0.01) {
      AttributeModifier modifier =
          new AttributeModifier(
              HEALTH_MODIFIER_KEY,
              bonusHearts * 2.0, // Convert hearts to HP
              AttributeModifier.Operation.ADD_NUMBER);

      attribute.addModifier(modifier);

      if (configManager.getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                "Added health modifier for "
                    + player.getName()
                    + ": "
                    + bonusHearts
                    + " bonus hearts (total: "
                    + hearts
                    + " hearts)");
      }
    }
  }

  private void applyHealthAggressive(
      @NotNull Player player, @NotNull AttributeInstance attribute, double hearts) {
    boolean removeOtherModifiers =
        configManager.getConfig().getBoolean("health.aggressive.remove-other-modifiers", true);

    if (removeOtherModifiers) {
      int removedCount = 0;
      for (AttributeModifier modifier : new java.util.ArrayList<>(attribute.getModifiers())) {
        attribute.removeModifier(modifier);
        removedCount++;

        if (configManager.getConfig().getBoolean("debug", false)) {
          plugin.getLogger().warning(
              "AGGRESSIVE: Removed modifier '" + modifier.getName()
              + "' (key: " + modifier.getKey() + ") from " + player.getName());
        }
      }

      if (removedCount > 0 && configManager.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().warning(
            "AGGRESSIVE: Removed " + removedCount + " health modifier(s) from " + player.getName());
      }
    } else {
      removeRankedSMPModifiers(attribute);
    }

    if (attribute.getBaseValue() != 20.0) {
      attribute.setBaseValue(20.0);
    }

    double bonusHearts = hearts - 10.0;
    if (Math.abs(bonusHearts) > 0.01) {
      AttributeModifier modifier =
          new AttributeModifier(
              HEALTH_MODIFIER_KEY,
              bonusHearts * 2.0,
              AttributeModifier.Operation.ADD_NUMBER);
      attribute.addModifier(modifier);
    }
  }

  public void resetHealth(@NotNull Player player) {
    double unrankedHearts = configManager.getRanks().unranked().hearts();
    setMaxHealth(player, unrankedHearts, true);
  }
}
