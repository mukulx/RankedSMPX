package dev.mukulx.rankedsmpx.locator;

import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LocatorBarAPI {

  private static LocatorBarManager manager;
  private static JavaPlugin plugin;
  private static AutoCloseable tickTaskHandle;
  private static volatile long updateIntervalTicks = 2L;
  private static volatile boolean loggingEnabled = true;

  public static void init(JavaPlugin hostingPlugin) {
    Objects.requireNonNull(hostingPlugin, "hostingPlugin cannot be null");
    if (manager != null) {
      return;
    }
    plugin = hostingPlugin;
    manager = new LocatorBarManager();
    manager.setLoggingEnabled(loggingEnabled);
    LocatorBarManager.set(manager);
    LocatorBarWaypoint.init();

    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
      LocatorBarFilter.inject(onlinePlayer);
    }

    Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(), hostingPlugin);
    tickTaskHandle =
        SchedulerUtil.runRepeatingCloseable(
            hostingPlugin, () -> manager.tick(hostingPlugin), 1L, updateIntervalTicks);
  }

  public static void setLoggingEnabled(boolean enabled) {
    loggingEnabled = enabled;
    if (manager != null) {
      manager.setLoggingEnabled(enabled);
    }
  }

  public static boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  public static boolean isAvailable() {
    return LocatorBarFilter.isAvailable();
  }

  public static JavaPlugin getPlugin() {
    return plugin;
  }

  public static void hideAll() {
    ensureInitialized();
    manager.setGlobalHidden(true);
  }

  public static void show(Player viewer) {
    ensureInitialized();
    Objects.requireNonNull(viewer, "viewer cannot be null");
    manager.setHidden(viewer, false);
  }

  public static void hide(Player viewer) {
    ensureInitialized();
    Objects.requireNonNull(viewer, "viewer cannot be null");
    manager.setHidden(viewer, true);
  }

  public static boolean isHidden(Player viewer) {
    ensureInitialized();
    Objects.requireNonNull(viewer, "viewer cannot be null");
    return manager.isHidden(viewer);
  }

  public static void clearTracking(Player holder) {
    ensureInitialized();
    Objects.requireNonNull(holder, "holder cannot be null");
    manager.clearTracking(holder);
  }

  public static boolean isTracking(Player holder) {
    ensureInitialized();
    Objects.requireNonNull(holder, "holder cannot be null");
    return manager.isTracking(holder);
  }

  public static void reset(Player player) {
    ensureInitialized();
    Objects.requireNonNull(player, "player cannot be null");
    manager.reset(player);
  }

  public static void revertAll() {
    ensureInitialized();
    manager.revertAll();
  }

  public static void shutdown() {
    if (tickTaskHandle != null) {
      SchedulerUtil.cancelTask(tickTaskHandle);
      tickTaskHandle = null;
    }
    LocatorBarFilter.removeAll();
    if (manager != null) {
      manager.revertAll();
      LocatorBarManager.set(null);
      manager = null;
    }
    plugin = null;
  }

  private static void ensureInitialized() {
    if (manager == null) {
      throw new IllegalStateException("LocatorBarAPI has not been initialized yet!");
    }
  }

  private LocatorBarAPI() {}
}
