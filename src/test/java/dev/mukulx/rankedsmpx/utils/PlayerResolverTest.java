package dev.mukulx.rankedsmpx.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.BedrockSupport;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerResolverTest {

  private static final UUID BEDROCK = UUID.fromString("00000000-0000-0000-0009-01fd79799f17");
  private static final UUID JAVA = new UUID(5, 5);

  private DatabaseManager database;
  private MessageManager messages;
  private BedrockSupport bedrock;
  private FakeServer server;
  private PlayerResolver resolver;

  @BeforeEach
  void setUp() {
    database = mock(DatabaseManager.class);
    when(database.findPlayersByName(anyString())).thenReturn(Map.of());
    when(database.findPlayersByNameSuffix(anyString())).thenReturn(Map.of());
    messages = mock(MessageManager.class);
    bedrock = mock(BedrockSupport.class);
    when(bedrock.isEnabled()).thenReturn(true);
    when(bedrock.getPrefix()).thenReturn(".");
    server = new FakeServer();
    resolver = new PlayerResolver(database, messages, bedrock, server);
  }

  @Test
  @DisplayName("An offline Floodgate player missing from the server cache is found by stored name")
  void offlineBedrockPlayer() {
    when(database.findPlayersByName(".SubPepper")).thenReturn(Map.of(BEDROCK, ".SubPepper"));
    server.names.put(BEDROCK, ".SubPepper");

    PlayerResolver.Result result = resolver.resolve(".SubPepper");

    assertTrue(result.found());
    assertEquals(BEDROCK, result.target().uuid());
    assertEquals(".SubPepper", result.target().name());
    assertNull(result.target().online());
    assertEquals(0, server.mojangLookups, "a Bedrock name must never go to Mojang");
  }

  @Test
  @DisplayName("Any Floodgate prefix works because the full stored name is matched")
  void customPrefix() {
    when(database.findPlayersByName("*subpepper")).thenReturn(Map.of(BEDROCK, "*SubPepper"));

    PlayerResolver.Result result = resolver.resolve("*subpepper");

    assertTrue(result.found());
    assertEquals("*SubPepper", result.target().name());
  }

  @Test
  @DisplayName("A UUID is accepted directly")
  void byUuid() {
    when(database.findPlayerName(BEDROCK)).thenReturn(".SubPepper");

    PlayerResolver.Result result = resolver.resolve(BEDROCK.toString());

    assertTrue(result.found());
    assertEquals(".SubPepper", result.target().name());
  }

  @Test
  @DisplayName("A UUID nobody has used is not found")
  void unknownUuid() {
    assertFalse(resolver.resolve(new UUID(7, 7).toString()).found());
  }

  @Test
  @DisplayName("Online players win before any stored name is checked")
  void onlineFirst() {
    Player player = player(JAVA, "Steve");
    server.online.put("Steve", player);

    PlayerResolver.Result result = resolver.resolve("Steve");

    assertSame(player, result.target().online());
    verify(database, never()).findPlayersByName(anyString());
  }

  @Test
  @DisplayName("The server cache is trusted before stored names, as before")
  void serverCacheFirst() {
    OfflinePlayer cached = offline(JAVA, "Steve", true);
    server.cached.put("Steve", cached);

    assertEquals(JAVA, resolver.resolve("Steve").target().uuid());
    verify(database, never()).findPlayersByName(anyString());
  }

  @Test
  @DisplayName("A stored name the player has since changed is ignored")
  void staleStoredName() {
    when(database.findPlayersByName("OldName")).thenReturn(Map.of(JAVA, "OldName"));
    server.names.put(JAVA, "NewName");

    assertFalse(resolver.resolve("OldName").found());
  }

  @Test
  @DisplayName("Java names unknown to the plugin still fall back to the server lookup")
  void javaFallback() {
    server.lookups.put("Notch", offline(JAVA, "Notch", true));

    assertEquals(JAVA, resolver.resolve("Notch").target().uuid());
    assertEquals(1, server.mojangLookups);
  }

  @Test
  @DisplayName("The made-up offline UUID for a never-seen name is rejected")
  void neverPlayed() {
    server.lookups.put("Nobody", offline(new UUID(8, 8), "Nobody", false));

    assertFalse(resolver.resolve("Nobody").found());
  }

  @Test
  @DisplayName("Two players stored under one name are reported instead of guessing")
  void ambiguous() {
    Map<UUID, String> both = new LinkedHashMap<>();
    both.put(BEDROCK, "Steve");
    both.put(JAVA, "Steve");
    when(database.findPlayersByName("Steve")).thenReturn(both);

    PlayerResolver.Result result = resolver.resolve("Steve");

    assertTrue(result.ambiguous());
    assertEquals(List.of(BEDROCK, JAVA), result.matches().stream().map(PlayerResolver.Target::uuid).toList());

    CommandSender sender = mock(CommandSender.class);
    AtomicBoolean notFound = new AtomicBoolean();
    assertNull(resolver.resolve(sender, "Steve", () -> notFound.set(true)));
    assertFalse(notFound.get());
    verify(messages).sendMessage(eq(sender), eq("general.player-ambiguous"), anyMap());
  }

  @Test
  @DisplayName("The command's own message is used when nothing matches")
  void notFoundCallback() {
    AtomicBoolean notFound = new AtomicBoolean();
    assertNull(resolver.resolve(mock(CommandSender.class), ".Ghost", () -> notFound.set(true)));
    assertTrue(notFound.get());
  }

  @Test
  @DisplayName("Leaving out the Floodgate prefix gives a hint, never a match")
  void suggestsPrefixedName() {
    when(database.findPlayersByNameSuffix("SubPepper")).thenReturn(Map.of(BEDROCK, ".SubPepper"));

    PlayerResolver.Result result = resolver.resolve("SubPepper");

    assertFalse(result.found());
    assertEquals(".SubPepper", result.suggestion());

    CommandSender sender = mock(CommandSender.class);
    AtomicBoolean notFound = new AtomicBoolean();
    assertNull(resolver.resolve(sender, "SubPepper", () -> notFound.set(true)));
    assertFalse(notFound.get());
    verify(messages)
        .sendMessage(
            sender,
            "general.player-not-found-suggest",
            Map.of("player", "SubPepper", "suggestion", ".SubPepper"));
  }

  @Test
  @DisplayName("Custom prefixes are suggested too, and online Bedrock players count")
  void suggestsCustomPrefixOnline() {
    when(bedrock.getPrefix()).thenReturn("*");
    server.online.put("*SubPepper", player(BEDROCK, "*SubPepper"));

    assertEquals("*SubPepper", resolver.resolve("subpepper").suggestion());
  }

  @Test
  @DisplayName("Only the configured Floodgate prefix is suggested when it is known")
  void ignoresOtherLeads() {
    when(database.findPlayersByNameSuffix("Pepper"))
        .thenReturn(Map.of(JAVA, "SubPepper", BEDROCK, "*Pepper"));

    assertNull(resolver.resolve("Pepper").suggestion());
  }

  @Test
  @DisplayName("Without Floodgate here, any lead a Java name cannot have is suggested")
  void suggestsWithoutFloodgate() {
    when(bedrock.getPrefix()).thenReturn(null);
    when(database.findPlayersByNameSuffix("Pepper"))
        .thenReturn(Map.of(JAVA, "SubPepper", BEDROCK, "#Pepper"));

    assertEquals("#Pepper", resolver.resolve("Pepper").suggestion());
  }

  @Test
  @DisplayName("No hint when bedrock.enabled is false")
  void noHintWhenDisabled() {
    when(bedrock.isEnabled()).thenReturn(false);
    when(database.findPlayersByNameSuffix("SubPepper")).thenReturn(Map.of(BEDROCK, ".SubPepper"));

    assertNull(resolver.resolve("SubPepper").suggestion());
  }

  private static Player player(UUID uuid, String name) {
    Player player = mock(Player.class);
    when(player.getUniqueId()).thenReturn(uuid);
    when(player.getName()).thenReturn(name);
    return player;
  }

  private static OfflinePlayer offline(UUID uuid, String name, boolean played) {
    OfflinePlayer player = mock(OfflinePlayer.class);
    when(player.getUniqueId()).thenReturn(uuid);
    when(player.getName()).thenReturn(name);
    when(player.hasPlayedBefore()).thenReturn(played);
    return player;
  }

  private static final class FakeServer implements PlayerResolver.Server {
    final Map<String, Player> online = new HashMap<>();
    final Map<String, OfflinePlayer> cached = new HashMap<>();
    final Map<String, OfflinePlayer> lookups = new HashMap<>();
    final Map<UUID, String> names = new HashMap<>();
    int mojangLookups;

    @Override
    public Player online(String name) {
      return online.get(name);
    }

    @Override
    public Player online(UUID uuid) {
      return online.values().stream().filter(p -> p.getUniqueId().equals(uuid)).findFirst().orElse(null);
    }

    @Override
    public Collection<String> onlineNames() {
      return online.keySet();
    }

    @Override
    public OfflinePlayer cached(String name) {
      return cached.get(name);
    }

    @Override
    public String lastKnownName(UUID uuid) {
      return names.get(uuid);
    }

    @Override
    public OfflinePlayer lookup(String name) {
      mojangLookups++;
      return lookups.get(name);
    }
  }
}
