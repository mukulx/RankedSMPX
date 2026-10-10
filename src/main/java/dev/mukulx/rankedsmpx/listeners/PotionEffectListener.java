package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.PotionManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PotionEffectListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final PotionManager potionManager;

  // Track effects currently being applied by our plugin to avoid recursion / re-processing
  private final Set<String> selfApplyingEffects = ConcurrentHashMap.newKeySet();

  public PotionEffectListener(
      @NotNull RankedSMPXPlugin plugin, @NotNull PotionManager potionManager) {
    this.plugin = plugin;
    this.potionManager = potionManager;
  }

  private String getEffectTrackingKey(@NotNull UUID uuid, @NotNull PotionEffectType type) {
    return uuid.toString() + ":" + getNormalizedEffectKey(type);
  }

  @NotNull
  private String getNormalizedEffectKey(@NotNull PotionEffectType type) {
    return type.getKey().getKey().toLowerCase(Locale.ROOT);
  }

  @NotNull
  private String normalizeName(@NotNull String name) {
    String normalized = name.trim().toLowerCase(Locale.ROOT);
    if (normalized.startsWith("minecraft:")) {
      normalized = normalized.substring("minecraft:".length());
    }
    normalized = normalized.replace('-', '_');

    return switch (normalized) {
      case "slow" -> "slowness";
      case "fast_digging" -> "haste";
      case "slow_digging" -> "mining_fatigue";
      case "increase_damage" -> "strength";
      case "heal" -> "instant_health";
      case "harm" -> "instant_damage";
      case "jump" -> "jump_boost";
      case "confusion" -> "nausea";
      case "damage_resistance" -> "resistance";
      default -> normalized;
    };
  }

  private boolean isIgnoredEffect(@NotNull PotionEffectType type) {
    String key = getNormalizedEffectKey(type);
    if (key.isEmpty()) {
      return false;
    }

    Set<String> ignoredConfig = new HashSet<>();
    for (String raw : plugin.getConfigManager().getConfig().getStringList("potions.ignored-effects")) {
      if (raw != null && !raw.isBlank()) {
        ignoredConfig.add(normalizeName(raw));
      }
    }

    return ignoredConfig.contains(key);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPotionEffect(EntityPotionEffectEvent event) {
    if (!(event.getEntity() instanceof Player player)) {
      return;
    }

    if (!plugin.isPluginEnabled()) {
      return;
    }

    EntityPotionEffectEvent.Action action = event.getAction();
    if (action != EntityPotionEffectEvent.Action.ADDED
        && action != EntityPotionEffectEvent.Action.CHANGED) {
      return;
    }

    PotionEffect newEffect = event.getNewEffect();
    if (newEffect == null) {
      return;
    }

    PotionEffectType effectType = newEffect.getType();
    UUID uuid = player.getUniqueId();
    String trackingKey = getEffectTrackingKey(uuid, effectType);

    if (selfApplyingEffects.contains(trackingKey)) {
      return;
    }

    if (plugin.getPotionEffectStore() != null
        && plugin.getPotionEffectStore().isRestoring(uuid)) {
      return;
    }

    EntityPotionEffectEvent.Cause cause = event.getCause();
    if (cause == EntityPotionEffectEvent.Cause.PLUGIN) {
      boolean extendCustom = plugin.getConfig().getBoolean("potions.extend-custom-effects", true);
      if (!extendCustom) {
        return;
      }
    }

    // Skip continuous ambient sources like Beacon/Conduit to prevent constant cancel/HUD jitter
    if (cause == EntityPotionEffectEvent.Cause.BEACON || cause == EntityPotionEffectEvent.Cause.CONDUIT) {
      return;
    }

    Integer rank = plugin.getRankManager().getRank(uuid);
    if (rank == null) {
      return;
    }

    // Instant effects (Instant Health, Instant Damage) should never have duration multiplied
    if (effectType.isInstant()) {
      return;
    }

    int baseDuration = newEffect.getDuration();

    if (baseDuration <= 0) {
      return;
    }

    if (isIgnoredEffect(effectType)) {
      if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                String.format(
                    "Skipping multiplier for %s (Rank %d): %s is in ignored-effects list",
                    player.getName(), rank, getNormalizedEffectKey(effectType)));
      }
      return;
    }

    double multiplier = potionManager.calculateMultiplier(rank);
    if (multiplier <= 1.0) {
      return;
    }

    int modifiedDuration = (int) Math.round(baseDuration * multiplier);
    if (modifiedDuration <= baseDuration) {
      return;
    }

    event.setCancelled(true);

    PotionEffect modifiedEffect =
        new PotionEffect(
            effectType,
            modifiedDuration,
            newEffect.getAmplifier(),
            newEffect.isAmbient(),
            newEffect.hasParticles(),
            newEffect.hasIcon());

    selfApplyingEffects.add(trackingKey);

    // Apply asynchronously from the event dispatch via entity scheduler to avoid NMS recursion
    SchedulerUtil.runForEntity(
        plugin,
        player,
        () -> {
          try {
            if (player.isOnline() && !player.isDead()) {
              player.removePotionEffect(modifiedEffect.getType());
              player.addPotionEffect(modifiedEffect);

              if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
                double baseSeconds = baseDuration / 20.0;
                double modifiedSeconds = modifiedDuration / 20.0;
                plugin
                    .getLogger()
                    .info(
                        String.format(
                            "Applied potion multiplier to %s (Rank %d, %s, Action: %s): %s effect - %.1fs (%d ticks) * %.2fx = %.1fs (%d ticks)",
                            player.getName(),
                            rank,
                            getCauseDescription(cause),
                            action.name(),
                            getNormalizedEffectKey(effectType),
                            baseSeconds,
                            baseDuration,
                            multiplier,
                            modifiedSeconds,
                            modifiedDuration));
              }
            }
          } finally {
            SchedulerUtil.runForEntityLater(
                plugin,
                player,
                () -> selfApplyingEffects.remove(trackingKey),
                1L);
          }
        });
  }

  public void cleanupPlayer(@NotNull UUID uuid) {
    String prefix = uuid.toString() + ":";
    selfApplyingEffects.removeIf(key -> key.startsWith(prefix));
  }

  private String getCauseDescription(@Nullable EntityPotionEffectEvent.Cause cause) {
    if (cause == null) return "Unknown";
    return switch (cause) {
      case POTION_DRINK -> "Drinkable Potion";
      case POTION_SPLASH -> "Splash Potion";
      case ARROW -> "Tipped Arrow";
      case AREA_EFFECT_CLOUD -> "Lingering Potion Cloud";
      case BEACON -> "Beacon";
      case COMMAND -> "Command";
      case CONDUIT -> "Conduit";
      case FOOD -> "Food";
      case MILK -> "Milk";
      case PLUGIN -> "Plugin";
      case TURTLE_HELMET -> "Turtle Helmet";
      case VILLAGER_TRADE -> "Villager Trade";
      case WITHER_ROSE -> "Wither Rose";
      case ATTACK -> "Attack";
      case AXOLOTL -> "Axolotl";
      case DEATH -> "Death";
      case DOLPHIN -> "Dolphin";
      case EXPIRATION -> "Expiration";
      case ILLUSION -> "Illusion";
      case PATROL_CAPTAIN -> "Patrol Captain";
      case SPIDER_SPAWN -> "Spider Spawn";
      case TOTEM -> "Totem";
      case UNKNOWN -> "Unknown";
      default -> cause.name();
    };
  }
}
