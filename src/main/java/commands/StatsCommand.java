package commands;

import managers.DatabaseManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Command to display server statistics.
 */
public class StatsCommand extends Command {
  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("stats", "Show server statistics (playback time and command usage)");
  }

  @Override
  public void execute(@NotNull SlashCommandInteractionEvent event) {
    if (!event.isFromGuild()) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    DatabaseManager db = DatabaseManager.getInstance();
    String guildId = event.getGuild().getId();

    // Get statistics
    long playbackSeconds = db.getGuildPlaybackTime(guildId);
    long commandUsage = db.getTotalCommandUsage(guildId);

    // Format playback time
    long hours = playbackSeconds / 3600;
    long minutes = (playbackSeconds % 3600) / 60;
    long seconds = playbackSeconds % 60;
    String playbackTime = String.format("%d giờ %d phút %d giây", hours, minutes, seconds);
    if (hours == 0 && minutes == 0) {
      playbackTime = String.format("%d giây", seconds);
    } else if (hours == 0) {
      playbackTime = String.format("%d phút %d giây", minutes, seconds);
    }

    // Build response
    StringBuilder sb = new StringBuilder();
    sb.append("📊 **Thống kê Server**\n\n");
    sb.append("🎵 **Tổng thời gian phát nhạc:**\n");
    sb.append("└ ").append(playbackTime).append("\n\n");
    sb.append("⚡ **Tổng số lần dùng lệnh:**\n");
    sb.append("└ ").append(commandUsage).append(" lần\n");

    event
        .replyEmbeds(
            EmbedUtils.createEmbed("📊 **Server Statistics**", sb.toString(), EmbedUtils.COLOR_INFO, event.getUser()))
        .setEphemeral(false)
        .queue();
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    if (!event.isFromGuild()) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getAuthor()))
          .queue();
      return;
    }

    DatabaseManager db = DatabaseManager.getInstance();
    String guildId = event.getGuild().getId();

    // Get statistics
    long playbackSeconds = db.getGuildPlaybackTime(guildId);
    long commandUsage = db.getTotalCommandUsage(guildId);

    // Format playback time
    long hours = playbackSeconds / 3600;
    long minutes = (playbackSeconds % 3600) / 60;
    long seconds = playbackSeconds % 60;
    String playbackTime = String.format("%d giờ %d phút %d giây", hours, minutes, seconds);
    if (hours == 0 && minutes == 0) {
      playbackTime = String.format("%d giây", seconds);
    } else if (hours == 0) {
      playbackTime = String.format("%d phút %d giây", minutes, seconds);
    }

    // Build response
    StringBuilder sb = new StringBuilder();
    sb.append("📊 **Thống kê Server**\n\n");
    sb.append("🎵 **Tổng thời gian phát nhạc:**\n");
    sb.append("└ ").append(playbackTime).append("\n\n");
    sb.append("⚡ **Tổng số lần dùng lệnh:**\n");
    sb.append("└ ").append(commandUsage).append(" lần\n");

    event.getMessage()
        .replyEmbeds(
            EmbedUtils.createEmbed("📊 **Server Statistics**", sb.toString(), EmbedUtils.COLOR_INFO, event.getAuthor()))
        .queue();
  }
}
