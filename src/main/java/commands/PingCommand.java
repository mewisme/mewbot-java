package commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Ping command to check bot latency.
 */
public class PingCommand extends Command {
  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("ping", "Check if the bot is responding");
    // Allow in DMs too (default behavior)
  }

  @Override
  public void execute(@NotNull SlashCommandInteractionEvent event) {
    long time = System.currentTimeMillis();
    event.replyEmbeds(EmbedUtils.createInfoEmbed("Pong! Calculating latency...", event.getUser())).queue(response -> {
      long latency = System.currentTimeMillis() - time;
      response.editOriginalEmbeds(
          EmbedUtils.createSuccessEmbed(String.format("🏓 Pong! Latency: **%d ms**", latency), event.getUser()))
          .queue();
    });
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    long time = System.currentTimeMillis();
    event.getMessage().replyEmbeds(EmbedUtils.createInfoEmbed("Pong! Calculating latency...", event.getAuthor()))
        .queue(response -> {
          long latency = System.currentTimeMillis() - time;
          response.editMessageEmbeds(
              EmbedUtils.createSuccessEmbed(String.format("🏓 Pong! Latency: **%d ms**", latency), event.getAuthor()))
              .queue();
        });
  }
}

