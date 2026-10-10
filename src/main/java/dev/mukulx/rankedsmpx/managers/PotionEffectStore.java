package dev.mukulx.rankedsmpx.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.database.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PotionEffectStore {

  private final RankedSMPXPlugin plugin;
  private final DatabaseManager databaseManager;
  private final Gson gson;

  // Track players who are currently having effects restored to prevent re-multiplication
  private final Set<UUID> restoringPlayers = ConcurrentHashMap.newKeySet();

  public PotionEffectStore(
      @NotNull RankedSMPXPlugin plugin, @NotNull DatabaseManager databaseManager) {
    this.plugin = plugin;
    this.databaseManager = databaseManager;
    this.gson = new Gson();
  }

  public void saveEffects(@NotNull Player player) {
    List<SerializedPotionEffect> effects = new ArrayList<>();

    for (PotionEffect effect : player.getActivePotionEffects()) {
      if (effect.getDuration() < 200) {
        continue;
      }

      effects.add(
          new SerializedPotionEffect(
              effect.getType().getKey().asString(),
              effect.getDuration(),
              effect.getAmplifier(),
              effect.isAmbient(),
              effect.hasParticles(),
              effect.hasIcon()));
    }

    if (effects.isEmpty()) {
      deleteEffects(player.getUniqueId());
      return;
    }

    String json = gson.toJson(effects);

    String sql =
        databaseManager.isMySql()
            ? "INSERT INTO potion_effects (uuid, effects, last_updated) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE effects=VALUES(effects), last_updated=VALUES(last_updated)"
            : "INSERT OR REPLACE INTO potion_effects (uuid, effects, last_updated) VALUES (?, ?, ?)";

    databaseManager.executeUpdate(
        sql, player.getUniqueId().toString(), json, System.currentTimeMillis());

    if (plugin.getConfigManager().getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(String.format("Saved %d potion effects for %s", effects.size(), player.getName()));
    }
  }

  public void restoreEffects(@NotNull Player player) {
    String sql = "SELECT effects FROM potion_effects WHERE uuid = ?";
    boolean shouldDelete = false;

    try (Connection conn = databaseManager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      if (conn == null) {
        plugin.getLogger().warning("Cannot restore potion effects - no database connection");
        return;
      }

      stmt.setString(1, player.getUniqueId().toString());
      ResultSet rs = stmt.executeQuery();

      if (rs.next()) {
        String json = rs.getString("effects");

        List<SerializedPotionEffect> effects =
            gson.fromJson(json, new TypeToken<List<SerializedPotionEffect>>() {}.getType());

        if (effects != null && !effects.isEmpty()) {
          // Mark player as restoring to prevent PotionEffectListener from multiplying again
          restoringPlayers.add(player.getUniqueId());

          try {
            for (SerializedPotionEffect serialized : effects) {
              try {
                PotionEffectType type = resolvePotionEffectType(serialized.type);
                if (type == null) {
                  plugin.getLogger().warning("Unknown potion effect type: " + serialized.type);
                  continue;
                }

                PotionEffect effect =
                    new PotionEffect(
                        type,
                        serialized.duration,
                        serialized.amplifier,
                        serialized.ambient,
                        serialized.particles,
                        serialized.icon);

                player.removePotionEffect(effect.getType());
                player.addPotionEffect(effect);

              } catch (Exception e) {
                plugin
                    .getLogger()
                    .warning(
                        "Failed to restore potion effect "
                            + serialized.type
                            + " for "
                            + player.getName()
                            + ": "
                            + e.getMessage());
              }
            }

            if (plugin
                .getConfigManager()
                .getConfig()
                .getBoolean("debug", false)) {
              plugin
                  .getLogger()
                  .info(
                      String.format(
                          "Restored %d potion effects for %s", effects.size(), player.getName()));
            }
          } finally {
            restoringPlayers.remove(player.getUniqueId());
          }
        }

        shouldDelete = true;
      }

      rs.close();

    } catch (SQLException e) {
      plugin
          .getLogger()
          .warning(
              "Failed to restore potion effects for " + player.getName() + ": " + e.getMessage());
    }

    // Delete from database after connection is closed to avoid deadlock
    if (shouldDelete) {
      deleteEffects(player.getUniqueId());
    }
  }

  public boolean isRestoring(@NotNull UUID uuid) {
    return restoringPlayers.contains(uuid);
  }

  public void deleteEffects(@NotNull UUID uuid) {
    String sql = "DELETE FROM potion_effects WHERE uuid = ?";
    databaseManager.executeUpdate(sql, uuid.toString());
  }

  @Nullable
  public PotionEffectType resolvePotionEffectType(@Nullable String storedType) {
    if (storedType == null || storedType.isBlank()) {
      return null;
    }

    String normalized = storedType.trim().toLowerCase(Locale.ROOT);
    String keyName = normalized.startsWith("minecraft:") ? normalized : "minecraft:" + normalized;
    NamespacedKey key = NamespacedKey.fromString(keyName);
    if (key != null) {
      try {
        PotionEffectType type = Registry.MOB_EFFECT.get(key);
        if (type != null) {
          return type;
        }
      } catch (Throwable ignored) {
      }
    }

    String rawName = normalized.startsWith("minecraft:") ? normalized.substring("minecraft:".length()) : normalized;
    String upper = rawName.toUpperCase(Locale.ROOT).replace('-', '_');

    String legacyName = switch (upper) {
      case "SLOW" -> "SLOWNESS";
      case "FAST_DIGGING" -> "HASTE";
      case "SLOW_DIGGING" -> "MINING_FATIGUE";
      case "INCREASE_DAMAGE" -> "STRENGTH";
      case "HEAL" -> "INSTANT_HEALTH";
      case "HARM" -> "INSTANT_DAMAGE";
      case "JUMP" -> "JUMP_BOOST";
      case "CONFUSION" -> "NAUSEA";
      case "DAMAGE_RESISTANCE" -> "RESISTANCE";
      default -> upper;
    };

    PotionEffectType type = registryEffect(legacyName);
    return type != null ? type : registryEffect(upper);
  }

  @Nullable
  private static PotionEffectType registryEffect(@NotNull String upperName) {
    NamespacedKey key = NamespacedKey.fromString(upperName.toLowerCase(Locale.ROOT));
    return key == null ? null : Registry.MOB_EFFECT.get(key);
  }

  private static class SerializedPotionEffect {
    String type;
    int duration;
    int amplifier;
    boolean ambient;
    boolean particles;
    boolean icon;

    SerializedPotionEffect(
        String type,
        int duration,
        int amplifier,
        boolean ambient,
        boolean particles,
        boolean icon) {
      this.type = type;
      this.duration = duration;
      this.amplifier = amplifier;
      this.ambient = ambient;
      this.particles = particles;
      this.icon = icon;
    }
  }
}
