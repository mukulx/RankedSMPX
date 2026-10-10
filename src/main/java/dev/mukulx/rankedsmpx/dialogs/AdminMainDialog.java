package dev.mukulx.rankedsmpx.dialogs;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class AdminMainDialog {

  private final RankedSMPXPlugin plugin;

  public AdminMainDialog(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(@NotNull Player player) {
    boolean isGameActive = plugin.getGameManager().isGameActive();
    int rankedCount = plugin.getRankManager().getAllRanks().size();

    Dialog dialog =
        Dialog.create(
            builder ->
                builder
                    .empty()
                    .base(
                        DialogBase.builder(Component.text("RankedSMP Admin Dashboard"))
                            .body(
                                List.of(
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Status: " + (isGameActive ? "ACTIVE" : "INACTIVE")
                                                + " | Ranked Players: " + rankedCount + "/" + plugin.getConfigManager().getTotalRanks()
                                                + " | Version: " + plugin.getPluginMeta().getVersion())),
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Select a management category below to configure features or manage players:"))))
                            .build())
                    .type(
                        DialogType.multiAction(
                            List.of(
                                ActionButton.create(
                                    Component.text("Game Operations"),
                                    Component.text("Start/stop seasons, test animations, and toggle auto-assign"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            plugin.getDialogManager().openGameOperations(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Rank Management"),
                                    Component.text("Assign ranks, remove ranks, inspect inventories, blacklist"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            plugin.getDialogManager().openRankManagement(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Hierarchy Hammer"),
                                    Component.text("Configure dash cooldowns, combos, mace smash, and particles"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            plugin.getDialogManager().openHammerSettings(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Special Items"),
                                    Component.text("Configure Dragon Egg Locator HUD bar and custom spears"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            plugin.getDialogManager().openSpecialFeatures(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Styles & Displays"),
                                    Component.text("Original style, Modern gradient presets, Tab list formatting"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            plugin.getDialogManager().openStyles(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("System & Config"),
                                    Component.text("Reload configs, toggle debug logging, and sync LuckPerms"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            plugin.getDialogManager().openSystemConfig(p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build()))),
                            ActionButton.create(
                                Component.text("Close"),
                                Component.text("Close dashboard"),
                                100,
                                null),
                            2)));
    player.showDialog(dialog);
  }
}
