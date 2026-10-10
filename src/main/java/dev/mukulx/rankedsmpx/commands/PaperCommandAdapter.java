package dev.mukulx.rankedsmpx.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public final class PaperCommandAdapter implements BasicCommand {

  private final String label;
  private final CommandExecutor executor;

  public PaperCommandAdapter(String label, CommandExecutor executor) {
    this.label = label;
    this.executor = executor;
  }

  @Override
  public void execute(CommandSourceStack source, String[] arguments) {
    try {
      executor.onCommand(source.getSender(), null, label, arguments);
    } catch (Throwable t) {
      org.bukkit.Bukkit.getLogger().log(java.util.logging.Level.SEVERE, "Unexpected error executing /" + label + " command: " + t.getMessage(), t);
      dev.mukulx.rankedsmpx.RankedSMPXPlugin.sendErrorMessage(source.getSender());
    }
  }

  @Override
  public Collection<String> suggest(CommandSourceStack source, String[] arguments) {
    if (arguments.length == 0) {
      arguments = new String[]{""};
    }
    
    if (executor instanceof TabCompleter completer) {
      List<String> suggestions = completer.onTabComplete(source.getSender(), null, label, arguments);
      return suggestions == null ? List.of() : suggestions;
    }
    return List.of();
  }
}
