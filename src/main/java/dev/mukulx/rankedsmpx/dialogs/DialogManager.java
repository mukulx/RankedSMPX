package dev.mukulx.rankedsmpx.dialogs;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DialogManager {

  private static final String DIALOG_CLASS = "io.papermc.paper.dialog.Dialog";
  private static final String MAIN_DIALOG_CLASS = "dev.mukulx.rankedsmpx.dialogs.AdminMainDialog";
  private static final String GAME_OPS_DIALOG_CLASS = "dev.mukulx.rankedsmpx.dialogs.GameOperationsDialog";
  private static final String RANK_MGMT_DIALOG_CLASS = "dev.mukulx.rankedsmpx.dialogs.RankManagementDialog";
  private static final String HAMMER_DIALOG_CLASS = "dev.mukulx.rankedsmpx.dialogs.hammer.HammerSettingsDialog";
  private static final String SPECIAL_ITEMS_DIALOG_CLASS = "dev.mukulx.rankedsmpx.dialogs.SpecialFeaturesDialog";
  private static final String STYLES_DIALOG_CLASS = "dev.mukulx.rankedsmpx.dialogs.StylesDialog";
  private static final String SYSTEM_CONFIG_DIALOG_CLASS = "dev.mukulx.rankedsmpx.dialogs.SystemConfigDialog";

  private final RankedSMPXPlugin plugin;
  private final boolean available;

  private Object adminMainDialog;
  private Object gameOperationsDialog;
  private Object rankManagementDialog;
  private Object hammerSettingsDialog;
  private Object specialFeaturesDialog;
  private Object stylesDialog;
  private Object systemConfigDialog;

  public DialogManager(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    this.available = isDialogApiAvailable();
    if (available) {
      plugin.getLogger().info("Paper Dialog API detected! Modern modal dialog menus enabled.");
    } else {
      plugin.getLogger().info("Paper Dialog API not detected on this server build. Falling back to standard GUI.");
    }
  }

  public boolean isAvailable() {
    return available;
  }

  public boolean openMainMenu(@NotNull Player player) {
    return openDialog(MAIN_DIALOG_CLASS, adminMainDialog, obj -> this.adminMainDialog = obj, player);
  }

  public boolean openGameOperations(@NotNull Player player) {
    return openDialog(GAME_OPS_DIALOG_CLASS, gameOperationsDialog, obj -> this.gameOperationsDialog = obj, player);
  }

  public boolean openRankManagement(@NotNull Player player) {
    return openDialog(RANK_MGMT_DIALOG_CLASS, rankManagementDialog, obj -> this.rankManagementDialog = obj, player);
  }

  public boolean openHammerSettings(@NotNull Player player) {
    return openDialog(HAMMER_DIALOG_CLASS, hammerSettingsDialog, obj -> this.hammerSettingsDialog = obj, player);
  }

  public boolean openSpecialFeatures(@NotNull Player player) {
    return openDialog(SPECIAL_ITEMS_DIALOG_CLASS, specialFeaturesDialog, obj -> this.specialFeaturesDialog = obj, player);
  }

  public boolean openStyles(@NotNull Player player) {
    return openDialog(STYLES_DIALOG_CLASS, stylesDialog, obj -> this.stylesDialog = obj, player);
  }

  public boolean openSystemConfig(@NotNull Player player) {
    return openDialog(SYSTEM_CONFIG_DIALOG_CLASS, systemConfigDialog, obj -> this.systemConfigDialog = obj, player);
  }

  private boolean openDialog(String className, Object cachedInstance, Consumer<Object> setter, @NotNull Player player) {
    if (!available) {
      return false;
    }
    try {
      Object instance = cachedInstance;
      if (instance == null) {
        Class<?> clazz = Class.forName(className, true, DialogManager.class.getClassLoader());
        instance = clazz.getConstructor(RankedSMPXPlugin.class).newInstance(plugin);
        setter.accept(instance);
      }
      Method openMethod = instance.getClass().getMethod("open", Player.class);
      openMethod.invoke(instance, player);
      return true;
    } catch (Throwable t) {
      plugin.getLogger().warning("Unable to open dialog [" + className + "]: " + t.getMessage());
      return false;
    }
  }

  private boolean isDialogApiAvailable() {
    try {
      Class.forName(DIALOG_CLASS, false, DialogManager.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException | LinkageError ignored) {
      return false;
    }
  }
}
