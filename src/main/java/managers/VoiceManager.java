package managers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
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

  private VoiceManager() {
    this.playerManager = new DefaultAudioPlayerManager();
    this.players = new HashMap<>();
    this.guildAudioManagers = new HashMap<>();

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
        audioManager.closeAudioConnection();
        logger.info("Disconnected from voice channel in guild: {}", guild.getName());
      }
    } catch (Exception e) {
      logger.error("Failed to disconnect from voice channel", e);
    }
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
  }
}
