package commands;

import managers.VoiceManager;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Command to make the bot leave the voice channel.
 */
public class LeaveCommand extends Command {
  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("leave", "Make the bot leave the voice channel");
  }

  @Override
  public void execute(@NotNull SlashCommandInteractionEvent event) {
    // Check if user is in a guild (not DM)
    if (!event.isFromGuild()) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Check if bot is connected to a voice channel
    if (!voiceManager.isConnected(event.getGuild())) {
      event.replyEmbeds(EmbedUtils.createWarningEmbed("I'm not connected to any voice channel!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    // Stop any playing tracks and stop looping
    managers.GuildAudioManager guildManager = voiceManager.getGuildAudioManager(event.getGuild());
    if (guildManager != null) {
      guildManager.getScheduler().stopLooping();
      guildManager.getPlayer().stopTrack();
    }

    // Disconnect from voice channel
    voiceManager.disconnectFromVoiceChannel(event.getGuild());
    voiceManager.cleanup(event.getGuild());

    event.replyEmbeds(EmbedUtils.createSuccessEmbed("✅ Successfully left the voice channel!", event.getUser()))
        .queue();
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    // Check if user is in a guild (not DM)
    if (!event.isFromGuild()) {
      event.getMessage().replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!",
          event.getAuthor())).queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Check if bot is connected to a voice channel
    if (!voiceManager.isConnected(event.getGuild())) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createWarningEmbed("I'm not connected to any voice channel!", event.getAuthor()))
          .queue();
      return;
    }

    // Stop any playing tracks and stop looping
    managers.GuildAudioManager guildManager = voiceManager.getGuildAudioManager(event.getGuild());
    if (guildManager != null) {
      guildManager.getScheduler().stopLooping();
      guildManager.getPlayer().stopTrack();
    }

    // Disconnect from voice channel
    voiceManager.disconnectFromVoiceChannel(event.getGuild());
    voiceManager.cleanup(event.getGuild());

    event.getMessage()
        .replyEmbeds(EmbedUtils.createSuccessEmbed("✅ Successfully left the voice channel!", event.getAuthor()))
        .queue();
  }
}
