package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.HammerManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HammerListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final HammerManager hammerManager;
  private final Map<UUID, Long> lastSwingTime = new ConcurrentHashMap<>();
  private final Map<UUID, Boolean> swingHit = new ConcurrentHashMap<>();

  public HammerListener(@NotNull RankedSMPXPlugin plugin, @NotNull HammerManager hammerManager) {
    this.plugin = plugin;
    this.hammerManager = hammerManager;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();

    if (!plugin.isPluginEnabled() || !plugin.getHierarchyHammerManager().isEnabled()) {
      return;
    }

    if (event.getAction() == Action.RIGHT_CLICK_AIR
        || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
      if (item == null || !hammerManager.isHammer(item)) {
        return;
      }

      event.setCancelled(true);
      hammerManager.performDash(player);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerArmSwing(io.papermc.paper.event.player.PlayerArmSwingEvent event) {
    if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
      return;
    }

    Player player = event.getPlayer();
    if (!plugin.isPluginEnabled() || !plugin.getHierarchyHammerManager().isEnabled()) {
      return;
    }

    ItemStack mainHand = player.getInventory().getItemInMainHand();
    if (!hammerManager.isHammer(mainHand)) {
      return;
    }

    if (hammerManager.isOnMissCooldown(player)) {
      return;
    }

    UUID uuid = player.getUniqueId();
    long now = System.currentTimeMillis();
    long lastSwing = lastSwingTime.getOrDefault(uuid, 0L);

    if (now - lastSwing < 200L) {
      return;
    }
    lastSwingTime.put(uuid, now);

    var rayTrace = player.getWorld().rayTraceEntities(
        player.getEyeLocation(),
        player.getEyeLocation().getDirection(),
        4.5,
        0.5,
        e -> e instanceof LivingEntity && !e.equals(player));

    if (rayTrace != null && rayTrace.getHitEntity() != null) {
      return;
    }

    swingHit.put(uuid, false);

    SchedulerUtil.runForEntityLater(
        plugin,
        player,
        () -> {
          if (Boolean.FALSE.equals(swingHit.getOrDefault(uuid, false))) {
            if (player.isOnline() && hammerManager.isHammer(player.getInventory().getItemInMainHand())) {
              hammerManager.registerMiss(player);
            }
          }
          swingHit.remove(uuid);
        },
        4L);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
    Player player = event.getPlayer();

    if (HammerManager.isStandingOnGround(player)) {
      hammerManager.onPlayerLand(player);
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onEntityDamage(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player attacker)) {
      return;
    }

    swingHit.put(attacker.getUniqueId(), true);

    ItemStack item = attacker.getInventory().getItemInMainHand();
    if (!hammerManager.isHammer(item)) {
      return;
    }

    if (!plugin.isPluginEnabled() || !plugin.getHierarchyHammerManager().isEnabled()) {
      return;
    }

    if (hammerManager.isOnMissCooldown(attacker)) {
      event.setCancelled(true);
      return;
    }

    if (plugin.getConfig().getBoolean("hierarchy-hammer.mace-smash.enabled", true)
        && attacker.getFallDistance()
            >= plugin.getConfig().getDouble("hierarchy-hammer.mace-smash.min-fall-distance", 1.5)) {
      double multiplier =
          Math.max(0.0, plugin.getConfig().getDouble("hierarchy-hammer.mace-smash.damage-multiplier", 1.0));
      event.setDamage(event.getDamage() * multiplier);
    }

    if (!(event.getEntity() instanceof Player victim)) {
      return;
    }

    UUID uuid = attacker.getUniqueId();

    double minFallDistance =
        plugin.getConfig().getDouble("hierarchy-hammer.mace-smash.min-fall-distance", 1.5);
    boolean isSmash =
        attacker.getFallDistance() > minFallDistance
            || !HammerManager.isStandingOnGround(attacker)
            || hammerManager.isSentencingActive(uuid);

    if (!isSmash) {
      return;
    }

    int required = hammerManager.getComboRequired();
    long now = System.currentTimeMillis();
    long resetMillis = hammerManager.getComboResetMillis();
    if (resetMillis > 0
        && hammerManager.getComboHits(uuid) > 0
        && now - hammerManager.getLastHitTime(uuid) > resetMillis) {
      hammerManager.setComboHits(uuid, 0);
    }

    int hits = hammerManager.getComboHits(uuid) + 1;
    hammerManager.setComboHits(uuid, hits);
    hammerManager.setLastHitTime(uuid, now);

    if (hits <= required) {
      if (hammerManager.isSentencingActive(uuid)) {
        hammerManager.addDashCharge(uuid);
      }
      attacker.getWorld().playSound(attacker.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_LAND, 0.5f, 1.0f);
      plugin.getMessageManager().sendActionBar(attacker, "§6" + hits + " §7/ §b" + required);
    } else {
      hammerManager.executeVerdict(attacker, victim, event);
    }
  }

  @EventHandler
  public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
    event.getPlayer().setGravity(true);
  }

  @EventHandler
  public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
    event.getPlayer().setGravity(true);
    UUID uuid = event.getPlayer().getUniqueId();
    hammerManager.resetSentencing(uuid);
    lastSwingTime.remove(uuid);
    swingHit.remove(uuid);
  }
}
