package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HierarchyHammerManager {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;
  private UUID currentHammerHolder;
  private boolean hammerGiven;

  public HierarchyHammerManager(
      @NotNull RankedSMPXPlugin plugin, @NotNull RankManager rankManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.messageManager = plugin.getMessageManager();
    this.currentHammerHolder = null;
    this.hammerGiven = false;
  }

  public boolean isEnabled() {
    return plugin.getConfig().getBoolean("hierarchy-hammer.enabled", true);
  }

  public boolean isAutoGiveEnabled() {
    return plugin.getConfig().getBoolean("hierarchy-hammer.auto-give-on-start", false)
        || plugin.getConfig().getBoolean("hierarchy-hammer.auto-give", false)
        || plugin.getConfig().getBoolean("hierarchy-hammer.give-on-start", false);
  }

  public void resetForNewSeason() {
    this.hammerGiven = false;
    this.currentHammerHolder = null;
  }

  public boolean hasHammer(@NotNull Player player) {
    org.bukkit.inventory.PlayerInventory inv = player.getInventory();
    if (inv == null) {
      return false;
    }
    for (ItemStack item : inv.getContents()) {
      if (isHierarchyHammer(item)) {
        return true;
      }
    }
    if (isHierarchyHammer(inv.getItemInOffHand())) {
      return true;
    }
    if (isHierarchyHammer(player.getItemOnCursor())) {
      return true;
    }
    return false;
  }

  @NotNull
  public ItemStack createHammer() {
    ItemStack hammer = new ItemStack(Material.MACE);
    ItemMeta meta = hammer.getItemMeta();

    if (meta != null) {
      org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "judgement_hammer");
      meta.getPersistentDataContainer()
          .set(key, org.bukkit.persistence.PersistentDataType.BYTE, (byte) 1);

      String name =
          plugin
              .getConfig()
              .getString(
                  "hierarchy-hammer.name",
                  "&#FFD700ʜ&#FFCC00ɪ&#FFC200ᴇ&#FFB700ʀ&#FFAD00ᴀ&#FFA200ʀ&#FF9800ᴄ&#FF8D00ʜ&#FF8300ʏ"
                      + " &#FFAA00ʜ&#FFAA00ᴀ&#FFAA00ᴍ&#FFAA00ᴍ&#FFAA00ᴇ&#FFAA00ʀ");

      meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(plugin.getMessageManager().colorize(name)));

      List<String> configLore = plugin.getConfig().getStringList("hierarchy-hammer.lore");
      List<String> lore = new ArrayList<>();
      for (String line : configLore) {
        String coloredLine = plugin.getMessageManager().colorize(line);
        lore.add(plugin.getMessageManager().applySmallCaps(coloredLine));
      }
      meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(lore));

      int customModelData =
          plugin
              .getConfig()
              .getInt(
                  "hierarchy-hammer.custom-model-data",
                  plugin.getConfig().getInt("hierarchy-hammer.model-id", 0));
      if (customModelData > 0) {
        org.bukkit.inventory.meta.components.CustomModelDataComponent modelData =
            meta.getCustomModelDataComponent();
        modelData.setFloats(java.util.List.of((float) customModelData));
        meta.setCustomModelDataComponent(modelData);
      }

      String itemModel = plugin.getConfig().getString("hierarchy-hammer.item-model", "");
      if (itemModel != null && !itemModel.isBlank()) {
        try {
          org.bukkit.NamespacedKey modelKey = org.bukkit.NamespacedKey.fromString(itemModel.toLowerCase());
          if (modelKey != null) {
            meta.setItemModel(modelKey);
          }
        } catch (Throwable ignored) {
        }
      }

      meta.addEnchant(Enchantment.UNBREAKING, 3, true);
      meta.addEnchant(Enchantment.MENDING, 1, true);

      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

      meta.setUnbreakable(true);
      meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

      hammer.setItemMeta(meta);
    }

    return hammer;
  }

  public boolean isHierarchyHammer(@Nullable ItemStack item) {
    if (item == null || item.getType() != Material.MACE) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    if (meta == null) {
      return false;
    }

    org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "judgement_hammer");
    if (meta.getPersistentDataContainer()
        .has(key, org.bukkit.persistence.PersistentDataType.BYTE)) {
      return true;
    }

    if (meta.hasDisplayName()) {
      String expectedName =
          plugin
              .getConfig()
              .getString(
                  "hierarchy-hammer.name",
                  "&#FFD700ʜ&#FFCC00ɪ&#FFC200ᴇ&#FFB700ʀ&#FFAD00ᴀ&#FFA200ʀ&#FF9800ᴄ&#FF8D00ʜ&#FF8300ʏ"
                      + " &#FFAA00ʜ&#FFAA00ᴀ&#FFAA00ᴍ&#FFAA00ᴍ&#FFAA00ᴇ&#FFAA00ʀ");
      // Compare against a freshly colorized name so old hammers are still recognised.
      return MessageManager.plain(meta.displayName())
          .equals(MessageManager.plain(MessageManager.legacy(plugin.getMessageManager().colorize(expectedName))));
    }

    return false;
  }

  public void purgeAllDuplicates(@Nullable UUID authorizedRank1) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            if (!player.isOnline()) return;
            if (authorizedRank1 == null || !player.getUniqueId().equals(authorizedRank1)) {
              removeAllHammersFromPlayer(player);
            } else {
              int count = 0;
              org.bukkit.inventory.PlayerInventory inv = player.getInventory();
              for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && isHierarchyHammer(item)) {
                  count++;
                  if (count > 1) {
                    inv.setItem(i, null);
                  }
                }
              }
              ItemStack offhand = inv.getItemInOffHand();
              if (offhand != null && isHierarchyHammer(offhand)) {
                count++;
                if (count > 1) {
                  inv.setItemInOffHand(null);
                }
              }
              ItemStack cursor = player.getItemOnCursor();
              if (cursor != null && isHierarchyHammer(cursor)) {
                count++;
                if (count > 1) {
                  player.setItemOnCursor(null);
                }
              }
            }
          });
    }
  }

  public void giveHammer(@NotNull Player player) {
    if (!isEnabled()) {
      return;
    }
    if (!SchedulerUtil.isOwnedOrReschedule(plugin, player, () -> giveHammer(player))) {
      return;
    }

    if (!isAutoGiveEnabled()) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                "Auto-give hammer is disabled. Use /rsmp give hammer <player> to manually give it.");
      }
      return;
    }

    purgeAllDuplicates(player.getUniqueId());

    if (hasHammer(player)) {
      currentHammerHolder = player.getUniqueId();
      return;
    }

    ItemStack hammer = createHammer();
    var leftovers = player.getInventory().addItem(hammer);
    if (!leftovers.isEmpty()) {
      player.getInventory().setItem(0, hammer);
    }
    currentHammerHolder = player.getUniqueId();
    hammerGiven = true;

    String title = messageManager.getMessage("hammer.received-title");
    String subtitle = messageManager.getMessage("hammer.received-subtitle");
    messageManager.sendTitle(player, title, subtitle, 10, 70, 20);

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Gave Hierarchy Hammer to " + player.getName());
    }
  }

  public void removeHammer(@NotNull Player player) {
    if (!isEnabled()) {
      return;
    }
    if (!SchedulerUtil.isOwnedOrReschedule(plugin, player, () -> removeHammer(player))) {
      return;
    }

    boolean hadHammer = hasHammer(player);
    removeAllHammersFromPlayer(player);

    if (hadHammer) {
      messageManager.sendMessage(player, "hammer.dropped");

      String title = messageManager.getMessage("hammer.dropped");
      String subtitle = messageManager.getMessage("hammer.dropped-subtitle");
      messageManager.sendTitle(player, title, subtitle, 10, 70, 20);

      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Removed Hierarchy Hammer from " + player.getName());
      }
    }

    if (currentHammerHolder != null && currentHammerHolder.equals(player.getUniqueId())) {
      currentHammerHolder = null;
    }
  }

  public void removeHammerOffline(@NotNull UUID uuid) {
    if (currentHammerHolder != null && currentHammerHolder.equals(uuid)) {
      currentHammerHolder = null;
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Cleared hammer holder for offline player: " + uuid);
      }
    }
  }

  public void updateHammerHolder() {
    if (!isEnabled()) {
      return;
    }

    UUID rank1Player = null;
    for (var entry : rankManager.getAllRanks().entrySet()) {
      if (entry.getValue() == 1) {
        rank1Player = entry.getKey();
        break;
      }
    }

    purgeAllDuplicates(rank1Player);

    if (rank1Player == null) {
      currentHammerHolder = null;
      return;
    }

    currentHammerHolder = rank1Player;
    Player newHolder = Bukkit.getPlayer(rank1Player);
    if (newHolder != null && newHolder.isOnline()) {
      SchedulerUtil.runForEntity(
          plugin,
          newHolder,
          () -> {
            if (!newHolder.isOnline()) return;
            if (isAutoGiveEnabled()) {
              giveHammer(newHolder);
            }
          });
    }
  }

  public boolean shouldHaveHammer(@NotNull UUID uuid) {
    if (!isEnabled()) {
      return false;
    }

    Integer rank = rankManager.getRank(uuid);
    return rank != null && rank == 1;
  }

  public void adminGiveHammer(@NotNull Player player) {
    if (!isEnabled()) {
      return;
    }
    if (!SchedulerUtil.isOwnedOrReschedule(plugin, player, () -> adminGiveHammer(player))) {
      return;
    }

    purgeAllDuplicates(player.getUniqueId());

    if (!hasHammer(player)) {
      ItemStack hammer = createHammer();
      player.getInventory().addItem(hammer);
    }
    currentHammerHolder = player.getUniqueId();

    String title = messageManager.getMessage("hammer.received-title");
    String subtitle = messageManager.getMessage("hammer.received-subtitle");
    messageManager.sendTitle(player, title, subtitle, 10, 70, 20);

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Admin gave Hierarchy Hammer to " + player.getName());
    }
  }

  private void removeAllHammersFromPlayer(@NotNull Player player) {
    org.bukkit.inventory.Inventory inv = player.getInventory();

    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack item = inv.getItem(i);
      if (item != null && isHierarchyHammer(item)) {
        inv.setItem(i, null);
      }
    }

    ItemStack offhand = player.getInventory().getItemInOffHand();
    if (offhand != null && isHierarchyHammer(offhand)) {
      player.getInventory().setItemInOffHand(null);
    }

    ItemStack cursor = player.getItemOnCursor();
    if (cursor != null && isHierarchyHammer(cursor)) {
      player.setItemOnCursor(null);
    }

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Wiped all hammers from " + player.getName());
    }
  }
}
