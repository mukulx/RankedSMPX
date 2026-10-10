package dev.mukulx.rankedsmpx.managers;

import dev.mukulx.rankedsmpx.RankedSMPXPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Adds messages that are new in this version to the server's messages.yml. There is no version
 * number: the bundled file is compared key by key, so existing text and comments are never touched.
 */
public class MessagesUpdater {

  private static final String FILE = "messages.yml";

  private final RankedSMPXPlugin plugin;

  public MessagesUpdater(@NotNull RankedSMPXPlugin plugin) {
    this.plugin = plugin;
  }

  /** @return how many messages were added */
  public int update(@NotNull File messagesFile) {
    try (InputStream in = plugin.getResource(FILE)) {
      if (in == null) {
        return 0;
      }
      List<String> defaults = lines(new String(in.readAllBytes(), StandardCharsets.UTF_8));
      List<String> current = Files.readAllLines(messagesFile.toPath(), StandardCharsets.UTF_8);

      ConfigUpdater.SegmentTree tree = ConfigUpdater.SegmentTree.parse(current);
      int added = tree.mergeFrom(ConfigUpdater.SegmentTree.parse(defaults));
      if (added == 0) {
        return 0;
      }

      backup(messagesFile);
      Files.write(messagesFile.toPath(), tree.toLines(), StandardCharsets.UTF_8);
      plugin.getLogger().info("Added " + added + " new message(s) to messages.yml");
      return added;
    } catch (Exception e) {
      plugin.getLogger().warning("Could not update messages.yml, using built-in text for new messages: " + e.getMessage());
      return 0;
    }
  }

  private static List<String> lines(String text) {
    return new ArrayList<>(List.of(text.split("\r?\n", -1)));
  }

  private void backup(File file) throws IOException {
    File dir = new File(plugin.getDataFolder(), "backups");
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("could not create backups folder");
    }
    Files.copy(file.toPath(), new File(dir, "messages-" + System.currentTimeMillis() + ".yml").toPath());
  }
}
