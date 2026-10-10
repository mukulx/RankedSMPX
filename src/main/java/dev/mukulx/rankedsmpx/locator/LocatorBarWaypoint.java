package dev.mukulx.rankedsmpx.locator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LocatorBarWaypoint {

  private static boolean initialized = false;
  private static boolean available = false;
  private static String failReason = "";

  private static Class<?> packetClass;
  private static Class<?> iconClass;
  private static Object nullIcon;
  private static Constructor<?> iconCtor;
  private static Constructor<?> icon2ArgCtor;
  private static Field iconColorField;
  private static Field iconStyleField;
  private static Object defaultStyle;

  private static Class<?> vec3iClass;
  private static Constructor<?> vec3iCtor;
  private static Class<?> trackedWaypointClass;
  private static Method setPosition;
  private static Method emptyWaypoint;
  private static Class<?> opEnumClass;
  private static Object opTrack;
  private static Object opUpdate;
  private static Object opUntrack;
  private static Constructor<?> packetCtor;

  private static Method getHandle;
  private static Field connectionField;
  private static Method sendPacket;

  public static void init() {
    if (initialized) {
      return;
    }
    initialized = true;
    try {
      packetClass =
          Class.forName("net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket");
      iconClass = Class.forName("net.minecraft.world.waypoints.Waypoint$Icon");
      try {
        nullIcon = iconClass.getField("NULL").get(null);
      } catch (Throwable ignored) {
      }

      try {
        iconCtor = iconClass.getConstructor();
      } catch (Throwable ignored) {
      }

      for (Constructor<?> c : iconClass.getConstructors()) {
        if (c.getParameterCount() == 2) {
          icon2ArgCtor = c;
          break;
        }
      }

      try {
        iconColorField = iconClass.getField("color");
        iconColorField.setAccessible(true);
      } catch (Throwable ignored) {
      }

      try {
        iconStyleField = iconClass.getField("style");
        iconStyleField.setAccessible(true);
      } catch (Throwable ignored) {
      }

      try {
        Class<?> waypointStyleAssetsClass =
            Class.forName("net.minecraft.world.waypoints.WaypointStyleAssets");
        defaultStyle = waypointStyleAssetsClass.getField("DEFAULT").get(null);
      } catch (Throwable ignored) {
      }

      vec3iClass = Class.forName("net.minecraft.core.Vec3i");
      vec3iCtor = vec3iClass.getConstructor(int.class, int.class, int.class);

      trackedWaypointClass = Class.forName("net.minecraft.world.waypoints.TrackedWaypoint");
      setPosition =
          trackedWaypointClass.getMethod("setPosition", UUID.class, iconClass, vec3iClass);
      emptyWaypoint = trackedWaypointClass.getMethod("empty", UUID.class);

      opEnumClass =
          Class.forName(
              "net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket$Operation");
      opTrack = resolveEnum(opEnumClass, new String[] {"TRACK", "ADD", "TRACK_POSITION", "TRACK_POS"});
      opUpdate = resolveEnum(opEnumClass, new String[] {"UPDATE", "UPDATE_POS"});
      opUntrack =
          resolveEnum(opEnumClass, new String[] {"UNTRACK", "REMOVE", "UNTrack", "REMOVE_WAYPOINT"});

      if (opTrack == null || opUpdate == null || opUntrack == null) {
        throw new IllegalStateException(
            "Could not resolve ClientboundTrackedWaypointPacket$Operation constants");
      }

      packetCtor = packetClass.getConstructor(opEnumClass, trackedWaypointClass);

      String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
      Class<?> craftPlayerClass = Class.forName(serverPackage + ".entity.CraftPlayer");
      getHandle = craftPlayerClass.getMethod("getHandle");
      Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
      connectionField = serverPlayerClass.getField("connection");
      Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");

      for (Method method : connectionField.getType().getMethods()) {
        if (method.getName().equals("send")
            && method.getParameterCount() == 1
            && packetInterface.isAssignableFrom(method.getParameterTypes()[0])) {
          sendPacket = method;
          break;
        }
      }

      if (sendPacket == null) {
        throw new IllegalStateException("Could not resolve connection.send(Packet)");
      }

      available = true;
    } catch (Throwable t) {
      available = false;
      failReason = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }
  }

  private static Object resolveEnum(Class<?> enumClass, String[] candidates) {
    for (Object constant : enumClass.getEnumConstants()) {
      for (String name : candidates) {
        if (((Enum<?>) constant).name().equalsIgnoreCase(name)) {
          return constant;
        }
      }
    }
    return null;
  }

  public static boolean isAvailable() {
    init();
    return available;
  }

  public static String getFailReason() {
    return failReason;
  }

  public static Object createIcon(Integer rgbColor) {
    if (rgbColor == null) {
      if (nullIcon != null) return nullIcon;
    }
    if (icon2ArgCtor != null) {
      Class<?>[] pTypes = icon2ArgCtor.getParameterTypes();
      Object optColor = rgbColor != null ? Optional.of(rgbColor) : Optional.empty();
      Object optStyle = defaultStyle != null ? Optional.of(defaultStyle) : Optional.empty();

      try {
        Object a0 = pTypes[0].equals(Optional.class) ? optStyle : defaultStyle;
        Object a1 = pTypes[1].equals(Optional.class) ? optColor : rgbColor;
        return icon2ArgCtor.newInstance(a0, a1);
      } catch (Throwable ignored) {
      }

      try {
        Object a0 = pTypes[0].equals(Optional.class) ? optColor : rgbColor;
        Object a1 = pTypes[1].equals(Optional.class) ? optStyle : defaultStyle;
        return icon2ArgCtor.newInstance(a0, a1);
      } catch (Throwable ignored) {
      }
    }
    if (iconCtor != null) {
      try {
        Object icon = iconCtor.newInstance();
        if (iconColorField != null) {
          if (iconColorField.getType().equals(Optional.class)) {
            iconColorField.set(icon, rgbColor != null ? Optional.of(rgbColor) : Optional.empty());
          } else {
            iconColorField.set(icon, rgbColor);
          }
        }
        if (iconStyleField != null && defaultStyle != null) {
          if (iconStyleField.getType().equals(Optional.class)) {
            iconStyleField.set(icon, Optional.of(defaultStyle));
          } else {
            iconStyleField.set(icon, defaultStyle);
          }
        }
        return icon;
      } catch (Throwable ignored) {
      }
    }
    return nullIcon;
  }

  private static Object buildPacket(Object op, UUID id, int x, int y, int z, Object icon)
      throws Exception {
    Object vec = vec3iCtor.newInstance(x, y, z);
    Object waypoint = setPosition.invoke(null, id, icon != null ? icon : nullIcon, vec);
    return packetCtor.newInstance(op, waypoint);
  }

  private static void send(Player viewer, Object packet) {
    try {
      Object serverPlayer = getHandle.invoke(viewer);
      Object connection = connectionField.get(serverPlayer);
      sendPacket.invoke(connection, packet);
    } catch (Throwable ignored) {
    }
  }

  public static void track(Player viewer, UUID id, int x, int y, int z) {
    init();
    if (!available) return;
    try {
      Object icon = createIcon(null);
      send(viewer, buildPacket(opTrack, id, x, y, z, icon));
    } catch (Throwable ignored) {
    }
  }

  public static void update(Player viewer, UUID id, int x, int y, int z) {
    init();
    if (!available) return;
    try {
      Object icon = createIcon(null);
      send(viewer, buildPacket(opUpdate, id, x, y, z, icon));
    } catch (Throwable ignored) {
    }
  }

  public static void untrack(Player viewer, UUID id) {
    init();
    if (!available) return;
    try {
      Object waypoint = null;
      if (emptyWaypoint != null) {
        try {
          waypoint = emptyWaypoint.invoke(null, id);
        } catch (Throwable ignored) {
        }
      }
      if (waypoint == null && setPosition != null) {
        Object zeroVec = vec3iCtor.newInstance(0, 0, 0);
        waypoint = setPosition.invoke(null, id, nullIcon != null ? nullIcon : createIcon(null), zeroVec);
      }
      if (waypoint != null && packetCtor != null && opUntrack != null) {
        send(viewer, packetCtor.newInstance(opUntrack, waypoint));
      }
    } catch (Throwable ignored) {
    }
  }

  private LocatorBarWaypoint() {}
}
