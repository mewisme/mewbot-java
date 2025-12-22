package managers;

import commands.Command;
import commands.LeaveCommand;
import commands.LofiCommand;
import commands.PingCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all registered commands and provides access to them.
 */
public class CommandManager {
  private final Map<String, Command> commands = new HashMap<>();

  public CommandManager() {
    // Register all commands
    registerCommand(new PingCommand());
    registerCommand(new LeaveCommand());
    registerCommand(new LofiCommand());
  }

  /**
   * Registers a command.
   *
   * @param command The command to register
   */
  public void registerCommand(Command command) {
    commands.put(command.getName(), command);
  }

  /**
   * Gets a command by name.
   *
   * @param name The command name
   * @return The command, or null if not found
   */
  public Command getCommand(String name) {
    return commands.get(name);
  }

  /**
   * Gets all registered commands.
   *
   * @return Map of all commands
   */
  public Map<String, Command> getCommands() {
    return commands;
  }
}
