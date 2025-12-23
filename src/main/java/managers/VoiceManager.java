package managers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages voice connections and audio playback for the bot.
 */
public class VoiceManager {
  private static final Logger logger = LoggerFactory.getLogger(VoiceManager.class);
  private static VoiceManager instance;
  private final AudioPlayerManager playerManager;
  private final Map<Long, AudioPlayer> players;
  private final Map<Long, GuildAudioManager> guildAudioManagers;
  // Store voice channel info for reconnection
  private final Map<Long, Long> guildVoiceChannels; // guildId -> voiceChannelId
  private final Map<Long, AudioTrack> guildPlayingTracks; // guildId -> track (for resume)

  private VoiceManager() {
    this.playerManager = new DefaultAudioPlayerManager();
    this.players = new HashMap<>();
    this.guildAudioManagers = new HashMap<>();
    this.guildVoiceChannels = new HashMap<>();
    this.guildPlayingTracks = new HashMap<>();

    // Configure player manager
    playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
    playerManager.registerSourceManager(new HttpAudioSourceManager());
  }

  /**
   * Gets the singleton instance of VoiceManager.
   *
   * @return VoiceManager instance
   */
  public static VoiceManager getInstance() {
    if (instance == null) {
      instance = new VoiceManager();
    }
    return instance;
  }

  /**
   * Gets or creates an AudioPlayer for a guild.
   *
   * @param guild The guild
   * @return AudioPlayer for the guild
   */
  public AudioPlayer getPlayer(Guild guild) {
    return players.computeIfAbsent(guild.getIdLong(), id -> {
      AudioPlayer player = playerManager.createPlayer();
      GuildAudioManager guildManager = new GuildAudioManager(player);
      guildAudioManagers.put(id, guildManager);
      return player;
    });
  }

  /**
   * Gets the GuildAudioManager for a guild.
   *
   * @param guild The guild
   * @return GuildAudioManager for the guild
   */
  public GuildAudioManager getGuildAudioManager(Guild guild) {
    return guildAudioManagers.get(guild.getIdLong());
  }

  /**
   * Connects to a voice channel.
   *
   * @param channel The voice channel to connect to
   * @return true if connected successfully, false otherwise
   */
  public boolean connectToVoiceChannel(VoiceChannel channel) {
    try {
      Guild guild = channel.getGuild();
      AudioManager audioManager = guild.getAudioManager();

      // Get or create player and guild manager for this guild
      // This ensures both are created if they don't exist
      getPlayer(guild);
      GuildAudioManager guildManager = getGuildAudioManager(guild);

      if (guildManager == null) {
        logger.error("Failed to create guild audio manager");
        return false;
      }

      // Set the sending handler
      audioManager.setSendingHandler(guildManager.getSendHandler());

      // Connect to voice channel
      audioManager.openAudioConnection(channel);

      // Set self deafened to disable listening (only play audio, no listening icon)
      audioManager.setSelfDeafened(true);

      // Store voice channel info for reconnection
      guildVoiceChannels.put(guild.getIdLong(), channel.getIdLong());

      logger.info("Connected to voice channel: {} in guild: {}",
          channel.getName(), guild.getName());
      return true;
    } catch (Exception e) {
      logger.error("Failed to connect to voice channel", e);
      return false;
    }
  }

  /**
   * Disconnects from voice channel in a guild.
   *
   * @param guild The guild to disconnect from
   */
  public void disconnectFromVoiceChannel(Guild guild) {
    try {
      AudioManager audioManager = guild.getAudioManager();
      if (audioManager.isConnected()) {
        // Store current track before disconnecting (for reconnection)
        GuildAudioManager guildManager = getGuildAudioManager(guild);
        if (guildManager != null) {
          AudioTrack currentTrack = guildManager.getPlayer().getPlayingTrack();
          if (currentTrack != null) {
            guildPlayingTracks.put(guild.getIdLong(), currentTrack.makeClone());
          }
        }

        audioManager.closeAudioConnection();
        // Don't remove voice channel info here - keep it for reconnection
        logger.info("Disconnected from voice channel in guild: {}", guild.getName());
      }
    } catch (Exception e) {
      logger.error("Failed to disconnect from voice channel", e);
    }
  }

