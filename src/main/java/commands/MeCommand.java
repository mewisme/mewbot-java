package commands;

import managers.DatabaseManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Command to display personal statistics and top listeners.
 */
public class MeCommand extends Command {
  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("me", "Show your listening statistics and top listeners");
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
    String userId = event.getUser().getId();

    // Get user's listening time
    long listeningSeconds = db.getUserListeningTime(guildId, userId);

    // Get top listeners
    List<Map<String, Object>> topListeners = db.getTopListeners(guildId, 10);

    // Format user's listening time
    long hours = listeningSeconds / 3600;
    long minutes = (listeningSeconds % 3600) / 60;
    long seconds = listeningSeconds % 60;
    String listeningTime = String.format("%d giờ %d phút %d giây", hours, minutes, seconds);
    if (hours == 0 && minutes == 0) {
      listeningTime = String.format("%d giây", seconds);
    } else if (hours == 0) {
      listeningTime = String.format("%d phút %d giây", minutes, seconds);
    }

    // Build response
    StringBuilder sb = new StringBuilder();
    sb.append("👤 **Thống kê cá nhân**\n\n");
    sb.append("🎧 **Thời gian nghe nhạc của bạn:**\n");
    sb.append("└ ").append(listeningTime).append("\n\n");

    if (!topListeners.isEmpty()) {
      sb.append("🏆 **Top người nghe nhạc nhiều nhất:**\n");
      int rank = 1;
      for (Map<String, Object> entry : topListeners) {
        String topUserId = (String) entry.get("user_id");
        long topSeconds = (Long) entry.get("total_listening_seconds");
        
        // Format time
        long topHours = topSeconds / 3600;
        long topMinutes = (topSeconds % 3600) / 60;
        String topTime = String.format("%d giờ %d phút", topHours, topMinutes);
        if (topHours == 0) {
          topTime = String.format("%d phút", topMinutes);
        }

        // Get user mention
        User topUser = event.getJDA().getUserById(topUserId);
        String userMention = topUser != null ? topUser.getAsMention() : "<@" + topUserId + ">";

        String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "•";
        sb.append(medal).append(" ").append(rank).append(". ").append(userMention)
            .append(" - ").append(topTime).append("\n");
        
        rank++;
        if (rank > 10) break;
      }
    }

    event.replyEmbeds(EmbedUtils.createEmbed("👤 **Personal Statistics**", sb.toString(), EmbedUtils.COLOR_INFO, event.getUser()))
        .setEphemeral(false)
        .queue();
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    if (!event.isFromGuild()) {
      event.getMessage().replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getAuthor()))
          .queue();
      return;
    }

    DatabaseManager db = DatabaseManager.getInstance();
    String guildId = event.getGuild().getId();
    String userId = event.getAuthor().getId();

    // Get user's listening time
    long listeningSeconds = db.getUserListeningTime(guildId, userId);

    // Get top listeners
    List<Map<String, Object>> topListeners = db.getTopListeners(guildId, 10);

    // Format user's listening time
    long hours = listeningSeconds / 3600;
    long minutes = (listeningSeconds % 3600) / 60;
    long seconds = listeningSeconds % 60;
    String listeningTime = String.format("%d giờ %d phút %d giây", hours, minutes, seconds);
    if (hours == 0 && minutes == 0) {
      listeningTime = String.format("%d giây", seconds);
    } else if (hours == 0) {
      listeningTime = String.format("%d phút %d giây", minutes, seconds);
    }

    // Build response
    StringBuilder sb = new StringBuilder();
    sb.append("👤 **Thống kê cá nhân**\n\n");
    sb.append("🎧 **Thời gian nghe nhạc của bạn:**\n");
    sb.append("└ ").append(listeningTime).append("\n\n");

    if (!topListeners.isEmpty()) {
      sb.append("🏆 **Top người nghe nhạc nhiều nhất:**\n");
      int rank = 1;
      for (Map<String, Object> entry : topListeners) {
        String topUserId = (String) entry.get("user_id");
        long topSeconds = (Long) entry.get("total_listening_seconds");
        
        // Format time
        long topHours = topSeconds / 3600;
        long topMinutes = (topSeconds % 3600) / 60;
        String topTime = String.format("%d giờ %d phút", topHours, topMinutes);
        if (topHours == 0) {
          topTime = String.format("%d phút", topMinutes);
        }

        // Get user mention
        User topUser = event.getJDA().getUserById(topUserId);
        String userMention = topUser != null ? topUser.getAsMention() : "<@" + topUserId + ">";

        String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "•";
        sb.append(medal).append(" ").append(rank).append(". ").append(userMention)
            .append(" - ").append(topTime).append("\n");
        
        rank++;
        if (rank > 10) break;
      }
    }

    event.getMessage().replyEmbeds(EmbedUtils.createEmbed("👤 **Personal Statistics**", sb.toString(), EmbedUtils.COLOR_INFO, event.getAuthor()))
        .queue();
  }
}

