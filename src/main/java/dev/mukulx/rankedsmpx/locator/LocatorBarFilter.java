package dev.mukulx.rankedsmpx.locator;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class LocatorBarFilter {

  static final String HANDLER_NAME = "rankedsmpx-locator-encoder";

  private static Class<?> craftPlayerClass;
  private static Method getHandle;
  private static Field spConnectionField;
  private static Field connChannelField;
  private static Field channelField;

  private static Class<?> waypointPacketClass;
  private static Method packetWaypoint;
  private static Method packetOperation;
  private static Class<?> opEnumClass;
  private static Object opUntrack;
  private static Method twId;
  private static Method eitherLeft;

  private static boolean initialized = false;
  private static boolean available = false;
  private static String failReason = "";

  private static final ConcurrentHashMap<UUID, ChannelHandler> handlers = new ConcurrentHashMap<>();

  public static boolean isAvailable() {
    ensureInit();
    return available;
  }

  public static String getFailReason() {
    return failReason;
  }

  private static void ensureInit() {
    if (initialized) return;
    initialized = true;
    try {
      String cb = Bukkit.getServer().getClass().getPackage().getName();
      craftPlayerClass = Class.forName(cb + ".entity.CraftPlayer");
      getHandle = craftPlayerClass.getMethod("getHandle");

      Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
      spConnectionField = serverPlayerClass.getField("connection");
      Class<?> commonListener =
          Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl");
      connChannelField = commonListener.getField("connection");
      Class<?> connectionClass = Class.forName("net.minecraft.network.Connection");
      channelField = connectionClass.getField("channel");

      waypointPacketClass =
          Class.forName("net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket");
      packetWaypoint = waypointPacketClass.getMethod("waypoint");
      packetOperation = waypointPacketClass.getMethod("operation");
      opEnumClass =
          Class.forName(
              "net.minecraft.network.protocol.game.ClientboundTrackedWaypointPacket$Operation");
      opUntrack = resolveEnumLocal(opEnumClass, new String[] {"UNTRACK", "REMOVE"});
      Class<?> trackedWaypointClass =
          Class.forName("net.minecraft.world.waypoints.TrackedWaypoint");
      twId = trackedWaypointClass.getMethod("id");
      Class<?> eitherClass = Class.forName("com.mojang.datafixers.util.Either");
      eitherLeft = eitherClass.getMethod("left");

      available = true;
    } catch (Throwable t) {
      available = false;
      failReason =
          t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
    }
  }

  private static Channel getChannel(Player player) throws Exception {
    Object sp = getHandle.invoke(player);
    Object listener = spConnectionField.get(sp);
    Object connection = connChannelField.get(listener);
    return (Channel) channelField.get(connection);
  }

  public static void inject(Player viewer) {
    ensureInit();
    if (!available) return;
    try {
      Channel channel = getChannel(viewer);
      io.netty.channel.ChannelPipeline pipeline = channel.pipeline();
      if (pipeline.get(HANDLER_NAME) != null) return;
      List<String> names = pipeline.names();
      String base = names.contains("outbound_config") ? "outbound_config" : "encoder";
      ChannelHandler handler = new LocatorBarOutboundHandler(viewer.getUniqueId());
      handlers.put(viewer.getUniqueId(), handler);
      pipeline.addBefore(base, HANDLER_NAME, handler);
    } catch (Throwable ignored) {
    }
  }

  public static void remove(Player viewer) {
    ensureInit();
    if (!available) return;
    try {
      Channel channel = getChannel(viewer);
      io.netty.channel.ChannelPipeline pipeline = channel.pipeline();
      if (pipeline.get(HANDLER_NAME) != null) {
        pipeline.remove(HANDLER_NAME);
      }
      handlers.remove(viewer.getUniqueId());
    } catch (Throwable ignored) {
    }
  }

  public static void removeAll() {
    ensureInit();
    if (!available) return;
    for (Player p : Bukkit.getOnlinePlayers()) remove(p);
  }

  private static Object resolveEnumLocal(Class<?> enumClass, String[] candidates) {
    for (Object c : enumClass.getEnumConstants()) {
      for (String name : candidates) {
        if (((Enum<?>) c).name().equalsIgnoreCase(name)) return c;
      }
    }
    return null;
  }

  private static UUID extractId(Object packet) {
    try {
      Object tw = packetWaypoint.invoke(packet);
      Object either = twId.invoke(tw);
      Object opt = eitherLeft.invoke(either);
      if (Boolean.TRUE.equals(opt.getClass().getMethod("isPresent").invoke(opt))) {
        return (UUID) opt.getClass().getMethod("get").invoke(opt);
      }
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static boolean isUntrack(Object packet) {
    try {
      Object op = packetOperation.invoke(packet);
      return opUntrack != null && opUntrack.equals(op);
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static void release(Object msg) {
    try {
      Class<?> util = Class.forName("io.netty.util.ReferenceCountUtil");
      util.getMethod("release", Object.class).invoke(null, msg);
    } catch (Throwable ignored) {
    }
  }

  private static final class LocatorBarOutboundHandler extends ChannelOutboundHandlerAdapter {
    private final UUID viewer;

    LocatorBarOutboundHandler(UUID viewer) {
      this.viewer = viewer;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
      try {
        if (available && waypointPacketClass.isInstance(msg)) {
          UUID id = extractId(msg);
          boolean untrack = isUntrack(msg);
          LocatorBarManager mgr = LocatorBarManager.get();
          if (mgr != null && mgr.shouldCancel(viewer, id, untrack)) {
            release(msg);
            try {
              promise.setSuccess();
            } catch (Throwable ignored) {
            }
            return;
          }
        }
      } catch (Throwable t) {
      }
      ctx.write(msg, promise);
    }
  }

  private LocatorBarFilter() {}
}