  /**
   * Manually removes voice channel info (when user explicitly leaves).
   *
   * @param guild The guild
   */
  public void removeVoiceChannelInfo(Guild guild) {
    guildVoiceChannels.remove(guild.getIdLong());
    guildPlayingTracks.remove(guild.getIdLong());
  }

  /**
   * Checks if bot is connected to a voice channel in a guild.
   *
   * @param guild The guild to check
   * @return true if connected, false otherwise
   */
  public boolean isConnected(Guild guild) {
    return guild.getAudioManager().isConnected();
  }

  /**
   * Gets the AudioPlayerManager.
   *
   * @return AudioPlayerManager
   */
  public AudioPlayerManager getPlayerManager() {
    return playerManager;
  }

  /**
   * Stops playback and cleans up resources for a guild.
   *
   * @param guild The guild to clean up
   */
  public void cleanup(Guild guild) {
    GuildAudioManager guildManager = guildAudioManagers.remove(guild.getIdLong());
    if (guildManager != null) {
      guildManager.getPlayer().destroy();
    }
    players.remove(guild.getIdLong());
    guildVoiceChannels.remove(guild.getIdLong());
    guildPlayingTracks.remove(guild.getIdLong());
  }

  /**
   * Gets the stored voice channel ID for a guild (for reconnection).
   *
   * @param guild The guild
   * @return Voice channel ID, or null if not stored
   */
  public Long getStoredVoiceChannelId(Guild guild) {
    return guildVoiceChannels.get(guild.getIdLong());
  }

  /**
   * Gets the stored playing track for a guild (for resume after reconnection).
   *
   * @param guild The guild
   * @return AudioTrack clone, or null if not stored
   */
  public AudioTrack getStoredPlayingTrack(Guild guild) {
    return guildPlayingTracks.get(guild.getIdLong());
  }

  /**
   * Attempts to reconnect to the stored voice channel and resume playback.
   *
   * @param guild The guild to reconnect in
   * @return true if reconnected successfully, false otherwise
   */
  public boolean reconnectToVoiceChannel(Guild guild) {
    Long voiceChannelId = getStoredVoiceChannelId(guild);
    if (voiceChannelId == null) {
      logger.debug("No stored voice channel for guild: {}", guild.getName());
      return false;
    }

    try {
      VoiceChannel channel = guild.getVoiceChannelById(voiceChannelId);
      if (channel == null) {
        logger.warn("Stored voice channel {} no longer exists in guild: {}", voiceChannelId, guild.getName());
        removeVoiceChannelInfo(guild);
        return false;
      }

      // Reconnect to voice channel
      if (!connectToVoiceChannel(channel)) {
        return false;
      }

      // Resume playback if there was a track playing
      AudioTrack storedTrack = getStoredPlayingTrack(guild);
      if (storedTrack != null) {
        GuildAudioManager guildManager = getGuildAudioManager(guild);
        if (guildManager != null) {
          // Check if scheduler was looping
          TrackScheduler scheduler = guildManager.getScheduler();
          if (scheduler != null && scheduler.isLooping()) {
            // Resume looping
            scheduler.setStreamUrl(storedTrack.getInfo().uri);
            guildManager.getPlayer().startTrack(storedTrack.makeClone(), false);
            logger.info("Resumed playback after reconnection in guild: {}", guild.getName());
          } else {
            // Just resume the track
            guildManager.getPlayer().startTrack(storedTrack.makeClone(), false);
            logger.info("Resumed track after reconnection in guild: {}", guild.getName());
          }
        }
      }

      return true;
    } catch (Exception e) {
      logger.error("Failed to reconnect to voice channel in guild: {}", guild.getName(), e);
      return false;
    }
  }
}
