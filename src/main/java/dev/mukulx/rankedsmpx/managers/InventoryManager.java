package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InventoryManager {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final ConfigManager configManager;
  private final Map<UUID, Inventory> extraInventories;

  private final Map<UUID, Long> inventoryLocks = new ConcurrentHashMap<>();
  private final Map<UUID, String> inventoryChecksums = new ConcurrentHashMap<>();
  private final Map<UUID, Long> lastSaveTime = new ConcurrentHashMap<>();

  private final Map<UUID, Map<Integer, ItemStack>> undeliveredOverflow = new ConcurrentHashMap<>();
  private static final int OVERFLOW_SLOT_BASE = 1000;

  private final Map<Inventory, UUID> retiredInventories =
      Collections.synchronizedMap(new IdentityHashMap<>());

  private final Map<UUID, Long> combatTags = new ConcurrentHashMap<>();

  private final Map<UUID, UUID> adminViewingSessions = new ConcurrentHashMap<>();
  private final Map<UUID, Inventory> adminViewingInventories = new ConcurrentHashMap<>();

  private static final long LOCK_TIMEOUT = 3000; // 3 seconds max lock time
  private static final long SAVE_DEBOUNCE = 50; // 50ms debounce for saves

  public InventoryManager(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.configManager = configManager;
    this.extraInventories = new ConcurrentHashMap<>();
  }

  private boolean isInventoryLocked(@NotNull UUID uuid) {
    long now = System.currentTimeMillis();

    Long lockTime = inventoryLocks.get(uuid);
    if (lockTime != null) {
      if (now - lockTime < LOCK_TIMEOUT) {
        return true;
      } else {
        inventoryLocks.remove(uuid);
        plugin.getLogger().warning("Removed expired lock for " + uuid);
      }
    }

    return false;
  }

  private boolean acquireLock(@NotNull UUID uuid) {
    long now = System.currentTimeMillis();
    Long existing = inventoryLocks.putIfAbsent(uuid, now);
    if (existing != null) {
      if (now - existing > LOCK_TIMEOUT) {
        inventoryLocks.put(uuid, now);
        plugin.getLogger().warning("Forced lock acquisition for " + uuid + " (expired lock)");
        return true;
      }
      return false;
    }
    return true;
  }

  private void releaseLock(@NotNull UUID uuid) {
    inventoryLocks.remove(uuid);
  }

  public void tagCombat(@NotNull UUID uuid) {
    combatTags.put(uuid, System.currentTimeMillis());
  }

  public boolean isInCombat(@NotNull UUID uuid) {
    if (!configManager.getConfig().getBoolean("inventory.combat.disabled-during-combat", false)) {
      return false;
    }
    return isCombatTagged(uuid);
  }

  public boolean isCombatTagged(@NotNull UUID uuid) {
    Long combatTime = combatTags.get(uuid);
    if (combatTime == null) {
      return false;
    }

    int combatDuration =
        configManager.getConfig().getInt("inventory.combat.combat-duration", 10) * 1000;
    long elapsed = System.currentTimeMillis() - combatTime;

    if (elapsed > combatDuration) {
      combatTags.remove(uuid);
      return false;
    }

    return true;
  }

  public int getRemainingCombatTime(@NotNull UUID uuid) {
    Long combatTime = combatTags.get(uuid);
    if (combatTime == null) {
      return 0;
    }

    int combatDuration =
        configManager.getConfig().getInt("inventory.combat.combat-duration", 10) * 1000;
    long elapsed = System.currentTimeMillis() - combatTime;
    int remaining = (int) ((combatDuration - elapsed) / 1000);

    return Math.max(0, remaining);
  }

  public void removeCombatTag(@NotNull UUID uuid) {
    combatTags.remove(uuid);
  }

  private String calculateChecksum(@NotNull Inventory inv) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < inv.getSize(); i++) {
      ItemStack item = inv.getItem(i);
      if (item != null && !item.getType().isAir() && item.getType() != Material.BARRIER) {
        sb.append(i)
            .append(":")
            .append(item.getType())
            .append(":")
            .append(item.getAmount())
            .append(";");
      }
    }
    return sb.toString();
  }

  public void openExtraInventory(@NotNull Player player) {
    UUID uuid = player.getUniqueId();

    if (!configManager.isInventoryEnabled()) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Extra inventory is disabled in config");
      }
      return;
    }

    if (isInCombat(uuid)) {
      int remaining = getRemainingCombatTime(uuid);
      plugin
          .getMessageManager()
          .sendLegacyMessage(
              player,
              "&c&lIN COMBAT! &7You cannot open your extra inventory for &b" + remaining + "s&7.");
      return;
    }

    if (configManager.isAntiDupeEnabled()) {
      if (isInventoryLocked(uuid)) {
        plugin
            .getMessageManager()
            .sendLegacyMessage(player, "&c&lPLEASE WAIT! &7Your inventory is being processed.");
        return;
      }

      if (!acquireLock(uuid)) {
        plugin
            .getMessageManager()
            .sendLegacyMessage(player, "&c&lPLEASE WAIT! &7Your inventory is being processed.");
        return;
      }
    }

    try {
      Integer rank = rankManager.getRank(uuid);
      if (rank == null) {
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin
              .getLogger()
              .info(player.getName() + " cannot access extra inventory (rank: " + rank + ")");
        }
        plugin
            .getMessageManager()
            .sendLegacyMessage(player, "&c&lNO ACCESS! &7Only ranked players can use extra inventory.");
        releaseLock(uuid);
        return;
      }

      int requestedSlots = configManager.getExtraSlots(rank);
      if (requestedSlots <= 0) {
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin
              .getLogger()
              .info(player.getName() + " has no extra slots configured for rank " + rank);
        }
        releaseLock(uuid);
        return;
      }

      int inventorySize = ((requestedSlots + 8) / 9) * 9;
      if (inventorySize > 54) inventorySize = 54;

      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info(
                "Opening extra inventory for "
                    + player.getName()
                    + " (Rank "
                    + rank
                    + "): "
                    + requestedSlots
                    + " usable slots, "
                    + inventorySize
                    + " total slots, "
                    + (inventorySize - requestedSlots)
                    + " barriers");
      }

      Inventory existingInv = extraInventories.get(uuid);
      ItemStack[] oldContents = null;

      if (existingInv != null && existingInv.getSize() != inventorySize) {
        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin
              .getLogger()
              .info(
                  "Rank changed for "
                      + player.getName()
                      + ", recreating inventory (old size: "
                      + existingInv.getSize()
                      + ", new size: "
                      + inventorySize
                      + ")");
        }

        saveInventoryInternal(uuid, existingInv);

        oldContents = new ItemStack[existingInv.getSize()];
        for (int i = 0; i < existingInv.getSize(); i++) {
          ItemStack item = existingInv.getItem(i);
          if (item != null && item.getType() != Material.BARRIER) {
            oldContents[i] = item.clone();
          }
        }

        extraInventories.remove(uuid);
        existingInv = null;
      }

      final int finalRequestedSlots = requestedSlots;
      final int finalInventorySize = inventorySize;
      final ItemStack[] finalOldContents = oldContents;

      Inventory inv =
          extraInventories.computeIfAbsent(
              uuid,
              u -> {
                Inventory newInv =
                    Bukkit.createInventory(
                        null,
                        finalInventorySize, dev.mukulx.rankedsmpx.managers.MessageManager.legacy(plugin
                            .getMessageManager()
                            .applySmallCaps(
                                "&bExtra Inventory &7(" + finalRequestedSlots + " slots)")));

                if (finalOldContents != null) {
                  int transferSlots = Math.min(finalOldContents.length, finalRequestedSlots);
                  for (int i = 0; i < transferSlots; i++) {
                    if (finalOldContents[i] != null) {
                      newInv.setItem(i, finalOldContents[i]);
                    }
                  }
                  if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin
                        .getLogger()
                        .info("Transferred items from old inventory to new inventory");
                  }
                }

                if (finalRequestedSlots < finalInventorySize) {
                  ItemStack barrier = new ItemStack(Material.BARRIER);
                  ItemMeta meta = barrier.getItemMeta();
                  if (meta != null) {
                    meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(
                        plugin.getMessageManager().applySmallCaps("&c&lLOCKED SLOT")));
                    meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(
                        java.util.List.of(
                            plugin.getMessageManager().applySmallCaps("&7This slot is locked"),
                            plugin
                                .getMessageManager()
                                .applySmallCaps("&7Rank up to unlock more slots!"))));
                    barrier.setItemMeta(meta);
                  }

                  for (int i = finalRequestedSlots; i < finalInventorySize; i++) {
                    newInv.setItem(i, barrier);
                  }

                  if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin
                        .getLogger()
                        .info(
                            "Added "
                                + (finalInventorySize - finalRequestedSlots)
                                + " barrier blocks to slots "
                                + finalRequestedSlots
                                + "-"
                                + (finalInventorySize - 1));
                  }
                }

                loadInventory(u, newInv);

                return newInv;
              });

      deliverOverflow(player, inv);

      String checksum = calculateChecksum(inv);
      inventoryChecksums.put(uuid, checksum);

      // Release the lock before opening the inventory so the event handler does not block
      releaseLock(uuid);

      player.openInventory(inv);

    } catch (Exception e) {
      releaseLock(uuid);
      plugin
          .getLogger()
          .severe("Error opening inventory for " + player.getName() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Nullable
  public Inventory getExtraInventory(@NotNull UUID uuid) {
    return extraInventories.get(uuid);
  }

  public boolean hasExtraInventory(@NotNull Player player) {
    if (!configManager.isInventoryEnabled()) {
      return false;
    }

    Integer rank = rankManager.getRank(player.getUniqueId());
    return rank != null && configManager.getExtraSlots(rank) > 0;
  }

  public int getExtraSlots(@NotNull Player player) {
    Integer rank = rankManager.getRank(player.getUniqueId());
    if (rank == null) {
      return 0;
    }
    return configManager.getExtraSlots(rank);
  }

  public void dropExtraInventory(@NotNull Player player) {
    if (!plugin.getConfig().getBoolean("inventory.drop-on-death", true)) {
      return;
    }

    Inventory inv = extraInventories.get(player.getUniqueId());
    if (inv == null) {
      dropStoredInventory(player);
      return;
    }

    for (ItemStack item : inv.getContents()) {
      if (item != null && !item.getType().isAir() && item.getType() != Material.BARRIER) {
        player.getWorld().dropItemNaturally(player.getLocation(), item);
      }
    }
    dropOverflow(player);

    inv.clear();

    saveInventoryInternal(player.getUniqueId(), inv);

    Integer rank = rankManager.getRank(player.getUniqueId());
    if (rank != null) {
      int requestedSlots = configManager.getExtraSlots(rank);
      int inventorySize = inv.getSize();

      if (requestedSlots < inventorySize) {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
          meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(plugin.getMessageManager().applySmallCaps("&c&lLOCKED SLOT")));
          meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(
              java.util.List.of(
                  plugin.getMessageManager().applySmallCaps("&7This slot is locked"),
                  plugin.getMessageManager().applySmallCaps("&7Rank up to unlock more slots!"))));
          barrier.setItemMeta(meta);
        }

        for (int i = requestedSlots; i < inventorySize; i++) {
          inv.setItem(i, barrier);
        }
      }
    }
  }

  public void dropExtraInventoryOnRankRemoval(@NotNull Player player) {
    Inventory inv = extraInventories.get(player.getUniqueId());
    if (inv == null) {
      dropStoredInventory(player);
      return;
    }

    for (ItemStack item : inv.getContents()) {
      if (item != null && !item.getType().isAir() && item.getType() != Material.BARRIER) {
        player.getWorld().dropItemNaturally(player.getLocation(), item);
      }
    }
    dropOverflow(player);

    inv.clear();
    saveInventoryInternal(player.getUniqueId(), inv);

    extraInventories.remove(player.getUniqueId());

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info("Dropped extra inventory items for " + player.getName() + " (rank removed)");
    }
  }

  public void markForDropOnJoin(@NotNull UUID uuid) {
    String sql =
        plugin.getDatabaseManager().isMySql()
            ? "INSERT INTO extra_inventories (uuid, contents, last_updated, drop_on_join) VALUES (?, '', ?, 1) ON DUPLICATE KEY UPDATE drop_on_join=1"
            : "INSERT OR REPLACE INTO extra_inventories (uuid, contents, last_updated, drop_on_join) VALUES (?, '', ?, 1)";

    plugin.getDatabaseManager().executeUpdate(sql, uuid.toString(), System.currentTimeMillis());

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Marked extra inventory for drop on join for UUID: " + uuid);
    }
  }

  public boolean shouldDropOnJoin(@NotNull UUID uuid) {
    String sql = "SELECT drop_on_join FROM extra_inventories WHERE uuid = ?";
    try (var conn = plugin.getDatabaseManager().getConnection();
        var stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, uuid.toString());
      var rs = stmt.executeQuery();

      if (rs.next()) {
        return rs.getInt("drop_on_join") == 1;
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("Failed to check drop_on_join for " + uuid + ": " + e.getMessage());
    }
    return false;
  }

  public void dropExtraInventoryOnJoin(@NotNull Player player) {
    int droppedCount = dropStoredInventory(player);

    if (droppedCount > 0) {
      plugin
          .getMessageManager()
          .sendLegacyMessage(
              player,
              "&c&lRANK REMOVED! &7Your extra inventory items have been dropped ("
                  + droppedCount
                  + " items).");
    }

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin
          .getLogger()
          .info(
              "Dropped "
                  + droppedCount
                  + " extra inventory items for "
                  + player.getName()
                  + " on join (rank was removed while offline)");
    }
  }

  /**
   * Drop everything stored in the database for this player (including overflow) at their feet and
   * clear the stored contents. Used when the inventory isn't loaded in memory.
   *
   * @return number of item stacks dropped
   */
  private int dropStoredInventory(@NotNull Player player) {
    UUID uuid = player.getUniqueId();
    Inventory tempInv = Bukkit.createInventory(null, 54, dev.mukulx.rankedsmpx.managers.MessageManager.legacy("Temp"));
    loadInventory(uuid, tempInv);

    int droppedCount = 0;
    for (ItemStack item : tempInv.getContents()) {
      if (item != null && !item.getType().isAir() && item.getType() != Material.BARRIER) {
        player.getWorld().dropItemNaturally(player.getLocation(), item);
        droppedCount++;
      }
    }
    droppedCount += dropOverflow(player);

    plugin.getDatabaseManager().executeUpdate(
        "UPDATE extra_inventories SET contents = '', drop_on_join = 0 WHERE uuid = ?",
        uuid.toString());
    extraInventories.remove(uuid);
    return droppedCount;
  }

  private int dropOverflow(@NotNull Player player) {
    Map<Integer, ItemStack> overflow = undeliveredOverflow.remove(player.getUniqueId());
    if (overflow == null) {
      return 0;
    }
    for (ItemStack item : overflow.values()) {
      player.getWorld().dropItemNaturally(player.getLocation(), item);
    }
    return overflow.size();
  }

  /**
   * Give the owner the stored items that no longer fit their extra inventory (rank dropped), then
   * persist so they are no longer stored. Must run on the owner's thread.
   */
  private void deliverOverflow(@NotNull Player player, @NotNull Inventory inv) {
    Map<Integer, ItemStack> overflow = undeliveredOverflow.remove(player.getUniqueId());
    if (overflow == null || overflow.isEmpty()) {
      return;
    }
    for (ItemStack item : overflow.values()) {
      for (ItemStack leftover : player.getInventory().addItem(item).values()) {
        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
      }
    }
    saveInventoryInternal(player.getUniqueId(), inv);
    plugin
        .getMessageManager()
        .sendLegacyMessage(
            player,
            "&b&lEXTRA INVENTORY! &b"
                + overflow.size()
                + " &7item(s) no longer fit your rank's slots and were moved to your inventory.");
  }

  public void saveInventory(@NotNull UUID uuid) {
    long now = System.currentTimeMillis();
    Long lastSave = lastSaveTime.get(uuid);
    if (lastSave != null && now - lastSave < SAVE_DEBOUNCE) {
      return; // Skip duplicate save within 50ms
    }

    Inventory inv = extraInventories.get(uuid);
    if (inv == null) {
      return;
    }

    String oldChecksum = inventoryChecksums.get(uuid);
    String newChecksum = calculateChecksum(inv);

    if (oldChecksum != null && !oldChecksum.equals(newChecksum)) {
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Inventory changed for " + uuid + ", saving new state");
      }
    }

    saveInventoryInternal(uuid, inv);

    inventoryChecksums.put(uuid, newChecksum);
    lastSaveTime.put(uuid, now);
  }

  public void saveInventoryAsync(@NotNull UUID uuid) {
    Inventory inv = extraInventories.get(uuid);
    if (inv == null) {
      return;
    }

    // Copy on the calling thread; the async task must not read a live inventory.
    final Inventory invSnapshot = Bukkit.createInventory(null, inv.getSize());
    ItemStack[] contents = inv.getContents();
    for (int i = 0; i < contents.length; i++) {
      invSnapshot.setItem(i, contents[i] == null ? null : contents[i].clone());
    }

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          saveInventoryInternal(uuid, invSnapshot);
          if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Async saved inventory for " + uuid);
          }
        });
  }

  private void saveInventoryInternal(@NotNull UUID uuid, @NotNull Inventory inv) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

      int usableSize = 0;
      Integer rank = rankManager.getRank(uuid);
      if (rank != null) {
        usableSize = configManager.getExtraSlots(rank);
      }

      dataOutput.writeInt(inv.getSize());
      for (int i = 0; i < inv.getSize(); i++) {
        ItemStack item = inv.getItem(i);
        if (item != null && item.getType() != Material.BARRIER && !item.getType().isAir()) {
          dataOutput.writeInt(i);
          dataOutput.writeObject(item);
        }
      }
      // Keep undelivered overflow stored (past the end, so it never collides with a real slot)
      Map<Integer, ItemStack> overflow = undeliveredOverflow.get(uuid);
      if (overflow != null) {
        int overflowSlot = OVERFLOW_SLOT_BASE;
        for (ItemStack item : overflow.values()) {
          dataOutput.writeInt(overflowSlot++);
          dataOutput.writeObject(item);
        }
      }
      dataOutput.writeInt(-1);

      dataOutput.flush();
      String serialized = Base64.getEncoder().encodeToString(outputStream.toByteArray());

      String sql =
          plugin.getDatabaseManager().isMySql()
              ? "INSERT INTO extra_inventories (uuid, contents, last_updated) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE contents=VALUES(contents), last_updated=VALUES(last_updated)"
              : "INSERT OR REPLACE INTO extra_inventories (uuid, contents, last_updated) VALUES (?, ?, ?)";

      plugin
          .getDatabaseManager()
          .executeUpdate(sql, uuid.toString(), serialized, System.currentTimeMillis());
      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin
            .getLogger()
            .info("Saved extra inventory for " + uuid + " (" + serialized.length() + " bytes)");
      }

    } catch (Exception e) {
      plugin.getLogger().severe("Failed to save inventory for " + uuid + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  public void loadInventory(@NotNull UUID uuid, @NotNull Inventory targetInv) {
    Map<Integer, ItemStack> overflow = new LinkedHashMap<>();
    loadInventoryInto(uuid, targetInv, overflow);
    if (overflow.isEmpty()) {
      undeliveredOverflow.remove(uuid);
    } else {
      undeliveredOverflow.put(uuid, overflow);
    }
  }

  private static boolean placeOrOverflow(
      @NotNull Inventory inv, int slot, @NotNull ItemStack item, @NotNull Map<Integer, ItemStack> overflow) {
    if (slot >= 0 && slot < inv.getSize()) {
      ItemStack current = inv.getItem(slot);
      if (current == null || current.getType() != Material.BARRIER) {
        inv.setItem(slot, item);
        return false;
      }
    }
    overflow.put(slot, item);
    return true;
  }

  private void loadInventoryInto(
      @NotNull UUID uuid, @NotNull Inventory targetInv, @NotNull Map<Integer, ItemStack> overflow) {
    String sql = "SELECT contents FROM extra_inventories WHERE uuid = ?";
    try (var conn = plugin.getDatabaseManager().getConnection();
        var stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, uuid.toString());
      var rs = stmt.executeQuery();

      if (rs.next()) {
        String contents = rs.getString("contents");
        if (contents != null && !contents.isEmpty()) {
          try (ByteArrayInputStream inputStream =
                  new ByteArrayInputStream(Base64.getDecoder().decode(contents));
              BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            int savedSize = dataInput.readInt();
            int slot;
            int loadedCount = 0;
            while ((slot = dataInput.readInt()) != -1) {
              ItemStack item = (ItemStack) dataInput.readObject();
              if (!placeOrOverflow(targetInv, slot, item, overflow)) {
                loadedCount++;
              }
            }
            if (plugin.getConfig().getBoolean("debug", false)) {
              plugin
                  .getLogger()
                  .info("Loaded extra inventory for " + uuid + " (" + loadedCount + " items)");
            }
          } catch (Exception e) {
            plugin
                .getLogger()
                .warning("Failed to deserialize inventory for " + uuid + ": " + e.getMessage());
            if (!contents.contains("\n") && contents.contains(":")) {
              if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Attempting legacy load for " + uuid);
              }
              loadLegacyInventory(uuid, contents, targetInv, overflow);
            }
          }
        }
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to load inventory for " + uuid + ": " + e.getMessage());
    }
  }

  public void loadInventory(@NotNull UUID uuid) {
    Inventory inv = extraInventories.get(uuid);
    if (inv != null) {
      loadInventory(uuid, inv);
    }
  }

  private void loadLegacyInventory(
      @NotNull UUID uuid,
      String contents,
      @NotNull Inventory inv,
      @NotNull Map<Integer, ItemStack> overflow) {
    String[] items = contents.split(",");
    for (String itemData : items) {
      if (itemData.isEmpty()) continue;

      String[] parts = itemData.split(":");
      if (parts.length >= 3) {
        try {
          int slot = Integer.parseInt(parts[0]);
          Material material = Material.valueOf(parts[1]);
          int amount = Integer.parseInt(parts[2]);

          ItemStack item = new ItemStack(material, amount);
          if (parts.length > 4 && parts[3].equals("name")) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
              meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(parts[4].replace("\\:", ":")));
              item.setItemMeta(meta);
            }
          }

          placeOrOverflow(inv, slot, item, overflow);
        } catch (Exception e) {
          plugin.getLogger().warning("Failed to restore legacy item: " + itemData);
        }
      }
    }
  }

  public void removeExtraInventory(@NotNull UUID uuid) {
    Inventory inv = extraInventories.remove(uuid);
    if (inv != null) {
      // Callers drop the cache because the rank changed; the next open rebuilds it from the
      // database at the new size, so persist it now.
      saveInventoryInternal(uuid, inv);
      inventoryChecksums.remove(uuid);

      List<HumanEntity> viewers = new ArrayList<>(inv.getViewers());
      if (!viewers.isEmpty()) {
        retiredInventories.put(inv, uuid);
        for (HumanEntity viewer : viewers) {
          SchedulerUtil.runForEntity(plugin, viewer, viewer::closeInventory);
        }
      }
    }
    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Removed extra inventory for UUID: " + uuid);
    }
  }

  public boolean saveRetiredOnClose(@NotNull Inventory inv) {
    UUID owner = retiredInventories.remove(inv);
    if (owner == null) {
      return false;
    }
    saveInventoryInternal(owner, inv);
    return true;
  }

  public void saveAllInventories() {
    if (extraInventories.isEmpty()) return;

    if (plugin.getConfig().getBoolean("debug", false)) {
      plugin.getLogger().info("Saving all extra inventories (" + extraInventories.size() + ")...");
    }
    for (UUID uuid : extraInventories.keySet()) {
      saveInventory(uuid);
    }
  }

  public void clearInventoryOnQuit(@NotNull UUID uuid) {
    saveInventory(uuid);

    extraInventories.remove(uuid);
    undeliveredOverflow.remove(uuid);

    inventoryLocks.remove(uuid);
    lastSaveTime.remove(uuid);
    inventoryChecksums.remove(uuid);
  }

  public java.util.Set<UUID> getAllExtraInventoryUUIDs() {
    return extraInventories.keySet();
  }

  public void trackAdminViewing(
      @NotNull UUID adminUUID, @NotNull UUID targetUUID, @NotNull Inventory viewInventory) {
    adminViewingSessions.put(adminUUID, targetUUID);
    adminViewingInventories.put(adminUUID, viewInventory);
  }

  public void stopTrackingAdminViewing(@NotNull UUID adminUUID) {
    adminViewingSessions.remove(adminUUID);
    adminViewingInventories.remove(adminUUID);
  }

  public boolean isAdminViewing(@NotNull UUID adminUUID) {
    return adminViewingSessions.containsKey(adminUUID);
  }

  @Nullable
  public UUID getAdminViewingTarget(@NotNull UUID adminUUID) {
    return adminViewingSessions.get(adminUUID);
  }

  @Nullable
  public Inventory getAdminViewingInventory(@NotNull UUID adminUUID) {
    return adminViewingInventories.get(adminUUID);
  }

  public boolean isBeingViewedByAdmin(@NotNull UUID targetUUID) {
    return adminViewingSessions.containsValue(targetUUID);
  }

  @Nullable
  public UUID getAdminViewingPlayer(@NotNull UUID targetUUID) {
    for (var entry : adminViewingSessions.entrySet()) {
      if (entry.getValue().equals(targetUUID)) {
        return entry.getKey();
      }
    }
    return null;
  }

  public void saveAdminViewingChanges(@NotNull UUID adminUUID) {
    UUID targetUUID = adminViewingSessions.get(adminUUID);
    Inventory viewInv = adminViewingInventories.get(adminUUID);

    if (targetUUID == null || viewInv == null) {
      return;
    }

    synchronized (this) {
      if (isInventoryLocked(targetUUID)) {
        plugin
            .getLogger()
            .warning(
                "Cannot save admin viewing changes for "
                    + targetUUID
                    + " (inventory locked by another operation)");
        return;
      }

      if (!acquireLock(targetUUID)) {
        plugin
            .getLogger()
            .warning(
                "Cannot save admin viewing changes for "
                    + targetUUID
                    + " (failed to acquire lock)");
        return;
      }

      try {
        Inventory targetInv = extraInventories.get(targetUUID);
        if (targetInv == null) {
          Integer rank = rankManager.getRank(targetUUID);
          if (rank != null && rank > 0) {
            targetInv = createExtraInventory(targetUUID, rank);
          } else {
            plugin
                .getLogger()
                .warning(
                    "Cannot save admin viewing changes for "
                        + targetUUID
                        + " (no rank or invalid rank)");
            return;
          }
        }

        for (int i = 0; i < Math.min(viewInv.getSize(), targetInv.getSize()); i++) {
          ItemStack item = viewInv.getItem(i);
          ItemStack targetItem = targetInv.getItem(i);

          if (targetItem != null && targetItem.getType() == Material.BARRIER) {
            continue;
          }

          targetInv.setItem(i, item);
        }

        saveInventoryInternal(targetUUID, targetInv);

        if (plugin.getConfig().getBoolean("debug", false)) {
          plugin
              .getLogger()
              .info(
                  "Saved admin viewing changes for " + targetUUID + " (admin: " + adminUUID + ")");
        }
      } finally {
        releaseLock(targetUUID);
      }
    }
  }

  @NotNull
  public Inventory createExtraInventory(@NotNull UUID uuid, int rank) {
    int requestedSlots = configManager.getExtraSlots(rank);
    int inventorySize = ((requestedSlots + 8) / 9) * 9;
    if (inventorySize > 54) inventorySize = 54;

    Inventory inv =
        Bukkit.createInventory(
            null,
            inventorySize, dev.mukulx.rankedsmpx.managers.MessageManager.legacy(plugin
                .getMessageManager()
                .applySmallCaps("&bExtra Inventory &7(" + requestedSlots + " slots)")));

    if (requestedSlots < inventorySize) {
      ItemStack barrier = new ItemStack(Material.BARRIER);
      ItemMeta meta = barrier.getItemMeta();
      if (meta != null) {
        meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(plugin.getMessageManager().applySmallCaps("&c&lLOCKED SLOT")));
        meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(
            java.util.List.of(
                plugin.getMessageManager().applySmallCaps("&7This slot is locked"),
                plugin.getMessageManager().applySmallCaps("&7Rank up to unlock more slots!"))));
        barrier.setItemMeta(meta);
      }

      for (int i = requestedSlots; i < inventorySize; i++) {
        inv.setItem(i, barrier);
      }
    }

    extraInventories.put(uuid, inv);

    return inv;
  }

  public void saveInventoryFromMigration(@NotNull UUID uuid, @NotNull ItemStack[] items) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

      dataOutput.writeInt(items.length);
      for (int i = 0; i < items.length; i++) {
        ItemStack item = items[i];
        if (item != null && !item.getType().isAir()) {
          dataOutput.writeInt(i);
          dataOutput.writeObject(item);
        }
      }
      dataOutput.writeInt(-1);

      dataOutput.flush();
      String serialized = Base64.getEncoder().encodeToString(outputStream.toByteArray());

      String sql =
          plugin.getDatabaseManager().isMySql()
              ? "INSERT INTO extra_inventories (uuid, contents, last_updated, drop_on_join) VALUES (?, ?, ?, 0) ON DUPLICATE KEY UPDATE contents=VALUES(contents), last_updated=VALUES(last_updated)"
              : "INSERT OR REPLACE INTO extra_inventories (uuid, contents, last_updated, drop_on_join) VALUES (?, ?, ?, 0)";

      plugin
          .getDatabaseManager()
          .executeUpdate(sql, uuid.toString(), serialized, System.currentTimeMillis());

      if (plugin.getConfig().getBoolean("debug", false)) {
        plugin.getLogger().info("Migrated inventory for " + uuid);
      }

    } catch (Exception e) {
      plugin.getLogger().severe("Failed to migrate inventory for " + uuid + ": " + e.getMessage());
      e.printStackTrace();
    }
  }
}
