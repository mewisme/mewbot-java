package commands;

import managers.CommandManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Help command to display all available commands.
 */
public class HelpCommand extends Command {
  private static CommandManager commandManager;
  private static String prefix = "m/"; // Default prefix

  /**
   * Sets the command manager and prefix for the help command.
   * This should be called during initialization.
   *
   * @param manager The command manager instance
   * @param cmdPrefix The command prefix
   */
  public static void initialize(CommandManager manager, String cmdPrefix) {
    commandManager = manager;
    prefix = cmdPrefix;
  }

  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("help", "Show all available commands");
  }

  @Override
  public void execute(@NotNull SlashCommandInteractionEvent event) {
    String helpMessage = buildHelpMessage(event.getGuild() != null);
    event.replyEmbeds(EmbedUtils.createEmbed("📚 **Command Help**", helpMessage, EmbedUtils.COLOR_INFO, event.getUser()))
        .setEphemeral(true)
        .queue();
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    String helpMessage = buildHelpMessage(event.isFromGuild());
    event.getMessage().replyEmbeds(
        EmbedUtils.createEmbed("📚 **Command Help**", helpMessage, EmbedUtils.COLOR_INFO, event.getAuthor()))
        .queue();
  }

  /**
   * Builds the help message with all available commands.
   *
   * @param isGuild Whether the command was executed in a guild
   * @return Formatted help message
   */
  private String buildHelpMessage(boolean isGuild) {
    if (commandManager == null || prefix == null) {
      return "Help command is not properly initialized.";
    }

    StringBuilder sb = new StringBuilder();
    
    sb.append("Here are all available commands:\n\n");

    Map<String, Command> commands = commandManager.getCommands();
    
    // Command descriptions map
    Map<String, String> descriptions = Map.of(
        "ping", "Check bot response time and latency",
        "lofi", "Play lofi music 24/7 in your voice channel",
        "leave", "Make the bot leave the voice channel",
        "volume", "Set or check the audio volume (0-100)",
        "focus", "Mute all users in voice channel (focus mode - only bot plays)",
        "stats", "Show server statistics (playback time and command usage)",
        "me", "Show your listening statistics and top listeners",
        "help", "Show this help message"
    );

    // Build command list (exclude help command to avoid duplication)
    for (Map.Entry<String, Command> entry : commands.entrySet()) {
      String commandName = entry.getKey();
      
      // Skip help command, we'll add it separately
      if ("help".equals(commandName)) {
        continue;
      }
      
      String description = descriptions.getOrDefault(commandName, "No description available");
      
      sb.append("**/").append(commandName).append("**");
      if (isGuild) {
        sb.append(" or `").append(prefix).append(commandName).append("`");
      }
      sb.append("\n");
      sb.append("└ ").append(description).append("\n\n");
    }

    // Add help command at the end
    sb.append("**/help**");
    if (isGuild) {
      sb.append(" or `").append(prefix).append("help`");
    }
    sb.append("\n");
    sb.append("└ Show this help message\n\n");

    // Add usage note
    if (isGuild) {
      sb.append("💡 **Tip:** You can use either slash commands (`/command`) or message commands (`")
          .append(prefix).append("command`)");
    } else {
      sb.append("💡 **Tip:** Use slash commands (`/command`) in DMs");
    }

    return sb.toString();
  }
}

