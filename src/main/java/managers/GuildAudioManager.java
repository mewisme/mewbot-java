package managers;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import java.nio.ByteBuffer;

/**
 * Manages audio for a specific guild.
 */
public class GuildAudioManager {
  private final AudioPlayer player;
  private final AudioPlayerSendHandler sendHandler;
  private final TrackScheduler scheduler;

  public GuildAudioManager(AudioPlayer player) {
    this.player = player;
    this.sendHandler = new AudioPlayerSendHandler(player);
    this.scheduler = new TrackScheduler(player);
    player.addListener(scheduler);
  }

  public AudioPlayer getPlayer() {
    return player;
  }

  public AudioSendHandler getSendHandler() {
    return sendHandler;
  }

  public TrackScheduler getScheduler() {
    return scheduler;
  }
}

/**
 * AudioSendHandler implementation for LavaPlayer.
 */
class AudioPlayerSendHandler implements AudioSendHandler {
  private final AudioPlayer audioPlayer;
  private com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame lastFrame;

  public AudioPlayerSendHandler(AudioPlayer audioPlayer) {
    this.audioPlayer = audioPlayer;
  }

  @Override
  public boolean canProvide() {
    lastFrame = audioPlayer.provide();
    return lastFrame != null;
  }

  @Override
  public ByteBuffer provide20MsAudio() {
    if (lastFrame == null) {
      return null;
    }
    return ByteBuffer.wrap(lastFrame.getData());
  }

  @Override
  public boolean isOpus() {
    return true;
  }
}
