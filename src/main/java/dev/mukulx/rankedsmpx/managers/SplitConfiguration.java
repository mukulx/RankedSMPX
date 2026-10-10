package dev.mukulx.rankedsmpx.managers;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One configuration view over config.yml and items.yml. Every path is routed to the file that owns
 * its top-level section, so callers do not need to know where each setting is stored.
 */
public final class SplitConfiguration extends YamlConfiguration {

  private final YamlConfiguration main;
  private final Map<String, YamlConfiguration> files;
  private final Map<String, File> locations;

  SplitConfiguration(
      @NotNull YamlConfiguration main,
      @NotNull Map<String, YamlConfiguration> others,
      @NotNull Map<String, File> locations) {
    this.main = main;
    this.files = new LinkedHashMap<>(others);
    this.files.put(ConfigLayout.MAIN, main);
    this.locations = locations;
    setDefaults(new Routed());
  }

  private YamlConfiguration owner(String path) {
    int dot = path.indexOf('.');
    String file = ConfigLayout.fileForSection(dot < 0 ? path : path.substring(0, dot));
    return files.getOrDefault(file, main);
  }

  @Override
  public Object get(@NotNull String path, @Nullable Object def) {
    return path.isEmpty() ? this : owner(path).get(path, def);
  }

  @Override
  public void set(@NotNull String path, @Nullable Object value) {
    owner(path).set(path, value);
  }

  @Override
  public ConfigurationSection getConfigurationSection(@NotNull String path) {
    return owner(path).getConfigurationSection(path);
  }

  @NotNull
  @Override
  public ConfigurationSection createSection(@NotNull String path) {
    return owner(path).createSection(path);
  }

  @NotNull
  @Override
  public Set<String> getKeys(boolean deep) {
    Set<String> keys = new LinkedHashSet<>();
    for (YamlConfiguration file : files.values()) {
      keys.addAll(file.getKeys(deep));
    }
    return keys;
  }

  public void saveAll() throws IOException {
    for (Map.Entry<String, File> entry : locations.entrySet()) {
      files.get(entry.getKey()).save(entry.getValue());
    }
  }

  private final class Routed extends MemoryConfiguration {
    @Override
    public Object get(@NotNull String path, @Nullable Object def) {
      if (path.isEmpty()) {
        return this;
      }
      Configuration defaults = owner(path).getDefaults();
      return defaults == null ? def : defaults.get(path, def);
    }
  }
}
