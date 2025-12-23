package bot;

import listeners.MessageCommandListener;
import listeners.SlashCommandListener;
import listeners.VoiceReconnectListener;
import listeners.VoiceTrackingListener;
import managers.CommandManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

/**
 * Helper class for building and configuring JDA instances.
 */
public class JdaBuilderHelper {
  /**
   * Builds and configures the JDA instance.
   *
   * @param token          The bot token
   * @param commandManager The command manager instance
   * @param prefix         The command prefix for message commands
   * @param activity       The bot activity
   * @param botInstance    The bot instance (for cluster mode)
   * @return Configured JDA instance
   */
  public static JDA build(String token, CommandManager commandManager, String prefix, Activity activity,
      BotInstance botInstance) {
    SlashCommandListener slashListener = new SlashCommandListener(commandManager);
    MessageCommandListener messageListener = new MessageCommandListener(commandManager, prefix);

    // Set bot instance for cluster mode
    if (botInstance != null) {
      slashListener.setBotInstance(botInstance);
      messageListener.setBotInstance(botInstance);
    }

    VoiceReconnectListener voiceReconnectListener = new VoiceReconnectListener();
    VoiceTrackingListener voiceTrackingListener = new VoiceTrackingListener();

    return JDABuilder.createDefault(token)
        .setActivity(activity)
        .addEventListeners(slashListener, messageListener, voiceReconnectListener, voiceTrackingListener)
        .setMemberCachePolicy(MemberCachePolicy.NONE)
        .enableIntents(
            GatewayIntent.GUILD_VOICE_STATES,
            GatewayIntent.MESSAGE_CONTENT)
        .build();
  }
}
