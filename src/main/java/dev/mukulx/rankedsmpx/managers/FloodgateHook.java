package dev.mukulx.rankedsmpx.managers;

import java.util.UUID;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;

/** Only loaded once Floodgate is known to be installed. */
final class FloodgateHook {

  private FloodgateHook() {}

  @NotNull
  static String prefix() {
    return FloodgateApi.getInstance().getPlayerPrefix();
  }

  static boolean isBedrock(@NotNull UUID uuid) {
    return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
  }
}
