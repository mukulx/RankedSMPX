package dev.mukulx.rankedsmpx.commands;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.InventoryManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InvCommand implements CommandExecutor {

  private final RankedSMPXPlugin plugin;
  private final InventoryManager inventoryManager;
  private final MessageManager messageManager;

  public InvCommand(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull InventoryManager inventoryManager,
      @NotNull MessageManager messageManager) {
    this.plugin = plugin;
    this.inventoryManager = inventoryManager;
    this.messageManager = messageManager;
  }

  @Override
  public boolean onCommand(
      @NotNull CommandSender sender,
      @NotNull Command command,
      @NotNull String label,
      @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(messageManager.getPrefix() + "§cOnly players can use this command!");
      return true;
    }

    if (!plugin.isPluginEnabled()) {
      player.sendMessage(messageManager.getPrefix() + "§cPlugin is currently disabled!");
      return true;
    }

    if (!inventoryManager.hasExtraInventory(player)) {
      player.sendMessage(
          messageManager.getPrefix() + "§cYou don't have access to extra inventory!");
      player.sendMessage(
          messageManager.getPrefix() + "§7Extra inventory is available for ranks 1-10.");
      return true;
    }

    inventoryManager.openExtraInventory(player);
    return true;
  }
}
