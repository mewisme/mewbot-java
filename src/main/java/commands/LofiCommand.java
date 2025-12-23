package commands;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import managers.DatabaseManager;
import managers.VoiceManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import utils.EmbedUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to play lofi music 24/7 from a stream URL.
 */
public class LofiCommand extends Command {
  private static final Logger logger = LoggerFactory.getLogger(LofiCommand.class);
  private static final String LOFI_STREAM_URL = "https://lofi4u.com/api/stream/live";
  private static final String SUCCESS_MESSAGE = "🎵 **Now playing lofi music 24/7!**\n\nThe bot will stay connected until kicked or `/leave` is used.";

  @Override
  @NotNull
  public SlashCommandData getCommandData() {
    return Commands.slash("lofi", "Play lofi music 24/7 in your voice channel");
  }

  @Override
  public void execute(@NotNull SlashCommandInteractionEvent event) {
    // Check if user is in a guild (not DM)
    if (!event.isFromGuild()) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    // Get the member who invoked the command
    Member member = event.getMember();
    if (member == null) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("Unable to get member information!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    // Check if member is in a voice channel
    if (member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
      event.replyEmbeds(EmbedUtils.createWarningEmbed("You need to be in a voice channel to use this command!",
          event.getUser())).setEphemeral(true).queue();
      return;
    }

    VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
    if (voiceChannel == null) {
      event.replyEmbeds(EmbedUtils.createErrorEmbed("Unable to get voice channel!", event.getUser()))
          .setEphemeral(true).queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Connect to voice channel (or reconnect if already connected)
    if (!voiceManager.isConnected(event.getGuild())) {
      if (!voiceManager.connectToVoiceChannel(voiceChannel)) {
        event.replyEmbeds(EmbedUtils.createErrorEmbed("Failed to connect to voice channel!", event.getUser()))
            .setEphemeral(true).queue();
        return;
      }
    }

    // Get the audio player and scheduler for this guild
    AudioPlayer player = voiceManager.getPlayer(event.getGuild());
    managers.GuildAudioManager guildManager = voiceManager.getGuildAudioManager(event.getGuild());

    // Set up looping for the stream
    if (guildManager != null) {
      guildManager.getScheduler().setStreamUrl(LOFI_STREAM_URL);
    }

    // Load and play the lofi stream
    event.deferReply().queue();

    voiceManager.getPlayerManager().loadItemOrdered(player, LOFI_STREAM_URL, new AudioLoadResultHandler() {
      @Override
      public void trackLoaded(AudioTrack track) {
        // Start playing the track
        player.startTrack(track, false);

        // Set volume from database (default 50%)
        int volume = DatabaseManager.getInstance().getGuildVolume(event.getGuild().getId());
        player.setVolume(volume);

        event.getHook().editOriginalEmbeds(EmbedUtils.createMusicEmbed(SUCCESS_MESSAGE, event.getUser())).queue();
        logger.info("Started playing lofi stream in guild: {} at {}% volume", event.getGuild().getName(), volume);
      }

      @Override
      public void playlistLoaded(AudioPlaylist playlist) {
        // If it's a playlist, play the first track
        AudioTrack firstTrack = playlist.getSelectedTrack();
        if (firstTrack == null) {
          firstTrack = playlist.getTracks().get(0);
        }

        player.startTrack(firstTrack, false);
        
        // Set volume from database (default 50%)
        int volume = DatabaseManager.getInstance().getGuildVolume(event.getGuild().getId());
        player.setVolume(volume);

        event.getHook().editOriginalEmbeds(EmbedUtils.createMusicEmbed(SUCCESS_MESSAGE, event.getUser())).queue();
        logger.info("Started playing lofi playlist in guild: {} at {}% volume", event.getGuild().getName(), volume);
      }

      @Override
      public void noMatches() {
        event.getHook().editOriginalEmbeds(
            EmbedUtils.createErrorEmbed("❌ Could not find the audio stream. Please check the URL.", event.getUser()))
            .queue();
        logger.error("No matches found for lofi stream URL: {}", LOFI_STREAM_URL);
      }

      @Override
      public void loadFailed(FriendlyException exception) {
        event.getHook().editOriginalEmbeds(EmbedUtils.createErrorEmbed(
            "❌ Failed to load the audio stream: " + exception.getMessage(), event.getUser())).queue();
        logger.error("Failed to load lofi stream", exception);
      }
    });
  }

  @Override
  public void executeMessage(@NotNull MessageReceivedEvent event, @NotNull String args) {
    // Check if user is in a guild (not DM)
    if (!event.isFromGuild()) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createErrorEmbed("This command can only be used in a server!", event.getAuthor()))
          .queue();
      return;
    }

    // Get the member who invoked the command
    Member member = event.getMember();
    if (member == null) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createErrorEmbed("Unable to get member information!", event.getAuthor())).queue();
      return;
    }

    // Check if member is in a voice channel
    if (member.getVoiceState() == null || !member.getVoiceState().inAudioChannel()) {
      event.getMessage().replyEmbeds(
          EmbedUtils.createWarningEmbed("You need to be in a voice channel to use this command!", event.getAuthor()))
          .queue();
      return;
    }

    VoiceChannel voiceChannel = member.getVoiceState().getChannel().asVoiceChannel();
    if (voiceChannel == null) {
      event.getMessage()
          .replyEmbeds(EmbedUtils.createErrorEmbed("Unable to get voice channel!", event.getAuthor())).queue();
      return;
    }

    VoiceManager voiceManager = VoiceManager.getInstance();

    // Connect to voice channel (or reconnect if already connected)
    if (!voiceManager.isConnected(event.getGuild())) {
      if (!voiceManager.connectToVoiceChannel(voiceChannel)) {
        event.getMessage()
            .replyEmbeds(EmbedUtils.createErrorEmbed("Failed to connect to voice channel!", event.getAuthor()))
            .queue();
        return;
      }
    }

    // Get the audio player and scheduler for this guild
    AudioPlayer player = voiceManager.getPlayer(event.getGuild());
    managers.GuildAudioManager guildManager = voiceManager.getGuildAudioManager(event.getGuild());

    // Set up looping for the stream
    if (guildManager != null) {
      guildManager.getScheduler().setStreamUrl(LOFI_STREAM_URL);
    }

    // Send initial message
    event.getMessage()
        .replyEmbeds(EmbedUtils.createInfoEmbed("⏳ Loading lofi stream...", event.getAuthor()))
        .queue(reply -> {
          // Load and play the lofi stream
          voiceManager.getPlayerManager().loadItemOrdered(player, LOFI_STREAM_URL,
              new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                  player.startTrack(track, false);
                  
                  // Set volume from database (default 50%)
                  int volume = DatabaseManager.getInstance().getGuildVolume(event.getGuild().getId());
                  player.setVolume(volume);
                  
                  reply.editMessageEmbeds(EmbedUtils.createMusicEmbed(SUCCESS_MESSAGE, event.getAuthor())).queue();
                  logger.info("Started playing lofi stream in guild: {} at {}% volume", event.getGuild().getName(), volume);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                  AudioTrack firstTrack = playlist.getSelectedTrack();
                  if (firstTrack == null) {
                    firstTrack = playlist.getTracks().get(0);
                  }
                  player.startTrack(firstTrack, false);
                  
                  // Set volume from database (default 50%)
                  int volume = DatabaseManager.getInstance().getGuildVolume(event.getGuild().getId());
                  player.setVolume(volume);
                  
                  reply.editMessageEmbeds(EmbedUtils.createMusicEmbed(SUCCESS_MESSAGE, event.getAuthor())).queue();
                  logger.info("Started playing lofi playlist in guild: {}", event.getGuild().getName());
                }

                @Override
                public void noMatches() {
                  reply.editMessageEmbeds(EmbedUtils.createErrorEmbed(
                      "❌ Could not find the audio stream. Please check the URL.", event.getAuthor())).queue();
                  logger.error("No matches found for lofi stream URL: {}", LOFI_STREAM_URL);
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                  reply.editMessageEmbeds(EmbedUtils.createErrorEmbed(
                      "❌ Failed to load the audio stream: " + exception.getMessage(), event.getAuthor())).queue();
                  logger.error("Failed to load lofi stream", exception);
                }
              });
        });
  }
}
