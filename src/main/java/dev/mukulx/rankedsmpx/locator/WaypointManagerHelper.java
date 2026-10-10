package dev.mukulx.rankedsmpx.locator;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class WaypointManagerHelper {

  private static Class<?> serverLevelClass;
  private static Class<?> waypointManagerClass;
  private static Method getWaypointManager;
  private static Method removePlayer;
  private static Method addPlayer;
  private static Method breakAllConnections;
  private static Method craftGetHandleWorld;
  private static Method craftGetHandlePlayer;
  private static boolean resolved = false;
  private static boolean available = false;

  private static void resolve() {
    if (resolved) {
      return;
    }
    resolved = true;
    try {
      String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
      Class<?> craftWorldClass = Class.forName(serverPackage + ".CraftWorld");
      craftGetHandleWorld = craftWorldClass.getMethod("getHandle");
      Class<?> craftPlayerClass = Class.forName(serverPackage + ".entity.CraftPlayer");
      craftGetHandlePlayer = craftPlayerClass.getMethod("getHandle");

      serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");
      waypointManagerClass = Class.forName("net.minecraft.server.waypoints.ServerWaypointManager");
      getWaypointManager = serverLevelClass.getMethod("getWaypointManager");
      Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
      removePlayer = waypointManagerClass.getMethod("removePlayer", serverPlayerClass);
      addPlayer = waypointManagerClass.getMethod("addPlayer", serverPlayerClass);
      breakAllConnections = waypointManagerClass.getMethod("breakAllConnections");
      available = true;
    } catch (Throwable t) {
      available = false;
    }
  }

  static boolean isAvailable() {
    resolve();
    return available;
  }

  private static Object waypointManager(World world) {
    try {
      Object serverLevel = craftGetHandleWorld.invoke(world);
      return getWaypointManager.invoke(serverLevel);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Object serverPlayer(Player player) {
    try {
      return craftGetHandlePlayer.invoke(player);
    } catch (Throwable ignored) {
      return null;
    }
  }

  static void hidePlayer(Player player) {
    resolve();
    if (!available) return;
    try {
      Object sp = serverPlayer(player);
      if (sp == null) return;
      Object manager = waypointManager(player.getWorld());
      if (manager == null) return;
      removePlayer.invoke(manager, sp);
    } catch (Throwable ignored) {
    }
  }

  static void showPlayer(Player player) {
    resolve();
    if (!available) return;
    try {
      Object sp = serverPlayer(player);
      if (sp == null) return;
      Object manager = waypointManager(player.getWorld());
      if (manager == null) return;
      addPlayer.invoke(manager, sp);
    } catch (Throwable ignored) {
    }
  }

  static void hideAll() {
    resolve();
    if (!available) return;
    for (World world : Bukkit.getWorlds()) {
      try {
        Object manager = waypointManager(world);
        if (manager != null) {
          breakAllConnections.invoke(manager);
        }
      } catch (Throwable ignored) {
      }
    }
  }

  static void showAllPlayers() {
    resolve();
    if (!available) return;
    for (Player player : Bukkit.getOnlinePlayers()) {
      showPlayer(player);
    }
  }

  private WaypointManagerHelper() {}
}
