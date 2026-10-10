package dev.mukulx.rankedsmpx.locator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.World;

final class GameRuleHelper {

  private static Class<?> gameRulesClass;
  private static Class<?> serverLevelClass;
  private static Object locatorBarRule;
  private static Method craftGetHandle;
  private static Method getGameRules;
  private static Method setRule;
  private static Method getRule;

  private static boolean resolved = false;

  private static void resolve() {
    if (resolved) {
      return;
    }
    resolved = true;
    try {
      String serverPackage = Bukkit.getServer().getClass().getPackage().getName();
      Class<?> craftWorldClass = Class.forName(serverPackage + ".CraftWorld");
      craftGetHandle = craftWorldClass.getMethod("getHandle");
      gameRulesClass = Class.forName("net.minecraft.world.level.gamerules.GameRules");
      serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");

      Field ruleField = gameRulesClass.getField("LOCATOR_BAR");
      locatorBarRule = ruleField.get(null);
      getGameRules = serverLevelClass.getMethod("getGameRules");
      setRule = gameRulesClass.getMethod("set", ruleField.getType(), Object.class, serverLevelClass);
      getRule = gameRulesClass.getMethod("get", ruleField.getType());
    } catch (Throwable t) {
      locatorBarRule = null;
    }
  }

  static boolean isAvailable() {
    resolve();
    return locatorBarRule != null;
  }

  static boolean set(World world, boolean enabled) {
    resolve();
    if (locatorBarRule == null || world == null) return false;
    try {
      Object serverLevel = craftGetHandle.invoke(world);
      Object gameRules = getGameRules.invoke(serverLevel);
      setRule.invoke(gameRules, locatorBarRule, enabled ? Boolean.TRUE : Boolean.FALSE, serverLevel);
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private GameRuleHelper() {}
}
