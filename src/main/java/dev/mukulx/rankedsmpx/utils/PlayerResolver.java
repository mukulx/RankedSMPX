package dev.mukulx.rankedsmpx.utils;

import dev.mukulx.rankedsmpx.database.DatabaseManager;
import dev.mukulx.rankedsmpx.managers.BedrockSupport;
import dev.mukulx.rankedsmpx.managers.MessageManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds the player an admin means from a name or UUID. Names the server cache has forgotten are
 * looked up in the plugin's own tables, which also covers Floodgate players Mojang never heard of.
 */
public final class PlayerResolver {

  private static final Pattern JAVA_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
  private static final Pattern NOT_NAME_CHARS = Pattern.compile("[^A-Za-z0-9_]+");

  public record Target(@NotNull UUID uuid, @NotNull String name, @Nullable Player online) {}

  public record Result(
      @Nullable Target target, @NotNull List<Target> matches, @Nullable String suggestion) {

    public boolean found() {
      return target != null;
    }

    public boolean ambiguous() {
      return target == null && matches.size() > 1;
    }
  }

  interface Server {
    @Nullable
    Player online(@NotNull String name);

    @Nullable
    Player online(@NotNull UUID uuid);

    @NotNull
    Collection<String> onlineNames();

    @Nullable
    OfflinePlayer cached(@NotNull String name);

    @Nullable
    String lastKnownName(@NotNull UUID uuid);

    /** May ask Mojang, so only called for names a Java account could have. */
    @Nullable
    OfflinePlayer lookup(@NotNull String name);
  }

  private final DatabaseManager database;
  private final MessageManager messages;
  private final BedrockSupport bedrock;
  private final Server server;

  public PlayerResolver(
      @NotNull DatabaseManager database,
      @NotNull MessageManager messages,
      @NotNull BedrockSupport bedrock) {
    this(database, messages, bedrock, new BukkitServer());
  }

  PlayerResolver(
      @NotNull DatabaseManager database,
      @NotNull MessageManager messages,
      @NotNull BedrockSupport bedrock,
      @NotNull Server server) {
    this.database = database;
    this.messages = messages;
    this.bedrock = bedrock;
    this.server = server;
  }

  @NotNull
  public Result resolve(@NotNull String input) {
    String query = input.trim();
    if (query.isEmpty()) {
      return notFound(null);
    }

    UUID uuid = parseUuid(query);
    if (uuid != null) {
      Target target = byUuid(uuid);
      return target != null ? found(target) : notFound(null);
    }

    Player online = server.online(query);
    if (online != null) {
      return found(new Target(online.getUniqueId(), online.getName(), online));
    }

    OfflinePlayer cached = server.cached(query);
    if (cached != null && cached.hasPlayedBefore()) {
      return found(offline(cached, query));
    }

    List<Target> stored = new ArrayList<>();
    for (Map.Entry<UUID, String> entry : database.findPlayersByName(query).entrySet()) {
      // A stored name can be stale after a rename; skip it if the server knows them by another name.
      String current = server.lastKnownName(entry.getKey());
      if (current == null || current.equalsIgnoreCase(query)) {
        String name = current != null ? current : entry.getValue();
        stored.add(new Target(entry.getKey(), name, server.online(entry.getKey())));
      }
    }
    if (stored.size() == 1) {
      return found(stored.get(0));
    }
    if (stored.size() > 1) {
      return new Result(null, List.copyOf(stored), null);
    }

    if (JAVA_NAME.matcher(query).matches()) {
      OfflinePlayer looked = server.lookup(query);
      if (looked != null && looked.hasPlayedBefore()) {
        return found(offline(looked, query));
      }
    }
    return notFound(suggest(query));
  }

