package managers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler for audio tracks that handles looping and track end events.
 */
public class TrackScheduler extends AudioEventAdapter {
  private static final Logger logger = LoggerFactory.getLogger(TrackScheduler.class);
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final AudioPlayer player;
  private String streamUrl;
  private boolean shouldLoop;

  public TrackScheduler(AudioPlayer player) {
    this.player = player;
    this.shouldLoop = false;
  }

  @Override
  public void onTrackEnd(AudioPlayer eventPlayer, AudioTrack track, AudioTrackEndReason endReason) {
    // If track ended naturally and we should loop, restart it
    if (endReason.mayStartNext && shouldLoop && streamUrl != null) {
      logger.info("Track ended, restarting stream: {}", streamUrl);
      // Reload the stream
      VoiceManager.getInstance().getPlayerManager().loadItemOrdered(this.player, streamUrl,
          new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack newTrack) {
              TrackScheduler.this.player.startTrack(newTrack.makeClone(), false);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
              // Should not happen for a stream URL
            }

            @Override
            public void noMatches() {
              logger.warn("Could not reload stream: {}", streamUrl);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
              logger.error("Failed to reload stream: {}", streamUrl, exception);
              // Retry after a delay using scheduled executor
              scheduler.schedule(() -> {
                if (shouldLoop && streamUrl != null) {
                  VoiceManager.getInstance().getPlayerManager().loadItemOrdered(TrackScheduler.this.player, streamUrl,
                      this);
                }
              }, 5, TimeUnit.SECONDS);
            }
          });
    }
  }

  /**
   * Sets the stream URL to loop.
   *
   * @param streamUrl The stream URL
   */
  public void setStreamUrl(String streamUrl) {
    this.streamUrl = streamUrl;
    this.shouldLoop = true;
  }

  /**
   * Stops looping.
   */
  public void stopLooping() {
    this.shouldLoop = false;
    this.streamUrl = null;
  }
}
