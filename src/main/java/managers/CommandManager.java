package managers;

import commands.Command;
import commands.FocusCommand;
import commands.HelpCommand;
import commands.LeaveCommand;
import commands.LofiCommand;
import commands.MeCommand;
import commands.PingCommand;
import commands.StatsCommand;
import commands.VolumeCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all registered commands and provides access to them.
 */
public class CommandManager {
  private final Map<String, Command> commands = new HashMap<>();
  private final String prefix;

  public CommandManager() {
    this.prefix = "m/"; // Default prefix, will be updated if needed
    // Register all commands
    registerCommand(new PingCommand());
    registerCommand(new LeaveCommand());
    registerCommand(new LofiCommand());
    registerCommand(new StatsCommand());
    registerCommand(new MeCommand());
    registerCommand(new VolumeCommand());
    registerCommand(new FocusCommand());
    
    // Register help command and initialize it
    HelpCommand helpCommand = new HelpCommand();
    registerCommand(helpCommand);
    HelpCommand.initialize(this, prefix);
  }

  /**
   * Constructor with prefix for help command initialization.
   *
   * @param prefix The command prefix
   */
  public CommandManager(String prefix) {
    this.prefix = prefix;
    // Register all commands
    registerCommand(new PingCommand());
    registerCommand(new LeaveCommand());
    registerCommand(new LofiCommand());
    registerCommand(new StatsCommand());
    registerCommand(new MeCommand());
    registerCommand(new VolumeCommand());
    registerCommand(new FocusCommand());
    
    // Register help command and initialize it
    HelpCommand helpCommand = new HelpCommand();
    registerCommand(helpCommand);
    HelpCommand.initialize(this, prefix);
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
