package listeners;

import managers.VoiceManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listener to handle automatic voice channel reconnection after network issues.
 */
public class VoiceReconnectListener extends ListenerAdapter {
  private static final Logger logger = LoggerFactory.getLogger(VoiceReconnectListener.class);
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final VoiceManager voiceManager;

  public VoiceReconnectListener() {
    this.voiceManager = VoiceManager.getInstance();
  }

  @Override
  public void onReady(@NotNull ReadyEvent event) {
    logger.info("Bot is ready, checking for voice channels to reconnect...");
    // Wait a bit for all guilds to be fully loaded
    scheduler.schedule(() -> {
      reconnectAllVoiceChannels(event.getJDA().getGuilds());
    }, 5, TimeUnit.SECONDS);
  }

  // Note: JDA doesn't have a direct ReconnectedEvent, but ReadyEvent fires on
  // reconnect
  // We'll handle reconnection in onReady and onGuildVoiceUpdate

  @Override
  public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
    // Detect when bot is disconnected from voice channel
    if (event.getEntity().equals(event.getGuild().getSelfMember())) {
      // Bot's voice state changed
      if (event.getChannelLeft() != null && event.getChannelJoined() == null) {
        // Bot was disconnected from voice channel
        Guild guild = event.getGuild();
        logger.warn("Bot was disconnected from voice channel in guild: {} (Channel: {})",
            guild.getName(), event.getChannelLeft().getName());

        // Check if this was an unexpected disconnect (not manual)
        // If we have stored voice channel info, it means we should reconnect
        Long storedChannelId = voiceManager.getStoredVoiceChannelId(guild);
        if (storedChannelId != null && storedChannelId.equals(event.getChannelLeft().getIdLong())) {
          // This was our stored channel, attempt to reconnect after a delay
          logger.info("Scheduling reconnection attempt for guild: {}", guild.getName());
          scheduler.schedule(() -> {
            if (!voiceManager.isConnected(guild)) {
              logger.info("Attempting to reconnect to voice channel in guild: {}", guild.getName());
              boolean reconnected = voiceManager.reconnectToVoiceChannel(guild);
              if (reconnected) {
                logger.info("Successfully reconnected to voice channel in guild: {}", guild.getName());
              } else {
                logger.warn("Failed to reconnect to voice channel in guild: {}", guild.getName());
              }
            }
          }, 2, TimeUnit.SECONDS);
        }
      }
    }
  }

  /**
   * Attempts to reconnect to all stored voice channels.
   *
   * @param guilds The guilds to check
   */
  private void reconnectAllVoiceChannels(java.util.List<Guild> guilds) {
    for (Guild guild : guilds) {
      Long storedChannelId = voiceManager.getStoredVoiceChannelId(guild);
      if (storedChannelId != null && !voiceManager.isConnected(guild)) {
        logger.info("Attempting to reconnect to stored voice channel in guild: {}", guild.getName());
        boolean reconnected = voiceManager.reconnectToVoiceChannel(guild);
        if (reconnected) {
          logger.info("Successfully reconnected to voice channel in guild: {}", guild.getName());
        } else {
          logger.warn("Failed to reconnect to voice channel in guild: {}", guild.getName());
        }
      }
    }
  }
}
