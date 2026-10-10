package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpearManager {

  private final RankedSMPXPlugin plugin;
  private final boolean spearsSupported;
  private Material diamondSpearMaterial = null;

  public SpearManager(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;

    this.spearsSupported = checkSpearSupport();

    if (spearsSupported) {
      plugin.getLogger().info("Spears detected! Spear features enabled (1.21.11+)");
    } else {
      plugin
          .getLogger()
          .info("Spears not supported on this server version. Spear features disabled.");
    }
  }

  private boolean checkSpearSupport() {
    try {
      diamondSpearMaterial = Material.valueOf("DIAMOND_SPEAR");
      return diamondSpearMaterial != null;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public boolean isSpearsSupported() {
    return spearsSupported;
  }

  public boolean isEnabled() {
    return plugin.getConfig().getBoolean("spear.enabled", true);
  }

  @Nullable
  public ItemStack createSpear() {
    if (!spearsSupported || diamondSpearMaterial == null) {
      return null;
    }

    if (!isEnabled()) {
      return null;
    }

    ItemStack spear = new ItemStack(diamondSpearMaterial);
    ItemMeta meta = spear.getItemMeta();

    if (meta != null) {
      if (spear.getType() == diamondSpearMaterial) {
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "ranked_spear");
        meta.getPersistentDataContainer()
            .set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

        boolean unbreakable = plugin.getConfig().getBoolean("spear.unbreakable", true);
        if (unbreakable) {
          meta.setUnbreakable(true);
          meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        ConfigurationSection enchants =
            plugin.getConfig().getConfigurationSection("spear.enchantments");
        if (enchants != null) {
          for (String enchantName : enchants.getKeys(false)) {
            int level = enchants.getInt(enchantName, 1);

            Enchantment enchant = getEnchantmentByName(enchantName);
            if (enchant != null) {
              meta.addEnchant(enchant, level, true);
              if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Applied " + enchantName + " " + level + " to spear");
              }
            } else {
              plugin.getLogger().warning("Unknown enchantment in spear config: " + enchantName);
            }
          }
        }

        spear.setItemMeta(meta);
      } else {
        plugin
            .getLogger()
            .warning(
                "Attempted to create spear but material mismatch: "
                    + spear.getType()
                    + " vs "
                    + diamondSpearMaterial);
        return null;
      }
    }

    return spear;
  }

  @Nullable
  private Enchantment getEnchantmentByName(@NotNull String name) {
    String key = name.trim().toLowerCase(java.util.Locale.ROOT);
    if (key.startsWith("minecraft:")) {
      key = key.substring("minecraft:".length());
    }
    key = switch (key) {
      case "damage_all" -> "sharpness";
      case "damage_undead" -> "smite";
      case "damage_arthropods" -> "bane_of_arthropods";
      case "durability" -> "unbreaking";
      case "dig_speed" -> "efficiency";
      case "loot_bonus_mobs" -> "looting";
      case "loot_bonus_blocks" -> "fortune";
      case "protection_environmental" -> "protection";
      case "protection_fire" -> "fire_protection";
      case "protection_fall" -> "feather_falling";
      case "protection_explosions" -> "blast_protection";
      case "protection_projectile" -> "projectile_protection";
      case "arrow_damage" -> "power";
      case "arrow_knockback" -> "punch";
      case "arrow_fire" -> "flame";
      case "arrow_infinite" -> "infinity";
      case "water_worker" -> "aqua_affinity";
      case "oxygen" -> "respiration";
      case "luck" -> "luck_of_the_sea";
      default -> key;
    };
    org.bukkit.NamespacedKey namespacedKey = org.bukkit.NamespacedKey.fromString(key);
    return namespacedKey == null
        ? null
        : io.papermc.paper.registry.RegistryAccess.registryAccess()
            .getRegistry(io.papermc.paper.registry.RegistryKey.ENCHANTMENT)
            .get(namespacedKey);
  }

  public boolean isSpear(@Nullable ItemStack item) {
    if (!spearsSupported || item == null || diamondSpearMaterial == null) {
      return false;
    }

    if (item.getType() != diamondSpearMaterial) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }

    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "ranked_spear");
    return meta.getPersistentDataContainer()
        .has(key, org.bukkit.persistence.PersistentDataType.BYTE);
  }

  public boolean giveSpear(@NotNull Player player) {
    if (!spearsSupported) {
      player.sendMessage("§cSpears are not supported on this server version (requires 1.21.11+)");
      return false;
    }

    if (!isEnabled()) {
      player.sendMessage("§cSpears are disabled in config!");
      return false;
    }

    ItemStack spear = createSpear();
    if (spear == null) {
      player.sendMessage("§cFailed to create spear");
      return false;
    }

    if (SchedulerUtil.isOwnedOrReschedule(plugin, player, () -> player.getInventory().addItem(spear))) {
      player.getInventory().addItem(spear);
    }
    player.sendMessage("§aYou received a Diamond Spear!");
    return true;
  }

  public boolean updateSpear(@NotNull ItemStack spear) {
    if (!spearsSupported || !isSpear(spear)) {
      return false;
    }

    ItemMeta meta = spear.getItemMeta();
    if (meta == null) {
      return false;
    }

    for (Enchantment ench : meta.getEnchants().keySet()) {
      meta.removeEnchant(ench);
    }

    boolean unbreakable = plugin.getConfig().getBoolean("spear.unbreakable", true);
    meta.setUnbreakable(unbreakable);
    if (unbreakable) {
      meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
    } else {
      meta.removeItemFlags(ItemFlag.HIDE_UNBREAKABLE);
    }

    ConfigurationSection enchants =
        plugin.getConfig().getConfigurationSection("spear.enchantments");
    if (enchants != null) {
      for (String enchantName : enchants.getKeys(false)) {
        int level = enchants.getInt(enchantName, 1);

        Enchantment enchant = getEnchantmentByName(enchantName);
        if (enchant != null) {
          meta.addEnchant(enchant, level, true);
        }
      }
    }

    spear.setItemMeta(meta);
    return true;
  }

  public int updatePlayerSpears(@NotNull Player player) {
    if (!spearsSupported) {
      return 0;
    }

    int updated = 0;

    for (ItemStack item : player.getInventory().getContents()) {
      if (isSpear(item) && updateSpear(item)) {
        updated++;
      }
    }

    for (ItemStack item : player.getInventory().getArmorContents()) {
      if (isSpear(item) && updateSpear(item)) {
        updated++;
      }
    }

    return updated;
  }

  public int updateAllSpears() {
    if (!spearsSupported) {
      return 0;
    }

    int totalScheduled = 0;

    for (Player player : plugin.getServer().getOnlinePlayers()) {
      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            int updated = updatePlayerSpears(player);
            if (updated > 0 && plugin.getConfig().getBoolean("debug", false)) {
              plugin
                  .getLogger()
                  .info("Updated " + updated + " spear(s) for player " + player.getName());
            }
          });
      totalScheduled++;
    }

    return totalScheduled;
  }
}
