package dev.mukulx.rankedsmpx.listeners;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.discord.DiscordLinkManager;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class DiscordLinkListener implements Listener {

  private final RankedSMPXPlugin plugin;

  public DiscordLinkListener(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (player == null) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("enabled", false)) {
      return;
    }

    if (!plugin.getDiscordConfig().getBoolean("linking.enabled", true)) {
      return;
    }

    if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isEnabled()) {
      return;
    }

    DiscordLinkManager linkManager = plugin.getDiscordManager().getLinkManager();
    if (linkManager == null) {
      return;
    }

    boolean isLinked = linkManager.isLinked(player.getUniqueId());

    if (isLinked) {
      linkManager.updateUsername(player.getUniqueId(), player.getName());
    }

    boolean linkingRequired = plugin.getDiscordConfig().getBoolean("linking.required", false);

    if (linkingRequired && !isLinked) {
      String linkingCode = linkManager.generateCode(player);

      String kickMessageTemplate =
          plugin
              .getDiscordConfig()
              .getString(
                  "linking.kick-message",
                  "&cYou must link your Discord account to play!\n&bYour linking code: &b{code}\n&7Use /link {code} in the Discord server to link your account.");

      final String kickMessage = kickMessageTemplate.replace("{code}", linkingCode);

      SchedulerUtil.runForEntity(
          plugin,
          player,
          () -> {
            if (player.isOnline()) {
              player.kick(Component.text(kickMessage.replace("&", "§")));
              plugin
                  .getLogger()
                  .info(
                      "Kicked "
                          + player.getName()
                          + " - Discord linking required (Code: "
                          + linkingCode
                          + ")");
            }
          });

      return;
    }

    if (!isLinked && plugin.getDiscordConfig().getBoolean("linking.remind-on-join", true)) {
      String reminderMessage =
          plugin
              .getDiscordConfig()
              .getString(
                  "linking.reminder-message",
                  "&b&l[!] &7Link your Discord account with &a/linkdiscord &7for exclusive features!");

      SchedulerUtil.runForEntityLater(
          plugin,
          player,
          () -> {
            if (player.isOnline()) {
              player.sendMessage(Component.text(reminderMessage.replace("&", "§")));
            }
          },
          60L);
    }
  }
}
