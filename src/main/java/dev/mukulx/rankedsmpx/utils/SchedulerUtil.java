package dev.mukulx.rankedsmpx.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/** Routes scheduler calls through Paper or Folia. */
public class SchedulerUtil {

  private static final boolean IS_FOLIA = detectFolia();

  private static boolean detectFolia() {
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      return true;
    } catch (ClassNotFoundException exception) {
      return false;
    }
  }

  public static boolean isFolia() {
    return IS_FOLIA;
  }

  public static void runAsync(@NotNull Plugin plugin, @NotNull Runnable task) {
    if (IS_FOLIA) {
      Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
      return;
    }
    Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
  }

  public static void run(@NotNull Plugin plugin, @NotNull Runnable task) {
    if (IS_FOLIA) {
      Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
      return;
    }
    Bukkit.getScheduler().runTask(plugin, task);
  }

  public static void runLater(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks) {
    if (IS_FOLIA) {
      Bukkit.getGlobalRegionScheduler()
          .runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
      return;
    }
    Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
  }

  public static void runLaterAsync(
      @NotNull Plugin plugin, @NotNull Runnable task, long delayTicks) {
    if (IS_FOLIA) {
      Bukkit.getAsyncScheduler()
          .runDelayed(plugin, scheduledTask -> task.run(), delayTicks * 50L, TimeUnit.MILLISECONDS);
      return;
    }
    Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks);
  }

  public static Object runTimer(
      @NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
    if (IS_FOLIA) {
      return Bukkit.getGlobalRegionScheduler()
          .runAtFixedRate(plugin, scheduledTask -> task.run(), delayTicks, periodTicks);
    }
    return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
  }

  public static TaskHandle runTimerForEntity(
      @NotNull Plugin plugin,
      @NotNull Entity entity,
      @NotNull java.util.function.Consumer<TaskHandle> task,
      long delayTicks,
      long periodTicks) {
    TaskHandle handle = new TaskHandle();
    Object scheduledTask;
    if (IS_FOLIA) {
      scheduledTask =
          entity
              .getScheduler()
              .runAtFixedRate(plugin, ignored -> task.accept(handle), null, delayTicks, periodTicks);
    } else {
      scheduledTask =
          Bukkit.getScheduler()
              .runTaskTimer(plugin, () -> task.accept(handle), delayTicks, periodTicks);
    }
    handle.set(scheduledTask);
    return handle;
  }

  public static TaskHandle runTimerAtLocation(
      @NotNull Plugin plugin,
      @NotNull Location location,
      @NotNull java.util.function.Consumer<TaskHandle> task,
      long delayTicks,
      long periodTicks) {
    TaskHandle handle = new TaskHandle();
    Object scheduledTask;
    if (IS_FOLIA) {
      scheduledTask =
          Bukkit.getRegionScheduler()
              .runAtFixedRate(plugin, location, ignored -> task.accept(handle), delayTicks, periodTicks);
    } else {
      scheduledTask =
          Bukkit.getScheduler()
              .runTaskTimer(plugin, () -> task.accept(handle), delayTicks, periodTicks);
    }
    handle.set(scheduledTask);
    return handle;
  }

  public static Object runTimerAsync(
      @NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
    if (IS_FOLIA) {
      return Bukkit.getAsyncScheduler()
          .runAtFixedRate(
              plugin,
              scheduledTask -> task.run(),
              delayTicks * 50L,
              periodTicks * 50L,
              TimeUnit.MILLISECONDS);
    }
    return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
  }

  public static void runForEntity(
      @NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
    if (IS_FOLIA) {
      entity.getScheduler().run(plugin, scheduledTask -> task.run(), null);
      return;
    }
    Bukkit.getScheduler().runTask(plugin, task);
  }

  /**
   * Guard for methods that mutate an entity and may be called from any thread (console commands,
   * Discord/JDA threads, async tasks, another player's region on Folia).
   *
   * @return true if the current thread owns the entity and the caller may proceed; otherwise
   *     {@code retry} is scheduled on the entity's thread and false is returned
   */
  public static boolean isOwnedOrReschedule(
      @NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable retry) {
    if (Bukkit.isOwnedByCurrentRegion(entity)) {
      return true;
    }
    runForEntity(plugin, entity, retry);
    return false;
  }

  public static void runForEntityLater(
      @NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
    if (IS_FOLIA) {
      entity.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
      return;
    }
    Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
  }

  public static void runAtLocationLater(
      @NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
    if (IS_FOLIA) {
      Bukkit.getRegionScheduler()
          .runDelayed(plugin, location, scheduledTask -> task.run(), delayTicks);
      return;
    }
    Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
  }

  public static AutoCloseable runRepeatingCloseable(
      @NotNull Plugin plugin, @NotNull Runnable task, long initialDelayTicks, long periodTicks) {
    if (IS_FOLIA) {
      long delayMs = Math.max(1, initialDelayTicks * 50L);
      long periodMs = Math.max(50, periodTicks * 50L);
      io.papermc.paper.threadedregions.scheduler.ScheduledTask foliaTask =
          Bukkit.getAsyncScheduler()
              .runAtFixedRate(
                  plugin, (t) -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
      return foliaTask::cancel;
    } else {
      org.bukkit.scheduler.BukkitTask bukkitTask =
          Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
      return bukkitTask::cancel;
    }
  }

  /** Runs inline on Paper and on the player's region thread on Folia. */
  public static void runOnPlayer(
      @NotNull Plugin plugin, @NotNull Player player, @NotNull Consumer<Player> task) {
    if (IS_FOLIA) {
      player.getScheduler().run(plugin, (t) -> task.accept(player), null);
    } else {
      task.accept(player);
    }
  }

  public static void cancelTask(@NotNull Object task) {
    if (task instanceof TaskHandle handle) {
      handle.cancel();
    } else if (task instanceof BukkitTask bukkitTask) {
      bukkitTask.cancel();
    } else if (task instanceof io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask) {
      scheduledTask.cancel();
    } else if (task instanceof AutoCloseable closeable) {
      try {
        closeable.close();
      } catch (Exception ignored) {
      }
    }
  }

  public static final class TaskHandle {
    private final AtomicReference<Object> task = new AtomicReference<>();

    private void set(Object scheduledTask) {
      task.set(scheduledTask);
    }

    public void cancel() {
      Object scheduledTask = task.get();
      if (scheduledTask != null) {
        SchedulerUtil.cancelTask(scheduledTask);
      }
    }
  }
}
