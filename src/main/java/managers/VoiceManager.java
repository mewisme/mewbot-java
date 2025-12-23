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
  // Track playback start time per guild for statistics
  private final Map<Long, Long> guildPlaybackStartTime; // guildId -> startTimestamp

  private VoiceManager() {
    this.playerManager = new DefaultAudioPlayerManager();
    this.players = new HashMap<>();
    this.guildAudioManagers = new HashMap<>();
    this.guildVoiceChannels = new HashMap<>();
    this.guildPlayingTracks = new HashMap<>();
    this.guildPlaybackStartTime = new HashMap<>();

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
   * Sets the playback start time for a guild (for statistics).
   *
   * @param guild     The guild
   * @param startTime Start timestamp in seconds
   */
  public void setGuildPlaybackStartTime(Guild guild, long startTime) {
    guildPlaybackStartTime.put(guild.getIdLong(), startTime);
  }

  /**
   * Sets the volume for a guild's audio player.
   *
   * @param guild  The guild
   * @param volume Volume (0-100)
   * @return true if volume was set successfully, false otherwise
   */
  public boolean setVolume(Guild guild, int volume) {
    try {
      // Clamp volume to 0-100
      volume = Math.max(0, Math.min(100, volume));

      AudioPlayer player = getPlayer(guild);
      if (player != null) {
        player.setVolume(volume);

        // Save to database
        DatabaseManager.getInstance().setGuildVolume(guild.getId(), volume);

        logger.info("Set volume to {}% for guild: {}", volume, guild.getName());
        return true;
      }
    } catch (Exception e) {
      logger.error("Failed to set volume", e);
    }
    return false;
  }

  /**
   * Gets the current volume for a guild's audio player.
   *
   * @param guild The guild
   * @return Current volume (0-100), or default from database if player not found
   */
  public int getVolume(Guild guild) {
    try {
      AudioPlayer player = getPlayer(guild);
      if (player != null) {
        return player.getVolume();
      }
    } catch (Exception e) {
      logger.error("Failed to get volume", e);
    }
    // Fallback to database if player not initialized
    return DatabaseManager.getInstance().getGuildVolume(guild.getId());
  }

  /**
   * Stops playback and cleans up resources for a guild.
   *
   * @param guild The guild to clean up
   */
  public void cleanup(Guild guild) {
    // Record final playback time before cleanup
    Long startTime = guildPlaybackStartTime.remove(guild.getIdLong());
    if (startTime != null && startTime > 0) {
      long duration = (System.currentTimeMillis() / 1000) - startTime;
      if (duration > 0) {
        DatabaseManager.getInstance().addGuildPlaybackTime(guild.getId(), duration);
      }
    }

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
          // Restore volume from database
          int volume = DatabaseManager.getInstance().getGuildVolume(guild.getId());
          guildManager.getPlayer().setVolume(volume);

          // Check if scheduler was looping
          TrackScheduler scheduler = guildManager.getScheduler();
          if (scheduler != null && scheduler.isLooping()) {
            // Resume looping
            scheduler.setStreamUrl(storedTrack.getInfo().uri);
            guildManager.getPlayer().startTrack(storedTrack.makeClone(), false);
            logger.info("Resumed playback after reconnection in guild: {} at {}% volume", guild.getName(), volume);
          } else {
            // Just resume the track
            guildManager.getPlayer().startTrack(storedTrack.makeClone(), false);
            logger.info("Resumed track after reconnection in guild: {} at {}% volume", guild.getName(), volume);
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
