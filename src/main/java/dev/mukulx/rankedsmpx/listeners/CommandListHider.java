package dev.mukulx.rankedsmpx.listeners;

import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;

/**
 * Keeps the tab-complete list short: removes the "rankedsmpx:name" copies and the rarely used
 * aliases. The commands still work when typed.
 */
public class CommandListHider implements Listener {

  private static final String NAMESPACE = "rankedsmpx:";

  private static final Set<String> HIDDEN_ALIASES =
      Set.of(
          "ri", "myrank", "rt", "topranks", "extrainv", "ei", "einv", "extrainvsee", "transfer",
          "giverank", "rrandom", "randomrank");

  @EventHandler
  public void onCommandSend(PlayerCommandSendEvent event) {
    event.getCommands().removeIf(name -> name.startsWith(NAMESPACE) || HIDDEN_ALIASES.contains(name));
  }
}
