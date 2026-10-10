package dev.mukulx.rankedsmpx.dialogs;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DialogSupport {

  private DialogSupport() {}

  static void put(@NotNull Map<String, String> values, @NotNull String path, @Nullable Object value) {
    if (value != null) {
      values.put(path, String.valueOf(value));
    }
  }

  /** Writes the values into their yml files (keeping comments) and reloads the plugin. */
  static boolean save(
      @NotNull RankedSMPXPlugin plugin, @NotNull Player player, @NotNull Map<String, String> values) {
    if (values.isEmpty()) {
      return true;
    }
    if (!plugin.getConfigUpdater().updateValues(values)) {
      player.sendMessage(
          Component.text("Settings could not be saved. Your config was left unchanged.", NamedTextColor.RED));
      return false;
    }
    plugin.reload();
    return true;
  }

  static void saved(@NotNull Player player, @NotNull String file) {
    player.sendMessage(Component.text("Settings saved to " + file + ".", NamedTextColor.GREEN));
  }
}
