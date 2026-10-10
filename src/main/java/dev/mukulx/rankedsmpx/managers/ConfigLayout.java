package dev.mukulx.rankedsmpx.managers;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ConfigLayout {

  public static final String MAIN = "config.yml";
  public static final String ITEMS = "items.yml";
  private static final String LEGACY_ADVANCED = "advanced.yml";
  static final int ITEMS_VERSION = 18;
  static final int CURRENT_VERSION = 20;

  private static final Map<String, String> SECTION_FILE =
      Map.ofEntries(
          Map.entry("hierarchy-hammer", ITEMS),
          Map.entry("spear", ITEMS),
          Map.entry("dragon-egg-locator", ITEMS));

  private static final Set<String> FREEFORM = Set.of("spear.enchantments");

  private ConfigLayout() {}

  @NotNull
  static String fileForSection(@NotNull String section) {
    return SECTION_FILE.getOrDefault(section, MAIN);
  }

  @NotNull
  public static String fileFor(@NotNull String path) {
    int dot = path.indexOf('.');
    return fileForSection(dot < 0 ? path : path.substring(0, dot));
  }

  @NotNull
  public static SplitConfiguration load(@NotNull JavaPlugin plugin) {
    Map<String, YamlConfiguration> others = new LinkedHashMap<>();
    Map<String, File> locations = new LinkedHashMap<>();
    YamlConfiguration main = null;
    for (String name : new String[] {MAIN, ITEMS}) {
      File file = new File(plugin.getDataFolder(), name);
      if (!file.exists()) {
        plugin.saveResource(name, false);
      }
      YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
      try (Reader defaults = new InputStreamReader(plugin.getResource(name), StandardCharsets.UTF_8)) {
        yaml.setDefaults(YamlConfiguration.loadConfiguration(defaults));
      } catch (IOException | NullPointerException e) {
        plugin.getLogger().warning("No bundled defaults for " + name + ": " + e.getMessage());
      }
      locations.put(name, file);
      if (name.equals(MAIN)) {
        main = yaml;
      } else {
        others.put(name, yaml);
      }
    }
    return new SplitConfiguration(main, others, locations);
  }

  /**
   * Moves item settings to items.yml and folds settings from the former advanced.yml into
   * config.yml. The config version is written last so an interrupted upgrade can safely retry.
   */
  public static void migrate(@NotNull JavaPlugin plugin) {
    File dir = plugin.getDataFolder();
    Logger log = plugin.getLogger();
    File config = new File(dir, MAIN);
    if (!config.exists()) {
      return;
    }
    YamlConfiguration old = YamlConfiguration.loadConfiguration(config);
    int version = old.getInt("config-version", 0);

    File ranks = new File(dir, "ranks.yml");
    if (!ranks.exists() && version >= RankTableFile.FIRST_RANKS_YML_VERSION) {
      plugin.saveResource("ranks.yml", false);
    }
    RankTableFile.loadOrCreate(ranks, old, log);

    if (version >= CURRENT_VERSION) {
      return;
    }
    migrateLegacyHammerDefaults(old, version);
    try {
      File backups = new File(dir, "backups");
      if (!backups.exists() && !backups.mkdirs()) {
        throw new IOException("Could not create backups directory");
      }
      String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS").format(new Date());
      File backup = new File(backups, "config_v" + version + "_" + stamp + ".yml");
      Files.copy(config.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
      File advanced = new File(dir, LEGACY_ADVANCED);
      YamlConfiguration legacyAdvanced = null;
      if (advanced.isFile()) {
        File advancedBackup = new File(backups, "advanced_v" + version + "_" + stamp + ".yml");
        Files.copy(advanced.toPath(), advancedBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        legacyAdvanced = YamlConfiguration.loadConfiguration(advanced);
      }

      if (version < ITEMS_VERSION) {
        rebuild(plugin, new File(dir, ITEMS), ITEMS, old, k -> ITEMS.equals(fileForSection(k)));
      }
      rebuildMain(plugin, config, old, legacyAdvanced);
      Files.deleteIfExists(advanced.toPath());
      log.info(
          "Config upgraded from v"
              + version
              + ": settings now live in config.yml, items.yml and ranks.yml. Old files saved in backups/"
              + backup.getName());
    } catch (IOException e) {
      log.severe("Config upgrade failed, keeping the old config.yml: " + e.getMessage());
    }
  }

  private static void rebuildMain(
      JavaPlugin plugin, File target, YamlConfiguration old, YamlConfiguration legacyAdvanced)
      throws IOException {
    YamlConfiguration template;
    try (Reader in = new InputStreamReader(plugin.getResource(MAIN), StandardCharsets.UTF_8)) {
      template = YamlConfiguration.loadConfiguration(in);
    }
    if (legacyAdvanced != null) {
      copyValues(legacyAdvanced, template, k -> MAIN.equals(fileForSection(k)));
    }
    copyValues(
        old,
        template,
        k -> MAIN.equals(fileForSection(k)) && !k.equals("config-version"));
    File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
    template.save(tmp);
    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  private static void migrateLegacyHammerDefaults(YamlConfiguration old, int version) {
    if (version < 15) {
      resetDefault(old, "hierarchy-hammer.combo-damage-multiplier", 2.0, 1.0);
      resetDefault(old, "hierarchy-hammer.combo-explosion-power", 2.0, 0.0);
    }
    if (version < 16) {
      resetDefault(old, "hierarchy-hammer.mace-smash.damage-multiplier", 2.0, 1.0);
    }
  }

  private static void resetDefault(YamlConfiguration config, String path, double shipped, double corrected) {
    if (config.isSet(path) && config.getDouble(path) == shipped) {
      config.set(path, corrected);
    }
  }

  private static void rebuild(
      JavaPlugin plugin,
      File target,
      String resource,
      YamlConfiguration old,
      Predicate<String> keepSection)
      throws IOException {
    YamlConfiguration template;
    try (Reader in = new InputStreamReader(plugin.getResource(resource), StandardCharsets.UTF_8)) {
      template = YamlConfiguration.loadConfiguration(in);
    }
    copyValues(old, template, keepSection);

    File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
    template.save(tmp);
    Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  static void copyValues(YamlConfiguration old, YamlConfiguration template, Predicate<String> keepSection) {
    for (String path : old.getKeys(true)) {
      if (old.isConfigurationSection(path)) {
        continue;
      }
      int dot = path.indexOf('.');
      String top = dot < 0 ? path : path.substring(0, dot);
      if (!keepSection.test(top)) {
        continue;
      }
      int last = path.lastIndexOf('.');
      boolean freeform = last > 0 && FREEFORM.contains(path.substring(0, last));
      if (template.contains(path) || freeform) {
        template.set(path, old.get(path));
      }
    }
  }
}
