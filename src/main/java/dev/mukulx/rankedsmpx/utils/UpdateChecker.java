package dev.mukulx.rankedsmpx.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

public class UpdateChecker {

  private static final String MODRINTH_API = "https://api.modrinth.com/v2/project/uq7abykG/version";
  private static final String PROJECT_URL = "https://modrinth.com/plugin/rankedsmpx";

  private final RankedSMPXPlugin plugin;
  private String latestVersion = null;
  private boolean updateAvailable = false;

  public UpdateChecker(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  public void checkForUpdates() {
    SchedulerUtil.runAsync(
        plugin,
        () -> {
          try {
            URL url = java.net.URI.create(MODRINTH_API).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                "User-Agent", "RankedSMPX/" + plugin.getPluginMeta().getVersion());
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
              try (BufferedReader reader =
                  new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                  response.append(line);
                }

                JsonArray versions = JsonParser.parseString(response.toString()).getAsJsonArray();

                if (versions.size() > 0) {
                  JsonObject latestVersionObj = versions.get(0).getAsJsonObject();
                  latestVersion = latestVersionObj.get("version_number").getAsString();

                  String currentVersion = plugin.getPluginMeta().getVersion();

                  if (!currentVersion.equals(latestVersion)) {
                    updateAvailable = true;

                    SchedulerUtil.run(
                        plugin,
                        () -> {
                          plugin.getLogger().warning("═══════════════════════════════════════════");
                          plugin.getLogger().warning("  A new version of RankedSMPX is available!");
                          plugin
                              .getLogger()
                              .warning(
                                  "  Current: " + currentVersion + " | Latest: " + latestVersion);
                          plugin.getLogger().warning("  Download: " + PROJECT_URL);
                          plugin.getLogger().warning("═══════════════════════════════════════════");
                        });
                  } else {
                    plugin.getLogger().info("You are running the latest version!");
                  }
                }
              }
            } else {
              plugin
                  .getLogger()
                  .warning("Failed to check for updates. Response code: " + responseCode);
            }

            connection.disconnect();

          } catch (Exception e) {
            plugin
                .getLogger()
                .log(Level.WARNING, "Could not check for updates. This is not critical.", e);
          }
        });
  }

  public boolean isUpdateAvailable() {
    return updateAvailable;
  }

  public String getLatestVersion() {
    return latestVersion;
  }

  public String getProjectUrl() {
    return PROJECT_URL;
  }
}
