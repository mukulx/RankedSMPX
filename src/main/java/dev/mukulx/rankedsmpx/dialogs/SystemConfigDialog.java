package dev.mukulx.rankedsmpx.dialogs;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SystemConfigDialog {

  private final RankedSMPXPlugin plugin;

  public SystemConfigDialog(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(@NotNull Player player) {
    FileConfiguration config = plugin.getConfig();
    boolean debugMode = config.getBoolean("debug", false);
    boolean lpEnabled = config.getBoolean("luckperms.enabled", false);
    String dbType = plugin.getDatabaseManager().isMySql() ? "MySQL" : "SQLite";

    Dialog dialog =
        Dialog.create(
            builder ->
                builder
                    .empty()
                    .base(
                        DialogBase.builder(Component.text("System & Diagnostics"))
                            .body(
                                List.of(
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Database: " + dbType + " | Version: " + plugin.getPluginMeta().getVersion())),
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Reload configuration files, toggle debug output, or sync permissions."))))
                            .inputs(
                                List.of(
                                    DialogInput.bool("debug", Component.text("Verbose Debug Console Logging"), debugMode, "Enabled", "Disabled"),
                                    DialogInput.bool("luckperms", Component.text("LuckPerms Dynamic Rank Contexts"), lpEnabled, "Enabled", "Disabled")))
                            .build())
                    .type(
                        DialogType.multiAction(
                            List.of(
                                ActionButton.create(
                                    Component.text("Reload Config"),
                                    Component.text("Reloads every config file from disk"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            p.performCommand("rsmp reload");
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Sync LuckPerms"),
                                    Component.text("Forces LuckPerms to recalculate rank contexts for all online players"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            saveToggles(view, p);
                                            if (plugin.getLuckPermsHook() != null && plugin.getLuckPermsHook().isHooked()) {
                                              for (Player online : Bukkit.getOnlinePlayers()) {
                                                plugin.getLuckPermsHook().signalUpdate(online);
                                              }
                                              p.sendMessage(Component.text("LuckPerms contexts synced for all online players!"));
                                            } else {
                                              p.sendMessage(Component.text("LuckPerms hook is not enabled."));
                                            }
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Save Settings"),
                                    Component.text("Saves settings to config.yml"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            if (saveToggles(view, p)) {
                                              DialogSupport.saved(p, "config.yml");
                                            }
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("« Back to Menu"),
                                    Component.text("Return to the main Admin Dashboard"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            saveToggles(view, p);
                                            plugin.getDialogManager().openMainMenu(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build()))),
                            ActionButton.create(
                                Component.text("Close"),
                                Component.text("Close dialog"),
                                100,
                                null),
                            2)));
    player.showDialog(dialog);
  }

  private boolean saveToggles(DialogResponseView view, Player player) {
    Map<String, String> values = new LinkedHashMap<>();
    DialogSupport.put(values, "debug", view.getBoolean("debug"));
    DialogSupport.put(values, "luckperms.enabled", view.getBoolean("luckperms"));
    return DialogSupport.save(plugin, player, values);
  }
}
