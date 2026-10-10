package dev.mukulx.rankedsmpx.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLNonTransientConnectionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorReportsTest {

  private static LogRecord record(Level level, Throwable thrown) {
    LogRecord r = new LogRecord(level, "message");
    r.setThrown(thrown);
    return r;
  }

  private static Throwable fromPlugin(Throwable t) {
    t.setStackTrace(
        new StackTraceElement[] {
          new StackTraceElement("dev.mukulx.rankedsmpx.managers.GameManager", "start", "GameManager.java", 1)
        });
    return t;
  }

  @Test
  @DisplayName("An unexpected exception from the plugin is reported")
  void realBug() {
    assertTrue(ErrorReports.isPluginBug(record(Level.SEVERE, fromPlugin(new NullPointerException()))));
  }

  @Test
  @DisplayName("Plain error messages without an exception are not bugs")
  void noException() {
    assertFalse(ErrorReports.isPluginBug(record(Level.SEVERE, null)));
  }

  @Test
  @DisplayName("Warnings and lower levels are not reported even with an exception")
  void lowLevel() {
    assertFalse(ErrorReports.isPluginBug(record(Level.WARNING, fromPlugin(new IllegalStateException()))));
    assertFalse(ErrorReports.isPluginBug(record(Level.INFO, fromPlugin(new IllegalStateException()))));
  }

  @Test
  @DisplayName("Shutdown and environment errors are not bugs")
  void environment() {
    assertFalse(ErrorReports.isPluginBug(record(Level.SEVERE, fromPlugin(new RejectedExecutionException()))));
    assertFalse(ErrorReports.isPluginBug(record(Level.SEVERE, fromPlugin(new InterruptedException()))));
    assertFalse(ErrorReports.isPluginBug(record(Level.SEVERE, fromPlugin(new java.net.UnknownHostException()))));
    assertFalse(
        ErrorReports.isPluginBug(
            record(Level.SEVERE, fromPlugin(new SQLNonTransientConnectionException("down")))));
  }

  @Test
  @DisplayName("An environment error hidden in the cause chain is still not a bug")
  void wrapped() {
    Throwable wrapper = fromPlugin(new RuntimeException("wrapped", new RejectedExecutionException()));
    assertFalse(ErrorReports.isPluginBug(record(Level.SEVERE, wrapper)));
  }

  @Test
  @DisplayName("Exceptions that never touch plugin code are not reported")
  void foreign() {
    Throwable t = new IllegalStateException();
    t.setStackTrace(new StackTraceElement[] {new StackTraceElement("some.other.Plugin", "run", "X.java", 1)});
    assertFalse(ErrorReports.isPluginBug(record(Level.SEVERE, t)));
  }
}
