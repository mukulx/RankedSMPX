package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomConfigManager {

  public static final String RANK_ITEM_PDC_KEY = "physical_rank_item";

  private final RankedSMPXPlugin plugin;
  private final NamespacedKey rankItemKey;
  private File customConfigFile;
  private FileConfiguration customConfig;

  public CustomConfigManager(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    this.rankItemKey = new NamespacedKey(plugin, RANK_ITEM_PDC_KEY);
    loadConfig(true);
  }

  public void loadConfig(boolean logLoad) {
    customConfigFile = new File(plugin.getDataFolder(), "custom.yml");
    if (!customConfigFile.exists()) {
      plugin.saveResource("custom.yml", false);
    }
    customConfig = YamlConfiguration.loadConfiguration(customConfigFile);
    if (logLoad && isRankDropEnabled()) {
      plugin.getLogger().info("Loaded custom.yml (Rank-Drop feature is ENABLED)");
    }
  }

  public void reload() {
    loadConfig(false);
  }

  @NotNull
  public FileConfiguration getConfig() {
    return customConfig;
  }

  public boolean isRankDropEnabled() {
    if (customConfig == null) return false;
    return customConfig.getBoolean("rank-drop.enabled",
        customConfig.getBoolean("lower-rank-drop.enabled", false));
  }

  public boolean isClaimEnabled() {
    if (customConfig == null) return true;
    return customConfig.getBoolean("rank-drop.claim-enabled",
        customConfig.getBoolean("lower-rank-drop.claim-enabled", true));
  }

  public boolean isBroadcastOnDrop() {
    if (customConfig == null) return true;
    return customConfig.getBoolean("rank-drop.broadcast-on-drop",
        customConfig.getBoolean("lower-rank-drop.broadcast-on-drop", true));
  }

  public int getClaimCooldownSeconds() {
    if (customConfig == null) return 10;
    return customConfig.getInt("rank-drop.claim-cooldown",
        customConfig.getInt("lower-rank-drop.claim-cooldown", 10));
  }

  @NotNull
  public ItemStack createRankItem(int rank) {
    String matName = customConfig.getString("rank-drop.item.material",
        customConfig.getString("lower-rank-drop.item.material", "BLACK_DYE"));
    Material mat = Material.matchMaterial(matName);
    if (mat == null) {
      mat = Material.BLACK_DYE;
    }

    ItemStack item = new ItemStack(mat, 1);
    ItemMeta meta = item.getItemMeta();
    if (meta != null) {
      meta.getPersistentDataContainer().set(rankItemKey, PersistentDataType.INTEGER, rank);

      String rankPrefix = plugin.getMessageManager().getRankPrefixLegacy(rank, false).trim();

      String rawName = customConfig.getString(
          "rank-drop.item.name",
          customConfig.getString("lower-rank-drop.item.name", "&#FFAA00&lRᴀɴᴋ {rank} &#FFAA00"))
          .replace("{rank}", String.valueOf(rank))
          .replace("{rank_prefix}", rankPrefix);
      meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(plugin.getMessageManager().colorize(rawName)));

      List<String> rawLore = customConfig.getStringList("rank-drop.item.lore");
      if (rawLore.isEmpty()) {
        rawLore = customConfig.getStringList("lower-rank-drop.item.lore");
      }
      List<String> lore = new ArrayList<>();
      for (String line : rawLore) {
        String formatted = line
            .replace("{rank}", String.valueOf(rank))
            .replace("{rank_prefix}", rankPrefix);
        lore.add(plugin.getMessageManager().applySmallCaps(plugin.getMessageManager().colorize(formatted)));
      }
      meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(lore));

      boolean glow = customConfig.getBoolean("rank-drop.item.glow",
          customConfig.getBoolean("lower-rank-drop.item.glow", true));
      if (glow) {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      }

      item.setItemMeta(meta);
    }

    return item;
  }

  public boolean isRankItem(@Nullable ItemStack item) {
    if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
      return false;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }
    return meta.getPersistentDataContainer().has(rankItemKey, PersistentDataType.INTEGER);
  }

  @Nullable
  public Integer getRankFromItem(@Nullable ItemStack item) {
    if (!isRankItem(item)) {
      return null;
    }
    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return null;
    }
    return meta.getPersistentDataContainer().get(rankItemKey, PersistentDataType.INTEGER);
  }

  @SuppressWarnings("removal")
  public void playActionSound(@NotNull Player player, boolean isSwap) {
    String sub = isSwap ? "swap" : "claim";
    String path = customConfig.contains("rank-drop.sounds." + sub)
        ? "rank-drop.sounds." + sub
        : "lower-rank-drop.sounds." + sub;

    if (!customConfig.getBoolean(path + ".enabled", true)) {
      return;
    }
    String soundName = customConfig.getString(path + ".sound", isSwap ? "ITEM_ARMOR_EQUIP_NETHERITE" : "ENTITY_PLAYER_LEVELUP");
    float volume = (float) customConfig.getDouble(path + ".volume", 1.0);
    float pitch = (float) customConfig.getDouble(path + ".pitch", isSwap ? 1.0 : 1.2);
    try {
      Sound sound = Sound.valueOf(soundName.toUpperCase());
      player.playSound(player.getLocation(), sound, volume, pitch);
    } catch (IllegalArgumentException ignored) {
    }
  }

  public String getMessage(String key, @Nullable Map<String, String> placeholders) {
    String raw = customConfig.getString("rank-drop.messages." + key,
        customConfig.getString("lower-rank-drop.messages." + key, ""));
    if (raw == null || raw.isBlank()) {
      return "";
    }

    if (raw.contains("{prefix}")) {
      String prefix = plugin.getMessageManager().getPrefix();
      raw = raw.replace("{prefix}", prefix != null ? prefix : "&bRankedSMPX &8» &r");
    }

    Map<String, String> allPlaceholders = new HashMap<>();
    if (placeholders != null) {
      allPlaceholders.putAll(placeholders);
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        try {
          int rankNum = Integer.parseInt(entry.getValue());
          String prefixKey = entry.getKey() + "_prefix";
          if (!allPlaceholders.containsKey(prefixKey)) {
            allPlaceholders.put(prefixKey, plugin.getMessageManager().getRankPrefixLegacy(rankNum, false).trim());
          }
        } catch (NumberFormatException ignored) {
        }
      }
    }

    for (Map.Entry<String, String> entry : allPlaceholders.entrySet()) {
      raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
    }

    return plugin.getMessageManager().applySmallCaps(plugin.getMessageManager().colorize(raw));
  }

  public String getMessage(String key) {
    return getMessage(key, null);
  }
}
