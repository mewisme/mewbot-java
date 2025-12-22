package bot;

import commands.Command;
import managers.CommandManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles registration of slash commands to Discord.
 */
public class CommandRegistrar {
  private static final Logger logger = LoggerFactory.getLogger(CommandRegistrar.class);

  /**
   * Registers slash commands globally.
   *
   * @param jda            The JDA instance
   * @param commandManager The command manager instance
   */
  public static void register(JDA jda, CommandManager commandManager) {
    List<SlashCommandData> commandDataList = buildCommandDataList(commandManager);
    logger.info("Registering {} commands...", commandDataList.size());

    // Register commands (Discord automatically replaces existing ones)
    jda.updateCommands().addCommands(commandDataList).queue(
        newCommands -> {
          logger.info("Successfully registered {} global commands!", newCommands.size());
          commandManager.getCommands().keySet().forEach(name -> logger.info("  - /{}", name));
        },
        error -> {
          logger.error("Failed to register global commands", error);
        });
  }

  /**
   * Builds the list of slash command data from command manager.
   *
   * @param commandManager The command manager instance
   * @return List of SlashCommandData
   */
  private static List<SlashCommandData> buildCommandDataList(CommandManager commandManager) {
    List<SlashCommandData> commandDataList = new ArrayList<>();
    for (Command command : commandManager.getCommands().values()) {
      commandDataList.add(command.getCommandData());
    }
    return commandDataList;
  }
}
