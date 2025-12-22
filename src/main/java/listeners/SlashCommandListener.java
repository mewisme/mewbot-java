package listeners;

import bot.BotCluster;
import bot.BotInstance;
import managers.CommandManager;
import commands.Command;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jetbrains.annotations.NotNull;

/**
 * Listener for slash command interactions.
 * Routes commands to their respective command handlers.
 * In cluster mode, only the primary bot responds.
 */
public class SlashCommandListener extends ListenerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(SlashCommandListener.class);
  private final CommandManager commandManager;
  private BotInstance botInstance;

  public SlashCommandListener(CommandManager commandManager) {
    this.commandManager = commandManager;
  }

  public void setBotInstance(BotInstance botInstance) {
    this.botInstance = botInstance;
  }

  @Override
  public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
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

    Command command = commandManager.getCommand(event.getName());

    if (command == null) {
      event.reply("Unknown command: " + event.getName())
          .setEphemeral(true)
          .queue();
      return;
    }

    try {
      command.execute(event);
    } catch (Exception e) {
      logger.error("Error executing command: {}", event.getName(), e);
      event.reply("An error occurred while executing this command!")
          .setEphemeral(true)
          .queue();
    }
  }
}
