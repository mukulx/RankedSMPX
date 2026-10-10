package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public class RanksCommand implements CommandExecutor {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;

  public RanksCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!plugin.isPluginEnabled()) {
      sender.sendMessage(messageManager.getPrefix() + "§cPlugin is currently disabled!");
      sender.sendMessage(
          messageManager.getPrefix() + "§7An admin can re-enable it using: §b/rsmp enable");
      return true;
    }

    if (!(sender instanceof Player player)) {
      sender.sendMessage(messageManager.getPrefix() + "§cOnly players can use this command!");
      return true;
    }

    openRanksGUI(player);
    return true;
  }

  private void openRanksGUI(@NotNull Player player) {
    int shown = Math.max(1, Math.min(45, plugin.getConfigManager().getTotalRanks()));
    String title = "§6§lTᴏᴘ §b§l" + shown + " §6§lRᴀɴᴋᴇᴅ §b§lPʟᴀʏᴇʀs";
    Inventory gui = Bukkit.createInventory(null, 54, dev.mukulx.rankedsmpx.managers.MessageManager.legacy(title));

    List<Map.Entry<UUID, Integer>> topRanks = rankManager.getTopRanks(shown);

    if (topRanks.isEmpty()) {
      player.sendMessage(messageManager.getPrefix() + "§7No ranked players yet!");
      return;
    }

    int slot = 0;
    for (Map.Entry<UUID, Integer> entry : topRanks) {
      if (slot >= 45) break; // Max 45 slots for players (leave bottom row for decoration)

      UUID uuid = entry.getKey();
      int rank = entry.getValue();
      OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
      String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      if (meta != null) {
        meta.setOwningPlayer(offlinePlayer);

        String displayName = getRankColor(rank) + "§l#" + rank + " §r§f" + playerName;
        meta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(displayName));

        double hearts = plugin.getHealthManager().calculateHearts(rank);
        double potionMultiplier = plugin.getPotionManager().calculateMultiplier(rank);
        double xpMultiplier = plugin.getXPManager().calculateMultiplier(rank);
        int extraSlots = plugin.getConfigManager().getExtraSlots(rank);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Rank: §6#" + rank + " §7/ §b" + plugin.getConfigManager().getTotalRanks());
        lore.add("");
        lore.add("§b§lBuffs & Perks:");
        lore.add("  §cHearts: §f" + String.format("%.1f", hearts));
        lore.add("  §dPotion Duration: §f" + String.format("%.2fx", potionMultiplier));
        lore.add("  §aXP Multiplier: §f" + String.format("%.2fx", xpMultiplier));

        if (extraSlots > 0) {
          lore.add("  §bExtra Inventory: §f" + extraSlots + " slots");
        }

        lore.add("");
        if (offlinePlayer.isOnline()) {
          lore.add("§a● Online");
        } else {
          lore.add("§7● Offline");
        }

        meta.lore(dev.mukulx.rankedsmpx.managers.MessageManager.legacyLines(lore));
        head.setItemMeta(meta);
      }

      gui.setItem(slot, head);
      slot++;
    }

    ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
    ItemMeta fillerMeta = filler.getItemMeta();
    if (fillerMeta != null) {
      fillerMeta.displayName(dev.mukulx.rankedsmpx.managers.MessageManager.legacy(" "));
      filler.setItemMeta(fillerMeta);
    }

    for (int i = 45; i < 54; i++) {
      gui.setItem(i, filler);
    }

    player.openInventory(gui);
  }

  private String getRankColor(int rank) {
    if (rank == 1) return "§6"; // Gold
    if (rank == 2) return "§f"; // White (Silver)
    if (rank == 3) return "§6"; // Gold (Bronze - using gold as closest to bronze)
    if (rank <= 10) return "§b"; // Cyan
    return "§7"; // Gray
  }
}
