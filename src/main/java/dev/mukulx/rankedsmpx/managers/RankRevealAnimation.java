package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankRevealAnimation {

  private final RankedSMPXPlugin plugin;
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  private final ConcurrentHashMap<UUID, Object> activeAnimations = new ConcurrentHashMap<>();

  public RankRevealAnimation(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void startAnimation(@NotNull Player player, int finalRank) {
    cancelAnimation(player.getUniqueId());

    List<String> rankPrefixes = new ArrayList<>();
    RankTable ranks = plugin.getConfigManager().getRanks();
    rankPrefixes.add(convertToMiniMessage(ranks.unranked().prefix()));
    for (int i = 1; i <= ranks.totalRanks(); i++) {
      rankPrefixes.add(convertToMiniMessage(ranks.row(i).prefix()));
    }

    int totalSpins = 40; // Total number of frames
    int ticksPerFrame = 2; // Ticks between each frame (2 ticks = 0.1 seconds)
    int slowdownStart = 30; // When to start slowing down

    int finalFrame = totalSpins - 1;

    Runnable animation =
        new Runnable() {
              int currentFrame = 0;
              int currentDelay = ticksPerFrame;

              @Override
              public void run() {
                if (currentFrame >= totalSpins) {
                  player.playSound(
                      player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                  cancelAnimation(player.getUniqueId());
                  return;
                }

                int displayIndex;
                if (currentFrame < finalFrame) {
                  displayIndex = currentFrame % rankPrefixes.size();
                } else {
                  displayIndex = finalRank; // finalRank maps to list index finalRank
                }

                String prefix = rankPrefixes.get(displayIndex);

                showSpinningTitle(player, prefix, currentFrame, finalFrame);

                if (currentFrame < finalFrame) {
                  float pitch = 0.8f + (currentFrame * 0.02f); // Gradually increase pitch
                  player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, pitch);
                } else {
                  player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                  plugin
                      .getMessageManager()
                      .sendLegacyMessage(
                          player, "&a&l&aYou have been assigned rank &6#" + finalRank + "&a!");
                }

                currentFrame++;

                if (currentFrame >= slowdownStart) {
                  int framesFromEnd = totalSpins - currentFrame;
                  if (framesFromEnd <= 5) {
                    currentDelay = ticksPerFrame + (6 - framesFromEnd) * 3;
                  }
                }
              }
        };
    Object task =
        SchedulerUtil.runTimerForEntity(
            plugin, player, ignored -> animation.run(), 1L, ticksPerFrame);

    activeAnimations.put(player.getUniqueId(), task);
  }

  private void showSpinningTitle(
      @NotNull Player player, @NotNull String prefix, int currentFrame, int finalFrame) {
    Component title;
    Component subtitle;

    if (currentFrame < finalFrame) {
      String titleText = "<gradient:#FFD700:#FFA500><bold>ASSIGNING RANK...</bold></gradient>";
      title = miniMessage.deserialize(titleText);
      subtitle = miniMessage.deserialize(prefix);
    } else {
      String titleText = "<gradient:#00FF00:#00AA00><bold>YOUR RANK</bold></gradient>";
      title = miniMessage.deserialize(titleText);
      subtitle = miniMessage.deserialize(prefix);
    }

    int fadeIn, stay, fadeOut;
    if (currentFrame < finalFrame) {
      fadeIn = 0; // instant
      stay = 8; // 0.4 seconds (8 ticks)
      fadeOut = 2; // 0.1 seconds (2 ticks)
    } else {
      fadeIn = 0; // instant
      stay = 70; // 3.5 seconds (70 ticks)
      fadeOut = 10; // 0.5 seconds (10 ticks)
    }

    if (!plugin.getMessageManager().titlesEnabled()) {
      return;
    }

    player.showTitle(
        Title.title(
            title,
            subtitle,
            Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L))));
  }

  private String convertToMiniMessage(@NotNull String text) {
    text = text.replaceAll("&#([0-9A-Fa-f]{6})", "<#$1>");

    text = text.replace("&0", "<black>");
    text = text.replace("&1", "<dark_blue>");
    text = text.replace("&2", "<dark_green>");
    text = text.replace("&3", "<dark_aqua>");
    text = text.replace("&4", "<dark_red>");
    text = text.replace("&5", "<dark_purple>");
    text = text.replace("&6", "<gold>");
    text = text.replace("&7", "<gray>");
    text = text.replace("&8", "<dark_gray>");
    text = text.replace("&9", "<blue>");
    text = text.replace("&a", "<green>");
    text = text.replace("&b", "<aqua>");
    text = text.replace("&c", "<red>");
    text = text.replace("&d", "<light_purple>");
    text = text.replace("&e", "<yellow>");
    text = text.replace("&f", "<white>");
    text = text.replace("&r", "<reset>");
    text = text.replace("&l", "<bold>");
    text = text.replace("&o", "<italic>");
    text = text.replace("&n", "<underlined>");
    text = text.replace("&m", "<strikethrough>");
    text = text.replace("&k", "<obfuscated>");

    text = text.replace("[", "").replace("]", "");

    return text;
  }

  public void cancelAnimation(@NotNull UUID uuid) {
    Object task = activeAnimations.remove(uuid);
    if (task != null) {
      SchedulerUtil.cancelTask(task);
    }
  }

  public void cancelAllAnimations() {
    for (Object task : activeAnimations.values()) {
      SchedulerUtil.cancelTask(task);
    }
    activeAnimations.clear();
  }
}
