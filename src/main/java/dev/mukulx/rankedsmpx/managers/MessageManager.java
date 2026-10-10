package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MessageManager {

  private final RankedSMPXPlugin plugin;
  private ConfigManager configManager;
  private FileConfiguration messages;
  private Component prefix;
  private boolean usePrefix;

  private final MiniMessage miniMessage = MiniMessage.miniMessage();
  private final LegacyComponentSerializer legacySerializer =
      LegacyComponentSerializer.legacySection();

  private static final LegacyComponentSerializer LEGACY_SECTION =
      LegacyComponentSerializer.builder()
          .character(LegacyComponentSerializer.SECTION_CHAR)
          .hexColors()
          .useUnusualXRepeatedCharacterHexFormat()
          .build();

  @org.jetbrains.annotations.Contract("null -> null; !null -> !null")
  public static Component legacy(@Nullable String legacyText) {
    return legacyText == null ? null : LEGACY_SECTION.deserialize(legacyText);
  }

  @org.jetbrains.annotations.Contract("null -> null; !null -> !null")
  public static java.util.List<Component> legacyLines(@Nullable java.util.List<String> lines) {
    return lines == null ? null : lines.stream().map(MessageManager::legacy).toList();
  }

  @NotNull
  public static String legacyString(@Nullable Component component) {
    return component == null ? "" : LEGACY_SECTION.serialize(component);
  }

  @NotNull
  public static String plain(@Nullable Component component) {
    return component == null
        ? ""
        : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
            .serialize(component);
  }

  private static final Map<Character, Character> SMALL_CAPS_MAP = new HashMap<>();

  static {
    SMALL_CAPS_MAP.put('a', 'ᴀ');
    SMALL_CAPS_MAP.put('b', 'ʙ');
    SMALL_CAPS_MAP.put('c', 'ᴄ');
    SMALL_CAPS_MAP.put('d', 'ᴅ');
    SMALL_CAPS_MAP.put('e', 'ᴇ');
    SMALL_CAPS_MAP.put('f', 'ꜰ');
    SMALL_CAPS_MAP.put('g', 'ɢ');
    SMALL_CAPS_MAP.put('h', 'ʜ');
    SMALL_CAPS_MAP.put('i', 'ɪ');
    SMALL_CAPS_MAP.put('j', 'ᴊ');
    SMALL_CAPS_MAP.put('k', 'ᴋ');
    SMALL_CAPS_MAP.put('l', 'ʟ');
    SMALL_CAPS_MAP.put('m', 'ᴍ');
    SMALL_CAPS_MAP.put('n', 'ɴ');
    SMALL_CAPS_MAP.put('o', 'ᴏ');
    SMALL_CAPS_MAP.put('p', 'ᴘ');
    SMALL_CAPS_MAP.put('q', 'ǫ');
    SMALL_CAPS_MAP.put('r', 'ʀ');
    SMALL_CAPS_MAP.put('s', 'ꜱ');
    SMALL_CAPS_MAP.put('t', 'ᴛ');
    SMALL_CAPS_MAP.put('u', 'ᴜ');
    SMALL_CAPS_MAP.put('v', 'ᴠ');
    SMALL_CAPS_MAP.put('w', 'ᴡ');
    SMALL_CAPS_MAP.put('x', 'x');
    SMALL_CAPS_MAP.put('y', 'ʏ');
    SMALL_CAPS_MAP.put('z', 'ᴢ');

    SMALL_CAPS_MAP.put('A', 'ᴀ');
    SMALL_CAPS_MAP.put('B', 'ʙ');
    SMALL_CAPS_MAP.put('C', 'ᴄ');
    SMALL_CAPS_MAP.put('D', 'ᴅ');
    SMALL_CAPS_MAP.put('E', 'ᴇ');
    SMALL_CAPS_MAP.put('F', 'ꜰ');
    SMALL_CAPS_MAP.put('G', 'ɢ');
    SMALL_CAPS_MAP.put('H', 'ʜ');
    SMALL_CAPS_MAP.put('I', 'ɪ');
    SMALL_CAPS_MAP.put('J', 'ᴊ');
    SMALL_CAPS_MAP.put('K', 'ᴋ');
    SMALL_CAPS_MAP.put('L', 'ʟ');
    SMALL_CAPS_MAP.put('M', 'ᴍ');
    SMALL_CAPS_MAP.put('N', 'ɴ');
    SMALL_CAPS_MAP.put('O', 'ᴏ');
    SMALL_CAPS_MAP.put('P', 'ᴘ');
    SMALL_CAPS_MAP.put('Q', 'ǫ');
    SMALL_CAPS_MAP.put('R', 'ʀ');
    SMALL_CAPS_MAP.put('S', 'ꜱ');
    SMALL_CAPS_MAP.put('T', 'ᴛ');
    SMALL_CAPS_MAP.put('U', 'ᴜ');
    SMALL_CAPS_MAP.put('V', 'ᴠ');
    SMALL_CAPS_MAP.put('W', 'ᴡ');
    SMALL_CAPS_MAP.put('X', 'x');
    SMALL_CAPS_MAP.put('Y', 'ʏ');
    SMALL_CAPS_MAP.put('Z', 'ᴢ');
  }

  public MessageManager(@NotNull RankedSMPXPlugin plugin, @NotNull ConfigManager configManager) {
    this.plugin = plugin;
    this.configManager = configManager;
    loadMessages();
  }

  private void loadMessages() {
    File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

    if (!messagesFile.exists()) {
      plugin.saveResource("messages.yml", false);
      plugin.getLogger().info("Created new messages.yml");
    }

    new MessagesUpdater(plugin).update(messagesFile);

    try {
      this.messages = YamlConfiguration.loadConfiguration(messagesFile);
      try (java.io.InputStream in = plugin.getResource("messages.yml")) {
        if (in != null) {
          // Anything still missing falls back to the built-in text instead of an error message.
          messages.setDefaults(
              YamlConfiguration.loadConfiguration(
                  new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)));
        }
      }
      String prefixString = messages.getString("settings.prefix", "&8[&6RankedSMP&8]&r ");

      this.prefix = parseMessage(prefixString, true);
      this.usePrefix = configManager.getConfig().getBoolean("message-prefix", false);
    } catch (Exception e) {
      plugin.getLogger().severe("Failed to load messages.yml: " + e.getMessage());
      plugin.getLogger().severe("Using default values. Please fix messages.yml syntax!");

      if (this.messages == null) {
        this.messages = new YamlConfiguration();
      }
      if (this.prefix == null) {
        this.prefix = legacyToComponent("&8[&6RankedSMP&8]&r ");
      }
      this.usePrefix = configManager.getConfig().getBoolean("message-prefix", false);
    }
  }

  public void reload() {
    ConfigManager cm = plugin.getConfigManager();
    if (cm != null) {
      this.configManager = cm;
    }

    FileConfiguration oldMessages = this.messages;
    Component oldPrefix = this.prefix;
    boolean oldUsePrefix = this.usePrefix;

    try {
      loadMessages();
    } catch (Exception e) {
      plugin
          .getLogger()
          .warning("Failed to reload messages.yml, keeping old values: " + e.getMessage());
      this.messages = oldMessages;
      this.prefix = oldPrefix;
      this.usePrefix = oldUsePrefix;
    }
  }

  @NotNull
  public Component getMessageComponent(@NotNull String path) {
    return getMessageComponent(path, null);
  }

  @NotNull
  public Component getMessageComponent(
      @NotNull String path, @Nullable Map<String, String> placeholders) {
    String message = messages.getString(path);
    if (message == null) {
      return legacyToComponent("&cMessage not found: " + path);
    }

    if (message.contains("{prefix}")) {
      message = message.replace("{prefix}", usePrefix ? componentToLegacy(prefix) : "");
    }

    if (placeholders != null) {
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
        message = message.replace("{" + entry.getKey() + "}", entry.getValue());
      }
    }

    return parseMessage(message, true);
  }

  @NotNull
  public String getMessage(@NotNull String path) {
    return getMessage(path, null);
  }

  @NotNull
  public String getMessage(@NotNull String path, @Nullable Map<String, String> placeholders) {
    return componentToLegacy(getMessageComponent(path, placeholders));
  }

  public void sendMessage(@NotNull CommandSender sender, @NotNull String path) {
    sendMessage(sender, path, null);
  }

  public void sendMessage(
      @NotNull CommandSender sender,
      @NotNull String path,
      @Nullable Map<String, String> placeholders) {
    Component component = getMessageComponent(path, placeholders);

    sender.sendMessage(component);
  }

  public void sendMessage(@NotNull CommandSender sender, @NotNull Component component) {
    sender.sendMessage(component);
  }

  public void sendLegacyMessage(@NotNull CommandSender sender, @NotNull String legacyText) {
    sender.sendMessage(applySmallCapsComponent(legacyText));
  }

  public void sendPrefixedMessage(@NotNull CommandSender sender, @NotNull String legacyText) {
    Component text = applySmallCapsComponent(legacyText);
    sender.sendMessage(usePrefix ? prefix.append(Component.space()).append(text) : text);
  }

  public void sendTitle(
      @NotNull org.bukkit.entity.Player player,
      @NotNull String title,
      @NotNull String subtitle,
      int fadeIn,
      int stay,
      int fadeOut) {
    if (!titlesEnabled()) {
      return;
    }

    Component titleComponent = applySmallCapsComponent(title);
    Component subtitleComponent = applySmallCapsComponent(subtitle);

    net.kyori.adventure.title.Title adventureTitle =
        net.kyori.adventure.title.Title.title(
            titleComponent,
            subtitleComponent,
            net.kyori.adventure.title.Title.Times.times(
                java.time.Duration.ofMillis(fadeIn * 50L),
                java.time.Duration.ofMillis(stay * 50L),
                java.time.Duration.ofMillis(fadeOut * 50L)));

    player.showTitle(adventureTitle);
  }

  /** False when the server turned titles off in config.yml (titles: false). */
  public boolean titlesEnabled() {
    return configManager.getConfig().getBoolean("titles", true);
  }

  public void sendActionBar(@NotNull org.bukkit.entity.Player player, @NotNull String message) {
    Component component = applySmallCapsComponent(message);
    player.sendActionBar(component);
  }

  @NotNull
  public Component legacyToComponent(@NotNull String legacyText) {
    return legacySerializer.deserialize(colorize(legacyText));
  }

  @NotNull
  public Component parseMessage(@NotNull String text, boolean applySmallCaps) {
    if (applySmallCaps && configManager.getConfig().getBoolean("small-caps-messages", true)) {
      text = toSmallCapsPreservingTags(text);
    }

    if (text.contains("<") && text.contains(">")) {
      try {
        return miniMessage.deserialize(text);
      } catch (Exception e) {
        return legacyToComponent(text);
      }
    }

    return legacyToComponent(text);
  }

  @NotNull
  public Component parseMessage(@NotNull String text) {
    return parseMessage(text, false);
  }

  @NotNull
  public String toSmallCapsPreservingTags(@NotNull String text) {
    StringBuilder result = new StringBuilder();
    int length = text.length();
    int i = 0;

    while (i < length) {
      char c = text.charAt(i);

      if (c == '<') {
        int tagEnd = text.indexOf('>', i);
        if (tagEnd != -1) {
          result.append(text, i, tagEnd + 1);
          i = tagEnd + 1;
          continue;
        }
      }

      if ((c == '&' || c == '§') && i + 7 < length && text.charAt(i + 1) == '#') {
        String hexPart = text.substring(i + 2, i + 8);
        boolean isHex = true;
        for (int k = 0; k < hexPart.length(); k++) {
          char hc = hexPart.charAt(k);
          if (!((hc >= '0' && hc <= '9') || (hc >= 'a' && hc <= 'f') || (hc >= 'A' && hc <= 'F'))) {
            isHex = false;
            break;
          }
        }
        if (isHex) {
          result.append(text, i, i + 8);
          i += 8;
          continue;
        }
      }

      if ((c == '§' || c == '&') && i + 13 < length && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
        boolean isSectionHex = true;
        for (int k = 0; k < 14; k += 2) {
          if (text.charAt(i + k) != '§' && text.charAt(i + k) != '&') {
            isSectionHex = false;
            break;
          }
        }
        if (isSectionHex) {
          result.append(text, i, i + 14);
          i += 14;
          continue;
        }
      }

      if ((c == '&' || c == '§') && i + 1 < length) {
        char nextChar = text.charAt(i + 1);
        boolean isLegacyCode = (nextChar >= '0' && nextChar <= '9')
            || (nextChar >= 'a' && nextChar <= 'f')
            || (nextChar >= 'A' && nextChar <= 'F')
            || (nextChar >= 'k' && nextChar <= 'o')
            || (nextChar >= 'K' && nextChar <= 'O')
            || nextChar == 'r' || nextChar == 'R';
        if (isLegacyCode) {
          result.append(c);
          result.append(nextChar);
          i += 2;
          continue;
        }
      }

      Character smallCaps = SMALL_CAPS_MAP.get(c);
      if (smallCaps != null) {
        result.append(smallCaps);
      } else {
        result.append(c);
      }
      i++;
    }

    return result.toString();
  }

  @NotNull
  public String componentToLegacy(@NotNull Component component) {
    return legacySerializer.serialize(component);
  }

  @NotNull
  public String colorize(@NotNull String text) {
    text = translateHexColorCodes(text);
    return text.replace('&', '§');
  }

  @NotNull
  private String translateHexColorCodes(@NotNull String text) {
    java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("&#([0-9A-Fa-f]{6})");
    java.util.regex.Matcher matcher = hexPattern.matcher(text);
    StringBuilder result = new StringBuilder();

    while (matcher.find()) {
      String hexCode = matcher.group(1);
      StringBuilder minecraftHex = new StringBuilder("§x");
      for (char c : hexCode.toCharArray()) {
        minecraftHex.append('§').append(c);
      }
      matcher.appendReplacement(result, minecraftHex.toString());
    }
    matcher.appendTail(result);

    return result.toString();
  }

  @NotNull
  public String toSmallCaps(@NotNull String text) {
    return toSmallCapsPreservingTags(text);
  }

  @NotNull
  public Component applySmallCapsComponent(@NotNull String legacyText) {
    return parseMessage(legacyText, true);
  }

  @NotNull
  public String applySmallCaps(@NotNull String text) {
    if (configManager.getConfig().getBoolean("small-caps-messages", true)) {
      text = toSmallCapsPreservingTags(text);
    }
    return colorize(text);
  }

  @NotNull
  public Component getPrefixComponent() {
    return usePrefix ? prefix : Component.empty();
  }

  @NotNull
  public String getPrefix() {
    return usePrefix ? componentToLegacy(prefix) : "";
  }

  @NotNull
  public String convertLegacyToMiniMessage(@NotNull String text) {
    String result = text;
    result = result.replace("&0", "<black>");
    result = result.replace("&1", "<dark_blue>");
    result = result.replace("&2", "<dark_green>");
    result = result.replace("&3", "<dark_aqua>");
    result = result.replace("&4", "<dark_red>");
    result = result.replace("&5", "<dark_purple>");
    result = result.replace("&6", "<gold>");
    result = result.replace("&7", "<gray>");
    result = result.replace("&8", "<dark_gray>");
    result = result.replace("&9", "<blue>");
    result = result.replace("&a", "<green>");
    result = result.replace("&b", "<aqua>");
    result = result.replace("&c", "<red>");
    result = result.replace("&d", "<light_purple>");
    result = result.replace("&e", "<yellow>");
    result = result.replace("&f", "<white>");
    result = result.replace("&r", "<reset>");
    result = result.replace("&l", "<bold>");
    result = result.replace("&o", "<italic>");
    result = result.replace("&n", "<underlined>");
    result = result.replace("&m", "<strikethrough>");
    result = result.replace("&k", "<obfuscated>");
    return result;
  }

  @NotNull
  public String getRankPrefixMiniMessage(@Nullable Integer rank, boolean isBlacklisted) {
    String prefix = getRankPrefixLegacy(rank, isBlacklisted);
    prefix = prefix.replaceAll("&#([0-9A-Fa-f]{6})", "<#$1>");
    return convertLegacyToMiniMessage(prefix);
  }

  @NotNull
  public String getRankPrefixLegacy(@Nullable Integer rank, boolean isBlacklisted) {
    return rowFor(rank, isBlacklisted).prefix();
  }

  @NotNull
  public String getRankSuffixLegacy(@Nullable Integer rank, boolean isBlacklisted) {
    return rowFor(rank, isBlacklisted).suffix();
  }

  private RankTable.Row rowFor(@Nullable Integer rank, boolean isBlacklisted) {
    RankTable ranks = configManager.getRanks();
    if (isBlacklisted) {
      return ranks.blacklisted();
    }
    return rank != null && rank > 0 ? ranks.row(rank) : ranks.unranked();
  }

  @NotNull
  public String convertMiniMessageToLegacy(@NotNull String miniMessage) {
    String legacy = miniMessage;
    legacy = legacy.replace("<black>", "§0");
    legacy = legacy.replace("<dark_blue>", "§1");
    legacy = legacy.replace("<dark_green>", "§2");
    legacy = legacy.replace("<dark_aqua>", "§3");
    legacy = legacy.replace("<dark_red>", "§4");
    legacy = legacy.replace("<dark_purple>", "§5");
    legacy = legacy.replace("<gold>", "§6");
    legacy = legacy.replace("<gray>", "§7");
    legacy = legacy.replace("<dark_gray>", "§8");
    legacy = legacy.replace("<blue>", "§9");
    legacy = legacy.replace("<green>", "§a");
    legacy = legacy.replace("<aqua>", "§b");
    legacy = legacy.replace("<red>", "§c");
    legacy = legacy.replace("<light_purple>", "§d");
    legacy = legacy.replace("<yellow>", "§e");
    legacy = legacy.replace("<white>", "§f");
    legacy = legacy.replace("<reset>", "§r");
    legacy = legacy.replace("<bold>", "§l");
    legacy = legacy.replace("<italic>", "§o");
    legacy = legacy.replace("<underlined>", "§n");
    legacy = legacy.replace("<strikethrough>", "§m");
    legacy = legacy.replace("<obfuscated>", "§k");

    java.util.regex.Pattern hexPattern = java.util.regex.Pattern.compile("<#([0-9A-Fa-f]{6})>");
    java.util.regex.Matcher matcher = hexPattern.matcher(legacy);
    StringBuilder hexConverted = new StringBuilder();
    while (matcher.find()) {
      String hexCode = matcher.group(1);
      StringBuilder mcHex = new StringBuilder("§x");
      for (char c : hexCode.toCharArray()) {
        mcHex.append('§').append(c);
      }
      matcher.appendReplacement(hexConverted, mcHex.toString());
    }
    matcher.appendTail(hexConverted);
    return hexConverted.toString();
  }

}
