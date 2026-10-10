package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MigrateCommand {

  private final RankedSMPXPlugin plugin;
  private final Map<String, PendingMigration> pendingMigrations = new HashMap<>();

  public MigrateCommand(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void execute(@NotNull CommandSender sender) {
    String senderKey = sender.getName();

    PendingMigration pending = pendingMigrations.get(senderKey);
    if (pending != null) {
      pendingMigrations.remove(senderKey);
      sender.sendMessage("§b§lᴍɪɢʀᴀᴛɪɴɢ... §7Starting migration process...");

      SchedulerUtil.runAsync(
          plugin,
          () -> {
            try {
              performMigration(pending.data);

              SchedulerUtil.run(
                  plugin,
                  () -> {
                    showMigrationSuccess(sender, pending.data);
                  });

            } catch (Exception e) {
              SchedulerUtil.run(
                  plugin,
                  () -> {
                    sender.sendMessage("§c§lꜰᴀɪʟᴇᴅ! §7Migration failed: " + e.getMessage());
                    plugin.getLogger().severe("Migration failed: " + e.getMessage());
                    e.printStackTrace();
                  });
            }
          });
      return;
    }

    File oldPluginFolder = new File(plugin.getDataFolder().getParentFile(), "RankedSMP");
    if (!oldPluginFolder.exists()) {
      sender.sendMessage("§c§lɴᴏᴛ ꜰᴏᴜɴᴅ! §7Old RankedSMP plugin data not found.");
      sender.sendMessage("§7Expected location: §b" + oldPluginFolder.getAbsolutePath());
      return;
    }

    if (Bukkit.getPluginManager().getPlugin("RankedSMP") != null) {
      sender.sendMessage("§c§lᴇʀʀᴏʀ! §7Old RankedSMP plugin is still loaded!");
      sender.sendMessage("§7Please disable/remove the old plugin first, then restart the server.");
      return;
    }

    sender.sendMessage("§b§lꜱᴄᴀɴɴɪɴɢ... §7Analyzing old RankedSMP data...");

    SchedulerUtil.runAsync(
        plugin,
        () -> {
          try {
            MigrationData data = scanOldData(oldPluginFolder);

            SchedulerUtil.run(
                plugin,
                () -> {
                  showMigrationPreview(sender, data);
                });

          } catch (Exception e) {
            SchedulerUtil.run(
                plugin,
                () -> {
                  sender.sendMessage("§c§lᴇʀʀᴏʀ! §7Failed to scan old data: " + e.getMessage());
                  plugin.getLogger().severe("Migration scan failed: " + e.getMessage());
                  e.printStackTrace();
                });
          }
        });
  }

  private MigrationData scanOldData(@NotNull File oldPluginFolder) throws Exception {
    MigrationData data = new MigrationData();

    File configFile = new File(oldPluginFolder, "config.yml");
    if (configFile.exists()) {
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
      ConfigurationSection playersSection = config.getConfigurationSection("players");

      if (playersSection != null) {
        for (String uuidStr : playersSection.getKeys(false)) {
          try {
            UUID uuid = UUID.fromString(uuidStr);
            ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
            if (playerSection != null && playerSection.contains("rank")) {
              int rank = playerSection.getInt("rank", 0);
              if (rank > 0 && rank <= plugin.getConfigManager().getTotalRanks()) {
                data.ranks.put(uuid, rank);
              }
            }
          } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid UUID in old config: " + uuidStr);
          }
        }
      }
    }

    File dbFile = new File(oldPluginFolder, "data.db");
    if (dbFile.exists()) {
      String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
      try (Connection conn = DriverManager.getConnection(url);
          PreparedStatement stmt =
              conn.prepareStatement("SELECT uuid, inventory, slots FROM extra_inventories");
          ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
          try {
            UUID uuid = UUID.fromString(rs.getString("uuid"));
            String inventoryData = rs.getString("inventory");
            int slots = rs.getInt("slots");

            data.inventories.put(uuid, new InventoryData(inventoryData, slots));
          } catch (Exception e) {
            plugin.getLogger().warning("Failed to read inventory: " + e.getMessage());
          }
        }
      }
    }

    return data;
  }

  private void showMigrationPreview(@NotNull CommandSender sender, @NotNull MigrationData data) {
    sender.sendMessage("");
    sender.sendMessage("§6§lᴍɪɢʀᴀᴛɪᴏɴ ᴘʀᴇᴠɪᴇᴡ");
    sender.sendMessage("");
    sender.sendMessage("§7Found in old RankedSMP plugin:");
    sender.sendMessage("  §b" + data.ranks.size() + " §7ranked players");
    sender.sendMessage("  §b" + data.inventories.size() + " §7extra inventories");
    sender.sendMessage("");

    if (data.ranks.isEmpty() && data.inventories.isEmpty()) {
      sender.sendMessage("§c§lɴᴏ ᴅᴀᴛᴀ! §7No data found to migrate.");
      sender.sendMessage("§7The old plugin folder exists but contains no player data.");
      return;
    }

    sender.sendMessage("§b§lᴡᴀʀɴɪɴɢ:");
    sender.sendMessage("§7• This will §cOVERWRITE §7any existing ranks in RankedSMPX");
    sender.sendMessage("§7• Extra inventories will be §aMERGED §7(no data loss)");
    sender.sendMessage("§7• This action §cCANNOT BE UNDONE");
    sender.sendMessage("");

    String senderKey = sender.getName();
    pendingMigrations.put(senderKey, new PendingMigration(data));

    sender.sendMessage("§7Run the command again to confirm:");
    sender.sendMessage("§b/rsmp migrate");
    sender.sendMessage("");
    sender.sendMessage("§8(This confirmation expires in 60 seconds)");

    SchedulerUtil.runLater(
        plugin,
        () -> {
          if (pendingMigrations.remove(senderKey) != null) {
            sender.sendMessage("§c§lᴇxᴘɪʀᴇᴅ! §7Migration confirmation expired.");
          }
        },
        1200L); // 60 seconds
  }

  public void confirmMigration(@NotNull CommandSender sender, @NotNull String confirmId) {
    sender.sendMessage("§7Please use §b/rsmp migrate §7to start migration.");
  }

  private void performMigration(@NotNull MigrationData data) throws Exception {
    SchedulerUtil.run(
        plugin,
        () -> {
          if (plugin.getGameManager().isGameActive()
              || !plugin.getRankManager().getAllRanks().isEmpty()) {
            plugin.getGameManager().stopGame();
            plugin.getLogger().info("Stopped existing game before migration");
          }
        });

    Thread.sleep(1000);

    int ranksImported = 0;
    int inventoriesImported = 0;

    for (Map.Entry<UUID, Integer> entry : data.ranks.entrySet()) {
      UUID uuid = entry.getKey();
      int rank = entry.getValue();

      plugin.getRankManager().setRank(uuid, rank);

      org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
      String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

      plugin
          .getDatabaseManager()
          .addRankHistory(uuid, playerName, rank, "GAINED", "Migrated from old RankedSMP plugin");

      org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
      if (player != null && player.isOnline()) {
        if (plugin.getTabIntegration() != null) {
          plugin.getTabIntegration().updatePlayer(player);
        }

        if (plugin.getVanillaTabManager() != null) {
          plugin.getVanillaTabManager().updatePlayer(player);
        }

        plugin.getHealthManager().applyHealth(player);
      }

      ranksImported++;
    }

    for (Map.Entry<UUID, InventoryData> entry : data.inventories.entrySet()) {
      UUID uuid = entry.getKey();
      InventoryData invData = entry.getValue();

      try {
        ItemStack[] items = deserializeOldInventory(invData.data);

        plugin.getInventoryManager().saveInventoryFromMigration(uuid, items);

        inventoriesImported++;
      } catch (Exception e) {
        plugin
            .getLogger()
            .warning("Failed to migrate inventory for " + uuid + ": " + e.getMessage());
      }
    }

    plugin
        .getLogger()
        .info(
            "Migration completed: "
                + ranksImported
                + " ranks, "
                + inventoriesImported
                + " inventories");

    if (ranksImported > 0 && !plugin.getGameManager().isGameActive()) {
      plugin.getGameManager().setGameActive(true);
      plugin.getLogger().info("Game activated after migrating " + ranksImported + " ranks");
    }
  }

  private ItemStack[] deserializeOldInventory(@NotNull String data) throws Exception {
    String yaml = new String(Base64.getDecoder().decode(data));
    YamlConfiguration config = new YamlConfiguration();
    config.loadFromString(yaml);

    if (!config.contains("items")) {
      return new ItemStack[0];
    }

    ConfigurationSection itemsSection = config.getConfigurationSection("items");
    if (itemsSection == null) {
      return new ItemStack[0];
    }

    int maxSlot = 0;
    for (String key : itemsSection.getKeys(false)) {
      try {
        int slot = Integer.parseInt(key);
        if (slot > maxSlot) {
          maxSlot = slot;
        }
      } catch (NumberFormatException e) {
      }
    }

    ItemStack[] items = new ItemStack[maxSlot + 1];
    for (String key : itemsSection.getKeys(false)) {
      try {
        int slot = Integer.parseInt(key);
        items[slot] = config.getItemStack("items." + key);
      } catch (NumberFormatException e) {
      }
    }

    return items;
  }

  private void showMigrationSuccess(@NotNull CommandSender sender, @NotNull MigrationData data) {
    sender.sendMessage("");
    sender.sendMessage("§a§lᴍɪɢʀᴀᴛɪᴏɴ ᴄᴏᴍᴘʟᴇᴛᴇ!");
    sender.sendMessage("");
    sender.sendMessage("§7Successfully migrated:");
    sender.sendMessage("  §a§b" + data.ranks.size() + " §7ranked players");
    sender.sendMessage("  §a§b" + data.inventories.size() + " §7extra inventories");
    sender.sendMessage("");
    sender.sendMessage("§aAll changes have been applied instantly!");
    sender.sendMessage("§7Use §b/ranks §7and §b/extrainv §7to verify the migration.");
    sender.sendMessage("");
  }

  private static class MigrationData {
    final Map<UUID, Integer> ranks = new HashMap<>();
    final Map<UUID, InventoryData> inventories = new HashMap<>();
  }

  private static class InventoryData {
    final String data;
    final int slots;

    InventoryData(String data, int slots) {
      this.data = data;
      this.slots = slots;
    }
  }

  private static class PendingMigration {
    final MigrationData data;
    final long timestamp;

    PendingMigration(MigrationData data) {
      this.data = data;
      this.timestamp = System.currentTimeMillis();
    }
  }
}