  /**
   * Resolves a command argument. If nothing matches, {@code notFound} sends the command's usual
   * message; a name shared by several players lists their UUIDs instead.
   */
  @Nullable
  public Target resolve(
      @NotNull CommandSender sender, @NotNull String input, @NotNull Runnable notFound) {
    Result result = resolve(input);
    if (result.found()) {
      return result.target();
    }
    if (result.ambiguous()) {
      messages.sendMessage(sender, "general.player-ambiguous", Map.of("player", input.trim()));
      for (Target match : result.matches()) {
        String id = match.uuid().toString();
        sender.sendMessage(
            Component.text(" - " + id, NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.copyToClipboard(id)));
      }
      return null;
    }
    if (result.suggestion() != null) {
      messages.sendMessage(
          sender,
          "general.player-not-found-suggest",
          Map.of("player", input.trim(), "suggestion", result.suggestion()));
      return null;
    }
    notFound.run();
    return null;
  }

  /** Why {@code input} did not resolve, for replies that do not go through messages.yml. */
  @NotNull
  public static String describeMissing(@NotNull String input, @NotNull Result result) {
    if (result.ambiguous()) {
      StringBuilder ids = new StringBuilder();
      for (Target match : result.matches()) {
        ids.append("\n`").append(match.uuid()).append('`');
      }
      return "**" + input + "** matches more than one player. Use one of these UUIDs:" + ids;
    }
    String missing = "Minecraft player **" + input + "** has never joined the server.";
    return result.suggestion() != null
        ? missing + " Did you mean `" + result.suggestion() + "`?"
        : missing;
  }

  /**
   * A Bedrock name for someone who typed it without the Floodgate prefix. Only a hint: the admin
   * still has to type the full name, so a Java player is never picked by mistake.
   */
  @Nullable
  private String suggest(@NotNull String query) {
    if (!bedrock.isEnabled()) {
      return null;
    }
    // Without Floodgate on this server (e.g. it only runs on the proxy) the prefix is unknown, so
    // accept any lead made of characters a Java name cannot contain.
    String prefix = bedrock.getPrefix();
    List<String> names = new ArrayList<>(server.onlineNames());
    names.addAll(database.findPlayersByNameSuffix(query).values());
    for (String name : names) {
      int lead = name.length() - query.length();
      if (lead <= 0 || !name.regionMatches(true, lead, query, 0, query.length())) {
        continue;
      }
      String start = name.substring(0, lead);
      boolean prefixed =
          prefix != null ? start.equals(prefix) : NOT_NAME_CHARS.matcher(start).matches();
      if (prefixed) {
        return name;
      }
    }
    return null;
  }

  @Nullable
  private Target byUuid(@NotNull UUID uuid) {
    Player online = server.online(uuid);
    if (online != null) {
      return new Target(uuid, online.getName(), online);
    }
    String name = server.lastKnownName(uuid);
    if (name == null) {
      name = database.findPlayerName(uuid);
    }
    return name != null ? new Target(uuid, name, null) : null;
  }

  private Target offline(@NotNull OfflinePlayer player, @NotNull String fallbackName) {
    String name = player.getName() != null ? player.getName() : fallbackName;
    return new Target(player.getUniqueId(), name, server.online(player.getUniqueId()));
  }

  @Nullable
  private static UUID parseUuid(@NotNull String input) {
    if (input.length() != 36 || input.charAt(8) != '-') {
      return null;
    }
    try {
      return UUID.fromString(input);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static Result found(@NotNull Target target) {
    return new Result(target, List.of(target), null);
  }

  private static Result notFound(@Nullable String suggestion) {
    return new Result(null, List.of(), suggestion);
  }

  private static final class BukkitServer implements Server {

    @Override
    public Player online(@NotNull String name) {
      return Bukkit.getPlayer(name);
    }

    @Override
    public Player online(@NotNull UUID uuid) {
      return Bukkit.getPlayer(uuid);
    }

    @Override
    public Collection<String> onlineNames() {
      return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    @Override
    public OfflinePlayer cached(@NotNull String name) {
      return Bukkit.getOfflinePlayerIfCached(name);
    }

    @Override
    public String lastKnownName(@NotNull UUID uuid) {
      return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    public OfflinePlayer lookup(@NotNull String name) {
      return Bukkit.getOfflinePlayer(name);
    }
  }
}
