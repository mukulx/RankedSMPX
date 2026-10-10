package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.CustomConfigManager;
import dev.mukulx.rankedsmpx.managers.HealthManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RankItemListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final CustomConfigManager customConfigManager;
  private final RankManager rankManager;
  private final HealthManager healthManager;

  private final java.util.Map<java.util.UUID, Long> claimCooldowns =
      new java.util.concurrent.ConcurrentHashMap<>();

  public RankItemListener(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull CustomConfigManager customConfigManager,
      @NotNull RankManager rankManager,
      @NotNull HealthManager healthManager) {
    this.plugin = plugin;
    this.customConfigManager = customConfigManager;
    this.rankManager = rankManager;
    this.healthManager = healthManager;
  }

  public long getRemainingCooldownSeconds(java.util.UUID uuid) {
    Long expiry = claimCooldowns.get(uuid);
    if (expiry == null) {
      return 0;
    }
    long remaining = expiry - System.currentTimeMillis();
    return remaining > 0 ? (long) Math.ceil(remaining / 1000.0) : 0;
  }

  public void setCooldown(java.util.UUID uuid, int seconds) {
    if (seconds > 0) {
      claimCooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    EquipmentSlot hand = event.getHand();
    if (hand == null) {
      hand = EquipmentSlot.HAND;
    }

    Player player = event.getPlayer();
    ItemStack mainHand = player.getInventory().getItemInMainHand();
    ItemStack offHand = player.getInventory().getItemInOffHand();

    // Prevent double firing on block clicks: if main hand is a rank item, ignore off-hand event
    if (customConfigManager.isRankItem(mainHand) && hand != EquipmentSlot.HAND) {
      return;
    }

    ItemStack item = (hand == EquipmentSlot.HAND) ? mainHand : offHand;
    if (!customConfigManager.isRankItem(item)) {
      if (customConfigManager.isRankItem(offHand)) {
        item = offHand;
        hand = EquipmentSlot.OFF_HAND;
      } else {
        return;
      }
    }

    event.setCancelled(true);

    if (!plugin.isPluginEnabled() || !plugin.getGameManager().isGameActive()) {
      return;
    }

    if (!customConfigManager.isRankDropEnabled() || !customConfigManager.isClaimEnabled()) {
      return;
    }

    if (plugin.getDatabaseManager().isBlacklisted(player.getUniqueId())) {
      plugin.getMessageManager().sendMessage(player, "general.blacklisted");
      return;
    }

    Integer targetRank = customConfigManager.getRankFromItem(item);
    if (targetRank == null || targetRank <= 0) {
      return;
    }

    Integer currentRank = rankManager.getRank(player.getUniqueId());

    if (currentRank != null && currentRank.equals(targetRank)) {
      String msg = customConfigManager.getMessage("already-rank", java.util.Map.of("rank", String.valueOf(targetRank)));
      if (!msg.isBlank()) {
        player.sendMessage(msg);
      }
      return;
    }

    int cooldownSec = customConfigManager.getClaimCooldownSeconds();
    long remaining = getRemainingCooldownSeconds(player.getUniqueId());
    if (remaining > 0) {
      String msg = customConfigManager.getMessage("cooldown", java.util.Map.of("seconds", String.valueOf(remaining)));
      if (!msg.isBlank()) {
        player.sendMessage(msg);
      }
      return;
    }

    if (currentRank == null) {
      consumeOneFromHand(player, hand);

      rankManager.setRank(player.getUniqueId(), targetRank);

      healthManager.applyHealth(player);

      if (cooldownSec > 0) {
        setCooldown(player.getUniqueId(), cooldownSec);
        try {
          player.setCooldown(item.getType(), cooldownSec * 20);
        } catch (Throwable ignored) {
        }
      }

      if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
        plugin.getTabIntegration().updatePlayer(player);
      }
      if (plugin.getVanillaTabManager() != null) {
        plugin.getVanillaTabManager().updatePlayer(player);
      }

      if (plugin.getHierarchyHammerManager() != null && targetRank == 1) {
        plugin.getHierarchyHammerManager().giveHammer(player);
      }

      customConfigManager.playActionSound(player, false);
      String msg = customConfigManager.getMessage("claim-success", java.util.Map.of("rank", String.valueOf(targetRank)));
      if (!msg.isBlank()) {
        player.sendMessage(msg);
      }

      plugin.getLogger().info(player.getName() + " claimed Rank #" + targetRank + " from physical rank item");
      return;
    }

    int oldRank = currentRank;
    ItemStack oldRankItem = customConfigManager.createRankItem(oldRank);

    swapOneInHand(player, hand, oldRankItem);

    rankManager.setRank(player.getUniqueId(), targetRank);

    healthManager.applyHealth(player);

    if (cooldownSec > 0) {
      setCooldown(player.getUniqueId(), cooldownSec);
      try {
        player.setCooldown(item.getType(), cooldownSec * 20);
      } catch (Throwable ignored) {
      }
    }

    if (plugin.getTabIntegration() != null && plugin.getTabIntegration().isHooked()) {
      plugin.getTabIntegration().updatePlayer(player);
    }
    if (plugin.getVanillaTabManager() != null) {
      plugin.getVanillaTabManager().updatePlayer(player);
    }

    if (plugin.getHierarchyHammerManager() != null) {
      plugin.getHierarchyHammerManager().updateHammerHolder();
    }

    customConfigManager.playActionSound(player, true);
    String msg = customConfigManager.getMessage("swap-success", java.util.Map.of(
        "old_rank", String.valueOf(oldRank),
        "new_rank", String.valueOf(targetRank),
        "rank", String.valueOf(targetRank)
    ));
    if (!msg.isBlank()) {
      player.sendMessage(msg);
    }

    plugin.getLogger().info(player.getName() + " swapped Rank #" + oldRank + " for Rank #" + targetRank + " using physical rank item");
  }

  private void consumeOneFromHand(Player player, EquipmentSlot hand) {
    ItemStack inHand = (hand == EquipmentSlot.OFF_HAND)
        ? player.getInventory().getItemInOffHand()
        : player.getInventory().getItemInMainHand();

    if (inHand.getAmount() <= 1) {
      if (hand == EquipmentSlot.OFF_HAND) {
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
      } else {
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
      }
    } else {
      inHand.setAmount(inHand.getAmount() - 1);
    }
  }

  private void swapOneInHand(Player player, EquipmentSlot hand, ItemStack replacement) {
    ItemStack inHand = (hand == EquipmentSlot.OFF_HAND)
        ? player.getInventory().getItemInOffHand()
        : player.getInventory().getItemInMainHand();

    if (inHand.getAmount() <= 1) {
      if (hand == EquipmentSlot.OFF_HAND) {
        player.getInventory().setItemInOffHand(replacement);
      } else {
        player.getInventory().setItemInMainHand(replacement);
      }
    } else {
      inHand.setAmount(inHand.getAmount() - 1);
      var leftovers = player.getInventory().addItem(replacement);
      if (!leftovers.isEmpty()) {
        for (ItemStack leftover : leftovers.values()) {
          player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPrepareCraft(PrepareItemCraftEvent event) {
    for (ItemStack item : event.getInventory().getMatrix()) {
      if (customConfigManager.isRankItem(item)) {
        event.getInventory().setResult(null);
        return;
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onCraft(CraftItemEvent event) {
    for (ItemStack item : event.getInventory().getMatrix()) {
      if (customConfigManager.isRankItem(item)) {
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player) {
          String msg = customConfigManager.getMessage("cannot-craft");
          if (!msg.isBlank()) {
            player.sendMessage(msg);
          }
        }
        return;
      }
    }
  }
}
