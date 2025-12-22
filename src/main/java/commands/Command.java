package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for all commands.
 * Each command must implement the execute method for slash commands,
 * executeMessage method for message commands, and provide command data.
 */
public abstract class Command {
  /**
   * Gets the slash command data for registration.
   * This defines the command name, description, options, etc.
   *
   * @return SlashCommandData for this command
   */
  @NotNull
  public abstract SlashCommandData getCommandData();

  /**
   * Executes the command when invoked via slash command.
   *
   * @param event The slash command interaction event
   */
  public abstract void execute(@NotNull SlashCommandInteractionEvent event);

  /**
   * Executes the command when invoked via message command.
   *
   * @param event The message received event
   * @param args  The command arguments (everything after the command name)
   */
  public abstract void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args);

  /**
   * Gets the name of the command.
   * By default, extracts it from the command data.
   *
   * @return The command name
   */
  @NotNull
  public String getName() {
    return getCommandData().getName();
  }
}

