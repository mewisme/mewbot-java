package listeners;

import bot.BotCluster;
import bot.BotInstance;
import managers.CommandManager;
import commands.Command;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.EmbedUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.NotNull;

/**
 * Listener for message-based commands.
 * Routes commands prefixed with the configured prefix to their respective
 * command handlers.
 * In cluster mode, only the primary bot responds.
 */
public class MessageCommandListener extends ListenerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(MessageCommandListener.class);
  private final CommandManager commandManager;
  private final String prefix;
  private BotInstance botInstance;

  public MessageCommandListener(CommandManager commandManager, String prefix) {
    this.commandManager = commandManager;
    this.prefix = prefix;
  }

  public void setBotInstance(BotInstance botInstance) {
    this.botInstance = botInstance;
  }

  @Override
  public void onMessageReceived(@NotNull MessageReceivedEvent event) {
    // Ignore messages from bots
    if (event.getAuthor().isBot()) {
      return;
    }

    Message message = event.getMessage();
    String content = message.getContentRaw();

    // Check if message starts with prefix
    if (!content.startsWith(prefix)) {
      return;
    }

    // Parse command and arguments
    String[] parts = content.substring(prefix.length()).trim().split("\\s+", 2);
    String commandName = parts[0].toLowerCase();
    String args = parts.length > 1 ? parts[1] : "";

    // Get command from manager
    Command command = commandManager.getCommand(commandName);

    if (command == null) {
      // Unknown command - silently ignore
      logger.debug("Unknown command: {}", commandName);
      return;
    }

    // Check if this bot should respond (cluster mode check)
    if (botInstance != null) {
      BotCluster cluster = BotCluster.getInstance();
      if (cluster != null) {
        Long guildId = event.isFromGuild() ? event.getGuild().getIdLong() : null;
        if (!cluster.shouldRespond(botInstance, guildId)) {
          // Not the primary bot, ignore silently
          return;
        }
      }
    }

    // Execute command using the command's executeMessage method
    try {
      command.executeMessage(event, args);
      logger.info("Command {} executed via message by {} in {}",
          commandName, event.getAuthor().getAsTag(),
          event.getGuild() != null ? event.getGuild().getName() : "DM");
    } catch (Exception e) {
      logger.error("Error executing command: {}", commandName, e);
      message
          .replyEmbeds(
              EmbedUtils.createErrorEmbed("❌ An error occurred while executing this command!", event.getAuthor()))
          .queue();
    }
  }
}
