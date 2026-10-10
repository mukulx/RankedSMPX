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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GameOperationsDialog {

  private final RankedSMPXPlugin plugin;

  public GameOperationsDialog(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(@NotNull Player player) {
    boolean isGameActive = plugin.getGameManager().isGameActive();
    int rankedCount = plugin.getRankManager().getAllRanks().size();
    boolean autoAssign = plugin.getConfig().getBoolean("auto-assign-ranks.enabled", true);

    Dialog dialog =
        Dialog.create(
            builder ->
                builder
                    .empty()
                    .base(
                        DialogBase.builder(Component.text("Game Operations"))
                            .body(
                                List.of(
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Season Status: " + (isGameActive ? "ACTIVE" : "INACTIVE")
                                                + " | Ranked Players: " + rankedCount + "/" + plugin.getConfigManager().getTotalRanks())),
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Configure season lifecycle and toggle automated player assignment."))))
                            .inputs(
                                List.of(
                                    DialogInput.bool(
                                        "auto_assign",
                                        Component.text("Auto-assign ranks on join"),
                                        autoAssign,
                                        "Enabled",
                                        "Disabled")))
                            .build())
                    .type(
                        DialogType.multiAction(
                            List.of(
                                ActionButton.create(
                                    Component.text("Start Season"),
                                    Component.text("Randomly assigns every rank and begins season"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            saveAutoAssign(view, p);
                                            p.performCommand("rsmp start");
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Stop Season"),
                                    Component.text("Wipes ranks, resets health and extra inventories"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            saveAutoAssign(view, p);
                                            p.performCommand("rsmp stop");
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Test Reveal Animation"),
                                    Component.text("Plays the rank reveal title animation on yourself"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            Integer rank = plugin.getRankManager().getRank(p.getUniqueId());
                                            plugin.getRankRevealAnimation().startAnimation(p, rank != null ? rank : 1);
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
                                            saveAutoAssign(view, p);
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

  private void saveAutoAssign(DialogResponseView view, Player player) {
    Map<String, String> values = new LinkedHashMap<>();
    DialogSupport.put(values, "auto-assign-ranks.enabled", view.getBoolean("auto_assign"));
    DialogSupport.save(plugin, player, values);
  }
}
