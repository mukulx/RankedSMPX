package dev.mukulx.rankedsmpx.utils;

import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransientConnectionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;

/** Decides which log records are plugin bugs worth showing the "report this" notice for. */
public final class ErrorReports {

  private static final String PLUGIN_PACKAGE = "dev.mukulx.rankedsmpx";

  private ErrorReports() {}

  /**
   * True only for a SEVERE record that carries an exception thrown from this plugin's code. Plain
   * error messages (bad config, game state, offline database) and the exceptions raised while the
   * server or plugin is shutting down are not bugs.
   */
  public static boolean isPluginBug(@NotNull LogRecord record) {
    Throwable thrown = record.getThrown();
    if (thrown == null || record.getLevel().intValue() < Level.SEVERE.intValue()) {
      return false;
    }
    boolean fromPlugin = false;
    for (Throwable t = thrown; t != null; t = t.getCause()) {
      if (isEnvironmentError(t)) {
        return false;
      }
      for (StackTraceElement frame : t.getStackTrace()) {
        if (frame.getClassName().startsWith(PLUGIN_PACKAGE)) {
          fromPlugin = true;
        }
      }
      if (t.getCause() == t) {
        break;
      }
    }
    return fromPlugin;
  }

  private static boolean isEnvironmentError(Throwable t) {
    return t instanceof InterruptedException
        || t instanceof RejectedExecutionException
        || t instanceof IllegalPluginAccessException
        || t instanceof java.net.SocketException
        || t instanceof java.net.UnknownHostException
        || t instanceof java.net.SocketTimeoutException
        || t instanceof java.net.http.HttpTimeoutException
        || t instanceof SQLTransientConnectionException
        || t instanceof SQLNonTransientConnectionException;
  }
}
