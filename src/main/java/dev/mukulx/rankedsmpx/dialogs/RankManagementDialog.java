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
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RankManagementDialog {

  private final RankedSMPXPlugin plugin;

  public RankManagementDialog(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(@NotNull Player player) {
    int totalRanked = plugin.getRankManager().getAllRanks().size();
    int maxRank = plugin.getConfigManager().getTotalRanks();

    Dialog dialog =
        Dialog.create(
            builder ->
                builder
                    .empty()
                    .base(
                        DialogBase.builder(Component.text("Rank Management"))
                            .body(
                                List.of(
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Total Ranked Players: " + totalRanked + " / " + maxRank)),
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Enter a player's username (online or offline) and select rank to assign, remove, or inspect."))))
                            .inputs(
                                List.of(
                                    DialogInput.text("target_player", Component.text("Player Username"))
                                        .initial(player.getName())
                                        .build(),
                                    DialogInput.numberRange("target_rank", Component.text("Rank (1-" + maxRank + ")"), 1.0f, (float) Math.max(2, maxRank))
                                        .step(1.0f)
                                        .initial(1.0f)
                                        .width(280)
                                        .build()))
                            .build())
                    .type(
                        DialogType.multiAction(
                            List.of(
                                ActionButton.create(
                                    Component.text("Assign Rank"),
                                    Component.text("Sets the player to the selected rank"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            handleSetRank(view, p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Remove Rank"),
                                    Component.text("Sets the player back to UNRANKED"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            handleRemoveRank(view, p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Inspect Extra Inv"),
                                    Component.text("Opens the target player's extra inventory"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            handleInspectInv(view, p);
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Toggle Blacklist"),
                                    Component.text("Adds or removes the player from the rank blacklist"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            handleBlacklist(view, p);
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

  private void handleSetRank(DialogResponseView view, Player admin) {
    String target = view.getText("target_player");
    Float rankFloat = view.getFloat("target_rank");
    int rank = rankFloat != null ? Math.round(rankFloat) : 1;

    if (target != null && !target.isBlank()) {
      admin.performCommand("rsmp set " + target.trim() + " " + rank);
    }
  }

  private void handleRemoveRank(DialogResponseView view, Player admin) {
    String target = view.getText("target_player");
    if (target != null && !target.isBlank()) {
      admin.performCommand("rsmp set " + target.trim() + " unranked");
    }
  }

  private void handleInspectInv(DialogResponseView view, Player admin) {
    String target = view.getText("target_player");
    if (target != null && !target.isBlank()) {
      admin.performCommand("extrainvsee " + target.trim());
    }
  }

  private void handleBlacklist(DialogResponseView view, Player admin) {
    String target = view.getText("target_player");
    if (target != null && !target.isBlank()) {
      String name = target.trim();
      boolean listed = plugin.getDatabaseManager().isBlacklisted(Bukkit.getOfflinePlayer(name).getUniqueId());
      admin.performCommand("rankblacklist " + (listed ? "remove " : "add ") + name);
    }
  }
}
