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
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class SpecialFeaturesDialog {

  private final RankedSMPXPlugin plugin;

  public SpecialFeaturesDialog(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(@NotNull Player player) {
    FileConfiguration config = plugin.getConfig();
    boolean locatorEnabled = config.getBoolean("dragon-egg-locator.enabled", true);
    boolean eggStorage = config.getBoolean("dragon-egg-locator.prevent-storage", true);
    boolean eggDrop = config.getBoolean("dragon-egg-locator.prevent-drop", false);
    boolean spearEnabled = config.getBoolean("spear.enabled", true);

    Dialog dialog =
        Dialog.create(
            builder ->
                builder
                    .empty()
                    .base(
                        DialogBase.builder(Component.text("Special Items & Features"))
                            .body(
                                List.of(
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Configure Waypoint HUD Dragon Egg Locator and custom items.")),
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Changes are saved to items.yml when you press Save."))))
                            .inputs(
                                List.of(
                                    DialogInput.bool("locator_enabled", Component.text("Enable Dragon Egg Locator Bar"), locatorEnabled, "Enabled", "Disabled"),
                                    DialogInput.bool("egg_storage", Component.text("Prevent Egg container storage"), eggStorage, "Enabled", "Disabled"),
                                    DialogInput.bool("egg_drop", Component.text("Prevent Egg dropping (Q key)"), eggDrop, "Enabled", "Disabled"),
                                    DialogInput.bool("spear_enabled", Component.text("Enable Diamond Spear (1.21.11+)"), spearEnabled, "Enabled", "Disabled")))
                            .build())
                    .type(
                        DialogType.multiAction(
                            List.of(
                                ActionButton.create(
                                    Component.text("Save Settings"),
                                    Component.text("Saves locator and spear settings to items.yml"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            if (save(view, p)) {
                                              DialogSupport.saved(p, "items.yml");
                                            }
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Give Dragon Egg"),
                                    Component.text("Spawns a Dragon Egg into your inventory"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            p.getInventory().addItem(new ItemStack(Material.DRAGON_EGG));
                                            p.sendMessage(Component.text("Dragon Egg added to your inventory!"));
                                          }
                                        },
                                        ClickCallback.Options.builder().uses(1).build())),
                                ActionButton.create(
                                    Component.text("Give Diamond Spear"),
                                    Component.text("Spawns the custom Diamond Spear (1.21.11+)"),
                                    150,
                                    DialogAction.customClick(
                                        (view, audience) -> {
                                          if (audience instanceof Player p) {
                                            p.performCommand("rsmp spear");
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
                                            save(view, p);
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

  private boolean save(DialogResponseView view, Player player) {
    Map<String, String> values = new LinkedHashMap<>();
    DialogSupport.put(values, "dragon-egg-locator.enabled", view.getBoolean("locator_enabled"));
    DialogSupport.put(values, "dragon-egg-locator.prevent-storage", view.getBoolean("egg_storage"));
    DialogSupport.put(values, "dragon-egg-locator.prevent-drop", view.getBoolean("egg_drop"));
    DialogSupport.put(values, "spear.enabled", view.getBoolean("spear_enabled"));
    return DialogSupport.save(plugin, player, values);
  }
}
