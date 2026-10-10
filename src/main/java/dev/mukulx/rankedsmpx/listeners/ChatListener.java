package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.managers.ConfigManager;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import dev.mukulx.rankedsmpx.managers.RankManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ChatListener implements Listener {

  private final RankedSMPXPlugin plugin;
  private final RankManager rankManager;
  private final MessageManager messageManager;
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  public ChatListener(
      @NotNull RankedSMPXPlugin plugin,
      @NotNull RankManager rankManager,
      @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.rankManager = rankManager;
    this.messageManager = plugin.getMessageManager();
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onPlayerChat(AsyncChatEvent event) {
    if (!plugin.isPluginEnabled() || !plugin.getConfig().getBoolean("rank-display.show-in-chat", false)) {
      return;
    }

    Player player = event.getPlayer();
    Component prefix = buildPrefix(player);

    event.renderer(
        (source, sourceDisplayName, message, viewer) ->
            Component.text()
                .append(prefix)
                .append(sourceDisplayName)
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(message)
                .build());
  }

  private Component buildPrefix(@NotNull Player player) {
    Integer rank = rankManager.getRank(player.getUniqueId());
    boolean blacklisted = plugin.getDatabaseManager().isBlacklisted(player.getUniqueId());
    String mm = messageManager.getRankPrefixMiniMessage(rank, blacklisted);
    if (!mm.endsWith("<reset>") && !mm.endsWith("<reset> ")) {
      if (mm.endsWith(" ")) {
        mm = mm.substring(0, mm.length() - 1) + "<reset> ";
      } else {
        mm = mm + "<reset>";
      }
    }
    return miniMessage.deserialize(mm);
  }
}
