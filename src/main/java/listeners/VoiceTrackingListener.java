package listeners;

import managers.DatabaseManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener to track user voice channel activity for statistics.
 */
public class VoiceTrackingListener extends ListenerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(VoiceTrackingListener.class);
  private final DatabaseManager databaseManager;
  // Track active sessions: guildId_userId -> sessionId
  private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();

  public VoiceTrackingListener() {
    this.databaseManager = DatabaseManager.getInstance();
  }

  @Override
  public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
    Member member = event.getMember();
    Guild guild = event.getGuild();
    
    // Only track non-bot members
    if (member.getUser().isBot()) {
      return;
    }

    String guildId = guild.getId();
    String userId = member.getId();
    String sessionKey = guildId + "_" + userId;

    // Check if bot is playing music in this guild
    managers.VoiceManager voiceManager = managers.VoiceManager.getInstance();
    if (!voiceManager.isConnected(guild)) {
      return; // Bot not playing, don't track
    }

    // Check if user joined the same channel as bot
    if (event.getChannelJoined() != null) {
      // User joined a voice channel
      if (event.getChannelJoined().equals(guild.getSelfMember().getVoiceState().getChannel())) {
        // User joined bot's voice channel, start tracking
        long sessionId = databaseManager.startPlaybackSession(guildId, userId);
        if (sessionId > 0) {
          activeSessions.put(sessionKey, sessionId);
          logger.debug("Started tracking listening session for user {} in guild {}", userId, guildId);
        }
      }
    }

    // Check if user left voice channel
    if (event.getChannelLeft() != null) {
      // User left a voice channel
      Long sessionId = activeSessions.remove(sessionKey);
      if (sessionId != null) {
        // End the session
        databaseManager.endPlaybackSession(sessionId);
        logger.debug("Ended tracking listening session for user {} in guild {}", userId, guildId);
      }
    }
  }
}

