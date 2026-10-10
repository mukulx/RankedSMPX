package dev.mukulx.rankedsmpx.locator;

import dev.mukulx.rankedsmpx.utils.SchedulerUtil;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class LocatorBarManager {

  enum Mode {
    HIDDEN,
    MANAGED,
    VANILLA
  }

  static final class Session {
    final UUID holder;
    volatile Mode mode = Mode.MANAGED;
    final Set<UUID> targets = new HashSet<>();
    volatile double range = -1;
    volatile Set<UUID> allowedIds = ConcurrentHashMap.newKeySet();
    final Set<UUID> shownIds = new HashSet<>();

    Session(UUID holder) {
      this.holder = holder;
    }
  }

  private static LocatorBarManager instance;

  static void set(LocatorBarManager m) {
    instance = m;
  }

  static LocatorBarManager get() {
    return instance;
  }

  private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
  private volatile boolean globalHidden = false;
  private volatile boolean loggingEnabled = true;

  void setLoggingEnabled(boolean enabled) {
    this.loggingEnabled = enabled;
  }

  boolean isLoggingEnabled() {
    return this.loggingEnabled;
  }
  void setGlobalHidden(boolean hidden) {
    globalHidden = hidden;
    JavaPlugin pl = LocatorBarAPI.getPlugin();
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (pl != null && SchedulerUtil.isFolia()) {
        SchedulerUtil.runOnPlayer(pl, p, player -> setHidden(player, hidden));
      } else {
        setHidden(p, hidden);
      }
    }
    if (hidden) {
      WaypointManagerHelper.hideAll();
    } else {
      WaypointManagerHelper.showAllPlayers();
    }
  }

  private void applyReceiveRange(Player p, boolean hidden) {
    try {
      AttributeInstance inst = p.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
      if (inst != null) {
        inst.setBaseValue(hidden ? 0 : inst.getDefaultValue());
      }
    } catch (Throwable ignored) {
    }
  }

  void setHidden(Player p, boolean hidden) {
    UUID id = p.getUniqueId();
    Session s = sessions.computeIfAbsent(id, Session::new);
    if (hidden) {
      s.mode = Mode.HIDDEN;
      s.targets.clear();
      s.allowedIds.clear();

      applyReceiveRange(p, true);
      WaypointManagerHelper.hidePlayer(p);

      for (UUID u : new HashSet<>(s.shownIds)) {
        LocatorBarWaypoint.untrack(p, u);
      }
      s.shownIds.clear();

      for (Player other : Bukkit.getOnlinePlayers()) {
        if (!other.getUniqueId().equals(id)) {
          LocatorBarWaypoint.untrack(p, other.getUniqueId());
        }
      }
    } else {
      s.mode = Mode.MANAGED;
      s.targets.clear();
      s.range = -1;
      s.shownIds.clear();
      s.allowedIds.clear();
      applyReceiveRange(p, true);
      WaypointManagerHelper.showPlayer(p);
    }
  }

  boolean isHidden(Player p) {
    Session s = sessions.get(p.getUniqueId());
    return s != null && s.mode == Mode.HIDDEN;
  }
  void clearTracking(Player holder) {
    UUID id = holder.getUniqueId();
    Session s = sessions.get(id);
    if (s != null) {
      s.targets.clear();
      s.range = -1;
      s.mode = Mode.MANAGED;
    }
  }

  boolean isTracking(Player holder) {
    Session s = sessions.get(holder.getUniqueId());
    return s != null && s.mode == Mode.MANAGED && !s.targets.isEmpty();
  }

  boolean shouldCancel(UUID viewerId, UUID waypointId, boolean isUntrack) {
    if (isUntrack) return false;

    Session s = sessions.get(viewerId);
    if (s == null) return globalHidden;
    if (s.mode == Mode.HIDDEN || globalHidden) return true;
    if (s.mode == Mode.MANAGED) {
      if (waypointId == null) return true;
      return !s.allowedIds.contains(waypointId);
    }
    return false;
  }

  void tick(JavaPlugin plugin) {
    for (Session s : sessions.values()) {
      if (s.mode != Mode.MANAGED) continue;
      Player holder = Bukkit.getPlayer(s.holder);
      if (holder == null || !holder.isOnline()) continue;

      if (plugin != null && SchedulerUtil.isFolia()) {
        SchedulerUtil.runOnPlayer(plugin, holder, (p) -> tickViewer(p, s));
      } else {
        tickViewer(holder, s);
      }
    }
  }

  private void tickViewer(Player holder, Session s) {
    if (holder == null || !holder.isOnline() || s.mode != Mode.MANAGED) return;

    Set<UUID> desired = new HashSet<>();
    Collection<? extends Player> candidates =
        s.targets.isEmpty() ? Bukkit.getOnlinePlayers() : resolve(s.targets);

    for (Player tu : candidates) {
      if (tu == null || !tu.isOnline() || !tu.isValid() || tu.isDead()) continue;
      if (tu.getUniqueId().equals(s.holder)) continue;
      if (!tu.getWorld().equals(holder.getWorld())) continue;
      if (!holder.canSee(tu)) continue;

      if (s.range > 0) {
        Location hl = holder.getLocation();
        Location tl = tu.getLocation();
        double dx = hl.getX() - tl.getX();
        double dy = hl.getY() - tl.getY();
        double dz = hl.getZ() - tl.getZ();
        if (dx * dx + dy * dy + dz * dz > s.range * s.range) continue;
      }

      desired.add(tu.getUniqueId());
    }

    s.allowedIds = desired;

    for (UUID shown : new HashSet<>(s.shownIds)) {
      if (!desired.contains(shown)) {
        LocatorBarWaypoint.untrack(holder, shown);
        s.shownIds.remove(shown);
      }
    }

    for (UUID targetId : desired) {
      Player target = Bukkit.getPlayer(targetId);
      if (target == null || !target.isOnline()) continue;

      Location loc = target.getLocation();
      int bx = loc.getBlockX();
      int by = loc.getBlockY();
      int bz = loc.getBlockZ();

      if (!s.shownIds.contains(targetId)) {
        LocatorBarWaypoint.track(holder, targetId, bx, by, bz);
        s.shownIds.add(targetId);
      } else {
        LocatorBarWaypoint.update(holder, targetId, bx, by, bz);
      }
    }
  }

  private Collection<Player> resolve(Set<UUID> ids) {
    Set<Player> out = new HashSet<>();
    for (UUID id : ids) {
      Player p = Bukkit.getPlayer(id);
      if (p != null) out.add(p);
    }
    return out;
  }

  void onPlayerJoin(Player p) {
    UUID id = p.getUniqueId();
    Session s = sessions.get(id);
    if (s != null) {
      s.shownIds.clear();
      s.allowedIds.clear();
      if (s.mode == Mode.HIDDEN || globalHidden) {
        setHidden(p, true);
      } else {
        setHidden(p, false);
      }
    } else if (globalHidden) {
      setHidden(p, true);
    } else {
      setHidden(p, false);
    }
  }

  void onPlayerQuit(Player p) {
    UUID id = p.getUniqueId();
    Session s = sessions.get(id);
    if (s != null) {
      s.shownIds.clear();
      s.allowedIds.clear();
    }
    for (Session otherSession : sessions.values()) {
      if (otherSession.shownIds.remove(id)) {
        Player viewer = Bukkit.getPlayer(otherSession.holder);
        if (viewer != null && viewer.isOnline()) {
          LocatorBarWaypoint.untrack(viewer, id);
        }
      }
    }
  }

  void onPlayerWorldChange(Player p) {
    UUID id = p.getUniqueId();
    Session s = sessions.get(id);
    if (s != null) {
      s.shownIds.clear();
      s.allowedIds.clear();
    }
    for (Session otherSession : sessions.values()) {
      if (otherSession.shownIds.remove(id)) {
        Player viewer = Bukkit.getPlayer(otherSession.holder);
        if (viewer != null && viewer.isOnline()) {
          LocatorBarWaypoint.untrack(viewer, id);
        }
      }
    }
  }

  void onPlayerRespawn(Player p) {
    UUID id = p.getUniqueId();
    Session s = sessions.get(id);
    if (s != null) {
      s.shownIds.clear();
      s.allowedIds.clear();
    }
  }

  void reset(Player p) {
    UUID id = p.getUniqueId();
    Session s = sessions.remove(id);
    if (s != null) {
      for (UUID u : new HashSet<>(s.shownIds)) {
        LocatorBarWaypoint.untrack(p, u);
      }
      s.shownIds.clear();
    }

    for (Player other : Bukkit.getOnlinePlayers()) {
      if (!other.getUniqueId().equals(id)) {
        LocatorBarWaypoint.untrack(p, other.getUniqueId());
      }
    }
    try {
      AttributeInstance inst = p.getAttribute(Attribute.WAYPOINT_RECEIVE_RANGE);
      if (inst != null) inst.setBaseValue(inst.getDefaultValue());
    } catch (Throwable ignored) {
    }
    WaypointManagerHelper.showPlayer(p);
  }

  void revertAll() {
    globalHidden = false;
    JavaPlugin pl = LocatorBarAPI.getPlugin();
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (pl != null && SchedulerUtil.isFolia()) {
        SchedulerUtil.runOnPlayer(pl, p, this::reset);
      } else {
        reset(p);
      }
    }
    WaypointManagerHelper.showAllPlayers();
  }
}
