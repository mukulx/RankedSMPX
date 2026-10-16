package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BedrockSupport {

  private final RankedSMPXPlugin plugin;
  private boolean enabled;
  private boolean floodgate;
  private String prefix;

  public BedrockSupport(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void initialize() {
    enabled = plugin.getConfig().getBoolean("bedrock.enabled", true);
    floodgate = false;
    prefix = null;

    PluginManager plugins = Bukkit.getPluginManager();
    boolean hasFloodgate = plugins.getPlugin("floodgate") != null;
    boolean hasGeyser = plugins.getPlugin("Geyser-Spigot") != null;
    if (!hasFloodgate && !hasGeyser) {
      return;
    }
    if (!enabled) {
      plugin
          .getLogger()
          .info("GeyserMC detected, but bedrock.enabled is false in config.yml - Bedrock support is off");
      return;
    }
    if (!hasFloodgate) {
      plugin
          .getLogger()
          .info(
              "GeyserMC detected without Floodgate - Bedrock players join with Java accounts,"
                  + " nothing extra needed");
      return;
    }

    try {
      prefix = FloodgateHook.prefix();
      floodgate = true;
    } catch (Throwable t) {
      plugin.getLogger().warning("Floodgate detected, but its API could not be loaded: " + t);
      return;
    }
    plugin
        .getLogger()
        .info(
            "GeyserMC (Floodgate) detected - Bedrock support enabled, username prefix \""
                + prefix
                + "\"");
    if (prefix.isEmpty()) {
      plugin
          .getLogger()
          .warning(
              "Floodgate username-prefix is empty, so Bedrock and Java players can share a name."
                  + " Use UUIDs in commands when that happens.");
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  /** The Floodgate username prefix, or null when Floodgate is not hooked. */
  @Nullable
  public String getPrefix() {
    return prefix;
  }

  public boolean isBedrock(@NotNull Player player) {
    if (!floodgate) {
      return false;
    }
    try {
      return FloodgateHook.isBedrock(player.getUniqueId());
    } catch (Throwable t) {
      return false;
    }
  }
}
