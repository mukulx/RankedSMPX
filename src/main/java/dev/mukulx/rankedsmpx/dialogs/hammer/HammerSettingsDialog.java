package dev.mukulx.rankedsmpx.dialogs.hammer;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import io.papermc.paper.dialog.Dialog;
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
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class HammerSettingsDialog {

  private static final String PATH = "hierarchy-hammer.";
  private final RankedSMPXPlugin plugin;

  public HammerSettingsDialog(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void open(@NotNull Player player) {
    FileConfiguration config = plugin.getConfig();
    Dialog dialog =
        Dialog.create(
            builder ->
                builder
                    .empty()
                    .base(
                        DialogBase.builder(Component.text("Hierarchy Hammer Settings"))
                            .body(
                                List.of(
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Settings are saved to items.yml and applied right away.")),
                                    DialogBody.plainMessage(
                                        Component.text(
                                            "Test changes before using them in a live event."))))
                            .inputs(
                                List.of(
                                    toggle("enabled", "Enable hammer", config.getBoolean(PATH + "enabled", true)),
                                    toggle("auto_start", "Auto-give on season start", config.getBoolean(PATH + "auto-give-on-start", false)),
                                    number("dash_cooldown", "Dash cooldown (seconds)", 0f, 120f, 1f, config.getDouble(PATH + "dash-cooldown", 0)),
                                    number("dash_speed", "Dash speed", 0.1f, 2.5f, 0.1f, config.getDouble(PATH + "dash-speed", 1.0)),
                                    number("combo_required", "Combo hits required", 1f, 10f, 1f, config.getDouble(PATH + "combo-required", 3)),
                                    number("combo_damage", "Combo damage multiplier", 0.1f, 10f, 0.1f, config.getDouble(PATH + "combo-damage-multiplier", 1.0)),
                                    number("combo_explosion", "Combo explosion power", 0f, 10f, 0.1f, config.getDouble(PATH + "combo-explosion-power", 0.0)),
                                    number("combo_reset", "Combo reset time (seconds)", 1f, 120f, 1f, config.getDouble(PATH + "combo-reset-time", 5)),
                                    number("miss_cooldown", "Miss cooldown (seconds)", 0f, 120f, 1f, config.getDouble(PATH + "miss-cooldown", 10)),
                                    number("verdict_cooldown", "Verdict cooldown (seconds)", 0f, 300f, 1f, config.getDouble(PATH + "verdict-cooldown", 30)),
                                    number("verdict_damage", "Verdict damage (2 = 1 heart)", 0f, 200f, 1f, config.getDouble(PATH + "verdict-damage", 40.0)),
                                    toggle("smash_enabled", "Enable mace smash", config.getBoolean(PATH + "mace-smash.enabled", true)),
                                    number("smash_damage", "Mace smash damage multiplier", 0.1f, 10f, 0.1f, config.getDouble(PATH + "mace-smash.damage-multiplier", 1.0)),
                                    number("smash_fall", "Minimum smash fall distance", 0f, 40f, 0.5f, config.getDouble(PATH + "mace-smash.min-fall-distance", 1.5)),
                                    toggle("particles", "Enable particles", config.getBoolean(PATH + "particles.enabled", true)),
                                    toggle("death_effect", "Skull effect on death", config.getBoolean(PATH + "death-effect.enabled", true)),
                                    toggle("prevent_storage", "Prevent container storage", config.getBoolean(PATH + "prevent-storage", true)),
                                    toggle("prevent_drop", "Prevent dropping", config.getBoolean(PATH + "prevent-drop", false))))
                            .build())
                    .type(
                        DialogType.confirmation(
                            ActionButton.create(
                                Component.text("Save Settings"),
                                Component.text("Save all Hammer settings to items.yml"),
                                150,
                                DialogAction.customClick(
                                    (view, audience) -> {
                                      if (audience instanceof Player submittingPlayer) {
                                        save(view, submittingPlayer);
                                      }
                                    },
                                    ClickCallback.Options.builder().uses(1).build())),
                            ActionButton.create(
                                Component.text("Cancel"),
                                Component.text("Discard changes"),
                                150,
                                null))));
    player.showDialog(dialog);
  }

  private void save(
      @NotNull io.papermc.paper.dialog.DialogResponseView view, @NotNull Player player) {
    Map<String, String> values = new LinkedHashMap<>();
    values.put(PATH + "enabled", String.valueOf(bool(view, "enabled")));
    values.put(PATH + "auto-give-on-start", String.valueOf(bool(view, "auto_start")));
    values.put(PATH + "dash-cooldown", String.valueOf(integer(view, "dash_cooldown")));
    values.put(PATH + "dash-speed", String.valueOf(number(view, "dash_speed")));
    values.put(PATH + "combo-required", String.valueOf(integer(view, "combo_required")));
    values.put(PATH + "combo-damage-multiplier", String.valueOf(number(view, "combo_damage")));
    values.put(PATH + "combo-explosion-power", String.valueOf(number(view, "combo_explosion")));
    values.put(PATH + "combo-reset-time", String.valueOf(integer(view, "combo_reset")));
    values.put(PATH + "miss-cooldown", String.valueOf(integer(view, "miss_cooldown")));
    values.put(PATH + "verdict-cooldown", String.valueOf(integer(view, "verdict_cooldown")));
    values.put(PATH + "verdict-damage", String.valueOf(number(view, "verdict_damage")));
    values.put(PATH + "mace-smash.enabled", String.valueOf(bool(view, "smash_enabled")));
    values.put(PATH + "mace-smash.damage-multiplier", String.valueOf(number(view, "smash_damage")));
    values.put(PATH + "mace-smash.min-fall-distance", String.valueOf(number(view, "smash_fall")));
    values.put(PATH + "particles.enabled", String.valueOf(bool(view, "particles")));
    values.put(PATH + "death-effect.enabled", String.valueOf(bool(view, "death_effect")));
    values.put(PATH + "prevent-storage", String.valueOf(bool(view, "prevent_storage")));
    values.put(PATH + "prevent-drop", String.valueOf(bool(view, "prevent_drop")));
    if (!plugin.getConfigUpdater().updateValues(values)) {
      player.sendMessage(Component.text("Hammer settings could not be saved. Your config was left unchanged.", NamedTextColor.RED));
      return;
    }
    plugin.reload();
    player.sendMessage(Component.text("Hierarchy Hammer settings saved to items.yml.", NamedTextColor.GREEN));
  }

  private static DialogInput toggle(String key, String label, boolean initial) {
    return DialogInput.bool(key, Component.text(label), initial, "Enabled", "Disabled");
  }

  private static DialogInput number(String key, String label, float min, float max, float step, double initial) {
    return DialogInput.numberRange(key, Component.text(label), min, max)
        .step(step)
        .initial((float) initial)
        .width(300)
        .build();
  }

  private static boolean bool(io.papermc.paper.dialog.DialogResponseView view, String key) {
    return Boolean.TRUE.equals(view.getBoolean(key));
  }

  private static double number(io.papermc.paper.dialog.DialogResponseView view, String key) {
    Float value = view.getFloat(key);
    return value == null ? 0D : value.doubleValue();
  }

  private static int integer(io.papermc.paper.dialog.DialogResponseView view, String key) {
    return Math.round((float) number(view, key));
  }
}
