package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * ConfigUpdater that merges new keys from the default config into the user's config while
 * preserving ALL comments and formatting in the user's file.
 *
 * <p>How it works:
 *
 * <ol>
 *   <li>Parse both files into a tree of "segments" — a segment is a comment block + key + value +
 *       optional child segments, preserving exact raw lines.
 *   <li>Walk the default tree. For every key in the default that is missing from the user's tree,
 *       insert the default's entire segment (with all comments) at the correct position (after the
 *       preceding sibling in the default order).
 *   <li>Never overwrite user values. The user's file is modified only to add new keys.
 *   <li>For old versions, preserve all user values, add missing defaults, and migrate renamed keys.
 * </ol>
 */
public class ConfigUpdater {

  private final RankedSMPXPlugin plugin;
  private final File configFile;
  private final File backupsFolder;
  private static final int TARGET_VERSION = 20;

  public static int getTargetVersion() {
    return TARGET_VERSION;
  }

  public ConfigUpdater(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), "config.yml");
    this.backupsFolder = new File(plugin.getDataFolder(), "backups");
  }

  public void updateConfig() {
    if (!configFile.exists()) {
      plugin.saveDefaultConfig();
      plugin.getLogger().info("Created default config.yml");
      return;
    }

    try {
      FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(configFile);
      int currentVersion = currentConfig.getInt("config-version", 0);

      mergeMissingKeys(currentConfig, currentVersion);
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to update config: " + e.getMessage());
      e.printStackTrace();
    }
    mergeMissingItemKeys();
  }

  private void mergeMissingItemKeys() {
    File itemsFile = new File(plugin.getDataFolder(), ConfigLayout.ITEMS);
    if (!itemsFile.exists()) {
      return;
    }
    try {
      SegmentTree current =
          SegmentTree.parse(Files.readAllLines(itemsFile.toPath(), StandardCharsets.UTF_8));
      SegmentTree defaults = SegmentTree.parse(readDefaultLines(ConfigLayout.ITEMS));
      int added = current.mergeFrom(defaults);
      if (added == 0) {
        return;
      }
      if (!createBackup(itemsFile)) {
        plugin.getLogger().severe("items.yml update cancelled because a backup could not be created.");
        return;
      }
      writeAtomically(itemsFile, current.toLines());
      plugin.reloadConfig();
      plugin.getLogger().info("Added " + added + " new option(s) to items.yml");
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to update items.yml: " + e.getMessage());
    }
  }

  private boolean createBackup(File file) {
    try {
      if (!backupsFolder.exists() && !backupsFolder.mkdirs()) {
        throw new IOException("Could not create backups directory");
      }
      String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss_SSS").format(new Date());
      String base = file.getName().replace(".yml", "");
      File backupFile = new File(backupsFolder, base + "_" + timestamp + ".yml");
      Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
      plugin.getLogger().info("Backup created: backups/" + backupFile.getName());
      return true;
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to create config backup: " + e.getMessage());
      return false;
    }
  }

  private void mergeMissingKeys(FileConfiguration currentConfig, int currentVersion)
      throws IOException {
    List<String> currentLines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
    List<String> defaultLines = readDefaultLines(ConfigLayout.MAIN);

    SegmentTree current = SegmentTree.parse(currentLines);
    SegmentTree defaults = SegmentTree.parse(defaultLines);

    int migrated = migrateLegacyRankDisplay(current, defaults);
    if (currentVersion < 15) {
      migrated += migrateHammerComboDefaults(current);
    }
    if (currentVersion < 16
        && isNumber(current.getRawValue("hierarchy-hammer.mace-smash.damage-multiplier"), 2.0)
        && current.setValue("hierarchy-hammer.mace-smash.damage-multiplier", "1.0")) {
      migrated++;
    }
    int added = current.mergeFrom(defaults);
    boolean versionUpdated =
        currentVersion < TARGET_VERSION
            && current.setValue("config-version", String.valueOf(TARGET_VERSION));
    if (added > 0 || migrated > 0 || versionUpdated) {
      if (!createBackup(configFile)) {
        plugin.getLogger().severe("Config update cancelled because a backup could not be created.");
        return;
      }
      writeAtomically(configFile, current.toLines());
      plugin.reloadConfig();
      plugin
          .getLogger()
          .info(
              "Updated config from version "
                  + currentVersion
                  + " to "
                  + TARGET_VERSION
                  + ": added "
                  + added
                  + " key(s) and migrated "
                  + migrated
                  + " legacy rank display setting(s) without overwriting user values");
    }
  }

  /**
   * Saves selected scalar config values without discarding comments or unrelated settings. Each
   * key is written to the file that owns its section (config.yml or items.yml).
   */
  public boolean updateValues(@NotNull Map<String, String> values) {
    if (values.isEmpty()) {
      return true;
    }
    Map<String, Map<String, String>> byFile = new java.util.LinkedHashMap<>();
    values.forEach(
        (path, value) ->
            byFile
                .computeIfAbsent(ConfigLayout.fileFor(path), f -> new java.util.LinkedHashMap<>())
                .put(path, value));

    boolean ok = true;
    for (Map.Entry<String, Map<String, String>> entry : byFile.entrySet()) {
      ok &= updateValuesIn(new File(plugin.getDataFolder(), entry.getKey()), entry.getValue());
    }
    if (ok) {
      plugin.reloadConfig();
    }
    return ok;
  }

  public boolean updateValuesIn(File file, Map<String, String> values) {
    if (!file.exists()) {
      plugin.getLogger().warning("Config save cancelled because " + file.getName() + " does not exist.");
      return false;
    }

    try {
      SegmentTree current =
          SegmentTree.parse(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
      boolean changed = false;
      for (Map.Entry<String, String> entry : values.entrySet()) {
        changed |= current.setValue(entry.getKey(), entry.getValue());
      }
      if (!changed) {
        return true;
      }
      if (!createBackup(file)) {
        plugin.getLogger().severe("Config save cancelled because a backup could not be created.");
        return false;
      }
      writeAtomically(file, current.toLines());
      return true;
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save " + file.getName() + " changes: " + e.getMessage());
      return false;
    }
  }

  private void writeAtomically(File file, @NotNull List<String> lines) throws IOException {
    Path configPath = file.toPath();
    Path tempFile = Files.createTempFile(configPath.getParent(), file.getName() + ".", ".tmp");
    try {
      Files.write(tempFile, lines, StandardCharsets.UTF_8);
      try {
        Files.move(
            tempFile,
            configPath,
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ignored) {
        Files.move(tempFile, configPath, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }

  private int migrateHammerComboDefaults(SegmentTree current) {
    int migrated = 0;
    if (isNumber(current.getRawValue("hierarchy-hammer.combo-damage-multiplier"), 2.0)
        && current.setValue("hierarchy-hammer.combo-damage-multiplier", "1.0")) {
      migrated++;
    }
    if (isNumber(current.getRawValue("hierarchy-hammer.combo-explosion-power"), 2.0)
        && current.setValue("hierarchy-hammer.combo-explosion-power", "0.0")) {
      migrated++;
    }
    return migrated;
  }

  private static boolean isNumber(String raw, double expected) {
    if (raw == null) {
      return false;
    }
    try {
      return Double.parseDouble(raw.trim()) == expected;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private int migrateLegacyRankDisplay(SegmentTree current, SegmentTree defaults) {
    int migrated = 0;
    boolean hasRankPrefixes = current.has("tab-integration.rank-prefixes");
    boolean hasRankSuffixes = current.has("tab-integration.rank-suffixes");
    String legacyPrefixFormat = current.getRawValue("tab-integration.prefix-format");
    String legacySuffixFormat = current.getRawValue("tab-integration.suffix-format");

    for (int rank = 1; rank <= 20; rank++) {
      String prefixPath = "tab-integration.rank-prefixes." + rank;
      String suffixPath = "tab-integration.rank-suffixes." + rank;
      String customPrefix = current.getRawValue("tab-integration.custom-rank-prefixes." + rank);

      if (!current.has(prefixPath)) {
        String value =
            customPrefix != null
                ? customPrefix
                : !hasRankPrefixes && legacyPrefixFormat != null
                    ? legacyPrefixFormat.replace("%rank%", String.valueOf(rank))
                    : null;
        if (value != null && current.addDefaultValue(defaults, prefixPath, value)) {
          migrated++;
        }
      }

      if (!current.has(suffixPath)
          && !hasRankSuffixes
          && legacySuffixFormat != null
          && current.addDefaultValue(
              defaults, suffixPath, legacySuffixFormat.replace("%rank%", String.valueOf(rank)))) {
        migrated++;
      }
    }
    return migrated;
  }

  private List<String> readDefaultLines(String resource) {
    try (java.io.InputStream is = plugin.getResource(resource)) {
      if (is == null) {
        plugin.getLogger().severe("Default " + resource + " not found in JAR!");
        return List.of();
      }
      byte[] bytes = is.readAllBytes();
      String text = new String(bytes, StandardCharsets.UTF_8);
      return new ArrayList<>(List.of(text.split("\n", -1)));
    } catch (IOException e) {
      plugin.getLogger().severe("Failed to read default " + resource + " from JAR: " + e.getMessage());
      return List.of();
    }
  }

  static class Segment {
    final List<String> header = new ArrayList<>();
    final List<String> extraLines = new ArrayList<>(); // list items, continuation lines
    String rawKey;
    String keyPath; // full dot-separated path
    int indent;
    String value; // raw value string, null if section or list
    String inlineComment; // inline comment on section line, e.g. "# comment"
    boolean isSection;
    boolean isListItem;
    final List<Segment> children = new ArrayList<>();

    Segment copyWithNewParent(String parentPath) {
      Segment c = new Segment();
      c.header.addAll(header);
      c.extraLines.addAll(extraLines);
      c.rawKey = rawKey;
      String cleanKey = SegmentTree.unquoteKey(rawKey);
      c.keyPath = parentPath.isEmpty() ? cleanKey : parentPath + "." + cleanKey;
      c.indent = indent;
      c.value = value;
      c.inlineComment = inlineComment;
      c.isSection = isSection;
      c.isListItem = isListItem;
      for (Segment child : children) {
        c.children.add(child.copyWithNewParent(c.keyPath));
      }
      return c;
    }

    List<String> toLines() {
      List<String> out = new ArrayList<>();
      for (String h : header) out.add(h);
      String prefix = " ".repeat(indent);
      if (isSection) {
        String commentPart = (inlineComment != null && !inlineComment.isEmpty()) ? " " + inlineComment : "";
        out.add(prefix + rawKey + ":" + commentPart);
        for (String ex : extraLines) out.add(ex);
        for (Segment child : children) {
          out.addAll(child.toLines());
        }
      } else if (isListItem) {
        out.add(" ".repeat(indent) + "- " + value);
        for (String ex : extraLines) out.add(ex);
      } else {
        String valStr = value != null ? value : "";
        out.add(prefix + rawKey + ": " + valStr);
        for (String ex : extraLines) out.add(ex);
      }
      return out;
    }

    @Override
    public String toString() {
      return "Segment{" + keyPath + "}";
    }
  }

  static class SegmentTree {
    final List<Segment> roots = new ArrayList<>();
    final List<String> footer = new ArrayList<>();

    final Map<String, Segment> lookup = new LinkedHashMap<>();

    static String unquoteKey(String key) {
      if (key == null) return "";
      String k = key.trim();
      if ((k.startsWith("\"") && k.endsWith("\"") && k.length() >= 2)
          || (k.startsWith("'") && k.endsWith("'") && k.length() >= 2)) {
        return k.substring(1, k.length() - 1).trim();
      }
      return k;
    }

    static int findInlineCommentIndex(String val) {
      if (val == null || val.isEmpty()) return -1;
      boolean inSingle = false;
      boolean inDouble = false;
      for (int i = 0; i < val.length(); i++) {
        char c = val.charAt(i);
        if (c == '\'' && !inDouble) {
          inSingle = !inSingle;
        } else if (c == '"' && !inSingle) {
          inDouble = !inDouble;
        } else if (c == '#' && !inSingle && !inDouble) {
          if (i == 0 || Character.isWhitespace(val.charAt(i - 1))) {
            return i;
          }
        }
      }
      return -1;
    }

    static SegmentTree parse(List<String> lines) {
      SegmentTree tree = new SegmentTree();
      tree.build(lines);
      return tree;
    }

    private void build(List<String> lines) {
      List<Segment> stack = new ArrayList<>();
      List<String> pendingHeader = new ArrayList<>();
      Segment currentRoot = null;

      for (String line : lines) {
        String trimmed = line.trim();

        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          pendingHeader.add(line);
          continue;
        }

        int lineIndent = countIndentRaw(line);

        if (trimmed.startsWith("- ")) {
          String val = trimmed.substring(2).trim();
          Segment top = stack.isEmpty() ? null : stack.get(stack.size() - 1);
          if (top != null && top.isSection) {
            Segment item = new Segment();
            item.isListItem = true;
            item.value = val;
            item.indent = lineIndent;
            item.keyPath = top.keyPath + "[-]";
            if (!pendingHeader.isEmpty()) {
              item.header.addAll(pendingHeader);
              pendingHeader.clear();
            }
            top.children.add(item);
          } else {
            if (!stack.isEmpty()) {
              stack.get(stack.size() - 1).extraLines.add(line);
            } else if (currentRoot != null) {
              currentRoot.extraLines.add(line);
            }
            pendingHeader.clear();
          }
          continue;
        }

        int colonIdx = findKeyColon(line);
        if (colonIdx < 0) {
          if (!stack.isEmpty()) {
            stack.get(stack.size() - 1).extraLines.add(line);
          } else if (currentRoot != null) {
            currentRoot.extraLines.add(line);
          }
          pendingHeader.clear();
          continue;
        }

        int keyIndent = countIndent(line, colonIdx);
        String rawKey = line.substring(keyIndent, colonIdx);
        String cleanKey = unquoteKey(rawKey);
        String valPart = line.substring(colonIdx + 1).trim();

        while (!stack.isEmpty()) {
          Segment top = stack.get(stack.size() - 1);
          if (keyIndent > top.indent) break;
          stack.remove(stack.size() - 1);
        }

        String fullPath;
        if (!stack.isEmpty()) {
          fullPath = stack.get(stack.size() - 1).keyPath + "." + cleanKey;
        } else {
          fullPath = cleanKey;
        }

        Segment seg = new Segment();
        seg.header.addAll(pendingHeader);
        pendingHeader.clear();
        seg.rawKey = rawKey;
        seg.keyPath = fullPath;
        seg.indent = keyIndent;

        boolean hasOnlyComment = valPart.startsWith("#");
        seg.isSection = valPart.isEmpty() || hasOnlyComment;
        if (hasOnlyComment) {
          seg.inlineComment = valPart;
          seg.value = null;
        } else {
          seg.value = seg.isSection ? null : valPart;
        }

        if (!stack.isEmpty()) {
          stack.get(stack.size() - 1).children.add(seg);
        } else {
          roots.add(seg);
          currentRoot = seg;
        }

        stack.add(seg);
        lookup.put(fullPath, seg);
      }

      if (!pendingHeader.isEmpty()) {
        footer.addAll(pendingHeader);
        pendingHeader.clear();
      }
    }

    private static int findKeyColon(String line) {
      for (int i = 0; i < line.length(); i++) {
        char c = line.charAt(i);
        if (c == ':') {
          boolean isEndOrSpace =
              (i + 1 == line.length())
                  || (line.charAt(i + 1) == ' ')
                  || (line.charAt(i + 1) == '\t')
                  || line.substring(i + 1).isBlank();
          if (isEndOrSpace) {
            String before = line.substring(0, i).trim();
            if (!before.isEmpty()
                && (before.matches("[\\w\\-\\.]+")
                    || before.matches("\"[^\"]+\"")
                    || before.matches("'[^']+'"))) {
              return i;
            }
          }
        }
      }
      return -1;
    }

    private static int countIndent(String line, int colonIdx) {
      int count = 0;
      for (int i = 0; i < line.length(); i++) {
        if (line.charAt(i) == ' ') count++;
        else break;
      }
      return count;
    }

    private static int countIndentRaw(String line) {
      int count = 0;
      for (int i = 0; i < line.length(); i++) {
        if (line.charAt(i) == ' ') count++;
        else break;
      }
      return count;
    }

    List<String> toLines() {
      List<String> out = new ArrayList<>();
      for (Segment root : roots) {
        out.addAll(root.toLines());
      }
      out.addAll(footer);
      return out;
    }

    int mergeFrom(SegmentTree other) {
      int[] count = {0};
      mergeSegments(roots, other.roots, "", count);
      return count[0];
    }

    boolean has(String keyPath) {
      return lookup.containsKey(keyPath);
    }

    String getRawValue(String keyPath) {
      Segment segment = lookup.get(keyPath);
      if (segment == null || segment.isSection || segment.value == null) {
        return null;
      }
      int hashIdx = findInlineCommentIndex(segment.value);
      return hashIdx >= 0 ? segment.value.substring(0, hashIdx).trim() : segment.value.trim();
    }

    boolean setValue(String keyPath, String value) {
      Segment segment = lookup.get(keyPath);
      if (segment == null || segment.isSection) {
        return false;
      }
      int hashIdx = findInlineCommentIndex(segment.value);
      String currentBareValue =
          hashIdx >= 0
              ? segment.value.substring(0, hashIdx).trim()
              : (segment.value != null ? segment.value.trim() : "");
      if (value.equals(currentBareValue)) {
        return false;
      }
      if (hashIdx >= 0) {
        segment.value = value + " " + segment.value.substring(hashIdx).trim();
      } else {
        segment.value = value;
      }
      return true;
    }

    boolean addDefaultValue(SegmentTree defaults, String keyPath, String value) {
      if (has(keyPath)) {
        return false;
      }

      int separator = keyPath.lastIndexOf('.');
      String parentPath = separator < 0 ? "" : keyPath.substring(0, separator);
      String rawKey = separator < 0 ? keyPath : keyPath.substring(separator + 1);
      ensureSection(defaults, parentPath);

      Segment parent = parentPath.isEmpty() ? null : lookup.get(parentPath);
      if (!parentPath.isEmpty() && parent == null) {
        return false;
      }

      Segment template = defaults.lookup.get(keyPath);
      Segment added;
      if (template != null) {
        added = template.copyWithNewParent(parentPath);
        added.value = value;
      } else {
        added = new Segment();
        added.rawKey = rawKey;
        String cleanKey = unquoteKey(rawKey);
        added.keyPath = parentPath.isEmpty() ? cleanKey : parentPath + "." + cleanKey;
        added.indent = parent == null ? 0 : parent.indent + 2;
        added.value = value;
      }

      if (parent == null) {
        roots.add(added);
      } else {
        parent.children.add(added);
      }
      registerSubtree(added);
      return true;
    }

    private void ensureSection(SegmentTree defaults, String keyPath) {
      if (keyPath.isEmpty() || has(keyPath)) {
        return;
      }
      int separator = keyPath.lastIndexOf('.');
      String parentPath = separator < 0 ? "" : keyPath.substring(0, separator);
      ensureSection(defaults, parentPath);

      Segment template = defaults.lookup.get(keyPath);
      Segment copy;
      if (template != null) {
        copy = template.copyWithNewParent(parentPath);
        copy.children.clear();
      } else {
        String rawKey = separator < 0 ? keyPath : keyPath.substring(separator + 1);
        String cleanKey = unquoteKey(rawKey);
        copy = new Segment();
        copy.rawKey = rawKey;
        copy.keyPath = parentPath.isEmpty() ? cleanKey : parentPath + "." + cleanKey;
        copy.isSection = true;
        Segment parent = parentPath.isEmpty() ? null : lookup.get(parentPath);
        copy.indent = parent == null ? 0 : parent.indent + 2;
      }

      if (parentPath.isEmpty()) {
        roots.add(copy);
      } else {
        Segment parent = lookup.get(parentPath);
        if (parent == null) {
          return;
        }
        parent.children.add(copy);
      }
      registerSubtree(copy);
    }

    private void mergeSegments(
        List<Segment> mySiblings, List<Segment> theirSiblings, String parentPath, int[] count) {
      for (Segment their : theirSiblings) {
        // Skip list items — they share keyPath "[-]" so we can't detect individual ones
        if (their.isListItem) continue;

        String fullPath = their.keyPath;
        Segment mine = lookup.get(fullPath);

        if (mine == null) {
          int insertAt = findInsertIndex(mySiblings, their, theirSiblings);
          Segment copy = their.copyWithNewParent(parentPath);
          mySiblings.add(insertAt, copy);
          registerSubtree(copy);
          count[0]++;
        } else if (their.isSection) {
          mergeSegments(mine.children, their.children, fullPath, count);
        }
      }
    }

    private void registerSubtree(Segment seg) {
      lookup.put(seg.keyPath, seg);
      for (Segment child : seg.children) {
        registerSubtree(child);
      }
    }

    /**
     * Find where to insert a missing segment among siblings, trying to preserve the ordering from
     * the default. Look for the segment that precedes {@code target} in the default's sibling list;
     * if that predecessor exists among mySiblings, insert right after it. Otherwise append.
     */
    private static int findInsertIndex(
        List<Segment> mySiblings, Segment target, List<Segment> theirSiblings) {
      if (mySiblings.isEmpty()) return 0;
      int targetIdx = theirSiblings.indexOf(target);
      if (targetIdx < 0) return mySiblings.size();

      for (int i = targetIdx - 1; i >= 0; i--) {
        Segment predecessor = theirSiblings.get(i);
        for (int j = mySiblings.size() - 1; j >= 0; j--) {
          if (mySiblings.get(j).keyPath.equals(predecessor.keyPath)) {
            return j + 1;
          }
        }
      }

      for (int i = targetIdx + 1; i < theirSiblings.size(); i++) {
        Segment successor = theirSiblings.get(i);
        for (int j = 0; j < mySiblings.size(); j++) {
          if (mySiblings.get(j).keyPath.equals(successor.keyPath)) {
            return j;
          }
        }
      }

      return mySiblings.size();
    }
  }
}
