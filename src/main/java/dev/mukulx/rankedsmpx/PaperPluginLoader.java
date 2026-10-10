package dev.mukulx.rankedsmpx;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public final class PaperPluginLoader implements PluginLoader {

  @Override
  public void classloader(PluginClasspathBuilder classpathBuilder) {
    MavenLibraryResolver resolver = new MavenLibraryResolver();
    resolver.addRepository(
        new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR)
            .build());

    if (isDiscordEnabled(classpathBuilder.getContext().getDataDirectory())) {
      addDependency(resolver, "net.dv8tion:JDA:6.4.1");
    }
    addDependency(resolver, "com.google.code.gson:gson:2.11.0");
    if (isMySqlEnabled(classpathBuilder.getContext().getDataDirectory())) {
      addDependency(resolver, "com.mysql:mysql-connector-j:9.7.0");
    }
    addDependency(resolver, "org.xerial:sqlite-jdbc:3.53.0.0");
    addDependency(resolver, "com.zaxxer:HikariCP:7.0.2");
    classpathBuilder.addLibrary(resolver);
  }

  private static void addDependency(MavenLibraryResolver resolver, String coordinates) {
    resolver.addDependency(new Dependency(new DefaultArtifact(coordinates), null));
  }

  /**
   * Reads the top-level Discord toggle without loading Bukkit's YAML API during Paper bootstrap.
   * JDA is added before the plugin classloader is created, so enabling Discord requires a restart.
   */
  private static boolean isDiscordEnabled(Path dataDirectory) {
    Path discordConfig = dataDirectory.resolve("discord.yml");
    if (!Files.isRegularFile(discordConfig)) {
      return false;
    }

    try (var lines = Files.lines(discordConfig)) {
      return lines
          .filter(line -> !line.strip().isEmpty() && !line.strip().startsWith("#"))
          .filter(line -> !Character.isWhitespace(line.charAt(0)))
          .map(String::strip)
          .filter(line -> line.startsWith("enabled:"))
          .findFirst()
          .map(line -> line.substring("enabled:".length()).strip())
          .map(value -> value.equalsIgnoreCase("true"))
          .orElse(false);
    } catch (IOException exception) {
      return false;
    }
  }

  /**
   * Reads {@code database.type} without loading Bukkit's YAML API during Paper bootstrap.
   * MySQL Connector/J must be on the classpath before the plugin starts, so changing this setting
   * requires a server restart.
   */
  private static boolean isMySqlEnabled(Path dataDirectory) {
    Boolean configured = readDatabaseType(dataDirectory.resolve("config.yml"));
    return configured != null
        ? configured
        : Boolean.TRUE.equals(readDatabaseType(dataDirectory.resolve("advanced.yml")));
  }

  private static Boolean readDatabaseType(Path configFile) {
    if (!Files.isRegularFile(configFile)) {
      return null;
    }

    try (var lines = Files.lines(configFile)) {
      boolean inDatabaseSection = false;
      for (String rawLine : (Iterable<String>) lines::iterator) {
        String stripped = rawLine.strip();
        if (stripped.isEmpty() || stripped.startsWith("#")) {
          continue;
        }

        boolean topLevel = !Character.isWhitespace(rawLine.charAt(0));
        if (topLevel) {
          inDatabaseSection = stripped.equals("database:");
          continue;
        }

        if (inDatabaseSection && stripped.startsWith("type:")) {
          String type = stripped.substring("type:".length()).strip();
          return type.equalsIgnoreCase("mysql");
        }
      }
    } catch (IOException exception) {
      return null;
    }

    return null;
  }
}
