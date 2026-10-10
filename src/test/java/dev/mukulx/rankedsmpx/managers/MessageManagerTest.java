package dev.mukulx.rankedsmpx.managers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MessageManagerTest {

  @Mock private RankedSMPXPlugin plugin;

  @Mock private ConfigManager configManager;

  @Mock private FileConfiguration config;

  @Mock private Logger logger;

  @Mock private CommandSender sender;

  @TempDir File tempDir;

  private MessageManager messageManager;
  private FileConfiguration messages;
  private final LegacyComponentSerializer legacySerializer =
      LegacyComponentSerializer.legacySection();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    when(plugin.getLogger()).thenReturn(logger);
    when(plugin.getDataFolder()).thenReturn(tempDir);
    when(plugin.getConfigManager()).thenReturn(configManager);

    when(configManager.getConfig()).thenReturn(config);
    when(config.getBoolean("small-caps-messages", true)).thenReturn(false);
    when(config.getBoolean("message-prefix", false)).thenReturn(true);

    File messagesFile = new File(tempDir, "messages.yml");
    messages = new YamlConfiguration();
    messages.set("settings.prefix", "&8[&6RankedSMP&8]&r ");
    messages.set("test.simple", "&aHello World");
    messages.set("test.with-placeholder", "&aHello {player}");
    messages.set("test.with-prefix", "{prefix}&aWelcome");

    try {
      messages.save(messagesFile);
    } catch (Exception e) {
      fail("Failed to create test messages.yml: " + e.getMessage());
    }

    messageManager = new MessageManager(plugin, configManager);
  }

  @Test
  @DisplayName("Get message should return formatted message")
  void testGetMessage() {
    String message = messageManager.getMessage("test.simple");
    assertTrue(message.contains("Hello World"));
  }

  @Test
  @DisplayName("Get message with placeholder should replace placeholder")
  void testGetMessageWithPlaceholder() {
    Map<String, String> placeholders = new HashMap<>();
    placeholders.put("player", "TestPlayer");

    String message = messageManager.getMessage("test.with-placeholder", placeholders);
    assertTrue(message.contains("TestPlayer"));
  }

  @Test
  @DisplayName("Get message component should return Adventure component")
  void testGetMessageComponent() {
    Component component = messageManager.getMessageComponent("test.simple");
    assertNotNull(component);

    String legacy = legacySerializer.serialize(component);
    assertTrue(legacy.contains("Hello World"));
  }

  @Test
  @DisplayName("Get message for non-existent path should return error message")
  void testGetMessageNotFound() {
    String message = messageManager.getMessage("test.nonexistent");
    assertTrue(message.contains("Message not found"));
  }

  @Test
  @DisplayName("Colorize should convert & to section symbol")
  void testColorize() {
    String input = "&aGreen &bBlue &cRed";
    String output = messageManager.colorize(input);

    assertEquals("§aGreen §bBlue §cRed", output);
  }

  @Test
  @DisplayName("Legacy to component should convert color codes")
  void testLegacyToComponent() {
    Component component = messageManager.legacyToComponent("&aHello &bWorld");
    assertNotNull(component);

    String legacy = legacySerializer.serialize(component);
    assertTrue(legacy.contains("Hello"));
    assertTrue(legacy.contains("World"));
  }

  @Test
  @DisplayName("Component to legacy should convert component to string")
  void testComponentToLegacy() {
    Component component = messageManager.legacyToComponent("&aHello");
    String legacy = messageManager.componentToLegacy(component);

    assertTrue(legacy.contains("Hello"));
  }

  @Test
  @DisplayName("Small caps should convert lowercase letters")
  void testSmallCapsLowercase() {
    when(config.getBoolean("small-caps-messages", true)).thenReturn(true);
    messageManager = new MessageManager(plugin, configManager);

    String input = "hello world";
    String output = messageManager.applySmallCaps(input);

    assertNotEquals(input, output);
    assertTrue(output.contains("ʜ")); // Small caps H
    assertTrue(output.contains("ᴇ")); // Small caps E
  }

  @Test
  @DisplayName("Small caps should convert uppercase letters")
  void testSmallCapsUppercase() {
    when(config.getBoolean("small-caps-messages", true)).thenReturn(true);
    messageManager = new MessageManager(plugin, configManager);

    String input = "HELLO WORLD";
    String output = messageManager.applySmallCaps(input);

    assertNotEquals(input, output);
    assertTrue(output.contains("ʜ")); // Small caps H
    assertTrue(output.contains("ᴇ")); // Small caps E
  }

  @Test
  @DisplayName("Small caps should preserve color codes")
  void testSmallCapsPreserveColorCodes() {
    when(config.getBoolean("small-caps-messages", true)).thenReturn(true);
    messageManager = new MessageManager(plugin, configManager);

    String input = "&aHello &bWorld";
    String output = messageManager.applySmallCaps(input);

    assertTrue(output.contains("§a"));
    assertTrue(output.contains("§b"));
    assertTrue(output.contains("ʜ"));
  }

  @Test
  @DisplayName("Small caps should preserve formatting codes like bold")
  void testSmallCapsPreserveBold() {
    when(config.getBoolean("small-caps-messages", true)).thenReturn(true);
    messageManager = new MessageManager(plugin, configManager);

    String input = "&lHello";
    String output = messageManager.applySmallCaps(input);

    assertTrue(output.contains("§l"), "Output should contain bold formatting");
    assertFalse(output.isEmpty(), "Output should not be empty");
  }

  @Test
  @DisplayName("Small caps should preserve numbers and symbols")
  void testSmallCapsPreserveNumbersAndSymbols() {
    when(config.getBoolean("small-caps-messages", true)).thenReturn(true);
    messageManager = new MessageManager(plugin, configManager);

    String input = "Rank 123 - 50%";
    String output = messageManager.applySmallCaps(input);

    assertTrue(output.contains("123"));
    assertTrue(output.contains("-"));
    assertTrue(output.contains("50%"));
    assertTrue(output.contains("ʀ")); // Small caps R
  }

  @Test
  @DisplayName("Small caps disabled should not convert text")
  void testSmallCapsDisabled() {
    when(config.getBoolean("small-caps-messages", true)).thenReturn(false);
    messageManager = new MessageManager(plugin, configManager);

    String input = "Hello World";
    String output = messageManager.applySmallCaps(input);

    assertEquals("Hello World", output);
  }

  @Test
  @DisplayName("Send message should call sender.sendMessage")
  void testSendMessage() {
    messageManager.sendMessage(sender, "test.simple");

    verify(sender, times(1)).sendMessage(any(Component.class));
  }

  @Test
  @DisplayName("Send legacy message should convert and send")
  void testSendLegacyMessage() {
    messageManager.sendLegacyMessage(sender, "&aHello World");

    verify(sender, times(1)).sendMessage(any(Component.class));
  }

  @Test
  @DisplayName("Get prefix should return prefix component")
  void testGetPrefix() {
    String prefix = messageManager.getPrefix();
    assertTrue(prefix.contains("RankedSMP"));
  }

  @Test
  @DisplayName("Get prefix component should return Adventure component")
  void testGetPrefixComponent() {
    Component prefix = messageManager.getPrefixComponent();
    assertNotNull(prefix);

    String legacy = legacySerializer.serialize(prefix);
    assertTrue(legacy.contains("RankedSMP"));
  }

  @Test
  @DisplayName("Message with prefix placeholder should replace prefix")
  void testMessageWithPrefixPlaceholder() {
    String message = messageManager.getMessage("test.with-prefix");
    assertTrue(message.contains("RankedSMP"));
    assertTrue(message.contains("Welcome"));
  }

  @Test
  @DisplayName("Apply small caps component should return component")
  void testApplySmallCapsComponent() {
    when(config.getBoolean("small-caps-messages", true)).thenReturn(true);
    messageManager = new MessageManager(plugin, configManager);

    Component component = messageManager.applySmallCapsComponent("&aHello");
    assertNotNull(component);

    String legacy = legacySerializer.serialize(component);
    assertTrue(legacy.contains("ʜ")); // Small caps H
  }

  @Test
  @DisplayName("Reload should reload messages configuration")
  void testReload() {
    File messagesFile = new File(tempDir, "messages.yml");
    FileConfiguration newMessages = YamlConfiguration.loadConfiguration(messagesFile);
    newMessages.set("test.new", "&aNew Message");

    try {
      newMessages.save(messagesFile);
    } catch (Exception e) {
      fail("Failed to save modified messages.yml: " + e.getMessage());
    }

    messageManager.reload();

    String message = messageManager.getMessage("test.new");
    assertTrue(message.contains("New Message"));
  }

  @Test
  @DisplayName("Prefix is left out when message-prefix is false, and {prefix} becomes empty")
  void testPrefixDisabled() {
    when(config.getBoolean("message-prefix", false)).thenReturn(false);
    messageManager = new MessageManager(plugin, configManager);

    assertEquals("", messageManager.getPrefix());
    assertEquals(Component.empty(), messageManager.getPrefixComponent());
    String message = messageManager.getMessage("test.with-prefix");
    assertFalse(message.contains("RankedSMP"));
    assertFalse(message.contains("{prefix}"));
    assertTrue(message.contains("Welcome"));
  }
}
