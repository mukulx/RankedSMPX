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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StylesDialog {

  private final RankedSMPXPlugin plugin;

  public StylesDialog(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(@NotNull Player player) {
    FileConfiguration config = plugin.getConfig();
    boolean tabEnabled = config.getBoolean("tab-integration.enabled", true);
    boolean smallCaps = config.getBoolean("small-caps-messages", true);
    boolean showOnJoin = config.getBoolean("rank-display.show-on-join", true);

    Dialog dialog =
        Dialog.create(
            builder ->
                builder
                    .empty()
                    .base(
                        DialogBase.builder(Component.text("Styles & Display Settings"))
                            .body(
                                List.of(
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Apply style presets or toggle individual display features.")),
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Presets automatically update TAB and Vanilla Tab prefix formatting."))))
                            .inputs(
                                List.of(
                                    DialogInput.bool("tab_enabled", Component.text("Tab List Integration"), tabEnabled, "Enabled", "Disabled"),
                                    DialogInput.bool("small_caps", Component.text("Small Caps in Chat Messages"), smallCaps, "Enabled", "Disabled"),
                                    DialogInput.bool("show_on_join", Component.text("Show Rank Prefix on Join"), showOnJoin, "Enabled", "Disabled")))
                            .build())
                    .type(
                        DialogType.multiAction(
                            List.of(
                                ActionButton.create(
                                    Component.text("Apply Original Style"),
                                    Component.text("Classic yellow tags: [#1], [#2], [UNRANKED]"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            saveToggles(view, p);
                                            p.performCommand("rsmp original");
                                            open(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Apply Modern Gradients"),
                                    Component.text("RGB gradients: Gold, Silver, Lime, Purple"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            saveToggles(view, p);
                                            p.performCommand("rsmp modern");
                                            open(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Save Settings"),
                                    Component.text("Saves the toggles to config.yml"),
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
    DialogSupport.put(values, "tab-integration.enabled", view.getBoolean("tab_enabled"));
    DialogSupport.put(values, "small-caps-messages", view.getBoolean("small_caps"));
    DialogSupport.put(values, "rank-display.show-on-join", view.getBoolean("show_on_join"));
    return DialogSupport.save(plugin, player, values);
  }
}
